#!/usr/bin/env bash

echo "=========================== Starting Release Script ==========================="
PS4="\[\e[35m\]+ \[\e[m\]"
set -vex
pushd "$(dirname "${BASH_SOURCE[0]}")/../"

# Both must match what maven-release-slim was given in the workflow, otherwise the "+bundled"
# coordinates below would not line up with the plain artifacts it just published.
: "${RELEASE_VERSION:?RELEASE_VERSION must be set (see .github/workflows/build.yml env)}"
: "${DEVELOPMENT_VERSION:?DEVELOPMENT_VERSION must be set (see .github/workflows/build.yml env)}"

COMMUNITY_MODULE="alfresco-googledrive-repo-community"
ENTERPRISE_MODULE="alfresco-googledrive-repo-enterprise"

# maven-release-slim leaves the working tree at DEVELOPMENT_VERSION (its last step re-runs
# versions:set) and it commits through the GitHub API, so the local HEAD and tag do not necessarily
# carry the release poms. Re-set the version here instead of checking out the tag, so this build
# does not depend on where the tag landed.
mvn -B -ntp versions:set -DnewVersion="${RELEASE_VERSION}" -DgenerateBackupPoms=false

# The bundled build below runs 'clean', which would delete the plain AMPs that maven-release-slim
# just built and that prepare_staging_deploy.sh links into the S3 payload. Park them and put them
# back afterwards. (The old flow avoided this by building in target/checkout, a separate tree that
# release:perform created; maven-release-slim builds in place.)
#
# The stash MUST live outside the project: '-am' pulls the parent POMs into the reactor, and the
# root project sorts first, so its 'clean' wipes <root>/target before the restore below runs.
PLAIN_AMP_STASH="$(mktemp -d)"
cp "${COMMUNITY_MODULE}/target/${COMMUNITY_MODULE}-${RELEASE_VERSION}.amp"   "${PLAIN_AMP_STASH}/"
cp "${ENTERPRISE_MODULE}/target/${ENTERPRISE_MODULE}-${RELEASE_VERSION}.amp" "${PLAIN_AMP_STASH}/"

# The default AMPs inherit the Drive SDK as 'provided' so they bundle nothing. Additionally build
# the self-contained "+bundled" variants for ACS versions that do not ship the SDK.
mvn -B -pl "${COMMUNITY_MODULE},${ENTERPRISE_MODULE}" -am \
    -Pbundled -DskipTests \
    clean package

COMMUNITY_BUNDLED_AMP="${COMMUNITY_MODULE}-${RELEASE_VERSION}+bundled.amp"
ENTERPRISE_BUNDLED_AMP="${ENTERPRISE_MODULE}-${RELEASE_VERSION}+bundled.amp"

# "+bundled" is a finalName suffix (see the modules' <finalName>), not a Maven classifier, so these
# are published under a distinct version -- which is what prepare_release_deploy.sh later resolves.
# The main deploy never runs for these variants, so attach their sources jar explicitly.
COMMUNITY_BUNDLED_SOURCES="${COMMUNITY_MODULE}-${RELEASE_VERSION}+bundled-sources.jar"
ENTERPRISE_BUNDLED_SOURCES="${ENTERPRISE_MODULE}-${RELEASE_VERSION}+bundled-sources.jar"

mvn -B deploy:deploy-file \
    -DgroupId=org.alfresco.integrations \
    -DartifactId="${COMMUNITY_MODULE}" \
    -Dversion="${RELEASE_VERSION}+bundled" \
    -Dpackaging=amp \
    -Dfile="${COMMUNITY_MODULE}/target/${COMMUNITY_BUNDLED_AMP}" \
    -Dsources="${COMMUNITY_MODULE}/target/${COMMUNITY_BUNDLED_SOURCES}" \
    -DrepositoryId=alfresco-public \
    -Durl=https://artifacts.alfresco.com/nexus/content/repositories/releases

mvn -B deploy:deploy-file \
    -DgroupId=org.alfresco.integrations \
    -DartifactId="${ENTERPRISE_MODULE}" \
    -Dversion="${RELEASE_VERSION}+bundled" \
    -Dpackaging=amp \
    -Dfile="${ENTERPRISE_MODULE}/target/${ENTERPRISE_BUNDLED_AMP}" \
    -Dsources="${ENTERPRISE_MODULE}/target/${ENTERPRISE_BUNDLED_SOURCES}" \
    -DrepositoryId=alfresco-enterprise-releases \
    -Durl=https://artifacts.alfresco.com/nexus/content/repositories/enterprise-releases

# Put the plain AMPs back so prepare_staging_deploy.sh sees both variants side by side.
cp "${PLAIN_AMP_STASH}/${COMMUNITY_MODULE}-${RELEASE_VERSION}.amp"   "${COMMUNITY_MODULE}/target/"
cp "${PLAIN_AMP_STASH}/${ENTERPRISE_MODULE}-${RELEASE_VERSION}.amp" "${ENTERPRISE_MODULE}/target/"
rm -rf "${PLAIN_AMP_STASH}"

# Leave the tree as maven-release-slim left it, so nothing downstream sees the release version.
mvn -B -ntp versions:set -DnewVersion="${DEVELOPMENT_VERSION}" -DgenerateBackupPoms=false

popd
set +vex
echo "=========================== Finishing Release Script =========================="
