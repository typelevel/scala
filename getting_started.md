---
layout: page
order: 1
title: How to use Typelevel Scala
site_nav_entry: true # this is an entry in the main site nav
---

There are just two requirements for using Typelevel Scala in your existing projects,

* You must be using (or be able to switch to) a corresponding version of Lightbend Scala. Currently this is 2.11.8 and 2.12.0.
* You must be using (or be able to switch to) SBT 0.13.13 or later. Earlier versions of SBT don’t have full support for using an alternative `scalaOrganization`.

If you are using Lightbend Scala 2.11.8 or 2.12.0 and SBT 0.13.13 the following steps will build your project with Typelevel Scala,

* Update your `project/build.properties` to require SBT 0.13.13,

	```scala
	sbt.version=0.13.13
	```

* Add the following to your `build.sbt` immediately next to where you set `scalaVersion`,

	```scala
	scalaOrganization in ThisBuild := "org.typelevel"
	```

	Alternatively, if you want to try Typelevel Scala without modifying your `build.sbt` you can instead create a file `local.sbt` at the root of your project with the following content,

	```scala
	scalaOrganization in ThisBuild := "org.typelevel"
	```
	
	This will be merged with your main build definitions and can be added to `.gitignore` or `.git/info/exclude` if so desired.


Now your build should function as before but using the Typelevel Scala toolchain instead of the Lightbend one. You can verify this from the SBT prompt,

```scala
> show scalaOrganization
[info] org.typelevel
>
```

This will immediately provide you with the fixes for [SI-7046](https://issues.scala-lang.org/browse/SI-7046) and [SI-9760](https://issues.scala-lang.org/browse/SI-9760). To additionally enable the the fix for [SI-2712](https://issues.scala-lang.org/browse/SI-2712) and the implementation of [SIP-23](https://github.com/scala/scala/pull/5310) you should add either or both of their enabling flags to `scalacOptions` (or `local.sbt` if that was the path you took earlier),

```scala
scalacOptions += "-Ypartial-unification" // enable fix for SI-2712
scalacOptions += "-Yliteral-types"       // enable SIP-23 implementation
```

Note that Typelevel Scala 2.11.8 replaces the [si2712fix compiler plugin](https://github.com/milessabin/si2712fix-plugin) — if you are using it you should remove it from your build before switching to Typelevel Scala.

Also note that the two compiler flags above should be used in preference to `-Xexperimental` at present — as well as enabling the above two features `-Xexperimental` also enables some other features which are not typically desirable.

You now can verify that these features have been enabled from the SBT console,

```scala
> console
[info] Compiling 3 Scala sources to /home/miles/projects/value-wrapper/target/scala-2.11/classes...
[info] Starting scala interpreter...
[info] 
Welcome to Scala 2.11.8 (Java HotSpot(TM) 64-Bit Server VM, Java 1.8.0_102).
Type in expressions for evaluation. Or try :help.

scala> val foo: "foo" = "foo" // Use of literal type "foo"
foo: "foo" = foo

scala> import scala.language.higherKinds
import scala.language.higherKinds

scala> def foo[F[_], A](fa: F[A]): String = fa.toString
foo: [F[_], A](fa: F[A])String

scala> foo((x: Int) => x*2)   // Function1[Int, Int] unifies with F[_]
res1: String = <function1>
```

## Try Typelevel Scala with an Ammonite instant REPL

The quickest way to get to a Typelevel Scala 2.11.8 REPL path is to run the provided [“try Typelevel Scala”](https://github.com/typelevel/scala/blob/typelevel-readme/try-typelevel-scala.sh) script, which has no dependencies other than an installed JDK. This script downloads and installs [coursier](https://github.com/alexarchambault/coursier) and uses it to fetch the [Ammonite REPL](https://github.com/lihaoyi/Ammonite) and Typelevel Scala 2.11.8. It then drops you immediately into a REPL session,

```scala
% curl -s https://raw.githubusercontent.com/typelevel/scala/typelevel-readme/try-typelevel-scala.sh | bash
Loading...
Compiling predef.sc
Compiling SharedPredef.sc
Compiling LoadedPredef.sc
Welcome to the Ammonite Repl 0.7.8
(Scala 2.11.8 Java 1.8.0_112)
@ repl.compiler.settings.YliteralTypes.value = true

@ trait Cond[T] { type V ; val value: V }
defined trait Cond
@
@ implicit val condTrue = new Cond[true] { type V = String ; val value = "foo" }
condTrue: AnyRef with Cond[true]{type V = String} = $sess.cmd2$$anon$1@22265a2d
@ implicit val condFalse = new Cond[false] { type V = Int ; val value = 23 }
condFalse: AnyRef with Cond[false]{type V = Int} = $sess.cmd3$$anon$1@606ab048
@
@ def cond[T](implicit cond: Cond[T]): cond.V = cond.value
defined function cond
@
@ cond[true] : String
res8: String = "foo"
@ cond[false] : Int
res7: Int = 23
@ Bye!
%
```

If you are interested, you are invited to read our [Contributing Guide](/contributing) and jump in.


