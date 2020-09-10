### Horizontal Pod Scaler for Selenium Grid Kubernetes

This is a small spring boot application which can be used to auto scale the selenium browser pods running inside a kubernetes cluster.

It can give an elasticity to the K8s based selenium grid where the browser nodes can be scaled up/down on demand basis.

To make sure this application does not end up sucking the entire compute power of your cluster, we have a lower and upper cap beyound which the scale will never go.

To run this application in docker:
```
docker pull sahajamit/selenium-grid-k8s-autoscaler
```

#### Configurable Properties
```
k8s.host=<Kubernetes HOST>
gridUrl=http://${k8s.host}:31178/grid/console
k8s.api.url=https://${k8s.host}:8443/apis/apps/v1/namespaces/default/deployments/selenium-node-chrome-deployment/scale

# This property will control the maximum scale to which the browser pods can be scaled up.
node.chrome.max.scale.limit=2

# This property will decide the minimum number of browser pods you want to run all the time. Recommended value is 1.
node.chrome.min.scale.limit=1

k8s.token=<Kubernetes Service Account Token>
grid.scale.check.frequency.in.sec=10
grid.daily.cleanup.cron=0 10 10 * * ?
logging.level.com.sahajamit.k8s=DEBUG
server.port=8088
```