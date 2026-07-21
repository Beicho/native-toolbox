#!/bin/sh
GRADLE_VERSION=8.2
BASE_URL="https://services.gradle.org/distributions"
GRADLE_DIST="gradle-${GRADLE_VERSION}-bin.zip"
GRADLE_HOME="${HOME}/.gradle/wrapper/dists/gradle-${GRADLE_VERSION}"

if [ ! -d "$GRADLE_HOME" ]; then
    echo "Downloading Gradle $GRADLE_VERSION..."
    mkdir -p "$GRADLE_HOME"
    wget -q "${BASE_URL}/${GRADLE_DIST}" -O "/tmp/${GRADLE_DIST}"
    unzip -q "/tmp/${GRADLE_DIST}" -d "$GRADLE_HOME"
    rm "/tmp/${GRADLE_DIST}"
fi

exec "${GRADLE_HOME}/gradle-${GRADLE_VERSION}/bin/gradle" "$@"
