import os
import subprocess
import tempfile
import unittest
from pathlib import Path


PIPELINE_DIRECTORY = Path(__file__).resolve().parents[1] / "pipeline"
SCRIPT = PIPELINE_DIRECTORY / "ensureS3Prefix.sh"

FAKE_AWS = r"""#!/usr/bin/env python3
import os
import sys

log_path = os.environ["AWS_MOCK_LOG"]
with open(log_path, "a", encoding="utf-8") as log_file:
    log_file.write(" ".join(sys.argv[1:]) + "\n")

command = sys.argv[1:]
if command[:2] == ["s3api", "list-objects-v2"]:
    status = os.environ.get("AWS_MOCK_LIST", "present")
    if status == "present":
        print("1")
        sys.exit(0)
    if status == "missing":
        print("0")
        sys.exit(0)
    if status == "error":
        print(
            "An error occurred (NoSuchBucket) when calling the ListObjectsV2 operation",
            file=sys.stderr,
        )
        sys.exit(255)
    print(status, file=sys.stderr)
    sys.exit(255)

if command[:2] == ["s3api", "put-object"]:
    status = os.environ.get("AWS_MOCK_PUT", "ok")
    if status == "ok":
        print('{"ETag": "\\"abc\\""}')
        sys.exit(0)
    print(status, file=sys.stderr)
    sys.exit(255)

print("unexpected aws invocation: " + " ".join(command), file=sys.stderr)
sys.exit(2)
"""


class EnsureS3PrefixTest(unittest.TestCase):
    def run_script(self, location, region, listing="present", put="ok"):
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            fake_bin = root / "bin"
            fake_bin.mkdir()
            fake_aws = fake_bin / "aws"
            fake_aws.write_text(FAKE_AWS, encoding="utf-8")
            fake_aws.chmod(0o755)
            log_path = root / "aws.log"
            environment = dict(os.environ)
            environment["PATH"] = f"{fake_bin}{os.pathsep}{environment.get('PATH', '')}"
            environment["AWS_MOCK_LOG"] = str(log_path)
            environment["AWS_MOCK_LIST"] = listing
            environment["AWS_MOCK_PUT"] = put
            completed = subprocess.run(
                ["bash", str(SCRIPT), location, region],
                cwd=root,
                env=environment,
                check=False,
                capture_output=True,
                text=True,
            )
            log = log_path.read_text(encoding="utf-8") if log_path.exists() else ""
            return completed, log

    def test_skips_create_when_folder_has_objects(self):
        completed, log = self.run_script("reltio.workflow-custom-jars/Reltio", "us-east-1")

        self.assertEqual(0, completed.returncode, completed.stderr)
        self.assertIn("already exists", completed.stdout)
        self.assertIn("--prefix Reltio/", log)
        self.assertIn("--bucket reltio.workflow-custom-jars", log)
        self.assertNotIn("put-object", log)

    def test_creates_missing_folder_from_repository_properties(self):
        completed, log = self.run_script(
            "s3://reltio.workflow-custom-jars/Reltio/",
            "us-east-1",
            listing="missing",
        )

        self.assertEqual(0, completed.returncode, completed.stderr)
        self.assertIn("creating it", completed.stdout)
        self.assertIn(
            "s3api put-object --bucket reltio.workflow-custom-jars --key Reltio/ --region us-east-1",
            log,
        )

    def test_creates_nested_prefix(self):
        completed, log = self.run_script(
            "customer-jars/Acme/workflows",
            "eu-west-1",
            listing="missing",
        )

        self.assertEqual(0, completed.returncode, completed.stderr)
        self.assertIn("--key Acme/workflows/", log)
        self.assertIn("--region eu-west-1", log)

    def test_skips_when_aws_bucket_has_no_folder_prefix(self):
        completed, log = self.run_script("reltio.workflow-custom-jars", "us-east-1")

        self.assertEqual(0, completed.returncode, completed.stderr)
        self.assertIn("No folder prefix", completed.stdout)
        self.assertEqual("", log)

    def test_does_not_create_when_list_fails(self):
        completed, log = self.run_script(
            "reltio.workflow-custom-jars/Reltio",
            "us-east-1",
            listing="error",
        )

        self.assertNotEqual(0, completed.returncode)
        self.assertIn("cannot list S3 folder", completed.stdout)
        self.assertNotIn("put-object", log)

    def test_put_object_failure_is_an_error(self):
        completed, _ = self.run_script(
            "reltio.workflow-custom-jars/Reltio",
            "us-east-1",
            listing="missing",
            put="AccessDenied",
        )

        self.assertNotEqual(0, completed.returncode)
        self.assertIn("failed to create S3 folder", completed.stdout)

    def test_requires_location_and_region(self):
        completed, _ = self.run_script("", "us-east-1")
        self.assertNotEqual(0, completed.returncode)
        self.assertIn("S3 location is required", completed.stdout)

        completed, _ = self.run_script("reltio.workflow-custom-jars/Reltio", "")
        self.assertNotEqual(0, completed.returncode)
        self.assertIn("AWS region is required", completed.stdout)


if __name__ == "__main__":
    unittest.main()
