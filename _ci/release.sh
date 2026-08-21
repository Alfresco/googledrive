#!/usr/bin/env bash

echo "=========================== Starting Release Script ==========================="
PS4="\[\e[35m\]+ \[\e[m\]"
set -vex
pushd "$(dirname "${BASH_SOURCE[0]}")/../"

: "${RELEASE_VERSION:?RELEASE_VERSION must be set (see .github/workflows/build.yml env)}"

RELEASE_CHECKOUT_DIR="target/checkout"

mvn -B -f "${RELEASE_CHECKOUT_DIR}/pom.xml" \
    -pl alfresco-googledrive-repo-community,alfresco-googledrive-repo-enterprise -am \
    -Pbundled -DskipTests -DbuildNumber=$GITHUB_RUN_NUMBER \
    clean package

COMMUNITY_BUNDLED_AMP="alfresco-googledrive-repo-community-${RELEASE_VERSION}+bundled.amp"
ENTERPRISE_BUNDLED_AMP="alfresco-googledrive-repo-enterprise-${RELEASE_VERSION}+bundled.amp"

COMMUNITY_BUNDLED_SOURCES="alfresco-googledrive-repo-community-${RELEASE_VERSION}+bundled-sources.jar"
ENTERPRISE_BUNDLED_SOURCES="alfresco-googledrive-repo-enterprise-${RELEASE_VERSION}+bundled-sources.jar"

mvn -B deploy:deploy-file \
    -DgroupId=org.alfresco.integrations \
    -DartifactId=alfresco-googledrive-repo-community \
    -Dversion="${RELEASE_VERSION}+bundled" \
    -Dpackaging=amp \
    -Dfile="${RELEASE_CHECKOUT_DIR}/alfresco-googledrive-repo-community/target/${COMMUNITY_BUNDLED_AMP}" \
    -Dsources="${RELEASE_CHECKOUT_DIR}/alfresco-googledrive-repo-community/target/${COMMUNITY_BUNDLED_SOURCES}" \
    -DrepositoryId=alfresco-public \
    -Durl=https://artifacts.alfresco.com/nexus/content/repositories/releases

mvn -B deploy:deploy-file \
    -DgroupId=org.alfresco.integrations \
    -DartifactId=alfresco-googledrive-repo-enterprise \
    -Dversion="${RELEASE_VERSION}+bundled" \
    -Dpackaging=amp \
    -Dfile="${RELEASE_CHECKOUT_DIR}/alfresco-googledrive-repo-enterprise/target/${ENTERPRISE_BUNDLED_AMP}" \
    -Dsources="${RELEASE_CHECKOUT_DIR}/alfresco-googledrive-repo-enterprise/target/${ENTERPRISE_BUNDLED_SOURCES}" \
    -DrepositoryId=alfresco-enterprise-releases \
    -Durl=https://artifacts.alfresco.com/nexus/content/repositories/enterprise-releases

cp "${RELEASE_CHECKOUT_DIR}/alfresco-googledrive-repo-community/target/${COMMUNITY_BUNDLED_AMP}"   "alfresco-googledrive-repo-community/target/"
cp "${RELEASE_CHECKOUT_DIR}/alfresco-googledrive-repo-enterprise/target/${ENTERPRISE_BUNDLED_AMP}" "alfresco-googledrive-repo-enterprise/target/"

popd
set +vex
echo "=========================== Finishing Release Script =========================="
