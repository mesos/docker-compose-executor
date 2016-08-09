### Getting started using Vagrant

### Requirements
 - Linux/Unix/Mac OS X 
 - Vagrant / Virtual Box

```
git clone git@github.com:mesos/docker-compose-executor.git
cd docker-compose-executor/
vagrant up
```
You'll have a box with mesos and marathon installed. 

Mesos console
```
 http://192.168.33.7:5050/
```

Marathon
```
http://192.168.33.7:8080/
```

Create a marathon app by sending a JSON payload via HTTP to Marathon's app endpoint (192.168.33.7:8080/v2/apps)
Mesos labels (fileName) takes comma delimited multi-compose files as follows:

"fileName": " sample-web-app/docker-compose.yml,sample-web-app/docker-compose2.yml"
```
{
    "id": "docker-compose-demo",
    "cmd": "echo hello world",
    "cpus": 1.0,
    "mem": 64.0,
    "ports":[0],
    "instances": 1,
    "executor":"/vagrant/examples/vagrant/docker-compose-executor.sh",
  	"labels": {
        "fileName": "sample-app/docker-compose.yml"
    },
  "uris":["https://dl.bintray.com/rdelvalle/mesos-compose-executor/sample-app.tar.gz"]
}
```

Using the curl command, a docker compose job can be created on Marathon as follows:
```
$ curl -H "Content-Type: application/json" -X POST -d '{"id":"docker-compose-demo","cmd":"echo hello world","cpus":1.0,"mem":64.0,"ports":[0],"instances":1,"executor":"/vagrant/examples/vagrant/docker-compose-executor.sh","labels":{"fileName":"sample-app/docker-compose.yml"},"uris":["https://dl.bintray.com/rdelvalle/mesos-compose-executor/sample-app.tar.gz"]}' http://192.168.33.7:8080/v2/apps
```

**Pluggable code for LaunchTask:**

Custom logic can be injected in launchTask, It is exposed as [plugin].(https://github.com/mesos/docker-compose-executor/blob/master/src/main/java/com/paypal/mesos/executor/pluginapi/ComposeExecutorPluginImpl.java)
Default implementation for launchTask, retrieves compose-file names from mesos TaskInfo object. This method can be over-ridden as suited, case by case basis. launchTask() method should return list of compose files path.
