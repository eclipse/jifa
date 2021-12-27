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

set -e
cd ..
rm -rf deploy
./gradlew buildJifa -x test
tar caf jifa.tgz deploy
mv jifa.tgz cloud/
cd cloud

docker build -t jifa-master-open "." -f Dockerfile_master
docker build -t jifa-worker-open "." -f Dockerfile_worker
