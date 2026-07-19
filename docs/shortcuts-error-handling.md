# Shortcuts Error Handling

Shortcut errors are shown in two ways, depending on whether the app can still keep the current screen open.

| Category | Shown as | When |
|---|---|---|
| Screen error | Full-screen error state | The shortcuts list or editor cannot be opened |
| Operation error | Snackbar at the top of the screen | Something failed while the user was creating, updating, deleting, disabling, or loading entity suggestions |

The same error can be shown in different ways. For example, `ShortcutNotFound` is a full-screen
error when the app cannot open a shortcut for editing. It is a Snackbar when the edit screen is
already open, but the shortcut is removed before the user saves or deletes it.

## Error Reference

| Error | Meaning | Typical cases |
|---|---|---|
| `AndroidVersionNotSupported` | The device's Android version does not support shortcuts. | The device is running a version older than Android 7.1. |
| `NoServersConfigured` | No Home Assistant servers are configured, so a shortcut destination cannot be resolved. | The last server was removed before the list loaded or while an editor was open. |
| `AppShortcutSlotsFull` | All app shortcut slots are already in use. | The user tries to create an app shortcut when there is no free slot left. |
| `ShortcutNotFound` | The shortcut no longer exists. | The user opens the shortcut list or edit screen, removes the shortcut from the phone or launcher outside this app, then tries to open, save, or delete that shortcut in the app without refreshing. |
| `HomeShortcutPinningNotSupported` | The current launcher does not support adding Home screen shortcuts. | The user tries to create or update a Home shortcut on a launcher that does not support pinning shortcuts. Existing shortcuts may still be viewable or disableable from an already-open edit screen. |
| `Unknown` | Something failed, but the app does not have a more specific reason. | Android rejects the shortcut change, shortcut details cannot be read, the launcher does not confirm a pin request, or the shortcut destination is invalid. The technical error is logged when available. |

## Screen Errors

These errors replace the screen content because the list or editor cannot be used. The table shows where each error can normally appear.

| Error | Shortcuts List | Create App | Create Home | Edit App | Edit Home |
|---|---|---|---|---|---|
| `AndroidVersionNotSupported` | ✓ | ✓ | ✓ | ✓ | ✓ |
| `NoServersConfigured` | ✓ | ✓ | ✓ | ✓ | ✓ |
| `ShortcutNotFound` | | | | ✓ | ✓ |
| `Unknown` | ✓ | ✓ | ✓ | ✓ | ✓ |

If the user opens the shortcut list, removes a shortcut from the phone or launcher outside this app, and then taps the old list item without refreshing, the edit screen shows `ShortcutNotFound`. The same applies if the user is already on the edit screen and the shortcut is removed before they save changes. Invalid saved links to shortcut screens are also treated as `ShortcutNotFound`.

Create App does not check for a free slot before opening the editor. The list disables Create App when it knows all app shortcut slots are occupied, but an already-open editor can still stay open. If no slot is available when the shortcut is saved, the app shows a Snackbar instead.

Full-screen editor errors use dedicated copy for:

| Error | Title/body |
|---|---|
| `AndroidVersionNotSupported` | Unsupported Android-version message |
| `NoServersConfigured` | No-server setup message |
| `ShortcutNotFound` | Shortcut-not-found message |
| `Unknown` and errors that normally happen after the screen is already open | Generic shortcut-error message |

## Entity Suggestions

The editor loads the server list separately from entity suggestions. If entity suggestions cannot be loaded, the editor stays open and the user sees an "Unable to load entities" message. Dashboard shortcuts remain fully editable because they do not need entity suggestions.

## Operation Errors

All operation failures are shown as a Snackbar and leave the editor open. Most errors use a generic message, such as "Unable to create shortcut", but errors the user can act on use a more specific message.

| Error | Snackbar message |
|---|---|
| `AppShortcutSlotsFull` | All app shortcut slots are in use |
| `ShortcutNotFound` | Shortcut no longer exists |
| `HomeShortcutPinningNotSupported` | Home shortcut pinning is not supported |
| Other operation errors | Generic create, update, delete, or disable failure |
| Entity catalog load failure | Unable to load entities |

The table below shows which errors can happen for each user action.

| Error | Create App | Create Home | Update App | Update Home | Delete App | Disable Home |
|---|---|---|---|---|---|---|
| `AndroidVersionNotSupported` | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| `AppShortcutSlotsFull` | ✓ | | | | | |
| `ShortcutNotFound` | | | ✓ | ✓ | | |
| `HomeShortcutPinningNotSupported` | | ✓ | | ✓ | | |
| `Unknown` | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |

`NoServersConfigured` happens while opening the screen, not while saving. The editor needs a server before the user can submit changes.

Delete App and Disable Home do not currently check whether the shortcut still exists before removing it. They can still return `Unknown` if Android rejects the request.

In normal editor use, the form prevents invalid shortcut destinations from being saved. The save logic checks them again, so bad or outdated data is rejected even if it somehow reaches the save action.

## Home Shortcuts When Pinning Is Unsupported

- **List:** Home shortcuts are not loaded or displayed, and the Create Home option is disabled. The normal list flow therefore cannot open a Home editor.
- **Already-open edit screen:** If an Edit Home screen is already open or restored, the shortcut can still be loaded and viewed because viewing does not require pinning support.
- **Update:** Updating from an already-open edit screen fails with a Snackbar because updating requires pinning support.
- **Disable:** Disabling from an already-open edit screen still works because disabling does not require pinning support.
