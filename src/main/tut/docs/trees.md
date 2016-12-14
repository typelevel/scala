---
layout: docs
title: Trees
---

This page describes the Scala [AST]() and its implementation within the Scala compiler code. 

The AST is designed to capture the syntactic constructs of the Scala Programming Language, 
as described in the [language specification](https://www.scala-lang.org/files/archive/spec/2.12/13-syntax-summary.html).

The implementation of the Tree is encoded in [this file](https://github.com/scala/scala/blob/2.13.x/src/reflect/scala/reflect/internal/Trees.scala) 

The AST is organised in a series of traits and self-types, which follows the following structure: 

WIP
