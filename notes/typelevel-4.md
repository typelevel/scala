# Typelevel Scala 4

We are pleased to announce the availability of [Typelevel][typelevel] [Scala][tls] 4 for Scala 2.12.2 and 2.11.11.

## Typelevel Scala releases

As of this release, Typelevel Scala releases are distinguished from the corresponding Lightbend Scala releases by a
version number suffix which indicates the Typelevel feature level beyond the baseline compiler. We are attempting to
maintain parity of Typelevel features across the Scala compiler versions we support.

The current Typelevel feature level is 4 and it is avaliable as a drop in replacement for Lightbend Scala 2.11.11 and
2.12.2.

Support for Scala 2.13.0-M1 will be added in due course. Support for Scala 2.10.6 will be considered if sponsors step
forward to support the necessary work.

## Additonal features included in TLS 4

The Typelevel Scala additions to Lightbend Scala 2.12.2 and 2.11.11 can be found on the branches
[2.12.2-bin-typelevel-4][2.12.2-bin-typelevel-4] and [2.11.11-bin-typelevel-4][2.11.11-bin-typelevel-4] respectively
of this repository.

Typelevel Scala 4 offers the features listed below over Lightbend Scala 2.12.2/2.11.11. Following [Typelevel Scala
policy][merge-policy] each one of these exists as a pull request against Lightbend Scala and is being considered for
merging.

Please report issues with the new features on the [TLS][tls] github repo issue tracker or directly on the
corresponding LBS pull request. Discussion can be found on the TLS [gitter channel][tls-gitter]. More general Scala
compiler development discussion can be found on [`scala/contributors`][lbs-gitter].

**TL;DR** ... [skip ahead](#how-to-use-typelevel-scala-4-with-sbt) to instructions on how to use in your project!

### Type arguments on patterns ([pull/5774][pr-pattern-targs], [@paulp][paulp])

Type arguments are now allowed on patterns. This allows extractors to be parametrised by types which can be used to
materialize implicits which can't be inferred from the static type of the scrutinee. As an example usage, this allows
pattern matches using shapeless's [Typeable][typeable] to be expressed very succinctly,

```
object Typeable {
  // ...

  def unapply[T: Typeable](t: Any): Option[T] = t.cast[T]
  
  // ...
}

// Unsafe match using an erased pattern ...
def unsafeToIntList(x: Any): List[Int] = x match {
  case xs: List[Int] => xs
  case _ => Nil
}

// Safe match using Typeable ...
def safeToIntList(x: Any): List[Int] = x match {
  case Typeable[List[Int]](xs) => xs
  case _ => Nil
}

scala> val ints = List(1, 2, 3)
ints: List[Int] = List(1, 2, 3)

scala> val strings = List("foo", "bar", "baz")
strings: List[String] = List(foo, bar, baz)

scala> unsafeToIntList(ints)
res0: List[Int] = List(1, 2, 3)

scala> unsafeToIntList(strings) // Whoops!
res1: List[Int] = List(foo, bar, baz)

scala> res7(0)
java.lang.ClassCastException: java.lang.String cannot be cast to java.lang.Integer
  at scala.runtime.BoxesRunTime.unboxToInt(BoxesRunTime.java:101)
  ... 39 elided

scala> safeToIntList(ints)
res2: List[Int] = List(1, 2, 3)

scala> safeToIntList(strings)
res3: List[Int] = List()
```

### Faster compilation of inductive implicits ([pull/5649][pr-inductive-implicits], [@milessabin][milessabin])

Enabled by `-Yinduction-heuristics`, this feature dramatically speeds up the compilation of inductive implicit
resolution, such as is found in [shapeless][shapeless]-based generic programming and type class derivation.

The following is an example of the sort of inductive definition which will benefit from this feature,

```scala
@annotation.inductive
trait Selector[L <: HList, U] {
  def apply(l: L): U
}

object Selector {
  def apply[L <: HList, U](implicit selector: Selector[L, U]): Selector[L, U] = selector

  // Base case for induction
  implicit def inHead[H, T <: HList]: Selector[H :: T, H] =
    new Selector[H :: T, H] {
      def apply(l : H :: T) = l.head
    }

  // Induction set: instance requires a smaller instance of the same type class
  implicit def inTail[H, T <: HList, U]
    (implicit st : Selector[T, U]): Selector[H :: T, U] =
      new Selector[H :: T, U] {
        def apply(l : H :: T) = st(l.tail)
      }
}
```

`Selector` extracts elements from values of `HList` types in a type safe way. Without this feature enabled
 materializing instances of this type class are roughly cubic in the length of the `HList`. With this feature
enabled the behaviour is close to linear and with a small constant factor. A benchmark demonstrating a 36x speedup for
selection from a 500 element `HList` can be found [here][induction-benchmark].

One current limitation is that where the induction step is wrapped in shapeless's `Lazy` the induction will not take
advantage of this algorithm. In some cases it may be possible to drop the use of `Lazy`, though not all. Later TLS
releases will provide an interpretation of byname implicit arguments which is equivalent to shapeless's `Lazy` and
which will get along nicely with this induction heuristic.

More details on the [PR][pr-inductive-implicits].

### Minimal kind-polymorphism ([pull/5538][pr-kind-polymorphism], [@mandubian][mandubian])

Enabled by `-Ykind-polymorphism`, it is now possible to write type and method definitions with type parameters of
arbitrary kinds.

The following is an example of a type class which is indexed by types of arbitrary kinds,

```scala
trait Foo[T <: AnyKind] { type Out ; def id(t: Out): Out = t }

object Foo {
  implicit def foo0[T] = new Foo[T] { type Out = T }
  implicit def foo1[T[_]] = new Foo[T] { type Out = T[Any] }
  implicit def foo2[T[_, _]] = new Foo[T] { type Out = T[Any, Any] }
}

def foo[T <: AnyKind](implicit f: Foo[T]): f.type = f

foo[Int].id(23)
foo[List].id(List[Any](1, 2, 3))
foo[Map].id(Map[Any, Any](1 -> "toto", 2 -> "tata", 3 -> "tutu"))
```

The special type `AnyKind` is used as a bound to indicate that a type variable may be instantiated by a type argument
of any kind. Instances of the type class are then provided for types of kind `*`, kind `* -> *` and kind `* -> * -> *`.

More examples can be found [here][kind-poly-examples].

More details on the [PR][pr-kind-polymorphism].

### Configurable default imports ([pull/5350][pr-default-imports], [@andyscott][andyscott])

Enabled by `-Ysysdef` and `-Ypredef`, it is now possible to specify the default imports for a Scala compilation
allowing, for example, an alternative `Predef` to be used.

By default, scalac imports `java.lang._`, `scala._`, and `scala.Predef._` into the namespace of Scala compilation
units. In Lightbend Scala, we have two flags that adjust the behavior of predefined imports.

+ `-Yno-imports` disables all of these.
+ `-Yno-predef` disables `scala.Predef._`.

There are a few other nuances to the behavior of imports. The most important behavior is that `scala.Predef._` is
unimported/evicted if there are any direct leading imports for anything within `scala.Predef`. For example, an import
 of `scala.Predef.DummyImplicit` will cause everything else normally imported by predef to be unimported.

This feature introduces the notion of "sysdefs" and "predefs" for Scala compilation units. Sysdefs are global imports
that are imported at the top of all Scala compilation units under all circumstances. Predefs are imported after
sysdefs and can be masked by top level imports in the source file (to match the existing behavior of directly
importing from scala.Predef).

It adds,

+ `-Ysysdef` which controls the global sysdef imports for all Scala compilation units. The default value is set to
  `java.lang._; scala._`.
+ `-Ypredef` which controls the predef imports for Scala compilation units. The default value is set to
  `scala.Predef._`.

More details on the [PR][pr-default-imports].

### Literal types ([pull/5310][pr-literal-types], [@milesabin][milessabin])

Enabled by `-Yliteral-types`, this implements [SIP-23][sip-23]. Literals can now appear in type position, designating
the corresponding singleton type. A `scala.ValueOf[T]` type class and corresponding `scala.Predef.valueOf[T]` operator
has been added yielding the unique value of types with a single inhabitant. Support for `scala.Symbol` literal types
has been added.

Literal types have many applications in situations where a type can be specified more precisely in terms of a literal
value. The following example defines the type of integers modulo some `M`.

```scala
case class Residue[M <: Int](n: Int) extends AnyVal {
  def +(rhs: Residue[M])(implicit m: ValueOf[M]): Residue[M] =
    Residue((this.n + rhs.n) % valueOf[M])
}

val fiveModTen = Residue[10](5)
val nineModTen = Residue[10](9)

fiveModTen + nineModTen      // OK == Residue[10](4)

val fourModEleven = Residue[11](4)

//fiveModTen + fourModEleven // compiler error: type mismatch;
                             //   found   : Residue[11]
                             //   required: Residue[10]
```

Because the modulus is specified by a literal type it can be checked at compile time and has no runtime storage cost.
Literal types for `String`s and `Symbol`s also greatly simplify type signatures when working with shapeless records.

More details on the [PR][pr-literal-types].

### Exhaustivity of extractors, guards, and unsealed traits ([pull/5617][pr-exhaustivity], [@sellout][sellout])

Enabled by `-Xstrict-patmat-analysis`, the feature provides more accurate reporting of failures of match
exhaustivity for patterns with guards or extractors. Additionally, `-Xlint:strict-unsealed-patmat` will warn on
inexhaustive matches against unsealed traits.

The following will now warn that the match might not be exhaustive,

```scala
def nonExhaustiveIfWeAssumeGuardsFalse(x: Option[Int]): Int = x match {
  case Some(n) if n % 2 == 0 => n
  case None => 0
}
```

More details on the [PR][pr-exhaustivity].

### Infix type printing ([pull/5589][pr-infix-types], [@allisonhb][allisonhb]) &mdash; graduated in LBS 2.12.2

Two-parameter types with symbolic names now print in infix form by default. A `@showAsInfix` annotation has been added
to enable or disable that behaviour explicitly for specific types.

This provides much easier to read type signatures in the Scala REPL and elsewhere with libraries such as shapeless
which make heavy use of infix type constructors,

```scala
scala> import shapeless._
import shapeless._

scala> case class Foo(i: Int, s: String)
defined class Foo

scala> val foo = Foo(23, "foo")
foo: Foo = Foo(23,foo)

scala> Generic[Foo].to(foo)
res0: Int :: String :: shapeless.HNil = 23 :: foo :: HNil
```

More details on the [PR][pr-infix-types].

### Trailing commas ([pull/5245][pr-trailing-commas], [@dwijnand][dwijnand]) &mdash; graduated in LBS 2.12.2

Comma separated lists are now allowed to have a trailing comma. This applies to tuples, argument and parameter groups
and import selectors.

The features makes adding and removing elements from multiline parameter lists a lot less laborious. For example, the
following is now legal,

```scala
trait SimpleExpr1 {
  def f: (Int, String) = (
    23,
    "bar",
  )

  // the Tuple1 value case, the trailing comma is ignored so the type is Int and the value 23
  def g: Int = (
    23,
  )
}

trait TypeArgs {
  class C[A, B]
  def f: C[
    Int,
    String,
  ]
}
```

More examples can be found [here][trailing-comma-examples].

More details on the [PR][pr-trailing-commas].

## How to use Typelevel Scala 4 with SBT

Requirements for using Typelevel Scala in your existing projects,

+ You must be using Lightbend Scala 2.12.2 or 2.11.11.
+ You must be using SBT 0.13.13 or later.
+ Your build should use `scalaOrganization.value` and `CrossVersion.patch` appropriately.

  You can find many examples linked to from [this issue][build-tweaks-1].
+ (Optional) Your build should scope `scalaVersion` to `ThisBuild`.

  You can find an example in [the shapeless build][build-tweaks-2],

  ```
  inThisBuild(Seq(
    organization := "com.chuusai",
    scalaVersion := "2.12.2",
    crossScalaVersions := Seq("2.10.6", "2.11.8", "2.12.2", "2.13.0-M1")
  ))
  ```

You can now temporarily build with Typelevel Scala by entering,

```
> ; set every scalaOrganization := "org.typelevel" ; ++2.12.2-bin-typelevel-4
```

on the SBT REPL.

To switch your project permanently to Typelevel Scala 4 update your `build.sbt` as follows,

```
inThisBuild(Seq(
  scalaOrganization := "org.typelevel"
  scalaVersion := "2.12.2-bin-typelevel-4"
))
```

Alternatively, if you want to try Typelevel Scala without modifying your `build.sbt` and you have scoped
`scalaVersion` as shown earlier you can instead create a file `local.sbt` at the root of your project with the
following content,

```
inThisBuild(Seq(
  scalaOrganization := "org.typelevel",
  scalaVersion      := "2.11.11-bin-typelevel-4"
))
```

These settings will override the `ThisBuild` scoped settings in your `build.sbt`.

Whichever method you choose, your build should now function as before but using the Typelevel Scala toolchain instead
of the Lightbend one. You can verify that the settings have been updated correctly from the SBT prompt using `show`,

```
> show scalaOrganization
[info] org.typelevel
> show scalaVersion
[info] 2.12.2-bin-typelevel-4
```

[build-tweaks-1]: https://github.com/typelevel/scala/issues/135
[build-tweaks-2]: https://github.com/milessabin/shapeless/blob/master/build.sbt#L13-L17

## How to use Typelevel Scala 4 with Maven

If you are using maven with the `scala-maven-plugin`, set the `<scalaOrganization>` to `org.typelevel`,

```
<plugin>
  <groupId>net.alchim31.maven</groupId>
  <artifactId>scala-maven-plugin</artifactId>
  <version>3.2.1</version>
  <configuration>
    <scalaOrganization>org.typelevel</scalaOrganization>
    <scalaVersion>2.12.2-bin-typelevel-4</scalaOrganization>
  </configuration>
</plugin>
```

## Contributors

Typelevel Scala is only possible because of the contributions made to Lightbend Scala &mdash; see the contributors
section of the LBS Scala 2.12.2 and 2.11.11[release notes][lbs-2.12.2].

We are hugely grateful to the following for their work on the enhancements included in Typelevel Scala 4 and look
forward to them being included in future Lightbend Scala releases,

+ Allison H. <allisonhb@gmx.com> [@allisonhb][allisonhb]
+ Andy Scott <andy.g.scott@gmail.com> [@andyscott][andyscott]
+ Dale Wijnand <dale.wijnand@gmail.com> [@dwijnand][dwijnand]
+ Greg Pfeil <greg@technomadic.org> [@sellout][sellout]
+ Miles Sabin <miles@milessabin.com> [@milessabin][milessabin]
+ Pascal Voitot <pascal.voitot.dev@gmail.com> [@mandubian][mandubian]
+ Paul Phillips <paulp@improving.org> [@paulp][paulp]

We would also like to thank Adriaan Moors, George Leontiev, Jason Zaugg and Vlad Ureche for their contributions to
Typelevel Scala in general and to the PRs merged in this release in particular.

[allisonhb]: https://github.com/allisonhb
[andyscott]: https://github.com/andyscott
[dwijnand]: https://github.com/dwijnand
[sellout]: https://github.com/sellout
[milessabin]: https://github.com/milessabin
[mandubian]: https://github.com/mandubian
[paulp]: https://github.com/paulp
[pr-infix-types]: https://github.com/scala/scala/pull/5589
[pr-default-imports]: https://github.com/scala/scala/pull/5350
[pr-trailing-commas]: https://github.com/scala/scala/pull/5245
[pr-exhaustivity]: https://github.com/scala/scala/pull/5617
[pr-literal-types]: https://github.com/scala/scala/pull/5310
[pr-inductive-implicits]: https://github.com/scala/scala/pull/5649
[pr-kind-polymorphism]: https://github.com/scala/scala/pull/5538
[pr-pattern-targs]: https://github.com/scala/scala/pull/5774
[merge-policy]: https://github.com/typelevel/scala#relationship-with-lightbend-scala
[tls]: https://github.com/typelevel/scala
[lbs]: https://github.com/scala/scala
[tls-gitter]: https://gitter.im/typelevel/scala
[lbs-gitter]: https://gitter.im/scala/contributors
[typelevel]: http://typelevel.org/
[lbs-readme]: https://github.com/scala/scala/blob/2.12.x/README.md
[2.12.2-bin-typelevel-4]: https://github.com/typelevel/scala/commits/2.12.2-bin-typelevel-4
[2.11.11-bin-typelevel-4]: https://github.com/typelevel/scala/commits/2.11.11-bin-typelevel-4
[lbs-2.12.2]: https://www.scala-lang.org/news/releases-1Q17.html
[sip-23]: http://docs.scala-lang.org/sips/pending/42.type.html
[shapeless]: https://github.com/milessabin/shapeless
[induction-benchmark]: https://github.com/typelevel/scala/blob/2.12.2-bin-typelevel-4/test/induction/inductive-implicits-bench.scala
[kind-poly-examples]: https://github.com/typelevel/scala/blob/2.12.2-bin-typelevel-4/test/files/pos/kind-poly.scala
[trailing-comma-examples]: https://github.com/typelevel/scala/blob/2.12.2-bin-typelevel-4/test/files/pos/trailing-commas.scala
[typeable]: https://github.com/milessabin/shapeless/commit/820f12c678a7c1bb2bfb98c201db207cc7fc81de
