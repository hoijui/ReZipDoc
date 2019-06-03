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
num_commits_max=1000

printUsage() {
	echo "`basename $0` - This script does:"
	echo "* create one or multiple repositories including archives"
	echo "* filters all of them with ReZip(Doc)"
	echo "* creates a report about the original and filtered repository sizes"
	echo "Think of it as a performance demo."
	echo
	echo "Usage:"
	echo "    `basename $0` [OPTIONS]"
	echo
	echo "Options:"
	echo "    -h, --help               show this help message"
	echo "    -m, --max-commits        maximum number of commits to consider in each pass"
	echo "    -s, --source [path|URL]  the repo to read commits from"
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
		-m|--max-commits)
			num_commits_max=$2
			shift # past argument
			;;
		-s|--source)
			source_repo="$2"
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

rnd=$(od -A n -t d -N 1 /dev/urandom | tr -d ' ')
tmp_repo="/tmp/rezipdoc-tmp-repo-${rnd}"
archive_repo="/tmp/rezipdoc-archives-repo-${rnd}"
filtered_repo="/tmp/rezipdoc-filtered-repo-${rnd}"
archive_clone_repo="/tmp/rezipdoc-archives-repo-clone-${rnd}"
filtered_clone_repo="/tmp/rezipdoc-filtered-repo-clone-${rnd}"

echo "Source repo:            '${source_repo}'"
echo "Source repo (copy):     '${tmp_repo}'"
echo "Archives repo:          '${archive_repo}'"
echo "Filtered repo:          '${filtered_repo}'"
echo "Archives repo (clone):  '${archive_clone_repo}'"
echo "Filtered repo (clone):  '${filtered_clone_repo}'"

echo
echo "Starting in 3 seconds ..."
sleep 3
echo

echo
echo "############################"
echo "# Create archives repo ... #"
echo "############################"
echo
"$this_script_dir/create-archives-repo.sh" \
	--max-commits ${num_commits_max} \
	--source "${source_repo}" \
	--target "${archive_repo}" \
	--tmp "${tmp_repo}"
if [ $? -ne 0 ]
then
	>&2 echo "Failed creating archives repo!"
	exit 1
fi

echo
echo "############################"
echo "# Create filtered repo ... #"
echo "############################"
echo
"$this_script_dir/filter-repo.sh" \
	--max-commits ${num_commits_max} \
	--source "${archive_repo}" \
	--target "${filtered_repo}"
if [ $? -ne 0 ]
then
	>&2 echo "Failed creating filtered repo!"
	exit 1
fi

echo
echo "###############################"
echo "# Create bare repo clones ... #"
echo "###############################"
echo
git clone --bare "${archive_repo}" "${archive_clone_repo}"
cd "${archive_clone_repo}"
git gc
git prune --expire now

git clone --bare "${filtered_repo}" "${filtered_clone_repo}"
cd "${filtered_clone_repo}"
git gc
git prune --expire now

du=/usr/bin/du
size_archive=`${du} -sh "${archive_clone_repo}"`
size_filtered=`${du} -sh "${filtered_clone_repo}"`

cd "$pwd_before"

echo
echo "###########"
echo "# Summary #"
echo "###########"
echo
echo "Source repo:            '${source_repo}'"
echo "Source repo (copy):     '${tmp_repo}'"
echo "Archives repo:          '${archive_repo}'"
echo "Filtered repo:          '${filtered_repo}'"
echo "Archives repo (clone):  '${archive_clone_repo}'"
echo "Filtered repo (clone):  '${filtered_clone_repo}'"
echo "Archives repo size:     ${size_archive}"
echo "Filtered repo size:     ${size_filtered}"
