#!/bin/bash
URL="http://${3:-localhost}:${2:-8080}/user/${1:-userId}/all-groups-feed"
echo "$URL" 1>&2
curl -s "$URL"
