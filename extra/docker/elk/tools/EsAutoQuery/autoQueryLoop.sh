#!/bin/bash

QUERY_FILE=${1:-query.json}
INDEX=${2:-logstash-*}
HOST_URL=${3:-http://localhost:9200}
URL=$HOST_URL/$INDEX/_search

function usage() {
    echo "Usage: $0 [QUERY_FILE [INDEX [HOST_URL]]]"
    echo
    echo "This script automatically sends the query file to elasticsearch"
    echo "each time it's modified."
}

if [ "${1}" == "--help" ];
then
    usage
    exit 0
fi

echo "Querying '$URL' with '$QUERY_FILE'"
while [ 1 ];
do
    curl -XGET "$URL" -H 'Content-Type: application/json' -d"@$QUERY_FILE" | js-beautify
    echo
    inotifywait -e modify query.json
done
