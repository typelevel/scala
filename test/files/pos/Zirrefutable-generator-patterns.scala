case class Foo[A](a: A) {
  def map[B](f: A => B): Foo[B] = Foo(f(a))
  def flatMap[B](f: A => Foo[B]): Foo[B] = Foo(f(a).a)
}

object Test {
  for {
    (a, b) <- Foo((1, 'd'))
    (c, d, e) <- Foo((1, true, "three"))
  } yield (a + c, e :+ b, !d)
}
