#!/usr/bin/env bash

echo "=========================== Starting Build&Test Script ==========================="
PS4="\[\e[35m\]+ \[\e[m\]"
set -vex
pushd "$(dirname "${BASH_SOURCE[0]}")/../"


mvn -B -U -Dbuildnumber=${TRAVIS_BUILD_NUMBER} clean install


popd
set +vex
echo "=========================== Finishing Build&Test Script =========================="

