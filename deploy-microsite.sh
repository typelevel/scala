#!/bin/bash

set -e

GIT_EMAIL="bot@typelevel.org"
GIT_NAME="Typelevel Bot"

git config --global user.name "$GIT_NAME"
git config --global user.email "$GIT_EMAIL"

sbt publishMicrosite
