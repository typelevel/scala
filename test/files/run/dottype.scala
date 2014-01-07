object Test {
  trait <[T1, T2] { }

  // type _1 =
  // Succ[_1] = _2
  // etc...

  type _1 = 1.type
  //(1+1).type


  //().type

  def g(x: Int) = x match {
    case _: 7.type => true
    case _         => false
  }

  trait Succ[T] {
    type Out
    def apply(x: T): Out
  }

  implicit object One extends Succ[1.type] {
    type Out = 2.type
    def apply(x: 1.type) = 2 // -1
  }

  def f[T](x: T)(implicit succ: Succ[T]) = succ(x)

  def main(args: Array[String]): Unit = {
    println(f(1))
    // println(f(5))
    println((g(1), g(5), g(7)))
  }

}
