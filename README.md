# Typelevel Scala

[![Build Status](http://typelevel-ci.orexio.org/job/typelevel-scala-master/badge/icon)](http://typelevel-ci.orexio.org/job/typelevel-scala-master/) [![Stories in Ready](https://badge.waffle.io/typelevel/scala.svg?label=ready&title=Ready)](http://waffle.io/typelevel/scala)

This is the repository for the [Typelevel fork](http://typelevel.org/blog/2014/09/02/typelevel-scala.html) of the Scala compiler.

  - [Report an issue](https://github.com/typelevel/scala/issues);
  - ... and contribute right here! Please, first read our [development guidelines](CONTRIBUTING.md),

## Requirements

The Typelevel fork is conservative, so that it can present the most
simple migration path possible. This means:

1. Any project *source* that compiles and runs under `scalac` should compile and run under `tlc` with the same set of flags. Two differences are allowed: warnings issued, and classpath (e.g. `tlc` may require some classpath entries that `scalac` does not, or vice versa)
2. Sources that compile under both `scalac` and `tlc` with the same flags should have the same semantics.
3. Any project *binaries* produced by `tlc` should load cleanly from a `scalac` project and should be callable *without* FFI. In other words, `tlc` should produce *Scala* APIs. The exact bytecode metadata may be distinct, so long as it is compatible.

Incompatible changes will be accepted, but only when under an
additional compiler flag prefixed with `-Z` so that users can opt-in
to the changed behaviour.

You can read a more detailed [Compatibility Guide](https://github.com/typelevel/scala/wiki/Typelevel-Scala-Compatibility-Guide)
on the wiki.

## Features

A comprehensive list of features and differences is [listed on the wiki](https://github.com/typelevel/scala/wiki/Differences).

## Publishing Locally

We do not currently have a release but you can build and publish a
version locally:

```sh
ant publish-local-opt -Dmaven.version.suffix="-typelevel"
```

Update your `build.sbt` with:

```scala
scalaVersion := "2.11.3-typelevel"

libraryDependencies += "org.scala-lang" % "scala-typelevel" % scalaVersion.value

  - Run `ant init` to download some necessary jars.
  - Import the project (in `src/eclipse`) via `File` → `Import Existing Projects into Workspace`. Check all projects and click ok.

For important details on building, debugging and file encodings, please see [the excellent tutorial on scala-ide.org](http://scala-ide.org/docs/tutorials/scalac-trunk/index.html) and the included README.md in src/eclipse.

### IntelliJ 14
Use the latest IntelliJ IDEA release and install the Scala plugin from within the IDE.

The following steps are required to use IntelliJ IDEA on Scala trunk
 - Run `ant init`. This will download some JARs to `./build/deps`, which are included in IntelliJ's classpath.
 - Run `./src/intellij/setup.sh`.
 - Open `./src/intellij/scala.ipr` in IntelliJ.
 - `File` → `Project Structure` → `Project` → `Project SDK`. Create an SDK entry named "1.6" containing the Java 1.6 SDK.
   (You may use a later SDK for local development, but the CI will verify against Java 6.)

Compilation within IDEA is performed in `-Dlocker.skip=1` mode: the sources are built
directly using the STARR compiler (which is downloaded from [the Central Repository](http://central.sonatype.org/), according to `starr.version` in `versions.properties`).

## Building with sbt (EXPERIMENTAL)

The experimental sbt-based build definition has arrived! Run `sbt package`
to build the compiler. You can run `sbt test` to run unit (JUnit) tests.
Use `sbt test/it:test` to run integration (partest) tests.

We would like to migrate to sbt build as quickly as possible. If you would
like to help please use the scala-internals mailing list to discuss your
ideas and coordinate your effort with others.

## Building with Ant

NOTE: we are working on migrating the build to sbt.

Run `ant build-opt` to build an optimized version of the compiler.
Verify your build using `ant test-opt`.

The Scala build system is based on Apache Ant. Most required pre-compiled
libraries are part of the repository (in 'lib/'). The following however is
assumed to be installed on the build machine: TODO

### Ant Tips and tricks

Here are some common commands. Most ant targets offer a `-opt` variant that runs under `-optimise` (CI runs the -optimize variant).

Command                 | Description
----------------------- | -----------
`./pull-binary-libs.sh` | downloads all binary artifacts associated with this commit.
`ant -p`                | prints out information about the commonly used ant targets.
`ant` or `ant build`    | A quick compilation (to `build/quick`) of your changes using the locker compiler.
`ant dist`              | builds a distribution in 'dists/latest'.
`ant all.clean`         | removes all build files and all distributions.

A typical debug cycle incrementally builds quick, then uses it to compile and run the file
`sandbox/test.scala` as follows:

  - `ant && build/quick/bin/scalac -d sandbox sandbox/test.scala && build/quick/bin/scala -cp sandbox Test`

We typically alias `build/quick/bin/scalac -d sandbox` to `qsc` and `build/quick/bin/scala -cp sandbox` to `qs` in our shell.

`ant test-opt` tests that your code is working and fit to be committed:

  - Runs the test suite and bootstrapping test on quick.
  - You can run the suite only (skipping strap) with `ant test.suite`.

`ant docs` generates the HTML documentation for the library from the sources using the scaladoc tool in quick.
Note: on most machines this requires more heap than is allocated by default.  You can adjust the parameters with `ANT_OPTS`. Example command line:

```sh
ANT_OPTS="-Xms512M -Xmx2048M -Xss1M -XX:MaxPermSize=128M" ant docs
```
