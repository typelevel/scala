case class Foo[A](a: A)

object Test {
  implicit val foo = new Foo(1)
  implicit def foo2 = new Foo("")
}
