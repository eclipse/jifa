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

eval $(minikube docker-env)
docker tag $(docker images jifa-master-open -q) jifadocker/jifa-master-open
docker push jifadocker/jifa-master-open

docker tag $(docker images jifa-worker-open -q) jifadocker/jifa-worker-open
docker push jifadocker/jifa-worker-open