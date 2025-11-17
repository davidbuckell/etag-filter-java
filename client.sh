#!/bin/bash

ETAG_HEADER="$1"
URL="http://localhost:8080/items/1"
#URL="https://etag-java-v1-adh5h7cwanhxd2ee.uksouth-01.azurewebsites.net/items/1"

while true; do
  RESPONSE=$(curl -s -i -H "If-None-Match: \"$ETAG_HEADER\"" "$URL")

  STATUS=$(echo "$RESPONSE" | head -n1 | awk '{print $2}')
  RESPONSE_ETAG=$(echo "$RESPONSE" | grep -i '^ETag:' | awk '{print $2}' | tr -d '\r')

  # Only print ETag if non-empty
  if [ -n "$RESPONSE_ETAG" ]; then
    echo "$(date '+%Y-%m-%d %H:%M:%S') Status: $STATUS ETag: $RESPONSE_ETAG"
  else
    echo "$(date '+%Y-%m-%d %H:%M:%S') Status: $STATUS"
  fi

  # Print body for 200 responses
  if [ "$STATUS" = "200" ]; then
    BODY=$(echo "$RESPONSE" | sed -n '/^\s*$/,$p' | tail -n +2)
    echo "$BODY"
  fi

  echo "----------------------------------------"
  sleep 1
done
