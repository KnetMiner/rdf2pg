#!/usr/bin/env bash
set -eE -o pipefail

# This is based on knetminer-ci and it's invoked by the standard GH Action workflow, to bootstrap
# the CI build.
#


# Installs additional requirements.
#
function stage_build_setup_local
{
	stage_build_setup
	sudo apt-get install libxml2-utils
}


# Completes the deployment of the knetminer-api, after the basic deployment consisting of 
# 'mvn deploy'. For instance, reinstalls the API on the test or production server.
#
# Here, updates the Downloads page.
function stage_deploy_local
{  
	stage_deploy # Prints diagnostics
  is_deploy_mode || return 0
  
	update_download_page
}


function update_download_page
{
	printf "=== Updating the Download Page\n"
	
	# git config --show-origin --show-scope --get-regexp 'credential|url\.' || true	

	printf "== Cloning the wiki\n"	
	cd /tmp
	rm -Rf rdf2pg.wiki
	# Disable credentials, to avoid problems with the act tool
	GIT_CONFIG_GLOBAL=/dev/null git clone https://github.com/KnetMiner/rdf2pg.wiki.git
	cd rdf2pg.wiki	
	

	printf "== Generating the Downloads page\n"

	my_dir="$PROJECT_HOME/ci-build-v2/java-maven"

	. "$my_dir/download-page-utils/download-page-utils.sh"
	
	url_base="https://artifactory.knetminer.com/#/public"
	template_path="$my_dir/Downloads-template.md"
	
	# placeholder group artifact extension is_stable 
	make_download_page \
  	"rdf2neoSnap" "$url_base" "uk.ac.rothamsted.kg" "rdf2neo-cli" "zip" "false" < "$template_path" \
  | make_download_page "rdf2neoRel" "$url_base" "uk.ac.rothamsted.kg" "rdf2neo-cli" "zip" "true" \
  |	make_download_page "rdf2graphmlSnap" "$url_base" "uk.ac.rothamsted.kg" "rdf2graphml-cli" "zip" "false" \
  | make_download_page "rdf2graphmlRel" "$url_base" "uk.ac.rothamsted.kg" "rdf2graphml-cli" "zip" "true" \
  > Downloads.md

	GIT_CONFIG_GLOBAL=/dev/null git diff --exit-code && return 

  if [[ "$CI_IS_ACT_TOOL" == 'true' ]]; then
  	# Cause act messes up with git credentials
		printf "== WARNING: we're under the act tool, skipping the Wiki push\n"
		return
	fi
	
	printf "== Committing wiki changes\n"
  git commit -a -m "docs(upgrade): update downloads page [CI automation]"
  git push --set-upstream origin master # credentials are already set at this point
}


printf "== Installing ci-build scripts and then running the build\n"

# DO USE a version tag in place of main, DO MAKE your builds stable and predictable
ci_build_url_base="https://raw.githubusercontent.com/KnetMiner/knetminer-ci/refs/tags/1.0"
# TODO: debugging, comment out!
ci_build_url_base="https://raw.githubusercontent.com/KnetMiner/knetminer-ci/heads/main"
script_url="$ci_build_url_base/ci-build-v2/install.sh"
. <(curl --fail-with-body -o - "$script_url") "$ci_build_url_base" java-maven

main
