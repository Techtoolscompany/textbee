# Fellowship 360 Gateway

Fork of TextBee, being adapted into a first-party church-facing SMS gateway for Fellowship 360.

## Current intent

This repository is no longer being treated as a generic hosted SMS product. The target shape is:

- a branded Android gateway app that churches install once
- a Fellowship 360-controlled web/admin surface
- a backend that can be coupled cleanly to Fellowship 360's super-admin and org-scoped device model

The fork is still early. Package IDs, signing, domain, and API compatibility have not been fully migrated yet.

## Repo structure

- `android/`
  Android gateway app written in Java/Gradle
- `api/`
  NestJS backend using MongoDB and Firebase Admin
- `web/`
  Next.js dashboard/landing app using Prisma + NextAuth

## Local build surface

### Android

Use Android Studio or Gradle directly:

```bash
cd android
./gradlew assembleDebug
```

What it needs:

- Android SDK / build tools
- Java toolchain compatible with the Gradle project
- Firebase config in `android/app/google-services.json`

Important: `applicationId` and Java package names are still on the upstream `com.vernu.sms` path and should not be changed casually. That is a deliberate migration step because it affects signing, upgrades, and Play Store identity.

### API

```bash
cd api
pnpm install
pnpm start:dev
pnpm build
pnpm test
```

Primary env requirements inferred from source:

- `MONGO_URI`
- `JWT_SECRET`
- `JWT_EXPIRATION`
- `FRONTEND_URL`
- `MAIL_HOST`
- `MAIL_PORT`
- `MAIL_USER`
- `MAIL_PASS`
- `MAIL_FROM`
- `MAIL_REPLY_TO`
- Firebase service-account vars used in `src/main.ts`

### Web

```bash
cd web
pnpm install
pnpm dev
pnpm build
```

Primary env requirements inferred from source:

- `NEXTAUTH_SECRET`
- `NEXT_PUBLIC_API_BASE_URL`
- `NEXT_PUBLIC_GOOGLE_CLIENT_ID`
- Prisma database env required by the Prisma schema/runtime
- mail/admin notification vars used by route handlers

## Immediate migration priorities

1. Lock the permanent Fellowship 360 gateway identity:
   - package name
   - app name
   - signing key
   - deployment domain(s)
2. Decide whether the forked backend stays standalone or becomes a compatibility layer in front of Fellowship 360's internal SMS gateway routes.
3. Remove direct TextBee branding and hosted URLs from Android, API, and web.
4. Align device registration, assignment, inbound SMS, and delivery-state flows with Fellowship 360's org model.

## Current constraint

This fork should be treated as a sibling product workspace inside the Fellowship 360 repo while it is being audited and reworked. Avoid assuming the upstream TextBee hosted service or domains remain authoritative.
