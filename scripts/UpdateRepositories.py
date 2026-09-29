import base64
import os
import requests
import shutil
import subprocess
import sys


def get_command_line_parameters():
    if len(sys.argv) != 6:
        print("Usage: script.py <pipeline_file> <bitbucket_token> <username> <repositories_list> <webhook_secret>")
        sys.exit(1)
    return sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4].split(','), sys.argv[5]


def get_auth_header(username, token):
    auth = base64.b64encode(f"{username}:{token}".encode()).decode()
    return {
        "Authorization": f"Basic {auth}",
        "Content-Type": "application/json"
    }


def remove_pipeline_variables(repo, token, username):
    url = f"https://api.bitbucket.org/2.0/repositories/{repo}/pipelines_config/variables"
    headers = get_auth_header(username, token)
    response = requests.get(url, headers=headers)
    sonar_variables = ["SONAR_TOKEN", "SONAR_PROJECT_KEY"]
    if response.status_code == 200:
        # delete all variables except sonar ones
        for variable in response.json()["values"]:
            if variable["key"] not in sonar_variables:
                variable_url = f"https://api.bitbucket.org/2.0/repositories/{repo}/pipelines_config/variables/{variable['uuid']}"
                response = requests.delete(variable_url, headers=headers)
                if response.status_code != 204:
                    print(f"Failed to delete variable {variable['key']} for {repo}")
        print(f"Pipeline variables removed for {repo}")
    else:
        print(f"Failed to get pipeline variables for {repo}")


def clone_repository(repo):
    subprocess.run(["git", "clone", f"https://bitbucket.org/{repo}.git"])
    print(f"Repository {repo} cloned")


def replace_pipeline_file(repo, pipeline_file):
    repo_dir = repo.split('/')[-1]
    pipeline_path = os.path.join(repo_dir, "bitbucket-pipelines.yml")
    if os.path.exists(pipeline_path):
        os.remove(pipeline_path)
    shutil.copy(pipeline_file, pipeline_path)
    print(f"Pipeline file replaced for {repo}")


def commit_and_push_changes(repo):
    repo_dir = repo.split('/')[-1]
    subprocess.run(["git", "-C", repo_dir, "add", "bitbucket-pipelines.yml"])
    subprocess.run(["git", "-C", repo_dir, "commit", "-m", "Update pipeline file"])
    subprocess.run(["git", "-C", repo_dir, "push"])
    print(f"Changes committed and pushed for {repo}")


def get_current_user_uuid(username, token):
    url = "https://api.bitbucket.org/2.0/user"
    headers = get_auth_header(username, token)
    response = requests.get(url, headers=headers)
    if response.status_code == 200:
        user_data = response.json()
        print("User UUID retrieved")
        return user_data["uuid"]
    else:
        print("Failed to get user information")
        print(response.status_code, response.text)
        return None


def update_branch_restriction(repo, token, username, branch):
    user_uuid = get_current_user_uuid(username, token)
    base_url = f"https://api.bitbucket.org/2.0/repositories/{repo}/branch-restrictions"
    headers = get_auth_header(username, token)

    # Get all branch restrictions
    restrictions = []
    url = base_url
    while url:
        response = requests.get(url, headers=headers)
        if response.status_code == 200:
            data = response.json()
            restrictions.extend(data["values"])
            url = data.get("next")
        else:
            print(f"Failed to get branch restrictions for {repo}")
            print(response.status_code, response.text)
            return

    # Check for existing "push" restriction
    push_restriction = None
    for restriction in restrictions:
        if restriction["kind"] == "push" and restriction["pattern"] == branch:
            push_restriction = restriction
            break

    if push_restriction:
        # Check if the user is already in the list
        found_user = None
        for user in push_restriction["users"]:
            if user["uuid"] == user_uuid:
                found_user = user
                break
        if not found_user:
            push_restriction["users"].append({"uuid": user_uuid})
            restriction_url = f"{base_url}/{push_restriction['id']}"
            response = requests.put(restriction_url, headers=headers, json=push_restriction)
            if response.status_code == 200:
                print(f"Branch restriction updated for {branch} in {repo}")
            else:
                print(f"Failed to update branch restriction for {branch} in {repo}")
                print(response.status_code, response.text)
    else:
        # Create a new "push" restriction
        payload = {
            "kind": "push",
            "pattern": branch,
            "users": [{"uuid": user_uuid}]
        }
        response = requests.post(base_url, headers=headers, json=payload)
        if response.status_code == 201:
            print(f"Branch restriction created for {branch} in {repo}")
        else:
            print(f"Failed to create branch restriction for {branch} in {repo}")
            print(response.status_code, response.text)


def delete_and_add_webhooks(repo, token, username, webhook_secret):
    url = f"https://api.bitbucket.org/2.0/repositories/{repo}/hooks"
    headers = get_auth_header(username, token)
    response = requests.get(url, headers=headers)
    if response.status_code == 200:
        for hook in response.json()["values"]:
            if "Codecov" not in hook["description"]:
                hook_url = f"{url}/{hook['uuid']}"
                delete_response = requests.delete(hook_url, headers=headers)
                if delete_response.status_code != 204:
                    print(f"Failed to delete webhook {hook['description']} for {repo}")
        print(f"Webhooks deleted for {repo}")
    else:
        print(f"Failed to get webhooks for {repo}")

    payload = {
        "description": "Custom workflow validator",
        "url": "https://yiaw5pvuiivwnriuhxp7qjqcaa0krpbn.lambda-url.us-east-1.on.aws/",
        "active": True,
        "events": ["pullrequest:created", "pullrequest:updated", "pullrequest:fulfilled"],
        "secret": webhook_secret
    }
    response = requests.post(url, headers=headers, json=payload)
    if response.status_code == 201:
        print(f"Webhook added for {repo}")
    else:
        print(f"Failed to add webhook for {repo}")
        print(response.status_code, response.text)


def main():
    pipeline_file, bitbucket_token, username, repositories, webhook_secret = get_command_line_parameters()
    for repo in repositories:
        repo = "reltio-ondemand/" + repo
        remove_pipeline_variables(repo, bitbucket_token, username)
        update_branch_restriction(repo, bitbucket_token, username, "master")
        clone_repository(repo)
        replace_pipeline_file(repo, pipeline_file)
        commit_and_push_changes(repo)
        delete_and_add_webhooks(repo, bitbucket_token, username, webhook_secret)


if __name__ == "__main__":
    main()
