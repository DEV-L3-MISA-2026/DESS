#!/usr/bin/env bash
# Stops the containers. The database volume is kept.
set -euo pipefail
. "$(dirname "$0")/_env.sh"
dc down
