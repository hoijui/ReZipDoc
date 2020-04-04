#!/usr/bin/env bash
# Copyright (c) 2019 Robin Vobruba <hoijui.quaero@gmail.com>
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.

# For info about this script, please refer to the `printUsage()` function below.

# Exit immediately on each error and unset variable;
# see: https://vaneyckt.io/posts/safer_bash_scripts_with_set_euxo_pipefail/
#set -Eeuo pipefail
set -Eeu

pwd_before=$(pwd)
this_script_file=$(basename $0)
this_script_dir=$(cd $(dirname $0); pwd)

# Settings and default values
source_repo=""
target_repo=""
num_commits_max=1000
use_orig_commit="false"
branch="master"
repo_tool_url="https://raw.githubusercontent.com/hoijui/ReZipDoc/master/scripts/rezipdoc-repo-tool.sh"
repo_tool_file_name=$(basename "$repo_tool_url")

printUsage() {
	echo "$(basename $0) - Creates a local clone of a repo, and filters"
	echo "the main branch with ReZip(Doc)."
	echo
	echo "Usage:"
	echo "    $(basename $0) [OPTIONS]"
	echo
	echo "Options:"
	echo "    -h, --help               show this help message"
	echo "    -b, --branch             git branch to filter"
	echo "    -m, --max-commits        maximum number of commits to filter into the new repo"
	echo "    -o, --orig               use the original commit message (default: prefix with \"FILTERED - \"),"
	echo "                             author, email and time"
	echo "    -s, --source [path|URL]  the repo to read commits from"
	echo "    -t, --target [path]      the repo to write commits to"
}

# Handle command line arguments
while [ $# -gt 0 ]
do
	opName="$1"
	shift # skip argument
	case ${opName} in
		-h|--help)
			printUsage
			exit 0
			;;
		-b|--branch)
			branch="$1"
			shift # past argument
			;;
		-m|--max-commits)
			num_commits_max=$1
			shift # past argument
			;;
		-o|--orig)
			use_orig_commit="true"
			;;
		-s|--source)
			source_repo="$1"
			shift # past argument
			;;
		-t|--target)
			target_repo="$1"
			shift # past argument
			;;
		*)
			# unknown option / not an option
			>&2 echo "Unknown option '${opName}'!"
			printUsage
			exit 1
			;;
	esac
done

git ls-remote "$source_repo" > /dev/null 2> /dev/null
if [ $? -ne 0 ]
then
	>&2 echo "Source repo is not a valid git repository: '$source_repo'!"
	exit 1
fi

if [ "$source_repo" = "$target_repo" ]
then
	>&2 echo "Source and target repos can not be equal!"
	exit 1
fi

if [ -e "$target_repo" ]
then
	>&2 echo "Target repo can not be an existing path: '$target_repo'!"
	exit 1
fi

# Check whether the source repo is a local directory or a URL
source_is_url="true"
[ -d "$source_repo" ] && source_is_url="false"
[ "$source_is_url" = "true" ] && source_type="URL" || source_type="local repo"

# If the source repo is a local directory, make the path to it absolute
[ "$source_is_url" != "true" ] && source_repo="$(cd "$source_repo"; pwd)"

echo "Source repo:  '${source_repo}' ($source_type)"
echo "Branch:       ${branch}"
echo "Max commits:  ${num_commits_max}"
echo "Target repo:  '${target_repo}'"

mkdir "$target_repo"
cd "$target_repo"
git init
git remote add source "$source_repo"
git fetch source

# Ensure we have a local filter installer script
if [ -e "$this_script_dir/$repo_tool_file_name" ]
then
	repo_tool="$this_script_dir/$repo_tool_file_name"
else
	rnd=$(od -A n -t d -N 1 /dev/urandom | tr -d ' ')
	repo_tool="/tmp/$(basename --suffix='.sh' \"$repo_tool_file_name\")-${rnd}.sh"
	curl -s "$repo_tool_url" -o "$repo_tool"
fi

# Install our filter if not yet installed
if ! ${repo_tool} check --commit --diff --renormalize > /dev/null 2> /dev/null
then
	${repo_tool} install --commit --diff --renormalize \
		|| ( >&2 echo "Failed installing filter!"; exit 2 )
fi

git checkout --orphan ${branch}_filtered
git commit --allow-empty --allow-empty-message -m ""

num_commits=$(git log -${num_commits_max} --format="%H" --reverse source/${branch} | wc -l)
i=0
for commit_hash in $(git log -${num_commits_max} --topo-order --format="%H" --reverse source/${branch})
do
	i=$(expr ${i} + 1)
	echo
	echo "############################################################"
	echo "Copying & filtering commit ${i}/${num_commits} - ${commit_hash} ..."
	echo

	commit_args=""
	if [ "$use_orig_commit" = "true" ]
	then
		#commit_msg=$(git log -1 --format="%s%n%n%b" ${commit_hash})
		commit_msg=""
		commit_args="$commit_args --reuse-message=${commit_hash}"
	else
		commit_msg=$(git log -1 --format="FILTERED - %s%n%n orig=%h%n%n%b" ${commit_hash})
		# We have to give the message through stdin,
		# because otherwise the quoting somehow gets fucked up (by sh)
		commit_args="$commit_args --file=-"
	fi

	set +e

	echo "Cherry-picking ..."
	git cherry-pick --strategy=recursive --strategy-option=theirs --allow-empty --mainline 1 --no-commit ${commit_hash}
	last_status=$?
	if [ ${last_status} -ne 0 ]
	then
		>&2 echo -e "\tfailed! (Cherry-picking for ${commit_hash})"
	fi

	echo "Removing ..."
	git status | grep 'deleted by them:' | cut -d':' -f2 | xargs -t -I {} git rm "{}"
	last_status=$?
	if [ ${last_status} -ne 0 ]
	then
		>&2 echo -e "\tfailed! (Removing for ${commit_hash})"
	fi

	echo "Adding the 1st ..."
	git add --all --force
	last_status=$?
	if [ ${last_status} -ne 0 ]
	then
		>&2 echo -e "\tfailed! (Adding the 1st for ${commit_hash})"
	fi

	if [ ${last_status} -eq 0 ]
	then
		echo "Adding the 2nd ..."
		git add --all --force --renormalize
		last_status=$?
		if [ ${last_status} -ne 0 ]
		then
			>&2 echo -e "\tfailed! (Adding the 2nd for ${commit_hash})"
		fi
	fi
	if [ ${last_status} -eq 0 ]
	then
		if output=$(git status --porcelain) && [ -z "${output}" ]
		then
			# Working directory clean (completely)
			if [ ${last_status} -ne 0 ]
			then
				>&2 echo -e "WARNING: Nothing to commit for ${commit_hash} -> skipping"
			fi
			last_status=0
		else
			echo "Committing ..."
			echo "$commit_msg" | git commit -v ${commit_args}
			last_status=$?
			if [ ${last_status} -ne 0 ]
			then
				>&2 echo -e "\tfailed! (Committing for ${commit_hash})"
			fi
		fi
	fi

	set -e

	if [ ${last_status} -ne 0 ]
	then
		git status
		echo "Failed!"
		exit ${last_status}
	fi

	echo "############################################################"
	echo
done

echo
echo "############################################################"
echo "############################################################"
echo

# Merge the first (empty) commit with the second one
echo "removing first (empty) commit ..."
git rebase --root

echo "Source repo:  '${source_repo}' ($source_type)"
echo "Branch:       ${branch}"
echo "Max commits:  ${num_commits_max}"
echo "Target repo:  '${target_repo}'"

# Check if the original and the filtered versions have the same final content
cd "${target_repo}"
set +e
git diff --exit-code --stat --color --color-moved "${branch}_filtered" "source/${branch}"
last_status=$?
set -e
if [ ${last_status} -ne 0 ]
then
	>&2 echo "ERROR: Original and filtered repos final content differ!"
	exit ${last_status}
fi

cd "$pwd_before"
