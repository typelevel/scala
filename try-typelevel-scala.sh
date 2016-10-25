#!/bin/sh
COURSIER_URL=https://raw.githubusercontent.com/alexarchambault/coursier/v1.0.0-M14-2/coursier
test -e ~/.coursier/coursier || \
  (mkdir -p ~/.coursier && curl -s --output ~/.coursier/coursier $COURSIER_URL && chmod +x ~/.coursier/coursier)
~/.coursier/coursier launch -q -P \
  com.lihaoyi:ammonite_2.11.8:0.7.8 \
  -E org.scala-lang:scala-library \
  -E org.scala-lang:scala-compiler \
  -E org.scala-lang:scala-reflect \
  org.typelevel:scala-compiler:2.11.8 \
  org.typelevel:scala-library:2.11.8 \
  org.typelevel:scala-reflect:2.11.8 \
  -- < /dev/tty
