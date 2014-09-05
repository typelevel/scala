object Bug {
  import scala.annotation.unchecked.{ uncheckedVariance => uV }

  trait Base[T] { def f: java.util.Iterator[T] }
  trait CovTrait[+T] extends java.util.Iterator[T @uV]
  type CovAlias[+T] = java.util.Iterator[T @uV]

  // compiles
  class A[+T] extends Base[T @uV] { def f: CovTrait[T] = null }
  // fails
  class B[+T] extends Base[T @uV] { def f: CovAlias[T] = null }
  // <console>:17: error: covariant type T occurs in invariant position in type => Bug.CovAlias[T] of method f
  //          class B[+T] extends Base[T @uV] { def f: CovAlias[T] = null }
  //                                                ^
}
