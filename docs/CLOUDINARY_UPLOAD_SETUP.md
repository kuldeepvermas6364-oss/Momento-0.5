# Momento 0.5 — Cloudinary upload setup

Cloudinary cloud name: `qrant8fw`

## What this adds
- A Cloudflare Worker that verifies a Firebase ID token before creating a Cloudinary upload signature.
- Signatures are scoped to the signed-in user's folder: `momento/<firebase-uid>`.
- The Cloudinary API secret stays in Cloudflare Worker secrets; it must never be added to the Android app or GitHub source.
- This endpoint signs image and video uploads only.

## 1. Create the Cloudflare Worker
Create a Worker named `momento-cloudinary-sign` and deploy the source in:
`cloudflare/worker/src/index.js`

Set these Worker secrets/variables in Cloudflare:
- `CLOUDINARY_CLOUD_NAME` = `qrant8fw` (Worker variable)
- `CLOUDINARY_API_KEY` = your Cloudinary API key (Worker secret)
- `CLOUDINARY_API_SECRET` = your Cloudinary API secret (Worker secret)
- `FIREBASE_WEB_API_KEY` = the Web API key from the Firebase project used by this Android app (Worker secret)

Do not put the API secret in GitHub, Android resources, BuildConfig, or chat.

## 2. Firebase
The Worker validates each Firebase ID token using Firebase Authentication. Email/Password and Google sign-in users can use the same endpoint once authenticated.

Before enabling metadata writes, add a `media/$uid` RTDB rule that allows only the matching authenticated user to read/write their own metadata. Do not use broad root-level authenticated writes.

## 3. Android integration status
The Worker is the secure signing component. The current upstream project is a YouTube client and does not yet contain a user-facing media picker/create-post flow. This Worker alone does not add an upload button. The Android uploader must call this endpoint with the current Firebase ID token, stream the selected file to Cloudinary, then save the returned `secure_url` and `public_id` in RTDB.

## 4. Smoke test
After deployment, send a POST request to the Worker with:
- Header: `Authorization: Bearer <Firebase-ID-token>`
- JSON body: `{"resourceType":"image"}`

A successful response contains a short-lived signature, timestamp, folder, Cloudinary API key, and cloud name. Never share the Firebase ID token or Cloudinary secrets.
