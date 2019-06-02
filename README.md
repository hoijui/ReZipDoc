# ReZipDoc = ReZip & ZipDoc

If you are storing ZIP based files in your `git` repo,
you probably want to use __ReZipDoc__.

* [__ReZip__](https://github.com/costerwi/rezip)
  For more efficient Git packing of ZIP based files
* [__ZipDoc__](https://github.com/costerwi/zipdoc)
  A Git `textconv` program to show text-based diffs of ZIP files

## Project state

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

### Installation from Maven Central

In a *nix shell, make sure that CWD is the local git repo you want to install this filter to.
Then run:

__NOTE__
This downloads and executes an online script on your machine,
which is a potential security risk.
You may first want to check-out the script.

```bash
sh <(curl -s https://raw.githubusercontent.com/hoijui/ReZipDoc/master/scripts/install-git-filter.sh)
```

To uninstall, just append ` remove`:

```bash
sh <(curl -s https://raw.githubusercontent.com/hoijui/ReZipDoc/master/scripts/install-git-filter.sh) remove
```

### Installation from sources

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
 * (repo) or in your repository, for example under _<repo-root>/tools/_

#### 3. Install the Filter(s)

Either in:

* (global) _${HOME}/.gitattributes_
* (repo) _<repo-root>/.gitattributes_

add these lines:

```bash
# Install the add/commit filter
git config --global --replace-all filter.reZip.clean "java -cp ~/bin/rezipdoc-*.jar io.github.hoijui.rezipdoc.ReZip"

# (optionally) Install the checkout filter
git config --global --replace-all filter.reZip.smudge "java -cp ~/bin/rezipdoc-*.jar io.github.hoijui.rezipdoc.ReZip"

# (optionally) Install the diff filter
git config --global --replace-all diff.zipDoc.textconv "java -cp ~/bin/rezipdoc-*.jar io.github.hoijui.rezipdoc.ZipDoc"
```

#### 4. Enable the filters

Assign attributes to paths in _<repo-root>/.gitattributes_:

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
