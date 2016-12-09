---
layout: page
title: Project Folder Structure
site_nav_entry: false
---

Once you [clone](https://github.com/typelevel/scala/) the compiler project (*hint:* ensure you are in a branch with the compiler code, not a documentation branch) you will see several folders and files. 

## Repository structure

Your starting point should be the `README.md` file at the root, which describes the project a bit more. This file contains a description of the project structure, which we reproduce below:

```
scala/
+--build.sbt                 The main sbt build script
+--build.xml                 The deprecated Ant build script
+--pull-binary-libs.sh       Pulls artifacts from remote repository, used by build
+--lib/                      Pre-compiled libraries for the build
+--src/                      All sources
   +---/compiler             Scala Compiler
   +---/library              Scala Standard Library
   +---/library-aux          Set of files for bootstrapping and documentation
   +---/reflect              Scala Reflection
   +---/repl                 REPL source
   +---/scaladoc             Scaladoc source
   +---/scalap               Scalap source   
   +---/eclipse              Eclipse project files
   +---/ensime               Ensime project templates
   +---/intellij             IntelliJ project templates
   +---...                   other folders like 'manual', etc
+--spec/                     The Scala language specification
+--scripts/                  Scripts for the CI jobs (including building releases)
+--test/                     The Scala test suite
   +---/benchmarks           Benchmark tests using JMH
   +---/files                Partest tests
   +---/junit                JUnit tests
   +---...                   Other folders like 'flaky' for flaky tests, etc
   partest                   Script to launch Partest from command line
+--build/                    [Generated] Build output directory
```


Relevant folders when working with the Scala compiler:

* **src**: source of the compiler. Also contains several IDE specific folders (ensime, eclipse, intellij), as well as the source code for tools like `scaladoc` and `scalap`
* **test**: scala test suite. You may want to focus on the partest side (/files) as well as `junit` and `benchmarks`, for a start

Folders of certain interest:

* **spec**: the Scala specification, as a Jekyll project. You can see it published online at [http://www.scala-lang.org/files/archive/spec/](http://www.scala-lang.org/files/archive/spec/) (select the relevant version to see HTML docs)
* **tools**: set of utility bash scripts, for example `scaladoc-diff` to see changes on Scaladoc

The following folders and files can be ignored when you start working with the Scala compiler as they are not relevant at this stage:

* **doc**: contains licenses for the project
* **docs**: contains some TODO tasks and notes. Not updated for a long while, doubtful to be relevant anymore
* **lib**: contains sha1 signatures for some jar files used in the project, for example the `ant-dotnet` jar to provide Scala support on .Net
* **META-INF**: contains Manifest file for the project
* **project**: contains helpers for the Scala build configuration. The project is using a standard `build.sbt` file, located at the root of the project
* **scripts**: used for CI 


## Compiler's Build Process

One of the first tasks you want to do is to build the compiler, as a full compile from scratch may take a while. This will allow you to work with the compiler codebase using incremental compilation, which will reduce the build times substantially.

Compiling the project has no mystery, as we are working with a standard [SBT project](http://www.scala-sbt.org/). This means you want to open a terminal at the root of the project and run `sbt`:

```scala
$ sbt
Picked up JAVA_TOOL_OPTIONS: -Dfile.encoding=UTF-8
[info] Loading global plugins from /Users/villegap/.sbt/0.13/plugins
[info] Loading project definition from /Users/villegap/Dropbox/Projectes/typelevel-scala/project/project
[info] Loading project definition from /Users/villegap/Dropbox/Projectes/typelevel-scala/project
[info] Updating {file:/Users/villegap/Dropbox/Projectes/typelevel-scala/project/}typelevel-scala-build...
[info] Resolving org.fusesource.jansi#jansi;1.4 ...
[info] Done updating.
[info] *** Welcome to the sbt build definition for Scala! ***
[info] Check README.md for more information.
> 
```

Note the output above corresponds to my local version at the time of this entry, yours may vary accordingly. 

Once in `sbt`, run `compile`. The first time this may take several minutes, depending on your computer specifications.

```scala
> compile
[info] Updating {file:/Users/villegap/Dropbox/Projectes/typelevel-scala/}library...
[info] Updating {file:/Users/villegap/Dropbox/Projectes/typelevel-scala/}root...
[info] Updating {file:/Users/villegap/Dropbox/Projectes/typelevel-scala/}bootstrap...
[info] Done updating.
[info] Resolving org.scala-lang#scala-library;2.12.0-M5 ...

[... several entries more where sbt resolves dependencies ...]
[warn] there were 38 deprecation warnings (since 2.10.0)
[warn] there were 25 deprecation warnings (since 2.11.0)
[warn] there were 45 deprecation warnings (since 2.12.0)
[warn] there were 10 deprecation warnings (since 2.12.0-M2)
[warn] there were 118 deprecation warnings in total; re-run with -deprecation for details
[warn] 5 warnings found
[info] Compiling 157 Scala sources to /Users/villegap/Dropbox/Projectes/typelevel-scala/build/quick/classes/reflect...

[... more deprecation warnings and compilation of other source files ...]
[info] /Users/villegap/Dropbox/Projectes/typelevel-scala/test/junit/scala/tools/testing/ClearAfterClass.java: /Users/villegap/Dropbox/Projectes/typelevel-scala/test/junit/scala/tools/testing/ClearAfterClass.java uses unchecked or unsafe operations.
[info] /Users/villegap/Dropbox/Projectes/typelevel-scala/test/junit/scala/tools/testing/ClearAfterClass.java: Recompile with -Xlint:unchecked for details.
[warn] there was one deprecation warning (since 2.10.0)
[warn] there were two deprecation warnings (since 2.10.1)
[warn] there were 15 deprecation warnings (since 2.11.0)
[warn] there were three deprecation warnings (since 2.11.8)
[warn] there were 21 deprecation warnings in total; re-run with -deprecation for details
[warn] 5 warnings found
[success] Total time: 263 s, completed 21-Nov-2016 18:51:37
```

Once it finishes, you are ready to start hacking the compiler. For more information on the initial compilation you may want to read [Miles Sabin's post](https://milessabin.com/blog/2016/05/13/scalac-hacking/) on working with the compiler, if you haven't already.