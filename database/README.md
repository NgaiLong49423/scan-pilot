> **Document:** Scan Pilot Database Status  
> **File:** `database/README.md`  
> **Version:** v0.1.0  
> **Created:** 2026-08-11  
> **Last Updated:** 2026-08-12  
> **Status:** Draft  

# Scan Pilot Database Status

PostgreSQL is the accepted database direction, with Spring Data JPA/Hibernate and Flyway migrations under consideration for implementation.

The data model and schema have not been designed. `database/schema.sql` remains empty and is not a source of truth.

Do not invent tables or migrations until finding identity, evidence persistence, scan lifecycle, GitHub integration, and workspace requirements are specified.

Relevant documents:

- [Architecture Direction](../docs/ARCHITECTURE.md)
- [Requirements](../docs/REQUIREMENTS.md)
- [Inspection Specification](../docs/INSPECTION-SPEC.md)
- [Accepted Decisions](../docs/DECISIONS.md)
