object Test {
  // type _1 =
  // Succ[_1] = _2
  // etc...

  type _1 = 1.type
  // (1+1).type
  /*
   scala> type _2 = (1+1).type
   <console>:1: error: '.' expected but identifier found.
          type _2 = (1+1).type
                      ^
   */

  type Unt = ().type

  type Null = null.type

  var x: 3.type = 3
  /*
    scala> x = 42
    <console>:8: error: type mismatch;
     found   : 42.type (with underlying type Int)
     required: 3.type
           x = 42
               ^
   */

  val y: 5.type = 5

  final val `( •_•) ( •_•)>⌐■-■ (⌐■_■)` = -10
  val unicode: `( •_•) ( •_•)>⌐■-■ (⌐■_■)`.type = `( •_•) ( •_•)>⌐■-■ (⌐■_■)`

  def g(x: Int) = x match {
    case _: y.type => 0
    case _: 7.type => 1
    case _         => 2
  }

  trait Succ[T] {
    type Out
    def apply(x: T): Out
  }

  implicit object One extends Succ[1.type] {
    type Out = 2.type
    def apply(x: 1.type) = 2
  }

  def f[T](x: T)(implicit succ: Succ[T]) = succ(x)

  case class Residue[N <: Int : SingleInhabitant](n: Long) { lhs =>
    def +(rhs: Residue[N]): Residue[N] =
      Residue((lhs.n + rhs.n) % inhabitant[N])
  }

  class Ranged[From <: Int : SingleInhabitant, To <: Int : SingleInhabitant] {
    def value = {
      val rnd = new scala.util.Random
      val from = inhabitant[From]
      val to = inhabitant[To]
      (from + rnd.nextInt(to - from + 1))
    }
  }

  class IntegralRanged[N, From <: N : SingleInhabitant, To <: N : SingleInhabitant](implicit ev: Integral[N]) {
    def value = {
      val from = ev.toInt(inhabitant[From])
      val to   = ev.toInt(inhabitant[To])
      val rnd  = from + scala.util.Random.nextInt(to - from)
      ev.fromInt(rnd)
    }
  }

  val range = new Ranged[10.type, 20.type]
  val integral = new IntegralRanged[Int, 1.type, 50.type]

  def noisyIdentity(x: Any): x.type = {
    println("got " + x)
    x
  }

  def main(args: Array[String]): Unit = {
    println(f(1))
    // println(f(5))
    println((g(1), g(5), g(7)))
    println(Residue[13.type](15) + Residue[13.type](20))
    println(range.value <= 20, range.value >= 10)
    println(integral.value <= 50, integral.value >= 1)

    noisyIdentity(1)
    noisyIdentity("PANDA!")
  }

  // Inlining functions with singleton result type
  /*
scala> def ok(): 7.type = {
     | println("PANDA!")
     | 7
     | }
ok: ()7.type

scala> ok()
res0: 7.type = 7

// Expected:
scala> ok()
PANDA!
res0: 7.type = 7
   */

  // Parser problem:
  /*
scala> val t: -1.type = -1
<console>:1: error: ';' expected but integer literal found.
       val t: -1.type = -1
               ^

scala> final val t = -1
t: -1.type = -1
   */

  // Not sure if this is a problem (I guess it is a special case of (2+3).type):
  /*
scala> def test: 5.type = {
     | val t = 3
     | val p = 2
     | t + p
     | }
<console>:10: error: type mismatch;
 found   : Int
 required: 5.type
       t + p
         ^
   */

  // Recursive weirdness (ConstantType folding?):
  /*
scala> val t: 1.type = t
<console>:9: warning: value t does nothing other than call itself recursively
       val t: 1.type = t
                       ^
t: 1.type = 1
   */
}
