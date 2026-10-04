---
name: reltio-workflow-repo-contribution (workspace override)
description: This repository's own demo CI, layered on top of the default skill. Does not change what is true for any real tenant.
---

# Workspace override — demo GitHub Actions pipeline

This file overrides `workflow-repo-contribution/SKILL.md` **for this repository only**
(`itsparthgupta/reltio-custom-workflows-test-template`, a personal test/demo mirror —
not a Reltio-provisioned tenant repository). Per `workflow-merge`'s rule, this workspace
copy wins for this playbook in this repo, and the divergence from the default must be
called out whenever it's quoted.

## What's different here vs. the default skill

The default skill says, correctly, for real tenant repositories:

> "On approval, a Reltio pipeline compiles the Java into a JAR and publishes it to a
> dedicated S3 bucket. Nobody does this by hand, and you do not upload to S3 yourself."

That remains true everywhere except this one personal repo. Here, there is **no**
Bitbucket-hosted repo, no Vault-backed webhook, no real Reltio review gate — so a
`.github/workflows/build-and-publish.yml` was added that *simulates* the shape of the
real pipeline (`bitbucket-pipelines.yml` in this same repo) purely so the local-vs-pipeline
round trip can be exercised without a real tenant:

- **On any PR against `main`**: runs `pipeline/WorkflowJarProjectValidator.py` and
  `mvn -B verify` — the same two local checks `workflow-jar-customization`'s gate section
  now documents. No S3 write happens on PR.
- **On merge to `main`**: re-runs the build, then runs `pipeline/collectJars.sh` to collect
  the shaded jars (stripping the `-everything` suffix, exactly like the real pipeline's
  `deploy-to-s3` step), and uploads them to the bucket/prefix read out of
  `repository.properties` (`s3://reltio.workflow-custom-jars/Reltio/`).

## Credentials — read this before assuming the pipeline "just works"

- AWS auth uses **temporary STS session credentials** stored as GitHub Actions repo
  secrets (`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN`) — not OIDC,
  and not long-lived IAM keys. These expire roughly hourly. **The merge-to-main job will
  fail with an AWS auth error once they expire** — that is expected, not a bug in the
  workflow. Re-run `gh secret set AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY / AWS_SESSION_TOKEN`
  (piping each value in via stdin or a temp file, never as a literal CLI argument) with a
  freshly minted STS session before the next demo.
- The repo is private (`isPrivate: true`), which is required for this setup — repo secrets
  are already hidden from logs and non-collaborators regardless, but keeping the repo
  private is still the right default for anything holding even temporary AWS credentials.
- This pipeline, its secrets, and this override file have no bearing on any real tenant's
  provisioned repository. Do not generalize "the pipeline is simple to set up" from this
  file to a real customer engagement — say explicitly, if asked, that production jar
  publishing remains Reltio-managed and out of customer control.

## Outputs

- A working GitHub Actions pipeline on this repo only, verifiable via `gh run list` /
  `gh run view` after a push to `main`.
- The next step, named: when the current STS session expires, this job will start failing
  on the `Configure AWS credentials` step until secrets are refreshed — not a code problem.
