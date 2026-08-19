> **Document:** Scan Pilot Database Status  
> **File:** `database/README.md`  
> **Version:** v1.0.0  
> **Created:** 2026-08-11  
> **Last Updated:** 2026-08-19  
> **Status:** Active  

# Scan Pilot Database Status

PostgreSQL is the active core database, managed via Spring Data JPA and Flyway migrations under `backend/src/main/resources/db/migration/`.

The initial core schema is established by `V1__init_core_schema.sql` (implemented under Issue `#22`), providing 12 core tables:
- User & Auth: `users`, `user_sessions`
- Repository & Monitoring: `repositories`, `monitored_branches`
- Scan Orchestration: `scan_jobs`, `scan_checkpoints`
- Finding & Evidence: `findings`, `finding_locations`, `evidence_items`
- Coverage Tracking: `coverage_records`, `coverage_items`
- Collaboration: `review_requests`

Canonical references:
- Schema Migration: `backend/src/main/resources/db/migration/V1__init_core_schema.sql`
- JPA Entities: `backend/src/main/java/com/scanpilot/persistence/entity/`
- Repositories: `backend/src/main/java/com/scanpilot/persistence/repository/`
- [Architecture Direction](../docs/ARCHITECTURE.md)
- [Evidence Model](../docs/EVIDENCE-MODEL.md)
- [Finding Tracking](../docs/FINDING-TRACKING.md)
- [Accepted Decisions](../docs/DECISIONS.md)
