# Phase 1: permissions and service diagnostics

## Rules and audit findings

- Accessibility **granted**, the accessibility service **connected**, overlay windows **attached**,
  and the in-app island switch are different states. Home and Permissions use the same health rule.
  A permitted but disconnected service is not labelled “starting” indefinitely.
- Notification access is checked for `NotifListener`, not any listener in this package.
  Android 8.0 uses the enabled-component setting; Android 8.1+ uses `NotificationManager`.
  Actual connection is reported only from listener callbacks.
- Both the drawing and touch windows must be created. Attachment errors preserve only the operation
  and exception class, not exception messages or screen content.
- Accessibility reconnection does not register the same monitors and collectors twice.
  Unbind/destroy clears the connected flag. `onInterrupt()` is **not** a disconnect signal: Android
  uses it to interrupt accessibility feedback, so it must not mark a still-bound service disconnected.
- Keep-alive requested, foreground startup confirmed, and foreground startup failed are distinct.
  Sticky restarts re-check whether accessibility is enabled. Its notification describes keep-alive,
  not proof that the island is drawing.
- Call permission details distinguish phone state, caller number, contact name, and answer/decline.
  All four permissions have consumers: `Controls` uses `TelecomManager` as a fallback when call
  notification actions are unavailable. Non-telephony devices show N/A rather than missing calls.
- Exact-alarm access reflects `AlarmManager.canScheduleExactAlarms()`, matching the timer's existing
  exact/inexact fallback. Battery optimization exemption is an Android API result, **not** proof of
  unrestricted OEM background execution.
- Own-app notification posting is separate from notification-listener access and FGS execution.
  No new notification permission is requested in this phase. Channel-specific notification blocking
  is not inferred from the app-level check.
- OEM autostart, recent-app locking and restricted settings remain explicit manual checks.

## Where to use it

Home → Permissions → Service diagnostics. System permission checks refresh when the page resumes
and after runtime permission requests; service callbacks update the page while it stays open.
“Refresh” re-reads permissions without changing settings or restarting services.
“Copy diagnostics” reads current state on click. The report contains app version, Android API level,
manufacturer, permission booleans, user settings and process-local service state. It includes no
notification text, contact/phone data, device identifiers or stack traces. Nothing is uploaded.

READY means connected with both windows attached and no configured hiding reason, not a visual
pixel/rendering guarantee. Connection flags reset with the process and are not historical uptime.
Transient screenshot hiding and OS-level overlay suppression are not diagnosed in this phase.

## Automated validation

With JDK 17 and Android SDK 35 configured:

```sh
./gradlew testDebugUnitTest assembleDebug lintDebug --no-daemon
```

`IslandHealthTest` covers permission-only startup, stale connected state after revoked access,
partial overlay attachment, paused/landscape/schedule states, state precedence, fresh-process
defaults and independence from optional notification/keep-alive services.

## Device validation checklist

Use Android 8.0/8.1, 12/12L, 13+ and at least one OEM battery-management implementation:

1. Fresh install: accessibility and notification connection must both be No. Refresh/copy must not
   request permissions or enable any service.
2. Enable accessibility, return to Permissions: connected and both windows should become Yes.
   Revoke it and return: connected must clear. Repeat off/on to check no duplicate overlays.
3. Leave access enabled after a forced process stop or app update: if Android has not rebound the
   service, show “not connected,” not “starting.” Toggle accessibility to recover.
4. Toggle the Home island switch; use landscape and an out-of-schedule time. Home and diagnostic
   text must agree with the reason the island is hidden.
5. Grant/revoke notification access separately. Connected must track callbacks, independently of
   the accessibility and app-notification switches. Disconnect must stop media monitoring.
6. Grant calls partially: the caption must name the missing capabilities. On a Wi-Fi-only tablet,
   calls must show N/A and must not launch a permission request.
7. On Android 12/12L deny exact alarms: show No and inexact-fallback guidance; grant and return:
   show Yes. On older Android show supported; on Android 13+ follow the actual platform grant.
8. Enable/disable keep-alive: verify the running flag follows foreground startup/destroy, not the
   preference. If Android refuses startup, show the operation/exception class until a successful
   start or the option is disabled.
9. Block the app's own notifications: distinguish that from notification-listener access and
   keep-alive execution. Change battery exemption and verify it updates on return.
10. Copy diagnostics after a system-setting change. Verify current grants, app version and service
    state, and absence of notification content, phone numbers, contacts and device IDs.
11. Check small screens, landscape, largest system font, TalkBack and both app languages. The
    diagnostic actions must remain readable and reachable without depending on badge colors.

## Not included

Automatic restart/rebind loops, OEM-specific permission probing, a service-lifecycle redesign,
permission onboarding redesign, timer scheduling changes and full log collection remain follow-ups.
