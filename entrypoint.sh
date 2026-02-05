#!/bin/bash
set -e

# Fix gradlew permissions and line endings if the file exists
if [ -f gradlew ]; then
    sed -i 's/\r$//' gradlew
    chmod +x gradlew
fi

# Execute the command passed to the container
exec "$@"
