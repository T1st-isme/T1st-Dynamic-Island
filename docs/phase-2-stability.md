# Phase 2: service lifecycle and activity arbitration

## Scope

This phase builds on the permission/service diagnostics from phase 1. It stabilizes resource
ownership and primary/bubble selection; it does not add new card types, languages or automatic
restart loops. Existing feature eligibility, expiry rules, category animations and gestures remain.

## Service sessions

`ServiceSession` serializes binding transitions on Android's main thread:

- Repeated connection callbacks initialize resources once.
- Unbind releases the session immediately; the later destroy callback is a no-op for that session.
- A subsequent connection on the same service instance gets a fresh coroutine scope and overlay
  lifecycle owner, rather than a cancelled scope or destroyed lifecycle.
- Partial initialization invokes cleanup. The original initialization exception is retained if
  cleanup also fails.
- Teardown cancels collectors before monitors publish final state, removes handler callbacks,
  attempts all unregister/remove operations, clears references and interaction state, and puts the
  invisible touch window back at an offscreen, non-touchable `1×1` rectangle.
- Start/stop failures expose only the operation and exception class in Permissions diagnostics.
  `onInterrupt()` still interrupts accessibility feedback, not the binding.

The local timer and stopwatch data are not deleted just because accessibility disconnects. Their
session-owned alarms/audio/collectors are stopped and are re-established on a later connection.

### Source ownership and late callbacks

- Notification listener disconnect, destroy and reconnect use the same cleanup path. Only keys
  observed by that listener are removed; independently observed phone calls and screen-read clock
  entries survive notification access loss.
- Notification reconnect reads the current snapshot without replaying old message banners.
- Media monitoring ignores callbacks after stop, clears player/lyrics references and removes a
  destroyed controller before selecting another session.
- Phone, alarm, timer, torch and system-event callbacks do not republish after their monitor stops.
- Navigation teardown cancels delayed cleanup and clears screen snapshots immediately. Switching
  navigation apps does not leave the old app's snapshot behind.
- Paused notification-clock cleanup is keyed and cancellable. Repeated updates for the same pause
  do not create duplicate jobs or postpone its original deadline; removal/disconnect cancels it.
  Newly scheduled pauses use the current event-display duration setting.

Runtime validation also exposed existing compatibility errors in these consumer paths. Bluetooth
name/class reads now explicitly check the Android 12+ grant, and ranking visibility/person reads
are guarded by the platform versions exposing those APIs. No permission or lint check was disabled.

## One selection policy

`selectActivities` receives eligible snapshots and returns primary, secondary, expansion eligibility
and whether the selected source disappeared. It does not depend on composition or animation timing.

The existing default priority is preserved:

1. Incoming call
2. Short notice
3. Ringing local timer
4. Payment prompt
5. Ongoing call
6. Torch
7. Running local timer
8. Live activity/navigation
9. Local stopwatch
10. Notification/screen clock activity
11. Media
12. Download progress
13. Charging
14. Upcoming alarm
15. Idle, if none qualify

In particular, notices still precede ringing timers. Changing that product priority, or introducing
a starvation-prevention policy for bursts of notices, is a separate decision.

### Focus and identity

- `Act.id` remains the category/animation key. A separate `instanceKey` identifies the selected source.
- Notification-backed activities use package and notification key. Media follows player package
  across track changes. Upcoming alarms include package and trigger time. Timer, stopwatch and torch
  retain singleton keys used by existing local controls.
- A phone-state call remains the canonical source when enriched by a dialer notification, so that
  notification appearing/disappearing does not collapse an expanded phone call.
- Each category still contributes at most one representative. Newest notification `postTime` wins;
  equal timestamps use a stable instance-key tie break rather than map insertion order.
- Exact manual focus can retain an older, still-eligible source from the same category. It cannot
  elevate an ordinary activity over an incoming call or ringing local timer.
- A transient/urgent interruption suspends an unrelated expanded card rather than deleting its
  valid focus. It can resume afterward; a removed source cannot transfer expansion to a different
  source merely because both have the same category.
- Primary and bubble never duplicate a category/source. Existing bubble exclusions remain:
  incoming calls and transient cards have no bubble; upcoming alarms are not a bubble candidate.
- Calibration/width preview suppress unrelated real-activity bubbles and expansion.

Call reconciliation retains the pre-existing single-call presentation; it is not a redesign for
simultaneous cellular and VoIP calls. Media identity remains package-level because the current
model does not expose a session token. Local singleton identity is not persisted across processes.

## Automated checks

With JDK 17 and SDK 35 available:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug --no-daemon --console=plain
```

Tests:

- `ActivitySelectionTest`: 18 simultaneous/default-priority/focus/identity/bubble scenarios.
- `CallSelectionTest`: 3 call-source reconciliation and focus-continuity cases.
- `ServiceSessionTest`: 7 duplicate callback, rebind, rollback and cleanup-failure cases.
- `PausedClockTimeoutsTest`: 6 virtual-time deadline, cancellation, source-reuse and duration-setting cases.
- Phase-1 `IslandHealthTest`: 8 diagnostic-state cases.

These JVM tests do not prove Android callback ordering, visual smoothness or OEM background behavior.

## Device checks before release

1. Toggle accessibility off/on repeatedly, including while expanded or replying. Confirm one
   display/touch window, no old touch target, no playing timer audio after unbind, and current
   diagnostic flags. Exercise hide-in-landscape and schedule transitions afterward.
2. Play music and expand it; receive/dismiss a call. Incoming wins; valid music focus returns.
   Answer the call, expand its card, and revoke/regrant notification access: the surviving
   phone-state call must keep its canonical identity.
3. Expand navigation, then let the local timer ring. The timer must win over manual navigation
   focus. Dismiss the timer and confirm valid navigation focus resumes.
4. Show a transient notice over music/navigation. End or dismiss it and verify current activity
   data returns, not a remembered stale payload.
5. Start two downloads/live notifications, including equal timestamps if possible. Check newest
   default, exact selected source retention, and fallback/collapse when that source is removed.
6. Revoke notification access while a phone-state call or screen-read clock is active. Listener
   data must disappear without erasing the independently observed activity.
7. Pause a Clock notification timer, then remove it or revoke/regrant access before cleanup.
   An old cleanup job must not remove a newly reconstructed entry using the same key.
8. Switch navigation apps, close accessibility and reconnect. Old screen-derived routes must not
   return from cancelled delayed jobs or screenshot callbacks.
9. End a media session, switch players and revoke notification access. Confirm no old controller,
   artwork or lyrics status is presented after disconnect.
10. On Android 8/9/11 test real messages and calls; on Android 12+ deny Bluetooth. Unsupported
    APIs/optional permissions must not terminate the service. Repeat background/lock/unlock tests
    on the target OEM phone; JVM tests and emulator demos do not substitute for these checks.

No device installation, force-stop, permission toggle or destructive device action is performed
automatically by this change.
