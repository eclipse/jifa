#! /bin/bash
# Copyright (c) 2021 Contributors to the Eclipse Foundation
#
# See the NOTICE file(s) distributed with this work for additional
# information regarding copyright ownership.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License 2.0 which is available at
# http://www.eclipse.org/legal/epl-2.0
#
# SPDX-License-Identifier: EPL-2.0
#

WORKER_NAME="worker-1.0"
FRONTEND_NAME="frontend-1.0"
JIFA_PROJECT="$(dirname $( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd ))"
cd $JIFA_PROJECT/artifacts

unzip -o $WORKER_NAME".zip"
mkdir $WORKER_NAME/webroot
cp $FRONTEND_NAME".zip" $WORKER_NAME/webroot
unzip -o $WORKER_NAME/webroot/$FRONTEND_NAME".zip" -d $WORKER_NAME/webroot
# You can specify your worker configuration file if you want to do some customizations
# export WORKER_OPTS=-Djifa.worker.config=/path/to/your/worker-config.json
cd $WORKER_NAME
./bin/worker
