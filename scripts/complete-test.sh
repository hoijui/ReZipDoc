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


# This script does:
# * create one or multiple repositories including archives
# * filters all of them with ReZip(Doc)
# * creates a report about the original and filtered repository sizes

pwd_before=$(pwd)
this_script_file=$(basename $0)
this_script_dir=$(cd `dirname $0`; pwd)

if [ "$1" = "" ]
then
	>&2 echo "1st argument needs to be a path or URL to a git repository"
	exit 1
fi
source_repo="$1"

rnd=$(od -A n -t d -N 1 /dev/urandom | tr -d ' ')
tmp_source_repo="/tmp/rezipdoc-source-repo-${rnd}"
archive_repo="/tmp/rezipdoc-archives-repo-${rnd}"
filtered_repo="/tmp/rezipdoc-filtered-repo-${rnd}"
archive_clone_repo="/tmp/rezipdoc-archives-repo-clone-${rnd}"
filtered_clone_repo="/tmp/rezipdoc-filtered-repo-clone-${rnd}"

echo "Source repo:            ${source_repo}"
echo "Source repo (copy):     ${tmp_source_repo}"
echo "Archives repo:          ${archive_repo}"
echo "Filtered repo:          ${filtered_repo}"
echo "Archives repo (clone):  ${archive_clone_repo}"
echo "Filtered repo (clone):  ${filtered_clone_repo}"

echo
echo "############################"
echo "# Create archives repo ... #"
echo "############################"
echo
"$this_script_dir/create-archives-repo.sh" "${source_repo}" "${archive_repo}" "${tmp_source_repo}"
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
"$this_script_dir/filter-repo.sh" "${archive_repo}" "${filtered_repo}"
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
echo "Source repo:            ${source_repo}"
echo "Source repo (copy):     ${tmp_source_repo}"
echo "Archives repo:          ${archive_repo}"
echo "Filtered repo:          ${filtered_repo}"
echo "Archives repo (clone):  ${archive_clone_repo}"
echo "Filtered repo (clone):  ${filtered_clone_repo}"
echo "Archives repo size:     ${size_archive}"
echo "Filtered repo size:     ${size_filtered}"
