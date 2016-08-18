package com.paypal.mesos.executor.compose;

import org.apache.log4j.Logger;
import org.apache.mesos.Protos;
import org.apache.mesos.Protos.ExecutorInfo;
import org.apache.mesos.Protos.Resource;
import org.apache.mesos.Protos.TaskInfo;
import org.apache.mesos.Protos.Value.Range;
import org.apache.mesos.Protos.Value.Ranges;

import java.util.*;

public class ComposeRewriteHelper {


    private static final String CONTAINER_NAME = "container_name";
    private static final String NETWORK = "network_mode";
    private static final String LINKS = "links";
    private static final String DEPENDS_ON = "depends_on";
    private static final String VOLUMES_FROM = "volumes_from";
    private static final String PORTS = "ports";
    private static final String LABELS = "labels";
    private static final String SERVICES = "services";

    private static final Logger log = Logger.getLogger(ComposeRewriteHelper.class);

    public Map<String, Map<String, Map<String, Object>>> updateYaml(Map<String, Map<String, Map<String, Object>>> yamlMap,
                                                                    TaskInfo taskInfo,
                                                                    ExecutorInfo executorInfo) {

        log.debug(" ############ STARTING updateYaml  ################");
        if (yamlMap == null || yamlMap.isEmpty()) {
            return null;
        }
        Map<String, Map<String, Map<String, Object>>> resultantContainerMap = new HashMap<String, Map<String, Map<String, Object>>>();
        resultantContainerMap.putAll(yamlMap);
        String taskId = taskInfo.getTaskId().getValue();

        Iterator<Long> portIterator = getPortMappingIterator(taskInfo);
        String executorId = executorInfo.getExecutorId().getValue();
        log.debug(" executorId: " + executorId);
        Map<String, Map<String, Object>> services = yamlMap.get(SERVICES);
        log.debug(" services map: " + services.toString());
        Map<String, Map<String, Object>> resultantServicesMap = new HashMap<String, Map<String, Object>>();
        for (Map.Entry<String, Map<String, Object>> containerEntry : services.entrySet()) {

            String key = containerEntry.getKey();
            Map<String, Object> containerValue = containerEntry.getValue();
            Map<String, Object> updatedContainerValues = updateContainerValue(executorId,
                    taskInfo,
                    containerValue,
                    portIterator);
            String updatedKey = prefixTaskId(taskId, key);
            resultantServicesMap.put(updatedKey, updatedContainerValues);
        }
        resultantContainerMap.put(SERVICES, resultantServicesMap);
        return resultantContainerMap;
    }


    private Map<String, Object> updateContainerValue(String executorId,
                                                     TaskInfo taskInfo,
                                                     Map<String, Object> containerDetails,
                                                     Iterator<Long> portIterator) {

        String taskId = taskInfo.getTaskId().getValue();

        if (log.isDebugEnabled()) {
            log.debug(" ##################  Starting updateContainerValue   ############### ");
            log.debug(" executorId: " + executorId + "  taskId: " + taskId + " containerDetails: " + containerDetails.toString() + " portIterator: " + portIterator.toString());
        }

        if (containerDetails.containsKey(CONTAINER_NAME)) {
            String containerValue = prefixTaskId(taskId, String.valueOf(containerDetails.get(CONTAINER_NAME)));
            log.debug("updated ContainerName: " + containerValue);
            containerDetails.put(CONTAINER_NAME, containerValue);
        }

        Object networkValue = containerDetails.get(NETWORK);
        if (networkValue != null && (String.valueOf(networkValue).contains("service"))) {
            String networkValueString = String.valueOf(networkValue);
            String[] split = networkValueString.split(":");
            String containerName = split[split.length - 1];
            log.debug("updated network: " + "service:" + prefixTaskId(taskId, containerName));
            containerDetails.put(NETWORK, "service:" + prefixTaskId(taskId, containerName));
        }

        Object linkValues = containerDetails.get(LINKS);
        if (linkValues != null) {
            log.debug("links present");
            List<String> updatedLinks = new ArrayList<String>();
            List<?> links = (List<?>) linkValues;
            for (Object iter : links) {
                String link = (String) iter;
                updatedLinks.add(prefixTaskId(taskId, link) + ":" + link);
                log.debug(prefixTaskId(taskId, link)+ ":" + link);
            }
            log.debug(" updatedLinks: " + updatedLinks);
            containerDetails.put(LINKS, updatedLinks);
        }

        Object dependson = containerDetails.get(DEPENDS_ON);
        if (dependson != null) {
            List<String> updatedDependsOn = new ArrayList<String>();
            List<?> dependsOnValues = (List<?>) dependson;
            for (Object o : dependsOnValues) {
                String dependsOn = (String) o;
                updatedDependsOn.add(prefixTaskId(taskId, dependsOn));
            }
            log.debug("updated DependsOn: " + updatedDependsOn);
            containerDetails.put(DEPENDS_ON, updatedDependsOn);
        }

        Object volumesFromValues = containerDetails.get(VOLUMES_FROM);
        if (volumesFromValues != null) {
            List<String> updatedVolumesFrom = new ArrayList<String>();
            List<?> volumesFrom = (List<?>) volumesFromValues;

            for (Object o : volumesFrom) {
                String volume = (String) o;
                updatedVolumesFrom.add(prefixTaskId(taskId, volume));
            }
            log.debug(" updated Volumes: " + updatedVolumesFrom);
            containerDetails.put(VOLUMES_FROM, updatedVolumesFrom);
        }

        Object portMappings = containerDetails.get(PORTS);
        if (portMappings != null) {
            List<String> updatedPorts = new ArrayList<String>();
            List<?> ports = (List<?>) portMappings;
            for (Object o : ports) {
                String port = (String) o;
                String replacedPort = replacePort(port, portIterator);
                updatedPorts.add(replacedPort);
            }
            log.debug(" updatedPorts: " + updatedPorts);
            containerDetails.put(PORTS, updatedPorts);
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> taskIdLabel = (Map<String, Object>) containerDetails.get(LABELS);
        if (taskIdLabel == null) {
            taskIdLabel = new HashMap<String, Object>();
        }
        taskIdLabel.put("taskId", taskId);
        taskIdLabel.put("executorId", executorId);
        for (Protos.Label l : taskInfo.getLabels().getLabelsList()) {
            taskIdLabel.put(l.getKey(), l.getValue());
        }

        log.debug(" updated taskIdLabel: " + taskIdLabel);
        containerDetails.put(LABELS, taskIdLabel);

        return containerDetails;
    }

    private String replacePort(String portString, Iterator<Long> portIterator) {
        if (portIterator.hasNext()) {
            String[] tokens = portString.split(":");
            if (tokens.length > 1) {
                return portIterator.next() + ":" + tokens[1];
            } else {
                throw new IllegalFormatFlagsException("port mappings in docker-compose file not valid");
            }
        } else {
            throw new NoSuchElementException("Insufficient number of ports allocated");
        }
    }

    private Iterator<Long> getPortMappingIterator(TaskInfo taskInfo) {
        List<Resource> list = taskInfo.getResourcesList();
        List<Long> ports = new ArrayList<Long>();
        for (Resource resource : list) {
            String name = resource.getName();
            if ("ports".equals(name)) {
                Ranges ranges = resource.getRanges();
                for (Range range : ranges.getRangeList()) {
                    long startPort = range.getBegin();
                    long endPort = range.getEnd();
                    for (int i = 0; i <= endPort - startPort; i++) {
                        ports.add(startPort + i);
                    }
                }
            }
        }
        return ports.iterator();
    }

    private String prefixTaskId(String taskId, String key) {
        StringBuilder builder = new StringBuilder();
        builder.append(key.toString()).append("-").append(taskId);
        String newId = builder.toString();

        newId.replaceAll("[^a-zA-Z0-9-]" ,"");
        if (newId.length() > 63)
            newId = newId.substring(0,63);
        return newId;
    }


}
