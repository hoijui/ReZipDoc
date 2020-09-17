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

pwd_before="$(pwd)"
this_script_file=$(basename "$0")
script_name="$this_script_file"
this_script_dir=$(cd "$(dirname "$0")"; pwd)

# Settings and default values
action=""
dry_prefix=""
scripts_base_url="https://raw.githubusercontent.com/hoijui/ReZipDoc"
# We can parse the latest released version from this
metadata_url="http://repo2.maven.org/maven2/io/github/hoijui/rezipdoc/rezipdoc/maven-metadata.xml"
scripts_install_dir="$HOME/bin"
script_names='rezipdoc-repo-tool.sh rezipdoc-history-filter.sh'
# Whether to use latest development scripts, or the stable (last release) versions
enable_development="false"
enable_path="false"
java_pkg="io.github.hoijui.rezipdoc"
maven_group="$java_pkg"
maven_artifact="rezipdoc"
fetch_url="https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=${maven_group}&a=${maven_artifact}&v=LATEST"

printUsage() {
	echo "$script_name - This installs (or removes) the ReZipDoc helper shell scripts"
	echo "to the local directory ~/bin/."
	echo
	echo "See the ReZipDoc README for further info."
	echo
	echo "NOTE This is really only required for developers, hacking on this code-base."
	echo
	echo "Usage:"
	echo "    $script_name ACTION [OPTIONS]"
	echo
	echo "Actions:"
	echo "    -h, --help  show this help message"
	echo "    install     install the latest (stable or development) versions of the scripts,"
	echo "                if none are installed yet"
	echo "    remove      remove the local scripts"
	echo "    update      install the latest (stable or development) versions of the scripts"
	echo "    check       check whether the scripts are installed"
	echo
	echo "Options:"
	echo "    --dev       (install|update) when installing or updating, install the latest dev scripts,"
	echo "                instead of stable"
	echo "    --dry       (install|remove|update) show what would be done, instead of actually doing anything"
	echo "    --path      (install) add the install directory to PATH in the current shell and after reboot"
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
		--dev)
			if [ "$action" = "remove" ] || [ "$action" = "check" ]
			then
				>&2 echo "Action '$action' does not support '--dev'."
				exit 2
			fi
			enable_development="true"
			;;
		--dry)
			if [ "$action" = "check" ]
			then
				>&2 echo "Action '$action' does not support '--dry'"
				exit 2
			fi
			dry_prefix="echo"
			;;
		--path)
			if [ "$action" = "remove" ] || [ "$action" = "update" ]
			then
				>&2 echo "Action '$action' does not support '--path'"
				exit 2
			fi
			enable_path="true"
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
	update_args=""
	if [ "$dry_prefix" != "" ]
	then
		update_args="$update_args --dry"
	fi

	install_args=""
	if [ "$enable_development" = "true" ]
	then
		install_args="$install_args --dev"
	fi

	# Call ourselves recursively
	$0 remove ${update_args} \
		&& $0 install ${update_args} ${install_args}
	exit $?
fi

# If we got this far, it means that 'action' is set to 'check|install|remove'

extra_info=""
if [ "$action" = "install" ]
then
	if [ "$enable_development" = "true" ]
	then
		version="master"
		revision="$version"
	else
		version=$(curl -s "$metadata_url" | grep '<latest>' | sed 's/.*<latest>//' | sed 's/<\/latest>.*//')
		revision="rezipdoc-$version"
	fi
	extra_info="$extra_info (version: $version)"
fi

echo "$script_name action: ${action}ing$extra_info ..."

exit_state=0

dir_in_path() {
	dir="$1"
	ret=1:was
	case :$PATH: in
		*:${dir}:*) ret=0
	esac
	return ${ret}
}

# global checks
if [ "$action" = "check" ] && [ "$enable_path" = "true" ]
then
	if [ -e "$scripts_install_dir" ]
	then
		echo "install directory exists:         '$scripts_install_dir'"
	else
		echo "install directory does not exist: '$scripts_install_dir'"
		exit_state=1
	fi
	if dir_in_path "$scripts_install_dir"
	then
		echo "install directory is in PATH:     '$scripts_install_dir'"
	else
		echo "install directory is not in PATH: '$scripts_install_dir'"
		exit_state=1
	fi
fi
if [ "$action" = "install" ] && [ "$enable_path" = "true" ]
then
	if [ ! -e "$scripts_install_dir" ]
	then
		echo    "creating scripts install dir: '$scripts_install_dir' ..."
		${dry_prefix} mkdir -p "$scripts_install_dir"
		install_state=$?
		exit_state=$((exit_state + install_state))
		[ ${install_state} -eq 0 ] \
			&& echo "done" || echo "failed!"
	fi
	if [ "$enable_path" = "true" ] && [ -e "$scripts_install_dir" ] && ! dir_in_path "$scripts_install_dir"
	then
		echo    "adding scripts install dir to PATH ..."
		${dry_prefix} export PATH="$PATH:$scripts_install_dir"
		profile_file="$HOME/.profile"
		[ "${dry_prefix}" != "" ] && profile_file="/dev/stdout"
		echo "export PATH=\"\$PATH:${scripts_install_dir}\"" >> ${profile_file}
		install_state=$?
		exit_state=$((exit_state + install_state))
		[ ${install_state} -eq 0 ] \
			&& echo "done" || echo "failed!"
	fi
fi

# per script-file checks
for script_name in ${script_names}
do
	if [ "$action" = "check" ]
	then
		if [ -f "$scripts_install_dir/$script_name" ]
		then
			echo "script is installed:     $script_name"
		else
			echo "script is NOT installed: $script_name"
			exit_state=1
		fi
	elif [ "$action" = "install" ]
	then
		if [ -f "$scripts_install_dir/$script_name" ]
		then
			echo    "was already installed:          $script_name"
			exit_state=1
		else
			echo -n "installing (version: $version): $script_name ... "
			${dry_prefix} curl -s "$scripts_base_url/$revision/scripts/$script_name" -o "$scripts_install_dir/$script_name" \
				&& ${dry_prefix} chmod +x "$scripts_install_dir/$script_name"
			install_state=$?
			exit_state=$((exit_state + install_state))
			[ ${install_state} -eq 0 ] \
				&& echo "done" || echo "failed!"
		fi
	elif [ "$action" = "remove" ]
	then
		if [ -f "$scripts_install_dir/$script_name" ]
		then
			echo -n "removing:        $script_name ... "
			${dry_prefix} rm -f "$scripts_install_dir/$script_name"
			remove_state=$?
			exit_state=$((exit_state + remove_state))
			[ ${remove_state} -eq 0 ] \
				&& echo "done" || echo "failed!"
		else
			echo    "was not present: $script_name"
			exit_state=1
		fi
	fi
done

exit ${exit_state}
