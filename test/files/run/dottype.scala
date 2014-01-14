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

  def main(args: Array[String]): Unit = {
    println(f(1))
    // println(f(5))
    println((g(1), g(5), g(7)))
  }

}
