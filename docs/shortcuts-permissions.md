# Home Screen Shortcut Permission

Notes from testing Home screen shortcut creation on different Android launchers.

## What matters

Creating a Home shortcut is an optimistic handoff to the launcher.

When the app calls `requestPinShortcut`, Android returns a boolean:

- `true` means the launcher supports shortcut pinning and accepted the request for handling.
- `false` means the launcher rejected the request immediately.

`true` does **not** mean the shortcut was added. The launcher may still show a confirmation sheet,
the user may cancel it, or an OEM shortcut permission may block the shortcut. Android does not give
the app a reliable cancel or deny result.

Because of this, the app closes the editor when `requestPinShortcut` returns `true` and shows a
neutral message: "Shortcut request sent. Confirm it in your launcher." If `requestPinShortcut`
returns `false`, the editor stays open and shows "Unable to create shortcut."

## What the permission is

This is not a normal Android runtime permission. It is controlled by the device manufacturer or
launcher.

- The app cannot grant it from code.
- The app cannot force the launcher prompt to appear.
- Tested devices only expose **Allow** or **Deny**.

## Device behavior seen in testing

### Samsung Galaxy A14

- When Home screen shortcuts are allowed, Samsung shows an "Add to Home screen?" bottom sheet every time.
- The app closes the editor after handing the request to Samsung Launcher.
- Confirming or canceling happens in Samsung's bottom sheet, outside the app.

### Xiaomi / MIUI

- The first request may show a MIUI permission sheet.
- Once allowed, MIUI can remember the choice and add later shortcuts without asking again.
- When denied, the request can fail silently even though Android reported that the launcher accepted
  the request.

## What the user sees

- **Launcher accepts the request:** the editor closes and the app shows the neutral launcher
  confirmation message.
- **Launcher rejects immediately:** this should not happen in normal navigation because unsupported
  launchers cannot open the Home shortcut editor. If it happens from a stale/restored screen, the
  editor stays open and the app shows an error.
- **User confirms in the launcher:** the shortcuts list refreshes when the app resumes.
- **User cancels or the OEM blocks the shortcut:** no shortcut appears, and the app may not receive a
  specific failure reason.

## Re-testing

Uninstalling the app is not required, but the setting location varies by manufacturer and launcher.
Some devices expose a Home screen shortcut permission in Settings; others do not show a separate
app-level toggle.

On Samsung Galaxy A14, no separate Home Assistant shortcut permission was found in app settings
during testing. Samsung still shows its own bottom sheet for each request when shortcut creation is
allowed.

For MIUI lab testing, the remembered prompt decision can be reset without clearing Home Assistant
data:

```bash
adb shell pm clear com.lbe.security.miui
```
