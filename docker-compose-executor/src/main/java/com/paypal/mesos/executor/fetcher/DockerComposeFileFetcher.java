package com.paypal.mesos.executor.fetcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.apache.mesos.Protos.Label;
import org.apache.mesos.Protos.Labels;
import org.apache.mesos.Protos.TaskInfo;
import org.yaml.snakeyaml.Yaml;


public class DockerComposeFileFetcher implements FileFetcher{

	private static final Logger log = Logger.getLogger(DockerComposeFileFetcher.class);
	
	private static final String GENERATED_YAML_FILE_NAME = "docker-compose$generated.yml";

	private DockerRewriteHelper helper;

	private Yaml yaml;

	@Inject
	public DockerComposeFileFetcher(DockerRewriteHelper helper,Yaml yaml) {
		this.helper = helper;
		this.yaml = yaml;
	}

	@Override
	public File getFile(TaskInfo taskInfo) throws FileNotFoundException,IOException{
		String path = getFileName(taskInfo);
		validateFile(path);
		Map<String,Map<String,Object>> rootYaml = readFromFile(path);
		Map<String,Map<String,Object>> updatedYaml = helper.updateYaml(rootYaml,taskInfo);
		String outputFileName = getOutputFileName(path);
		return writeToFile(outputFileName,updatedYaml);
	}

	
	
	private String getOutputFileName(String path){
		if(path != null && path.split("/").length > 1){
			String [] tokens = path.split("/");
			String result = tokens[0];
			for(int i=1;i<tokens.length-1;i++){
				result = result +"/" +tokens[i];
			}
			return result+"/"+GENERATED_YAML_FILE_NAME;
		}else{
			return GENERATED_YAML_FILE_NAME;
		}
	}

	//TODO figure out a way to lookup a map instead of iteration
	private String getFileName(TaskInfo taskInfo){
		Labels labels = taskInfo.getLabels();
		for(Label label:labels.getLabelsList()){
			if("fileName".equals(label.getKey())){
				return label.getValue();
			}
		}
		log.warn("error reading fileName from taskInfo");
		return null;
	}

	private File writeToFile(String fileName,Map<String,Map<String,Object>> updatedRootYaml) throws IOException,FileNotFoundException{
		log.info("fileName is:"+fileName);
		File file = new File(fileName);
		FileWriter fileWriter = new FileWriter(file);
		yaml.dump(updatedRootYaml,fileWriter);
		fileWriter.flush();
		fileWriter.close();
		return file;
	}

	private Map<String,Map<String,Object>> readFromFile(String path) throws FileNotFoundException,IOException{
		FileReader fileReader = new FileReader(new File(path));
		@SuppressWarnings("unchecked")
		Map<String,Map<String,Object>>yamlMap = (Map<String,Map<String,Object>>)yaml.load(fileReader);
		fileReader.close();
		return yamlMap;
	}


	private void validateFile(String path) throws FileNotFoundException{
		if(path == null || path.length() == 0 || !fileExists(path)){
			throw new FileNotFoundException("No .yml/.yaml file found @"+path);
		}
	}

	private boolean fileExists(String path){
		File file = new File(path);
		String fileName = file.getName();
		return file.isFile() && (fileName.endsWith(".yml") || fileName.endsWith(".yaml"));
	}

}

