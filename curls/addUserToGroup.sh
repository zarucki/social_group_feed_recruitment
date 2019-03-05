#!/bin/bash
URL="http://${4:-localhost}:${3:-8080}/user/${1:-userId}/add-to-group/${2:-groupId}"
echo "$URL" 1>&2
curl -s "$URL"
