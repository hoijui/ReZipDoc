# ReZipDoc = ReZip & ZipDoc

* __ReZip__ For more efficient Git packing of ZIP based files
* __ZipDoc__ A Git `textconv` program to dump a ZIP files contents as text to stdout

[![License](https://img.shields.io/badge/license-GPL%203-orange.svg)](https://www.gnu.org/licenses/gpl-3.0.en.html)
[![GitHub last commit](https://img.shields.io/github/last-commit/hoijui/ReZipDoc.svg)](https://github.com/hoijui/ReZipDoc)
[![Issues](https://img.shields.io/badge/issues-GitHub-red.svg)](https://github.com/hoijui/ReZipDoc/issues)

for branch `master`:
[![Build Status](https://travis-ci.org/hoijui/ReZipDoc.svg?branch=master)](https://travis-ci.org/hoijui/ReZipDoc)

## Installation

This program requires Java JRE 8 or newer.

### 1. Build the JAR

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

### 2. Install the JAR

Store _rezipdoc-\*.jar_ somewhere locally, either:

 * (global) in your home directory, for example under _~/bin/_
 * (repo) or in your repository, for example under _<repo-root>/tools/_

### 3. Install the Filter(s)

Either in:

* (global) _${HOME}/.gitattributes_
* (repo) _<repo-root>/.gitattributes_

add these lines:

```bash
# Install the add/commit filter
git config --global --replace-all filter.reZip.clean "java -cp ~/bin/rezipdoc-*.jar net.rezipdoc.ReZip"

# (optionally) Install the checkout filter
git config --global --replace-all filter.reZip.smudge "java -cp ~/bin/rezipdoc-*.jar net.rezipdoc.ReZip"

# (optionally) Install the diff filter
git config --global --replace-all diff.zipDoc.textconv "java -cp ~/bin/rezipdoc-*.jar net.rezipdoc.ZipDoc"
```

### 4. Enable the filters

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
# This combines in-history uncompression and uncompressed view of ZIP files
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
even if it is obtained without going through a re-packing-with-compression filter.

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
_\*.png_ files in the presentation which were already stored without compression in the original _\*.pptx_.
