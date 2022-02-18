#!/bin/sh
cd $(git rev-parse --show-toplevel)
export VERSION=`cat current-rc.txt`
cd admin-frontend
mkdir -p target/build_web
cp build/* target
cd target
tar xvf *.tar
sh actual_build.sh

