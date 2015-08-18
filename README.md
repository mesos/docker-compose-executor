# compose-executor

compose-executor project aims to enable Mesos frameworks to launch a pod of docker containers. Kubernetes/K8 introduced the notion of collection of docker containers that shares namespaces and treated the collection as a single scaling unit. Brendan Burns talked about some design patterns/use cases for pods in [DockerCon'15](https://www.youtube.com/watch?v=Ph3t8jIt894).

Docker Compose is a cherished tool used in docker community that helps us model a collection of docker containers. The specification is very flexible where the containers can be launched in multi hosts, or a single host and then you can also model a pod collapsing namespaces (net, IPC supported in docker, PID support coming). The spec is also gaining popularity by 3rd party platforms/IDE like Azure (baked in Visual Studio support), AWS also added support behind it since DockerCon'15.

## Goal

The project goal is to model a pod of containers with docker-compose and launch it with your favorite Mesos frameworks like Marathon, Apache Aurora, etc. One does not need to switch to Kubernetes on Mesos if all that they are looking is to launch pods (model pod like workloads). With network and storage plugins supported directly in Docker, one can model advanced pods supported through compose. Also instead of using some different spec to define pods, we wanted to build around compose that is well accepted in the docker community.

## Licensing
compose-executor is licensed under the Apache License, Version 2.0. See [LICENSE](/LICENSE) for the full license text.
