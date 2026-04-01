# Fellowship 360 Gateway API

NestJS backend for the Fellowship 360 Gateway fork.

## Install

```bash
pnpm install
```

## Run

```bash
pnpm start:dev
pnpm build
pnpm start
```

## Test

```bash
pnpm test
pnpm test:e2e
pnpm test:cov
```

## Required environment

At minimum, this service expects:

- `MONGO_URI`
- `JWT_SECRET`
- `JWT_EXPIRATION`
- `FRONTEND_URL`
- `MAIL_HOST`
- `MAIL_PORT`
- `MAIL_USER`
- `MAIL_PASS`
- `MAIL_FROM`
- Firebase service-account variables used in `src/main.ts`

## Migration note

This backend is still structurally close to upstream TextBee. Before deeper feature work, decide whether this service remains a standalone public API or becomes an internal gateway layer under Fellowship 360 ownership.
