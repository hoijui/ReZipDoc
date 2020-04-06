#!/usr/bin/env bash

script_dir=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")

"${script_dir}/rezipdoc-sample-filter-session.sh" \
	"https://github.com/case06/upklib_v2.git"
