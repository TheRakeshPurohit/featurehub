#!/bin/sh
cloud-build-local --substitutions _DOCKER_PREFIX=bluetrainsoftware.lan:5000/featurehub,_DOCKERHUB_PASSWORD=$FEATUREHUB_PASSWORD  --config=pipeline/app-e2e-pipeline/cloudbuild.yaml --dryrun=false --write-workspace=/tmp/cloud-build .
