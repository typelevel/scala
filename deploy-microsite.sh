#!/bin/bash

set -e

if [ "$TRAVIS_BRANCH" = "typelevel-readme" -a "$TRAVIS_PULL_REQUEST" = "false" ]; then
  GIT_EMAIL="bot@typelevel.org"
  GIT_NAME="Typelevel Bot"

  git config --global user.name "$GIT_NAME"
  git config --global user.email "$GIT_EMAIL"
  git config --global push.default simple

  sbt publishMicrosite
else
  echo "Skipping deployment!"
fi

