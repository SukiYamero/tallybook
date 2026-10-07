# Google access and Drive persistence — planning draft

Status: proposed scope, awaiting the product decisions below. No application implementation or Cloud
configuration has been performed. Baseline: `f826e32`; the worktree was clean and on detached HEAD when
planning started. Review date: 2026-10-07.

## Objective and authority

Deliver Google authentication, explicit Drive consent, local-first saving, recoverable cloud backups,
and restoration on Android first, with Google Play preparation after physical-device acceptance.
iOS runtime delivery is explicitly deferred; shared business logic remains in commonMain. The
accepted MVP requirement and the boundary with the future sync
engine live in [Business logic](../business-logic.md#alcance-del-mvp). Stack choices live in
[Stack](../stack.md); workflow, external-API evidence and acceptance gates live in
[Repository workflow](../README.md). This draft proposes new behavior; it does not change accepted
domain rules or mark a feature delivered.

Observable acceptance: sign in, grant Drive access, create/edit/delete a movimiento, observe local
commit and a verified Drive backup, restart, and restore the same data on a fresh Android installation
or another authorized physical Android device.
Deleting and Undo must be represented by the eventual backup of the current committed dataset.

## Repository evidence and consequences

| Observation | Evidence | Consequence and task owner |
| --- | --- | --- |
| Real local persistence and mutation flows already exist | `shared/src/commonMain/sqldelight/com/kurobello/tallybook/core/database/{Movimiento,Categoria}.sq`; `core/data/MovimientoRepositoryImpl.kt` | Extend their transaction boundary; do not rebuild movimiento management. Data owner, stage 2. |
| One database, without account ownership | `core/database/DatabaseDriverFactory.kt`: `tallybook.db`; tables have no account discriminator | Bind the entire local dataset to one Google subject. Prevent accidental upload under another account. Data/session owners, stages 2 and 3. |
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

## Decisions requiring product confirmation

1. **Device model:** recommend one primary writing device per account, with explicit backup/restore and
   device replacement. Independent editing and automatic combination across devices belongs to the
   sync feature. The proposal protects separate backups but does not provide a distributed writer lock.
2. **Delivery target — confirmed:** physical Android acceptance first, then Google Play readiness;
   iPhone can wait. The estimate includes the feature's Play-facing configuration, data disclosures
   and deletion pathway, not completion of unrelated MVP features or a guarantee of store approval.
3. **Cloud prerequisites — partly known:** a Moneta project and Cloudflare domain/tunnel were created.
   Audit its current environment, clients, ownership, branding and public pages before creating a new
   project. Their live state was not inspected through an authenticated Google Cloud session.
4. **Backup location:** proposed hidden `appDataFolder`, accessed only by Tallybook. If users need a
   visible/exportable Drive file, re-plan around `drive.file` and file selection/export UX.
5. **Existing data:** proposed explicit adoption by the first Google account. If both local and cloud
   datasets exist, choose one after preview and protecting the local dataset; no silent merge.
6. **Retention/limits:** proposed three verified recent backups for the current unambiguous device
   lineage, plus a protected backup before destructive recovery. Initial supported payload cap: 5 MiB
   of encoded JSON. Confirm the cap against a representative user dataset before implementation.

Proceed with research and planning under these proposed defaults. Account-switch, replacement,
retention and destructive restoration behavior must be agreed before dependent implementation.

Profiles and a developer-owned metrics/backend system are explicitly deferred. The small account and
backup controls required to operate this feature do not implement a full profile screen. Include
redacted local diagnostics and useful error states now; do not add remote telemetry or its consent/
privacy/dependency surface to this delivery. Public release remains gated on the rest of the MVP.

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

- Fresh installations require Google sign-in and initial Drive consent before the normal production
  flow. Cancellation or denial returns to a recoverable onboarding state without deleting local data.
  Development catalog access remains governed by the existing launch-mode rules.
- Returning users with an already owned local dataset can work offline. Expired/revoked Drive access
  produces a persistent reconnect/pending state, not an automatic local wipe or a login prompt for every
  movimiento. Explicitly signed-out users cannot access the normal dataset shell.
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

- Keep the single existing database. Add an owner record for the whole dataset instead of adding
  account predicates to every movimiento query. Session identity, local owner and operation epoch
  must agree before any cloud operation or local mutation.
- The unowned legacy dataset is not uploaded until adoption is explicitly confirmed. If cloud data
  also exists, show counts/currencies and recovery choices before writing either dataset.
- Sign-out cancels/detaches cloud work, clears credential state, closes navigation/management owners,
  drafts and Undo queues, and hides local data. It keeps the owned database for later reconnection.
  Warn about pending changes; signing out must not pretend they reached Drive.
- Signing into B while the retained dataset belongs to A does not show A's data or upload it into B.
  Offer reconnecting A, or an explicit local replacement flow. Replacement is gated until A's data is
  protected by a verified backup; offline or pending A data blocks replacement. No arbitrary transfer
  from A to B and no simultaneous local multi-account workspace in this delivery.
- Sign-out, revoke Google access, erase local data and delete Drive backups are distinct actions.
  Account switching uses the replacement contract, not cloud deletion. Include a clearly confirmed
  deletion/disconnection path for Play readiness: cancel backup work first, delete only Tallybook-owned
  cloud files while permission remains, then erase local owned data and revoke credentials. Partial
  remote deletion is retryable and must not report full completion. Prevent this installation's pending
  work from re-uploading erased data, including on relaunch. Settle other-device invalidation in the
  deletion integration gate; do not claim to erase an offline device's local cache. Never delete the
  user's Google account or Moneta data.
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

### Local-first backup

- Every successful insert, update, delete, Undo restoration and affected persisted preference change
  marks the dataset dirty. Real SQL data mutations increment a persistent local revision in the same
  transaction. No-op/missing-ID outcomes do not create a fake data change.
- Currency changes use the same durable preference handoff as restoration: journal the intended
  owner/epoch/value in SQL first, apply through the existing DataStore owner, then finalize and advance
  the SQL revision. A pending journal blocks snapshot acknowledgement/capture and normal dataset
  entry until reconciled on retry/startup. The SQL value is a pending command, not a second readable
  preference source. A crash between the two stores cannot leave changed currency marked backed up.
- A single application-owned backup coordinator coalesces changes for approximately one second while
  foregrounded. Reattempt pending work on launch/resume and explicit Retry/Back up now. No guarantee
  that a suspended/killed app uploads in the background; the pending state survives process death.
- Capture a coherent committed dataset under a shared short maintenance gate; release the gate before
  network work. Serialize uploads per session. If revision R+1 is committed while R uploads, acknowledge
  only R and keep R+1 pending. Never hold a database transaction across HTTP.
- A backup envelope includes format version, owner subject, installation lineage, backup operation ID,
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
- Retention deletes only older verified snapshots from the same established lineage after a newer
  verified success. Preserve protected pre-restore snapshots and other/ambiguous lineages. Failed
  pruning does not undo a successful backup; it is separately observable. Never delete the last
  recoverable cloud copy to free quota before replacement has succeeded.

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
- On nonempty local data, explicit confirmation is required. Preserve a verified recovery copy of
  the current local dataset before replacement. Offline or failed protection blocks this destructive
  step. A valid empty snapshot can clear nonempty local data only through this same confirmation.
- Restoration takes the maintenance gate and disposes the active shell/drafts/Undo before replacing
  rows. SQL replacement, ownership, backup revision and an unfinished preference-handoff marker commit
  in one SQL transaction. Apply primary currency through its existing DataStore owner, then finalize
  the SQL marker. Startup resumes unfinished handoffs before bootstrap or normal UI; a preference
  failure cannot expose a dataset with the wrong restored currency or mark recovery complete.
- Replacing a device forks a new installation lineage from the explicitly selected verified backup.
  Automatic device transfer must not copy installation identity, operation IDs or credential metadata.
  List remote heads before uploading; if an unexpected branch is detected, stop automatic backup and
  ask the user to select the intended device/data. Immutable files preserve both branches if competing
  writers race between checks. This is detection and preservation, not global mutual exclusion.
- A branch choice persists the selected head plus the observed heads of ignored/retired lineages in
  the local backup metadata. Those unchanged retained backups do not trigger the same conflict again
  after restart. Advance of any ignored head is a new conflict; no automatic merge or deletion.
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

### Screen inventory and supplied visual inspiration

The supplied images are inspiration, not final designs or accepted permission/identity contracts.
Reuse Tallybook's existing typography, green accent, background/surface tokens, rounded controls and
spacing; do not introduce a second visual system or copy Moneta branding. UI design and implementation
are included in the existing 8–12-hour shared UI allowance; automated/physical verification is also
included in the separate test allowance. A full profile, bespoke illustration system or extensive
motion direction is outside this screen inventory and would require re-estimation.

| Surface | Owner and included behavior | Inspiration |
| --- | --- | --- |
| Welcome / Google sign-in | Tallybook Compose screen: brand, short value statement, Google CTA, working terms/privacy links, loading and retry/error states | [Welcome reference](references/google-drive/welcome-inspiration.png) |
| Account selection | Native Google/Credential Manager surface; integrate launching, selection/cancellation and lifecycle, without rebuilding its account list or hardcoding reference identities | [Account-picker reference](references/google-drive/account-picker-inspiration.png) |
| Drive explanation | Tallybook Compose screen before native authorization: explain private app-folder storage, limited access and permission revocation, then launch actual consent | [Drive-access reference](references/google-drive/drive-access-inspiration.png) |
| Actual Google consent | Native Google authorization UI; app appearance/text follows Google configuration and platform behavior, not a custom facsimile | Native SDK/Console configuration |
| Existing backup / recovery choice | Tallybook surface: discovery loading/no backup/error, preview counts/source/date/account, adoption choice and explicit destructive-recovery confirmation | Existing Tallybook components |
| Account and backup controls | Tallybook surface within the existing shell: selected account, last verified backup, pending/failure states, Back up now, Retry, Restore and sign-out entry | Existing Tallybook components |
| Disconnection / deletion | Tallybook confirmation/progress/error surfaces for the agreed local/cloud erasure contract; includes partial-failure retry | Existing Tallybook components |

Flow: Welcome -> native account selection -> Tallybook Drive explanation -> native Google consent ->
backup discovery/adoption or restore choice -> existing Home. Returning owned sessions follow the
startup/offline/reconnect contract rather than replaying every onboarding screen.

Keep the spacious centered welcome and anchored primary CTA from the first reference; adapt the
permission explanation's readable heading, grouped benefits and primary/secondary action hierarchy
from the third. Layouts must scroll/reflow for smaller viewports and enlarged text while keeping the
primary action reachable; cover light/dark themes and supported locales. Match actual permission
scope in copy. Describe where financial data is stored rather than making an absolute "no servers"
promise that would become false when telemetry is introduced. The Drive reference does not establish
end-to-end encryption, visible Drive files or guaranteed immediate backup.

The reference's "Not now" action is not an accepted guest/local-only mode. Under the current proposed
first-run contract it pauses onboarding without opening the normal shell, preserves existing data,
and permits later retry. A product decision to allow full use without initial Drive consent changes
that contract and must be reflected in the canonical MVP rules before implementation.

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
| Long account labels, status errors, enlarged text, TalkBack/VoiceOver | Controls/status remain understandable and reachable | Shared UI / focused physical checks |

## Dependency stages and implementation ownership

Proposed paths below do not exist unless listed as existing. Package organization stays feature-vertical
inside the current shared module; this feature does not introduce a module tree. Exact class signatures
are finalized after the integration gate. Shared mutable files have one owner per active stage.

### Stage 1 — audit Moneta/Cloud and prove Android identity/Drive access

- Objective: settle SDK/bridge versions and real configuration before building dependent UX.
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
  before dependent writes. Record existing domain/pages and the exact Play account eligibility.

### Stage 2 — persistent backup/recovery boundary

- Data owner: existing movimiento/preference repositories, bootstrapper and proposed SQL ownership,
  revision, pending-upload and recovery-marker source/migration. Reuse SQLDelight migration tests.
- Codec owner: proposed `core/model/backup` envelope contracts and `core/data/backup` codec/snapshot
  store; semantic/domain validation without Compose or platform imports.
- Network owner: proposed `core/network/drive`, Ktor client factory and typed Drive errors; platform
  engines and SDK-provided token refresh. Bearers are sent only to approved Google API endpoints;
  untrusted payloads never choose credential destinations. Own network DI/build files sequentially
  after stage 1 to avoid overlapping edits.
- Backup coordinator owner: proposed `core/data/backup` repository/coordinator, staged-byte ownership,
  revision acknowledgements, bounded retries, candidate discovery and retention.
- Exit: real in-memory/file-backed SQLDelight -> production codec -> production Ktor configuration
  with MockEngine -> verified acknowledgement; restore round-trip restores IDs, values, ordering,
  categories, primary currency and balances. Failure injection proves rollback/reopen/handoff safety.
  This material data contract receives one bounded independent review before dependent UI integration.

### Stage 3 — session, onboarding and backup controls

- One integration owner for `AppStartupViewModel.kt`, `App.kt`, existing navigation/modal owners and DI
  startup composition. Proposed `feature/account/{ui,viewmodel}` contains account/backup surfaces.
- Use existing components/tokens and string resources; match current effects/state conventions.
- Connect every successful persistence mutation to revision tracking; local success and remote status
  stay distinct. Close retained management/Search/History owners when changing/restoring datasets so
  stale jobs, drafts and Undo cannot act on replaced data.
- Implement/test the confirmed deletion path and matching public help/deletion entry, with one owner
  for static page content and Cloud configuration. Keep staging/production namespaces and consent
  projects consistent; account deletion must not erase Moneta files in a reused project.
- Exit: accepted first-connection scenarios, offline returning user, account mismatch, manual backup,
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
6. Physical scenario: clean test installation -> sign-in/consent -> create -> verified backup -> edit
   -> delete/Undo -> offline create -> close/reopen -> pending retry -> verified backup. Restart and
   check values, counters and status. Exercise permission revocation and user cancellation.
7. Android recovery on a fresh isolated installation or second authorized physical Android device;
   confirm IDs, dates, exact money, categories, primary currency, counts and derived balances.
   Android -> iPhone and iPhone -> Android recovery is required for the deferred iOS increment, using
   the same production/development project respectively. Reinstall/recovery tests require explicit
   dataset/device authorization; never erase the user's real dataset for QA.
8. Focused account A/B isolation, recovery over nonempty data, interrupted recovery and second-writer
   checks. Use deterministic fault tests for hard-to-trigger failures plus a bounded physical check
   for native lifecycle behavior. Check main new Android controls with TalkBack and enlarged text;
   VoiceOver belongs to deferred iOS acceptance. Test deletion, no post-deletion re-upload and the
   external pathway independently from ordinary sign-out.

Android delivery requires passing relevant source checks, truthful Android runtime/recovery and
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
| Finalize contracts and Android integration/dependency proof | 4–6 |
| Google Cloud/OAuth/environment configuration | 4–6 |
| Android authentication/authorization and lifecycle | 8–12 |
| Local ownership, revision/migration and crash-safe recovery | 12–18 |
| Snapshot codec, Drive client, backup/retry/retention | 12–16 |
| Shared onboarding/account/backup/recovery UI | 8–12 |
| Integration/regression tests and physical Android QA | 10–14 |
| Independent review, fixes and delivery documentation | 4–6 |
| Play readiness, minimal public pages and deletion pathway | 8–14 |
| **Android-first base total** | **70–104** |
| **Android planning range with approximately 20% contingency** | **85–125** |

Central Android planning target: approximately 105 hours. At eight effective engineering hours/day this
is roughly 11–16 working days; with six effective hours/day, roughly 15–21 days. These are effort-derived
durations, not a promised date. Calendar time additionally depends on Cloud access, physical targets,
signing and external approval. The native proof in stage 1 should tighten the range before committing
to a delivery date.

Deferred iOS implementation and physical/cross-platform verification: approximately **20–35 additional
hours**, conditional on the supported SPM integration. Combined staged Android+iOS planning range:
**105–160 hours**. App Store submission/equivalent-login changes are not included.

Assumptions: one primary writing device, complete backups within the agreed cap, no metrics backend/
full sync, existing UI reuse, maintainers available for Console, isolated physical test data and working
Android device. Minimal public pages/deletion flow reuse the existing domain/hosting; new hosting or a
developer-operated account/deletion backend requires re-estimation. Play test-window/review time and
finishing deferred profile/MVP work do not fit inside these engineering hours.

Re-estimate before proceeding if simultaneous editors/automatic merging are required; the SPM bridge
cannot run on supported tooling; public-store release needs another login/account model; a dataset
exceeds the cap; independent local multi-account storage or custom end-to-end encryption is required;
or native/runtime infrastructure needs substantial repair. Do not fold these into the contingency as
though they were already included. Google/Apple verification and store waiting time are excluded from
engineering hours and cannot be guaranteed.

## Next executable increment

Confirm the device/backup scope, inspect the existing Moneta Cloud project/domain and Play Console
eligibility, then run stage 1's bounded Android identity/authorization/Drive proof. Its outcome settles
compatibility, ownership, production namespaces and target access before dependent persistence/UI
work begins. Do not create a replacement Cloud project before checking whether the existing one is
development-only, usable for production or contains data/branding that should stay isolated.
