#!/usr/bin/env bash

AWS_BUCKET=$(./reltio-custom-workflows/pipeline/readProperty.sh "aws.bucket")
echo "AWS_BUCKET = [$AWS_BUCKET]"
echo
# find all jars in deploy folder and put filenames to array
jars=()
while IFS=  read -r -d $'\0'; do
    jars+=("$(basename -- "$REPLY")")
done < <(find ./deploy -name "*.jar" -print0)
#translate jars array like '<li>$AWS_BUCKET/$jar</li>'
jarsList=""
for jar in "${jars[@]}"
do
    jarsList="$jarsList<li>$AWS_BUCKET/$jar</li>"
done
# read comma-separated emails list from the variable and convert it to the array
IFS=',' read -r -a emails <<< "$(./reltio-custom-workflows/pipeline/readProperty.sh "email.list")"
# translate emails array to the string like [{"email": "recipient@example.com"}]}]
emailList=""
for email in "${emails[@]}"
do
    emailList="$emailList{\"email\": \"$email\"},"
done
# remove last comma
emailList=${emailList::-1}
echo "emailList = [$emailList]"
# get current date in format like 2000-01-01
BUILD_DATE=$(date +%Y)
# create email body from reltio-custom-workflows/pipeline/jarBuildEmail.html template
EMAIL_BODY=$(cat ./reltio-custom-workflows/pipeline/jarBuildEmail.html)
EMAIL_BODY=${EMAIL_BODY//"{JAR_LIST}"/$jarsList}
EMAIL_BODY=${EMAIL_BODY//"{BITBUCKET_PROJECT_KEY}"/$3}
EMAIL_BODY=${EMAIL_BODY//"{REPOSITORY_NAME}"/$2}
EMAIL_BODY=${EMAIL_BODY//"{BUILD_DATE}"/$BUILD_DATE}
# remove new lines from the email body
EMAIL_BODY=${EMAIL_BODY//$'\n'/}
# remove return carriage from the email body
EMAIL_BODY=${EMAIL_BODY//$'\r'/}
# remove tabs from the email body
EMAIL_BODY=${EMAIL_BODY//$'\t'/}
# escape double quotes in the email body
EMAIL_BODY=${EMAIL_BODY//\"/\\\"}
# escape slashes in the email body
EMAIL_BODY=${EMAIL_BODY//"\\"/"\\\\"}
# escape ampersands in the email body
EMAIL_BODY=${EMAIL_BODY//\&/\\&}

cp ./reltio-custom-workflows/pipeline/sendEmail.json ./deploy/sendEmail.json
# replace {EMAIL_LIST} with the emails list
sed -i "s/EMAIL_LIST/$emailList/g" ./deploy/sendEmail.json
# replace {EMAIL_SUBJECT} with the email subject
sed -i "s/EMAIL_SUBJECT/$(./reltio-custom-workflows/pipeline/readProperty.sh "email.subject")/g" ./deploy/sendEmail.json
# replace {EMAIL_BODY} with the email body
sed -i "s|EMAIL_BODY|$EMAIL_BODY|g" ./deploy/sendEmail.json

curl --request POST --url https://api.sendgrid.com/v3/mail/send --header "Authorization: Bearer $1" --header 'Content-Type: application/json' --data @./deploy/sendEmail.json
echo
echo "Email was sent to $emailList"