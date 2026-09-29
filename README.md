# README #

The repository with custom code and automatic pipelines for Reltio

This copy is a **test fixture** derived from the Reltio-provisioned Custom Workflow
Repository template, used to exercise Reltio IDE's Custom Workflow Repository
feature (connect, BPMN discovery, sync badge, deploy-from-repo) end to end
against a real GitHub clone. It is not a real customer repository.

### Modules ###

* **delete-review-with-validation** - module with example of deleting review with validation
* **parallel-approve-delete** - module with example of parallel approve/delete process
* **workflow-external-dcr** - module with example of custom DCR apply/reject actions

### BPMN (`bpmn/`) ###

Added for testing only — the original template ships no BPMN files, since
customers add their own once they start authoring. Two samples are included
here, each paired with an existing Java module above so a deploy exercises a
process that actually references real custom Java classes:

* **`bpmn/deleteReviewWithValidation.bpmn20.xml`** — process id
  `deleteReviewWithValidation`, calls `DeleteReviewValidator` from
  `delete-review-with-validation`.
* **`bpmn/parallelApproveDelete.bpmn20.xml`** — process id
  `parallelApproveDelete`, calls `DeleteEntity` from `parallel-approve-delete`.

Edit either file's content and run **Refresh Sync Status** in the IDE to flip
its badge from `clean` to `changed`.