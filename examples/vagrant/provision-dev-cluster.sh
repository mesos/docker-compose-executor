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

apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys 36A1D7869245C8950F966E92D8576A8BA88D21E9
echo deb https://get.docker.com/ubuntu docker main > /etc/apt/sources.list.d/docker.list

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
    lxc-docker \
    openjdk-8-jdk \
    python-dev \
    zookeeperd \
    python-pip \
    maven \
    build-essential                         \
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

readonly MESOS_VERSION=0.25.0


function install_mesos {
  deb=mesos_${MESOS_VERSION}-0.2.70.ubuntu1404_amd64.deb
  wget -c http://downloads.mesosphere.io/master/ubuntu/14.04/$deb
  dpkg --install $deb
} 

function install_marathon {
  sudo pip install docker-compose
  sudo dpkg --purge marathon
  wget -c https://dl.dropboxusercontent.com/u/26009359/marathon_0.11.1-1.0.432.ubuntu1404_amd64.deb
  sudo dpkg --install  marathon_0.11.1-1.0.432.ubuntu1404_amd64.deb
}

function build_docker_compose_executor {
   sudo mvn -f /home/vagrant/marathon/pom.xml clean package -U
   sudo chmod 777 /home/vagrant/marathon/target/docker-compose-executor-0.0.1-SNAPSHOT-jar-with-dependencies.jar    
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
prepare_sources
install_marathon
install_cluster_config
install_ssh_config
start_services
enable_gradle_daemon
configure_netrc
sudoless_docker_setup
build_docker_compose_executor
