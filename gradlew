#!/usr/bin/env sh

# Gradle wrapper script
set -e

DIR="$(cd "$(dirname "$0")" && pwd)"
export GRADLE_USER_HOME="$DIR/.gradle"

if [ -z "$GRADLE_VERSION" ]; then
  GRADLE_VERSION=7.4.2
fi

if [ -z "$GRADLE_OPTS" ]; then
  GRADLE_OPTS=""
fi

if [ -z "$JAVA_HOME" ]; then
  echo "JAVA_HOME is not set. Please set it to your JDK installation."
  exit 1
fi

exec "$DIR/gradlew" "$@"