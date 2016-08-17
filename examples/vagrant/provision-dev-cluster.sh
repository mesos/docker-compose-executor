#!/bin/bash -ex
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

apt-key adv --keyserver hkp://p80.pool.sks-keyservers.net:80 --recv-keys 58118E89F3A912897C070ADBF76221572C52609D
echo deb https://apt.dockerproject.org/repo ubuntu-trusty main > /etc/apt/sources.list.d/docker.list
apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv E56151BF
echo deb http://repos.mesosphere.com/ubuntu trusty main > /etc/apt/sources.list.d/mesosphere.list

apt-get update -q --fix-missing
apt-get -qy install software-properties-common
add-apt-repository ppa:george-edison55/cmake-3.x
apt-get update -q
apt-cache policy cmake

add-apt-repository ppa:openjdk-r/ppa -y
apt-get update
apt-get -y install \
    bison \
    curl \
    git \
    libapr1-dev \
    libcurl4-nss-dev \
    libsasl2-dev \
    libsvn-dev \
    docker-engine \
    openjdk-8-jdk \
    python-dev \
    zookeeperd \
    python-pip \
    maven \
    build-essential                        \
   autoconf                                \
   automake                                \
   ca-certificates                         \
   gdb                                     \
   wget                                    \
   libcurl4-nss-dev                        \
   libsasl2-dev                            \
   libtool                                 \
   libsvn-dev                              \
   libapr1-dev                             \
   libgoogle-glog-dev                      \
   libboost-dev                            \
   protobuf-compiler                       \
   libprotobuf-dev                         \
   make                                    \
   python                                  \
   python2.7                               \
   libpython-dev                           \
   python-dev                              \
   python-protobuf                         \
   python-setuptools                       \
   unzip                                   \
   --no-install-recommends


update-alternatives --set java /usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java

readonly IP_ADDRESS=192.168.33.7
readonly MESOS_VERSION=0.28.2-2.0.27
readonly MARATHON_VERSION=1.1.2-1.0.482
readonly PROJECT_HOME_DIR='/home/vagrant/marathon'
readonly PROJECT_VERSION=`mvn -f $PROJECT_HOME_DIR/pom.xml org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version | sed -n -e '/^\[.*\]/ !{ /^[0-9]/ { p; q } }'`


function install_mesos {
  apt-get -y install mesos=${MESOS_VERSION}.ubuntu1404
}

function install_marathon {
  apt-get -y install marathon=${MARATHON_VERSION}.ubuntu1404
}

function install_docker_compose {
  pip install docker-compose
}

function get_pom_version {
  mvn -f ${PROJECT_HOME_DIR}/pom.xml org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version | sed -n -e '/^\[.*\]/ !{ /^[0-9]/ { p; q } }'
}

function build_docker_compose_executor {
  mvn -f /home/vagrant/marathon/pom.xml clean package -U
  chmod 777 /home/vagrant/marathon/target/docker-compose-executor_${PROJECT_VERSION}.jar
}

function install_cluster_config {
  mkdir -p /etc/marathon
  ln -sf /home/vagrant/marathon/examples/vagrant/clusters.json /etc/marathon/clusters.json
}

function install_ssh_config {
  cat >> /etc/ssh/ssh_config <<EOF

# Allow local ssh w/out strict host checking
Host *
    StrictHostKeyChecking no
    UserKnownHostsFile /dev/null
EOF
}

function enable_gradle_daemon {
  install -o vagrant -g vagrant -d -m 0755 /home/vagrant/.gradle
  cat > /home/vagrant/.gradle/gradle.properties <<EOF
org.gradle.daemon=true
EOF
  chown vagrant:vagrant /home/vagrant/.gradle/gradle.properties
}

function configure_netrc {
  cat > /home/vagrant/.netrc <<EOF
machine $(hostname -f)
login aurora
password secret
EOF
  chown vagrant:vagrant /home/vagrant/.netrc
}

function sudoless_docker_setup {
  gpasswd -a vagrant docker
  service docker restart
}

function start_services {
  #Executing true on failure to please bash -e in case services are already running
  start zookeeper    || true
  start mesos-master || true
  start mesos-slave  || true
  start marathon || true
}

function prepare_sources {
  cat > /usr/local/bin/update-sources <<EOF
#!/bin/bash
rsync -urzvhl /vagrant/ /home/vagrant/marathon \
    --filter=':- /vagrant/.gitignore' \
    --exclude=.git \
    --delete
# Install/update the upstart configurations.
sudo cp /vagrant/examples/vagrant/upstart/*.conf /etc/init
EOF
  chmod +x /usr/local/bin/update-sources
  update-sources
}

install_mesos
install_marathon
install_docker_compose
prepare_sources
install_marathon
install_cluster_config
install_ssh_config
start_services
enable_gradle_daemon
configure_netrc
sudoless_docker_setup
build_docker_compose_executor
