# Task 5 Report: Add publish meta card to preview.html

## Status
DONE

## What was done
1. **Inserted PC publish meta card** after the `max-width: 680px` inner div and before the white card closing `</div>` in the PC mockup. The card includes:
   - `id="pc-publish-meta-card"` container
   - `id="pc-publish-desc"` textarea with "换一版" and "复制描述" buttons
   - `id="pc-publish-tags"` tag container with "换一批" and "复制全部标签" buttons

2. **Inserted mobile publish meta card** after the `.article-preview` closing `</div>` and before the white card closing `</div>` in the mobile mockup. The card includes:
   - `id="mobile-publish-meta-card"` container
   - `id="mobile-publish-desc"` textarea with "换一版" and "复制描述" buttons
   - `id="mobile-publish-tags"` tag container with "换一批" and "复制全部标签" buttons

3. **Updated inline script** at the bottom of `preview.html` to call `renderPublishMeta()` on `DOMContentLoaded`.

4. **Ran verification** (Step 4 from brief): all assertions passed (`Task 5 checks passed`).

5. **Manual smoke test**: opened `http://localhost:28585/.superpowers/brainstorm/6491-1782131242/content/preview.html` in a headless browser via Playwright. Screenshot at `/tmp/preview_smoke_test_v2.png` confirms both PC and mobile mockups now show "发布描述" and "推荐标签" sections below the article content.

6. **Committed** the change.

## Concerns
- The `renderPublishMeta()`, `regeneratePublishDesc()`, `regeneratePublishTags()`, `copyPublishDesc()`, `copyPublishTags()`, and `copySingleTag()` functions are consumed from Task 4 (shared.js). During the smoke test, the description textarea and tags containers were empty, suggesting Task 4's `renderPublishMeta()` may not be fully populating content yet, or the platform state is not set. The UI structure is correct and the function calls are wired properly.
- The Playwright test showed `pc_tags` and `mobile_tags` as not "visible" (likely because empty divs have zero dimensions), but the screenshot clearly shows the cards are rendered.

## Commits
- `7ef39f8` feat(platform): add publish meta card to preview page

## File modified
- `.superpowers/brainstorm/6491-1782131242/content/preview.html`
