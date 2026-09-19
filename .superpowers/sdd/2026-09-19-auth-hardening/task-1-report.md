# Task 1 report: additive V19 schema

## Scope

- Added Flyway `V19__add_auth_identity_and_handoff_columns.sql`.
- Added `users.session_version INTEGER NOT NULL DEFAULT 0`.
- Added `user_auth_identities` with provider/issuer/subject identity keys, local user linkage, provider e-mail metadata, timestamps, provider-subject uniqueness, and one identity per user/issuer.
- Added nullable OAuth handoff columns `code_hash`, `user_id`, and `consumed_at`.
- Kept legacy `code` and `jwt` columns and made them nullable so hash-only rows can coexist during rolling deployment.
- Added the minimal JPA mappings for `User.sessionVersion`, the additive OAuth fields, and the identity table.
- Did not implement Google binding, OAuth exchange behavior, session validation, cookie hardening, or documentation updates outside this report.

## TDD evidence

### Red

Command:

```text
cd backend && ./mvnw -Dtest=UserTableMigrationTest test
```

Result: expected failure before V19 existed. The existing schema was at Flyway v18; the test reported missing `users.session_version`, missing `user_auth_identities`, and missing OAuth `code_hash`/`user_id`/`consumed_at` (3 failures plus the nullability query error).

### Green

Command:

```text
cd backend && ./mvnw -Dtest=UserTableMigrationTest test
```

Result: `BUILD SUCCESS`; 5 tests run, 0 failures, 0 errors, 0 skipped against PostgreSQL 17 Testcontainers. Flyway applied V19 successfully and Hibernate schema validation passed.

H2 compatibility command:

```text
cd backend && ./mvnw -Dtest=NossaListaApplicationTests test
```

Result: `BUILD SUCCESS`; 1 test run, 0 failures, 0 errors. This caught and led to splitting V19's multi-column `ALTER TABLE ... ADD` into separate statements for H2 compatibility.

Quality gate:

```text
./scripts/quality.sh --pre-commit
```

Result: `pre-commit gate ok` (backend and frontend checks passed).

## Concerns for follow-up tasks

- Task 2 should add the identity repository and Google binding policy on top of the `UserAuthIdentity` mapping.
- Task 3 should populate `code_hash`/`user_id`, atomically set `consumed_at`, and retain the legacy dual-format behavior while old pods remain.
- The migration intentionally retains legacy `code` and `jwt`; V20 removal is outside this task and must wait for the rollout safety condition in the plan.
