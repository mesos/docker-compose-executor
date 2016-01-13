/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <mesos/hook.hpp>
#include <mesos/mesos.hpp>
#include <mesos/module.hpp>

#include <mesos/module/hook.hpp>

#include <process/future.hpp>
#include <process/process.hpp>
#include <process/protobuf.hpp>

#include <stout/foreach.hpp>
#include <stout/os.hpp>
#include <stout/try.hpp>

using namespace mesos;

using process::Future;

// Must be kept in sync with variables of the same name in
// tests/hook_tests.cpp.
const char* testLabelKey = "MESOS_Test_Label";
const char* testLabelValue = "ApacheMesos";
const char* testRemoveLabelKey = "MESOS_Test_Remove_Label";

class DockerComposeHook : public Hook
{
public:
  virtual Result<Labels> masterLaunchTaskLabelDecorator(
      const TaskInfo& taskInfo,
      const FrameworkInfo& frameworkInfo,
      const SlaveInfo& slaveInfo)
  {
    LOG(INFO) << "Executing 'masterLaunchTaskLabelDecorator' hook";

    Labels labels;
    return labels;
  }

  virtual Result<Labels> slaveRunTaskLabelDecorator(
      const TaskInfo& taskInfo,
      const ExecutorInfo& executorInfo,
      const FrameworkInfo& frameworkInfo,
      const SlaveInfo& slaveInfo)
  {
    LOG(INFO) << "Executing 'slaveRunTaskLabelDecorator' hook";

    Labels labels;
    return labels;
  }

  
  virtual Result<Environment> slaveExecutorEnvironmentDecorator(
      const ExecutorInfo& executorInfo)
  {
    LOG(INFO) << "Executing 'slaveExecutorEnvironmentDecorator' hook";

    Environment environment;
    return environment;
  }

  // This hook is called when the executor is being removed.
  virtual Try<Nothing> slaveRemoveExecutorHook(
      const FrameworkInfo& frameworkInfo,
      const ExecutorInfo& executorInfo)
  {
    LOG(INFO) << "Executing 'slaveRemoveExecutorHook'";
    std::string executorId = executorInfo.executor_id().value();
    std::string containerIdCommand = "docker ps --filter=\"label=executorId="+executorId+"\" -q";
    char* command = new char[containerIdCommand.length()+1];
    std::strcpy(command,containerIdCommand.c_str());
    std::list<std::string> result = exec(command);
    std::list<std::string>::iterator it;
    for(it = result.begin();it != result.end();++it){
        stopContainer(*it);
        removeContainer(*it);
    }
    return Nothing();
  }

  int stopContainer(std::string containerId){
    std::string command = "docker stop "+containerId;
    return system(command.c_str());
  }

  int removeContainer(std::string containerId){
     std::string command = "docker rm "+containerId;
     return system(command.c_str());
  }

  std::list<std::string> exec(const char* cmd) {
     FILE* fpipe;
     char containerId[128];
     std::list<std::string> result;
     if(0 == (fpipe = (FILE*)popen(cmd,"r"))){
        LOG(INFO) << "Failed to open";
        return NULL;
     }
     while(fgets(containerId,sizeof(containerId),fpipe) != NULL){
         result.push_back(containerId);
     }
     return result;
  }

  virtual Result<Labels> slaveTaskStatusLabelDecorator(
      const FrameworkID& frameworkId,
      const TaskStatus& status)
  {
    LOG(INFO) << "Executing 'slaveTaskStatusLabelDecorator' hook";

    Labels labels;
    return labels;
  }
};


static Hook* createHook(const Parameters& parameters)
{
  return new DockerComposeHook();
}


// Declares a Hook module named 'org_apache_mesos_TestHook'.
mesos::modules::Module<Hook> org_apache_mesos_DockerComposeHook(
    MESOS_MODULE_API_VERSION,
    MESOS_VERSION,
    "Apache Mesos",
    "modules@mesos.apache.org",
    "DockerComposeHook  module.",
    NULL,
    createHook);
