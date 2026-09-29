
import fetch from 'node-fetch';
export const handler = async (event) => {
    const data = JSON.parse(event.body);
    // get access token from environment variables by repository name, replace - with _
    const accessToken = process.env[`BITBUCKET_ACCESS_TOKEN_${data.repository.name.replace(/-/g, '_')}`];
    // if the pull request is open get list of changed files
    if (data.pullrequest.state === 'OPEN') {
        const response = await fetch(data.pullrequest.links.self.href, {
            headers: {
                Authorization: `Bearer ${accessToken}`,
            },
        });
        const pullRequest = await response.json();
        const diffstatResponse = await fetch(pullRequest.links.diffstat.href, {
            headers: {
                Authorization: `Bearer ${accessToken}`,
            },
        });
        const diffstat = await diffstatResponse.json();
        //find all path in diffstat.values
        const oldFiles = diffstat.values.find(e => e.old !== null && e.old.type === "commit_file" && e.old.path === "bitbucket-pipelines.yml");
        const newFiles = diffstat.values.find(e => e.new !== null && e.new.type === "commit_file" && e.new.path === "bitbucket-pipelines.yml");

        if(typeof oldFiles != "undefined" || typeof newFiles != "undefined") {
            await fetch(data.pullrequest.links.decline.href, {
                method: "POST",
                headers: {
                    "Authorization": `Bearer ${accessToken}`,
                    "accept": "application/json",
                    "content-type": "application/json"
                },
                body: "{\"message\":\"It is not allowed to change bitbucket-pipelines.yml\"}"
            })
            return {
                statusCode: 200
            };
        } else {
            await fetch(data.pullrequest.links.approve.href, {
                method: "POST",
                headers: {
                    Authorization: `Bearer ${accessToken}`,
                    Accept: `application/json`
                }
            })
            return {
                statusCode: 200
            }
        }
    }
    return {
        statusCode: 200
    };
};
