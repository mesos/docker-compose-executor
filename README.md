# compose-executor

compose-executor project aims to enable Mesos frameworks to launch a pod of docker containers. Kubernetes/K8 introduced the notion of a collection of docker containers that share namespaces and treat the collection as a single scaling unit. Brendan Burns talked about some design patterns/use cases for pods in [DockerCon'15](https://www.youtube.com/watch?v=Ph3t8jIt894).

Docker Compose is a cherished tool used in docker community that helps us model a collection of docker containers. The specification is very flexible: containers can be launched both in multiple hosts or a single host. Furthermore, you can also model a pod collapsing namespaces (net, IPC supported in docker, PID support coming). The spec is also gaining popularity in 3rd party platforms/IDE like Azure (baked in Visual Studio support). AWS also added support behind it since DockerCon'15.

## Goal

The project goal is to model a pod of containers with docker-compose and launch it with your favorite Mesos frameworks like Marathon, Apache Aurora, etc. One does not need to switch to Kubernetes on Mesos if all that they are looking to do is launch pods (model pod like workloads). With network and storage plugins supported directly in Docker, one can model advanced pods supported through compose. Furthermore, instead of using some different spec to define pods, we wanted to build around the compose spec that is well accepted in the docker community.

## Getting started

* [Getting Started](docs/getting-started.md)
* [How Docker Compose Executor works](docs/how-it-works.md)
* [Developing Docker Compose Executor](docs/dev-build.md)

### cgroup_parent support in docker-compose

* docker-py project pull-request: https://github.com/docker/docker-py/pull/716
* Work in progress repo of compose project: https://github.com/mohitsoni/compose

## Licensing
compose-executor is licensed under the Apache License, Version 2.0. See [LICENSE](/LICENSE) for the full license text.
