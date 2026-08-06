#!/usr/bin/env bash

echo "========================== Starting Prepare Release Deploy Script ==========================="
PS4="\[\e[35m\]+ \[\e[m\]"
set -vex
pushd "$(dirname "${BASH_SOURCE[0]}")/../"

# Fail fast if the release version is missing (it comes from the workflow env). Without it the artifact
# downloads and the 'git checkout tags/${VERSION}' below would fail in non-obvious ways.
: "${RELEASE_VERSION:?RELEASE_VERSION must be set (see .github/workflows/build.yml env)}"

if [ ! -d deploy_dir_community ]; then

    mkdir -p deploy_dir_community deploy_dir_enterprise

    # Version to publish - taken from the workflow env (RELEASE_VERSION), same value used by release.sh.
    export VERSION="${RELEASE_VERSION}"

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
    # Bundled (self-contained) AMP variants, published under "${VERSION}+bundled"
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

fi

popd
set +vex
echo "========================== Finishing Prepare Release Deploy Script =========================="
