# Momento 0.5 — Cloudinary upload setup

Cloudinary cloud name: `qrant8fw`

## What this adds
- A Cloudflare Worker that verifies a Firebase ID token before creating a Cloudinary upload signature.
- Signatures are scoped to the signed-in user's folder: `momento/<firebase-uid>`.
- The Cloudinary API secret stays in Cloudflare Worker secrets; it must never be added to the Android app or GitHub source.
- This endpoint signs image and video uploads only.

## 1. Create the Cloudflare Worker
Worker `momento-cloudinary-sign` has already been deployed to your connected Cloudflare account.

**Worker URL:** `https://momento-cloudinary-sign.kuldeepvermas6364.workers.dev`

Source is maintained in `cloudflare/worker/src/index.js`.

Set these Worker secrets/variables in Cloudflare:
- `CLOUDINARY_CLOUD_NAME` = `qrant8fw` (Worker variable)
- `CLOUDINARY_API_KEY` = your Cloudinary API key (Worker secret)
- `CLOUDINARY_API_SECRET` = your Cloudinary API secret (Worker secret)
- `FIREBASE_WEB_API_KEY` = the Web API key from the Firebase project used by this Android app (Worker secret)

Do not put the API secret in GitHub, Android resources, BuildConfig, or chat.

## 2. Firebase
The Worker validates each Firebase ID token using Firebase Authentication. Email/Password and Google sign-in users can use the same endpoint once authenticated.

Before enabling metadata writes, add this rule inside the root `rules` object (merge it into your existing rules; do not replace unrelated rules):

```json
"media": {
  "$uid": {
    ".read": "auth != null && auth.uid === $uid",
    ".write": "auth != null && auth.uid === $uid"
  }
}
```

Do not use broad root-level authenticated writes.

## 3. Android integration status
The Android-side `CloudinaryUploadClient` has been added at `composeApp/src/androidMain/kotlin/org/company/app/data/upload/CloudinaryUploadClient.kt`. It requests a signature using the current Firebase ID token, streams the selected Android content URI to Cloudinary, and saves the returned `secureUrl`, `publicId`, resource type, format, byte count, and timestamp under `media/<uid>` in RTDB. The caller must pass the deployed Worker URL, the signed-in user's UID and fresh Firebase ID token, plus `image` or `video`.

The repository is currently a YouTube browsing client and does not yet contain a user-facing media picker/create-post flow. The upload pipeline is now available to call, but a visible upload button and post/story/short creation flow still need to be connected to it.

## 4. Smoke test
After deployment, send a POST request to the Worker with:
- Header: `Authorization: Bearer <Firebase-ID-token>`
- JSON body: `{"resourceType":"image"}`

A successful response contains a short-lived signature, timestamp, folder, Cloudinary API key, and cloud name. Never share the Firebase ID token or Cloudinary secrets.
