#!/bin/bash
dir=$(cd -P -- "$(dirname -- "$0")" && pwd -P)

./addUserToGroup.sh 1 1
echo $'\n\n'

./addUserToGroup.sh 1 2
echo $'\n\n'

./addUserToGroup.sh 2 2
echo $'\n\n'

./getUserGroups.sh 1
echo $'\n\n'

./getUserGroups.sh 2
echo $'\n\n'

./makePostToGroupByUser.sh 1 1 "my first message" "user name 1"
echo $'\n\n'

echo "Sleeping for 3 seconds."
sleep 3

./makePostToGroupByUser.sh 2 1 "my second message" "user name 1"
echo $'\n\n'

echo "Sleeping for 3 seconds."
sleep 3

./makePostToGroupByUser.sh 2 2 "my 1st msg" "user name 2"
echo $'\n\n'

./makePostToGroupByUser.sh 1 2 "lets try hacking into other group" "user name 2"
echo $'\n\n'

./getGroupFeed.sh 1
echo $'\n\n'

./getGroupFeed.sh 2
echo $'\n\n'

./getGroupFeedForUser.sh 1
echo $'\n\n'

./getGroupFeedForUser.sh 2
echo $'\n\n'

./getGroupFeedForUser.sh 1
echo $'\n\n'

./getGroupFeedForUser.sh 2
echo $'\n\n'
