#!/usr/bin/env bash

echo "========================== Starting Prepare Release Deploy Script ==========================="
PS4="\[\e[35m\]+ \[\e[m\]"
set -vex
pushd "$(dirname "${BASH_SOURCE[0]}")/../"

: "${RELEASE_VERSION:?RELEASE_VERSION must be set (see .github/workflows/build.yml env)}"

mkdir -p deploy_dir_community deploy_dir_enterprise

export VERSION="${RELEASE_VERSION}"

mvn -B org.apache.maven.plugins:maven-dependency-plugin:3.1.1:copy \
    -Dartifact=org.alfresco.integrations:alfresco-googledrive-repo-community:${VERSION}:amp \
    -DoutputDirectory=deploy_dir_community
mvn -B org.apache.maven.plugins:maven-dependency-plugin:3.1.1:copy \
    -Dartifact=org.alfresco.integrations:alfresco-googledrive-repo-enterprise:${VERSION}:amp \
    -DoutputDirectory=deploy_dir_enterprise
mvn -B org.apache.maven.plugins:maven-dependency-plugin:3.1.1:copy \
    -Dartifact=org.alfresco.integrations:alfresco-googledrive-repo-community:${VERSION}+bundled:amp \
    -DoutputDirectory=deploy_dir_community
mvn -B org.apache.maven.plugins:maven-dependency-plugin:3.1.1:copy \
    -Dartifact=org.alfresco.integrations:alfresco-googledrive-repo-enterprise:${VERSION}+bundled:amp \
    -DoutputDirectory=deploy_dir_enterprise
mvn -B org.apache.maven.plugins:maven-dependency-plugin:3.1.1:copy \
    -Dartifact=org.alfresco.integrations:alfresco-googledrive-share:${VERSION}:amp \
    -DoutputDirectory=deploy_dir_community
ln "deploy_dir_community/alfresco-googledrive-share-${VERSION}.amp" "deploy_dir_enterprise/alfresco-googledrive-share-${VERSION}.amp"

git checkout "tags/${VERSION}"
mvn -B generate-resources
git checkout -

git clone --depth=1 https://github.com/Alfresco/third-party-license-overrides.git
python3 ./third-party-license-overrides/thirdPartyLicenseCSVCreator.py --project "${GITHUB_WORKSPACE}" --version "${VERSION}" --combined --output "deploy_dir_enterprise"

echo "Local deploy_dir_community content:"
ls -lA deploy_dir_community
echo ""
echo "Local deploy_dir_enterprise content:"
ls -lA deploy_dir_enterprise

popd
set +vex
echo "========================== Finishing Prepare Release Deploy Script =========================="
