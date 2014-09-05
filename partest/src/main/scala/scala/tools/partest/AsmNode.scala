package scala.tools.partest

import scala.collection.JavaConverters._
import org.objectweb.asm
import asm._
import asm.tree._
import java.lang.reflect.Modifier

sealed trait AsmNode[+T] {
  def node: T
  def access: Int
  def desc: String
  def name: String
  def signature: String
  def attrs: List[Attribute]
  def visibleAnnotations: List[AnnotationNode]
  def invisibleAnnotations: List[AnnotationNode]
  def characteristics = f"$name%15s $desc%-30s$accessString$sigString"
  def erasedCharacteristics = f"$name%15s $desc%-30s$accessString"

  private def accessString     = if (access == 0) "" else " " + Modifier.toString(access)
  private def sigString        = if (signature == null) "" else " " + signature
  override def toString        = characteristics
}

object AsmNode {
  type AsmMethod = AsmNode[MethodNode]
  type AsmField  = AsmNode[FieldNode]
  type AsmMember = AsmNode[_]

  implicit class AsmMethodNode(val node: MethodNode) extends AsmNode[MethodNode] {
    def access: Int                                = node.access
    def desc: String                               = node.desc
    def name: String                               = node.name
    def signature: String                          = node.signature
    def attrs: List[Attribute]                     = node.attrs.asScalaTyped[Attribute]
    def visibleAnnotations: List[AnnotationNode]   = node.visibleAnnotations.asScalaTyped[AnnotationNode]
    def invisibleAnnotations: List[AnnotationNode] = node.invisibleAnnotations.asScalaTyped[AnnotationNode]
  }
  implicit class AsmFieldNode(val node: FieldNode) extends AsmNode[FieldNode] {
    def access: Int                                = node.access
    def desc: String                               = node.desc
    def name: String                               = node.name
    def signature: String                          = node.signature
    def attrs: List[Attribute]                     = node.attrs.asScalaTyped[Attribute]
    def visibleAnnotations: List[AnnotationNode]   = node.visibleAnnotations.asScalaTyped[AnnotationNode]
    def invisibleAnnotations: List[AnnotationNode] = node.invisibleAnnotations.asScalaTyped[AnnotationNode]
  }

  def apply(node: MethodNode): AsmMethodNode = new AsmMethodNode(node)
  def apply(node: FieldNode): AsmFieldNode   = new AsmFieldNode(node)
}
