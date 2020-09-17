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
this_script_file=$(basename "$0")
script_name="$this_script_file"
this_script_dir=$(cd "$(dirname "$0")"; pwd)

# Settings and default values
source_repo="."
add_archive_bin="false"
add_archive_src="true"
num_commits_max=1000

printUsage() {
	echo "$script_name - This script does:"
	echo "* create one or multiple repositories including archives"
	echo "* filters all of them with ReZip(Doc)"
	echo "* creates a report about the original and filtered repository sizes"
	echo "Think of it as a performance demo."
	echo
	echo "Usage:"
	echo "    $script_name [OPTIONS]"
	echo
	echo "Options:"
	echo "    -h, --help               show this help message"
	echo "    --no-src-archive         do not add the sources ZIP to each commit"
	echo "    --bin-archive            add the binary archive (JAR) to each commit (NOTE this will take a long time)"
	echo "    -m, --max-commits        maximum number of commits to consider in each pass"
	echo "    -s, --source [path|URL]  the repo to read commits from"
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
		--no-src-archive)
			add_archive_src="false"
			;;
		--bin-archive)
			add_archive_bin="true"
			;;
		-m|--max-commits)
			num_commits_max="$1"
			shift # past argument
			;;
		-s|--source)
			source_repo="$1"
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

if ! git ls-remote "$source_repo" > /dev/null 2> /dev/null
then
	>&2 echo "Source repo is not a valid git repository: '$source_repo'!"
	exit 1
fi

rnd=$(od -A n -t d -N 1 /dev/urandom | tr -d ' ')
tmp_repo="/tmp/rezipdoc-tmp-repo-${rnd}"
archive_repo="/tmp/rezipdoc-archives-repo-${rnd}"
filtered_repo="/tmp/rezipdoc-filtered-repo-${rnd}"

echo "Source repo:         '${source_repo}'"
echo "Source repo (copy):  '${tmp_repo}'"
echo "Max commits:         ${num_commits_max}"
echo "Archives repo:       '${archive_repo}'"
echo "Filtered repo:       '${filtered_repo}'"

echo
echo "Starting in 3 seconds ..."
sleep 3
echo

echo
echo "##############################"
echo "# Creating archives repo ... #"
echo "##############################"
echo
archives_extra_args=""
if [ "$add_archive_src" = "false" ]
then
	archives_extra_args="$archives_extra_args --no-src-archive"
fi
if [ "$add_archive_bin" = "true" ]
then
	archives_extra_args="$archives_extra_args --bin-archive"
fi
if ! "$this_script_dir/rezipdoc-create-archives-repo.sh" \
	--max-commits "${num_commits_max}" \
	--source "${source_repo}" \
	--target "${archive_repo}" \
	--tmp "${tmp_repo}" \
	${archives_extra_args}
then
	>&2 echo "Failed creating archives repo!"
	exit 1
fi

echo
echo "##############################"
echo "# Creating filtered repo ... #"
echo "##############################"
echo
if ! "$this_script_dir/rezipdoc-history-filter.sh" \
	--max-commits "${num_commits_max}" \
	--source "${archive_repo}" \
	--target "${filtered_repo}"
then
	>&2 echo "Failed creating filtered repo!"
	exit 1
fi

echo
echo "################################"
echo "# Checking bare repo sizes ... #"
echo "################################"
echo

_git_compact() {

	rm -rf .git/refs/original/
	git reflog expire --expire=now --all
	git gc --prune=now --aggressive
}

create_bare_repo() {

	orig_repo="$1"
	bare_repo="$2"

	git clone --bare "${orig_repo}" "${bare_repo}"
	cd "${bare_repo}"
	_git_compact
}

check_git_repo_size() {

	orig_repo="$1"
	bare_repo="/tmp/rezipdoc-test-$(basename "$orig_repo")-bare-$rnd"

	create_bare_repo "${orig_repo}" "${bare_repo}"

	du=/usr/bin/du
	repo_size_human=$(${du} -sh "${bare_repo}" | sed 's/[ \t].*//')
	repo_size_apparent=$(${du} -sb "${bare_repo}" | sed 's/[ \t].*//')
	repo_size_raw=$(${du} -s "${bare_repo}" | sed 's/[ \t].*//')

	rm -Rf "${bare_repo}"

	printf "%s\t%s\t%s\n" "$repo_size_human" "$repo_size_raw" "$repo_size_apparent"
}

size_archive=$(check_git_repo_size "${archive_repo}")
size_filtered=$(check_git_repo_size "${filtered_repo}")

cd "$pwd_before"

echo
echo "###########"
echo "# Summary #"
echo "###########"
echo
echo "Source repo:         '${source_repo}'"
echo "Source repo (copy):  '${tmp_repo}'"
echo "Max commits:         ${num_commits_max}"
echo "Archives repo:       '${archive_repo}'"
echo "Filtered repo:       '${filtered_repo}'"
echo "Archives repo size:  ${size_archive}"
echo "Filtered repo size:  ${size_filtered}"
