#!/usr/bin/env bash
mkdir deploy
jarFileNames=()
while IFS= read -r -d $'\0'; do
  jarFileNames+=("$REPLY")
done < <(find . -name "*-everything.jar" -print0)

for jarFileName in "${jarFileNames[@]}"; do
  jarFileNameWithoutEverything=${jarFileName%-everything.jar}
  echo "jarFileName = [$jarFileNameWithoutEverything]"
  cp "$jarFileName" "./deploy/$(basename -- "$jarFileNameWithoutEverything").jar"
done
