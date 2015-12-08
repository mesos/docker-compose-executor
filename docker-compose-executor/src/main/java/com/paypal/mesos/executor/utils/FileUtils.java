package com.paypal.mesos.executor.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

public class FileUtils {

	private static final Logger log = Logger.getLogger(FileUtils.class);

	public static final String PID = "pid";

	public static Map<String,Map<String,Object>> readFromFile(String path) {
		try{
			FileReader fileReader = new FileReader(new File(path));
			@SuppressWarnings("unchecked")
			Map<String,Map<String,Object>>yamlMap = (Map<String,Map<String,Object>>)provideYaml().load(fileReader);
			fileReader.close();
			return yamlMap;
		}catch(FileNotFoundException e){
			log.warn("not able to write to file:"+e);
		}catch(IOException e){
			log.warn("IO Exception:"+e);
		}
		return null;
	}

	private static Yaml provideYaml(){
		DumperOptions options=new DumperOptions();
		options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		return new Yaml(options);
	}
	
	public static File writeToFile(String fileName,Object updatedRootYaml) {
		try{
			File file = new File(fileName);
			if(!file.exists()){
				file.createNewFile();
			}
			FileWriter fileWriter = new FileWriter(file,true);
			Yaml yaml = provideYaml();
			yaml.dump(updatedRootYaml,fileWriter);
			fileWriter.flush();
			fileWriter.close();
			return file;
		}catch(FileNotFoundException e){
			log.warn("not able to write to file:"+fileName,e);
		}catch(IOException e){
			log.warn("IO Exception while writing file:"+fileName,e);
		}
		return null;
	}
	
	public static boolean fileExists(String path){
		if(path == null){
			return false;
		}else{
			File file = new File(path);
			return file.exists();
		}
	}

	public static List<Integer> getPids(String fileName){
		List<Integer> pidList = new ArrayList<Integer>();
		if(fileExists(fileName)){
			Map<String,Map<String,Object>> containerDetailsMap = readFromFile(fileName);
			if(containerDetailsMap != null){
				for(Entry<String,Map<String,Object>> entry:containerDetailsMap.entrySet()){
					Map<String,Object> value = entry.getValue();
					if(value != null && value.containsKey(PID) && ((int)value.get(PID)) != 0){
						pidList.add((int)value.get(PID));
					}
				}
			}
		}
		return pidList; 
	}
}
