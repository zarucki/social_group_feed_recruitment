#!/bin/bash
dir=$(cd -P -- "$(dirname -- "$0")" && pwd -P)
USER_ID=${1:-USER_ID}

./addUserToGroup.sh "$USER_ID" 1
echo $'\n\n'
./addUserToGroup.sh "$USER_ID" 2
echo $'\n\n'
./getUserGroups.sh "$USER_ID"
echo $'\n\n'
./makePostToGroupByUser.sh 1 "$USER_ID" "my first message" "user name 1"
echo $'\n\n'
