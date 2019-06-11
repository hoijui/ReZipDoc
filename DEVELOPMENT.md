# Developer instructions

This file contains info relevant in the development process,
and is generally uninteresting for users.

---

__NOTE__

Make sure you are using the right Maven and JDK versions when releasing.
For example, if you use JDK 6, software using JDK 8+ will not be able
to use your artifact due to incompatible byte-code.
The same applies vice versa.

---

## Release a SNAPSHOT

Here we release a development version to the Sonatype snapshot repository only.

```bash
mvn clean deploy
```

## Release

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
			<gpg.keyname>First-name Last-name (Comment) &lt;user@email.org&gt;</gpg.keyname>
		</properties>
	</profile>
</profiles>
```

If you have not yet done so, generate and publish a key-pair.
See [the Sonatype guide](http://central.sonatype.org/pages/working-with-pgp-signatures.html)
for further details about how to work with GPG keys.

### Perform the release

Before starting the actual release process,
we check whether everything is in order:
The code compiles, packaging goes well, unit tests pass,
the signing works, and the site is generated without errors.

```bash
mvn \
	clean \
	package \
	verify \
	gpg:sign \
	site
```

If the above command finished without errors,
we are ready to run a test release run ("dry"):

```bash
mvn release:clean
mvn \
	-DdryRun=true \
	release:prepare
```

If that also went fine, we prepare the release locally, for real now:

---

__NOTE__

This is where using the right JDK version is important!

You might want to do that in this way:

```bash
# open a temporary shell, to not spill the changes in env vars
bash
# set env vars
export JAVA_HOME="/usr/lib/jvm/java-8-openjdk-amd64"
export PATH="${JAVA_HOME}/bin/:${PATH}"
```

Then perform the steps below,
and afterwards `exit` from the temporary shell again.

---

```bash
# run the prepare phase for real
mvn release:clean
mvn \
	-DdryRun=false \
	release:prepare
```

This does the following:

* asks for the release and new snapshot versions to use (for all modules)
* package the release JARs
* signs with GPG
* creates the release and post-release commits
* tags the release

Now we publish the release to the public, to Maven Central.
This will take quite some time,
and the second line might end with an error
while waiting for the promoting to finish,
which you can ignore.

Make sure to set `release_tag` to the actual tag.

```bash
release_tag="rezipdoc-0.2"
git push origin master "$release_tag"
mvn release:perform
mvn deploy
```

This does the following:

* pushes to origin
* checks-out the release tag
* builds
* deploy into Sonatype staging repository
* promote it on Maven Central repository
* release a SNAPSHOT with the new version

This last step should take no more then 4h.

... done! :-)

If all has gone well, you should be able to
[find the release on Maven Central](https://search.maven.org/search?q=g:io.github.hoijui.rezipdoc).
