# Fellowship 360 Gateway Plan

## Goal

Turn this fork into the first-party SMS gateway app churches install from day one, while Fellowship 360 remains the operator control plane.

## Buildable surfaces

### Android app

- Build system: Gradle / Android application
- Current identity:
  - `applicationId`: `com.vernu.sms`
  - visible name: now being shifted to Fellowship 360 Gateway
- Key risk:
  package name + signing key are upgrade-critical; changing them late forces reinstall/re-enrollment

### API

- Build system: NestJS + TypeScript
- Data/service dependencies:
  - MongoDB
  - JWT
  - SMTP
  - Firebase Admin
- Key risk:
  current backend assumes upstream TextBee account/device semantics, not Fellowship 360 org/device ownership

### Web

- Build system: Next.js + Prisma + NextAuth
- Key risk:
  the web app still contains hosted-product marketing and TextBee domain assumptions

## Rebrand plan

### Phase 1: safe surface changes

- Rename repo docs and obvious app-facing strings
- Rename package names/descriptions where they do not affect runtime identity
- Replace upstream-only docs with Fellowship 360 fork intent

### Phase 2: identity lock

- Choose final Android package name
- Choose final signing strategy
- Choose final gateway domain(s)
- Replace Firebase project and mobile config

### Phase 3: backend ownership

- Decide whether this API remains standalone or becomes a compatibility facade
- Map device registration and API key flow to Fellowship 360 super-admin ownership
- Remove assumptions that a church manages itself inside the gateway product

### Phase 4: control-plane convergence

- Fellowship 360 super-admin becomes the source of truth
- Gateway app becomes a device/client
- Device assignment, heartbeat, deletion, and message logs align to Fellowship 360 org IDs

## Critical risks

1. Android package rename
   This affects install/update continuity.

2. Signing-key continuity
   If the first shipped signing key changes later, churches cannot update cleanly.

3. Backend duplication
   Running both this backend's device model and Fellowship 360's device model without a clear authority will create drift.

4. Domain coupling
   The web and API currently reference `textbee.dev` flows and should not be re-pointed until final domains are chosen.

## Recommended next critical-path step

Define the permanent app identity before deeper implementation:

- final Android package name
- final app display name
- final signing owner/process
- final public download URL/domain
- whether this fork's API stays public-facing or becomes internal-only
