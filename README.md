# AppWorks Support Portal

**Status after audit & fixes: Functionally complete backend, tests added, code reformatted.**

## What's actually built and working

**Core entities (8):** Customer (soft-delete), Environment, Metric, CustomerMetric, Schedule, Run, AlertConfiguration, AlertRecipient.

**APIs (7 controllers):**
- Customers: full CRUD, nested environments/metrics under `/api/v1/customers/{customerId}/...`
- Metrics: global catalog CRUD via `/api/v1/metrics`
- Schedules: CRUD via `/api/v1/schedules` with optional customerId filter
- Runs: manual "Run Now" via `POST /api/v1/runs`, basic recent history via `GET /api/v1/runs`, and now **filterable/paginated Run History** via `GET /api/v1/runs/history` (filters: customerId, environmentId, metricId, status, from, to; supports page/size)
- Dashboard: `GET /api/v1/dashboard/summary`, `GET /api/v1/dashboard/customer-health`, and **`GET /api/v1/dashboard/metric-status`** (added after comparing against the reference UI's "All Metrics — Last Run Status" grid — each metric's single most recent run status, across all customers, `null` if never run)

**Monitoring engine:** Works end-to-end. SchedulePollingService polls every 60s, executes due schedules, invokes MockAppworksClient, evaluates thresholds against metrics (PASS/WARNING/FAIL/ERROR logic), saves Run records, and now actually calls AlertService.

**Alert system:** AlertService exists and is wired. Currently logs a mock email (no real SMTP yet). Respects frequency throttling (EVERY_FAILURE / HOURLY / DAILY) and correctly skips PASS runs.

**Mock AppWorks integration:** MockAppworksClient uses deterministic hashing, so the same customer/environment/metric always produces the same result (good for predictable testing, but means health won't show variation in a demo without code changes).

**Database:** H2 by default (zero setup), PostgreSQL profile ready. PostgreSQL and SMTP credentials are read from environment variables and are not stored in this repository. See `src/main/resources/application-example.yml` for the configuration template.

**Tests (9 integration + unit test classes):** 
- Customers, Environments, Metrics, CustomerMetrics (from Milestones 1–3, pre-audit)
- Schedules (7 tests covering CRUD, metric-assignment validation, cron normalization, duplicate detection)
- Runs (5 tests covering manual execution, multi-metric runs, validation, history)
- Run History filtering (5 tests covering customerId/environmentId/metricId/status filters, pagination, and a shared-database-safe unfiltered check)
- Dashboard (3 tests covering summary stats, per-customer health, delta-based assertions since other test classes leave committed rows behind in the shared H2 database)
- AlertService (4 unit tests: PASS-skip, first-alert, throttling, disabled config)
- MonitoringService thresholds (10 unit tests: GREATER_THAN, LESS_THAN, critical/warning/pass boundaries, null handling)
- SchedulePollingService (4 unit tests: due execution, future skip, disabled skip, never-executed initial run)

## Frontend

A single-page app at `src/main/resources/static/index.html`, served automatically by Spring Boot at `http://localhost:8080/` (same origin as the API — no CORS setup needed). Styled to match the actual reference UI screenshots (dark theme, sidebar nav, badge colors) rather than an invented design. Every page is wired to real endpoints:

- **Dashboard** — live KPIs, recent runs, customer health bars, **and an "All Metrics — Last Run Status" grid** (added after comparing against the reference UI), all from `/api/v1/dashboard/*` and `/api/v1/runs/history`
- **Customers** — list + create + detail view (environments, assigned metrics), all real CRUD
- **Metrics** — list + create, real CRUD
- **Run History** — filterable by customer/**metric**/status with pagination, real data (metric filter added after comparing against the reference UI)
- **Schedules** — list + create + delete + **enable/disable toggle** (added after comparing against reference UI), cascading customer→environment/metric dropdowns that only show metrics actually assigned to the selected customer (matches the backend's validation)
- **AppWorks Config** — the per-metric endpoint/enable toggle is real (calls the actual `PUT /api/v1/metrics/{id}`). The "Spring Boot Server Settings" section (API Base URL, DB Host, Redis Host) is shown **disabled with an explicit note** that there's no backend endpoint for it — the reference UI has this section but nothing in the requirements ever specified persisting arbitrary server config through the API, so faking a working save button would be dishonest UI, not a shortcut.
- **Run Now** — global button, opens the same manual-run flow as Run History, calls the real `POST /api/v1/runs`

I could not click through this in a real browser in my sandbox. What I did verify: extracted the `<script>` block and checked it with `node --check` (confirms valid JS syntax, catches broken string escaping), and cross-referenced every DOM element ID referenced in JS against the actual HTML elements, and every field name used in the frontend against the actual backend DTO field names. **You should still click through it yourself** — syntax validity and field-name matching don't catch every possible runtime or layout issue, especially anything involving actual browser rendering.

## Real bugs found during audit and fixed

7. **Run History filter completely broken against real PostgreSQL** (found via manual browser testing after switching to the postgres profile, not by any automated test — the entire test suite runs against H2, which is far more lenient about this than Postgres). The original query used `(? IS NULL OR column = ?)` for every optional filter, so a single value could either skip or apply the filter. Postgres's JDBC driver throws `ERROR: could not determine data type of parameter $9` on this pattern, because it binds each `?` as an independent position and can't infer a type for one used only in an `IS NULL` check — even though it's logically the same value as a properly-typed `?` elsewhere in the query. This broke both `GET /api/v1/runs/history` directly and the Dashboard (which calls that same endpoint for "Recent Runs"), meaning the Dashboard would fail to load entirely on Postgres. **Fixed properly, not patched**: replaced the JPQL null-check pattern with a JPA `Specification` (`RunSpecifications`) that only adds a predicate when a filter value is actually provided — so there is never an ambiguous, type-less parameter sent to the database in the first place.

8. **A second, compounding bug that hid bug #7 completely**: the frontend's page router (`render()`) called `return renderDashboard()` without `await`ing it inside its `try` block. Since `return` exits the `try` block immediately — before the returned promise settles — any error thrown inside `renderDashboard()` (or any other page's render function) became an invisible, unhandled promise rejection. The page would just show "Loading..." forever with zero indication anything was wrong, in the UI, the console, or anywhere else visible to the user. Fixed: every route handler in `render()` is now properly `await`ed inside the `try` block, so real errors surface as an actual error message on the page.

1. **AlertService was dead code.** It existed but was never called from MonitoringService. Fixed: AlertService.notify(run) now fires after every run completes.

2. **AlertService alert-on-every-run bug.** The notify() method had no check for run status, so it would email on every PASS run, not just failures/warnings. Fixed: now checks `if (status == PASS) return;` first.

3. **Delete guards missing.** Environment and Metric deletes had no guard against being referenced by Schedule — would've thrown a raw 500 if you tried. ScheduleRepository already had `existsByEnvironmentId` and `existsByMetricId` defined, just never wired in. Fixed: both services now call these guards and throw a clean 409.

4. **Schedule validation gap.** ScheduleService.create() never checked whether a metric was actually assigned to the customer via CustomerMetric. You could schedule any metric in the global catalog without permission. Fixed: now validates and rejects with a clear 400 error.

5. **Schedule controller incomplete.** Only `POST` and `GET all` existed. Fixed: added `GET /{id}`, `PUT /{id}`, `DELETE /{id}`.

6. **Schedule delete threw a foreign-key violation if the schedule had already executed.** Found via manual UI testing (clicking Delete in the browser appeared to do nothing), not by automated tests — a real gap the manual click-through caught that the test suite hadn't. `Run.schedule` is a FK with no delete behavior configured; once a schedule fires and creates a `Run` row, deleting that schedule hit a raw, unhandled 500. Made worse by a second, compounding bug: the frontend's `deleteSchedule()` had no error handling at all, so the failure was completely invisible to the user. Fixed both: `ScheduleService.delete()` now detaches referencing runs first (sets their `schedule_id` to null, preserving the run rows and their data — a schedule is disposable, the runs it already caused aren't), and the frontend now shows an alert on any delete failure instead of failing silently. Added a regression test that actually exercises this exact path.

## What still isn't done

1. **Real SMTP email sending.** ~~spring-boot-starter-mail is on the classpath, but JavaMailSender is not wired. AlertService logs mock emails for now.~~ **Done, but OFF by default.** `AlertService` now sends real email via `JavaMailSender` when `app.alerts.email-enabled: true` in `application.yml` — defaults to `false` since no real SMTP server/credentials exist yet. Fill in `spring.mail.*` (host/port/username/password) with real values, flip the flag, done. SMTP failures are caught and logged, never allowed to crash the monitoring run that triggered them — verified by a dedicated Mockito unit test (`AlertServiceEmailFailureTest`) that simulates a broken mail server without ever making a real network call.

2. **Dashboard API.** ~~The reference UI expects endpoints returning customer count, runs-today, success rate, per-customer health. Not built yet.~~ **Done.** `GET /api/v1/dashboard/summary` and `GET /api/v1/dashboard/customer-health`. Design decision: health = percentage of a customer's all-time runs that were PASS (WARNING and FAIL both count against it). If you want a rolling window (e.g. last 7 days) instead of all-time, `DashboardService.getCustomerHealth()` is where to change it. Both endpoints return `null` (not misleading 0%/100%) when there's no run data yet.

3. **Filterable run history.** ~~Only `GET /api/v1/runs` returns unfiltered recent 20. No filters by customer/metric/status/environment/date yet.~~ **Done.** `GET /api/v1/runs/history` — all filters optional, supports pagination. Kept as a separate endpoint from `GET /api/v1/runs` deliberately, to avoid changing that endpoint's existing response shape and breaking its existing test/callers.

4. **Frontend.** ~~The reference UI HTML exists but is still hardcoded static data. Not connected to the real APIs yet.~~ **Done.** See the "Frontend" section above.

5. **Real AppWorks SOAP integration.** Currently mocked. Real client goes in `/src/main/java/com/appworks/portal/integration/` once WSDL/endpoint/auth details are provided.

6. **Security.** No authentication/authorization. Should add Spring Security when that becomes a requirement.

## Important design decisions

- **Cron normalization:** The reference UI uses 5-field Unix cron (`*/5 * * * *`). Spring requires 6-field with seconds (`0 */5 * * * *`). ScheduleService.normalize() automatically converts on input, stores the 6-field form. All cron examples from the reference work out of the box.

- **Customer soft-delete:** `DELETE /api/v1/customers/{id}` sets status=INACTIVE, doesn't remove the row. Necessary because Environment, Schedule, and Run all hold customer_id FK. A hard delete would violate the FK or cascade-destroy run history.

- **Schedule uniqueness:** Enforced as one schedule per (customer, environment, metric) triple via database unique constraint. If you need multiple schedules for the same customer/environment/metric (e.g., one every 5 min and one daily), that's a schema change, not a code fix.

- **MockAppworksClient determinism:** Returns a value derived from hashCode(customerCode + environmentName + metricServiceKey). Same input = same output, always. Useful for predictable testing; useless for demonstrating changing health. If that matters for a demo, make the hash time-dependent or add a `@Value("${mock-appworks.use-random-values:false}")` toggle.

- **Table names:** `monitor_schedule` and `monitor_run`, not `schedule` and `run`, because "schedule" is a SQL reserved word in some dialects.

## How to verify it works

```bash
cd appworks-support-portal
mvn clean install        # compiles, runs all 9 test classes (16+ test methods total)
mvn spring-boot:run      # starts on http://localhost:8080
```

For PostgreSQL and email alerts, set `DB_USERNAME`, `DB_PASSWORD`, `SMTP_HOST`,
`SMTP_USERNAME`, and `SMTP_PASSWORD` in the environment before starting the
`postgres` profile. Set `DB_URL` or `SMTP_PORT` only when the defaults do not
match your setup. Never commit a local `.env` or `application-local.yml` file.

Once running:

```bash
# Create a customer
curl -X POST http://localhost:8080/api/v1/customers \
  -H "Content-Type: application/json" \
  -d '{"name":"Acme","code":"ACME","contactEmail":"ops@acme.com"}'

# Create an environment for that customer (use the ID returned above)
curl -X POST http://localhost:8080/api/v1/customers/1/environments \
  -H "Content-Type: application/json" \
  -d '{"name":"PROD","baseUrl":"https://acme.appworks.com/api"}'

# Create a metric (global)
curl -X POST http://localhost:8080/api/v1/metrics \
  -H "Content-Type: application/json" \
  -d '{"name":"Queue Depth","serviceKey":"SVC_QUEUE_03","warningThreshold":300,"criticalThreshold":500,"comparisonOperator":"GREATER_THAN"}'

# Assign the metric to the customer
curl -X POST http://localhost:8080/api/v1/customers/1/metrics \
  -H "Content-Type: application/json" \
  -d '{"metricId":1}'

# Create a schedule
curl -X POST http://localhost:8080/api/v1/schedules \
  -H "Content-Type: application/json" \
  -d '{"customerId":1,"environmentId":1,"metricId":1,"cronExpression":"*/5 * * * *"}'

# Manually run a metric (Run Now)
curl -X POST http://localhost:8080/api/v1/runs \
  -H "Content-Type: application/json" \
  -d '{"customerId":1,"environmentId":1,"metricIds":[1]}'

# Dashboard
curl http://localhost:8080/api/v1/dashboard/summary
curl http://localhost:8080/api/v1/dashboard/customer-health

# Check recent run history
curl http://localhost:8080/api/v1/runs

# Filterable run history (all query params optional)
curl "http://localhost:8080/api/v1/runs/history?customerId=1&status=FAIL&page=0&size=10"
curl "http://localhost:8080/api/v1/runs/history?from=2026-09-01T00:00:00&to=2026-09-30T23:59:59"
```

Wait 60+ seconds and check the logs — you should see log output indicating scheduled runs firing (SchedulePollingService polls every 60s).

## File structure

```
src/main/java/com/appworks/portal/
├── entity/               (Customer, Environment, Metric, CustomerMetric, Schedule, Run, AlertConfiguration, AlertRecipient)
├── repository/           (8 JpaRepository interfaces)
├── service/              (9 services: Customer, Environment, Metric, CustomerMetric, Schedule, Run, Monitoring, Alert, SchedulePolling)
├── controller/           (6 REST controllers)
├── dto/                  (Request/response DTOs for all entities)
├── exception/            (BadRequestException, DuplicateResourceException, ResourceInUseException, ResourceNotFoundException, GlobalExceptionHandler)
├── integration/          (AppworksClient interface, MockAppworksClient, AppworksResult)
└── SupportPortalApplication.java

src/test/java/com/appworks/portal/
├── CustomerControllerTest.java      (5 tests)
├── EnvironmentControllerTest.java   (4 tests)
├── MetricControllerTest.java        (3 tests)
├── CustomerMetricControllerTest.java (5 tests)
├── ScheduleControllerTest.java      (7 tests — added during audit)
├── RunControllerTest.java           (5 tests — added during audit)
├── AlertServiceTest.java            (4 tests — added during audit)
├── MonitoringServiceThresholdTest.java (10 tests — added during audit)
└── SchedulePollingServiceTest.java  (4 tests — added during audit)

src/main/resources/
└── application.yml      (H2 default + postgres profile)

pom.xml                  (Spring Boot 3.3.4, Maven, Java 21)
```

## Code quality notes

During audit, 22 files were reformatted from single-line dense code into readable, properly indented code with explanatory comments. No logic changed, only style. All brace balance verified post-reform.

## Next steps

1. **Run the build locally.** Confirm `mvn clean install` passes all 9 test classes.
2. **Manually test the APIs** using the curl examples above.
3. **Decide on real AppWorks integration:** Get WSDL / SOAP endpoint / credentials from your team, write a real AppworksClient impl (can coexist with Mock), mark it `@Primary`.
4. **Wire up real email:** Configure SMTP, create EmailService, wire JavaMailSender into AlertService.
5. **Build the remaining APIs:** Dashboard (stats), filterable run history.
6. **Connect the frontend:** Wire the reference UI to the real REST APIs.
7. **Add security:** Spring Security, role-based access if needed.

## Known limitations / TODOs

- No pagination on list endpoints
- No soft-delete for Environment or Metric (only Customer) — add if schedules/runs reference them
- No logging framework configured (using Spring's default SLF4J)
- No metrics/observability (Micrometer, Prometheus)
- No rate limiting
- No API documentation (Swagger/OpenAPI)
