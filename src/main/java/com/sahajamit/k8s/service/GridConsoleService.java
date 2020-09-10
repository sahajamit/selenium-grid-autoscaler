package com.sahajamit.k8s.service;

import com.sahajamit.k8s.domain.GridConsoleStatus;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GridConsoleService {

    @Value("${gridUrl}")
    private String gridUrl;

    private HttpClient client = new HttpClient();

    private String availableNode = "img src='/grid/resources/org/openqa/grid/images/chrome.png' width='16' height='16' title";
    private String busyNode = "img src='/grid/resources/org/openqa/grid/images/chrome.png' width='16' height='16' class='busy' ";
    private Pattern pendingRequests = Pattern.compile("\\d{1,} requests waiting for a slot to be free");

    public GridConsoleStatus getStatus() throws IOException {
        HttpMethod method = new GetMethod(gridUrl);
        client.executeMethod(method);
        String htmlContent = method.getResponseBodyAsString();

        GridConsoleStatus status = new GridConsoleStatus();


        int availableNodesCount = StringUtils.countOccurrencesOf(htmlContent, availableNode);
        status.setAvailableNodesCount(availableNodesCount);


        int busyNodesCount = StringUtils.countOccurrencesOf(htmlContent, busyNode);
        status.setBusyNodesCount(busyNodesCount);


        int waitingRequestsCount = 0;
        Matcher matcher = pendingRequests.matcher(htmlContent);
        if (matcher.find()) {
            waitingRequestsCount = Integer.parseInt(matcher.group().split(" ")[0]);
        }
        status.setWaitingRequestsCount(waitingRequestsCount);

        return status;
    }

}
