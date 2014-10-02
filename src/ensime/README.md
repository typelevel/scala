ENSIME
======

[ENSIME](https://github.com/ensime/ensime-server) provides IDE-like
features in [GNU Emacs](http://www.gnu.org/software/emacs/) (and
other text editors if somebody wants to write/maintain a plugin).

ENSIME does not compile sources to `.class` files: `ant` is still
required to produce output.

Included here is a `.ensime.SAMPLE` which can be used as the basis of
a configuration for working on scala itself.

Paths must be absolute, so you will need to customise this sample for
your environment: after running `ant init` (to download all jars and
sources) this should be as simple as performing a search and replace
on `BASE` and `M2_REPO` with your paths.

To speed things up and reduce memory/CPU overhead, delete the
downstream modules that you're not interested in.

Binary Compatibility
--------------------

ENSIME uses the same version of the presentation compiler as is used
by the project, therefore a build of ENSIME for the specified
`:scala-version` must exist.

When working on maintenance branches of scala, it should be as
simple as using the last stable release of scala (on that branch) as
the version number. In this mode, ENSIME will expect `.class` files to
exist in the `build/quick` (produced by a quick build).

However, when working on development branches it is unlikely that a
version of ENSIME will be available: so use the most recent stable
release. In this case, ENSIME will fail horribly when attempting to
read the `.class` files (e.g. the pickle format may have changed), so
delete all the `:target` and `:targets` settings and type `C-c C-c a`
(`ensime-typecheck-all`) when starting your session to use "source
only" mode. It may take several minutes for typechecking to complete,
and there will be many false negative warnings. Warnings that are a
result of new language features cannot be fixed. For what remains,
open up the `interactive` module and fix some bugs in the presentation
compiler :-P

NOTE: If https://github.com/ensime/ensime-server/issues/624 has been
implemented by the time you are reading this, then you might not have
to issue the `ensime-typecheck-all` call or tweak the `:targets`.
