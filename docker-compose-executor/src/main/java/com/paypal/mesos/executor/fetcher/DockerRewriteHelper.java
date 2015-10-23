package com.paypal.mesos.executor.fetcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IllegalFormatFlagsException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;
import org.apache.mesos.Protos.Resource;
import org.apache.mesos.Protos.TaskInfo;
import org.apache.mesos.Protos.Value.Range;
import org.apache.mesos.Protos.Value.Ranges;

public class DockerRewriteHelper {

	private static final Logger log = Logger.getLogger(DockerRewriteHelper.class);
	
	private static final String CONTAINER_NAME = "container_name";
	private static final String NETWORK = "net";
	private static final String LINKS = "links";
	private static final String VOLUMES_FROM = "volumes_from";
	private static final String PORTS = "ports";
	private static final String LABELS = "labels";
	
	public Map<String,Map<String,Object>> updateYaml(Map<String,Map<String,Object>> yamlMap,TaskInfo taskInfo){
		if(yamlMap == null || yamlMap.isEmpty()){
			return null;
		}
		Map<String,Map<String,Object>> resultantContainerMap = new HashMap<String,Map<String,Object>>();
		String taskId = taskInfo.getTaskId().getValue();
		Iterator<Long> portIterator = getPortMappingIterator(taskInfo);
		
		for(Map.Entry<String, Map<String,Object>> containerEntry:yamlMap.entrySet()){
			String key = containerEntry.getKey();
			Map<String,Object> containerValue = containerEntry.getValue();
			Map<String,Object> updatedContainerValues = updateContainerValue(taskId,containerValue,portIterator);
			String updatedKey = prefixTaskId(taskId, key);
			resultantContainerMap.put(updatedKey,updatedContainerValues);
		}
		return resultantContainerMap;
	}


	private Map<String,Object> updateContainerValue(String taskId,Map<String,Object> containerDetails,Iterator<Long> portIterator){

		if(containerDetails.containsKey(CONTAINER_NAME)){
			String containerValue = prefixTaskId(taskId,String.valueOf(containerDetails.get(CONTAINER_NAME)));
			containerDetails.put(CONTAINER_NAME, containerValue);
		}

		Object networkValue = containerDetails.get(NETWORK);
		if(networkValue !=null && (String.valueOf(networkValue).contains("container"))){
			String networkValueString = String.valueOf(networkValue);
			String [] split = networkValueString.split(":");
			String containerName = split[split.length-1];
			containerDetails.put(NETWORK, "container:"+prefixTaskId(taskId, containerName));	
		}

		Object linkValues = containerDetails.get(LINKS);
		if(linkValues != null){
			List<String> updatedLinks = new ArrayList<String>();
			@SuppressWarnings("unchecked")
			List<String> links = (ArrayList<String>)linkValues;
			for(String link:links){
				updatedLinks.add(prefixTaskId(taskId, link));
			}
			containerDetails.put(LINKS, updatedLinks);
		}

		Object volumesFromValues = containerDetails.get(VOLUMES_FROM);
		if(volumesFromValues != null){
			List<String> updatedVolumesFrom = new ArrayList<String>();
			@SuppressWarnings("unchecked")
			List<String> volumesFrom = (ArrayList<String>)volumesFromValues;
			for(String volumeFrom:volumesFrom){
				updatedVolumesFrom.add(prefixTaskId(taskId, volumeFrom));
			}
			containerDetails.put(VOLUMES_FROM, updatedVolumesFrom);
		}

		Object portMappings = containerDetails.get(PORTS);
		if(portMappings != null){
			List<String> updatedPorts = new ArrayList<String>();
			@SuppressWarnings("unchecked")
			List<String> portStrings = (ArrayList<String>)portMappings;
			for(String portString:portStrings){
				String replacedPort = replacePort(portString,portIterator);
				updatedPorts.add(replacedPort);
			}
			containerDetails.put(PORTS, updatedPorts);
		}
		
		Map<String,String> taskIdLabel = new HashMap<String, String>();
		taskIdLabel.put("taskId", taskId);
		containerDetails.put(LABELS, taskIdLabel);
		
		return containerDetails;
	}

	private String replacePort(String portString,Iterator<Long> portIterator){
		if(portIterator.hasNext()){
			String [] tokens = portString.split(":");
			if(tokens.length > 1){
				return portIterator.next()+":"+tokens[1];
			}else{
				throw new IllegalFormatFlagsException("port mappings in docker-compose file not valid");
			}
		}else{
			throw new NoSuchElementException("Insufficient number of ports allocated");
		}
	}
	
	private Iterator<Long> getPortMappingIterator(TaskInfo taskInfo){
		List<Resource> list = taskInfo.getResourcesList();
		List<Long> ports = new ArrayList<Long>();
		for(Resource resource:list){
			String name = resource.getName();
			if("ports".equals(name)){
				Ranges ranges = resource.getRanges();
				for(Range range:ranges.getRangeList()){
					long startPort = range.getBegin();
					long endPort = range.getEnd();
					for(int i=0;i<=endPort-startPort;i++){
						ports.add(startPort+i);
					}
				}
			}
		}
		return ports.iterator();
	}
	
	private String prefixTaskId(String taskId,String key){
		StringBuilder builder = new StringBuilder();
		return builder.append(taskId).append("_").append(key).toString();
	}
}
