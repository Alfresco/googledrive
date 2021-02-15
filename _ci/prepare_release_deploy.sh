#!/usr/bin/env bash

echo "========================== Starting Prepare Release Deploy Script ==========================="
PS4="\[\e[35m\]+ \[\e[m\]"
set -vex
pushd "$(dirname "${BASH_SOURCE[0]}")/../"

if [ ! -d deploy_dir_community ]; then

    mkdir -p deploy_dir_community deploy_dir_enterprise

    # Identify latest annotated tag (latest version)
    export VERSION=$(git describe --abbrev=0 --tags)

    # Download the WhiteSource report
#    mvn -B org.alfresco:whitesource-downloader-plugin:inventoryReport \
#        -N \
#        "-Dorg.whitesource.product=Google Docs Integration" \
#        -DsaveReportAs=deploy_dir_community/3rd-party.xlsx
#    ln "deploy_dir_community/3rd-party.xlsx" "deploy_dir_enterprise/3rd-party.xlsx"

    # Download the AMP artifacts
    mvn -B org.apache.maven.plugins:maven-dependency-plugin:3.1.1:copy \
        -Dartifact=org.alfresco.integrations:alfresco-googledrive-repo-community:${VERSION}:amp \
        -DoutputDirectory=deploy_dir_community
    mvn -B org.apache.maven.plugins:maven-dependency-plugin:3.1.1:copy \
        -Dartifact=org.alfresco.integrations:alfresco-googledrive-repo-enterprise:${VERSION}:amp \
        -DoutputDirectory=deploy_dir_enterprise
    mvn -B org.apache.maven.plugins:maven-dependency-plugin:3.1.1:copy \
        -Dartifact=org.alfresco.integrations:alfresco-googledrive-share:${VERSION}:amp \
        -DoutputDirectory=deploy_dir_community
    ln "deploy_dir_community/alfresco-googledrive-share-${VERSION}.amp" "deploy_dir_enterprise/alfresco-googledrive-share-${VERSION}.amp"

    echo "Local deploy_dir_community content:"
    ls -lA deploy_dir_community
    echo ""
    echo "Local deploy_dir_enterprise content:"
    ls -lA deploy_dir_enterprise

fi

popd
set +vex
echo "========================== Finishing Prepare Release Deploy Script =========================="