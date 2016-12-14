---
layout: docs
title: Compiler Phases
---

The Scala compiler has to do a lot of work to turn your code into jvm bytecode that can be executed. For several reasons, this work is broken into steps which are executed sequentially, the so called *phases* of the compiler.

## The Phases

If you want to see the phases listed, simply run in your terminal:

```scala
$ scalac -Xshow-phases
Picked up JAVA_TOOL_OPTIONS: -Dfile.encoding=UTF-8

    phase name  id  description
    ----------  --  -----------
        parser   1  parse source into ASTs, perform simple desugaring
         namer   2  resolve names, attach symbols to named trees
packageobjects   3  load package objects
         typer   4  the meat and potatoes: type the trees
        patmat   5  translate match expressions
superaccessors   6  add super accessors in traits and nested classes
    extmethods   7  add extension methods for inline classes
       pickler   8  serialize symbol tables
     refchecks   9  reference/override checking, translate nested objects
       uncurry  10  uncurry, translate function values to anonymous classes
     tailcalls  11  replace tail calls by jumps
    specialize  12  specialized-driven class and method specialization
 explicitouter  13  this refs to outer pointers
       erasure  14  erase types, add interfaces for traits
   posterasure  15  clean up erased inline classes
      lazyvals  16  allocate bitmaps, translate lazy vals into lazified defs
    lambdalift  17  move nested functions to top level
  constructors  18  move field definitions into constructors
       flatten  19  eliminate inner classes
         mixin  20  mixin composition
       cleanup  21  platform-specific cleanups, generate reflective calls
    delambdafy  22  remove lambdas
         icode  23  generate portable intermediate code
           jvm  24  generate JVM bytecode
      terminal  25  the last phase during a compilation run
```

As you can see above, at the time of this writing the compiler uses 25 phases that start with a *parser* of the code and end with the *terminal* phase, that ends the compilation.

Please note that the reality of the compiler is not as clean as this documentation would have you believe. Due to efficiency and other concerns, some phases are intertwined and they should be considered a single phase in itself. In other cases some phases do more than advertised.

### Phases in the Compiler

If you open your compiler codebase and go to class `Global`, line 405, you will see:

```scala
 // phaseName = "parser"
  lazy val syntaxAnalyzer = new {
    val global: Global.this.type = Global.this
  } with SyntaxAnalyzer {
    val runsAfter = List[String]()
    val runsRightAfter = None
    override val initial = true
  }
```

This corresponds to the first phase of the compiler, `parser`. All the phases are defined as `lazy val`, in order, after this entry. 

If you want to learn more about a specific phase you can always go to the class implementing it. In the example above, that would be `SyntaxAnalyzer`. Most of the phases have documentation that explains the function of the phase.

Some things that may not be obvious from reading the documentation of the phases, but are relevant:

* `parser`, the very first phase, tackles a lot of syntactic sugar. For example, at the end of this phase *for-comprehensions* will have been transformed into a series of *flatMap*, *map*, and *filter*.
* `namer`, `packageobjects`, and `typer` (phases 2,3, and 4) are effectively a single phase. Although split into 3 elements for implementation reasons, the dependencies between them means you can consider it one.
* After `typer` (phase 4) completes, all the typechecking has been completed. For example, any error due to Type classes missing an implicit implementation will happen at this stage. The remaining 20 phases work with code that type-checks. Guaranteed ;)
* `pickler` (phase 8) generates attributes for class files which are later on used for compilation against binaries. This is what allows you to use a certain jar file as a library without having to bring the source code of the library along.
* `uncurry` (phase 10) turns functions (like `val f: Int => Int`) to anonymous classes. In JVM 8 this benefits from new structures introduced to work with lambdas.
* `lambalift` (phase 17) lifts nested functions outside of methods, a different task that `uncurry`
* `constructors` (phase 18) generates the constructors for the classes. Keep in mind Scala constructors are very different to the ones expected by the jvm (for instance any expression in the body of a class is executed during construction) so there' quite a bit going on in this phase.

## Example

If we talk about phases we want to mention the `-Xprint` flag in `scalac`. This flag allows you to see the differences between phases.

To test it, create a file with the following code:

```scala
class Foo{
  val i = 23
  val j = "blah"
  val k = i+j

  def wibble = {
    for(c <- k) yield c*2
  }
}
```

and compile it with:

```bash
$ scalac -Xprint:all <youfile>.scala
```

You will see the compiler outputs the code as it's seen after each phase that modified something. For example the output will show that after the parser phase the output looks like:

```scala
[[syntax trees at end of                    parser]] // sample.scala
package <empty> {
  class Foo extends scala.AnyRef {
    def <init>() = {
      super.<init>();
      ()
    };
    val i = 23;
    val j = "blah";
    val k = i.$plus(j);
    def wibble = k.map(((c) => c.$times(2)))
  }
}
```

You can notice the class is now inside a package `empty` as we didn't declare any package. You can see a method `<init>` that acts like a pseudo-constructor has been added, the `+` operation assigned to `k` has been expanded to a method `$plus`, and the for comprehension of `wibble` has been expanded to a `map` call.
	
Quite a lot has changed in just the first phase :) After typing finishes we get the following output:

```scala
[[syntax trees at end of                     namer]] // sample.scala: tree is unchanged since parser
[[syntax trees at end of            packageobjects]] // sample.scala: tree is unchanged since parser
[[syntax trees at end of                     typer]] // sample.scala
package <empty> {
  class Foo extends scala.AnyRef {
    def <init>(): Foo = {
      Foo.super.<init>();
      ()
    };
    private[this] val i: Int = 23;
    <stable> <accessor> def i: Int = Foo.this.i;
    private[this] val j: String = "blah";
    <stable> <accessor> def j: String = Foo.this.j;
    private[this] val k: String = Foo.this.i.+(Foo.this.j);
    <stable> <accessor> def k: String = Foo.this.k;
    def wibble: scala.collection.immutable.IndexedSeq[Int] = scala.this.Predef.augmentString(Foo.this.k).map[Int, scala.collection.immutable.IndexedSeq[Int]](((c: Char) => c.*(2)))(scala.this.Predef.fallbackStringCanBuildFrom[Int])
  }
}
```

The main difference is that types are assigned. For example our `val i` has type `Int`, as do the other `val` of `def` in the class. We can also see the synthetic methods added to access the values of `i`, `j`, and `k`. 

You can experiment around with your own files to see changes in the output of `scalac`. A warning though, avoid using `extends App` constructs as the compiler treats them in a specific way and that may clutter your output.

The full output for the example above follows:

```scala
 scalac -Xprint:all sample.scala 
Picked up JAVA_TOOL_OPTIONS: -Dfile.encoding=UTF-8
[[syntax trees at end of                    parser]] // sample.scala
package <empty> {
  class Foo extends scala.AnyRef {
    def <init>() = {
      super.<init>();
      ()
    };
    val i = 23;
    val j = "blah";
    val k = i.$plus(j);
    def wibble = k.map(((c) => c.$times(2)))
  }
}

[[syntax trees at end of                     namer]] // sample.scala: tree is unchanged since parser
[[syntax trees at end of            packageobjects]] // sample.scala: tree is unchanged since parser
[[syntax trees at end of                     typer]] // sample.scala
package <empty> {
  class Foo extends scala.AnyRef {
    def <init>(): Foo = {
      Foo.super.<init>();
      ()
    };
    private[this] val i: Int = 23;
    <stable> <accessor> def i: Int = Foo.this.i;
    private[this] val j: String = "blah";
    <stable> <accessor> def j: String = Foo.this.j;
    private[this] val k: String = Foo.this.i.+(Foo.this.j);
    <stable> <accessor> def k: String = Foo.this.k;
    def wibble: scala.collection.immutable.IndexedSeq[Int] = scala.this.Predef.augmentString(Foo.this.k).map[Int, scala.collection.immutable.IndexedSeq[Int]](((c: Char) => c.*(2)))(scala.this.Predef.fallbackStringCanBuildFrom[Int])
  }
}

[[syntax trees at end of                    patmat]] // sample.scala: tree is unchanged since typer
[[syntax trees at end of            superaccessors]] // sample.scala: tree is unchanged since typer
[[syntax trees at end of                extmethods]] // sample.scala: tree is unchanged since typer
[[syntax trees at end of                   pickler]] // sample.scala: tree is unchanged since typer
[[syntax trees at end of                 refchecks]] // sample.scala: tree is unchanged since typer
[[syntax trees at end of                   uncurry]] // sample.scala
package <empty> {
  class Foo extends Object {
    def <init>(): Foo = {
      Foo.super.<init>();
      ()
    };
    private[this] val i: Int = 23;
    <stable> <accessor> def i(): Int = Foo.this.i;
    private[this] val j: String = "blah";
    <stable> <accessor> def j(): String = Foo.this.j;
    private[this] val k: String = Foo.this.i().+(Foo.this.j());
    <stable> <accessor> def k(): String = Foo.this.k;
    def wibble(): scala.collection.immutable.IndexedSeq[Int] = scala.this.Predef.augmentString(Foo.this.k()).map[Int, scala.collection.immutable.IndexedSeq[Int]]({
      @SerialVersionUID(value = 0) final <synthetic> class $anonfun extends scala.runtime.AbstractFunction1[Char,Int] with Serializable {
        def <init>(): <$anon: Char => Int> = {
          $anonfun.super.<init>();
          ()
        };
        final def apply(c: Char): Int = c.*(2)
      };
      (new <$anon: Char => Int>(): Char => Int)
    }, scala.this.Predef.fallbackStringCanBuildFrom[Int]())
  }
}

[[syntax trees at end of                 tailcalls]] // sample.scala: tree is unchanged since uncurry
[[syntax trees at end of                specialize]] // sample.scala: tree is unchanged since uncurry
[[syntax trees at end of             explicitouter]] // sample.scala
package <empty> {
  class Foo extends Object {
    def <init>(): Foo = {
      Foo.super.<init>();
      ()
    };
    private[this] val i: Int = 23;
    <stable> <accessor> def i(): Int = Foo.this.i;
    private[this] val j: String = "blah";
    <stable> <accessor> def j(): String = Foo.this.j;
    private[this] val k: String = Foo.this.i().+(Foo.this.j());
    <stable> <accessor> def k(): String = Foo.this.k;
    def wibble(): scala.collection.immutable.IndexedSeq[Int] = scala.this.Predef.augmentString(Foo.this.k()).map[Int, scala.collection.immutable.IndexedSeq[Int]]({
      @SerialVersionUID(value = 0) final <synthetic> class $anonfun extends scala.runtime.AbstractFunction1[Char,Int] with Serializable {
        def <init>($outer: Foo.this.type): <$anon: Char => Int> = {
          $anonfun.super.<init>();
          ()
        };
        final def apply(c: Char): Int = c.*(2);
        <synthetic> <paramaccessor> <artifact> private[this] val $outer: Foo.this.type = _;
        <synthetic> <stable> <artifact> def $outer(): Foo.this.type = $anonfun.this.$outer
      };
      (new <$anon: Char => Int>(Foo.this): Char => Int)
    }, scala.this.Predef.fallbackStringCanBuildFrom[Int]())
  }
}

[[syntax trees at end of                   erasure]] // sample.scala
package <empty> {
  class Foo extends Object {
    def <init>(): Foo = {
      Foo.super.<init>();
      ()
    };
    private[this] val i: Int = 23;
    <stable> <accessor> def i(): Int = Foo.this.i;
    private[this] val j: String = "blah";
    <stable> <accessor> def j(): String = Foo.this.j;
    private[this] val k: String = Foo.this.i().+(Foo.this.j());
    <stable> <accessor> def k(): String = Foo.this.k;
    def wibble(): scala.collection.immutable.IndexedSeq = new collection.immutable.StringOps(scala.this.Predef.augmentString(Foo.this.k()).$asInstanceOf[String]()).map({
  @SerialVersionUID(value = 0) final <synthetic> class $anonfun extends scala.runtime.AbstractFunction1 with Serializable {
    def <init>($outer: Foo): <$anon: Function1> = {
      $anonfun.super.<init>();
      ()
    };
    final def apply(c: Char): Int = c.*(2);
    <synthetic> <paramaccessor> <artifact> private[this] val $outer: Foo = _;
    <synthetic> <stable> <artifact> def $outer(): Foo = $anonfun.this.$outer;
    final <bridge> <artifact> def apply(v1: Object): Object = scala.Int.box($anonfun.this.apply(unbox(v1)))
  };
  (new <$anon: Function1>(Foo.this): Function1)
}, scala.this.Predef.fallbackStringCanBuildFrom()).$asInstanceOf[scala.collection.immutable.IndexedSeq]()
  }
}

[[syntax trees at end of               posterasure]] // sample.scala
package <empty> {
  class Foo extends Object {
    def <init>(): Foo = {
      Foo.super.<init>();
      ()
    };
    private[this] val i: Int = 23;
    <stable> <accessor> def i(): Int = Foo.this.i;
    private[this] val j: String = "blah";
    <stable> <accessor> def j(): String = Foo.this.j;
    private[this] val k: String = Foo.this.i().+(Foo.this.j());
    <stable> <accessor> def k(): String = Foo.this.k;
    def wibble(): scala.collection.immutable.IndexedSeq = new collection.immutable.StringOps(scala.this.Predef.augmentString(Foo.this.k())).map({
  @SerialVersionUID(value = 0) final <synthetic> class $anonfun extends scala.runtime.AbstractFunction1 with Serializable {
    def <init>($outer: Foo): <$anon: Function1> = {
      $anonfun.super.<init>();
      ()
    };
    final def apply(c: Char): Int = c.*(2);
    <synthetic> <paramaccessor> <artifact> private[this] val $outer: Foo = _;
    <synthetic> <stable> <artifact> def $outer(): Foo = $anonfun.this.$outer;
    final <bridge> <artifact> def apply(v1: Object): Object = scala.Int.box($anonfun.this.apply(unbox(v1)))
  };
  (new <$anon: Function1>(Foo.this): Function1)
}, scala.this.Predef.fallbackStringCanBuildFrom()).$asInstanceOf[scala.collection.immutable.IndexedSeq]()
  }
}

[[syntax trees at end of                  lazyvals]] // sample.scala: tree is unchanged since posterasure
[[syntax trees at end of                lambdalift]] // sample.scala
package <empty> {
  class Foo extends Object {
    def <init>(): Foo = {
      Foo.super.<init>();
      ()
    };
    private[this] val i: Int = 23;
    <stable> <accessor> def i(): Int = Foo.this.i;
    private[this] val j: String = "blah";
    <stable> <accessor> def j(): String = Foo.this.j;
    private[this] val k: String = Foo.this.i().+(Foo.this.j());
    <stable> <accessor> def k(): String = Foo.this.k;
    def wibble(): scala.collection.immutable.IndexedSeq = new collection.immutable.StringOps(scala.this.Predef.augmentString(Foo.this.k())).map({
  (new <$anon: Function1>(Foo.this): Function1)
}, scala.this.Predef.fallbackStringCanBuildFrom()).$asInstanceOf[scala.collection.immutable.IndexedSeq]();
    @SerialVersionUID(value = 0) final <synthetic> class $anonfun$wibble$1 extends scala.runtime.AbstractFunction1 with Serializable {
      def <init>($outer: Foo): <$anon: Function1> = {
        $anonfun$wibble$1.super.<init>();
        ()
      };
      final def apply(c: Char): Int = c.*(2);
      <synthetic> <paramaccessor> <artifact> private[this] val $outer: Foo = _;
      <synthetic> <stable> <artifact> def $outer(): Foo = $anonfun$wibble$1.this.$outer;
      final <bridge> <artifact> def apply(v1: Object): Object = scala.Int.box($anonfun$wibble$1.this.apply(scala.Char.unbox(v1)))
    }
  }
}

[[syntax trees at end of              constructors]] // sample.scala
package <empty> {
  class Foo extends Object {
    private[this] val i: Int = _;
    <stable> <accessor> def i(): Int = Foo.this.i;
    private[this] val j: String = _;
    <stable> <accessor> def j(): String = Foo.this.j;
    private[this] val k: String = _;
    <stable> <accessor> def k(): String = Foo.this.k;
    def wibble(): scala.collection.immutable.IndexedSeq = new collection.immutable.StringOps(scala.this.Predef.augmentString(Foo.this.k())).map({
  (new <$anon: Function1>(Foo.this): Function1)
}, scala.this.Predef.fallbackStringCanBuildFrom()).$asInstanceOf[scala.collection.immutable.IndexedSeq]();
    @SerialVersionUID(value = 0) final <synthetic> class $anonfun$wibble$1 extends scala.runtime.AbstractFunction1 with Serializable {
      final def apply(c: Char): Int = c.*(2);
      final <bridge> <artifact> def apply(v1: Object): Object = scala.Int.box($anonfun$wibble$1.this.apply(scala.Char.unbox(v1)));
      def <init>($outer: Foo): <$anon: Function1> = {
        $anonfun$wibble$1.super.<init>();
        ()
      }
    };
    def <init>(): Foo = {
      Foo.super.<init>();
      Foo.this.i = 23;
      Foo.this.j = "blah";
      Foo.this.k = Foo.this.i().+(Foo.this.j());
      ()
    }
  }
}

[[syntax trees at end of                   flatten]] // sample.scala
package <empty> {
  class Foo extends Object {
    private[this] val i: Int = _;
    <stable> <accessor> def i(): Int = Foo.this.i;
    private[this] val j: String = _;
    <stable> <accessor> def j(): String = Foo.this.j;
    private[this] val k: String = _;
    <stable> <accessor> def k(): String = Foo.this.k;
    def wibble(): scala.collection.immutable.IndexedSeq = new collection.immutable.StringOps(scala.this.Predef.augmentString(Foo.this.k())).map({
  (new <$anon: Function1>(Foo.this): Function1)
}, scala.this.Predef.fallbackStringCanBuildFrom()).$asInstanceOf[scala.collection.immutable.IndexedSeq]();
    def <init>(): Foo = {
      Foo.super.<init>();
      Foo.this.i = 23;
      Foo.this.j = "blah";
      Foo.this.k = Foo.this.i().+(Foo.this.j());
      ()
    }
  };
  @SerialVersionUID(value = 0) final <synthetic> class anonfun$wibble$1 extends scala.runtime.AbstractFunction1 with Serializable {
    final def apply(c: Char): Int = c.*(2);
    final <bridge> <artifact> def apply(v1: Object): Object = scala.Int.box(anonfun$wibble$1.this.apply(scala.Char.unbox(v1)));
    def <init>($outer: Foo): <$anon: Function1> = {
      anonfun$wibble$1.super.<init>();
      ()
    }
  }
}

[[syntax trees at end of                     mixin]] // sample.scala: tree is unchanged since flatten
[[syntax trees at end of                   cleanup]] // sample.scala: tree is unchanged since flatten
[[syntax trees at end of                delambdafy]] // sample.scala: tree is unchanged since flatten
[[syntax trees at end of                     icode]] // sample.scala: tree is unchanged since flatten
[[syntax trees at end of                       jvm]] // sample.scala: tree is unchanged since flatten
```

Want to contribute? [Edit this file](https://github.com/typelevel/scala/edit/typelevel-readme/src/main/resources/microsite/docs/phases.md)