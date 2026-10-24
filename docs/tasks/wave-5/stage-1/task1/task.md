# Wave 5 / Stage 1 / Task 1 — ChangeStamp, ChangeClock and installation node

## Objective

Create the framework-free stamp model and the single process-wide clock that issues stamps, plus the
device-scoped installation ID used as the stamp node. Nothing here reads or writes SQL: the clock
receives its seed (the stored maximum) through a function supplied by task2.

## Context and skills

- Read `AGENTS.md`, [the wave README](../../README.md#change-stamps-and-tombstones-stage-1) and
  `docs/README.md` (external-API gate).
- Skills: `android-dev`, `kotlin-api-design`, `kotlin-concurrency-and-flow`, `kmp-boundaries`,
  `datastore`, `android-testing`, `test-driven-development`.
- Existing patterns to follow: `core/data/IdGenerator.kt` (fun interface + Koin binding in
  `di/TimeModule.kt`), `core/data/PreferencesDataStoreFactory.kt` (one DataStore per file, scope owned
  by the factory), `PreferencesPathProvider` with `AndroidPreferencesPathProvider`
  (`context.filesDir/tallybook.preferences_pb`) and `IosPreferencesPathProvider` (Application Support).

## Files

- Create: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/model/ChangeStamp.kt`
- Create: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/data/ChangeClock.kt`
- Create: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/data/InstallationIdRepository.kt`
  (+ `InstallationIdRepositoryImpl.kt`)
- Modify: `core/data/PreferencesPathProvider.kt`, `androidMain/.../AndroidPreferencesPathProvider.kt`,
  `iosMain/.../IosPreferencesPathProvider.kt` — resolve a file name inside the same directory, so one
  provider owns the directory per platform and the device file sits next to the existing one.
- Modify: `core/data/PreferencesDataStoreFactory.kt` (file name parameter), `di/PreferencesModule.kt`
  (second, qualified device DataStore), `di/TimeModule.kt` (`ChangeClock` single).
- Create: `androidApp/src/main/res/xml/data_extraction_rules.xml`; Modify:
  `androidApp/src/main/AndroidManifest.xml` (`android:dataExtractionRules`).
- Test: `shared/src/commonTest/kotlin/com/kurobello/tallybook/core/model/ChangeStampTest.kt`,
  `shared/src/commonTest/kotlin/com/kurobello/tallybook/core/data/ChangeClockTest.kt`,
  `shared/src/androidHostTest/kotlin/com/kurobello/tallybook/core/data/InstallationIdRepositoryTest.kt`,
  extend `androidHostTest/.../di/RepositoryModulesTest.kt` (or the preferences module test) for the graph.

## Contracts

```kotlin
@JvmInline value class ChangeStamp private constructor(val value: String) : Comparable<ChangeStamp> {
  companion object {
    val SEED: ChangeStamp // "0000000000000-0000-0000000000000000"
    fun of(epochMillis: Long, counter: Int, node: String): ChangeStamp
    fun parse(value: String): ChangeStamp // rejects anything off-format
  }
}

interface ChangeClock {
  /**
   * Reads the node once and advances to the stored maximum; idempotent, never moves backward.
   * Called at startup and again after any dataset replacement (future Drive restore).
   */
  suspend fun prepare(storedMax: suspend () -> DataOutcome<ChangeStamp?>): DataOutcome<Unit>
  /** Synchronous so repositories can call it inside a SQLDelight transaction. */
  fun next(): ChangeStamp
}

interface InstallationIdRepository {
  suspend fun nodeId(): DataOutcome<String> // 16 lowercase hex chars, created on first read
}

enum class PreferencesFile(val fileName: String) { APP, DEVICE }  // "tallybook[.device].preferences_pb"
interface PreferencesPathProvider { fun path(file: PreferencesFile): String }
fun interface BackupExclusion { fun exclude(file: PreferencesFile) } // throws okio.IOException
```

- `ChangeStamp` is only built through `of`/`parse`, so an off-format value is unrepresentable.
- `prepare` returns `DataOutcome` and takes a `DataOutcome` reader so a node or stored-max read
  failure reaches `LocalDataBootstrapper` as `DataError.Local` instead of an exception; on failure the
  clock stays unprepared. The task2 reader wraps `selectMaxChangeStamp` in `runLocalDataOperation`.
- `PreferencesDataStoreFactory.create(file)` memoizes one DataStore per `PreferencesFile` on the
  factory's single scope (`create()` keeps defaulting to `APP`); Koin binds the device store under
  `named(PreferencesFile.DEVICE)`.

`HybridChangeClock(clock: Clock, installationIds: InstallationIdRepository)` is the production
implementation. `next()` before `prepare()` is a programming error (`check`), not a `DataOutcome`.

## Exact rules

- Format, ordering, SEED, monotonicity and node scope: [README](../../README.md#change-stamps-and-tombstones-stage-1).
- Counter overflow (more than 0xFFFF stamps in one millisecond) advances the millis component by one
  and resets the counter; the encoded order must still be strictly increasing.
- `next()` must be safe under concurrent callers (repositories run on the IO dispatcher). Use a lock-free
  CAS loop on the last issued stamp; first confirm the status of `kotlin.concurrent.atomics` in Kotlin
  2.4.10 (experimental opt-in vs stable) per the external-API gate, and record the source link here.
  Do not hold a coroutine `Mutex` (not callable inside a transaction).
  Verified: on Kotlin 2.4.10 `AtomicLong`/`updateAndFetch` still require
  `@OptIn(ExperimentalAtomicApi::class)` (compiler error without it); the opt-in is confined to
  `HybridChangeClock`. The state is one packed `Long` (`millis shl 16 or counter`), so `last + 1`
  carries an overflow into millis. Source:
  <https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.concurrent.atomics/-atomic-long/>.
- Node ID: derived from `Uuid.random()` (64 bits as 16 hex), stored under one key in
  `tallybook.device.preferences_pb`. It is never placed in `AppPreferences` or the financial file.
- Android: exclude `tallybook.device.preferences_pb` from both `<cloud-backup>` and
  `<device-transfer>` with `domain="file"` (DataStore writes under `filesDir`). minSdk is 31, so
  `android:dataExtractionRules` is sufficient and `android:fullBackupContent` is not required.
  Source: <https://developer.android.com/identity/data/autobackup#include-exclude-android-12>.
- iOS: Application Support is included in iCloud/device backups; set `isExcludedFromBackup` on the
  device file after creation so a restored iPhone generates a new node. Source:
  <https://developer.apple.com/documentation/foundation/urlresourcevalues/isexcludedfrombackup>.
  Apple: "Some operations commonly made to user documents will cause this property to be reset";
  DataStore 1.2.1 writes `<file>.tmp` and `atomicMove`s it over the file (verified in
  `OkioStorageConnection`), so `InstallationIdRepositoryImpl` re-applies the exclusion on every
  successful read, and the Android rules also exclude the `.tmp` sibling.
  Compile-only evidence on iOS; runtime stays in the iOS backlog item.

## TDD implementation

- [x] Contract commit first (unblocks task2): `ChangeStamp`, the `ChangeClock` interface and a
      deterministic `TestChangeClock` fixture in `androidHostTest/.../core/data/` that is prepared on
      construction and issues increasing stamps from a fixed start.
- [x] RED: `ChangeStampTest` — `of` pads to 13 digits/4 hex, `parse` rejects wrong lengths/case/
      separators, text order equals (millis, counter, node) order, `SEED` is the minimum.
- [x] GREEN: implement `ChangeStamp`.
- [x] RED: `ChangeClockTest` with a controllable `Clock` and fake node: strictly increasing on equal
      millis, on backward wall clock, on counter overflow; `prepare` with a stored max ahead of the wall
      clock continues after it; a second `prepare` with a higher stored max advances, with a lower one
      changes nothing; the node is read once; `next()` before `prepare` fails.
- [x] GREEN: implement `HybridChangeClock`.
- [x] RED/GREEN: `InstallationIdRepositoryTest` on a real temp-file DataStore — first read creates,
      second read and a recreated repository return the same ID; the financial preferences file is not
      touched.
- [x] Wire Koin (qualified device DataStore, `ChangeClock` single) and prove the graph resolves with one
      DataStore instance per file.
- [x] Add the data extraction rules and manifest attribute; run `:androidApp:assembleDebug`.
- [x] Checks below.

## Checks

`./gradlew :shared:testAndroidHostTest --tests '*ChangeStamp*' --tests '*ChangeClock*' --tests '*InstallationId*'`
while iterating, then `./gradlew :shared:testAndroidHostTest`,
`./gradlew :shared:compileKotlinIosSimulatorArm64`, `./gradlew :androidApp:assembleDebug`,
`./gradlew detekt ktfmtCheck`. Backup exclusion cannot be proven by host tests; Stage 6 records it as a
manual check (`adb shell bmgr` is optional, not a gate).

## Blocks

Not blocked. Its contract commit unblocks task2; the rest runs in parallel with task2. Blocks task3.
