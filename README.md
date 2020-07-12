# ReZipDoc

A _repack uncompressed_ & _diff visualizer_ for ZIP based files stored in git repos.

Most
[<img alt="git" src="https://upload.wikimedia.org/wikipedia/commons/e/e0/Git-logo.svg" height="20" align="center" />](
https://git-scm.com/)
repos hosting
[<img alt="Open Source Hardware" src="https://upload.wikimedia.org/wikipedia/commons/f/fd/Open-source-hardware-logo.svg" height="80" align="center" />](
https://en.wikipedia.org/wiki/Open-source_hardware)
should use [__ReZipDoc__](https://github.com/hoijui/ReZipDoc).

## What is this?

[git](https://git-scm.com/) does not like binary files.
They make the repo grow fast in size in MB (see [delta compression](https://en.wikipedia.org/wiki/Delta_encoding)),
and when you try to see what changed in them in a commit, you only get this:

> Binary files _A_ and _B_ differ!

... not very useful!

__ReZipDoc__ solves both of these issues, though only for ZIP based files --
like for example FreeCAD and LibreOffice files --
not all binary ones.

So if you are storing ZIP based files in your `git` repo,
you probably want to use __ReZipDoc__.

## Index

* [Project state](#project-state)
* [How to use](#how-to-use)
* [Installation](#installation)
	* [Install helper scripts](#install-helper-scripts)
	* [Install diff viewer or filter](#install-diff-viewer-or-filter)
	* [Install filter manually](#install-filter-manually)
* [Filter repo history](#filter-repo-history)
	* [Filtering example](#filtering-example)
* [Culprits](#culprits)
* [Motivation](#motivation)
* [How it works](#how-it-works)
* [Benefits](#benefits)
* [Observations](#observations)
* [Based on](#based-on)

## Project state

This repo contains a heavily revised, refined version of ReZip (and ZipDoc),
plus [unit tests](src/test/java/io/github/hoijui/rezipdoc)
and [helper scripts](scripts),
which were not available in the original.

[![License](https://img.shields.io/badge/license-GPL%203-orange.svg)](https://www.gnu.org/licenses/gpl-3.0.en.html)
[![GitHub last commit](https://img.shields.io/github/last-commit/hoijui/ReZipDoc.svg)](https://github.com/hoijui/ReZipDoc)
[![Issues](https://img.shields.io/badge/issues-GitHub-57f.svg)](https://github.com/hoijui/ReZipDoc/issues)

`master`:
[![Build Status](https://travis-ci.org/hoijui/ReZipDoc.svg?branch=master)](https://travis-ci.org/hoijui/ReZipDoc)
[![Open Hub project report](https://www.openhub.net/p/ReZipDoc/widgets/project_thin_badge.gif)](https://www.openhub.net/p/ReZipDoc?ref=sample)

[![SonarCloud Status](https://sonarcloud.io/api/project_badges/measure?project=hoijui_ReZipDoc&metric=alert_status)](https://sonarcloud.io/dashboard?id=hoijui_ReZipDoc)
[![SonarCloud Coverage](https://sonarcloud.io/api/project_badges/measure?project=hoijui_ReZipDoc&metric=coverage)](https://sonarcloud.io/component_measures/metric/coverage/list?id=hoijui_ReZipDoc)
[![SonarCloud Bugs](https://sonarcloud.io/api/project_badges/measure?project=hoijui_ReZipDoc&metric=bugs)](https://sonarcloud.io/component_measures/metric/reliability_rating/list?id=hoijui_ReZipDoc)
[![SonarCloud Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=hoijui_ReZipDoc&metric=vulnerabilities)](https://sonarcloud.io/component_measures/metric/security_rating/list?id=hoijui_ReZipDoc)

## How to use

If your git repo makes heavy use of ZIP based files,
then you probably want to use ReZipDoc in one of these three ways:

* install __ZipDoc diff viewer__ -
  This allows you to see changes within you ZIP based files
  when looking at git history in a human-readable way.
  It does not change your past nor future git history.

  To use this, [install](#install-diff-viewer-or-filter) with `--diff` only.
* install __ReZip filter__ -
  This will change your future git repos history,
  storing ZIP based files without compression.

  To use this, [install](#install-diff-viewer-or-filter) with `--commit --diff --renormalize`.
* install __ReZip filter & filter repo__ -
  This changes both the past (<- ___Caution!___)
  and future history of your repo.

  To use this, [create a copy of the repo with filtered history](#filter-repo-history).

## Installation

The filter and diff tool require Java 8 or newer.

The helper scripts - which are mostly used for installing the filter -
require a POSIX (~= Unix) environment.
This is the case on OSX, Linux, BSD, Unix and even Windows, if git is installed.

The recommended procedure is to
[install the helper scripts](#install-helper-scripts) once,
and then use them to comfortably install the filter into local git repos.

> __NOTE__\
This downloads and executes an online script onto your machine,
which is a potential security risk.
You may want to check-out the script before running it.

### Install helper scripts

> __NOTE__\
This has to be done once per developer machine.

They get installed into `~/bin/`,
and if the directory did not exist before,
it will get added to `PATH`.

<!--
	https://git.io/fjgIX
is a short URL created with
	https://raw.githubusercontent.com/hoijui/ReZipDoc/master/scripts/rezipdoc-scripts-tool.sh
	curl -s -i https://git.io -F "url=https://raw.githubusercontent.com/hoijui/ReZipDoc/master/scripts/rezipdoc-scripts-tool.sh" | grep "Location:" | sed 's/.* //'
-->

To install:
```bash
curl -s -L https://git.io/fjgIX | sh -s install --path
```

To update (to latest development version):
```bash
curl -s -L https://git.io/fjgIX | sh -s update --dev
```

To remove:
```bash
curl -s -L https://git.io/fjgIX | sh -s remove
```

### Install diff viewer or filter

> __NOTE__\
This has to be done once per repo.

This installs the latest release of ReZipDoc into your local git repo.

Make sure you already have [installed the helper scripts](#install-helper-scripts)
on your machine.

Switch to the local git repo you want to install this filter to,
for example:

```bash
cd ~/src/myRepo/
```

As explained in [How to use](#how-to-use),
you now want to use one of the following:

1. Install the diff viewer

	```bash
	rezipdoc-repo-tool.sh install --diff
	```
2. Install the filter

	```bash
	rezipdoc-repo-tool.sh install --commit --renormalize
	```
3. Filter the history & install the filter

	If you [filter the repo history](#filter-repo-history),
	the freshly created, filtered repo will already have the filter installed as above.

To uninstall the diff viewer and/or filter, run:

```bash
rezipdoc-repo-tool.sh remove
```

#### Install filter manually

Only use this if you can not use [the above](#install-diff-viewer-or-filter), for some reason.

1. Build the JAR

	Run this in bash:

	```bash
	cd
	mkdir -p src
	cd src
	git clone git@github.com:hoijui/ReZipDoc.git
	cd ReZipDoc
	mvn package
	echo "Created ReZipDoc binary:"
	ls -1 $PWD/target/rezipdoc-*.jar
	```

2. Install the JAR

	Store _rezipdoc-\*.jar_ somewhere locally, either:

	 * (global) in your home directory, for example under _~/bin/_
	 * (repo - tracked) in your repository, tracked, for example under _<repo-root>/tools/_
	 * (repo - local) __recommended__ in your repository, locally only, under _<repo-root>/.git/_

3. Install the Filter(s)

	execute these lines:

	```bash
	# Install the add/commit filter
	git config --replace-all filter.reZip.clean "java -cp .git/rezipdoc-*.jar io.github.hoijui.rezipdoc.ReZip --uncompressed"

	# (optionally) Install the checkout filter
	git config --replace-all filter.reZip.smudge "java -cp .git/rezipdoc-*.jar io.github.hoijui.rezipdoc.ReZip --compressed"

	# (optionally) Install the diff filter
	git config --replace-all diff.zipDoc.textconv "java -cp .git/rezipdoc-*.jar io.github.hoijui.rezipdoc.ZipDoc"
	```

4. Enable the filters

	In one of these files:

	* (global) _${HOME}/.gitattributes_
	* (repo - tracked) _<repo-root>/.gitattributes_
	* (repo - local) __recommended__ _<repo-root>/.git/info/attributes_

	Assign attributes to paths:

	```bash
	# This forces git to treat files as if they were text-based (for example in diffs)
	[attr]textual     diff merge text
	# This makes git re-zip ZIP files uncompressed on commit
	# NOTE See the ReZipDoc README for how to install the required git filter
	[attr]reZip       textual filter=reZip
	# This makes git visualize ZIP files as uncompressed text with some meta info
	# NOTE See the ReZipDoc README for how to install the required git filter
	[attr]zipDoc      textual diff=zipDoc
	# This combines in-history decompression and uncompressed view of ZIP files
	[attr]reZipDoc    reZip zipDoc

	# MS Office
	*.docx   reZipDoc
	*.xlsx   reZipDoc
	*.pptx   reZipDoc
	# OpenOffice
	*.odt    reZipDoc
	*.ods    reZipDoc
	*.odp    reZipDoc
	# Misc
	*.mcdx   reZipDoc
	*.slx    reZipDoc
	# Archives
	*.zip    reZipDoc
	# Java archives
	*.jar    reZipDoc
	# FreeCAD files
	*.fcstd  reZipDoc
	```

## Filter repo history

This always creates a new copy of the repository.

>__NOTE__\
This only filters a single branch.

Make sure you have the [helper scripts installed](#install-helper-scripts) and in your `PATH`.

This filters the `master` branch of the repo at `~/src/myRepo`
into a new local repo `~/src/myRepo_filtered`,
using the original commit messages, authors and dates:

```bash
rezipdoc-history-filter.sh \
	--source ~/src/myRepo \
	--branch master \
	--orig \
	--target ~/src/myRepo_filtered
```

It also works with an online source:

```bash
rezipdoc-history-filter.sh \
	--source "https://github.com/case06/ZACplus.git" \
	--branch master \
	--orig \
	--target /tmp/ZACplus_filtered
```

After doing this, the new, filtered repo will already have the filter installed,
so future commits will be filtered.

### Filtering example

We are going to run
[a script that filters the Zinc-Oxide Open Hardware battery (ZAC+) project repo](
https://github.com/hoijui/ReZipDoc/blob/master/scripts/rezipdoc-sample-session-ZACplus.sh),
which has a header comment explaining what it does in detail.

In short, it downloads ReZipDoc helper scripts to `~/bin`,
adds that dir to `PATH` if it is not there yet,
creates temporary git repos in `/tmp/`,
and generates some command-line output.

Run it like this:

<!--
	https://git.io/fjgR5
is a short URL created with
	https://raw.githubusercontent.com/hoijui/ReZipDoc/master/scripts/rezipdoc-sample-session-ZACplus.sh
	curl -s -i https://git.io -F "url=https://raw.githubusercontent.com/hoijui/ReZipDoc/master/scripts/rezipdoc-sample-session-ZACplus.sh" | grep "Location:" | sed 's/.* //'
-->

```bash
curl -s -L https://git.io/fjgR5 | sh -s
```

## Culprits

As described in [gitattributes](http://git-scm.com/docs/gitattributes),
you may see unnecessary merge conflicts when you add attributes to a file that
causes the repository format for that file to change.
To prevent this, Git can be told to run a virtual check-out and check-in of all
three stages of a file when resolving a three-way merge:

```bash
git config --add --bool merge.renormalize true
```

## Motivation

Many popular applications, such as
[Microsoft Office](http://en.wikipedia.org/wiki/Office_Open_XML) and
[Libre/Open Office](http://en.wikipedia.org/wiki/OpenDocument),
save their documents as XML in compressed zip containers.
Small changes to these document's contents may result in big changes to their
compressed binary container file.
When compressed files are stored in a Git repository
these big differences make delta compression inefficient or impossible
and the repository size is roughly the sum of its revisions.

This small program acts as a Git clean filter driver.
It reads a ZIP file from stdin and outputs the same ZIP content to stdout,
but without compression.

##### pros

+ human readable/plain-text diffs of (ZIP based) archives,
  (if they contain plain-text files)
+ smaller overall repository size if the archive contents change frequently

##### cons

- slower `git add`/`git commit` process
- slower checkout process, if the smudge filter s used

## How it works

When adding/committing a ZIP based file,
ReZip unpacks it and repacks it without compression,
before adding it to the index/commit.
In an uncompressed ZIP file,
the archived files appear _as-is_ in its content
(together with some binary meta-info before each file).
If those archived files are plain-text files,
this method will play nicely with git.

## Benefits

The main benefit of ReZip over Zippey,
is that the actual file stored in the repository is still a ZIP file.
Thus, in many cases, it will still work _as-is_
with the respective application (for example Open Office),
even if it is obtained without going through
the re-packing-with-compression smudge filter,
so for example when downloading the file through a web-interface,
instead of checking it out with git.

## Observations

The following are based on my experience in real-world cases.
Use at your own risk.
Your mileage may vary.

### SimuLink

* One packed repository with ReZip was 54% of the size of the packed repository
  storing compressed ZIPs.
* Another repository with 280 _\*.slx_ files and over 3000 commits was originally 281 MB
  and was reduced to 156 MB using this technique (55% of baseline).

### MS Power-Point

I found that the loose objects stored without this filter were about 5% smaller
than the original file size (zLib on top of zip compression).
When using the ReZip filter, the loose objects were about 10% smaller than the
original files, since zLib could work more efficiently on uncompressed data.
The packed repository with ReZip was only 10% smaller than the packed repository
storing compressed zips.
I think this unremarkable efficiency improvement is due to a large number of
_\*.png_ files in the presentation which were already stored without compression
in the original _\*.pptx_.

## Based on

* [__ReZip__](https://github.com/costerwi/rezip)
  For more efficient Git packing of ZIP based files
* [__ZipDoc__](https://github.com/costerwi/zipdoc)
  A Git `textconv` program to show text-based diffs of ZIP files
