# Typelevel Scala Compiler Documentation

The consolidated documentation for Typelevel Scala compiler.

Based on Ensime documentation (see: http://ensime.github.io/)

## TODO

Pending tasks:

* edit `_layouts/page.html`, `_layouts/homepage.html`, and `_layouts/section.html` to point the *edit* link to the proper path. Can't be done until this project has a final home :)
* decide where will it be stored (gh-pages, other?) to create bash script for easy publication of changes.
* add more documentation!

## Setup

### Requirements

This documentation uses [Jekyll](https://jekyllrb.com/), a Ruby static website generator.

To install Ruby we recommend to use [RVM](https://rvm.io/rvm/install) for your platform

Then install Bundler (`gem install bundler`)

### Local deployment

To run the documentation locally execute `bin/run.sh`. This will update your gems and run Jekyll in `watch` mode, 
which means it will detect any changes to the pages and reload automatically.

Then load `http://127.0.0.1:4000/` to see the blog.

## Hack the theme

This site uses the Jekyll theme [Hyde](http://hyde.getpoole.com/) by [@mdo](http://hyde.getpoole.com/). 
More information about the theme [in its official page](http://hyde.getpoole.com/)

### Update the gems

Make sure they match the [versions](https://pages.github.com/versions/) GitHub Pages uses.

### Update the base theme

```
git remote add hyde git@github.com:poole/hyde.git
git fetch hyde
git merge hyde master
```
