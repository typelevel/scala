package scala.tools.nsc

// These are for compatibility with sbt's compiler interface.
package object doc {
  type Settings = scala.tools.nsc.Settings
}

package interactive {
  trait RangePositions
}
