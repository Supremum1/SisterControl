#!/usr/bin/env sh
set -eu

python3 /app/server.py &
exec java -jar /app/sister-control-backend.jar
