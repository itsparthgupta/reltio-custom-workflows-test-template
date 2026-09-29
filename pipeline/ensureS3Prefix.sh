#!/usr/bin/env bash
set -euo pipefail

# Create the S3 prefix/folder from aws.bucket when it is missing.
# aws.bucket may be "bucket", "bucket/prefix", or "s3://bucket/prefix".
# The bucket itself must already exist.

location="${1:-}"
region="${2:-}"

trim() {
  local value="$1"
  value="${value#"${value%%[![:space:]]*}"}"
  value="${value%"${value##*[![:space:]]}"}"
  value="${value//$'\r'/}"
  printf '%s' "$value"
}

location="$(trim "$location")"
region="$(trim "$region")"

if [ -z "$location" ]; then
  echo "ERROR: S3 location is required (aws.bucket)"
  exit 1
fi

if [ -z "$region" ]; then
  echo "ERROR: AWS region is required (aws.region)"
  exit 1
fi

normalized="${location#s3://}"
while [ "${normalized#/}" != "$normalized" ]; do
  normalized="${normalized#/}"
done
while [ "${normalized%/}" != "$normalized" ]; do
  normalized="${normalized%/}"
done
bucket="${normalized%%/*}"
prefix=""
if [ "$normalized" != "$bucket" ]; then
  prefix="${normalized#${bucket}/}"
fi

if [ -z "$bucket" ]; then
  echo "ERROR: could not parse bucket name from [$location]"
  exit 1
fi

if [ -z "$prefix" ]; then
  echo "No folder prefix in aws.bucket [$location]; nothing to create"
  exit 0
fi

folder_key="${prefix%/}/"
echo "Checking whether S3 folder [s3://${bucket}/${folder_key}] exists"

set +e
list_output="$(aws s3api list-objects-v2 \
  --bucket "$bucket" \
  --prefix "$folder_key" \
  --max-keys 1 \
  --region "$region" \
  --query 'KeyCount' \
  --output text 2>&1)"
list_status=$?
set -e

if [ "$list_status" -ne 0 ]; then
  echo "ERROR: cannot list S3 folder [s3://${bucket}/${folder_key}]"
  printf '%s\n' "$list_output"
  exit 1
fi

key_count="$(trim "$list_output")"
if [ -n "$key_count" ] && [ "$key_count" != "0" ] && [ "$key_count" != "None" ] && [ "$key_count" != "null" ]; then
  echo "S3 folder [s3://${bucket}/${folder_key}] already exists"
  exit 0
fi

echo "S3 folder [s3://${bucket}/${folder_key}] does not exist; creating it"

set +e
put_output="$(aws s3api put-object \
  --bucket "$bucket" \
  --key "$folder_key" \
  --region "$region" 2>&1)"
put_status=$?
set -e

if [ "$put_status" -ne 0 ]; then
  echo "ERROR: failed to create S3 folder [s3://${bucket}/${folder_key}]"
  printf '%s\n' "$put_output"
  exit 1
fi

echo "Created S3 folder [s3://${bucket}/${folder_key}]"
exit 0
