#! /bin/bash

if [ $# -ne 1 ]
then
	echo invalid parameters $*, specify a single parameter as the topic to listen on
	exit 1
fi

while true
do
	curl "http://localhost:3904/events/$1/TEST/1?timeout=60000"
	echo ""
done