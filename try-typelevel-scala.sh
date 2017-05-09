#!/bin/sh
COURSIER_URL=https://raw.githubusercontent.com/alexarchambault/coursier/v1.0.0-RC2/coursier
test -e ~/.coursier/coursier || \
  (mkdir -p ~/.coursier && curl -s --output ~/.coursier/coursier $COURSIER_URL && chmod +x ~/.coursier/coursier)
~/.coursier/coursier launch -q -P \
  com.lihaoyi:ammonite_2.12.2:0.8.4 \
  -E org.scala-lang:scala-library \
  -E org.scala-lang:scala-compiler \
  -E org.scala-lang:scala-reflect \
  org.typelevel:scala-compiler:2.12.2-bin-typelevel-4 \
  org.typelevel:scala-library:2.12.2-bin-typelevel-4 \
  org.typelevel:scala-reflect:2.12.2-bin-typelevel-4 \
  -- < /dev/tty
