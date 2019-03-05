#!/bin/bash
USER_ID=${2:-userId}
CONTENT=${3:-post_content}
USERNAME=${4:-userName}

URL="http://${6:-localhost}:${5:-8080}/group/${1:-groupId}"
BODY='{
	"userId":"'"$USER_ID"'",
	"userName":"'"$USERNAME"'",
	"content":"'"$CONTENT"'"
}'

echo "$URL" 1>&2
echo "$BODY"1>&2

curl -s -XPOST "$URL" -H "Content-Type: application/json" \
	--data "$BODY"
