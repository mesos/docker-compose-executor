package com.paypal.mesos.executor.compose;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.apache.mesos.Protos.Label;
import org.apache.mesos.Protos.Labels;
import org.apache.mesos.Protos.TaskInfo;


public class ComposeFileListImpl implements ComposeFileList{

	private static final Logger log = Logger.getLogger(ComposeFileListImpl.class);

//	private static final String GENERATED_YAML_FILE_NAME = "-generated.yml";
	private static final String FILE_DELIMITER = ",";
//
//	private ComposeRewriteHelper helper;
//
//	private Yaml yaml;

//	ExecutorInfo info;

//	public ComposeFileListImpl(DockerRewriteHelper helper,Yaml yaml) {
//		this.helper = helper;
//		this.yaml = yaml;
//	}

	@Inject
	public ComposeFileListImpl() {

	}

	@Override
	public List<String> getFile(TaskInfo taskInfo) throws FileNotFoundException,IOException{
		System.out.println("############## STARTING DockerComposeFileFetcher.getFile ##############");

		List<String> paths = getFileName(taskInfo);
		System.out.println(" paths: " + paths);
		validateFiles(paths);

		return paths;
		// Refactored code .. move yaml update logic in a method..
//		return updateYamlFiles(paths, executorInfo, taskInfo);
	}

//	private List<String> updateYamlFiles(List<String> paths, ExecutorInfo executorInfo,TaskInfo taskInfo) throws IOException {
//		List<String> newPath = new ArrayList<>();
//		for (String file: paths) {
//			Map<String,Map<String,Map<String,Object>>> rootYaml = readFromFile(file);
//			System.out.println(" rootYaml dump: " + rootYaml.toString());
//			Map<String,Map<String,Map<String,Object>>> updatedYaml = helper.updateYaml(rootYaml, taskInfo, executorInfo);
//			System.out.println(" updatedYaml dump: "+updatedYaml.toString());
//			String outputFileName = getOutputFileName(file);
//			System.out.println("outputFileName: "+outputFileName);
//			writeToFile(outputFileName, updatedYaml);
//			newPath.add(outputFileName);
//		}
//		return newPath;
//	}

/*	private String getOutputFileName(String path){
		if(path != null ){
			StringBuffer buf = new StringBuffer(path.trim());
			buf.append(GENERATED_YAML_FILE_NAME);
			return  buf.toString();
		}
		return null;
	}*/

	//TODO figure out a way to lookup a map instead of iteration
	private List<String> getFileName(TaskInfo taskInfo){

		Labels labels = taskInfo.getLabels();
		for(Label label:labels.getLabelsList()){
			if("fileName".equals(label.getKey())){
				return Arrays.asList(label.getValue().split(FILE_DELIMITER));
			}
		}
		log.warn("error reading fileName from taskInfo");
		return null;
	}

/*
	private File writeToFile(String fileName,Map<String,Map<String,Map<String,Object>>> updatedRootYaml) throws IOException,FileNotFoundException{
		File file = new File(fileName);
		FileWriter fileWriter = new FileWriter(file);
		yaml.dump(updatedRootYaml,fileWriter);
		fileWriter.flush();
		fileWriter.close();
		return file;
	}

	private Map<String,Map<String,Map<String,Object>>> readFromFile(String path) throws FileNotFoundException,IOException{
		FileReader fileReader = new FileReader(new File(path));
		@SuppressWarnings("unchecked")
		Map<String,Map<String,Map<String,Object>>> yamlMap = (Map<String,Map<String,Map<String,Object>>>)yaml.load(fileReader);
		fileReader.close();
		return yamlMap;
	}
*/


	private void validateFiles(List<String> paths) throws FileNotFoundException{
		System.out.println(" ########### validateFiles ###########");
		System.out.println(" paths: "+paths.toString());
		if(paths == null || paths.size() == 0 ){
			throw new FileNotFoundException("empty .yml/.yaml file list @");
		}
		for (String name: paths) {
			File file = new File(name);
			if (!file.isFile() ||  !(name.endsWith(".yml") || name.endsWith(".yaml"))){
				throw new FileNotFoundException("No .yml/.yaml file found @"+name);
			}
		}
		return;
	}

	private boolean fileExists(List<String> paths){
		for (String name: paths) {
			File file = new File(name);
			String fileName = file.getName();
			return file.isFile() && (fileName.endsWith(".yml") || fileName.endsWith(".yaml"));
		}
		return  true;
	}
}

