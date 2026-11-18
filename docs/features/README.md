# Feature status

This registry is the single source of feature delivery status. Each linked document owns the feature's
accepted scope, integration details and verification boundaries.

| Feature | Status | Reference |
| --- | --- | --- |
| Add movimiento | DONE | [Add movimiento](add-movimiento.md) |
| History | DONE | [History](history.md) |
| Search and filters | DONE | [Search and filters](search.md) |
| Manage movimientos and Undo | DONE | [Manage movimientos and Undo](manage-movimientos.md) |
| Profile and preferences | DONE | [Profile and preferences](profile.md) |
| Smart entry | DONE | [Add movimiento](add-movimiento.md) |
| Custom categories | DONE | [Custom categories](custom-categories.md) |
| Movimiento title and day-grouped Search | DONE | [Movimiento title](movimiento-title.md) |
| Single category and tags | DONE | [Tags](tags.md) |
| Sync-ready persistence (change stamps and tombstones) | DONE | [Accounts and sync-ready persistence](accounts.md) |
| Accounts, payment method and transfers | DONE | [Accounts, payment method and transfers](accounts.md) |
| Statistics | DONE | [Statistics](statistics.md) |
| Recurring movimientos | DONE | [Recurring movimientos](recurrentes.md) |
| Voice dictation and dictated recurrence | DONE | [Voice dictation](voice-dictation.md) |
| Several movimientos in one sentence | IN_PROGRESS | [Add movimiento](add-movimiento.md) |

## Status rules

- `PLANNED`: agreed scope awaiting implementation.
- `IN_PROGRESS`: implementation or acceptance is underway.
- `DONE`: the user accepted the delivered scope and it is integrated into `main`.

Update the existing row when a feature changes status; Git owns the change history. `DONE` does not
expand the accepted scope or imply that deferred verification has run. Those boundaries remain in the
feature document, with outstanding work tracked in [the backlog](../backlog.md).
