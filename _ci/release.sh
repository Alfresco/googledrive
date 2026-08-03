#!/usr/bin/env bash

echo "=========================== Starting Release Script ==========================="
PS4="\[\e[35m\]+ \[\e[m\]"
set -vex
pushd "$(dirname "${BASH_SOURCE[0]}")/../"

# Github Actions CI runner work on DETACHED HEAD, so we need to checkout the release branch
git checkout -B "${BRANCH_NAME}"

# The workflow (ci.yml) checks out with persist-credentials: false, so git has no stored credentials.
# Point origin at an authenticated URL (bot token) so the pull below works. The maven
# release plugin authenticates its own push/clone separately via -Dusername/-Dpassword.
# Disable xtrace for this single line so the token is not written to the build log.
set +x
git remote set-url origin "https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/${GITHUB_REPOSITORY}.git"
set -x

git pull

# Add email to link commits to user
git config user.email "${GIT_EMAIL}"
git config user.name "${GIT_USERNAME}"

# Run the release plugin - with "[skip ci]" in the release commit message.
mvn -B \
    "-Darguments=-DskipTests -DbuildNumber=$GITHUB_RUN_NUMBER" \
    release:clean release:prepare release:perform \
    -DreleaseVersion="${RELEASE_VERSION}" \
    -DdevelopmentVersion="${DEVELOPMENT_VERSION}" \
    -DautoVersionSubmodules=true \
    -DscmCommentPrefix="[maven-release-plugin][skip ci] " \
    -Dusername="${GIT_USERNAME}" \
    -Dpassword="${GIT_PASSWORD}"

# release:perform produces the standard zero-dependency AMPs (SDK 'provided'). Additionally build and
# publish the self-contained "+bundled" AMP variants (SDK bundled) for ACS versions that do not ship
# the SDK, mirroring alfresco-s3-connector. Built from the freshly tagged sources in target/checkout.
RELEASE_CHECKOUT_DIR="target/checkout"

mvn -B -f "${RELEASE_CHECKOUT_DIR}/pom.xml" \
    -pl alfresco-googledrive-repo-community,alfresco-googledrive-repo-enterprise -am \
    -Pbundled -DskipTests -DbuildNumber=$GITHUB_RUN_NUMBER \
    clean package

COMMUNITY_BUNDLED_AMP="alfresco-googledrive-repo-community-${RELEASE_VERSION}+bundled.amp"
ENTERPRISE_BUNDLED_AMP="alfresco-googledrive-repo-enterprise-${RELEASE_VERSION}+bundled.amp"

mvn -B deploy:deploy-file \
    -DgroupId=org.alfresco.integrations \
    -DartifactId=alfresco-googledrive-repo-community \
    -Dversion="${RELEASE_VERSION}+bundled" \
    -Dpackaging=amp \
    -Dfile="${RELEASE_CHECKOUT_DIR}/alfresco-googledrive-repo-community/target/${COMMUNITY_BUNDLED_AMP}" \
    -DrepositoryId=alfresco-public \
    -Durl=https://artifacts.alfresco.com/nexus/content/repositories/releases

mvn -B deploy:deploy-file \
    -DgroupId=org.alfresco.integrations \
    -DartifactId=alfresco-googledrive-repo-enterprise \
    -Dversion="${RELEASE_VERSION}+bundled" \
    -Dpackaging=amp \
    -Dfile="${RELEASE_CHECKOUT_DIR}/alfresco-googledrive-repo-enterprise/target/${ENTERPRISE_BUNDLED_AMP}" \
    -DrepositoryId=alfresco-enterprise-releases \
    -Durl=https://artifacts.alfresco.com/nexus/content/repositories/enterprise-releases

# Expose the bundled AMPs in the module target dirs so the staging (S3) upload picks them up
cp "${RELEASE_CHECKOUT_DIR}/alfresco-googledrive-repo-community/target/${COMMUNITY_BUNDLED_AMP}"   "alfresco-googledrive-repo-community/target/"
cp "${RELEASE_CHECKOUT_DIR}/alfresco-googledrive-repo-enterprise/target/${ENTERPRISE_BUNDLED_AMP}" "alfresco-googledrive-repo-enterprise/target/"

popd
set +vex
echo "=========================== Finishing Release Script =========================="
