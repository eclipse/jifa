#! /bin/bash

WORKER_NAME="worker-1.0"
FRONTEND_NAME="frontend-1.0"
JIFA_PROJECT="$(dirname $( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd ))"
cd $JIFA_PROJECT/deploy

unzip -o $WORKER_NAME".zip"
mkdir $WORKER_NAME/webroot
cp $FRONTEND_NAME".zip" $WORKER_NAME/webroot
unzip -o $WORKER_NAME/webroot/$FRONTEND_NAME".zip" -d $WORKER_NAME/webroot
cd $WORKER_NAME
./bin/worker
