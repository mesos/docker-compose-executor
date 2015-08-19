# compose-executor

compose-executor project aims to enable Mesos frameworks to launch a pod of docker containers. Kubernetes/K8 introduced the notion of collection of docker containers that shares namespaces and treated the collection as a single scaling unit. Brendan Burns talked about some design patterns/use cases for pods in [DockerCon'15](https://www.youtube.com/watch?v=Ph3t8jIt894).

Docker Compose is a cherished tool used in docker community that helps us model a collection of docker containers. The specification is very flexible where the containers can be launched in multi hosts, or a single host and then you can also model a pod collapsing namespaces (net, IPC supported in docker, PID support coming). The spec is also gaining popularity by 3rd party platforms/IDE like Azure (baked in Visual Studio support), AWS also added support behind it since DockerCon'15.

## Goal

The project goal is to model a pod of containers with docker-compose and launch it with your favorite Mesos frameworks like Marathon, Apache Aurora, etc. One does not need to switch to Kubernetes on Mesos if all that they are looking is to launch pods (model pod like workloads). With network and storage plugins supported directly in Docker, one can model advanced pods supported through compose. Also instead of using some different spec to define pods, we wanted to build around compose that is well accepted in the docker community.

## Tasks
1. Write a general purpose docker-compose executor so that it can be used by any Mesos frameworks (We are delivering a patch in Apache Aurora to have custom executor support other than thermos. Marathon can readily support it). Executor will launch a pod of containers per task and maintain the lifecycle.
2. DockerContainerizer is not best suited to run it because it can launch only one container per task and has few other shortcomings. So, we plan to use MesosContainerizer with cgroups isolator and then with the newly supported cgroups-parent flag, put the docker containers in the pod under the executor cgroup so that it’s a hierarchy. We are delivering a patch in docker-py and compose to support this docker flag.
3. Using Mesos hooks, we plan to catch rare conditions where executor crashes and containers can leak and then do cleanup of those.
4. The compose spec file can be downloaded in the mesos sandbox through mesos task URL.
5. We will make sure the container names haves proper prefix to reflect the mesos task.
6. Multiple pods can be launched in the single host without conflicts.
7. Container stdout/stderr logs should be redirected to sandbox stdout/stderr.
8. Going to write a  standalone tool that can parse the compose file to add up the resources (cpu/mem) of the containers in the compose spec because for Mesos it’s a single task. This is needed before submitting the task to Marathon/Aurora. Surely in future there can be frameworks which consumes the compose file in the framework and handles it in server side with local pod or distributed containers treated a unit (compose with docker network namespace say) and spread across multiple tasks. We are not modelling that for now.
9. We are going to write a sample compose file trying to model like a pod collapsing net namespace. Once pid namespace is collapsed, one can run tools like wetty in a separate container in the pod and give terminal emulator to the pod (avoid ssh/docker exec access). We will show a wetty integration merged into a container rather than a separate container for now. Other monitoring/debug tools can be launched in a separate container in the pod.

## Licensing
compose-executor is licensed under the Apache License, Version 2.0. See [LICENSE](/LICENSE) for the full license text.
