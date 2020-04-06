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

# This does the following:
# 1. install the helper scripts locally
# 2. clone the ZACplus (Zinc-Oxygen battery) project into a local repo
# 3. creates a ReZip filtered clone of that project
# 4. print the bare size of these two repos for comparison
# 5. opens a GUI history browser for each of these repos,
#    as to compare the difference in changes in FreeCAD files (.fcstd),
#    which show as a simple "binary files differ" in the original,
#    vs a textual diff of the contained plain-text files in the fitlered version.

# Exit immediately on each error and unset variable;
# see: https://vaneyckt.io/posts/safer_bash_scripts_with_set_euxo_pipefail/
#set -Eeuo pipefail
set -Eeu

# We use this repo, because it has a lot of FreeCAD files,
# which are essentially ZIP files.
git_url="https://github.com/case06/ZACplus.git"
project_name="ZACplus"

# Create a random number between 0 and 255
rnd=$(od -A n -t d -N 1 /dev/urandom | tr -d ' ')

# Helper function that creates a bar git repo clone.
# This is useful to evaluate the real size of a git repo,
# as this is the size transferred over the network when cloning,
# or which is used on a server like GitHub.
create_bare_repo() {

	repo_orig="$1"
	bare_repo="$2"

	git clone --bare "${repo_orig}" "${bare_repo}"
	cd "${bare_repo}"
	git gc
	git prune --expire now
}

# Helper function that evaluates the size of a git repo,
# using different ways of measuring.
check_git_repo_size() {

	repo_orig="$1"
	bare_repo="/tmp/rezipdoc-test-$(basename \"$repo_orig\")-bare-$rnd"

	create_bare_repo "${repo_orig}" "${bare_repo}"

	du=/usr/bin/du
	repo_size_human=$(${du} -sh "${bare_repo}" | sed 's/[ \t].*//')
	repo_size_apparent=$(${du} -sb "${bare_repo}" | sed 's/[ \t].*//')
	repo_size_raw=$(${du} -s "${bare_repo}" | sed 's/[ \t].*//')

	rm -Rf "${bare_repo}"

	echo "$repo_size_human\t$repo_size_raw\t$repo_size_apparent"
}

repo_orig="/tmp/${project_name}-orig-$rnd"
repo_filtered="/tmp/${project_name}-filtered-$rnd"

# Install helper scripts
# NOTE Potential security risk!
curl -s -L https://raw.githubusercontent.com/hoijui/ReZipDoc/master/scripts/rezipdoc-scripts-tool.sh \
	| sh -s install --path --dev || true

# Create local clone of the project.
git clone "$git_url" "$repo_orig"
size_orig=$(check_git_repo_size "$repo_orig")

# Create a ReZip filtered clone of the above project
rezipdoc-history-filter.sh \
	--source "$repo_orig" \
	--branch master \
	--orig \
	--target "$repo_filtered"

(cd "$repo_filtered"; git remote rm source)
size_filtered=$(check_git_repo_size "$repo_filtered")

# Print bare repo sizes
echo -e "Bare repo size '$repo_orig':     $size_orig"
echo -e "Bare repo size '$repo_filtered': $size_filtered"

# Open git history browser sessions,
# so one can see the binary vs textual diff representation
# of the ZIP based files
(cd "$repo_orig"; gitk &)
(cd "$repo_filtered"; gitk &)
