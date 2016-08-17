PROJECT_HOME_DIR='/home/vagrant/marathon/'
PROJECT_VERSION=`mvn -f $PROJECT_HOME_DIR/pom.xml org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version | sed -n -e '/^\[.*\]/ !{ /^[0-9]/ { p; q } }'`
COMPOSE_JAR_NAME=/home/vagrant/marathon/target/docker-compose-executor_$PROJECT_VERSION.jar
java -jar ${COMPOSE_JAR_NAME}
