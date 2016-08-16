# Overview

compose-executor project aims to enable Mesos frameworks to launch a pod of docker containers. Kubernetes/K8 introduced the notion of a collection of docker containers that share namespaces and treat the collection as a single scaling unit. Brendan Burns talked about some design patterns/use cases for pods in [DockerCon'15](https://www.youtube.com/watch?v=Ph3t8jIt894).

Docker Compose is a cherished tool used in docker community that helps us model a collection of docker containers. The specification is very flexible. Furthermore, you can also model a pod collapsing namespaces (net, IPC , pid).

Composite containers representing an application is a common requirement for modular architecture. Composition requires co-location treating a set of containers as a single unit (aka pod) for scheduling. Sidecar, ambassador, adapter patterns use container pod. docker compose in docker community is an excellent way of defining a collection of containers and can be used to represent pod. Mesos on the other hand plays the critical role of a resource and cluster manager for large clusters. The native docker integration in Mesos can only launch a single container. The recently introduced universal containerizer in Mesos 1.0 does not solve it either. This presents a challenge to launch container pods in Mesos. docker swarm on the other hand as of 1.12 supports DAB (generated from compose) that does not represent a local pod (has to be emulated through constraints). compose-executor helps to immediately address the need of Mesos and docker users helping them launch a set of docker containers, aka pods as a single unit in Mesos. 

## Goal

The project goal is to model a pod of containers with docker-compose and launch it with your favorite Mesos frameworks like Marathon, Apache Aurora, etc. One does not need to switch to Kubernetes on Mesos if all that they are looking to do is launch pods (model pod like workloads). With network and storage plugins supported directly in Docker, one can model advanced pods supported through compose. Furthermore, instead of using some different spec to define pods, we wanted to build around the compose spec that is well accepted in the docker community. A developer can now write the pod spec once, run it locally in their laptop using compose and later seamlessly move into Mesos without having to modify the pod spec.

## Getting started

* [Getting Started](docs/getting-started.md)
* [How Docker Compose Executor works](docs/how-it-works.md)
* [Developing Docker Compose Executor](docs/dev-build.md)

## Tested Frameworks

* [Marathon](https://github.com/mesosphere/marathon)
* [Apache Aurora using gorealis](https://github.com/rdelval/gorealis/blob/master/docs/getting-started.md)


## Licensing
compose-executor is licensed under the Apache License, Version 2.0. See [LICENSE](/LICENSE) for the full license text.
