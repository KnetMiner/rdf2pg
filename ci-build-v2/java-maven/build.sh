#!/usr/bin/env bash
set -eE -o pipefail

# This is based on knetminer-ci and it's invoked by the standard GH Action workflow, to bootstrap
# the CI build.
#

# Completes the deployment of the knetminer-api, after the basic deployment consisting of 
# 'mvn deploy'. For instance, reinstalls the API on the test or production server.
#
# Here, updates the Downloads page.
function stage_deploy_local
{  
	stage_deploy # Prints diagnostics
  is_deploy_mode || return 0
  
  printf "== Updating the Download Page\n"

	cd "$PROJECT_HOME"

	cat <<EOT
	
	WARNING: this is a TODO, we need to upgrade old scripts to grab the Download links from Maven repos
	
	So, nothing happens now.
	
EOT
}


printf "== Installing ci-build scripts and then running the build\n"

# DO USE a version tag in place of main, DO MAKE your builds stable and predictable
ci_build_url_base="https://raw.githubusercontent.com/KnetMiner/knetminer-ci/refs/tags/1.0"
script_url="$ci_build_url_base/ci-build-v2/install.sh"
. <(curl --fail-with-body -o - "$script_url") "$ci_build_url_base" java-maven

main
