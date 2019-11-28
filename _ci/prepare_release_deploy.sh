#!/usr/bin/env bash

echo "========================== Starting Prepare Release Deploy Script ==========================="
PS4="\[\e[35m\]+ \[\e[m\]"
set -vex
pushd "$(dirname "${BASH_SOURCE[0]}")/../"

# Identify latest annotated tag (latest version)
export VERSION=$(git describe --abbrev=0 --tags)

mkdir -p deploy_dir_community deploy_dir_enterprise

# Download the WhiteSource report
mvn org.alfresco:whitesource-downloader-plugin:inventoryReport \
    -N \
    "-Dorg.whitesource.product=Google Docs Integration" \
    -DsaveReportAs=deploy_dir/3rd-party.xlsx
ln "deploy_dir_community/3rd-party.xlsx" "deploy_dir_enterprise/3rd-party.xlsx"

# Download the AMP artifacts
mvn org.apache.maven.plugins:maven-dependency-plugin:3.1.1:copy \
    -Dartifact=org.alfresco.integrations:googledrive-repo-community:${VERSION}:amp \
    -DoutputDirectory=deploy_dir_community
mvn org.apache.maven.plugins:maven-dependency-plugin:3.1.1:copy \
    -Dartifact=org.alfresco.integrations:googledrive-repo-enterprise:${VERSION}:amp \
    -DoutputDirectory=deploy_dir_enterprise
mvn org.apache.maven.plugins:maven-dependency-plugin:3.1.1:copy \
    -Dartifact=org.alfresco.integrations:googledrive-share:${VERSION}:amp \
    -DoutputDirectory=deploy_dir_community
ln "deploy_dir_community/googledrive-share-${VERSION}.amp" "deploy_dir_enterprise/googledrive-share-${VERSION}.amp"

echo "Local deploy_dir_community content:"
ls -lA deploy_dir_community
echo ""
echo "Local deploy_dir_enterprise content:"
ls -lA deploy_dir_enterprise

popd
set +vex
echo "========================== Finishing Prepare Release Deploy Script =========================="