package com.paypal.mesos.executor.fetcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.mesos.Protos.TaskInfo;

public class DockerRewriteHelper {

	private static final String CONTAINER_NAME = "container_name";
	private static final String NETWORK = "net";
	private static final String LINKS = "links";

	public Map<String,Map<String,Object>> updateYaml(Map<String,Map<String,Object>> yamlMap,TaskInfo taskInfo){
		if(yamlMap == null || yamlMap.isEmpty()){
			return null;
		}
		Map<String,Map<String,Object>> resultantContainerMap = new HashMap<String,Map<String,Object>>();
		String taskId = "12345";//taskInfo.getTaskId().getValue();

		for(Map.Entry<String, Map<String,Object>> containerEntry:yamlMap.entrySet()){
			String key = containerEntry.getKey();
			Map<String,Object> containerValue = containerEntry.getValue();
			Map<String,Object> updatedContainerValues = updateContainerValue(taskId,containerValue);
			String updatedKey = prefixTaskId(taskId, key);
			resultantContainerMap.put(updatedKey,updatedContainerValues);
		}
		return resultantContainerMap;
	}

	
	private Map<String,Object> updateContainerValue(String taskId,Map<String,Object> containerDetails){

		if(containerDetails.containsKey(CONTAINER_NAME)){
			String containerValue = taskId+"_"+String.valueOf(containerDetails.get(CONTAINER_NAME));
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

		return containerDetails;
	}
	
	private String prefixTaskId(String taskId,String key){
		StringBuilder builder = new StringBuilder();
		return builder.append(taskId).append("_").append(key).toString();
	}
}
