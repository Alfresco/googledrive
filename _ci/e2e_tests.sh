#!/usr/bin/env bash

echo "=========================== Starting End-to-End Tests Script ==========================="
PS4="\[\e[35m\]+ \[\e[m\]"
set -vex
pushd "$(dirname "${BASH_SOURCE[0]}")/../"


mvn -B -U clean install -Plocal \
 -DbuildNumber=$GITHUB_RUN_NUMBER \
 -DskipTests

mvn -B -U clean verify -Pdocker-end-to-end-setup -pl 'alfresco-googledrive-end-to-end-tests'

popd
set +vex
echo "=========================== Finishing End-to-End Tests Script =========================="