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

