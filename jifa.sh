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
MOUNTS=""
INPUT_FILES=""
INPUT_FILE_COUNT=0
JVM_OPTIONS=""

check_docker() {
  if ! command -v docker &>/dev/null; then
    echo "docker is not installed"
    exit 1
  fi
}

launch_jifa() {
  check_docker
  docker run --pull=always -e JAVA_TOOL_OPTIONS="$JVM_OPTIONS" -p ${PORT}:${PORT} $MOUNTS eclipsejifa/jifa:${TAG} --jifa.port=${PORT} $INPUT_FILES
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
  --jvm-options)
    JVM_OPTIONS=$2
    shift
    ;;
  *)
    ABSOLUTE_PATH=$(realpath "$1")
    if [ ! -f "$ABSOLUTE_PATH" ]; then
      echo "$1 does not exist or is not a regular file"
      exit 1
    fi

    FILE_NAME=$(basename "$ABSOLUTE_PATH")

    MOUNTS="$MOUNTS -v $ABSOLUTE_PATH:/input-file-$INPUT_FILE_COUNT/$FILE_NAME"
    INPUT_FILES="$INPUT_FILES --jifa.input-files[$INPUT_FILE_COUNT]=/input-file-$INPUT_FILE_COUNT/$FILE_NAME"
    INPUT_FILE_COUNT=$((INPUT_FILE_COUNT+1))
    ;;
  esac
  shift
done

launch_jifa