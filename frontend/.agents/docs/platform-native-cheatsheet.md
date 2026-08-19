> **Document:** Platform-Native & Stdlib Cheat-Sheet  
> **File:** `.agents/docs/platform-native-cheatsheet.md`  
> **Version:** v1.0.0  
> **Created:** 2026-08-19  
> **Last Updated:** 2026-08-19  
> **Status:** Active  

# Platform-Native & Standard Library Cheat-Sheet for Scan Pilot

Before reaching for a third-party library or writing custom utility boilerplate, consult this cheat-sheet. Native platform and standard library features are free, pre-tested, standard-compliant, and do not add dependency maintenance overhead.

---

## 1. Java 21 & Spring Boot 3 (Backend)

| Common Over-Engineering | Java 21 / Spring Boot Native Equivalent |
|---|---|
| Hand-rolled DTOs with getters/setters/equals | `public record ScanResultDto(UUID id, String status, Instant scannedAt) {}` |
| Deep `instanceof` casting blocks | Pattern Matching for `switch` / `instanceof`: `if (obj instanceof Finding f) { ... }` |
| Multi-line string concatenation / SQL / JSON templates | Java Text Blocks: `""" SELECT ... """` |
| External Apache Commons `StringUtils` | `org.springframework.util.StringUtils.hasText(str)` |
| External `CollectionUtils` | `org.springframework.util.CollectionUtils.isEmpty(list)` |
| Apache Commons IO / File utils | `java.nio.file.Files.readString(path)`, `Files.writeString(...)` |
| Apache HttpClient / Unirest for simple REST calls | `java.net.http.HttpClient` or Spring `RestClient` / `WebClient` |
| Custom UUID generator | `java.util.UUID.randomUUID()` |
| Complex custom thread pools for light async tasks | Java 21 Virtual Threads (`Executors.newVirtualThreadPerTaskExecutor()`) |

---

## 2. PostgreSQL (Database Layer)

| Common Application-Layer Bloat | PostgreSQL Native Feature |
|---|---|
| Java-side uniqueness verification before insert | `UNIQUE` constraint on column / compound index + catch violation |
| Value range checks in Java service | `CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))` |
| Custom UUID generation in application | `id UUID PRIMARY KEY DEFAULT gen_random_uuid()` |
| Complex JSON mapping tables for dynamic finding attributes | `jsonb` column with `@>` containment operators and GIN indexing |
| Pagination logic and count overhead | `LIMIT :size OFFSET :offset` with indexed keyset pagination |
| Hierarchical / Tree repository structure traversal | Recursive Common Table Expressions (`WITH RECURSIVE`) |
| Auto-updating `updated_at` timestamps | `DEFAULT CURRENT_TIMESTAMP` with trigger or `BEFORE UPDATE` trigger |

---

## 3. Web & Browser APIs (Frontend React + TypeScript)

| Package Developers Often Install | Native Browser / Web API Equivalent |
|---|---|
| `uuid` npm package | `crypto.randomUUID()` |
| `lodash.clonedeep` | `structuredClone(object)` |
| `query-string` / `qs` | `new URLSearchParams(window.location.search)` |
| `date-fns` format (for basic UI dates) | `new Intl.DateTimeFormat('en-US', { dateStyle: 'medium' }).format(date)` |
| `numeral` / currency formatting | `new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(amount)` |
| `clipboard.js` | `navigator.clipboard.writeText(text)` |
| Infinite scroll libraries | `new IntersectionObserver(callback).observe(sentinelElement)` |
| Timeout fetch wrapper packages | `fetch(url, { signal: AbortSignal.timeout(5000) })` |

---

## 4. HTML5 & CSS3 (UI Controls)

| UI Component Library Often Used | Native HTML5 / CSS3 Equivalent |
|---|---|
| Date picker popup component | `<input type="date" />` |
| Modal / Dialog component library | `<dialog ref={dialogRef}>` with `dialog.showModal()` |
| Accordion / Collapsible section | `<details><summary>Title</summary><div>Content</div></details>` |
| Tooltip package | `title` attribute or pure CSS `:hover` tooltip |
| Responsive layout media query bloat | CSS Grid: `grid-template-columns: repeat(auto-fit, minmax(280px, 1fr))` |
| Dynamic clamped text sizes | CSS `clamp(1rem, 2.5vw, 1.75rem)` |
| Multi-line text truncation | CSS `-webkit-line-clamp: 3; display: -webkit-box; -webkit-box-orient: vertical; overflow: hidden;` |
