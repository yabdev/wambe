# Wambe web

Next.js App Router frontend for US-002. It consumes the frozen
`@wambe/api-client` generated from the approved Java API contract.

## Run locally

```powershell
Copy-Item .env.example .env.local
npm install
npm run dev
```

`NEXT_PUBLIC_DEMO_MODE=true` keeps local UI work self-contained by persisting drafts in
browser storage. Set it to `false` and configure Supabase plus the Java API URL to use
real authentication, RLS-backed events, signed uploads and media scanning.

## Verify

```powershell
npm run lint
npm run typecheck
npm test
npm run build
npx playwright install chromium
npm run test:e2e -- --project=chromium
```

The application includes Supabase SSR/PKCE auth, dashboard and draft resume, the
responsive four-step editor, debounced idempotent autosave, Google Maps pin selection,
signed media upload handling, privacy/review/publish, share and lifecycle management,
public Open Graph metadata, PWA metadata and accessibility-focused states.
This is a [Next.js](https://nextjs.org) project bootstrapped with [`create-next-app`](https://nextjs.org/docs/app/api-reference/cli/create-next-app).

## Getting Started

First, run the development server:

```bash
npm run dev
# or
yarn dev
# or
pnpm dev
# or
bun dev
```

Open [http://localhost:3000](http://localhost:3000) with your browser to see the result.

You can start editing the page by modifying `app/page.tsx`. The page auto-updates as you edit the file.

This project uses [`next/font`](https://nextjs.org/docs/app/building-your-application/optimizing/fonts) to automatically optimize and load [Geist](https://vercel.com/font), a new font family for Vercel.

## Learn More

To learn more about Next.js, take a look at the following resources:

- [Next.js Documentation](https://nextjs.org/docs) - learn about Next.js features and API.
- [Learn Next.js](https://nextjs.org/learn) - an interactive Next.js tutorial.

You can check out [the Next.js GitHub repository](https://github.com/vercel/next.js) - your feedback and contributions are welcome!

## Deploy on Vercel

The easiest way to deploy your Next.js app is to use the [Vercel Platform](https://vercel.com/new?utm_medium=default-template&filter=next.js&utm_source=create-next-app&utm_campaign=create-next-app-readme) from the creators of Next.js.

Check out our [Next.js deployment documentation](https://nextjs.org/docs/app/building-your-application/deploying) for more details.
