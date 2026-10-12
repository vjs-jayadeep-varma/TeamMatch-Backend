# TeamMatch backend

## Requirements and starting the backend

- JDK 21.
- MySQL Server and the MySQL command-line client.
- Node.js and npm for the frontend; see the [frontend setup guide](../frontend/README.md).

From PowerShell, first configure the required `DB_URL`, `DB_USERNAME`, and
`DB_PASSWORD` environment variables for your MySQL database using your local
environment or secret store. Do not put database credentials in source files,
frontend variables, or shared shell history. Complete the appropriate fresh or
existing database procedure below before starting the application.

From the `backend` directory, run the backend tests and start Spring Boot:

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

The Maven wrapper downloads its configured Maven distribution when needed. The
application listens on port `8080` by default; set `SERVER_PORT` to override
it. For administrator sign-in, configure both `TEAMMATCH_ADMIN_USERNAME` and
`TEAMMATCH_ADMIN_PASSWORD` through the local environment or secret store.
The administrator username may be any non-blank value up to 254 characters;
the password must be 12–72 characters and at most 72 UTF-8 bytes.
it is a separate configured account and is not created through student
registration. Open `/admin/login` in the frontend to use the dedicated admin
sign-in page. The backend encodes the configured password with the configured
password encoder and grants this account the ADMIN role. Student registration
always creates a STUDENT account. The configured admin username must not match
an existing or future student email; startup, registration, and profile updates
reject that collision rather than mapping a student identity to the admin.

The authenticated owner can cancel an `OPEN` project with
`PATCH /api/projects/{id}/close`. The transition is one-way to `CANCELLED`;
existing memberships, requests, and invitations are retained, while new
requests and accepting pending requests are rejected.

The normal profile authenticates registered students from MySQL. The optional
`dev` Spring profile also requires `TEAMMATCH_DEV_USERNAME` and
`TEAMMATCH_DEV_PASSWORD`; its shared development identity has no student or
administrator role and cannot sign in to the student/admin UI. Registered
database students and any configured administrator remain the application
accounts.

## Database setup and upgrades

The repository contains ordered MySQL schema scripts under
[`src/main/resources/db/migration`](./src/main/resources/db/migration). They are
**not automatically executed**: the application has no Flyway or Liquibase
dependency. Apply the scripts manually with a MySQL client before starting the
backend. Hibernate uses `ddl-auto=validate` in both normal and `dev` profiles;
it checks the mapped schema at startup but does not create or alter tables.

Back up an existing database before running schema changes. Do not run the
initial-schema migration against a database that already has TeamMatch tables.

### Fresh database

1. Create an empty MySQL database and a database user with access to it. For
   example, run these statements using a MySQL account authorized to provision
   databases and users (replace the database/user identifiers as appropriate):

   ```sql
   CREATE DATABASE teammatch CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   CREATE USER 'teammatch_app'@'localhost' IDENTIFIED BY '<set-a-strong-secret>';
   GRANT ALL PRIVILEGES ON teammatch.* TO 'teammatch_app'@'localhost';
   ```

   Store that user's password in your local secret store/environment, not in
   this repository. Confirm that the database is empty before proceeding:

   ```sql
   USE teammatch;
   SHOW TABLES;
   ```

   Do not run V1 if this returns tables; inspect the schema and follow the
   existing-database procedure instead.
2. From the `backend` directory, apply the scripts in this exact order to that
   database:

   1. `src/main/resources/db/migration/V1__create_initial_schema.sql`
   2. `src/main/resources/db/migration/V2__add_team_request_recipient.sql`
   3. `src/main/resources/db/migration/V3__add_student_interests.sql`

   For example, from PowerShell with the MySQL client installed:

   ```powershell
   Get-Content -Raw .\src\main\resources\db\migration\V1__create_initial_schema.sql | mysql --host=localhost --user=teammatch_app -p --database=teammatch
   Get-Content -Raw .\src\main\resources\db\migration\V2__add_team_request_recipient.sql | mysql --host=localhost --user=teammatch_app -p --database=teammatch
   Get-Content -Raw .\src\main\resources\db\migration\V3__add_student_interests.sql | mysql --host=localhost --user=teammatch_app -p --database=teammatch
   ```

3. Configure `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` for the same database,
   then start the backend. Startup validation will fail if the schema is
   incomplete or incompatible.
4. Verify the migration results before using the application:

   ```sql
   SHOW TABLES;
   SHOW CREATE TABLE team_requests;
   SHOW CREATE TABLE student_interests;
   SELECT COUNT(*) AS requests_without_recipient
   FROM team_requests request_record
   LEFT JOIN students recipient
     ON recipient.id = request_record.recipient_student_id
   WHERE recipient.id IS NULL;
   ```

   Confirm that all expected tables are present, the two extension tables
   contain the V2/V3 columns and constraints, and the query returns zero.
   Successful backend startup with `ddl-auto=validate` is the final check that
   the mapped entity tables and columns are compatible; it does not verify all
   indexes or data invariants.

### Existing database

1. Back up the database and inspect its schema before changing it. The V1
   migration is only for a fresh, empty database; **do not apply V1 to an
   existing database**.
2. Confirm the database has the pre-V2 TeamMatch tables and columns represented
   by the baseline in V1, especially `team_requests.team_id`,
   `team_requests.requesting_student_id`, and `projects.owner_student_id`.
   Inspect the affected definitions with `SHOW CREATE TABLE` before deciding
   which migration is missing.
3. Before applying V2 to a pre-V2 schema, check that every old request has a
   team, project, and project owner from which a recipient can be inferred:

   ```sql
   SELECT COUNT(*) AS requests_without_inferable_recipient
   FROM team_requests request_record
   LEFT JOIN teams team_record ON team_record.id = request_record.team_id
   LEFT JOIN projects project_record ON project_record.id = team_record.project_id
   LEFT JOIN students owner_record ON owner_record.id = project_record.owner_student_id
   WHERE team_record.id IS NULL
      OR project_record.id IS NULL
      OR owner_record.id IS NULL;
   ```

   Continue only if this returns zero. V2 backfills historical recipients from
   each team's project owner, then adds the non-null foreign key and
   recipient/created-time index. Apply V2 once only when the recipient column
   is absent. If the column exists but is nullable, lacks its expected
   constraint/index, or otherwise appears partially migrated, do not rerun
   V2; stop and prepare a reviewed, data-preserving repair for that exact
   schema. If the column, constraint, and index already exist, do not rerun V2.
4. If the `student_interests` table is absent, apply V3 once after V2. If it
   already exists with the expected primary key and student foreign key, do
   not rerun V3. If it exists but is incomplete or differs from the expected
   definition, stop and prepare a reviewed, data-preserving repair rather than
   rerunning V3.
5. Configure `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` and start the backend.
   Hibernate validates the schema and reports incompatibilities; it does not
   repair them.
6. Verify the upgraded schema with `SHOW CREATE TABLE team_requests`,
   `SHOW CREATE TABLE student_interests`, and the zero-result recipient query
   from the fresh-database verification above. Also confirm successful backend
   startup with `ddl-auto=validate`.

Because there is no migration history table, operators must track which scripts
have been applied. Do not re-run non-idempotent `ALTER TABLE` or `CREATE TABLE`
scripts. If an existing schema differs from the baseline or historical request
recipients cannot safely be inferred, stop and prepare a reviewed,
data-preserving migration for that database instead of applying these scripts
blindly. MySQL DDL may leave a migration partially applied if a script fails;
inspect the resulting schema after any failure and do not retry the whole
script without confirming which statements already succeeded.

### Configuration

`DB_URL` must be a MySQL JDBC URL for the target database, for example
`jdbc:mysql://localhost:3306/teammatch_db`. Set `DB_USERNAME` and `DB_PASSWORD`
through the environment or deployment secret store. The backend listens on
`SERVER_PORT` or port `8080` by default.

For local development with the `dev` Spring profile, also configure
`TEAMMATCH_DEV_USERNAME` and `TEAMMATCH_DEV_PASSWORD`. Administrator accounts
are provisioned separately using `TEAMMATCH_ADMIN_USERNAME` and
`TEAMMATCH_ADMIN_PASSWORD`; the administrator password is encoded at startup.
Never commit credentials or place them in frontend variables.

The administrator dashboard uses read-only, role-protected endpoints under
`/api/admin`: `/overview` provides platform totals, while `/students`,
`/projects`, `/teams`, and `/team-requests` return bounded pages (maximum 100
records per request). Student and project lists support a `query` parameter;
record detail routes expose student skills/interests/projects/memberships and
project requirements/team membership without returning passwords or hashes.
Request pages can be filtered by `status` and `projectId`. A student session
receives HTTP 403 for every administrator endpoint.

### Optional local demonstration data

The repository includes an opt-in data seeder for a local MySQL database named
`teammatch_db`. It is available only with the `dev` Spring profile, is disabled
by default, and refuses non-local MySQL hosts or a different database name.
It does not run in the `prod` profile. It uses the registration service to
create missing student accounts (so passwords are BCrypt-hashed), the existing
skill/project/request services to add their records, and one transaction for
the complete seed. It never changes an existing account password or removes
records. Existing accounts with the demo emails retain their profile fields;
the seeder only adds missing skills/interests. Existing project-title
collisions with incompatible owners or required skills cause the transaction
to fail rather than modifying those projects.

Before enabling it, inspect the local database and verify that it is the
intended `teammatch_db`, that V1–V3 are already correctly applied, and that
existing demo-email accounts/projects can be safely reused. The seeder is not a
database migration and must not be used instead of applying the ordered schema
migrations above.

The read-only inventory query is
[`demo_seed_read_only_inventory.sql`](./src/main/resources/db/demo_seed_read_only_inventory.sql).
It starts a read-only transaction, reports the relevant schema and current
counts/records, and rolls the transaction back. From the `backend` directory,
you can run it with the MySQL client; the password option prompts locally and
does not put the password in the command:

```powershell
$target = [Uri]($env:DB_URL.Substring("jdbc:mysql:".Length))
Get-Content -Raw .\src\main\resources\db\demo_seed_read_only_inventory.sql |
  mysql --host=$target.Host --port=$target.Port --user=$env:DB_USERNAME --password --database=teammatch_db --table
```

Verify the first result says `teammatch_db`. Review the schema and current
records before enabling the seeder. Share only the inventory output if asking
for help; never share connection variables or passwords.

In PowerShell, set the connection variables through your local environment or
secret store without putting passwords in scripts or source control. Then
explicitly opt in and start with the `dev` profile:

```powershell
$env:TEAMMATCH_DEMO_SEED_ENABLED = "true"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Unset the opt-in after that run:

```powershell
Remove-Item Env:TEAMMATCH_DEMO_SEED_ENABLED
```

New local student accounts follow the demo email/password convention specified
for this dataset. The seeder never logs passwords. Run it only against a
private local demo database; do not expose these accounts or credentials in a
public deployment. Re-running it reuses matching records and adds only missing
data. It preserves existing accounts' passwords and profile fields and does
not force pre-existing request statuses back to the seed examples.

After startup, review the seeder's aggregate created/reused counts. Check the
student, skill, project, team, membership, and request records using the
read-only verification queries already described above and the relevant
student endpoints. Then sign in with two local demo accounts to verify the
persisted request and team workflow. The seeder must not be run against a
production database.
