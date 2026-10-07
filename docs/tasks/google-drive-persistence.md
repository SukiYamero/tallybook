# Google access and Drive persistence — planning draft

Status: planning; confirmed product rules are recorded in the canonical business document. Remaining
questions and native/cloud integration gates are listed below. No application implementation or Cloud
configuration has been performed. Current committed planning baseline: `990edb8`. Review date:
2026-10-07.

## Objective and authority

Deliver guest/local access, Google authentication, explicit Drive consent, local-first saving,
recoverable hidden backups, and primary-device transfer on Android first, with Google Play preparation
after physical-device acceptance.
iOS runtime delivery is explicitly deferred; shared business logic remains in commonMain. The
accepted MVP requirement and the boundary with the future sync
engine live in [Business logic](../business-logic.md#alcance-del-mvp). Stack choices live in
[Stack](../stack.md); workflow, external-API evidence and acceptance gates live in
[Repository workflow](../README.md). This document specifies proposed implementation and verification
for the accepted scope; it does not mark the feature implemented or delivered.

Observable acceptance: sign in, grant Drive access, create/edit/delete a movimiento, observe local
commit and a verified Drive backup, restart, and restore the same data on a fresh Android installation
or another authorized physical Android device.
Deleting and Undo must be represented by the eventual backup of the current committed dataset.

## Repository evidence and consequences

| Observation | Evidence | Consequence and task owner |
| --- | --- | --- |
| Real local persistence and mutation flows already exist | `shared/src/commonMain/sqldelight/com/kurobello/tallybook/core/database/{Movimiento,Categoria}.sq`; `core/data/MovimientoRepositoryImpl.kt` | Extend their transaction boundary; do not rebuild movimiento management. Data owner, stage 2. |
| One database, without account ownership | `core/database/DatabaseDriverFactory.kt`: `tallybook.db`; tables have no account discriminator | Isolate a guest dataset and one retained Google dataset, each with an owner and lifecycle; replace singleton active repository/driver binding safely. Data/session owners, stages 2 and 3. |
| Currency preference lives in a separate store | `core/data/AppPreferencesRepositoryImpl.kt`; `core/model/AppPreferences.kt` | Restore needs a recoverable SQLDelight/DataStore handoff; a SQL transaction alone cannot restore both. Data owner, stage 2. |
| Startup only bootstraps preferences and categories | `AppStartupViewModel.kt`; `core/data/LocalDataBootstrapper.kt`; `App.kt` | Resolve interrupted recovery, account state and initial backup selection before the normal shell opens. Session/UI owners, stages 2 and 3. |
| Google/network dependencies are absent | `gradle/libs.versions.toml`; `shared/build.gradle.kts`; `androidApp/build.gradle.kts` | Add only the chosen authentication packages, Ktor engines and JSON integration needed here. Platform/network owners, stages 1 and 2. |
| Android manifest has no Internet permission and enables automatic backup | `androidApp/src/main/AndroidManifest.xml` | Add network permission and explicit backup rules; copied session/device metadata must not impersonate an installation. Android owner, stages 1 and 2. |
| iOS has no Google URL callback/configuration | `iosApp/iosApp/Info.plist`; `iOSApp.swift`; `ContentView.swift` | Integrate SDK, callback routing, presenter lifecycle and restoration in the deferred iOS increment. |
| Physical iOS runtime and Android test-runner gaps are documented | [Backlog](../backlog.md) | New target checks cannot be inferred from old compilation. Use bounded diagnosis and manual physical checks where appropriate; record unexecuted automated checks. Verification owner, stage 4. |

Declared versions were read from the catalog: Kotlin 2.4.10, CMP 1.11.1, AGP 9.0.1, SQLDelight 2.3.2,
Koin 4.2.2, coroutines 1.9.0 and serialization 1.7.3. Android minSdk is 31 and compile/targetSdk is 36;
the Xcode project
declares iOS 18.2 as deployment target. These are declared values, not a new resolved-dependency report.
Google and Ktor versions must be selected and resolved in stage 1; the SPM bridge is an iOS-follow-up
gate. Compatibility is not
established by documentation alone. No unrelated dependency upgrades are authorized by this plan.

## Confirmed scope and remaining decisions

Product behavior is owned by [Business logic](../business-logic.md#alcance-del-mvp): one primary device
with explicit transfer, hidden backups, guest mode, whole-copy choice/replacement, offline durability,
optional local access after logout, no multiple-profile UI, two normal backups plus a protected
pre-restore copy, and confirmed local/cloud erasure with the appropriate authentication.

Android physical acceptance and Play preparation come first; iOS can wait. `kurobello.com` is available.
A Moneta Cloud project and Cloudflare setup exist, but their live branding/permissions/URLs are not
audited. Reuse infrastructure only after checking current ownership and isolation.

Confirmed boundaries: guest and accessible Google-owned data support local editing offline. Google
publication, not local editing, requires login, connectivity and current primary authority. The
optional signed-out local access permits both reads and edits, without token restoration/upload. Retry on
foreground connectivity recovery and reopen is sufficient; background scheduling is excluded. Erasure
targets Tallybook only. Moneta is a proof-of-concept source reference, not a deployed product or a
dataset migration/deletion target.

Remaining account information: confirm Play account type/creation date and eligibility before
forecasting the release calendar.

Integration/default gates: prove primary-control and deletion-generation behavior against actual
Drive APIs; confirm a representative payload cap (initial proposal 5 MiB); default the logout-local
access option to disabled; define the verified external deletion pathway before Play readiness.
These are not claims of already working integration.

Multiple profiles/accounts and the developer-owned metrics/backend system remain deferred. Include
the minimal account settings needed for the access option and backup operations, not a full profile
feature. Include redacted local diagnostics without remote telemetry. Future multi-profile support
must be possible through explicit dataset identity and scoped dependencies; do not build its registry
or switcher now. Public release remains gated on the rest of the MVP.

## Moneta configuration references

Read-only reference repository: `/Users/sukiyamero/Desktop/programacion/web/moneta`.

- Its `README.md` documents Cloudflare Tunnel serving `dev.kurobello.com` and a web OAuth client with
  that origin. This is a browser development setup; native Android needs package/certificate clients
  rather than a tunnel or JavaScript-origin callback.
- Its `specs.md` backlog records unpublished privacy/terms pages, domain verification and consent
  branding as pending. That document is evidence of a pending task, not proof of today's Console state.
- Its `docs/pendientes-usuario.md` describes both a visible `KuroBello` Drive folder and hidden app
  configuration. Tallybook backup discovery must namespace/filter its own files and schema if projects
  are reused. Never interpret or prune Moneta files, and do not treat them as Tallybook recovery data.
- Migrating Moneta financial data or reusing its web sync protocol is outside this delivery. Reusing
  infrastructure is subject to the Cloud audit; runtime continuity is not established by similar names.

### Moneta sync source findings

Source inspection, not newly executed tests: `src/lib/sync/opLog.ts` (`replayEntity`, lines 130–170)
chooses whole-entity versions by HLC; concurrent put/put selects one version, and some edit/delete
conflicts revive the edit. `src/lib/hlc.ts`, `outbox.ts` and `sync/engine.ts` implement persistent
operation logs, pending-local inclusion, per-device files, replay and shard compaction. This is the
advanced sync engine, not an implementation of primary-device snapshot transfer.

`src/lib/profiles` isolates local and Google data in separate databases. `authStore.ts` uses an auth
generation to reject stale login results; `syncSession.ts/getSyncContext` checks account binding.
Adopt these contracts in common code, not its framework/storage implementation. Do not port its
email fallback for account identity, device-global tip cache, additive guest adoption or the incomplete
revalidation around an awaited token refresh.

`sync/erase.ts/eraseProfileData` stops future triggers and clears this device's outbox, but
`sync/engine.ts` does not cancel an already in-flight push when triggers stop. The source has no
cross-device erasure generation; `engine.test.ts:625` explicitly preserves in-flight completion.
Source tests cover HLC ordering, replay, pending writes and some account isolation, but were not run
for this analysis. They do not establish stale/offline-device erasure safety for Tallybook.

Recommended Console sequence: inventory existing project/domain and permissions; choose isolated
development versus production projects; enable Drive v3; configure Branding/Audience/Data Access;
register native Android clients and the required web client ID; add test users; prove local-device
access; configure verified public production URLs and Play signing clients; complete production
readiness and internal-track checks. Cloudflare can host stable public pages; its development tunnel
is not a production policy/support/deletion URL. Do not delete or reset the Moneta project/data.

## Approaches and recommendation

| Approach | Trade-off |
| --- | --- |
| Versioned complete snapshots in appDataFolder — recommended | Small shared implementation; enables reinstall/device recovery; preserves earlier successful backups; does not merge independent edits. |
| One mutable cloud file | Fewer files, but uncertain write outcomes and competing devices can destroy the only current backup; safe replacement still needs history/concurrency protection. |
| Full incremental multi-device sync | Supports concurrent editors, but needs conflict policy, durable entity operations/deletions and retry ordering. It is the separately scoped sync engine. |

Use immutable snapshot files rather than a mutable shared head. Retain small durable local metadata
for pending backup and last verified local revision; this is not an entity outbox. No HLC, shard
read-modify-write, background scheduler or automatic conflict merge in this delivery.

## Product contracts

### Sign-in, permission and startup

- Fresh installations offer Continue with Google and Continue as guest. Guest opens the local shell
  without network, credentials or cloud provisioning. Existing unowned local data belongs to that
  guest context until explicitly associated. Development catalog access follows existing rules.
- Guest -> Google authentication -> Drive authorization -> copy selection/confirmation -> Google
  dataset activation is one recoverable association flow. Cancel/denial preserves the guest data and
  permits guest use; identity alone does not authorize upload or relabel the guest database.
- Signed-in users can read and mutate retained Google data offline. Drive token expiry/revocation
  preserves local editing and shows pending/reconnect state for cloud work. Local mutation permission
  depends on dataset access/ownership, not current network, native token or primary publication role.
- Explicit sign-out follows the local-access setting: enabled permits retained-account local reads;
  disabled routes to welcome/authentication or separate guest data. Neither path silently reacquires
  Google credentials, keeps backup running, or treats retained Google data as guest-owned.
- Authentication and Drive authorization are separate operations. Check the actual granted scopes;
  an authenticated account is not evidence of permission to save. Android uses Credential Manager and
  AuthorizationClient; iOS uses the native Google SDK, following [the existing stack](../stack.md).
  [Android authorization contract](https://developer.android.com/identity/authorization).
- Bind ownership to a stable Google subject, never the email address. Resolve that identity through
  the native SDK/Google authenticated identity endpoint and prove that the token authorizing Drive
  belongs to the selected account. Android stage 1 proves that binding; cross-platform subject
  equivalence is required in the deferred iOS increment.
  An ID token is not a Drive bearer token.
- SDK/platform storage owns credential persistence. Tokens must not enter DataStore, SQL tables,
  backups, saved UI state or logs. Client IDs are configuration; client secrets and service-account
  credentials do not belong in a distributed mobile app. No Firebase/backend is required by this scope.
- Native presentation belongs to the live Activity/view controller. Duplicate launches, rotation,
  Back/cancel, app backgrounding and late results must not leak presenters or complete a newer session.
  Token refresh is serialized and bounded; interactive consent occurs only from a foreground action.

### Account ownership and account changes

- Use two bounded local storage contexts: guest and one retained Google account. Proposed separate
  SQLDelight databases and currency-preference stores give each its own owner without adding account
  predicates to every movimiento query. Preserve the existing unowned database as guest on migration;
  do not assume its singleton DI bindings already support this lifecycle.
- One DatasetSession owner binds the active repositories, driver, preferences and jobs. Identify
  datasets by a typed ID and owner (Guest or Google subject), not email or an unvalidated path. This
  boundary permits future multiple profiles without implementing a registry/account switcher now.
  Metadata is device-scoped only when genuinely shared; data revisions/backup state are dataset-scoped.
- Scope changes stop/dispose old observers and feedback/Undo owners before rebinding. Late auth,
  network and mutation results revalidate the dataset ID, owner and epoch after each suspension;
  they cannot update the new scope. Close each driver and DataStore scope; never open two DataStore
  instances for the same path. Only the active dataset is exposed to the shell.
- Association previews local versus cloud data and explicitly selects one whole copy. No ID-union or
  additive Moneta-style guest adoption. Use a durable migration/association journal across context
  stores: protect the source, replace/verify the destination, finish currency handoff, then reset the
  discarded guest source only after the confirmed target is usable. Reopen resumes unfinished steps;
  it never deletes source data before a verified target exists or exposes duplicate half-adoption.
- Sign-out invalidates cloud/session jobs and native results, clears credential state, closes drafts
  and Undo queues, and retains Google data plus any pending revision. The device setting, proposed
  label "Allow local access after signing out", determines access to that retained dataset. Default
  disabled; changing it is a device access setting, not a backed-up financial preference.
- Persist explicit signed-out state separately from credential/cache presence. Startup must honor
  it before any SDK restore/token request; an old native credential cannot silently authenticate,
  restart backup or bypass a disabled local-access setting. Only a deliberate Google sign-in clears
  the explicit signed-out state; enabled local access permits editing without clearing it.
- With the setting enabled, signed-out Google data remains readable/editable and local revisions
  accumulate under its original owner. With it disabled, even read access requires authentication.
  Entering guest mode preserves that locked
  Google context and opens a separate guest dataset. Disabling local access while signed out immediately
  closes the Google shell. Back/session restoration cannot bypass the setting.
- Signing into B while the retained dataset belongs to A does not show A's data or upload it into B.
  Offer reconnecting A, or an explicit local replacement flow. Replacement is gated until A's data is
  protected by a verified backup; offline or pending A data blocks replacement. No arbitrary transfer
  from A to B and no multiple-Google-account registry in this delivery. Preserve guest data separately.
- Sign-out, revoke Google access, erase local data and delete Drive backups are distinct actions.
  Account switching uses the replacement contract, not cloud deletion. Include a clearly confirmed
  deletion/disconnection path for Play readiness: cancel backup work first, delete only Tallybook-owned
  cloud files while permission remains, then erase local owned data and revoke credentials. Partial
  remote deletion is retryable and must not report full completion. Prevent this installation's pending
  work from re-uploading erased data, including on relaunch. Settle other-device invalidation in the
  deletion integration gate; do not claim to erase an offline device's local cache. Never delete the
  user's Google account or Moneta data.
- Guest erasure requires only confirmation and can complete offline, clearing guest records,
  preferences, recovery staging, guest-owned permanent protected artifacts and pending work without
  touching retained Google data or its protected artifacts.
  Google-associated erasure requires fresh authentication as its owner, authorized Drive access and
  connectivity; a local-access checkbox is not sufficient authority. Confirmation names local data,
  all linked-owner local protected artifacts, both regular cloud copies and the protected recovery
  copy. Retained guest data stays outside that
  account-erasure action unless a separate explicit guest deletion is requested.
- Provide a public deletion/help entry usable after uninstall. Resolve a demonstrably operable method
  during stage 1: a static Google-authorized web deletion flow in the same production project is an
  option, with separate explicit consent and ownership checks. Merely listing a support email is not
  evidence that the developer can delete a user's hidden Drive files. Document which data the developer
  can access/delete and which Google-managed removal steps the user controls. A blocked external
  deletion pathway is a release gate, not a reason to add the deferred metrics backend.
- The deletion protocol must fence the dataset generation before removing snapshots, reconcile every
  known staged/upload ID (including unknown accepted-upload outcomes), persist unfinished erasure,
  and verify its sweep before reporting completion. Cancellation alone does not retract a Google
  request. An uploader that finishes against an obsolete/deleting generation cannot acknowledge
  success and must reconcile/remove its obsolete file. Failed cleanup stays visibly pending.
- Proposed cross-installation fence: a minimal app-owned control record for dataset generation and
  erasure state, separate from financial snapshots. Old installations check it before upload and
  after an upload outcome; an erased/stale generation quarantines old local data and requires local
  erasure before a new dataset can be enrolled. External deletion applies the same protocol. Retain
  only minimal technical erasure metadata if the documented deletion/retention policy permits it;
  otherwise choose another demonstrable invalidation mechanism. This is a stage-1 design/integration
  gate, not a proven Drive transactional primitive or an exception to data-deletion obligations.
- Do not claim globally completed erasure while an upload with unknown outcome can still materialize.
  Stage 1 must prove how in-flight writers are quiesced/reconciled and what happens if a stale writer
  never returns. If the selected APIs cannot demonstrate that contract within the proposed allowance,
  re-plan the deletion flow/effort before declaring Play readiness. Erasing remote data does not
  remotely erase an offline installation; that limitation and its next-resume behavior must be clear.

### Primary-device ownership and transfer

- Bind Google publication to dataset identity/generation, primary installation ID and a separate
  publication epoch. Restore never copies installation identity. Ordinary primary transfer advances
  publication authority without marking the dataset erased or invalidating recoverable local edits.
- Guest and accessible linked mutations (create/edit/delete/Undo and backed-up preferences) commit
  locally and mark the proper dataset dirty without a Google request. A secondary can edit its own
  retained local copy, but cannot publish until explicitly promoted. Access-toggle, context ownership,
  recovery and erasure gates still apply; this is not permission to expose another account's data.
- Before cloud work, require authenticated matching subject, granted Drive scope, connectivity and
  fresh primary/generation/accepted-head validation. Failure or another primary pauses publication,
  not local editing. Never automatically reclaim primary ownership merely because this device has
  pending data. Offer Become primary as an explicit foreground action.
- Serialize local snapshot capture/replacement, upload acknowledgements, transfer and erasure within
  the installation. Local edits may continue while a captured revision uploads. A changed publication
  epoch prevents stale acknowledgement or automatic publication; keep newer local revisions pending.
  A fresh control read is not an atomic distributed lock.
- Takeover requires online owner authentication/Drive permission, explicit confirmation and a proved
  cloud authority transition. Preserve local data, pending revision and last acknowledged baseline
  before and after claiming the role. Persist a transfer journal; until finalized, cloud publication
  is blocked. Transfer alone does not replace local rows/preferences or relabel pending data as
  backed up. Source becomes secondary for publication on its next fresh check but retains local edits.
- Once primary authority is established, fetch/validate current cloud candidates and run the whole-copy
  divergence check below before publishing. Downloading a candidate for preview does not apply it.
  A second device may explicitly restore first when it has no local work, or keep its existing local
  work and resolve after promotion; no implicit restore is attached to the Become primary action.
- Publication role changes do not rebind the local dataset or discard its draft/Undo owner. Context
  disposal is needed for actual data replacement or access/account changes, not merely a new primary
  publication epoch. A finalized takeover with unresolved copy choice persists Backup paused until
  resolved; the automatic coordinator must preserve that state across restart.
- Stage 1 must establish safe competing-transfer and transfer-versus-upload behavior using the actual
  Drive control protocol. Do not assume File.version, an ETag or a read-then-PATCH sequence provides
  compare-and-swap. The inspected v3 update reference describes patching, not the needed cross-device
  atomicity guarantee. If a safe primitive/serialized transfer protocol cannot be demonstrated, stop
  dependent primary enforcement and re-plan; do not label last-writer-wins control as exclusive.
  [Drive update contract](https://developers.google.com/workspace/drive/api/reference/rest/v3/files/update).
- Reauthentication of the same account rechecks principal/generation before retrying pending backup.
  A revoked role, newer accepted head or erased generation blocks automatic upload. Whole-copy choice
  resolves an actual divergence; it is not a promise that no future stale operation can arrive.

### Whole-copy conflict resolution before publication

- Persist the exact last acknowledged cloud head and corresponding local revision/content baseline
  on verified upload or explicit restore. Do not compare revision counters from different devices or
  use wall-clock timestamps as conflict authority. Preserve baseline identity when old files are pruned.
  A successfully confirmed absence of any initial backup is an explicit Empty baseline; failed
  discovery or lost metadata is Unknown, not Empty.
- If local data is dirty and the accepted cloud head still represents that baseline, eligible primary
  backup proceeds without a conflict prompt. If both local and remote financial content changed,
  pause publication and present This device or Drive copy with source/count/date/account context.
  Equivalent verified financial content does not require a destructive choice just because envelope
  metadata differs. Missing/uncertain baselines require explicit copy review before overwriting.
- If no local change is pending, do not upload stale cached data. Offer the current verified cloud
  copy for explicit refresh/restoration using the existing confirmation contract. No silent import
  over a changed local dataset. An erased dataset generation follows erasure quarantine, not a normal
  conflict prompt that could resurrect deleted data.
- Takeover completes before this publication resolution, but cancellation leaves the newly acquired
  primary role with backup paused and all candidates unchanged. Becoming primary is not confirmation
  of either data copy. The chosen whole-copy operation must protect the losing nonempty copy, verify
  source owner/content, then publish local selection or restore cloud selection without additive merge.
- Tie the choice to captured local revision and remote head/publication epoch. Recheck before apply/
  publication; new local edits, changed remote content or lost authority require refreshed preview or
  retry, never application of a stale choice. A short maintenance gate prevents local mutation during
  the actual replacement, not throughout network work or the whole modal lifetime.
- Retrying after interruption reopens the durable operation journal. Upload failure retains local
  data and pending state; success acknowledges only the selected captured revision. The automatic
  coordinator cannot click a conflict choice or assume a takeover chose the local data.

### Local-first backup

- Every successful insert, update, delete, Undo restoration and affected persisted preference change
  marks the dataset dirty. Real SQL data mutations increment a persistent local revision in the same
  transaction. Guest dirty state is local-only until association; linked dirty state belongs to its
  Google owner and generation. No-op/missing-ID outcomes do not create a fake data change.
- Currency changes use the same durable preference handoff as restoration: journal the intended
  owner/epoch/value in SQL first, apply through the existing DataStore owner, then finalize and advance
  the SQL revision. A pending journal blocks snapshot acknowledgement/capture and normal dataset
  entry until reconciled on retry/startup. The SQL value is a pending command, not a second readable
  preference source. A crash between the two stores cannot leave changed currency marked backed up.
- A single application-owned backup coordinator coalesces Google changes for approximately five
  seconds after the latest persisted mutation while foregrounded. Reattempt pending work on a
  platform connectivity-available signal, launch/resume and explicit Retry/Back up now. Native
  connectivity callbacks are hints; only a successful Google request establishes availability and
  current authority. Unregister callbacks when their owner is disposed; no continuous polling.
- Reconnection retry runs only for an authenticated eligible primary and a dirty linked dataset.
  Guest, signed-out-local, secondary and erasing sessions never upload. No scheduler/background worker
  is included; suspended/killed apps retry on reopen. The pending revision survives process death.
- Capture a coherent committed dataset under a shared short maintenance gate; release the gate before
  network work. Serialize uploads per session. If revision R+1 is committed while R uploads, acknowledge
  only R and keep R+1 pending. Never hold a database transaction across HTTP.
- A backup envelope includes format version, owner subject, dataset generation, primary installation,
  installation lineage, backup operation ID,
  parent backup ID, local revision, capture instant, declared counts, catalog version, and exact data.
  Preserve movimiento ID, tipo, integer minor units, moneda, civil fecha, categoria ID, descripcion and
  createdAt. Include persisted category data and primary currency. Do not copy tokens, navigation,
  drafts, transient Undo or entire database files/WALs.
- UTF-8 JSON is versioned independently of the SQL schema. Decode Long values without floating-point
  conversion; dates remain civil ISO strings. A checked payload digest detects transport corruption;
  it is neither end-to-end encryption nor proof against malicious alteration. Use existing available
  digest primitives after compatibility validation, not a hand-written cryptographic implementation.
- Request a Drive-generated file ID in `appDataFolder`, persist it with the exact staged snapshot, and
  use it for upload retries. A timeout with unknown outcome must reconcile that ID/content before
  declaring success. A 409 means inspect the existing file, not assume identical bytes.
  [Generated IDs](https://developers.google.com/workspace/drive/api/reference/rest/v3/files/generateIds),
  [retry contract](https://developers.google.com/workspace/drive/api/guides/manage-uploads#use_a_pre-generated_id_to_upload_files).
- Use multipart creation for payloads within the agreed 5 MiB cap. Capture/stage/download enforce
  bounded size before unbounded allocation. Oversize data stays local with an actionable error.
  Resumable uploads or a larger cap require re-estimation, unless stage 1 proves they are needed for
  the accepted dataset. [Upload types](https://developers.google.com/workspace/drive/api/guides/manage-uploads).
- Verify remote content/counts/digest before advancing last-backed-up revision or removing staged
  bytes. Recovery discovery paginates the full app backup listing and validates downloaded candidates.
  Timestamp is display metadata, not the sole authority for ordering or choosing a dataset.
- Normal cloud retention is two verified backups for the selected account dataset/accepted lineage,
  plus at most one protected pre-restore recovery copy. A new normal upload can temporarily make
  three normal files until success is verified and pruning completes. Protected copies do not count
  against the two. Replace an older protected copy only after its replacement is verified; do not
  prune it because it is old or a new empty snapshot exists.
- Delete only obsolete verified files after newer success, preserving unresolved/staged work and
  unacknowledged branches until explicit resolution. Normal steady-state caps exclude failures being
  reconciled; storage/UI must expose incomplete cleanup rather than silently delete competing data.
  Never delete the last recoverable copy to free quota before replacement succeeds.
- Confirmed whole-copy resolution authorizes retirement of superseded Tallybook financial snapshots
  after the selected copy is verified and the protected recovery copy is secured. Retain only the two
  normal accepted copies and one protected copy; ignored-lineage files are not permanently exempt.
  Keep minimal acknowledged head/retirement metadata, not unlimited historical payloads. Failed cleanup
  is pending and visible until the agreed cap is reached. No arbitrary Moneta/other-app files are pruned.

### Request, latency and cost budget

Local mutations in guest and linked datasets issue zero Drive requests. Fresh control/head reads
belong to coalesced publication and explicit takeover/recovery, not every edit. Do not list the folder
or download financial snapshots for each mutation. Cache known file IDs and coalesce uploads;
full discovery/pagination is reserved
for first connection, recovery, control loss and conflict resolution.

Initial normal-cycle target: no more than eight Drive calls for one coalesced create/verify/control/
cleanup cycle, excluding authentication, discovery pagination and justified retry. Count requests with
the production HTTP factory tests and measure encoded bytes, download verification cost and elapsed
time on the physical device; these are planned targets, not measured performance. Connectivity or
authority failure pauses cloud work while local mutation continues. Conflict discovery/download may
need more calls and is measured separately from an ordinary nonconflicting backup cycle.

Google currently documents no additional cost for standard Drive API use and announces future charges
above quota thresholds; do not promise it is permanently free or invent a per-user bill. Actual quotas
and any billing notices must be inspected in the selected project before release. User Drive storage
limits still apply. [Usage and pricing](https://developers.google.com/workspace/drive/api/guides/limits).

### Restoration and device replacement

- A fresh/reinstalled app lists backups before creating an empty one. No backup is a valid empty
  state, distinguishable from permission failure or failed listing.
- Preview the selected candidate: account, capture date, source device, record counts and currencies.
  Never auto-select one of diverging device branches as the definitive latest dataset.
- Download, size-check, decode and validate the entire candidate before mutation: supported version,
  owner, counts/digest, unique IDs, known currencies/types, exact money, canonical dates/instants and
  compatible category records. Reuse persistence compatibility rules rather than creation-form rules;
  a legacy missing categoria or a date now in the future must not silently disappear or be rewritten.
  Unsupported or unreadable data aborts the whole restore and leaves local data intact.
- Association/recovery offers Local copy or Drive copy with account/count/date context. User choice
  replaces the destination completely; no additive import. If a local copy is chosen, protect the
  previous accepted cloud copy before publishing its replacement. If Drive is chosen, protect the
  nonempty local copy before local replacement. No available copy may be silently selected because
  another candidate failed to load.
- A protected guest copy remains local and must not upload merely because Google identity is known.
  A protected Google copy can be stored in Drive only with its owner's consent/permission and current
  publication authority; otherwise use a verified local recovery artifact and explain its device-only
  survival. Protective copies retain their original source owner; B never inherits A's data. An empty
  candidate requires the same destructive confirmation. Failed protection blocks replacement.
- All local protected artifacts have a typed source dataset ID and owner. Guest artifacts remain
  guest-owned even after selection of a Google copy. Linked-owner artifacts obey the logout-local
  access setting, cannot be opened through guest or another account, and are covered by linked-owner
  erasure. Device-local source protection is not a hidden bypass around the normal data access gate.
- Restoration takes the maintenance gate and disposes the active shell/drafts/Undo before replacing
  rows. SQL replacement, ownership, backup revision and an unfinished preference-handoff marker commit
  in one SQL transaction. Apply primary currency through its existing DataStore owner, then finalize
  the SQL marker. Startup resumes unfinished handoffs before bootstrap or normal UI; a preference
  failure cannot expose a dataset with the wrong restored currency or mark recovery complete.
- Restoring on a fresh installation establishes its local lineage from the selected verified backup.
  Taking the primary role on an existing installation preserves its dataset/lineage and pending work,
  advancing publication authority according to the transfer contract.
  Automatic device transfer must not copy installation identity, operation IDs or credential metadata.
  Read the known control/head IDs before ordinary publication. Full head discovery is needed only
  on initial enrollment, recovery or detected missing/conflicting state. If an unexpected branch is
  detected, stop automatic backup and ask the user to select the intended device/data.
  Immutable files preserve both branches if competing
  writers race between checks. Snapshot immutability alone does not implement primary exclusivity.
- A branch choice persists the selected head plus the observed heads of ignored/retired lineages in
  the local backup metadata. Those unchanged retained backups do not trigger the same conflict again
  after restart. Advance of any ignored head is a new conflict; no automatic merge. Confirmed resolution
  then retires superseded payloads under the agreed retention policy, keeping acknowledged IDs as
  metadata even after their old files are pruned.
  A restored/new installation explicitly enrolls its selected parent and records the observed branch
  choices before enabling backup. Pruned ancestors are not proof of a new branch: discovery uses
  lineage/head metadata and the acknowledged resolution, not an assumption that every parent file
  still exists. Loss of resolution metadata requires an explicit choice again.
- On same-device resume, remote backups are not silently imported over local changes. Recovery remains
  a separate explicit action. Automatic multi-device convergence is deferred.

### User-visible state

Use an account/backup entry in the existing shell and shared token/component system, with onboarding
and restore confirmation surfaces. Reuse the established ViewModel/UiState/Actions pattern.

Show selected account, Drive access state, last verified backup time and pending status. Distinguish
local save success from Drive success: Pending, Backing up, Backed up, Permission required, Failed and
Device/data conflict. Persistent errors survive screen navigation; transient snackbar feedback uses
the existing feedback mechanism. A failed cloud write never changes a locally committed movimiento
into a failed local save. Local save failure never triggers a cloud backup of an uncommitted draft.
Support existing Spanish/default, English, Argentine Spanish and Brazilian Portuguese resources,
accessible loading/status labels and the official Google sign-in branding.

Also show Guest, Signed-out local access, Pending sign-in/connection, Secondary/cannot upload and
Copy choice required states. Accessible retained Google data keeps its local mutation controls while
offline or signed out with the setting enabled; only cloud actions require login/current authority.
Keep the guest/cloud identity visible enough to explain which data is being edited and why it has
not uploaded. Reconnect/takeover actions never imply a merge or confirmation of a particular copy.

### Screen inventory and supplied visual inspiration

The supplied images are inspiration, not final designs or accepted permission/identity contracts.
Reuse Tallybook's existing typography, green accent, background/surface tokens, rounded controls and
spacing; do not introduce a second visual system or copy Moneta branding. UI design and implementation
are included in the 12–16-hour shared UI allowance; automated/physical verification is also
included in the separate test allowance. A full profile, bespoke illustration system or extensive
motion direction is outside this screen inventory and would require re-estimation.

| Surface | Owner and included behavior | Inspiration |
| --- | --- | --- |
| Welcome / Google sign-in | Tallybook Compose screen: brand, short value statement, Google CTA, guest CTA, working terms/privacy links, loading and retry/error states | [Welcome reference](references/google-drive/welcome-inspiration.png) |
| Account selection | Native Google/Credential Manager surface; integrate launching, selection/cancellation and lifecycle, without rebuilding its account list or hardcoding reference identities | [Account-picker reference](references/google-drive/account-picker-inspiration.png) |
| Drive explanation | Tallybook Compose screen before native authorization: explain private app-folder storage, limited access and permission revocation, then launch actual consent | [Drive-access reference](references/google-drive/drive-access-inspiration.png) |
| Actual Google consent | Native Google authorization UI; app appearance/text follows Google configuration and platform behavior, not a custom facsimile | Native SDK/Console configuration |
| Existing backup / recovery choice | Tallybook surface: discovery loading/no backup/error, preview counts/source/date/account, adoption choice and explicit destructive-recovery confirmation | Existing Tallybook components |
| Account and backup controls | Tallybook surface within the existing shell: guest/account/access mode, local-access toggle, principal/publication state, pending changes/conflict choice, last verified backup, Back up now, Retry, Restore, Become primary and sign-out | Existing Tallybook components |
| Disconnection / deletion | Tallybook confirmation/progress/error surfaces for the agreed local/cloud erasure contract; includes partial-failure retry | Existing Tallybook components |

Google flow: Welcome -> native account selection -> Tallybook Drive explanation -> native consent ->
backup discovery/copy choice -> primary enrollment or local access with publication paused -> Home.
Guest flow: Welcome -> Continue as guest -> guest Home -> optional later Google association with
explicit copy replacement. Returning sessions follow the access-toggle and principal contracts.

Keep the spacious centered welcome and anchored primary CTA from the first reference; adapt the
permission explanation's readable heading, grouped benefits and primary/secondary action hierarchy
from the third. Layouts must scroll/reflow for smaller viewports and enlarged text while keeping the
primary action reachable; cover light/dark themes and supported locales. Match actual permission
scope in copy. Describe where financial data is stored rather than making an absolute "no servers"
promise that would become false when telemetry is introduced. The Drive reference does not establish
end-to-end encryption, visible Drive files or guaranteed immediate backup.

The Drive explanation's "Not now" keeps the current data in guest/local mode with no cloud backup;
it does not erase data or silently mark Drive connected. An already-linked retained account still
follows the account access setting rather than being relabeled as guest. Guest mode is accepted
product scope; adding that CTA overrides the reference's single-button welcome layout.

Use Google's current approved sign-in assets and required logo/text/padding treatments even when the
surrounding screen uses Tallybook tokens. The reference button's exact shadow, geometry and font are
not permission to override Google branding.
[Google sign-in branding](https://developers.google.com/identity/branding-guidelines).

## Google and platform configuration checklist

Owner: project maintainer for Cloud permissions/signing/domain ownership; platform implementers for
native configuration and integration evidence. No actual Console settings were changed during planning.

| Configuration | Concrete completion evidence |
| --- | --- |
| Cloud environments | Development and production projects identified; audit Moneta before reuse. Keep Tallybook file namespaces isolated. Android and future iOS clients for an environment belong to the same project. Prove shared appDataFolder access when iOS is delivered. Never test destructive cases against real user data. |
| Drive API | Enable Google Drive API v3 in each environment; credentials have the intended project and consent configuration. |
| Google Auth Platform | Branding, audience, data access, developer/support contacts and public home/privacy/terms URLs configured. External audience for ordinary Google accounts; named testers while Testing. |
| Scopes | Identity scopes only as required plus `https://www.googleapis.com/auth/drive.appdata`. No full-Drive scope. Hidden app storage is non-sensitive, but still requires consent and production branding/readiness. [App data](https://developers.google.com/workspace/drive/api/guides/appdata). |
| Android OAuth clients | Package `com.kurobello.tallybook`; register actual SHA-1 for debug and each production signing certificate. For Play distribution use the Play app-signing certificate, not just the upload certificate. Web client ID for Credential Manager as required by its selected API. [Android setup](https://developer.android.com/identity/authorization). |
| Android app setup | Correct IDs per app environment, Internet permission, live Activity/result launcher, explicit credential-state clearing and token invalidation. Test the distributed signed build; debug success alone does not prove release OAuth configuration. |
| iOS OAuth client | Bundle ID `com.kurobello.tallybook`, native client ID, reversed-client-ID URL scheme and URL handler. Keep the shared TEAM_ID. Use Xcode for any project.pbxproj edits. [iOS setup](https://developers.google.com/identity/sign-in/ios/start-integrating). |
| iOS dependency integration | Validate the SPM/spm4Kmp path already selected in Stack, including framework linkage and physical callback/token restoration. Use its documented fallback only if the bounded integration fails. [Plugin primary source](https://github.com/frankois944/spm4Kmp), [SDK token refresh](https://developers.google.com/identity/sign-in/ios/api-access). |
| Platform automatic backup | Define Android cloud/device-transfer and iOS backup exclusions for session, operation staging and installation identity. Restored app files or surviving iOS Keychain state must not trigger an empty upload or duplicate writer identity. |
| OAuth production readiness | Review actual scope classification and branding/verification status. Testing has user restrictions; refresh tokens issued for external Testing apps with more than basic identity scopes can expire after seven days. Treat as environment behavior, not a custom weekly-token workaround. [OAuth lifecycle](https://developers.google.com/identity/protocols/oauth2), [production policy](https://developers.google.com/identity/protocols/oauth2/production-readiness/policy-compliance). |

### Google Play readiness for this feature

- Inventory actual identity, financial-data, installation-ID and SDK transfers; align the privacy page,
  prominent disclosures where required and Data safety/deletion answers with the shipped behavior.
  "No own backend" alone does not settle collection/sharing classifications or SDK disclosures.
  [Data safety guidance](https://support.google.com/googleplay/android-developer/answer/10787469).
- Resolve the app-account classification and supply the required in-app/web deletion pathway. Treat
  Google login + cross-device app identity as potentially in scope until the exact model is checked;
  do not infer an exemption because credentials come from Google.
  [Account deletion](https://support.google.com/googleplay/android-developer/answer/13327111).
- Establish Play Console ownership/account type/date, package registration and Play App Signing;
  register the app-signing certificate with OAuth. Create a release AAB using the project's actual
  signing setup; verify the installed internal-track build can sign in, save and recover. Internal app
  sharing can use a different certificate and must not substitute for track-signing evidence.
- Complete feature-relevant app-access/reviewer instructions, content rating/audience, financial
  features declaration and current target-API requirements against live Console requirements. Review
  global launch prerequisites separately; this feature cannot certify the whole unfinished MVP.
  [Financial declaration](https://support.google.com/googleplay/android-developer/answer/13849271).
- If the developer account is personal and was created after 2023-11-13, current published guidance
  requires at least 12 continuously opted-in closed testers for 14 days before applying for production
  access. Verify applicability in the account. Recruitment, the elapsed window and review waiting
  time are calendar dependencies, not coding hours.
  [Testing eligibility](https://support.google.com/googleplay/android-developer/answer/14151465).
- Public policy/help/deletion pages must load over stable HTTPS and use consistent Tallybook/developer
  branding. If no pages exist, the readiness allowance includes minimal static content/routes on the
  existing domain; new hosting/domain procurement or a full marketing website is extra scope.

Android publication preparation reduces predictable rejection causes; neither this checklist nor
passing device tests guarantees Google's approval. Publishing/submission is a later authorized action.

### Developer account and domain email

A Google Account is required; a Gmail address is not. An operational existing address such as
`dev@kurobello.com` can be registered as a Google Account and verified through its inbox. Alternatively,
an existing Google login can own Play Console while a separate domain address is used for public
support/developer contact. [Google account creation](https://support.google.com/accounts/answer/27441),
[Play contact requirements](https://support.google.com/googleplay/android-developer/answer/13628312).

Cloudflare routing can forward `dev@kurobello.com` or `support@kurobello.com` to the existing personal
mailbox. Audit existing MX/routing settings before changing them, verify the destination and test mail
from a different sender, then confirm reception of Google verification messages. Receiving forwarded
mail does not itself provide a domain mailbox/outbound SMTP service. Do not purchase Workspace just
to satisfy a nonexistent Gmail requirement. The account type must correspond to the actual legal
publisher; domain ownership alone does not establish an organization developer account.
[Cloudflare routing setup](https://developers.cloudflare.com/email-service/get-started/route-emails/).

`appDataFolder` is hidden from normal Drive file browsing and users can remove its data from Drive's
app-management controls. It is distinct from OS device backups. Hidden storage does not provide
application-level end-to-end encryption. Both loss of cloud data and explicit permission revocation
need recoverable UI. [Storage behavior](https://developers.google.com/workspace/drive/api/guides/appdata).

For App Store delivery, resolve Apple's current login-services rule before claiming release readiness.
Guideline 4.8 has equivalent-login requirements and a specific third-party-client exception; merely
using Drive does not establish that Tallybook qualifies. If another login is required, define how it
relates to mandatory Google Drive ownership and re-estimate it as a separate product change.
Privacy disclosures, account deletion where applicable, review access and SDK privacy manifests also
need release evidence. [Apple primary guidance](https://developer.apple.com/app-store/review/guidelines/#login-services).

## Boundary matrix

| Input or failure | Required outcome | Owner / smallest useful verification |
| --- | --- | --- |
| No Google account, no usable credential, consent denied/cancelled | Actionable retry/account-selection UI; no dataset loss or infinite prompt | Platform + session / fakes and physical native flow |
| Partial scopes, expired/revoked token | Preserve local commits; refresh once when appropriate or require reconnect; no prompt from background | Platform + network / MockEngine 401 and physical revocation |
| Workspace policy or missing Play services | Explain unavailable/blocked access; no fallback to deprecated login or broad Drive scopes | Platform / reachable physical account/device check when available |
| Wrong project/client ID/SHA-1/URL callback | Fail with useful configuration diagnosis, with no secret/token in logs | Platform / signed build and iOS callback integration |
| First connection: local-only, cloud-only, both, neither | Explicit adoption or recovery; never upload empty data before discovery completes | Session + data / small table-driven tests |
| Guest offline creation then Google login, denied permission or cancelled copy choice | Guest stays usable and unchanged; no cloud request before consent/association; confirmed choice replaces rather than merges | Session + data / isolated context and journal tests |
| Logout with local-access toggle enabled/disabled; cold start, Back or restored navigation | Enabled permits local reads/CRUD; disabled locks linked data; separate guest never exposes it; no automatic auth/upload | Session + UI / mode-state tests and physical relaunch |
| Google mutation while offline, signed out with local access enabled or secondary | Local create/edit/delete/Undo/preferences commit and mark dirty with zero HTTP; only publication waits for authentication/connectivity/current primary | Session + data / context-gated real repository, reopen and zero-request assertions |
| Two concurrent takeovers or transfer during upload/commit | Safe authority transition proven by integration; stale work never publishes as current; incomplete transfer blocks upload without discarding local changes | Platform + backup / stage-1 live race test and crash recovery |
| A edits offline while B becomes primary and edits Drive | A keeps local changes; reconnect cannot auto-upload; explicit promotion then local/Drive whole-copy choice before publication | Backup + session / two dataset fixtures with controlled remote heads and physical acceptance |
| Takeover succeeds then copy choice is cancelled or app restarts | New primary role retained, local rows/pending baseline unchanged and publication stays paused; no implicit restore/upload | Backup + session / journal reopen and exact row/head assertions |
| Local revision or remote head changes after copy preview | Stale choice cannot replace/publish data; refresh candidates and protect the selected losing copy | Backup + data / suspended operation with revision/head changes |
| Sign into B while data belongs to A | Reject access/upload; preserve A; gate explicit replacement | Session / fake late callbacks + integration transaction |
| Sign-out/account change during upload or Undo | Old epoch cannot acknowledge, restore or display data in new session | Session + data / controlled suspended-job tests |
| Deletion while Google accepts an earlier upload; external deletion then stale-device resume | No obsolete success or automatic resurrection; reconcile the file, retain incomplete-erasure state and quarantine stale data | Session + backup / controlled late HTTP outcome, database reopen and isolated external-deletion check |
| Local revision changes while snapshot uploads | Acknowledge captured revision only; next revision remains dirty | Backup + data / deterministic scheduler and real SQL writes |
| Process killed before/after local commit, upload, acknowledgement or recovery marker | Resume safely from durable state; no false cloud success or mixed recovery | Data + backup / reopen database + failure injection; physical relaunch |
| Timeout after Google accepted upload, or duplicate Retry | Same pre-generated ID; verify existing bytes; no duplicate movement/data overwrite | Network + backup / 409/unknown-outcome contract test |
| No network, 429, retryable 403 reason, 5xx | Bounded backoff with jitter; local pending survives; explicit retry after exhaustion | Network / MockEngine reason/status sequence |
| 403 quota, denied access or API configuration error | Classify by reason; no indiscriminate retry or quota cleanup that loses last backup | Network / MockEngine; [Drive error meanings](https://developers.google.com/workspace/drive/api/guides/handle-errors) |
| Backup deleted externally, stale file ID or interrupted listing | Rediscover/offer new backup from owned local data; distinguish no-data from failed read | Backup / 404 and multi-page listing tests |
| Malformed, truncated, oversized, future-version or wrong-owner payload | Reject before mutation; preserve local database; compatible older backup can be selected explicitly | Codec + data / serialized fixtures and rollback assertions |
| Duplicate IDs, exact Long extremes, CLP versus two-decimal currencies | Preserve representable exact values or reject incompatible input; never coerce silently | Codec + data / domain-derived fixtures |
| Missing legacy categoria or clock moved backward | Preserve accepted persisted semantics; no creation-only validation on recovery | Data / fixture matching repository compatibility policy |
| Failed preference write after SQL replacement | Keep startup in recovery; complete handoff on retry before shell opens | Data + session / DataStore failure + database reopen |
| Crash during ordinary currency change between SQL journal, DataStore commit and revision completion | Reconcile command before capture/UI; never mark a changed preference backed up at the old revision | Data / failure injection at each boundary + both stores reopened |
| Delete followed by Undo while upload is in flight | Final snapshot matches final SQL state; no restored row from an old account/restore session | Data + backup / existing mutation path + coordinator test |
| Two writers or cloned installation metadata | Preserve distinct snapshots; stop on observed branches; explicit choice, no silent newest-wins | Backup + platform / staged two-device scenario |
| Select one branch, restart, back up, then retired device advances | Unchanged ignored head stays acknowledged; a new retired-head advance stops backup again | Backup / persistent resolution metadata + shuffled listings |
| Broken newest backup, paginated history or device clock skew | Discover candidates fully; validate before offering recovery; revision/lineage and explicit choice outrank timestamps | Backup / shuffled multi-page candidates |
| Failed retention cleanup | Keep verified backup status; surface cleanup failure separately; preserve protected/other-lineage files | Backup / deletion failure test |
| Two regular backups and a protected copy, repeated transfer/resolution, failed pruning and another restore | Verified resolution retires ignored-lineage payloads; reach two-plus-one steady state; protected copy survives and cleanup failures remain visible | Backup / exact file-ID/count assertions and repeated-cycle request counts |
| Guest erase offline versus Google erase signed out/offline | Guest data can be cleared after confirmation; linked erase requires owner authentication/Drive/network; never cross context/app boundary | Data + session / both contexts with failure/reopen assertions |
| Guest -> choose Drive replacement -> logout -> guest erasure with both owners' protected artifacts present | Guest erase removes only guest artifacts; Google artifacts remain access-gated and linked erase later removes them | Data + session / artifact owner/path assertions with cold reopen |
| Long account labels, status errors, enlarged text, TalkBack/VoiceOver | Controls/status remain understandable and reachable | Shared UI / focused physical checks |

## Dependency stages and implementation ownership

Proposed paths below do not exist unless listed as existing. Package organization stays feature-vertical
inside the current shared module; this feature does not introduce a module tree. Exact class signatures
are finalized after the integration gate. Shared mutable files have one owner per active stage.

### Stage 1 — audit Moneta/Cloud and prove Android identity/Drive access

- Objective: settle Android SDK/network versions, real configuration and principal/deletion control
  behavior before building dependent UX.
- Android owner: `androidApp/src/main/kotlin/com/kurobello/tallybook/MainActivity.kt`, manifest/native
  configuration and proposed native auth
  adapter; activity-bound launchers and cancellation semantics.
- iOS adapter/SPM/callback implementation waits for the follow-up. Keep the common interfaces neutral
  and shared sources compilable without claiming iOS runtime support.
- Integration owner: version catalog and Gradle edits, proposed semantic auth/token interfaces in
  `core/data/auth`, per-platform Koin bindings. Platform SDK handles only presentation/credentials;
  common code owns identity/permission policy.
- Inputs: Cloud maintainer access, client IDs, signing fingerprints, confirmed physical Android device and
  selected scope. Outputs: stable common account identity, permission result and fresh access token.
- Exit: signed Android can sign in, cancel, grant Drive, create/read a tiny isolated test file through
  the common network boundary, restart and reacquire usable credentials. Record resolved versions and
  actual target evidence. Settle production project/namespace and operable deletion-path contracts
  before dependent writes. Prove competing takeover, transfer-versus-upload and stale-device control
  checks rather than assume atomicity from a read/update. Record domain email routing, public pages
  and exact Play account eligibility. Unsupported control semantics require re-planning before stage 2.

### Stage 2 — persistent backup/recovery boundary

- Data owner: existing movimiento/preference repositories, bootstrapper, proposed DatasetSession and
  guest/retained-Google scoped DI/driver/preference factories; SQL ownership/revision, local access gates,
  pending-upload and association/recovery markers. Reuse migration tests. Own DB/context lifetime
  changes in `DatabaseDriverFactory`, database/Repository/Preferences modules and platform path providers.
- Codec owner: proposed `core/model/backup` envelope contracts and `core/data/backup` codec/snapshot
  store; semantic/domain validation without Compose or platform imports.
- Network owner: proposed `core/network/drive`, Ktor client factory and typed Drive errors; platform
  engines and SDK-provided token refresh. Bearers are sent only to approved Google API endpoints;
  untrusted payloads never choose credential destinations. Own network DI/build files sequentially
  after stage 1 to avoid overlapping edits.
- Backup coordinator owner: proposed `core/data/backup` repository/coordinator, staged-byte ownership,
  primary control/transfer, acknowledged baseline and whole-copy resolution journal, revision
  acknowledgements, connectivity-triggered bounded retries,
  candidate discovery and two-plus-protected retention. Own its scoped connectivity binding and cleanup.
- Exit: real in-memory/file-backed SQLDelight -> production codec -> production Ktor configuration
  with MockEngine -> verified acknowledgement; restore round-trip restores IDs, values, ordering,
  categories, primary currency and balances. Failure injection proves rollback/reopen, guest isolation,
  promotion and preference/primary-transfer handoff safety. Assert request counts as well as correctness.
  This material data contract receives one bounded independent review before dependent UI integration.

### Stage 3 — session, onboarding and backup controls

- One integration owner for `AppStartupViewModel.kt`, `App.kt`, existing navigation/modal owners and DI
  startup composition. Proposed `feature/account/{ui,viewmodel}` contains guest/account/access-mode,
  logout-toggle, primary/transfer and backup surfaces without a full multi-profile screen.
- Use existing components/tokens and string resources; match current effects/state conventions.
- Connect every successful persistence mutation to revision tracking; local success and remote status
  stay distinct. Close retained management/Search/History owners when changing/restoring datasets so
  stale jobs, drafts and Undo cannot act on replaced data.
- Implement/test the confirmed deletion path and matching public help/deletion entry, with one owner
  for static page content and Cloud configuration. Keep staging/production namespaces and consent
  projects consistent; account deletion must not erase Moneta files in a reused project.
- Exit: accepted first-connection scenarios, offline returning user, account mismatch, manual backup,
  guest/linked offline CRUD, both logout-toggle modes, promotion without local replacement, copy
  conflict resolution/cancellation,
  recovery confirmation, reconnect and stale native/HTTP results have meaningful tests and working UI.

### Stage 4 — Android acceptance, Play readiness, final review and handoff

- Verification owner controls one confirmed physical Android target at a time and an isolated test
  dataset. Validate fresh-install recovery and the Play-signed internal-track artifact.
- Dedicated final reviewer owns no implementation files during review; review focused diff for loss,
  ownership, simpler alternatives, public behavior, cancellation and scope. Fix confirmed issues and
  rerun affected checks. No repeated intermediate reviews for routine UI changes.
- Document the actual accepted contracts in their canonical owners, update the feature registry only
  at the appropriate agreed state, and compact this draft when the feature closes. Merge still follows
  the repository's user-acceptance workflow.

### Deferred iOS increment

Implement the iOS adapter, SPM/SDK/callback configuration and token restoration after Android
acceptance. Verify shared Google subject/storage, physical iPhone lifecycle and cross-platform recovery
in both directions. Preserve TEAM_ID; Xcode owns project file mutations. Resolve Apple's release
requirements only when iOS publication is prioritized. This increment has its own runtime acceptance
and is not a hidden prerequisite for the first Android delivery.

## Verification and definition of done

Execution-time checks, not checks run during planning:

1. Targeted common/Android host tests through `./gradlew :shared:testAndroidHostTest`, including current
   movement creation/edit/delete/Undo regressions affected by transaction ownership/revision changes.
   Reuse the production HTTP factory with MockEngine; no assertion that mocks prove Google behavior.
2. SQL migration/schema verification through the existing `DatabaseSchemaTest` and relevant generated
   SQLDelight migration task discovered from the actual wrapper task list. Confirm no data loss when
   opening the current version-2 schema with the new owner/revision migration.
3. Narrow Android build `./gradlew :androidApp:assembleDebug`; mandatory Kotlin
   `./gradlew detekt ktfmtCheck`. Add Native compilation for affected shared sources; discover exact
   selected-target tasks rather than assume their names.
4. Preserve shared Native compilation as affected contracts change. Deferred iOS shell/runtime checks:
   build via the existing Xcode project/scheme and SDK (`xcodebuild -project
   iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphoneos build` when usable), followed by physical run.
   No simulator/emulator acceptance or simulator tests under this repository's physical-device policy.
5. Identify exactly one authorized physical Xiaomi/tablet/iPhone before each install/runtime/visual
   step. Resolve current JDK/SDK/adb/Xcode paths without editing local.properties or signing team.
   If hardware is unavailable, gate that target step and report it unverified.
6. Physical scenario: guest offline create -> association/Drive/copy confirmation -> linked offline
   create/edit/delete/Undo -> close/reopen with pending work intact -> sign-in/connect -> primary check
   -> verified backup. Verify local-access-enabled logout CRUD and local-access-disabled isolation.
   Use upload failure to prove local commits remain applied and foreground/reopen retry is safe.
   Test a changed remote head from another device, promotion without local replacement, both copy
   choices and cancelled/stale choice. Verify deletion authority, values and counters.
   Exercise permission revocation and user cancellation.
7. Android recovery on a fresh isolated installation or second authorized physical Android device;
   confirm IDs, dates, exact money, categories, primary currency, counts and derived balances.
   Android -> iPhone and iPhone -> Android recovery is required for the deferred iOS increment, using
   the same production/development project respectively. Reinstall/recovery tests require explicit
   dataset/device authorization; never erase the user's real dataset for QA.
8. Focused account A/B and guest isolation, recovery over nonempty data, interrupted recovery and
   principal transfer/concurrent-takeover
   checks. Use deterministic fault tests for hard-to-trigger failures plus a bounded physical check
   for native lifecycle behavior. Check main new Android controls with TalkBack and enlarged text;
   VoiceOver belongs to deferred iOS acceptance. Test deletion, no post-deletion re-upload and the
   external pathway independently from ordinary sign-out.

Android delivery requires passing relevant source checks, truthful Android guest/access-mode,
primary-control/transfer and runtime/recovery evidence, plus
Play-signed artifact evidence, consistent OAuth/privacy/deletion setup, no false Drive success, no
silent account data transfer, reviewer findings resolved and user acceptance. Missing Android/Cloud/
signing evidence means partial Android delivery. iOS remains explicitly deferred, not verified.

Planning-time evidence: repository/docs/manifest/source inspection and primary documentation review.
No build, tests, native SDK integration, device installation, Cloud audit or Google file write was run.
Document verification and independent planning-review results belong in the handoff, not a fake runtime
completion claim.

## Effort estimate

Engineering person-hours for an experienced KMP developer, including meaningful tests in each slice
and a final verification pass. Based on the inspected repository, not a measured throughput forecast.
Codex can assist implementation, but external setup, native integration and physical acceptance still
need elapsed time and human availability; agent parallelism does not divide the total by agent count.

| Work package | Hours |
| --- | ---: |
| Finalize contracts and Android/primary-control integration proof | 6–8 |
| Google Cloud/OAuth/environment configuration | 4–6 |
| Android authentication/authorization and lifecycle | 8–12 |
| Guest/Google contexts, access gates, migrations and recovery | 18–26 |
| Snapshot codec, Drive client, primary transfer and backup/retry | 16–22 |
| Shared guest/onboarding/account/access/backup/recovery UI | 12–16 |
| Integration/regression tests and physical Android QA | 14–20 |
| Independent review, fixes and delivery documentation | 6–8 |
| Play readiness, minimal public pages and deletion pathway | 8–14 |
| **Android-first base total** | **92–132** |
| **Android planning range with approximately 20% contingency** | **110–160** |

Central Android planning target: approximately 135 hours. At eight effective engineering hours/day this
is roughly 14–20 working days; with six effective hours/day, roughly 19–27 days. These are effort-derived
durations, not a promised date. Calendar time additionally depends on Cloud access, physical targets,
signing and external approval. The native proof in stage 1 should tighten the range before committing
to a delivery date.

Deferred iOS implementation and physical/cross-platform verification: approximately **25–40 additional
hours**, conditional on the supported SPM integration. Combined staged Android+iOS planning range:
**135–200 hours**. App Store submission/equivalent-login changes are not included.

Assumptions: one primary cloud publisher with fresh online checks, local editing available offline,
manual whole-copy conflict resolution, guest/retained-Google isolation,
foreground/reopen retry, complete backups within the agreed cap, no metrics backend/full sync,
existing UI reuse, maintainers available for Console, isolated physical test data and working
Android device. Minimal public pages/deletion flow reuse the existing domain/hosting; new hosting or a
developer-operated account/deletion backend requires re-estimation. Play test-window/review time and
finishing deferred profile/MVP work do not fit inside these engineering hours.

Guest/context switching, the logout access setting and primary transfer are substantive parts of this
range, not free side effects of Google login. Conditional control/deletion integration still gates the
estimate; unresolved cloud atomicity is not hidden inside the contingency.

Re-estimate before proceeding if safe primary-control transitions cannot be demonstrated with the
chosen APIs; automatic cross-device field/record merging or closed-app scheduling is required; the SPM bridge
cannot run on supported tooling; public-store release needs another login/account model; a dataset
exceeds the cap; independent local multi-account storage or custom end-to-end encryption is required;
or native/runtime infrastructure needs substantial repair. Do not fold these into the contingency as
though they were already included. Google/Apple verification and store waiting time are excluded from
engineering hours and cannot be guaranteed.

## Next executable increment

Inspect the existing Cloud project/domain/email routing and Play Console eligibility, then run stage
1's bounded Android identity/authorization/Drive and primary-transfer/deletion proof. Its outcome settles
compatibility, ownership, production namespaces and target access before dependent persistence/UI
work begins. Do not create a replacement Cloud project before checking whether the existing one is
development-only, usable for production or contains data/branding that should stay isolated.
