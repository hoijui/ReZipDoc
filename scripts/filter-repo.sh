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


# Creates a local clone of a repo, and filters the main branch with ReZip(Doc).

pwd_before=$(pwd)
this_script_file=$(basename $0)
this_script_dir=$(cd `dirname $0`; pwd)

if [ "$1" = "" ]
then
	>&2 echo "1st argument needs to be a path or URL to a git repository"
	exit 1
fi
source_repo="$1"

if [ "$2" = "" -o -e "$2" ]
then
	>&2 echo "2nd argument needs to be a path to a non-existing directory (it will be created by this script)"
	exit 2
fi
target_repo="$2"

echo "Source repo: ${source_repo}"
echo "Target repo: ${target_repo}"

mkdir "$target_repo"
cd "$target_repo"
git init
git remote add source "$source_repo"
git fetch source

# Install our filter
if [ -e "$this_script_dir/install-git-filter.sh" ]
then
	sh "${this_script_dir}/install-git-filter.sh"
else
	curl -s https://raw.githubusercontent.com/hoijui/ReZipDoc/master/scripts/install-git-filter.sh | sh
fi

git checkout --orphan master_filtered
git commit --allow-empty --allow-empty-message -m ""

num_commits=$(git log --format="%H" --reverse source/master | wc -l)
i=0
for commit_hash in $(git log --format="%H" --reverse source/master)
do
	i=`expr ${i} + 1`
	echo
	echo "############################################################"
	echo "Copying & filtering commit ${i}/${num_commits} - ${commit_hash} ..."
	echo

	commit_msg=`git log -1 --format="FILTERED - %s%n%n orig=%h%n%n%b" ${commit_hash}`
	git cherry-pick --strategy-option=theirs -Xpatience --allow-empty --no-commit ${commit_hash} 2> /dev/null \
		; git add --all --force 2> /dev/null \
		&& git add --all --force --renormalize 2> /dev/null \
		&& git commit -m "${commit_msg}" 2> /dev/null \
		|| break

	echo "############################################################"
	echo
done

echo "Source repo: ${source_repo}"
echo "Target repo: ${target_repo}"

cd "$pwd_before"