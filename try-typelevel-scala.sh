#!/bin/sh
COURSIER_URL=https://raw.githubusercontent.com/alexarchambault/coursier/v1.0.0-M12/coursier
test -e ~/.coursier/cr || (mkdir -p ~/.coursier && curl -s --output ~/.coursier/cr $COURSIER_URL && chmod +x ~/.coursier/cr)
~/.coursier/cr launch \
  com.lihaoyi:ammonite_2.11.8:0.7.2 \
  -E org.scala-lang:scala-library \
  -E org.scala-lang:scala-compiler \
  -E org.scala-lang:scala-reflect \
  org.typelevel:scala-compiler:2.11.8 \
  org.typelevel:scala-library:2.11.8 \
  org.typelevel:scala-reflect:2.11.8 \
  -- \
  --predef='
    repl.compiler.settings.YpartialUnification.value = true
    repl.compiler.settings.YliteralTypes.value = true
  '
