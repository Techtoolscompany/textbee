# Fellowship 360 Gateway Web

Next.js web app for the Fellowship 360 Gateway fork.

## Install

```bash
pnpm install
```

## Run

```bash
pnpm dev
pnpm build
pnpm start
```

## Primary environment

- `NEXTAUTH_SECRET`
- `NEXT_PUBLIC_API_BASE_URL`
- `NEXT_PUBLIC_GOOGLE_CLIENT_ID`
- Prisma database env required by the Prisma schema
- mail/admin notification env used by route handlers

## Migration note

This web app still contains upstream TextBee hosted-product assumptions and should be treated as a rebrand/migration surface, not a finished Fellowship 360 operator console.
