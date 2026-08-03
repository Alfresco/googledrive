#!/usr/bin/env bash

echo "========================== Starting Prepare Staging Deploy Script ==========================="
PS4="\[\e[35m\]+ \[\e[m\]"
set -vex
pushd "$(dirname "${BASH_SOURCE[0]}")/../"

if [ ! -d deploy_dir_community ]; then

    mkdir -p deploy_dir_community deploy_dir_enterprise

    ARTIFACT_GD_REPO_COMMUNITY=$(find . -name "alfresco-googledrive-repo-community-*.amp" ! -name "*+bundled.amp" -printf "%f\n" | head -1)
    ARTIFACT_GD_REPO_ENTERPRISE=$(find . -name "alfresco-googledrive-repo-enterprise-*.amp" ! -name "*+bundled.amp" -printf "%f\n" | head -1)
    ARTIFACT_GD_REPO_COMMUNITY_BUNDLED=$(find . -name "alfresco-googledrive-repo-community-*+bundled.amp" -printf "%f\n" | head -1)
    ARTIFACT_GD_REPO_ENTERPRISE_BUNDLED=$(find . -name "alfresco-googledrive-repo-enterprise-*+bundled.amp" -printf "%f\n" | head -1)
    ARTIFACT_GD_SHARE=$(find . -name "alfresco-googledrive-share-*.amp" -printf "%f\n" | head -1)

    export VERSION=$(echo "${ARTIFACT_GD_SHARE}" | sed -e "s/^alfresco-googledrive-share-//" -e "s/\.amp$//")

    # Download the WhiteSource report
#    mvn -B org.alfresco:whitesource-downloader-plugin:inventoryReport \
#        -N \
#        "-Dorg.whitesource.product=Google Docs Integration" \
#        -DsaveReportAs=deploy_dir_community/3rd-party.xlsx

    # Hard-link the artifacts into deploy directories
#    ln "deploy_dir_community/3rd-party.xlsx" "deploy_dir_enterprise/3rd-party.xlsx"
    ln "alfresco-googledrive-repo-community/target/${ARTIFACT_GD_REPO_COMMUNITY}"   "deploy_dir_community/${ARTIFACT_GD_REPO_COMMUNITY}"
    ln "alfresco-googledrive-repo-enterprise/target/${ARTIFACT_GD_REPO_ENTERPRISE}" "deploy_dir_enterprise/${ARTIFACT_GD_REPO_ENTERPRISE}"
    # Bundled variants are optional (only present when the -Pbundled build ran); guard so 'set -e' does not abort staging.
    [ -n "${ARTIFACT_GD_REPO_COMMUNITY_BUNDLED}" ]  && ln "alfresco-googledrive-repo-community/target/${ARTIFACT_GD_REPO_COMMUNITY_BUNDLED}"   "deploy_dir_community/${ARTIFACT_GD_REPO_COMMUNITY_BUNDLED}"
    [ -n "${ARTIFACT_GD_REPO_ENTERPRISE_BUNDLED}" ] && ln "alfresco-googledrive-repo-enterprise/target/${ARTIFACT_GD_REPO_ENTERPRISE_BUNDLED}" "deploy_dir_enterprise/${ARTIFACT_GD_REPO_ENTERPRISE_BUNDLED}"
    ln "alfresco-googledrive-share/target/${ARTIFACT_GD_SHARE}"                     "deploy_dir_community/${ARTIFACT_GD_SHARE}"
    ln "alfresco-googledrive-share/target/${ARTIFACT_GD_SHARE}"                     "deploy_dir_enterprise/${ARTIFACT_GD_SHARE}"

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
echo "========================== Finishing Prepare Staging Deploy Script =========================="
