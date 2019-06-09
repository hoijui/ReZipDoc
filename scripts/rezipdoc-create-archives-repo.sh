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
add_archive_bin="false"
add_archive_src="true"
num_commits_max=1000
source_repo=$(cd ${this_script_dir}; cd ..; pwd)
rnd=$(od -A n -t d -N 1 /dev/urandom | tr -d ' ')
target_repo="/tmp/rezipdoc-archives-repo-${rnd}"
# We use this one for building the binaries
tmp_repo="/tmp/rezipdoc-tmp-repo-${rnd}"

printUsage() {
	echo "`basename $0` - This creates a git repository with a lot of archives as content,"
	echo "focusing on archives that contain mostly plain-text content that changes"
	echo "non-radically over time."
	echo
	echo "Practically, it will build the main JAR of this project for every commit"
	echo "of this repo, plus some text files, and commit each change, if there was one."
	echo
	echo "Usage:"
	echo "    `basename $0` [OPTIONS]"
	echo
	echo "Options:"
	echo "    -h, --help               show this help message"
	echo "    --no-src-archive         do not add the sources ZIP to each commit"
	echo "    --bin-archive            add the binary archive (JAR) to each commit"
	echo "    -m, --max-commits        maximum number of commits to transcribe into the new repo"
	echo "    -s, --source [path|URL]  the repo to transcribe from"
	echo "    -t, --target [path]      the repo to transcribe to"
	echo "    --tmp [path]             the repo to use for temporary checkout and binary building"
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
		--no-src-archive)
			add_archive_src="false"
			;;
		--bin-archive)
			add_archive_bin="true"
			;;
		-m|--max-commits)
			num_commits_max=$2
			shift # past argument
			;;
		-s|--source)
			source_repo="$2"
			shift # past argument
			;;
		-t|--target)
			target_repo="$2"
			shift # past argument
			;;
		--tmp)
			tmp_repo="$2"
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

if [ "$add_archive_bin" != "true" -a "$add_archive_src" != "true" ]
then
	>&2 echo "Please include at least one of binary and source archive!"
	exit 1
fi

git ls-remote "$source_repo" > /dev/null 2> /dev/null
if [ $? -ne 0 ]
then
	>&2 echo "Source repo is not a valid git repository: '$source_repo'!"
	exit 1
fi

if [ -e "$tmp_repo" ]
then
	>&2 echo "Temporary repo can not be an existing path: '$tmp_repo'!"
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

echo "Using source repo:       ${source_repo}"
echo "Using tmp-checkout repo: ${tmp_repo}"
echo "Using target repo:       ${target_repo}"

git clone "$source_repo" "$tmp_repo"

mkdir "$target_repo"
cd "$target_repo"
git init
# This disables the global git-ignore file, which might otherwise prevent us
# from adding binaries (like archives).
git config core.excludesfile 'some-file-that-does-not-exist'

cd "$tmp_repo"

num_commits=$(git log -${num_commits_max} --format="%H" --reverse master | wc -l)
i=0
for commit_hash in $(git log -${num_commits_max} --format="%H" --reverse master)
do
	i=`expr ${i} + 1`
	echo
	echo "############################################################"
	echo "Building commit ${i}/${num_commits} - ${commit_hash} ..."
	echo

	cd "$tmp_repo"
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
	cp "$tmp_repo/README"*  ./ 2> /dev/null
	cp "$tmp_repo/LICENSE"* ./ 2> /dev/null
	cp "$tmp_repo/pom.xml"  ./ 2> /dev/null
	cp -r "$tmp_repo/src"*  ./

	# Add archive(s)
	if [ "$add_archive_bin" = "true" ]
	then
		# uncompressing this is probably less interesting/useful,
		# because it contains mainly class files, which are binary,
		# and thus might not play much nicer with git
		# then the compressed archive
		cp "$tmp_repo/target/"*".jar" ./
	fi
	if [ "$add_archive_src" = "true" ]
	then
		# As this probably contains mostly text files,
		# it should play much nicer with git when uncompressed.
		(cd "$tmp_repo"; zip --quiet -r "$target_repo/src.zip" src)
	fi

	git add --all --force
	git commit -m "${commit_msg}"

	cd "$tmp_repo"
	echo "############################################################"
	echo
done

# Make sure a potential global value might be used again
git --unset config core.excludesfile

echo "Created tmp-checkout repo: ${tmp_repo}"
echo "Created target repo: ${target_repo}"

cd "$pwd_before"