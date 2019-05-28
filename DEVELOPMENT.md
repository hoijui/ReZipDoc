# Developer instructions

This file contains info relevant in the development process,
and is generally uninteresting for users.

## Release a SNAPSHOT

To release a development version to the Sonatype snapshot repository only:

```bash
# open a "private" shell, to not spill the changes in env vars
bash
# set env vars
export JAVA_HOME="${JAVA_8_HOME}"
export PATH="${JAVA_HOME}/bin/:${PATH}"
# do the release
mvn clean deploy
# leave our "private" shell instance again
exit
```

## Release

### Prepare "target/" for the release process

```bash
mvn release:clean
```

### Setup for signing the release

To be able to sign the release artifacts,
make sure you have a section in your `~/.m2/settings.xml` that looks like this:

```xml
<profiles>
	<profile>
		<id>ossrh</id>
		<activation>
			<activeByDefault>true</activeByDefault>
		</activation>
		<properties>
			<gpg.executable>gpg2</gpg.executable>
			<!--
				This needs to match the `uid` as displayed by `gpg2 --list-keys`,
				and needs to be XML escaped.
			-->
			<gpg.keyname>Firstname Lastname (Comment) &lt;user@email.org&gt;</gpg.keyname>
		</properties>
	</profile>
</profiles>
```

If you have not yet done so, generate and publish a key-pair.
See [the Sonatype guide](http://central.sonatype.org/pages/working-with-pgp-signatures.html)
for further details about how to work with GPG keys.

### Prepare the release

```bash
# open a "private" shell, to not spill the changes in env vars
bash
# set env vars
export JAVA_HOME="${JAVA_8_HOME}"
export PATH="${JAVA_HOME}/bin/:${PATH}"
# check if everything is in order
mvn \
	clean \
	package \
	verify \
	gpg:sign \
	site
mvn release:clean
mvn \
	-DdryRun=true \
	release:prepare
# run the prepare phase for real
mvn release:clean
mvn \
	-DdryRun=false \
	release:prepare
# leave our "private" shell instance again
exit
```

This does the following:

* _Important for backwards compatibility_:
use the oldest possible JDK version to compile (currently 1.8)
* asks for the release and new snapshot versions to use (for all modules)
* packages
* signs with GPG
* commits
* tags

### Perform the release (main part)

```bash
# open a "private" shell, to not spill the changes in env vars
bash
# set env vars
export JAVA_HOME="${JAVA_8_HOME}"
export PATH="${JAVA_HOME}/bin/:${PATH}"
# perform the release
git push origin master <release-tag>
mvn release:perform
mvn deploy -P release
# leave our "private" shell instance again
exit
```

This does the following:

* pushes to origin
* checks-out the release tag
* builds
* deploy into Sonatype staging repository
* promote it on Maven Central repository (may have a delay of up to 4h)

