case class Foo[A](a: A)

object Test {
  implicit val foo: Foo[Int] = new Foo(1)
  implicit def foo2: Foo[String] = new Foo("")
}
