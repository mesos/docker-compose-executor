# Installing and Running Executor

### Requirements
 - Linux/Unix/Mac OS X 
 - JDK 7
 - Maven

```
git clone git@github.com:mesos/docker-compose-executor.git
mvn package -U
```
Use the fat jar generated with all the dependencies.

### Getting Executor Into Mesos-Slave

> Copy the fat jar into your mesos slave

> Marathon supports custom executor and you can specify a shell script (compose_executor.sh).
```
  COMPOSE_JAR_NAME=<path to jar file>/docker-compose-executor-0.0.1-SNAPSHOT-jar-with-dependencies.jar
  COMPOSE_CLASS_NAME=com.paypal.mesos.executor.App
  java -cp ${COMPOSE_JAR_NAME} ${COMPOSE_CLASS_NAME}
```
> Create a new marathon app
```
1.Create a marathon app POST /v2/apps
Sample payload:

{
    "id": "docker-compose-demo",
    "cmd": "",
    "cpus": 1.0,
    "mem": 64.0,
    "ports":[0,0,0],
    "instances": 1,
    "executor":"<path to your shell script>",
  	"labels": {
        "fileName": "<path to your docker-compose.yml>"
    },
  "uris":["<url where you mesos-slave should download your files from>"]
}
```


---
<sub>
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

<sub>
  http://www.apache.org/licenses/LICENSE-2.0

<sub>
Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
