# Feature status

This registry is the single source of feature delivery status. Each linked document owns the feature's
accepted scope, integration details and verification boundaries.

| Feature | Status | Reference |
| --- | --- | --- |
| Add movimiento | DONE | [Add movimiento](add-movimiento.md) |
| History | DONE | [History](history.md) |
| Search and filters | PLANNED | [Wave 4](../tasks/wave-4/README.md) |

## Status rules

- `PLANNED`: agreed scope awaiting implementation.
- `IN_PROGRESS`: implementation or acceptance is underway.
- `DONE`: the user accepted the delivered scope and it is integrated into `main`.

Update the existing row when a feature changes status; Git owns the change history. `DONE` does not
expand the accepted scope or imply that deferred verification has run. Those boundaries remain in the
feature document, with outstanding work tracked in [the backlog](../backlog.md).
