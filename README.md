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
* **location-dcr-approval** - `CaptureRequestor` service task that records a Location DCR's
  `createdBy` as the `requestor` process variable, so rejection paths can route a follow-up
  task back to whoever submitted the change

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
* **`bpmn/locationDcrApproval.bpmn20.xml`** — process id `locationDcrApproval`, a two-step
  Location DCR approval:
  1. `ROLE_API` reviews first (Approve / Reject with mandatory comment). Reject routes a
     task straight back to the original requestor with the reason.
  2. `ROLE_ADMIN` reviews second, with three options: Approve (applies the DCR via
     `ChangeRequest3rdPartyApply`), Reject (mandatory comment, routes back to the
     requestor via `ChangeRequest3rdPartyReject`), or Send Back to API (mandatory
     comment, loops to step 1 again).
  Calls `CaptureRequestor` from `location-dcr-approval` and reuses
  `ChangeRequest3rdPartyApply` / `ChangeRequest3rdPartyReject` from `workflow-external-dcr`.
  After importing, set this process definition's `objectTypes` to
  `configuration/entityTypes/Location` in the Workflow Modeler properties panel (not
  expressible in raw BPMN) so it only triggers for Location DCRs.

Edit either file's content and run **Refresh Sync Status** in the IDE to flip
its badge from `clean` to `changed`.