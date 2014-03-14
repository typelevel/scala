package scala.tools.reflect

import scala.reflect.macros.runtime.Context


trait SingleInhabitantMacro { self: FormatInterpolator =>
  val c: Context

  import c.universe._

  def materialize(tpe: Type): Tree = {
    val value = tpe match {
      case ConstantType(const)  => Literal(const)
      case SingleType(pre, sym) => Select(TypeTree(pre), sym)
      case _                    =>
        c.abort(c.enclosingPosition, tpe + " is not a singleton type!")
    }
    Apply(
      TypeApply(
        Select(reify(SingleInhabitant).tree, newTermName("apply")),
        TypeTree(tpe) :: Nil
      ),
      value :: Nil
    )
  }
}
