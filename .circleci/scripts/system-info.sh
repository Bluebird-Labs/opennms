#!/usr/bin/env bash

set -eEuo pipefail
trap 's=$?; echo >&2 "$0: Error on line "$LINENO": $BASH_COMMAND"; exit $s' ERR

echo "<<< CPU Info"
cat /proc/cpuinfo
echo ">>> CPU Info"

echo "<<< Mem Info"
cat /proc/meminfo
echo ">>> Mem Info"

echo "<<< ULimits"
ulimit -a
echo ">>> ULimits"

echo "<<< Executing user"
whoami
echo ">>> Executing user"

echo "<<< Environment"
env
echo ">>> Environment"

echo "<<< Disk usage"
df -h
echo ">>> Disk usage"

echo "<<< Java Info"
echo "JAVA_HOME: ${JAVA_HOME}"
java "-XX:+PrintFlagsFinal" -version
echo ">>> Java Info"

echo "<<< Maven Info"
mvn -version
echo ">>> Maven Info"
