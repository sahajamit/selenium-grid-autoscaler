### Horizontal Pod Scaler for Selenium Grid Kubernetes

This is a small spring boot application which can be used to auto scale the selenium browser pods running inside a kubernetes cluster.

It can give an elasticity to the K8s based selenium grid where the browser nodes can be scaled up/down on demand basis.

To make sure this application does not end up sucking the entire compute power of your cluster, we have a lower and upper cap beyond which the scale will never go.

Modified from this article and source: 
https://sahajamit.medium.com/spinning-up-your-own-auto-scalable-selenium-grid-in-kubernetes-part-2-15b11f228ed8
https://github.com/sahajamit/selenium-grid-autoscaler

#### Configurable Properties (found in /src/main/resources/application.properties)
```
k8s_host=<Kubernetes HOST>
selenium_grid_host=<Grid_URL_Or_IP>:<Grid_Port> 
gridUrl=http://${selenium_grid_host}/grid/console
k8s_api_url=https://${k8s_host}/v3/project/path_to_chrome_deployment

# This property will control the maximum scale to which the browser pods can be scaled up.
node.chrome.max.scale.limit=2

# This property will decide the minimum number of browser pods you want to run all the time. Recommended value is 1.
node.chrome.min.scale.limit=1

k8s.token=<Kubernetes Service Account Token>
grid_scale_check_frequency_in_sec=10
grid_daily_cleanup_cron=0 10 10 * * ?
logging_level_com_sahajamit_k8s=DEBUG
server_port=8088
```

## Quick start
- Docker is required: https://docs.docker.com/docker-for-windows/install/
- Modify the application.properties with what you need
  - All of this can be supplied as environment variables as well
  - Of note the API Bearer token will be required for this to work
- Build the java application
- Run the following to build the container (from the root repo directory):
  - docker build -t selenium-grid-autoscaler .
- Run the following to start the container:
  - docker run selenium-grid-autoscaler