#!/bin/bash
# $1 is container id, which you can find by docker container ls
docker exec -it ${1:-my-mongo}  mongo -u root -p example

