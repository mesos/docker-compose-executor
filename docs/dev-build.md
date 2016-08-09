# Installing and Running Executor

### Requirements
 - Linux/Unix/Mac OS X 
 - JDK 7
 - Maven

```
git clone git@github.com:mesos/docker-compose-executor.git
cd docker-compose-executor
mvn package -U
```
Use the fat jar generated with all the dependencies.

### Getting Executor Into Mesos-Slave

> Copy the fat jar into your mesos slave

> Marathon supports custom executor and you can specify a shell script (compose_executor.sh).
```
  COMPOSE_JAR_NAME=<path to jar file>/docker-compose-executor_0.0.1.jar
  java -jar ${COMPOSE_JAR_NAME}
```
> Create a new marathon app
```
{
    "id": "docker-compose-demo",
    "cmd": "",
    "cpus": 1.0,
    "mem": 64.0,
    "ports":[0,0,0],
    "instances": 1,
    "executor":"<path to your shell script(compose_executor.sh) in your mesos slave>",
  	"labels": {
        "fileName": "<path to your docker-compose.yml>"
    },
  "uris":["<url where you mesos-slave should download your files from>"]
}
```


