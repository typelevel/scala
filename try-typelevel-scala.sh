#!/bin/sh
COURSIER_URL=https://git.io/vgvpD
test -e ~/.coursier/coursier || \
  (mkdir -p ~/.coursier && curl -L -s --output ~/.coursier/coursier $COURSIER_URL && chmod +x ~/.coursier/coursier)
~/.coursier/coursier launch -q -P \
  com.lihaoyi:ammonite_2.12.3:1.0.1 \
  -E org.scala-lang:scala-library \
  -E org.scala-lang:scala-compiler \
  -E org.scala-lang:scala-reflect \
  org.typelevel:scala-compiler:2.12.3-bin-typelevel-4 \
  org.typelevel:scala-library:2.12.3-bin-typelevel-4 \
  org.typelevel:scala-reflect:2.12.3-bin-typelevel-4 \
  -- < /dev/tty
