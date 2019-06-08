# ReZipDoc = ReZip & ZipDoc

If you are storing ZIP based files in your `git` repo,
you probably want to use __ReZipDoc__.

* [__ReZip__](https://github.com/costerwi/rezip)
  For more efficient Git packing of ZIP based files
* [__ZipDoc__](https://github.com/costerwi/zipdoc)
  A Git `textconv` program to show text-based diffs of ZIP files

---

Index:

* [Project state](#project-state)
* [Installation](#installation)
	* [easy](#installation-from-maven-central-easy)
	* [long process (more control, more secure)](#installation-from-sources-long-process)
* [Filter a project](#filter-a-project)
* [Culprits](#culprits)
* [Motivation](#motivation)
* [How it works](#how-it-works)
* [Benefits](#benefits)
* [Observations](#observations)

---

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

[![SonarCloud Status](https://sonarcloud.io/api/project_badges/measure?project=io.github.hoijui.rezipdoc:rezipdoc&metric=alert_status)](https://sonarcloud.io/dashboard?id=io.github.hoijui.rezipdoc:rezipdoc) 
[![SonarCloud Coverage](https://sonarcloud.io/api/project_badges/measure?project=io.github.hoijui.rezipdoc:rezipdoc&metric=coverage)](https://sonarcloud.io/component_measures/metric/coverage/list?id=io.github.hoijui.rezipdoc:rezipdoc)
[![SonarCloud Bugs](https://sonarcloud.io/api/project_badges/measure?project=io.github.hoijui.rezipdoc:rezipdoc&metric=bugs)](https://sonarcloud.io/component_measures/metric/reliability_rating/list?id=io.github.hoijui.rezipdoc:rezipdoc)
[![SonarCloud Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=io.github.hoijui.rezipdoc:rezipdoc&metric=vulnerabilities)](https://sonarcloud.io/component_measures/metric/security_rating/list?id=io.github.hoijui.rezipdoc:rezipdoc)

## Installation

This program requires Java JRE 8 or newer.

If you have a Unix/Linux environment on your system,
which is the case on OSX, Linux, Unix,
or on Windows with git installed,
then [installation from Maven Central](#installation-from-maven-central)
is the easy way to use ReZipDoc.
Otherwise, or if you want to use the latest development version or your own code,
you want to [install from sources](#installation-from-sources).

### Installation from Maven Central (easy)

This installs the latest release of ReZipDoc into your local git repo.

__NOTE__
This downloads and executes an online script onto your machine,
which is a potential security risk.
You may want to check-out the script before running it.

We will install the script to the local user directory,
so you may use it to easily install the filter to multiple local repos.
If this is not yet the case, you may want to create the dir `$HOME/bin`
and put it in your `PATH` env:

```bash
# Create the users 'bin' directory
mkdir -p $HOME/bin
# add it to the PATH in the current shell
export PATH="$PATH:$HOME/bin"
# add it to the PATH after boot
echo "export PATH=\"\$PATH:\$HOME/bin\"" >> ~/.profile
```

Download/Install the filter installer script:

```bash
# download the script
curl -s "https://raw.githubusercontent.com/hoijui/ReZipDoc/master/scripts/install-filter.sh" \
	-o "$HOME/bin/install-filter.sh"
# (optional) inspect the script to make sure it is no mal-ware
#$EDITOR $HOME/bin/install-filter.sh"
# mark it as executable
chmo +x "$HOME/bin/install-filter.sh"
```

Now to actually install the filter in a specific local repo,
make sure that CWD is the local git repo you want to install this filter to,
and install it:

```bash
cd ~/src/myGitRepo/
install-filter.sh --install
```

To uninstall, use ` --remove`:

```bash
cd ~/src/myGitRepo/
install-filter.sh --remove
```

### Installation from sources (long process)

#### 1. Build the JAR

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

#### 2. Install the JAR

Store _rezipdoc-\*.jar_ somewhere locally, either:

 * (global) in your home directory, for example under _~/bin/_
 * (repo - tracked) in your repository, tracked, for example under _<repo-root>/tools/_
 * (repo - local) __recommended__ in your repository, locally only, under _<repo-root>/.git/_

#### 3. Install the Filter(s)

execute these lines:

```bash
# Install the add/commit filter
git config --replace-all filter.reZip.clean "java -cp .git/rezipdoc-*.jar io.github.hoijui.rezipdoc.ReZip --uncompressed"

# (optionally) Install the checkout filter
git config --replace-all filter.reZip.smudge "java -cp .git/rezipdoc-*.jar io.github.hoijui.rezipdoc.ReZip --compressed"

# (optionally) Install the diff filter
git config --replace-all diff.zipDoc.textconv "java -cp .git/rezipdoc-*.jar io.github.hoijui.rezipdoc.ZipDoc"
```

#### 4. Enable the filters

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
[attr]reZip       filter=reZip textual
# This makes git visualize ZIP files as uncompressed text with some meta info
# NOTE See the ReZipDoc README for how to install the required git filter
[attr]zipDoc      diff=zipDoc textual
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

## Filter a project

This downloads the filter script and filters the `master` branch
of the repo at `pwd` into the new repo "../myRepo\_filtered".

_NOTE_
This only filters a single branch.

```bash
curl -s https://raw.githubusercontent.com/hoijui/ReZipDoc/master/scripts/filter-repo.sh -o filter-repo.sh
curl -s https://raw.githubusercontent.com/hoijui/ReZipDoc/master/scripts/filter-repo.sh -o filter-repo.sh
./filter-repo.sh \
	--source . \
	--branch master \
	--target ../myRepo_filtered
```

It also works with an online source:

```bash
./filter-repo.sh \
	--source "https://github.com/hoijui/ReZipDoc.git" \
	--branch master \
	--target myRepo_filtered
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
