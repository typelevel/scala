package scala.reflect.internal

import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import scala.tools.nsc.symtab.SymbolTableForUnitTesting

@RunWith(classOf[JUnit4])
class TypesTest {

  object symbolTable extends SymbolTableForUnitTesting
  import symbolTable._, definitions._

  @Test
  def testRefinedTypeSI8611(): Unit = {
    def stringNarrowed = StringTpe.narrow
    assert(stringNarrowed != stringNarrowed)
    assert(!(stringNarrowed =:= stringNarrowed))

    def boolWithString = refinedType(BooleanTpe :: StringTpe :: Nil, NoSymbol)
    assert(boolWithString != boolWithString)
    assert(boolWithString =:= boolWithString)

    val boolWithString1 = boolWithString
    val boolWithString1narrow1 = boolWithString1.narrow
    val boolWithString1narrow2 = boolWithString1.narrow
    // Two narrowings of the same refinement end up =:=. This was the root
    // cause of SI-8611. See `narrowUniquely` in `Logic` for the workaround.
    assert(boolWithString1narrow1 =:= boolWithString1narrow2)
    val uniquelyNarrowed1 = refinedType(boolWithString1narrow1 :: Nil, NoSymbol)
    val uniquelyNarrowed2 = refinedType(boolWithString1narrow2 :: Nil, NoSymbol)
    assert(uniquelyNarrowed1 =:= uniquelyNarrowed2)
  }

  @Test
  def testLub(): Unit = {
    assert(lub(List()) =:= NothingTpe)
    assert(lub(List(LiteralType(Constant(0)), LiteralType(Constant(1)))) =:= IntTpe)
    assert(lub(List(LiteralType(Constant(1)), LiteralType(Constant(1)), LiteralType(Constant(1)))) =:= LiteralType(Constant(1)))
    assert(lub(List(LiteralType(Constant("a")), LiteralType(Constant("b")))) =:= StringTpe)
    assert(lub(List(LiteralType(Constant("a")), LiteralType(Constant("a")))) =:= LiteralType(Constant("a")))
    assert(lub(List(typeOf[Class[String]], typeOf[Class[String]])) =:= typeOf[Class[String]])
    assert(lub(List(typeOf[Class[String]], typeOf[Class[Object]])) =:= typeOf[Class[_ >: String <: Object]])
  }
}
