#!/bin/sh
# Copyright (c) 2023 Contributors to the Eclipse Foundation
#
# See the NOTICE file(s) distributed with this work for additional
# information regarding copyright ownership.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License 2.0 which is available at
# http://www.eclipse.org/legal/epl-2.0
#
# SPDX-License-Identifier: EPL-2.0

set -eu

#TAG="latest"
TAG=0.2.0-SNAPSHOT
PORT="8102"

check_docker() {
  if ! command -v docker &>/dev/null; then
    echo "docker is not installed"
    exit 1
  fi
}

run_jifa() {
  check_docker
  docker run -p ${PORT}:8102 eclipsejifa/jifa:${TAG}
}

while [ $# -gt 0 ]; do
  case $1 in
  -t)
    TAG=$2
    shift
    ;;
  -p)
    PORT=$2
    shift
    ;;
  esac
  shift
done

run_jifa
