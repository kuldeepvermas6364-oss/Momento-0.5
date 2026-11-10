# Momento Remote Settings

The Android/KMP client observes Firebase Realtime Database at `/remoteSettings`.
The client is read-only. Change settings in Firebase Console or through a trusted
server/Admin SDK; never grant ordinary clients write access to this node.

## Initial Realtime Database JSON

```json
{
  "remoteSettings": {
    "maintenanceMode": false,
    "maintenanceMessage": "Momento is temporarily under maintenance. Please try again soon.",
    "announcement": "",
    "announcementEnabled": false,
    "theme": "system",
    "homeLayout": "list",
    "homeColumns": 2,
    "features": {
      "shorts": true,
      "subscriptions": true,
      "library": true,
      "announcements": true
    },
    "api": {},
    "updatedAt": 0
  }
}
```

## Realtime Database rules

Merge this block into the existing top-level `rules` object; do not replace
other application rules. Public read is limited to non-secret UI configuration.
Writes are denied to app clients. Use Firebase Console or a trusted Admin SDK
service to publish changes.

```json
"remoteSettings": {
  ".read": true,
  ".write": false
}
```

Never put API keys, private tokens, credentials, user data, or server secrets
inside this public configuration. Do not store security-critical authorization
decisions here: a client-side feature flag is not an access-control boundary.

## Supported fields

- `maintenanceMode`, `maintenanceMessage`
- `announcementEnabled`, `announcement`
- `theme`: `system`, `light`, or `dark`
- `homeLayout`: `list` or `grid`
- `homeColumns`: suggested grid column count
- `features`: UI feature visibility/availability flags
- `api`: non-secret endpoint URLs only
- `updatedAt`: optional timestamp

The repository exposes a live Flow to the app. A screen/feature must consume a
field before that field can change visible behavior. New native code, screens,
permissions, and dependencies still require a new app build.
