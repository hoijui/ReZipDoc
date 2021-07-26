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
set -Eeuo pipefail
#set -Eeu

pwd_before="$(pwd)"
this_script_file=$(basename "$0")
script_name="$this_script_file"
this_script_dir=$(cd "$(dirname "$0")"; pwd)

# Settings and default values
action=""
target_path_specs=''
for ext in $(cat "$this_script_dir/../src/main/resources/ext_archives.txt")
do
	target_path_specs="$target_path_specs *.$ext"
done
# As described in [gitattributes](http://git-scm.com/docs/gitattributes),
# you may see unnecessary merge conflicts when you add attributes to a file that
# causes the repository format for that file to change.
# To prevent this, Git can be told to run a virtual check-out and check-in of all
# three stages of a file when resolving a three-way merge.
# This might slowdown merges
enable_renormalize="false"
enable_commit="false"
enable_checkout="false"
enable_diff="false"
java_pkg="io.github.hoijui.rezipdoc"
maven_group="$java_pkg"
maven_artifact="rezipdoc"
use_local_binary_if_available=true
fetch_url="https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=${maven_group}&a=${maven_artifact}&v=LATEST"
binary_file_glob=".git/rezipdoc-*.jar"
conf_file=".git/config"
#attributes_file="${HOME}/.gitattributes"
#attributes_file=".gitattributes"
attributes_file=".git/info/attributes"
# This serves as a magic marker, marking our generated text parts
# It could be any string that is unique enough.
gen_token="go-generate-token"

marker_begin="# BEGIN $gen_token"
marker_end="# END $gen_token"
header_note="# NOTE Do not manually edit this section; it was generated with $this_script_file"

printUsage() {
	echo "$script_name - This installs (or removes) a custom git filter"
	echo "and a diff tool to the local repo, which make using"
	echo "ZIP based archives more git-workflow friendly."
	echo
	echo "See the ReZipDoc README for further info."
	echo
	echo "NOTE This is really only required for developers, hacking on this code-base."
	echo "NOTE All of this gets installed into the local repo only, under '.git/',"
	echo "     meaning it is not versioned."
	echo
	echo "Usage:"
	echo "    $script_name ACTION [OPTIONS]"
	echo
	echo "Actions:"
	echo "    -h, --help  show this help message"
	echo "    install     install the specified parts of the filter into the local repo"
	echo "    remove      remove *everything* regarding the filter from the local repo"
	echo "    update      first remove, then install the previously installed parts of the filter again"
	echo "    check       check whether the specified parts of the filter are installed (-> return value 0)"
	echo
	echo "Options:"
	echo "    --commit       (filter part) re-archives ZIP files without compression on commit"
	echo "    --checkout     (filter part) re-archives ZIP files wit compression on checkout"
	echo "    --diff         (filter part) represents ZIP based files uncompressed in diff views"
	echo "    --renormalize  (filter part) check-out and -in files on merge conflicts"
}

set_action() {
	new_action="$1"
	if [ "$action" != "" ]
	then
		>&2 echo "You may only specify one action!"
		printUsage
		exit 1
	fi
	action="$new_action"
}

# Handle command line arguments
while [ $# -gt 0 ]
do
	option="$1"
	case ${option} in
		-h|--help)
			printUsage
			exit 0
			;;
		install)
			set_action "install"
			;;
		remove)
			set_action "remove"
			;;
		update)
			set_action "update"
			;;
		check)
			set_action "check"
			;;
		--renormalize)
			enable_renormalize="true"
			;;
		--commit)
			enable_commit="true"
			;;
		--checkout)
			enable_checkout="true"
			;;
		--diff)
			enable_diff="true"
			;;
		*)
			# unknown option / not an option
			>&2 echo "Unknown option '${option}'!"
			printUsage
			exit 1
			;;
	esac
	shift # next argument or value
done


if [ "$action" = "" ]
then
	>&2 echo "No action defined!"
	printUsage
	exit 1
fi
if [ "$action" = "update" ]
then
	parts=""
	for chk_part in --commit --checkout --diff --renormalize
	do
		if $0 check ${chk_part} > /dev/null 2>&1
		then
			parts="$parts $chk_part"
		fi
	done
	# Call ourselves recursively, re-installing the same parts that already were installed
	$0 remove \
		&& $0 install ${parts}
fi

# If we got so far, it means that 'action' is set to 'check|install|remove'

if ! git ls-remote ./ > /dev/null 2>&1
then
	>&2 echo "The current working directory is not a valid git repo!"
	exit 1
fi

if [ "$action" = "check" ]
then
	if [ "$enable_renormalize" != "true" ] && [ "$enable_commit" != "true" ] && [ "$enable_checkout" != "true" ] && [ "$enable_diff" != "true" ]
	then
		>&2 echo "Please check for at least one of --commit, --checkout, --diff, --renormalize"
		exit 2
	fi
elif [ "$action" = "install" ]
then
	if [ "$enable_commit" != "true" ] && [ "$enable_checkout" != "true" ] && [ "$enable_diff" != "true" ]
	then
		>&2 echo "Please install at least one of --commit, --checkout, --diff"
		exit 2
	fi
else
	if [ "$enable_renormalize" = "true" ] || [ "$enable_commit" = "true" ] || [ "$enable_checkout" = "true" ] || [ "$enable_diff" = "true" ]
	then
		>&2 echo "Remove always removes the whole filter installation;"
		>&2 echo "no need to specify parts with any of --commit, --checkout, --diff, --renormalize"
		exit 2
	fi
fi

echo "$script_name action: ${action}ing ..."

# Install our binary (the JAR)
pre_text="git filter and diff binary -"
if [ -e ${binary_file_glob} ]
then
	if [ "$action" = "check" ]
	then
		echo "$pre_text installed!"
	elif [ "$action" = "install" ]
	then
		echo "$pre_text installing skipped (file already exists)"
	else
		echo -n "$pre_text removing ... "
		rm ${binary_file_glob} \
			&& echo "done" || echo "failed!"
	fi
else
	if [ "$action" = "check" ]
	then
		>&2 echo "$pre_text not installed!"
		exit 1
	elif [ "$action" = "install" ]
	then
		echo -n "$pre_text installing ... "

		# Delete previously downloaded versions
		rm -f "$binary_file_glob"

		mvn_target_dir="${this_script_dir}/../target"
		# Try to get the latest version, in case there are multiple ones
		local_binary=$(find "$mvn_target_dir" -maxdepth 1 -type f -name "rezipdoc-*.jar" | grep -v "\-sources" | grep -v "\-javadoc" | sort --version-sort | tail -1)
		if [ "$use_local_binary_if_available" = "true" ] && [ "$local_binary" != "" ]
		then
			cp "$local_binary" ./
		else
			# Download the latest ReZipDoc release JAR from the Maven Central repository
			wget --content-disposition "$fetch_url"
			# this results in a file like "rezipdoc-0.1.jar" in the CWD
		fi

		source_binary_file=$(find . -maxdepth 1 -name "rezipdoc-*.jar")

		echo -n " using binary '$source_binary_file' ... "

		# Extract the version from the release JAR name
		version=$(echo "$source_binary_file" | xargs basename --suffix='.jar' | sed -e 's/.*rezipdoc-//')

		binary_file=".git/$(basename "$source_binary_file")"

		mv "$source_binary_file" "$binary_file" \
			&& echo "done" || echo "failed!"
	else
		echo "$pre_text removing skipped (file does not exist)"
	fi
fi

# Configure the filter and diff
pre_text="git filter and diff config entry in $conf_file -"
if [ "$action" = "check" ]
then
	is_config_present() {
		filter_name="$1"
		if git config ${filter_name} > /dev/null
		then
			echo "$pre_text ${filter_name} is present!"
		else
			>&2 echo "$pre_text ${filter_name} is not present!"
			exit 1
		fi
	}

	if [ "$enable_commit" = "true" ]
	then
		is_config_present filter.reZip.clean
	fi

	if [ "$enable_checkout" = "true" ]
	then
		is_config_present filter.reZip.smudge
	fi

	if [ "$enable_diff" = "true" ]
	then
		is_config_present diff.zipDoc.textconv
	fi

	echo "$pre_text present!"
elif [ "$action" = "install" ]
then
	echo -n "$pre_text writing ... "

	extra_args=""
	#extra_args="--replace-all"
	set +e

	# Install the add/commit filter
	if [ "$enable_commit" = "true" ]
	then
		git config ${extra_args} filter.reZip.clean "java -cp '$binary_file' ${java_pkg}.ReZip --uncompressed"
	fi

	# Install the checkout filter
	if [ "$enable_checkout" = "true" ]
	then
		git config ${extra_args} filter.reZip.smudge "java -cp '$binary_file' ${java_pkg}.ReZip --compressed"
	fi

	# Install the diff filter
	if [ "$enable_diff" = "true" ]
	then
		git config ${extra_args} diff.zipDoc.textconv "java -cp '$binary_file' ${java_pkg}.ZipDoc"
	fi

	[ $? -eq 0 ] && echo "done" || echo "failed!"
	set -e
else
	if git config --local --get-regexp "filter\.reZip\..*" > /dev/null 2>&1
	then
		echo -n "$pre_text filter - removing ... "
		git config --remove-section filter.reZip \
			&& echo "done" || echo "failed!"
	else
		echo "$pre_text filter - removing skipped (not present)"
	fi

	if git config --local --get-regexp "diff\.zipDoc\..*" > /dev/null 2>&1
	then
		echo -n "$pre_text diff - removing ... "
		git config --remove-section diff.zipDoc \
			&& echo "done" || echo "failed!"
	else
		echo "$pre_text diff - removing skipped (not present)"
	fi
fi

# Apply the filter and diff-view to matching file(s)
pre_text="git attributes entries to $attributes_file -"
if grep -q "$marker_begin" "$attributes_file" > /dev/null 2>&1
then
	# Our section does exist in the attributes_file
	if [ "$action" = "check" ]
	then
		echo "$pre_text exist!"
		if [ "$enable_commit" = "true" ] || [ "$enable_checkout" = "true" ] && ! grep -q -r "^\[attr\]reZip" "$attributes_file"
		then
			# only report failure if checking was explicitly requested
			>&2 echo "$pre_text - '[attr]reZip' does not exist!"
			exit 1
		fi
		if [ "$enable_diff" = "true" ] && ! grep -q -r "^\[attr\]zipDoc" "$attributes_file"
		then
			# only report failure if checking was explicitly requested
			>&2 echo "$pre_text - '[attr]zipDoc' does not exist!"
			exit 1
		fi
	elif [ "$action" = "install" ]
	then
		echo "$pre_text writing skipped (section already exists)"
	else
		echo -n "$pre_text removing ... "
		sed -e "/$marker_begin/,/$marker_end/d" --in-place "$attributes_file" \
			&& echo "done" || echo "failed!"
	fi
else
	# Our section does NOT exist in the attributes_file
	if [ "$action" = "check" ]
	then
		>&2 echo "$pre_text do not exist!"
		if [ "$enable_commit" = "true" ] || [ "$enable_checkout" = "true" ] || [ "$enable_diff" = "true" ]
		then
			# only report failure if checking was explicitly requested
			exit 1
		fi
	elif [ "$action" = "install" ]
	then
		echo "$pre_text writing ..."
		{
			echo "$marker_begin"
			echo "$header_note"
			cat << EOF
# This forces git to treat files as if they were text-based (for example in diffs)
[attr]textual     diff merge
EOF
		} >> "$attributes_file"

		parts=""
		if [ "$enable_commit" = "true" ] || [ "$enable_checkout" = "true" ]
		then
			parts="$parts reZip"
			cat >> "$attributes_file" << EOF
# This makes git re-zip ZIP files uncompressed on commit
[attr]reZip       textual filter=reZip
EOF
		fi

		if [ "$enable_diff" = "true" ]
		then
			parts="$parts zipDoc"
			cat >> "$attributes_file" << EOF
# This makes git visualize ZIP files as uncompressed text with some meta info
[attr]zipDoc      textual diff=zipDoc
EOF
		fi

		cat >> "$attributes_file" << EOF
[attr]reZipDoc   ${parts}

EOF

		# Disable globbing
		set -o noglob
		for path_spec in ${target_path_specs}
		do
			echo "        writing $path_spec ... "
			echo "$path_spec    reZipDoc" >> "$attributes_file"
		done
		# Re-enable globbing
		set +o noglob

		echo "$marker_end" >> "$attributes_file" \
			&& echo "    done" || echo "    failed!"
	else
		echo "$pre_text removing skipped (section not present)"
	fi
fi


# As described in [gitattributes](http://git-scm.com/docs/gitattributes),
# you may see unnecessary merge conflicts when you add attributes to a file that
# causes the repository format for that file to change.
# To prevent this, Git can be told to run a virtual check-out and check-in of all
# three stages of a file when resolving a three-way merge.
# This might slowdown merges

# Set git merge renormalization
pre_text="git merge renormalization -"
renormalize_enabled=$(git config --get merge.renormalize || true)
if [ "$renormalize_enabled" = "true" ]
then
	# Renormalization is enabled
	if [ "$action" = "check" ]
	then
		echo "$pre_text enabled!"
	elif [ "$action" = "install" ]
	then
		echo "$pre_text enabling skipped (is already enabled)"
	else
		echo -n "$pre_text disabling ... "
		git config --unset-all merge.renormalize \
			&& echo "done" || echo "failed!"
	fi
else
	# Renormalization is disabled
	if [ "$action" = "check" ]
	then
		>&2 echo "$pre_text not enabled!"
		if [ "$enable_renormalize" = "true" ]
		then
			# only report failure if checking was explicitly requested
			exit 1
		fi
	elif [ "$action" = "install" ]
	then
		echo -n "$pre_text enabling ... "
		git config --add --bool merge.renormalize true \
			&& echo "done" || echo "failed!"
	else
		echo "$pre_text disabling skipped (is already disabled)"
	fi
fi
