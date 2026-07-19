# Android Shortcuts — Use Cases

Shortcuts let you jump straight to a specific dashboard or device control from your home screen, without opening the app first. You manage them from **Settings > Companion App > Manage Shortcuts**.

## Shortcut types

| Type | How it appears | Limit | How to create |
|---|---|---|---|
| **App shortcut** | Long-press the Home Assistant app icon to reveal the shortcut menu | Up to 5 (most launchers show 4) | Add it in Manage Shortcuts, then long-press the app icon to access the shortcut menu |
| **Home shortcut** | A standalone icon on your home screen | No limit | Add it in Manage Shortcuts and tap **Add to Home screen**, or drag it manually |

## What you can shortcut to

| Destination | What it does | When to use it |
|---|---|---|
| **Dashboard** | Opens a specific dashboard or view inside the app | Access a frequently used dashboard instantly (e.g., the house layout, a security camera view) |
| **Entity / Device** | Opens the more-info or control screen for a single entity | Toggle a light, check a sensor, or adjust a thermostat with one tap |

> **Note:** Entity shortcuts require a Home Assistant server version of 2025.6.0 or later. If your server is older, only dashboard shortcuts are available.

## Shortcut limits

| Category | Slots available |
|---|---|
| App shortcuts | 5 total (most launchers display 4) |
| Home shortcuts | Unlimited |

When all app shortcut slots are occupied, Manage Shortcuts disables the App shortcut create option. If the slot state changes while an editor is already open, saving the new App shortcut fails and the editor remains open.

## Managing shortcuts

| Action | App shortcuts | Home shortcuts |
|---|---|---|
| Create | Supported | Supported |
| Update | Supported | Supported |
| Disable | Not needed | Supported |
| Delete | Supported | Not supported in-app — remove the icon manually from your home screen |

## Requirements

| Feature | Minimum Android version |
|---|---|
| App shortcuts | Android 7.1 |
| Home shortcuts | Android 8.0 |
