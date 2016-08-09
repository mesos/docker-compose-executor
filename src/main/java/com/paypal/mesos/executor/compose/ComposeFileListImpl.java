package com.paypal.mesos.executor.compose;

import org.apache.log4j.Logger;
import org.apache.mesos.Protos.Label;
import org.apache.mesos.Protos.Labels;
import org.apache.mesos.Protos.TaskInfo;

import javax.inject.Inject;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;


public class ComposeFileListImpl implements ComposeFileList {

    private static final Logger log = Logger.getLogger(ComposeFileListImpl.class);
    private static final String FILE_DELIMITER = ",";


    @Inject
    public ComposeFileListImpl() {
    }

    @Override
    public List<String> getFile(TaskInfo taskInfo) throws FileNotFoundException, IOException {

        List<String> paths = getFileName(taskInfo);
        if (log.isDebugEnabled())
            log.debug("############## ComposeFileList.getFile, paths: ##############" + paths);
        validateFiles(paths);

        return paths;
    }


    //TODO figure out a way to lookup a map instead of iteration
    private List<String> getFileName(TaskInfo taskInfo) {

        Labels labels = taskInfo.getLabels();
        for (Label label : labels.getLabelsList()) {
            if ("fileName".equals(label.getKey())) {
                List<String> files = Arrays.asList(label.getValue().split(FILE_DELIMITER));
                for (int i = 0; i < files.size(); i++)
                    files.set(i, files.get(i).trim());
                return files;
            }
        }
        log.warn("error reading fileName from taskInfo");
        return null;
    }


    private void validateFiles(List<String> paths) throws FileNotFoundException {
        if (paths == null || paths.size() == 0) {
            log.error("empty .yml/.yaml file list @");
            throw new FileNotFoundException("empty .yml/.yaml file list @");
        }
        for (String name : paths) {
            File file = new File(name);
            if (!file.isFile() || !(name.endsWith(".yml") || name.endsWith(".yaml"))) {
                log.error("No .yml/.yaml file found @" + name);
                throw new FileNotFoundException("No .yml/.yaml file found @" + name);
            }
        }
        return;
    }

    private boolean fileExists(List<String> paths) {
        for (String name : paths) {
            File file = new File(name);
            String fileName = file.getName();
            return file.isFile() && (fileName.endsWith(".yml") || fileName.endsWith(".yaml"));
        }
        return true;
    }
}

