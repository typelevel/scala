trait Monad[M[_]] {
  def point[A](a: A): M[A]
  def bind[A, B](ma: M[A])(f: A => M[B]): M[B]
}

object Test {
  trait RightMonad[L] extends Monad[[x] => Either[L, x]] {
    def point[A](a: A): Either[L, A] =
      Right(a)
    def bind[A, B](ma: Either[L, A])(f: A => Either[L, B]): Either[L, B] =
      ma.right.flatMap(f)
  }

  def test1[A[_]] = 999

  test1[[a] => (a, a)]
  test1[[b] => Unit]
  test1[[x] => x => x]
  test1[[y] => Option[y]]
  test1[[z] => Map[z, Set[z]]]

  def test2[X[_[_]]] = 999

  test2[[abc[_]] => abc[Int]]
  test2[[xyz[_]] => xyz[xyz[xyz[Unit]]]]
  test2[[q[_]] => Monad[q]]
}
