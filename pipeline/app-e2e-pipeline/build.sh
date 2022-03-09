#!/bin/sh
cd $(git rev-parse --show-toplevel)
cloud-build-local --substitutions _DOCKER_PREFIX=bluetrainsoftware.lan/featurehub,_DOCKER_PASSWORD=$FEATUREHUB_PASSWORD,_DOCKER_USERNAME=$FEATUREHUB_USER  --config=pipeline/app-e2e-pipeline/cloudbuild.yaml --dryrun=false --write-workspace=/tmp/cloud-build .
