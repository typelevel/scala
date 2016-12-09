---
layout: page
order: 2
title: Typelevel Scala releases
site_nav_entry: true # this is an entry in the main site nav
---

Currently Typelevel Scala is available as a drop in replacement for Lightbend Scala 2.11.8. As soon as Lightbend Scala 2.12.0-RC1 is published a corresponding release of Typelevel Scala will be published.

## Typelevel Scala 2.11.8

The Typelevel Scala additions to Lightbend Scala 2.11.8 can be found on the branch [2.11.8-bin-typelevel](https://github.com/typelevel/scala/commits/2.11.8-bin-typelevel) of this repository.

Typelevel Scala 2.11.8 offers the following fixes and features over Lightbend Scala 2.11.8,

* Support for partial unification (ie. a fix for [SI-2712](https://issues.scala-lang.org/browse/SI-2712)) — merged in Lightbend Scala 2.12.0-RC1.

	An improvement to type inference for type constructors, enabled by the `-Ypartial-unification` scalac option. This has many benefits for libraries, such as Cats and Scalaz, which make extensive use of higher-kinded types.

	Full details on the [pull request](https://github.com/scala/scala/pull/5102).

* Support for literal types (aka SIP-23) — proposed for Lightbend Scala 2.12.1.

	Implements [literal types](https://github.com/scala/scala/pull/5310). Enabled by `-Yliteral-types.`

	Literals can now appear in type position, designating the corresponding singleton type. A `scala.ValueOf[T]` type class and corresponding `scala.Predef.valueOf[T]` operator has been added yielding the unique value of types with a single inhabitant. Support for `scala.Symbol` literal types has been added.

* A partial fix for SI-7046 — proposed for Lightbend Scala 2.12.1.

	The macro API call `knownDirectSubclasses` now yields the correct result in most cases and will report an error in cases where it is unable to yield the correct result.

	This is only a partial fix because subclasses defined in local scopes might be missed by `knownDirectSubclasses`. In mitigation it is very likely that a local subclass would represent an error in any scenario where `knownDirectSubclasses` might be used. An error will be reported in these cases.

	Full details on the [pull request](https://github.com/scala/scala/pull/5284).

* A fix for SI-9760 — merged in Lightbend Scala 2.12.0-RC1.

	Higher kinded type arguments are now refined by GADT pattern matching. Details can be found on [the ticket](https://issues.scala-lang.org/browse/SI-9760).
	
	
## Typelevel Scala 2.12.0

The Typelevel Scala additions to Lightbend Scala 2.12.0 can be found on the branch [2.12.0-bin-typelevel](https://github.com/typelevel/scala/commits/2.12.0-bin-typelevel) of this repository.

Typelevel Scala 2.12.0 offers the following fixes and features over Lightbend Scala 2.12.0,

* Support for literal types (aka SIP-23) — proposed for Lightbend Scala 2.12.1.

	Implements [literal types](https://github.com/scala/scala/pull/5310). Enabled by ``-Yliteral-types``.

	Literals can now appear in type position, designating the corresponding singleton type. A `scala.ValueOf[T]` type class and corresponding `scala.Predef.valueOf[T]` operator has been added yielding the unique value of types with a single inhabitant. Support for `scala.Symbol` literal types has been added.

* A partial fix for SI-7046 — proposed for Lightbend Scala 2.12.1.

	The macro API call `knownDirectSubclasses` now yields the correct result in most cases and will report an error in cases where it is unable to yield the correct result.

	This is only a partial fix because subclasses defined in local scopes might be missed by `knownDirectSubclasses`. In mitigation it is very likely that a local subclass would represent an error in any scenario where `knownDirectSubclasses` might be used. An error will be reported in these cases.

	Full details on the [pull request](https://github.com/scala/scala/pull/5284).

The following have already been merged in Lightbend Scala 2.12.x and so are included here automatically,

* Support for partial unification (ie. a fix for [SI-2712](https://issues.scala-lang.org/browse/SI-2712)) — merged in Lightbend Scala 2.12.0-RC1.

	An improvement to type inference for type constructors, enabled by the `-Ypartial-unification` scalac option. This has many benefits for libraries, such as Cats and Scalaz, which make extensive use of higher-kinded types.

	Full details on the [pull request](https://github.com/scala/scala/pull/5102).

* A fix for SI-9760 — merged in Lightbend Scala 2.12.0-RC1.

	Higher kinded type arguments are now refined by GADT pattern matching. Details can be found on [the ticket](https://issues.scala-lang.org/browse/SI-9760).
	

If you are interested, you are invited to read our [Contributing Guide](/contributing) and jump in.
