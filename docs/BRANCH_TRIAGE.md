# Branch Triage and Repair-Path Guard

## Canonical repair path

For the root-cause exercise-selection and image-resolution work, use only:

- Working branch: `fix/catalog-backed-gender-safe-plans`
- Intended base: `main` at `4ac477feb72d28b1339933660f594ef2fb8f796b`

Do not switch to another historical branch or merge an old pull request as a shortcut. Before continuing, verify the working branch still exists and inspect its diff against the current `main`.

## Superseded branches — excluded from the repair path

The following pull requests were closed on 2026-11-05 and must not be reopened, cherry-picked, or merged as part of this repair without a fresh, explicit technical review:

| PR | Branch | Reason |
|---|---|---|
| #9 | `feature/gymvisual-exercise-library` | Historical catalog/matcher work; does not implement the complete root-cause fix. |
| #18 | `feature/exercise-library-selection-speed` | Historical library performance work; does not implement the complete root-cause fix. |

Their Git branches still exist because branch deletion was not available through the connected GitHub operations. Closing the PRs does not delete their branches.

## Other historical work

Open PRs #27, #28, #30, #33, and #34 are not automatically approved or part of the canonical repair path. Inspect each diff against the current `main` and the canonical repair branch before considering any reuse. Do not merge them wholesale.

## Mandatory checks before any branch is used

1. Confirm the branch name is the canonical repair branch above.
2. Confirm its base/merge-base against `main`; do not assume a similarly named or stale branch is current.
3. Review every changed file and commit; reject unrelated or duplicate changes.
4. Verify the actual root-cause requirements: gender-neutral manual selection, catalog-backed gender-aware plan generation, persisted catalog ID/gender, ID-based image resolution, safe database migration, and backward compatibility.
5. Run the relevant tests and CI. A successful APK-only build is not equivalent to a passing full Android CI.
6. Merge only after the checks pass. Never treat a closed/superseded PR as an active candidate.

This file is a repository guardrail, not evidence that the root-cause repair is complete.
