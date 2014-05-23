package policy
package building

import sbt._, Keys._
import com.typesafe.tools.mima.core._

object MimaPolicy {
  private val removedPackages = wordSet("""
    scala.text
    scala.concurrent
    scala.collection.parallel
    scala.collection.concurrent
    scala.util.hashing
  """)

  private val removedClasses = wordSet("""
    scala.Responder
    scala.collection.CustomParallelizable
    scala.collection.IterableProxy
    scala.collection.IterableProxyLike
    scala.collection.IterableView
    scala.collection.IterableViewLike
    scala.collection.JavaConversions
    scala.collection.MapProxy
    scala.collection.MapProxyLike
    scala.collection.Parallel
    scala.collection.Parallelizable
    scala.collection.Searching
    scala.collection.SeqProxy
    scala.collection.SeqProxyLike
    scala.collection.SeqView
    scala.collection.SeqViewLike
    scala.collection.SetProxy
    scala.collection.SetProxyLike
    scala.collection.TraversableProxy
    scala.collection.TraversableProxyLike
    scala.collection.TraversableView
    scala.collection.TraversableViewLike
    scala.collection.ViewMkString
    scala.collection.generic.AtomicIndexFlag
    scala.collection.generic.CanCombineFrom
    scala.collection.generic.DefaultSignalling
    scala.collection.generic.DelegatedContext
    scala.collection.generic.DelegatedSignalling
    scala.collection.generic.GenericParCompanion
    scala.collection.generic.GenericParMapCompanion
    scala.collection.generic.GenericParMapTemplate
    scala.collection.generic.GenericParTemplate
    scala.collection.generic.HasNewCombiner
    scala.collection.generic.IdleSignalling
    scala.collection.generic.ParFactory
    scala.collection.generic.ParMapFactory
    scala.collection.generic.ParSetFactory
    scala.collection.generic.Signalling
    scala.collection.generic.TaggedDelegatedContext
    scala.collection.generic.VolatileAbort
    scala.collection.immutable.IntMap
    scala.collection.immutable.IntMapEntryIterator
    scala.collection.immutable.IntMapIterator
    scala.collection.immutable.IntMapKeyIterator
    scala.collection.immutable.IntMapUtils
    scala.collection.immutable.IntMapValueIterator
    scala.collection.immutable.LongMap
    scala.collection.immutable.LongMapEntryIterator
    scala.collection.immutable.LongMapIterator
    scala.collection.immutable.LongMapKeyIterator
    scala.collection.immutable.LongMapUtils
    scala.collection.immutable.LongMapValueIterator
    scala.collection.immutable.MapProxy
    scala.collection.immutable.SetProxy
    scala.collection.immutable.StreamView
    scala.collection.immutable.StreamViewLike
    scala.collection.mutable.AVLIterator
    scala.collection.mutable.AVLTree
    scala.collection.mutable.BufferProxy
    scala.collection.mutable.DefaultMapModel
    scala.collection.mutable.ImmutableMapAdaptor
    scala.collection.mutable.ImmutableSetAdaptor
    scala.collection.mutable.IndexedSeqView
    scala.collection.mutable.Leaf
    scala.collection.mutable.LongMap
    scala.collection.mutable.MapProxy
    scala.collection.mutable.Node
    scala.collection.mutable.ObservableBuffer
    scala.collection.mutable.ObservableMap
    scala.collection.mutable.ObservableSet
    scala.collection.mutable.PriorityQueueProxy
    scala.collection.mutable.History
    scala.collection.mutable.Subscriber
    scala.collection.mutable.Publisher
    scala.collection.mutable.QueueProxy
    scala.collection.mutable.RevertibleHistory
    scala.collection.mutable.SetProxy
    scala.collection.mutable.StackProxy
    scala.collection.mutable.Undoable
    scala.collection.mutable.UnrolledBuffer
    scala.text.DocGroup
  """)

  object HasName { def unapply(x: HasDeclarationName) = Some(x.decodedName) }
  object HasFullNames { def unapply(x: Iterable[ClassInfo]) = Some(x.toList map (_.fullName)) }
  object PackageOf {
    def unapply(x: HasDeclarationName): Option[String] = x match {
      case x: MemberInfo => unapply(x.owner)
      case x: ClassInfo  => Some(x.fullName split "[.]" dropRight 1 mkString ".")
      case _             => None
    }
  }

  private def arityFilters = {
    for (s <- Vector("Product", "Tuple", "Function", "runtime.AbstractFunction") ; n <- (10 to 22) ; d <- List("", "$")) yield
      ProblemFilters.exclude[MissingClassProblem](s"scala.$s$n$d")
  }
  private def deletedFilter: ProblemFilter = {
    val methods       = wordSet("""
      view par parCombiner
      ++ -- toMap toSet toSeq toIterable toTraversable
      scala$collection$mutable$SynchronizedBuffer$$super$++
      Everything
    """)

    {
      case MissingClassProblem(cl) if removedPackages exists (cl.fullName startsWith _ + ".") => false
      case MissingClassProblem(cl) if removedClasses(cl.fullName split "[$#]" head)           => false
      case p: MissingMethodProblem if p.affectedVersion == Problem.ClassVersion.Old           => false // adding methods okay with me
      case MissingMethodProblem(HasName(name)) if methods(name)                               => false
      case MissingTypesProblem(_, HasFullNames(missing)) if missing forall removedClasses     => false
      case IncompatibleMethTypeProblem(PackageOf("scala.sys.process"), _)                     => false // TODO - SyncVar
      case IncompatibleMethTypeProblem(HasName(name), _) if methods(name)                     => false
      case IncompatibleResultTypeProblem(HasName(name), _) if methods(name)                   => false
      case UpdateForwarderBodyProblem(meth)                                                   => false
      case _                                                                                  => true
    }
  }
  private def individualFilters = Vector(
    ProblemFilters.exclude[MissingMethodProblem]("scala.collection.Iterator.corresponds")
  )

  def filters = (arityFilters ++ individualFilters) :+ deletedFilter

  def patternMatch(p: Problem) = p match {
    case MissingFieldProblem(oldfld: MemberInfo)                                      =>
    case MissingMethodProblem(meth: MemberInfo)                                       =>
    case UpdateForwarderBodyProblem(meth: MemberInfo)                                 =>
    case MissingClassProblem(oldclazz: ClassInfo)                                     =>
    case AbstractClassProblem(oldclazz: ClassInfo)                                    =>
    case FinalClassProblem(oldclazz: ClassInfo)                                       =>
    case FinalMethodProblem(newmemb: MemberInfo)                                      =>
    case IncompatibleFieldTypeProblem(oldfld: MemberInfo, newfld: MemberInfo)         =>
    case IncompatibleMethTypeProblem(oldmeth: MemberInfo, newmeths: List[MemberInfo]) =>
    case IncompatibleResultTypeProblem(oldmeth: MemberInfo, newmeth: MemberInfo)      =>
    case AbstractMethodProblem(newmeth: MemberInfo)                                   =>
    case IncompatibleTemplateDefProblem(oldclazz: ClassInfo, newclazz: ClassInfo)     =>
    case MissingTypesProblem(newclazz: ClassInfo, missing: Iterable[ClassInfo])       =>
    case CyclicTypeReferenceProblem(clz: ClassInfo)                                   =>
    case InaccessibleFieldProblem(newfld: MemberInfo)                                 =>
    case InaccessibleMethodProblem(newmeth: MemberInfo)                               =>
    case InaccessibleClassProblem(newclazz: ClassInfo)                                =>
    case x: MemberProblem                                                             =>
    case x: TemplateProblem                                                           =>
  }
}

