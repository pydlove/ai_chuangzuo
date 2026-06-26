# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

This is **爱创作** ("AI Creation"), a pure frontend HTML/CSS/JS prototype for an AI-powered social-media article generator. There is no build system, no backend, and no package manager. The prototype simulates a multi-page app with PC and mobile mockups side by side.

## Tech stack

The official technology stack is documented in `docs/architecture/tech-stack.md`. Database table conventions (naming, field standards, user-side vs admin-side split, Flyway migration rules) are documented in `docs/architecture/mysql-table-conventions.md`. Java package conventions (module split, package layout, naming rules) are documented in `docs/architecture/java-package-conventions.md`. API interface conventions (URL design, response format, error codes, authentication, idempotency) are documented in `docs/architecture/api-interface-conventions.md`. Exception and error code conventions (exception hierarchy, global handler, validation errors) are documented in `docs/architecture/exception-errorcode-conventions.md`. Key decisions:

- **Backend**: Spring Boot + Spring Security + MyBatis-Plus, JDK 17.
- **Database**: MySQL 8.x with Flyway for migrations.
- **No external middleware**: No Redis, no standalone message queue. Use Caffeine for in-memory caching, MySQL task tables + Spring Scheduler for async queues, JWT for stateless auth, and local-disk storage for files.
- **Frontend**: Vue 3 + Pinia + Vue Router 4 + Ant Design Vue, built with Vite.

Before introducing any additional middleware (Redis, RabbitMQ, Elasticsearch, MongoDB, OSS/MinIO, etc.), confirm resource constraints and update the architecture doc.

## Running the prototype

Start the local preview server:

```bash
./scripts/local/start.sh
```

This runs `python3 -m http.server` on port `28585` from the repository root. Common entry points:

- Home: http://localhost:28585/.superpowers/brainstorm/6491-1782131242/content/index.html
- Create: http://localhost:28585/.superpowers/brainstorm/6491-1782131242/content/create.html
- Preview/export: http://localhost:28585/.superpowers/brainstorm/6491-1782131242/content/preview.html

There is no separate build, lint, or test command. Changes to HTML/CSS/JS are reflected immediately after a browser refresh.

## Verification

The project uses ad-hoc Playwright scripts for functional checks. Examples in the repo root:

- `tests/e2e/verify_template.py` — checks the template library modal renders and counts templates correctly.
- `capture_ref.py` / `capture_ref2.py` — capture reference screenshots from the production site.

To run a verification script, first start the prototype server (it must be on the URL the script expects, e.g. `http://localhost:8080` for the legacy scripts), then:

```bash
python3 tests/e2e/verify_template.py
```

When adding a feature, follow the existing pattern: write a short Playwright script, run it, and inspect screenshots.

## Repository layout

- **Working prototype pages**: `.superpowers/brainstorm/6491-1782131242/content/`
  - 11 standalone HTML pages: `index.html`, `login.html`, `create.html`, `loading.html`, `preview.html`, `works.html`, `pricing.html`, `settings.html`, `order.html`, `payment.html`, `forgot.html`.
  - `shared.css` and `shared.js` are loaded by every page.
- **Legacy monolithic prototype**: `.superpowers/brainstorm/6491-1782131242/content/full-prototype-v20-legacy.html` — the original single-file version that was split into the standalone pages. Do not edit it; it is archived for reference.
- **Design docs**: `docs/superpowers/specs/` (requirements) and `docs/superpowers/plans/` (implementation plans, often with per-task verification steps).
- **Architecture docs**: `docs/architecture/tech-stack.md`, `docs/architecture/mysql-table-conventions.md`, `docs/architecture/java-package-conventions.md`, and `docs/architecture/api-interface-conventions.md`.
- **Progress ledger**: `.superpowers/sdd/progress.md` tracks completed subagent-driven development tasks and review outcomes.
- **Directory conventions**: `docs/project-structure-convention.md` defines the top-level directory layout (e.g., `scripts/`, `project/`, `config/`, `docs/`, `tests/`, `data/`, `logs/`). Follow it when adding new code, scripts, or documentation.

## Code architecture

### Page structure

Each HTML page follows the same prototype frame:

1. A top `.prototype-nav` bar links to the other pages.
2. A single `.prototype-screen` contains one or more `.mockup` blocks.
3. Each `.mockup` typically renders both a PC and a mobile view of the same feature using inline styles and `shared.css` classes.

Navigation between pages is plain `<a>`/`<button onclick="location.href='...'">`; there is no router.

### Shared JavaScript (`shared.js`)

`shared.js` is a single global script. The major functional areas are:

- **Generation queue** (`submitGenerationTask`, `processGenerationQueue`, `startQueueConsumer`, `renderGenerationQueue`, etc.): simulates an async article-generation backend using `localStorage` and `setInterval`. Tasks move through `queued → generating → completed`.
- **Template library** (`openTemplateLibrary`, `templatePresets`, `getTemplateStyles`, `applyTemplateFeedback`): modal for selecting one of 30 export templates grouped by platform (`wechat`, `xiaohongshu`, `toutiao`, `baijiahao`, `zhihu`, `douyin`, `general`).
- **Style library** (`openStyleLibrary`, `userStylePresets`, `renderStyleEditor`, `applyStyleFeedback`): modal for picking or editing writing-style presets.
- **Word count modal** (`openWordCountModal`, `wordCountPresets`, `currentWordCount`, `currentWordLabel`): four-tab popup for choosing article length by platform, scenario, tier, or custom value (1–3000).
- **Title optimizer** (`setupTitleOptimizeTriggers`, `openTitleOptimize`, `renderAITitles`, `renderPlatformTitles`): hover-to-edit AI title suggestions on the preview page.
- **Export / cards** (`exportWord`, `copyArticleText`, `generateCards`, `renderCardToCanvas`, `downloadCanvas`): client-side Word export, text copy, and canvas-based card generation.

### State management

- Global variables in `shared.js` hold ephemeral UI state (e.g. `currentWordCount`, `isLoggedIn`).
- The generation queue is persisted in `localStorage` under the key `aichuangzuo_generation_queue`.
- User-created styles live only in memory; refreshing the page resets them.

### Styling

- `shared.css` contains reusable component classes (`.prototype-frame`, `.prototype-nav`, `.nav-cta`, `.feature-card`, auth tabs, etc.).
- Most layout and spacing is inline `style="..."` in the HTML.
- Primary brand color is `#07c160` (green).

## How to make changes

1. Identify whether the feature lives in `shared.js`/`shared.css` (cross-page) or in a specific page HTML.
2. Check `docs/superpowers/specs/` and `docs/superpowers/plans/` for the relevant design or implementation plan. Plans often include exact insertion points and verification commands.
3. Edit the standalone page files, not the legacy `full-prototype-v20-legacy.html`.
4. Verify manually in the browser and/or with a short Playwright script.
5. Update `.superpowers/sdd/progress.md` if the change completes a tracked task.

## Important conventions

- Keep the prototype frontend-only; do not add a backend or build step without explicit user approval.
- When adding new templates, styles, or presets, keep keys consistent with the existing naming (`wechat`, `xiaohongshu`, `toutiao`, `baijiahao`, `zhihu`, `douyin`, `general`).
- The split standalone pages are the source of truth; the legacy single-file prototype is read-only.
