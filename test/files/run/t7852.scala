import scala.tools.partest._
import org.objectweb.asm
import org.objectweb.asm.util._
import scala.tools.nsc.util.stringFromWriter
import scala.collection.JavaConverters._

object Test extends BytecodeTest {
  val nullChecks = Set(asm.Opcodes.IFNONNULL, asm.Opcodes.IFNULL)

  def show: Unit = {
    def test(methodName: String, expected: Int) {
      val classNode  = loadClassNode("Lean")
      val methodNode = getMethod(classNode, methodName)
      val got        = methodNode.jopcodes count nullChecks

      assert(got == expected, s"$methodName: expected $expected but got $got comparisons")
    }
    test("string", expected = 0)
    test("module", expected = 0)
    test("moduleIndirect", expected = 2)
  }
}

class Lean {
  def string {
    "" == toString
  }

  def module {
    Nil == (toString: Any)
  }

  def moduleIndirect {
    val n: Nil.type = null
    n == (toString: Any) // still need null checks here.
  }
}
