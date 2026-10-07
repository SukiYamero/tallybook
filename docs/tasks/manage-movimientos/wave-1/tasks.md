# Wave 1 — independent producers

Read the [planning contract](../README.md) before dispatch. Confirm the component and mutation
interfaces together; then assign independent source ownership.

- [Task 1](task1/task.md): reusable snackbar, catalog fixture and component tests. Its commit is
  independently reviewable without movimiento wiring.
- [Task 2](task2/task.md): repository mutations, SQL and domain/observation tests.

Workers may execute in parallel. The orchestrator serializes Gradle runs and Git index/commits.
Wave 2 starts only after both producer contracts compile and their focused checks are verified.
An unavailable target check remains an explicit gate, not a passing result.
