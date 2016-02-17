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

Create a marathon app
send a http post request to v2/apps

```
{
    "id": "docker-compose-demo",
    "cmd": "echo hello world",
    "cpus": 1.0,
    "mem": 64.0,
    "ports":[0,0,0],
    "instances": 1,
    "executor":"/home/vagrant/aurora/examples/vagrant/docker-compose-executor.sh",
  	"labels": {
        "fileName": "web-app/docker-compose.yml"
    },
  "uris":["https://dl.dropboxusercontent.com/u/26009359/web-app.zip"]
}
```

