# How it Works?

## Introduction

Docker compose executor allows you to run a pod of docker containers using docker-compose.
The goal is to make sure all docker network and storage plugins work out of the box.

Running multiple pods on the same host may create many conflicts (containerId's , ports etc.). Executor takes care of resolving these conflicts.

Docker Compose isn't flexible with restart polocies for the whole pod. Executor honors restart polocies of each independent cotainer and destroy's the pod if it finds a voilation. Goal is to allow developers to plugin their own version of pod monitor.

## What exactly is done ?

We use mesos labels(fileName) to read the location of docker-compose file.

We generate a new docker-compose file resolving all the conflicts

We tag each container with specific taskId and executorId and use this information to clean up containers.

The pod is monitored at a configurable interval and will be destroyed if restart policy of any  container is voilated.

We leverage mesos hooks to make sure containers are cleaned if executor is terminated.


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


