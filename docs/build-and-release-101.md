# Build
The `google-drive` project uses _Travis CI_. \
The `.travis.yml` config file can be found in the root of the repository.


## Stages and Jobs
1. **Build**: Java Build with Unit Tests, WhiteSource
3. **Release**: Release and Deployment by publishing to Nexus and AWS Staging bucket.
4. **Company Release**: Downloads the `WhiteSource` report and `distribution` and publishes to AWS Releases.



## Branches
Travis CI builds differ by branch:
* `master`:
  - regular builds which include only the _Build_ and _Tests_ stages;
* `GOOGLEDOCS-*` (any branch starting with "GOOGLEDOCS-"):
  - regular builds which include only the _Build_ and _Tests_ stages;
* `SP|HF/* `
  - regular builds which include only the _Build_ and _Tests_ stages; 
* `release` and `release/SP|HF/*`
  - builds that include the _Build_ and _Release_ stages;
  - PR builds with `release` as the target branch only execute dry runs of the actual release, 
    without actually publishing anything;
* `company_release`:
  - builds that include the _Company Release_ stage only;


All other branches are ignored.


## Release process steps & info
Prerequisites:
 - the `master` / `SP|HF/*` branch has a green build and it contains all the changes that should be included in
  the next release

Steps:
1. Create a new branch from the `master` / `SP|HF/*` branch with the name `GOOGLEDOCS-###_release_version`;
2. (Optional) Update the project version if the current POM version is not the next desired
 release; use a maven command, i.e. `mvn versions:set -DnewVersion=#.##.#-SNAPSHOT versions:commit`;
3. Update the project's dependencies (remove the `-SNAPSHOT` suffixes) through a new commit on the
 `GOOGLEDOCS-###_release_version` branch;
4. Open a new Pull Request from the `GOOGLEDOCS-###_release_version` branch into the `release` / `release/SP|HF/*` branch and
 wait for a green build; the **Release** stage on the PR build will only execute a _Dry_Run_ of
  the release;
5. Once it is approved, merge the PR through the **Create a merge commit** option (as opposed to
 _Squash and merge_ or _Rebase and merge_), delete the `GOOGLEDOCS-###_release_version` branch, and wait 
 for a green build on the `release` / `release/SP|HF/*` branch;
6. Merge back the release branch into the initial branch;
7. Update the project dependencies (append the `-SNAPSHOT` suffixes)

Steps (6) and (7) can be done either directly from an IDE or through the GitHub flow, by creating
another branch and PR. Just make sure you don't add extra commits directly to the release branch,
as this will trigger another release.

## Company Release process steps & info
Prerequisites:
  - Engineering Release of the desired version has been done.

Steps:
1. Create a new `company_release` branch from the `release` or `release/SP|HF/*`. This job uses the git tag to identify 
the version to be uploaded to S3 release bucket.
2. If the last commit on `company_release` branch contains `[skip_ci]` in its message it will 
prevent Travis from starting a build. Push an empty commit in order to trigger the build, `git 
commit --allow-empty -m "Company Release <version>"`. Wait for a green build on the branch.
3. Delete local and remote `company_release` branch.

