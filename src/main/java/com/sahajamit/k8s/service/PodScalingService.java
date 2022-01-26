package com.sahajamit.k8s.service;

import com.sahajamit.k8s.domain.GridConsoleStatus;
import com.squareup.okhttp.*;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

@Service
public class PodScalingService {
    private static final TrustManager[] UNQUESTIONING_TRUST_MANAGER = new TrustManager[]{
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }
    };
    private static final Logger logger = LoggerFactory.getLogger(PodScalingService.class);
    @Value("${gridUrl}")
    private String gridUrl;
    @Value("${k8s_api_url}")
    private String k8sApiUrl;
    @Value("${node_chrome_max_scale_limit}")
    private int maxScaleLimit;
    @Value("${node_chrome_min_scale_limit}")
    private int minScaleLimit;
    @Value("${k8s_token}")
    private String k8sToken;
    @Autowired
    private GridConsoleService gridStatusService;
    private OkHttpClient httpClient;
    private static int currentScale;
    @Value("${grid_scale_check_frequency_in_sec}")
    private int scaleCheckFrequency;
    @Value("${grid_scale_down_wait_time_in_sec}")
    private int scaleDownWaitTime;
    private int currentScaleDownWaitTime = 0;

    @PostConstruct
    private void init() throws NoSuchAlgorithmException, KeyManagementException {
        logger.info("Grid Console URL: {}", gridUrl);
        logger.info("K8s API URL: {}", k8sApiUrl);
        httpClient = new OkHttpClient();
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, UNQUESTIONING_TRUST_MANAGER, null);
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        httpClient.setSslSocketFactory(sc.getSocketFactory());
    }

    private int getScale() throws IOException {
        Request r = new Request.Builder()
                .url(k8sApiUrl)
                .header("Authorization", "Bearer " + k8sToken)
                .get()
                .build();
        Call call = httpClient.newCall(r);
        Response response = call.execute();
        if (response.code() != 200)
            throw new RuntimeException("Error getting current scale; Make sure the API Token and k8s_api_url are correct");
        String htmlContent = response.body().string();
        JSONObject jsonObject = new JSONObject(htmlContent);
        return jsonObject.getJSONObject("status").getInt("replicas");
    }

    private void updateScale(int scaledValue) throws IOException, InterruptedException {
        if (scaledValue > maxScaleLimit){
            logger.warn("Scale required {} which is more than the max scale limit of {}. Hence no auto-scaling is performed.", scaledValue, maxScaleLimit);
            if (currentScale != maxScaleLimit)
                scale(maxScaleLimit);
        }
        else if (scaledValue < minScaleLimit)
            logger.warn("Scale required {} which is less than the min scale limit of {}. Hence no auto-scaling is performed.", scaledValue, minScaleLimit);
        else {
            scale(scaledValue);
        }
    }

    private void scale(int scaledValue) throws IOException, InterruptedException {
        MediaType JSON = MediaType.parse("application/strategic-merge-patch+json");
        String payload = String.format("{ \"spec\": { \"replicas\": %s } }", scaledValue);
        Request r = new Request.Builder()
                .url(k8sApiUrl)
                .header("Authorization", "Bearer " + k8sToken)
                .header("Accept", "application/json")
                .header("Content-Type", "application/strategic-merge-patch+json")
                .patch(RequestBody.create(JSON, payload))
                .build();
        Call call = httpClient.newCall(r);
        Response response = call.execute();
        if (response.code() != 200)
            throw new RuntimeException("Error while scaling the grid: " + response.body().string());
        String responseString = response.body().string();
        JSONObject jsonObject = new JSONObject(responseString);
        int updatedScale;
        
        JSONObject spec = jsonObject.getJSONObject("spec");
        if (spec.has("replicas"))
            updatedScale = spec.getInt("replicas");
        else
            updatedScale = 0;

        if (updatedScale != scaledValue)
            logger.error("Error in scaling. Here is the json response: " + responseString);
        else
            waitForScaleToHappen(scaledValue);
    }

    public void adjustScale(GridConsoleStatus gridStatus) throws IOException, InterruptedException {
        logger.debug("Let's check if auto-scaling is required...");
        int totalRunningNodes = gridStatus.getAvailableNodesCount() + gridStatus.getBusyNodesCount();
        int queuedRequests = gridStatus.getWaitingRequestsCount();
        currentScale = getScale();
        int requiredScale;
        if (queuedRequests > 0) {
            requiredScale = totalRunningNodes + queuedRequests;
            currentScaleDownWaitTime = 0;
            logger.info("Scale up is required. Current scale: {} and required scale: {}", currentScale, requiredScale);
        } else if (totalRunningNodes < minScaleLimit) {
            requiredScale = minScaleLimit;
            currentScaleDownWaitTime = 0;
            logger.info("Scale up is required. Current scale: {} and required scale: {}", currentScale, requiredScale);
        } else if (totalRunningNodes > minScaleLimit && gridStatus.getBusyNodesCount() == 0) {
            logger.info("Scale down may be required, waiting " + (scaleDownWaitTime - currentScaleDownWaitTime) + " seconds...");
            if (currentScaleDownWaitTime >= scaleDownWaitTime) {
                logger.info("Scale down is required. Current available scale: {} and minimum required scale: {}", currentScale, minScaleLimit);
                requiredScale = minScaleLimit;
            } else {
                currentScaleDownWaitTime += scaleCheckFrequency;
                return;
            }
        } else {
            logger.debug("No scaling is required..");
            if (currentScaleDownWaitTime > 0) {
                logger.info("Grid is still in use, no scale down will be done at this time.");
            }
            currentScaleDownWaitTime = 0;
            return;
        }
        updateScale(requiredScale);
    }

    public void cleanUp() throws IOException, InterruptedException {
        logger.info("Cleaning up the Grid by re-starting all the nodes");
        scale(0);
        scale(minScaleLimit);
    }

    private void waitForScaleToHappen(int scale) throws IOException, InterruptedException {
        GridConsoleStatus gridStatus = null;
        try {
            gridStatus = gridStatusService.getStatus();
        } catch (Exception e) {
            logger.error("Error getting Grid status, will try again...");
        }
        int existingScale = gridStatus.getAvailableNodesCount() + gridStatus.getBusyNodesCount();
        while (existingScale != scale) {
            int pollingTime = 5;
            logger.info("Sleeping {} seconds for scaling to happen. Current scale: {} and required scale: {}", pollingTime, existingScale, scale);
            TimeUnit.SECONDS.sleep(pollingTime);
            try {
                gridStatus = gridStatusService.getStatus();
            } catch (Exception e) {
                logger.error("Error getting Grid status, will try again...");
            }
            existingScale = gridStatus.getAvailableNodesCount() + gridStatus.getBusyNodesCount();
        }
        logger.info("Selenium Grid is successfully scaled to {}", scale);
    }

}
