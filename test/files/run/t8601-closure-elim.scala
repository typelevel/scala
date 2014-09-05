import scala.tools.partest._
import org.objectweb.asm
import org.objectweb.asm.util._
import scala.collection.JavaConverters._

object Test extends BytecodeTest {
  val nullChecks = Set(asm.Opcodes.NEW)

  def show: Unit = {
    def test(methodName: String) {
      val classNode = loadClassNode("Foo")
      val methodNode = getMethod(classNode, "b")
      val ops = methodNode.jopcodes
      assert(!ops.contains(asm.Opcodes.NEW), ops)// should be allocation free if the closure is eliminiated
    }
    test("b")
  }
}

class Foo {
  @inline final def a(x: Int => Int) = x(1)
  final def b {
    val delta = 0
    a(x => delta + 1)
  }
}
