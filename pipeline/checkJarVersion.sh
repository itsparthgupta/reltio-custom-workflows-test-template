#!/usr/bin/env bash

# get jar-file name from module name and version from the pom.xml file
function getJarFileName() {
  local module_name=$1
  local version=$2
  local jar_file_name="$module_name-$version.jar"
  echo "$jar_file_name"
}

# list all modules and versions from the pom.xml file using grep and sed
function listModules() {
  local pom_file=$1
  while IFS= read -r -d $'\0'; do
    echo "$REPLY"
  done < <(grep -oP "(?<=<module>)[^<]+" "$pom_file" | tr ' ' '\n' | tr -d '\r' | tr '\n' '\0')
}

# get module version from module pom.xml file using grep and sed
function getVersion() {
  local pom_file=$1
  awk '/<parent>/,/<\/parent>/ {next} /<version>/ {print gensub(/.*<version>([^<]+)<\/version>.*/, "\\1", "g"); exit}' "$pom_file"
}

function getArtefactId() {
  local pom_file=$1
  awk '/<parent>/,/<\/parent>/ {next} /<artifactId>/ {print gensub(/.*<artifactId>([^<]+)<\/artifactId>.*/, "\\1", "g"); exit}' "$pom_file"
}

pom_file=$1
s3bucket=$2

# call listModules function and iterate over the result, collect all jar files, that are existed in the s3bucket
function getBucketFiles() {
  local pom_file=$1
  local s3bucket=$2
  for module in $(listModules "$pom_file"); do
    version=$(getVersion "$module/pom.xml")
    artefactId=$(getArtefactId "$module/pom.xml")
    jar_file_name=$(getJarFileName "$artefactId" "$version")
    if aws s3 ls "$s3bucket/$jar_file_name" 2>&1 | grep -q "$jar_file_name"; then
      echo "$jar_file_name"
    fi
  done
}

# function to add general comment to the PR
function addGeneralComment() {
  local commentContent=$1
  # execute curl command and print response to the console output
  response=$(curl -s -w "%{http_code}" --request POST \
    --url "https://api.bitbucket.org/2.0/repositories/$BITBUCKET_WORKSPACE/$BITBUCKET_REPO_SLUG/pullrequests/$BITBUCKET_PR_ID/comments" \
    --header "Authorization: Bearer $BITBUCKET_ACCESS_TOKEN" \
    --header 'Accept: application/json' \
    --header 'Content-Type: application/json' \
    --data "$commentContent")
  http_code=$(tail -n1 <<<"$response" | tail -c 4)
  # print content if status is not 201
  if [ "$http_code" != "201" ]; then
    sed '$ d' <<<"$response" # get all but the last line which contains the status code
    echo
    echo "$http_code"
    echo
    exit 1
  fi
}

function checkIfCommentExists() {
  response=$(curl -s -w "%{http_code}" --request GET \
    --url "https://api.bitbucket.org/2.0/repositories/$BITBUCKET_WORKSPACE/$BITBUCKET_REPO_SLUG/pullrequests/$BITBUCKET_PR_ID/comments" \
    --header "Authorization: Bearer $BITBUCKET_ACCESS_TOKEN" \
    --header 'Accept: application/json')
  # get status code from the last line as last 3 letters
  http_code=$(tail -n1 <<<"$response" | tail -c 4)

  # print content if status is not 201
  if [ "$http_code" != "200" ]; then
    sed '$ d' <<<"$response" # get all but the last line which contains the status code
    echo
    echo "$http_code"
    echo
    exit 1
  fi
  #remove last http_code from the end of the response
  response=${response::-3}
  # use jq to parse json with deleted = false and contains the following string
  ./jq -r '.values[] | select(.deleted == false) | select(.content.raw | contains("[WARNING] The following files already exist in the bucket, please update project version or they will be overwritten after the merge:")) | .id' <<<"$response"
}

# translate bucketFiles array to comma-separated string
jarFilesList=""
for jarFile in $(getBucketFiles "$pom_file" "$s3bucket"); do
  jarFilesList="$jarFilesList$jarFile, "
done
#if jarFilesList is not empty
if [ -n "$jarFilesList" ]; then
  #check if comment already exists
  commentId=$(checkIfCommentExists)
  #if comment already exists, then do nothing
  if [ -n "$commentId" ]; then
    exit 0
  fi
  # remove last comma
  jarFilesList=${jarFilesList::-2}.
  # add comment to the pull request, that jar files already exist in the s3bucket
  commentContent='{
  "content" : {
    "raw": "[WARNING] The following files already exist in the bucket, please update project version or they will be overwritten after the merge: {LIST}"
    }
  }'
  # replace {LIST} with the jarFilesList
  commentContent=${commentContent//"{LIST}"/$jarFilesList}
  addGeneralComment "$commentContent"
else
  echo "No jar files found in the bucket"
fi
