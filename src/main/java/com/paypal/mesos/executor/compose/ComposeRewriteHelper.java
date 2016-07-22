package com.paypal.mesos.executor.compose;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IllegalFormatFlagsException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.mesos.Protos;
import org.apache.mesos.Protos.ExecutorInfo;
import org.apache.mesos.Protos.Resource;
import org.apache.mesos.Protos.TaskInfo;
import org.apache.mesos.Protos.Value.Range;
import org.apache.mesos.Protos.Value.Ranges;

public class ComposeRewriteHelper {

	
	private static final String CONTAINER_NAME = "container_name";
	private static final String NETWORK = "network_mode";
	private static final String LINKS = "links";
	private static final String DEPENDS_ON = "depends_on";
	private static final String VOLUMES_FROM = "volumes_from";
	private static final String PORTS = "ports";
	private static final String LABELS = "labels";
	private static final String SERVICES = "services";
	
	public Map<String,Map<String,Map<String,Object>>> updateYaml(Map<String,Map<String,Map<String,Object>>> yamlMap,
																 TaskInfo taskInfo,
																 ExecutorInfo executorInfo){

		System.out.println(" ############ STARTING updateYaml  ################");
		if(yamlMap == null || yamlMap.isEmpty()){
			return null;
		}
		Map<String,Map<String,Map<String,Object>>> resultantContainerMap = new HashMap<String,Map<String,Map<String,Object>>>();
		resultantContainerMap.putAll(yamlMap);
		String taskId = taskInfo.getTaskId().getValue();
		System.out.println(" taskId: " + taskId);

		Iterator<Long> portIterator = getPortMappingIterator(taskInfo);
		// System.out.println(" portIterator: " + portIterator.toString());// this doesnt help
		String executorId = executorInfo.getExecutorId().getValue();
		System.out.println(" executorId: " + executorId);
		Map<String,Map<String,Object>> services = yamlMap.get(SERVICES);
		System.out.println(" services map: " + services.toString());
		Map<String,Map<String,Object>> resultantServicesMap = new HashMap<String,Map<String,Object>>();
		for(Map.Entry<String, Map<String,Object>> containerEntry:services.entrySet()){
			
			String key = containerEntry.getKey();
			Map<String,Object> containerValue = containerEntry.getValue();
			Map<String,Object> updatedContainerValues = updateContainerValue(executorId,
				taskInfo,
				containerValue,
				portIterator);
			String updatedKey = prefixTaskId(taskId, key);
			resultantServicesMap.put(updatedKey,updatedContainerValues);
		}
		resultantContainerMap.put(SERVICES, resultantServicesMap);
		return resultantContainerMap;
	}


	private Map<String,Object> updateContainerValue(String executorId,
													TaskInfo taskInfo,
													Map<String,Object> containerDetails,
													Iterator<Long> portIterator){

		String taskId = taskInfo.getTaskId().getValue();
		System.out.println(" ##################  Starting updateContainerValue   ############### ");
		System.out.println(" executorId: " + executorId + "  taskId: " + taskId + " containerDetails: " + containerDetails.toString() + " portIterator: " + portIterator.toString());

		if(containerDetails.containsKey(CONTAINER_NAME)){
			String containerValue = prefixTaskId(taskId,String.valueOf(containerDetails.get(CONTAINER_NAME)));
			System.out.println("updated ContainerName: "+containerValue);
			containerDetails.put(CONTAINER_NAME, containerValue);
		}

		Object networkValue = containerDetails.get(NETWORK);
		if(networkValue !=null && (String.valueOf(networkValue).contains("service"))){
			String networkValueString = String.valueOf(networkValue);
			String [] split = networkValueString.split(":");
			String containerName = split[split.length-1];
			System.out.println("updated network: " + "service:" + prefixTaskId(taskId, containerName));
			containerDetails.put(NETWORK, "service:"+prefixTaskId(taskId, containerName));	
		}
		System.out.println("In update container values");
		Object linkValues = containerDetails.get(LINKS);
		if(linkValues != null){
			System.out.println("links present");
			List<String> updatedLinks = new ArrayList<String>();
			@SuppressWarnings("unchecked")
			List<String> links = (ArrayList<String>)linkValues;
			for(String link:links){
				updatedLinks.add(prefixTaskId(taskId, link)+":"+link);
				System.out.println(prefixTaskId(taskId, link));
			}
			System.out.println(" updatedLinks: "+updatedLinks);
			containerDetails.put(LINKS, updatedLinks);
		}

		Object dependson = containerDetails.get(DEPENDS_ON);
		if(dependson != null){
			List<String> updatedLinks = new ArrayList<String>();
			@SuppressWarnings("unchecked")
			List<String> links = (ArrayList<String>)dependson;
			for(String link:links){
				updatedLinks.add(prefixTaskId(taskId, link));
			}
			System.out.println("updated Links: "+updatedLinks);
			containerDetails.put(DEPENDS_ON, updatedLinks);
		}
		
		Object volumesFromValues = containerDetails.get(VOLUMES_FROM);
		if(volumesFromValues != null){
			List<String> updatedVolumesFrom = new ArrayList<String>();
			@SuppressWarnings("unchecked")
			List<String> volumesFrom = (ArrayList<String>)volumesFromValues;
			for(String volumeFrom:volumesFrom){
				updatedVolumesFrom.add(prefixTaskId(taskId, volumeFrom));
			}
			System.out.println(" updated Volumes: "+updatedVolumesFrom);
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
			System.out.println(" updatedPorts: "+updatedPorts);
			containerDetails.put(PORTS, updatedPorts);
		}
		
		@SuppressWarnings("unchecked")
		Map<String,String> taskIdLabel = (Map<String,String>)containerDetails.get(LABELS);
		if(taskIdLabel == null){
			taskIdLabel = new HashMap<String,String>();
		}
		taskIdLabel.put("taskId", taskId);
		taskIdLabel.put("executorId",executorId);

		for(Protos.Label l : taskInfo.getLabels().getLabelsList()) {
			taskIdLabel.put(l.getKey(), l.getValue());
		}

		System.out.println(" updated taskIdLabel: "+ taskIdLabel);
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
