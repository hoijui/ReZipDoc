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


# This creates a git repository with a lot of archives as content,
# focusing on archives that contain mostly plain-text content that changes
# non-radically over time.
#
# Practically, it will build the main JAR of this project for every commit
# of this repo, plus some text files, and commit each change, if there was one.

add_archive_bin=false
add_archive_src=true
num_commits_max=125

if [ "$add_archive_bin" != "true" -a "$add_archive_src" != "true" ]
then
	>&2 echo "Please include at least one of binary and source archive!"
	exit 1
fi

pwd_before=$(pwd)
this_script_file=$(basename $0)
this_script_dir=$(cd `dirname $0`; pwd)

if [ "$1" != "" ]
then
	source_repo="$1"
else
	source_repo=$(cd ${this_script_dir}; cd ..; pwd)
fi

rnd=$(od -A n -t d -N 1 /dev/urandom | tr -d ' ')
if [ "$2" != "" ]
then
	target_repo="$2"
else
	target_repo="/tmp/rezipdoc-archive-intensive-test-repo-${rnd}"
fi
# We need this one for building the binaries
if [ "$3" != "" ]
then
	tmp_source_repo="$3"
else
	tmp_source_repo="/tmp/rezipdoc-tmp-checkout-repo-${rnd}"
fi

echo "Using source repo:       ${source_repo}"
echo "Using tmp-checkout repo: ${tmp_source_repo}"
echo "Using target repo:       ${target_repo}"

mkdir "$target_repo"
cd "$target_repo"
git init
git config core.excludesfile 'some-file-that-does-not-exist'

git clone "$source_repo" "$tmp_source_repo"
cd "$tmp_source_repo"

num_commits=$(git log -${num_commits_max} --format="%H" --reverse master | wc -l)
i=0
for commit_hash in $(git log -${num_commits_max} --format="%H" --reverse master)
do
	i=`expr ${i} + 1`
	echo
	echo "############################################################"
	echo "Building commit ${i}/${num_commits} - ${commit_hash} ..."
	echo

	cd "$tmp_source_repo"
	git checkout ${commit_hash}
	if [ "${add_archive_bin}" = "true" ]
	then
		rm -f target/*.jar
		mvn package -DskipTests
	fi
	commit_msg=`git log -1 --format="ARCH - %s%n%n orig=%h%n%n%b" ${commit_hash}`

	cd "$target_repo"
	find -type f | grep -v "\.git" | xargs rm -Rf

	# Add some Project-global text files/sources
	cp "$tmp_source_repo/README"*  ./ 2> /dev/null
	cp "$tmp_source_repo/LICENSE"* ./ 2> /dev/null
	cp "$tmp_source_repo/pom.xml"  ./ 2> /dev/null
	cp -r "$tmp_source_repo/src"*  ./

	# Add archive(s)
	if [ "$add_archive_bin" = "true" ]
	then
		# uncompressing this is probably less interesting/useful,
		# because it contains mainly class files, which are binary,
		# and thus might not play much nicer with git
		# then the compressed archive
		cp "$tmp_source_repo/target/"*".jar" ./
	fi
	if [ "$add_archive_src" = "true" ]
	then
		# As this probably contains mostly text files,
		# it should play much nicer with git when uncompressed.
		(cd "$tmp_source_repo"; zip -r "$target_repo/src.zip" src)
	fi

	git add --all --force
	git commit -m "${commit_msg}"

	cd "$tmp_source_repo"
	echo "############################################################"
	echo
done

echo "Created tmp-checkout repo: ${tmp_source_repo}"
echo "Created target repo: ${target_repo}"

cd "$pwd_before"