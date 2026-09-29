import os
import sys
import xml.etree.ElementTree as XmlTree
import requests
import json

JDK_VERSION = 17
AWS_VERSION = '1.11.802'
AWS2_VERSION = '2.42.33'


def print_project_info(root, ns):
    group_id = root.find(f'{{{ns}}}groupId')
    artifact_id = root.find(f'{{{ns}}}artifactId')
    version = root.find(f'{{{ns}}}version')
    if group_id is not None:
        print("Group ID: " + group_id.text)
    if artifact_id is not None:
        print("Artifact ID: " + artifact_id.text)
    if version is not None:
        print("Version: " + version.text)
    modules = root.find(f'{{{ns}}}modules')
    if modules is not None:
        print("Modules:")
        for module in modules.findall(f'{{{ns}}}module'):
            print(module.text)


def is_comment_exists(message, filename, url):
    headers = {
        "Accept": "application/json",
        "Authorization": "Bearer " + os.environ['BITBUCKET_ACCESS_TOKEN']
    }
    response = requests.request(
        "GET",
        url,
        headers=headers
    )
    if response.status_code != 200:
        print("ERROR: failed to read comments")
        print(response.status_code)
        print(response.text)
        return False
    # check if response json has comment with the same message and filename and deleted false
    json_response = json.loads(response.text)
    for comment in json_response['values']:
        if comment['content']['raw'] == message \
                and comment['inline']['path'] == filename \
                and comment['deleted'] is False:
            return True
    return False


def add_general_comment(message, filename):
    if 'BITBUCKET_PR_ID' not in os.environ:
        print("BITBUCKET_PR_ID is not present. Skipping comment.")
        return

    # remove ./ from filename
    filename = filename[2:]
    print("Adding comment: " + message)
    print("Filename: " + filename)

    url = f"https://api.bitbucket.org/2.0/repositories/{os.environ['BITBUCKET_WORKSPACE']}/{os.environ['BITBUCKET_REPO_SLUG']}/pullrequests/{os.environ['BITBUCKET_PR_ID']}/comments"

    if is_comment_exists(message, filename, url):
        print("Comment already exists")
        return

    headers = {
        "Accept": "application/json",
        "Content-Type": "application/json",
        "Authorization": "Bearer " + os.environ['BITBUCKET_ACCESS_TOKEN']
    }
    payload = json.dumps({
        "content": {"raw": f"{message}"},
        "inline": {"path": f"{filename}"}
    })

    response = requests.request(
        "POST",
        url,
        data=payload,
        headers=headers
    )

    if response.status_code != 201:
        print("ERROR: failed to add comment")
        print(response.status_code)
        print(response.text)
    else:
        print("Comment added")


def check_jdk_version(root, ns, filename):
    jdk_version = None
    build = root.find(f'{{{ns}}}build')
    if build is not None:
        plugin_management = build.find(f'{{{ns}}}pluginManagement')
        if plugin_management is not None:
            management_plugins = plugin_management.find(f'{{{ns}}}plugins')
            if management_plugins is not None:
                jdk_version = get_jdk_version_in_plugins(management_plugins, ns)

        plugins_tag = build.find(f'{{{ns}}}plugins')
        if plugins_tag is not None:
            plugins = plugins_tag.find(f'{{{ns}}}plugins')
            if plugins is not None:
                jdk_version = get_jdk_version_in_plugins(plugins, ns)

    if jdk_version is not None and jdk_version.startswith('$'):
        jdk_version = resolve_property(jdk_version, root, ns)

    if jdk_version is not None and int(jdk_version) > JDK_VERSION:
        print(f"ERROR: JDK version is {jdk_version}, it should be no more than {JDK_VERSION}")
        add_general_comment("JDK version is " + jdk_version + ", it should be no more than " + JDK_VERSION, filename)
        return False
    else:
        if jdk_version is not None:
            print("JDK version is ", JDK_VERSION)
        else:
            print("JDK version is not specified")
        return True


def resolve_property(pom_property, root, ns):
    return root.find(f'{{{ns}}}properties').find(pom_property[2:-1], ns).text


def get_jdk_version_in_plugins(plugins, ns):
    for plugin in plugins.findall(f'{{{ns}}}plugin'):
        group_id = plugin.find(f'{{{ns}}}groupId')
        artifact_id = plugin.find(f'{{{ns}}}artifactId')
        if group_id is not None and artifact_id is not None:
            if group_id.text == 'org.apache.maven.plugins' and artifact_id.text == 'maven-compiler-plugin':
                configuration = plugin.find(f'{{{ns}}}configuration')
                if configuration is not None:
                    target = configuration.find(f'{{{ns}}}target')
                    release = configuration.find(f'{{{ns}}}release')
                    if target is not None:
                        print("Target: " + target.text)
                        return target.text
                    else:
                        if release is not None:
                            print("Release: " + release.text)
                            return release.text
    return None


def check_aws_dependencies_has_correct_version(root, ns, filename):
    dependency_management = root.find(f'{{{ns}}}dependencyManagement')
    if dependency_management is None:
        dependencies = root.find(f'{{{ns}}}dependencies')
    else:
        dependencies = dependency_management.find(f'{{{ns}}}dependencies')
    if dependencies is not None:
        for dependency in dependencies.findall(f'{{{ns}}}dependency'):
            group_id = dependency.find(f'{{{ns}}}groupId')
            artifact_id = dependency.find(f'{{{ns}}}artifactId')
            version = dependency.find(f'{{{ns}}}version')
            if group_id is not None and artifact_id is not None and version is not None:
                if group_id.text == 'com.amazonaws' \
                        and (artifact_id.text.startswith('aws-java-sdk') or artifact_id.text == 'jmespath-java'):
                    if version.text != AWS_VERSION:
                        print("ERROR: AWS version is not ", AWS_VERSION)
                        add_general_comment("ERROR: AWS version is not " + AWS_VERSION, filename)
                        return False
                    else:
                        add_general_comment(
                            "WARNING: AWS SDK for Java v1 (com.amazonaws) has reached end-of-support on December 31, 2025. Please upgrade your code to use AWS SDK v2 (software.amazon.awssdk). For more details, see [Reltio Doc](https://docs.reltio.com/en/objectives/manage-workflow-tasks/workflow-management-at-a-glance/workflow-management-set-up/custom-workflow-review-checklist)",
                            filename)
                        print("AWS version is ", AWS_VERSION)
                        return True
    return True


def check_aws_v2_dependencies_has_correct_version(root, ns, filename):
    dependency_management = root.find(f'{{{ns}}}dependencyManagement')
    if dependency_management is None:
        dependencies = root.find(f'{{{ns}}}dependencies')
    else:
        dependencies = dependency_management.find(f'{{{ns}}}dependencies')
    if dependencies is not None:
        for dependency in dependencies.findall(f'{{{ns}}}dependency'):
            group_id = dependency.find(f'{{{ns}}}groupId')
            artifact_id = dependency.find(f'{{{ns}}}artifactId')
            version = dependency.find(f'{{{ns}}}version')
            if group_id is not None and artifact_id is not None and version is not None:
                if group_id.text == 'software.amazon.awssdk':
                    if version.text != AWS2_VERSION:
                        print("ERROR: AWS version is not ", AWS2_VERSION)
                        add_general_comment("ERROR: AWS version is not " + AWS2_VERSION, filename)
                        return False
                    else:
                        print("AWS version is ", AWS2_VERSION)
    return True


def check_scope_is_provided(root, group_id_value, artifact_id_value, ns, filename):
    dependency_management = root.find(f'{{{ns}}}dependencyManagement')
    if dependency_management is None:
        dependencies = root.find(f'{{{ns}}}dependencies')
    else:
        dependencies = dependency_management.find(f'{{{ns}}}dependencies')
    if dependencies is not None:
        for dependency in dependencies.findall(f'{{{ns}}}dependency'):
            group_id = dependency.find(f'{{{ns}}}groupId')
            artifact_id = dependency.find(f'{{{ns}}}artifactId')
            scope = dependency.find(f'{{{ns}}}scope')
            if group_id is not None and artifact_id is not None:
                if group_id.text == group_id_value and artifact_id.text == artifact_id_value:
                    if scope is None or scope.text != 'provided':
                        print(f"ERROR: [{group_id_value}:{artifact_id_value}] scope is not 'provided'")
                        add_general_comment(f"ERROR: [{group_id_value}:{artifact_id_value}] scope is not 'provided'",
                                            filename)
                        return False
                    else:
                        print(f"[{group_id_value}:{artifact_id_value}] scope is provided")
                        return True
    return True


def check_module_poms(root, pom_dir, ns):
    modules = root.find(f'{{{ns}}}modules')
    summary_result = 0
    if modules is not None:
        for module in modules.findall(f'{{{ns}}}module'):
            module_pom_filename = pom_dir + '/' + module.text + '/pom.xml'
            print("Checking module pom: " + module_pom_filename)
            summary_result += check_pom(module_pom_filename)
    return summary_result


def check_pom(filename):
    pom = XmlTree.parse(filename)
    root = pom.getroot()
    ns = root.tag[1:].split("}")[0]
    print("ns: " + ns)
    print_project_info(root, ns)
    is_jdk_version_valid = check_jdk_version(root, ns, filename)
    is_aws_version_valid = check_aws_dependencies_has_correct_version(root, ns, filename)
    is_aws_v2_version_valid = check_aws_v2_dependencies_has_correct_version(root, ns, filename)
    is_gson_scope_provided = check_scope_is_provided(root, 'com.google.code.gson', 'gson', ns, filename)
    wf_custom_is_provided = check_scope_is_provided(root, 'com.reltio.workflow', 'workflow-custom', ns, filename)
    wf_api_is_provided = check_scope_is_provided(root, 'com.reltio.workflow', 'workflow-api', ns, filename)
    is_workflow_dependencies_has_scope_provided = wf_custom_is_provided and wf_api_is_provided

    pom_dir = os.path.dirname(filename)
    modules_invalid_count = check_module_poms(root, pom_dir, ns)
    if is_jdk_version_valid \
            and is_aws_version_valid \
            and is_aws_v2_version_valid \
            and is_gson_scope_provided \
            and is_workflow_dependencies_has_scope_provided \
            and modules_invalid_count == 0:
        return 0
    else:
        return 1


def main():
    return check_pom(sys.argv[1])


exit(main())
