#!/usr/bin/env bash
file="./repository.properties"
while IFS='=' read -r key value; do
  #if key equals to $1 then return value
  if [ "$key" == "$1" ]; then
    echo "${value//$'\r'/}"
    exit 0
  fi
done <"$file"
exit 1
