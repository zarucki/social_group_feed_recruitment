#!/bin/bash
URL="http://${3:-localhost}:${2:-8080}/group/${1:-groupId}/feed"
echo "$URL" 1>&2
curl -s "$URL"
