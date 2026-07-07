# Task 5 Report: 更新路由 src/router/index.js

## Status: DONE

## What was implemented

Replaced the existing `project/admin/web/src/router/index.js` with the new nested-route structure specified in the brief:

- `/login` → LoginView (unchanged, name `AdminLogin`)
- `/console` → AdminLayout wrapper with `meta.requiresAuth = true`, redirect to `/console/users`, and child `users` route → UserListView (name `AdminUserList`)
- `/` → `/console`
- `beforeEach` guard preserves the unauthenticated-redirect-to-`/login` logic, and the logged-in-redirect target was updated from `/console` to `/console/users` per the brief
- Formatting (single quotes, no semicolons, 2-space indent) matches the brief verbatim
- `useUserStore` import unchanged
- `ConsoleView.vue` left on disk as instructed (route no longer references it)

## What was tested

1. **Build:** `cd project/admin/web && npm run build` — succeeded in 2.42s, generated `AdminLayout-*.js/css` and `UserListView-*.js/css` chunks as expected.
2. **Dev server:** `cd project/admin/web && npm run dev` — Vite started on port 22346 in 143ms with no errors.
3. **Route fetch check:** `curl http://localhost:22346/` returned 200 with the app shell HTML. The served `/src/router/index.js` shows the new nested `children: [{ path: 'users', name: 'AdminUserList', ... }]` shape, confirming the Vite transform is using the new file.
4. **Cleanup:** Dev server stopped, port 22346 freed.

## Files changed

- `project/admin/web/src/router/index.js` — replaced (1 file, +11 / -4 lines)
- Commit: `0701728` on branch `feature/admin-login`

## Self-review

- [x] `/login` route preserved, name `AdminLogin`
- [x] `/console` route now uses AdminLayout, requiresAuth true, redirects to `/console/users`
- [x] `/console/users` child route renders UserListView, name `AdminUserList`
- [x] `/` → `/console`
- [x] `beforeEach` guard preserves both branches
- [x] Logged-in redirect target is `/console/users` (per brief, was `/console` before)
- [x] `npm run build` succeeded
- [x] Dev server started on 22346 with no errors
- [x] Committed

## Issues / concerns

None. Minor note: the `gitStatus` snapshot at conversation start showed branch `feature/publish-platform-seo`, but the actual current branch (and where prior Tasks 3 and 4 were committed) is `feature/admin-login`. Commit landed on the correct branch. No action required.
