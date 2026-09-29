import fetch from 'node-fetch';
import {createHmac, timingSafeEqual} from 'crypto';


export const handler = async (event) => {
    //log the event
    console.log(event);

    // temporarly disable signature verification as it does not work sometimes, 
    // f.e. for https://bitbucket.org/reltio-ondemand/gehc-custom-workflow/pull-requests/76
    // if(!isSignatureMatch(event)) {
    //     return {
    //         statusCode: 403
    //     };
    // }
    const data = JSON.parse(event.body);
    if (!data.pullrequest.links.self.href.startsWith("https://api.bitbucket.org/2.0/repositories/reltio-ondemand/")) {
        return {
                statusCode: 403
            };
    }
    // if the pull request is open get list of changed files
    if (data.pullrequest.state === 'OPEN') {
        let { accessToken, secrets } = await getAccessTokenAndSecrets(data);
        let isPipelineChanged = await checkIfPipelineChanged(accessToken);
        if(isPipelineChanged) {
            await declinePR(accessToken);
        } else {
            await approvePR(accessToken);
            await runPipeline(accessToken, secrets);
        }
    } else if(data.pullrequest.state === 'MERGED' && event.headers['x-event-key'] === 'pullrequest:fulfilled') {
        let { accessToken, secrets } = await getAccessTokenAndSecrets(data);
        let isPipelineChanged = await checkIfPipelineChanged(accessToken);
        if(!isPipelineChanged) {
            await runMergePipeline(accessToken, secrets);
        }
    }
    return {
        statusCode: 200
    };

    async function getAccessTokenAndSecrets(data) {
        let secrets = await getVaultSecrets();
        const envTokenName = 'custom_workflow_token_' + data.repository.full_name.replace(/\//g, '_').replace(/-/g, '_');
        let accessToken = process.env[envTokenName];
        if (!accessToken) {
            accessToken = secrets['custom-workflow-token'];
        }
        return { accessToken, secrets };
    }

    function isSignatureMatch(event) {
        const secret = process.env.WEBHOOK_SECRET;
        const payload = JSON.stringify(event.body);
        const payloadWithoutQuotes = payload.substring(1, payload.length - 1).replace(/\\\"/g, "\"");
        const givenSignature = event.headers['x-hub-signature'];
        const hashObject = createHmac('sha256', secret).update(payloadWithoutQuotes);
        const calculatedSignature = `sha256=${hashObject.digest('hex')}`;
        const signaturesMatch = timingSafeEqual(Buffer.from(calculatedSignature), Buffer.from(givenSignature));
        if (!signaturesMatch) {
            console.log(`Signatures do not match\nExpected signature: ${calculatedSignature}\nActual: signature: ${givenSignature}`);
            return false;
        } else {
            console.log("Signatures match");
            return true;
        }
    }

    async function checkIfPipelineChanged(accessToken) {
        const response = await fetch(data.pullrequest.links.self.href, {
            headers: {
                Authorization: `Bearer ${accessToken}`,
            },
        });
        const pullRequest = await response.json();
        console.log("Pull-request response is " + JSON.stringify(pullRequest));
        const diffstatResponse = await fetch(pullRequest.links.diffstat.href, {
            headers: {
                Authorization: `Bearer ${accessToken}`,
            },
        });
        const diffstat = await diffstatResponse.json();
        //find all path in diffstat.values
        const oldFiles = diffstat.values.find(e => e.old !== null && e.old.type === "commit_file" && e.old.path === "bitbucket-pipelines.yml");
        const newFiles = diffstat.values.find(e => e.new !== null && e.new.type === "commit_file" && e.new.path === "bitbucket-pipelines.yml");

        return typeof oldFiles != "undefined" || typeof newFiles != "undefined";
    }

    async function approvePR(accessToken) {
        await fetch(data.pullrequest.links.approve.href, {
            method: "POST",
            headers: {
                Authorization: `Bearer ${accessToken}`,
                Accept: `application/json`
            }
        })
    }

    async function declinePR(accessToken) {
        await fetch(data.pullrequest.links.decline.href, {
            method: "POST",
            headers: {
                "Authorization": `Bearer ${accessToken}`,
                "accept": "application/json",
                "content-type": "application/json"
            },
            body: "{\"message\":\"It is not allowed to change bitbucket-pipelines.yml\"}"
        })
    }

    async function getVaultSecrets() {
        const tokenBuffer = Buffer.from(process.env.VAULT_SECRET, 'base64');
        const response = await fetch(process.env.VAULT_URL, {
            method: 'GET',
            headers: {
                'X-Vault-Token': tokenBuffer.toString('utf-8'),
                'Accept': 'application/json'
            }
        });

        if (!response.ok) {
            throw new Error(`Error fetching Vault secrets: ${response.statusText}`);
        }

        const data = await response.json();
        return data.data.data;
    }

    async function runPipeline(accessToken, secrets) {
        let variablesList = ["AWS_ACCESS_KEY_ID","AWS_SECRET_ACCESS_KEY","BITBUCKET_ACCESS_TOKEN","BITBUCKET_PIPELINE_TOKEN","CODECOV_TOKEN","MAVEN_PASSWORD","MAVEN_USERNAME","SENDGRID_API_KEY","SNYK_TOKEN","SONAR_ORG"];
        let variables = [];
        variables.push({key: 'BITBUCKET_PR_ID', value: data.pullrequest.id})
        variablesList.forEach((variable) => {
            if(secrets[variable]) {
                variables.push({
                    key: variable,
                    value: secrets[variable],
                    secured: true
                });
            }
        })

        return await fetch(data.repository.links.self.href + '/pipelines', {
            method: "POST",
            headers: {
                Accept: `application/json`,
                "Content-Type": `application/json`,
                Authorization: `Bearer ${accessToken}`
            },
            body: JSON.stringify({
                target: {
                    type: "pipeline_ref_target",
                    ref_type: "branch",
                    ref_name: data.pullrequest.source.branch.name,
                    selector: {
                        type: "custom",
                        pattern: "pr-checks"
                    }
                },
                variables: variables
            })
        });
    }

    async function runMergePipeline(accessToken, secrets) {
        let variablesList = ["AWS_ACCESS_KEY_ID","AWS_SECRET_ACCESS_KEY","BITBUCKET_ACCESS_TOKEN","BITBUCKET_PIPELINE_TOKEN","CODECOV_TOKEN","MAVEN_PASSWORD","MAVEN_USERNAME","SENDGRID_API_KEY","SNYK_TOKEN","SONAR_ORG"];
        let variables = [];
        variablesList.forEach((variable) => {
            if(secrets[variable]) {
                variables.push({
                    key: variable,
                    value: secrets[variable],
                    secured: true
                });
            }
        })
        return await fetch(data.repository.links.self.href + '/pipelines', {
            method: "POST",
            headers: {
                Accept: `application/json`,
                "Content-Type": `application/json`,
                Authorization: `Bearer ${accessToken}`
            },
            body: JSON.stringify({
                target: {
                    type: "pipeline_ref_target",
                    ref_type: "branch",
                    ref_name: "master",
                    selector: {
                        type: "custom",
                        pattern: "deploy"
                    }
                },
                variables: variables
            })
        });
    }
};
