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

resolvers += Resolver.mavenLocal
```
