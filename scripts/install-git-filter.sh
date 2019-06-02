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


# This installs or removes a custom git filter and (optionally) a diff tool
# to the local repo.
#
# Please use this driver to prevent merge conflicts in the generated sources.
#
# NOTE This is really only required for developers, hacking on this code-base.
# NOTE All of this gets installed into the local repo only, under ".git/",
#      meaning it is not versioned.


# As described in [gitattributes](http://git-scm.com/docs/gitattributes),
# you may see unnecessary merge conflicts when you add attributes to a file that
# causes the repository format for that file to change.
# To prevent this, Git can be told to run a virtual check-out and check-in of all
# three stages of a file when resolving a three-way merge.
# This might slowdown merges
enable_renormalize="true"

pwd_before=$(pwd)
this_script_file=$(basename $0)
this_script_dir=$(cd `dirname $0`; pwd)

target_path_specs='*.docx *.xlsx *.pptx *.odt *.ods *.odp *.mcdx *.slx *.zip *.jar *.fcstd'
install_smudge="false"
install_diff="false"
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

action="$1"
if [ "$action" = "" ]
then
	# Set the default action
	action="install"
fi
if [ "$action" != "install" -a "$action" != "remove" -a "$action" != "reinstall" -a "$action" != "check" ]
then
	>&2 echo "Invalid action '$action'; please choose either of: install, remove, reinstall, check"
	exit 1
fi
if [ "$action" = "reinstall" ]
then
	# Call ourselves recursively
	$0 remove && $0 install
	exit $?
fi

echo "$0 action: $action ..."

# Install our binary (the JAR)
pre_text="git filter and diff \"binary\" file $binary_file_glob - "
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
		>2& echo "$pre_text not installed!"
		exit 1
	elif [ "$action" = "install" ]
	then
		echo -n "$pre_text installing ... "

		# Delete previously downloaded versions
		rm ${binary_file_glob}

		target_dir="${this_script_dir}/../target"
		local_binary=`find "$target_dir" -maxdepth 1 -type f -name "rezipdoc-*.jar"  | grep -v "\-sources" | grep -v "\-javadoc" | head -1`
		if [ "$use_local_binary_if_available" = "true" -a "local_binary" != "" ]
		then
			cp "$local_binary" ./
		else
			# Download the latest ReZipDoc release JAR from the Maven Central repository
			wget --content-disposition "$fetch_url"
			# this results in a file like "rezipdoc-0.1.jar" in the CWD
		fi

		source_binary_file=`find -maxdepth 1 -name "rezipdoc-*.jar"`

		echo "Using driver binary '$source_binary_file'"

		# Extract the version from the release JAR name
		version=`echo "$source_binary_file" | xargs basename --suffix='.jar' | sed -e 's/.*rezipdoc-//'`

		binary_file=".git/`basename $source_binary_file`"

		mv "$source_binary_file" "$binary_file"
		[ $? -eq 0 ] \
			&& echo "done" || echo "failed!"
	else
		echo "$pre_text removing skipped (file does not exist)"
	fi
fi

# Configure the filter and (optionally) diff
pre_text="git filter and diff config entry in $conf_file - "
if [ "$action" = "check" ]
then
	is_config_present() {
		filter_name="$1"
		git config ${filter_name} > /dev/null
		if [ $? -eq 0 ]
		then
			echo "$pre_text ${filter_name} is present!"
		else
			>&2 echo "$pre_text ${filter_name} is not present!"
			exit 1
		fi
	}

	is_config_present filter.reZip.clean

	if [ "$install_smudge" = "true" ]
	then
		is_config_present filter.reZip.smudge
	fi

	if [ "$install_diff" = "true" ]
	then
		is_config_present diff.zipDoc.textconv
	fi

	echo "$pre_text present!"
elif [ "$action" = "install" ]
then
	echo -n "$pre_text writing ... "

	extra_args=""
	#extra_args="--replace-all"

	# Install the add/commit filter
	git config ${extra_args} filter.reZip.clean "java -cp '$binary_file' ${java_pkg}.ReZip --uncompressed"

	# (optionally) Install the checkout filter
	if [ "$install_smudge" = "true" ]
	then
		git config ${extra_args} filter.reZip.smudge "java -cp '$binary_file' ${java_pkg}.ReZip --compressed"
	fi

	# (optionally) Install the diff filter
	if [ "$install_diff" = "true" ]
	then
		git config ${extra_args} diff.zipDoc.textconv "java -cp '$binary_file' ${java_pkg}.ZipDoc"
	fi

	[ $? -eq 0 ] && echo "done" || echo "failed!"
else
	echo -n "$pre_text removing ... "

	git config --remove-section filter.reZip
	git config --remove-section diff.zipDoc

	[ $? -eq 0 ] && echo "done" || echo "failed!"
fi

# Apply the filter and (optionally) diff to matching file(s)
pre_text="git attributes entries to $attributes_file - "
grep -q "$marker_begin" "$attributes_file" 2> /dev/null
if [ $? -eq 0 ]
then
	# Our section does exist in the attributes_file
	if [ "$action" = "check" ]
	then
		echo "$pre_text exist!"
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
		>2& echo "$pre_text do not exist!"
		exit 1
	elif [ "$action" = "install" ]
	then
		echo "$pre_text writing ..."
		echo "$marker_begin" >> "$attributes_file"
		echo "$header_note" >> "$attributes_file"

		cat >> "$attributes_file" << EOF
# This forces git to treat files as if they were text-based (for example in diffs)
[attr]textual     diff merge
# This makes git re-zip ZIP files uncompressed on commit
[attr]reZip       filter=reZip textual
EOF

		if [ "$install_diff" = "true" ]
		then
			cat >> "$attributes_file" << EOF
# This makes git visualize ZIP files as uncompressed text with some meta info
[attr]zipDoc      diff=zipDoc textual
# This combines in-history decompression and the uncompressed diff view of ZIP files
[attr]reZipDoc    reZip zipDoc
EOF
		else
			cat >> "$attributes_file" << EOF
[attr]reZipDoc    reZip
EOF
		fi
		echo >> "$attributes_file"

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
pre_text="git merge renormalization  - "
renorm_enabled=`git config --get merge.renormalize`
if [ "$renorm_enabled" = "true" ]
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
		>2& echo "$pre_text not enabled!"
		exit 1
	elif [ "$action" = "install" ]
	then
		echo -n "$pre_text enabling ... "
		git config --add --bool merge.renormalize true
		[ $? -eq 0 ] \
			&& echo "done" || echo "failed!"
	else
		echo "$pre_text disabling skipped (is already disabled)"
	fi
fi
