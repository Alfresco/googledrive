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

# Run the release plugin - with "[skip ci]" in the release commit message
mvn -B \
    "-Darguments=-DskipTests -DbuildNumber=$GITHUB_RUN_NUMBER" \
    release:clean release:prepare release:perform \
    -DscmCommentPrefix="[maven-release-plugin][skip ci] " \
    -Dusername="${GIT_USERNAME}" \
    -Dpassword="${GIT_PASSWORD}"

popd
set +vex
echo "=========================== Finishing Release Script =========================="
