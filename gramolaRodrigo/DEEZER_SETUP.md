# Deezer setup (auth + preview)

This app uses Deezer previews for playback and can optionally connect your bar to Deezer via OAuth.

## 1) Create your Deezer app

- Go to https://developers.deezer.com/myapps and create an application
- Set:
  - Domain: 127.0.0.1 (for local dev), or your device IP/domain if headless
  - Redirect URL: http://127.0.0.1:8080/auth (or http://<device-ip>:8080/auth)
  - Terms of Use link: any valid URL of your choice
  - Description: at least 10 characters
- Note the App ID and Secret.

## 2) Configure the backend

Edit `gramolaRodrigo/src/main/resources/application.properties` and set:

```
deezer.app-id=YOUR_APP_ID
deezer.secret=YOUR_APP_SECRET
deezer.redirect-url=http://127.0.0.1:8080/auth
```

For a headless device, set the redirect to `http://<device-ip>:8080/auth` or a domain pointing to your device.

## 3) Sign in with Deezer

- Start the backend on port 8080.
- From a browser, open:
  - `http://localhost:8080/deezer/login?barId=<yourBarId>`
  - Approve Deezer permissions and accept the redirect to `/auth`.
  - The backend stores the access token in memory for the `barId` you passed.
- (Optional) Verify:
  - `http://localhost:8080/deezer/me?barId=<yourBarId>` returns user info from Deezer.

Notes
- Tokens are stored in-memory for demo/exam purposes. Persist them if you need durability.
- For playback of 30s previews, no OAuth is required; the app streams via `GET /api/deezer/preview/{id}`.
