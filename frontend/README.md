# DESS frontend (stretch goal)

React + TypeScript + Vite, Material UI (Community), TanStack Query, React Router, React Hook Form + Zod.
The REST API is the MVP acceptance surface: build screens only when the core workflow is stable.

```bash
npm install
npm run dev        # http://localhost:5173, proxies /api to the local backend
npm run build
```

Layout: `src/app` (router, providers), `src/api` (HTTP client and hooks), `src/components`,
`src/features/{institution,academic,scheduling,assessment}`.

Calendar: start with a simple custom weekly grid. FullCalendar Standard (MIT) is an alternative;
its resource timeline views are Premium, so do not make them an MVP dependency.
