#!/bin/sh
# make sure you have built all artifacts first
if [ $# -eq 0 ]
  then
    VERSION=`cat ../current-rc.txt`
else
  VERSION=$1
fi
if [[ $VERSION != *"RC"* ]]; then
  echo "Is RC, not also tagging latest"
else
  echo "Is not RC, tagging latest"
  BUILD_PARAMS="$BUILD_PARAMS -Djib.to.tags=latest"
fi
export PACKAGES=${PACKAGES:-pom-packages.xml}
DOCKER_PREFIX="${OVERRIDE_DOCKER_PREFIX:-featurehub}"
mvn -f $PACKAGES -DskipTests $BUILD_PARAMS -Ddocker.project.prefix=$DOCKER_PREFIX -Ddocker-cloud-build=true -Dbuild.version=$VERSION clean install

