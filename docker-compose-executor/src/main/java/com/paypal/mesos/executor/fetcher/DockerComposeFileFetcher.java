package com.paypal.mesos.executor.fetcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.mesos.Protos.TaskInfo;
import org.yaml.snakeyaml.Yaml;


public class DockerComposeFileFetcher implements FileFetcher{


	private static final String GENERATED_YAML_FILE_NAME = "docker-compose$generated.yml";

	private DockerRewriteHelper helper;

	private Yaml yaml;

	@Inject
    public DockerComposeFileFetcher(DockerRewriteHelper helper,Yaml yaml) {
		this.helper = helper;
		this.yaml = yaml;
	}
	
	//TODO decide how to get file path from taskInfo
	@Override
	public File getFile(TaskInfo taskInfo) throws FileNotFoundException,IOException{
		String path = "/Users/tgadiraju/work/hackprep/docker-compose-example/docker-compose.yml";
		validateFile(path);
		Map<String,Map<String,Object>> rootYaml = readFromFile(path);
		Map<String,Map<String,Object>> updatedYaml = helper.updateYaml(rootYaml,taskInfo);
		return writeToFile(updatedYaml);
	}

	private File writeToFile(Map<String,Map<String,Object>> updatedRootYaml) throws IOException,FileNotFoundException{
		File file = new File(GENERATED_YAML_FILE_NAME);
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
		if(StringUtils.isBlank(path) || !fileExists(path)){
			throw new FileNotFoundException("No .yml/.yaml file found @"+path);
		}
	}

	private boolean fileExists(String path){
		File file = new File(path);
		String fileName = file.getName();
		return file.isFile() && (fileName.endsWith(".yml") || fileName.endsWith(".yaml"));
	}

}

