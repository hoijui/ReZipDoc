#!/bin/sh
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


pwd_before=$(pwd)
this_script_file=$(basename $0)
this_script_dir=$(cd `dirname $0`; pwd)

# Settings and default values
source_repo=""
target_repo=""
num_commits_max=1000
use_orig_commit="false"
branch="master"
repo_tool_url="https://raw.githubusercontent.com/hoijui/ReZipDoc/master/scripts/rezipdoc-repo-tool.sh"
repo_tool_file_name=`basename "$repo_tool_url"`

printUsage() {
	echo "`basename $0` - Creates a local clone of a repo, and filters"
	echo "the main branch with ReZip(Doc)."
	echo
	echo "Usage:"
	echo "    `basename $0` [OPTIONS]"
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
while [ ${#} -gt 0 ]
do
	opName="$1"
	case ${opName} in
		-h|--help)
			printUsage
			exit 0
			;;
		-b|--branch)
			branch="$2"
			shift # past argument
			;;
		-m|--max-commits)
			num_commits_max=$2
			shift # past argument
			;;
		-o|--orig-msg)
			use_orig_commit="true"
			;;
		-s|--source)
			source_repo="$2"
			shift # past argument
			;;
		-t|--target)
			target_repo="$2"
			shift # past argument
			;;
		*)
			# unknown option / not an option
			>&2 echo "Unknown option '${opName}'!"
			printUsage
			exit 1
			;;
	esac
	shift # next argument or value
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
[ "$source_is_url" = "true" ] && source_type="local repo" || source_type="URL"

# If the source repo is a local directory, make the path to it absolute
[ "$source_is_url" != "true" ] && source_repo="`cd \"$source_repo\"; pwd`"

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
	repo_tool="/tmp/`basename --suffix='.sh' \"$repo_tool_file_name\"`-${rnd}.sh"
	curl -s "$repo_tool_url" -o "$repo_tool"
fi

# Install our filter if not yet installed
${repo_tool} --check > /dev/null 2> /dev/null
if [ $? -ne 0 ]
then
	${repo_tool} install --commit --diff --renormalize
fi

git checkout --orphan ${branch}_filtered
git commit --allow-empty --allow-empty-message -m ""

num_commits=$(git log -${num_commits_max} --format="%H" --reverse source/${branch} | wc -l)
i=0
for commit_hash in $(git log -${num_commits_max} --format="%H" --reverse source/${branch})
do
	i=`expr ${i} + 1`
	echo
	echo "############################################################"
	echo "Copying & filtering commit ${i}/${num_commits} - ${commit_hash} ..."
	echo

	commit_args=""
	if [ "$use_orig_commit" = "true" ]
	then
		#commit_msg=`git log -1 --format="%s%n%n%b" ${commit_hash}`
		commit_args="$commit_args --reuse-message=${commit_hash}"
	else
		commit_msg=`git log -1 --format="FILTERED - %s%n%n orig=%h%n%n%b" ${commit_hash}`
		# We have to give the message through stdin,
		# because otherwise the quoting somehow gets fucked up (by sh)
		commit_args="$commit_args --file=-"
	fi
	git cherry-pick --strategy-option=theirs -Xpatience --allow-empty --no-commit ${commit_hash} 2> /dev/null \
		; git add --all --force 2> /dev/null \
		&& git add --all --force --renormalize 2> /dev/null \
		&& echo "$commit_msg" | git commit -v ${commit_args} \
		|| break

	echo "############################################################"
	echo
done

# Merge the first (empty) commit with the second one
git rebase --root

echo "Source repo:  '${source_repo}' ($source_type)"
echo "Branch:       ${branch}"
echo "Max commits:  ${num_commits_max}"
echo "Target repo:  '${target_repo}'"

cd "$pwd_before"
