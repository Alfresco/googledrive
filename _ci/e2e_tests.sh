#!/usr/bin/env bash

echo "=========================== Starting End-to-End Tests Script ==========================="
PS4="\[\e[35m\]+ \[\e[m\]"
set -vex
pushd "$(dirname "${BASH_SOURCE[0]}")/../"

# VARIANT=bundled builds the self-contained AMP (adds the -Pbundled profile); default is zero-dependency.
BUILD_PROFILES="local"
if [ "$VARIANT" = "bundled" ]; then
  BUILD_PROFILES="bundled,local"
fi

mvn -B -U clean install -P${BUILD_PROFILES} \
 -DbuildNumber=$GITHUB_RUN_NUMBER \
 ${ACS_VERSION:+-Dacs.version=$ACS_VERSION} \
 -DskipTests

mvn -B -U clean verify -Pdocker-end-to-end-setup -pl 'alfresco-googledrive-end-to-end-tests' -Ddocker.keepContainer=true

popd
set +vex
echo "=========================== Finishing End-to-End Tests Script =========================="