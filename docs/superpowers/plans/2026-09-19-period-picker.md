# Period Picker Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the reusable period picker select real, non-future day, week, and month ranges.

**Architecture:** Keep calendar range construction as pure commonMain functions and keep Compose
components stateless. The catalog owns sample selection state, captures the device date once and
injects it into the pure typed ranges, then maps them to localized labels and header copy.

**Tech Stack:** Kotlin Multiplatform, kotlinx-datetime 0.8.0, Compose Multiplatform 1.11.1.

**Spec:** `docs/tasks/period-picker.md`

## Global Constraints

- Production UI and calendar logic live in `shared/src/commonMain`.
- Period values are typed; no string date parsing is used for selection state.
- Tests are deterministic and never read the wall clock.
- The selector has no balance or movement-data dependency.

---

### Task 1: Typed calendar ranges

**Files:**
- Create: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/component/TallybookPeriodSelection.kt`
- Test: `shared/src/commonTest/kotlin/com/kurobello/tallybook/core/ui/component/TallybookPeriodSelectionTest.kt`

**Interfaces:**
- Produces `TallybookPeriod`, `TallybookPeriodScope`, and functions that build selectable day,
  week, and month periods from a focused month and injected `today`.

- [x] **Step 1: Write failing common tests** for a current month ending at `today`, a complete
  historical month, and a past-year month preserved when its year changes.
- [x] **Step 2: Run `:shared:testAndroidHostTest`** and confirm the missing symbols fail.
- [x] **Step 3: Implement immutable typed ranges and pure builders** with no Compose or clock
  dependency.
- [x] **Step 4: Run `:shared:testAndroidHostTest`** and confirm the range tests pass.

### Task 2: Header and catalog interaction

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/component/TallybookPeriodPicker.kt`
- Modify: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/catalog/UiCatalogControls.kt`
- Modify: `shared/src/androidDeviceTest/kotlin/com/kurobello/tallybook/core/ui/component/TallybookPeriodPickerTest.kt`

**Interfaces:**
- Consumes the typed ranges from Task 1.
- Produces an optional title/subtitle header placed before `TallybookYearMenu`.

- [x] **Step 1: Write failing UI tests** that select a month, then verify Day and Week render its
  bounded options and the header changes with scope.
- [x] **Step 2: Run `:shared:compileAndroidDeviceTest`** and confirm the required picker API is
  absent.
- [x] **Step 3: Implement the stateless header and wire catalog state** through typed callbacks.
- [x] **Step 4: Run `:shared:compileAndroidDeviceTest`** and confirm the UI tests compile.

### Task 3: Verification and device review

**Files:**
- Modify: `docs/ui/components.md`

- [x] **Step 1: Document the selector ownership and year menu behavior.**
- [x] **Step 2: Run `ktfmtFormat`, `ktfmtCheck`, `detekt`, `:shared:testAndroidHostTest`, and
  `:androidApp:assembleDebug`.**
- [x] **Step 3: Install the APK on the Xiaomi and verify current-period clipping, a historical
  full month, the four-row year menu, and scope/title changes.**
