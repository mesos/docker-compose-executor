package com.paypal.mesos.executor.compose;

import com.paypal.mesos.executor.DockerComposeExecutor;
import com.paypal.mesos.executor.DockerComposeProcessObserver;
import com.paypal.mesos.executor.monitoring.ComposeMonitor;
import org.apache.mesos.Protos;
import org.junit.Assert;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;


/**
 * Created by kkrishna on 8/2/16.
 */
public class ComposeRewriteHelperTest {

    DockerComposeExecutor composeExecutor = new DockerComposeExecutor( new ComposeFileListImpl(),
                                                                        new DockerComposeProcessObserver(),
                                                                        new ComposeMonitor(),
                                                                        new ComposeRewriteHelper() );

    @org.junit.Test
    public void testTransformYaml() {

     /* resources {
        name: "ports"
        type: RANGES
        ranges {
            range {
                begin:
                31227
                end:
                31229
            }
        }
        role:
        "*"
      }*/
        Protos.TaskInfo info = Protos.TaskInfo.newBuilder()
                .setTaskId(Protos.TaskID.newBuilder().setValue("test.b8b72bb1-5901-11e6-af2b-0242a9458e21"))
                .setLabels(Protos.Labels.newBuilder().addLabels(Protos.Label.newBuilder().setKey("key1").setValue("value1")))
                .addResources(Protos.Resource.newBuilder().setName("ports").setType(Protos.Value.Type.valueOf(1))
                        .setRanges(Protos.Value.Ranges.newBuilder().addRange(Protos.Value.Range.newBuilder().setBegin(31227L).setEnd(31230L))))
                .setSlaveId(Protos.SlaveID.newBuilder().setValue("slave-id-b8b72bb1-5901-11e6-af2b-0242a9458e21"))
                .buildPartial();
        System.out.println("taskInfo: " + info.toString());

        Protos.ExecutorInfo executorInfo = Protos.ExecutorInfo.newBuilder().
                setExecutorId(Protos.ExecutorID.newBuilder().setValue("test.b0b5d762-590e-11e6-af2b-0242a9458e21"))
                .buildPartial();
        System.out.println("executorInfo: " + executorInfo.toString());
        List<String> files = Arrays.asList("src/test/resources/docker-compose.yml");

        try {
            List<String> updatedFiles = composeExecutor.updateYamlFiles(files, executorInfo, info);
            Assert.assertEquals(files.size(), updatedFiles.size());
            System.out.println("updated files: " + updatedFiles.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}

