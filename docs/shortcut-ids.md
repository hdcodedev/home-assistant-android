# Shortcut IDs — Legacy (v1) vs v2

How a shortcut's ID is created and stored.

In the legacy version, home shortcuts had no data layer — you typed the ID by hand into a free-text field, and whatever you entered was saved straight to the system. That made IDs unpredictable and easy to break. In v2, IDs are handled by a data source, so home shortcuts get a stable, auto-generated ID.

| Shortcut type | Legacy (v1) | v2 |
|---|---|---|
| App shortcut | `shortcut_1` … `shortcut_5` (fixed by position) | `shortcut_1` … `shortcut_5` (fixed by position) |
| Home shortcut | Free text you type, e.g. `my_lights` | Auto-generated `home_shortcut_<uuid>`, e.g. `home_shortcut_3f8a9b2c-1e7d-4b2a-9c10-2d4f6e8a0b11` |
