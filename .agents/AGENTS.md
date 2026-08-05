# Optix POS — Master Engineering Protocol

This project is an **Enterprise Offline-First POS SaaS**.

These rules apply to EVERY task, bug, and feature in this workspace without exception.

---

## Project Architecture

Every data flow must traverse ALL SIX layers:

```
Jetpack Compose UI
        ↓
    ViewModel
        ↓
Room Database (local truth)
        ↓
  SyncManager / RealtimeSyncManager
        ↓
  Socket.IO / WebSocket
        ↓
  NestJS Controller
        ↓
  NestJS Service
        ↓
  Prisma ORM
        ↓
  PostgreSQL
```

A feature is **NOT COMPLETE** unless ALL SIX layers are implemented and verified.

---

## Mandatory 4-Step Protocol

### STEP 1 — Find the REAL root cause

Before writing any code, **trace the complete architecture**:

```
UI → ViewModel → Repository → Room → SyncManager → RealtimeSyncManager
   → WebSocket → NestJS Controller → Service → Prisma → PostgreSQL
```

Do NOT guess. Do NOT assume. Trace everything.

---

### STEP 2 — Fix the ROOT CAUSE

- Never patch symptoms.
- Never hardcode values.
- Never duplicate state.
- Never create temporary fixes.
- If the architecture is wrong, fix the architecture.

---

### STEP 3 — Verify

Verification is NOT optional. Verify EVERY layer:

```
UI renders correctly
    ↓
Room updated immediately
    ↓
WebSocket event fired
    ↓
Backend received and saved to PostgreSQL
    ↓
Second device received the event
    ↓
App Restart → state restored
    ↓
Logout → state cleared
    ↓
Login → state restored from cloud
    ↓
Reinstall → full sync restores data
```

---

### STEP 4 — Write the Verification Report

Only after successful verification, produce this report:

```
ROOT CAUSE:
Files Modified:
Architecture Changes:
Database Changes:
API Changes:
Android Changes:
Verification:
Remaining Issues:
PASS / FAIL / NOT VERIFIED
```

**Never write PASS without actually testing. Never write FAIL without explaining why.**

If something cannot be verified, write `NOT VERIFIED` and explain why.

---

## Hard Prohibitions

You are FORBIDDEN from:

- ❌ Changing UI only and stopping
- ❌ Changing Room only and stopping
- ❌ Changing the backend only and stopping
- ❌ Changing WebSocket only and stopping
- ❌ Saying "fixed" without actually testing
- ❌ Assuming sync works without verifying
- ❌ Assuming uploads work without verifying
- ❌ Assuming WebSocket events fire without log evidence
- ❌ Assuming Room updated without querying or observing
- ❌ Assuming PostgreSQL updated without checking

Everything must be **proven with logcat, API response, or direct DB evidence**.

---

## Survival Checklist

Before declaring any feature COMPLETE, ask:

| Scenario | Must Pass |
| :--- | :--- |
| App Restart | ✅ State persists via Room |
| Logout | ✅ Sensitive state cleared |
| Login on same device | ✅ Cloud sync restores state |
| Second device | ✅ WebSocket propagates within 200ms |
| Offline writes | ✅ Queued and synced on reconnect |
| Reconnect | ✅ Missed events recovered via pull |
| Server restart | ✅ Client reconnects and re-syncs |
| Reinstall | ✅ Full cloud restore on first login |

If **any** scenario fails, the feature is **NOT COMPLETE**.

---

## Device Info

- Device A: `RZ8R91ZRCHM`
- Device B: `RZCW41K3EEH`
- Package: `com.aistudio.zaddybilling.qpxbwm`
- ADB: `$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe`
- Launch: `adb shell am start -n com.aistudio.zaddybilling.qpxbwm/com.example.MainActivity`
- Logcat filter: `adb logcat -s OPTIX_FLOW`

---

## Performance Targets

- WebSocket event → Room updated: **< 200ms**
- No repeated `GET /business/profile` while WebSocket is active
- No echo: sender device must never process its own events
- No polling while WebSocket is connected (`[WEBSOCKET ACTIVE] Skipping polling sync loop`)

---

## Absolute Verification Rule

If ANY verification test fails after implementation, you MUST continue debugging.

- Do NOT stop.
- Do NOT declare PASS.
- Do NOT move on to the next feature.
- Do NOT say "mostly working".

Continue until **ALL tests pass**.

If one single test fails, the feature is **INCOMPLETE**.

There are only three allowed final states:

```
PASS      — every single verification test passed with evidence
FAIL      — at least one test failed; root cause identified; debugging continues
NOT VERIFIED — test could not be run; explain exactly why
```

Writing PASS when any test has not been run is **FORBIDDEN**.

---

## Edit Justification Rule

For **every file you modify**, you MUST explicitly state:

> **WHY this file must change.**

If you cannot clearly explain why a file needs to change, **do not edit it**.

Format required before every file edit:

```
FILE: <filename>
REASON: <exact architectural reason why this file must change>
CHANGE: <what specifically is being added or removed>
```

Editing a file without this justification is **FORBIDDEN**.

This prevents:
- Unnecessary churn
- Accidental breakage of unrelated features
- Scope creep in file edits
- Superficial fixes that touch the wrong layer
================================================================================
                            OPTIX ENGINEERING CONSTITUTION
================================================================================

You are contributing to Optix, a production-grade Enterprise POS platform.

This is NOT a prototype.
This is NOT a demo.
This is NOT a practice project.

The application already has real paying customers.

Every change must preserve stability, scalability, security, offline capability,
multi-device synchronization and realtime consistency.

================================================================================
PRIMARY OBJECTIVE
================================================================================

Your objective is NOT to write code.

Your objective is to build reliable production software.

Correctness is always more important than speed.

Never sacrifice architecture to make one screen appear to work.

================================================================================
RULE 1 — NEVER BREAK EXISTING FEATURES
================================================================================

Before changing any file:

• Identify every feature depending on that file.
• Identify every module that could be affected.
• Understand the architecture first.

Never modify code blindly.

Every completed milestone MUST end with a complete regression test.

================================================================================
RULE 2 — ROOT CAUSE ENGINEERING
================================================================================

Never fix symptoms.

Always identify:

Problem
↓

Root Cause
↓

Architecture Impact
↓

Minimal Correct Fix
↓

Verification
↓

Regression Test

Never implement workarounds unless explicitly requested.

================================================================================
RULE 3 — NO GUESSING
================================================================================

Never assume.

Never write:

"I think..."

"It should..."

"Probably..."

Instead prove everything using:

Logs

Compiler Output

HTTP Requests

HTTP Responses

Prisma Results

Room Database

PostgreSQL

WebSocket Events

Stack Traces

Actual Runtime Behaviour

================================================================================
RULE 4 — AUDIT BEFORE EDITING
================================================================================

Before editing:

Understand

Architecture

↓

Dependencies

↓

Data Flow

↓

Runtime Flow

↓

Then write code.

Do NOT edit first and understand later.

================================================================================
RULE 5 — NEVER CREATE DUMMY IMPLEMENTATIONS
================================================================================

Do NOT create:

Placeholder UI

Fake buttons

TODO logic

Mock repositories

Temporary API calls

Hardcoded values

Stub implementations

Every UI element must perform its intended function.

If a feature is unfinished:

Leave it hidden.

Do NOT fake it.

================================================================================
RULE 6 — NO BUILD ERROR LOOPS
================================================================================

If compilation begins failing repeatedly:

STOP.

Do NOT continue patching compiler errors.

Instead:

Audit

↓

Find structural issue

↓

Repair architecture

↓

Compile again

Never chase compiler errors one by one.

================================================================================
RULE 7 — BUILD FREQUENTLY
================================================================================

Workflow:

Understand

↓

Edit

↓

Build

↓

Verify

↓

Continue

Never edit dozens of files before compiling.

================================================================================
RULE 8 — NO DUPLICATE CODE
================================================================================

Before creating:

Composable

Repository

DAO

Service

ViewModel

Utility

Helper

Search the project first.

Reuse existing code whenever possible.

Never duplicate logic.

================================================================================
RULE 9 — MODULAR ARCHITECTURE
================================================================================

Never create giant files.

Split by responsibility.

Example:

Billing/

Staff/

Inventory/

Customers/

Settings/

Analytics/

Sync/

Components/

One responsibility per module.

================================================================================
RULE 10 — ROOM IS THE SOURCE OF TRUTH
================================================================================

Compose UI

↓

ViewModel

↓

Repository

↓

Room Database

↓

Realtime Sync

↓

Backend

Never bind UI directly to HTTP responses.

================================================================================
RULE 11 — WEBSOCKET FIRST
================================================================================

Default architecture:

Realtime WebSocket

NOT polling.

Polling is only an emergency fallback.

Every shared business change should propagate through WebSocket events.

================================================================================
RULE 12 — BUSINESS IS THE ROOT ENTITY
================================================================================

Everything belongs to Business.

Business

├── Owner

├── Staff

├── Products

├── Categories

├── Customers

├── Orders

├── Inventory

├── Receipt

├── Payment QR

├── Settings

├── Sessions

├── Activity

├── Notifications

Never design Owner and Staff as separate businesses.

Staff inherits Business.

================================================================================
RULE 13 — PERMISSION ENFORCEMENT
================================================================================

Permissions must be enforced:

Android

AND

NestJS Backend

Never rely only on hiding buttons.

Every protected action must validate permissions.

================================================================================
RULE 14 — OFFLINE FIRST
================================================================================

Every feature must support:

Offline

↓

Queue

↓

Reconnect

↓

Conflict Resolution

↓

Realtime Synchronization

Offline users must never lose data.

================================================================================
RULE 15 — DATABASE SAFETY
================================================================================

Never introduce destructive migrations.

Every schema change requires:

Migration

Backward Compatibility

Verification

Existing customer data must never be lost.

================================================================================
RULE 16 — REALTIME CONSISTENCY
================================================================================

Whenever data changes:

Save PostgreSQL

↓

Emit WebSocket

↓

Receive Event

↓

Update Room

↓

Compose Recompose

Every connected device must remain consistent.

================================================================================
RULE 17 — NO HARDCODING
================================================================================

Never hardcode:

Business IDs

JWTs

URLs

Roles

Permissions

Dates

Timezones

Device IDs

Everything comes from configuration or database.

================================================================================
RULE 18 — DATA FLOW VERIFICATION
================================================================================

Every feature must be verified across the entire pipeline.

Compose

↓

ViewModel

↓

Repository

↓

Room

↓

Sync Manager

↓

WebSocket

↓

NestJS

↓

Prisma

↓

PostgreSQL

↓

WebSocket

↓

Room

↓

Compose

No broken link is acceptable.

================================================================================
RULE 19 — REGRESSION TESTING
================================================================================

Before declaring any milestone complete verify:

Authentication

Business Setup

Business Profile

Products

Categories

Customers

Inventory

Billing

Orders

History

Analytics

Receipt Settings

Logo

Payment QR

Realtime Sync

Offline Sync

Conflict Resolution

Staff Management

Permissions

Sessions

Activity Timeline

Notifications

Business Timings

Token Reset

WebSockets

Room

PostgreSQL

Android

NestJS

If any previous feature breaks,

the milestone is NOT complete.

================================================================================
RULE 20 — GIT DISCIPLINE
================================================================================

Before every milestone:

Create a commit or tag.

Never implement multiple large features without a recovery point.

================================================================================
RULE 21 — DO NOT TOUCH UNRELATED MODULES
================================================================================

Only modify files required for the current milestone.

If another module must change,

explain exactly why.

Do not refactor unrelated code.

Do not "clean up" unrelated modules.

================================================================================
RULE 22 — SECURITY
================================================================================

Never expose:

Passwords

JWT secrets

Private keys

API secrets

Database credentials

Hash passwords properly.

Validate JWTs.

Authorize every protected endpoint.

================================================================================
RULE 23 — PERFORMANCE
================================================================================

Avoid:

Repeated API calls

Duplicate database queries

Infinite recomposition

Memory leaks

Blocking the main thread

Prefer efficient, scalable implementations.

================================================================================
RULE 24 — COMPLETION CRITERIA
================================================================================

A feature is NOT complete because:

The screen exists.

The API exists.

The button exists.

A feature is COMPLETE only if:

✓ UI works
✓ Backend works
✓ Database works
✓ Room works
✓ PostgreSQL works
✓ WebSocket works
✓ Offline works
✓ Multi-device works
✓ Restart safe
✓ Logout safe
✓ Login safe
✓ Reinstall safe
✓ Regression tested
✓ Build successful

================================================================================
RULE 25 — OUTPUT FORMAT
================================================================================

Before writing code:

Provide:

1. Architecture Audit
2. Root Cause
3. Implementation Plan
4. Files To Modify
5. Database Changes
6. Backend Changes
7. Android Changes
8. Realtime Changes
9. Risks
10. Verification Plan

After implementation:

Provide:

✓ Files Modified
✓ Build Status
✓ Verification Results
✓ Regression Results
✓ Remaining Issues

Never claim success without verification.

================================================================================
FINAL RULE
================================================================================

Quality is more important than speed.

Never optimize for producing code quickly.

Optimize for:

Correctness

Maintainability

Scalability

Reliability

Security

Realtime consistency

Offline capability

Enterprise architecture

If there is any uncertainty:

STOP.

Investigate.

Understand.

Then implement.

Never guess.
================================================================================
Never implement a large feature in one milestone.

Every feature must be split into independently testable milestones.

Each milestone must:

Compile

Pass regression

Be deployable

Be rollback-safe

Only then continue.
Before every milestone:

git add .
git commit

After every successful milestone:

git tag

No milestone may begin without a recovery point.
Never decrease Room database version.

Every schema modification requires:

Migration

Verification

Restart

Upgrade

Fresh install

Existing install

No destructive migration.

Never rely on uninstalling for production.
Never modify files that are unrelated.

Before editing:

Explain WHY.

Explain WHICH architecture layer changes.

Explain WHICH features could regress.

Minimize edited files.

Minimize AI context usage.

Avoid rewriting large files unnecessarily.
Before implementation:

State:

Rollback plan

Affected modules

Recovery plan

Git commit reference

If rollback cannot be performed,

do not start implementation.
No feature is allowed to break:

Authentication

Billing

Realtime

Offline

WebSockets

Room

Staff

Products

Orders

Business Profile

If any existing feature breaks,

the milestone automatically fails.
Never say:

Done

Fixed

Implemented

Completed

Unless:

Project builds

Backend builds

Android builds

Physical devices tested

Logs verified

Regression verified

Evidence provided