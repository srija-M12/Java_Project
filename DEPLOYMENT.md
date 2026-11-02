# Deployment preparation

The repository now builds the React frontend into the Spring Boot application. A Docker host can run the frontend and API from one HTTPS origin, which also keeps the session cookie flow simple.

## Required hosted service variables

Set these in the hosting service's private variable/secret settings. Do not commit real values to GitHub.

| Variable | Value |
| --- | --- |
| `PORT` | Supplied by the hosting service; the container uses it automatically. |
| `DB_URL` | JDBC URL for the hosted MySQL database, for example `jdbc:mysql://HOST:3306/student_registration`. |
| `DB_USERNAME` | A dedicated MySQL application user, not `root`. |
| `DB_PASSWORD` | Password for that MySQL user. |
| `CAMPUS_CODE` | A new, private code for the hosted instance. Do not reuse a development code. |
| `SESSION_COOKIE_SECURE` | `true` when the site is served over HTTPS. |
| `APP_CORS_ALLOWED_ORIGINS` | Only needed if the frontend is hosted separately; set it to the exact frontend origin. |

## Before the first public launch

1. Create a new hosted MySQL database. Do not expose its port to the public internet unless needed temporarily for importing the schema.
2. Run `database/schema/schema.sql`, then `course_details.sql`, `registration_rules.sql`, `timetable_rules.sql`, and `attendance.sql` against the hosted database.
3. Add the required course, faculty, faculty-availability, policy, and demo account data. Do not import real student records into a public demo unless you have permission to publish them.
4. Create the backend web service from this repository's root `Dockerfile` and set the variables above.
5. Open the generated HTTPS URL. Check `/api/health`, sign in, create a registration, generate a timetable, and record attendance.

The application currently keeps login sessions in the running backend process. Use one backend instance for this project demo; horizontal scaling requires a shared session store.

## One-host setup with Railway

Railway supports GitHub-connected services, Dockerfiles, and a MySQL service. In Railway, create a project, add a MySQL database, then add a service connected to this repository. Set the service's root directory to `/` so it finds the root `Dockerfile`.

Add these variables to the application service, using Railway's reference-variable picker for the MySQL values (select the MySQL service you created):

```text
DB_URL=jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}
DB_USERNAME=${{MySQL.MYSQLUSER}}
DB_PASSWORD=${{MySQL.MYSQLPASSWORD}}
CAMPUS_CODE=<a new private value>
SESSION_COOKIE_SECURE=true
```

If your MySQL service has a different name, select it in the reference-variable picker instead of `MySQL`. `PORT` is supplied by Railway. Since the frontend is included in the same backend service, `APP_CORS_ALLOWED_ORIGINS` can be left unset.

Railway's current Free plan advertises a 30-day trial with $5 credits, then $1/month, with resource limits. Check the current pricing and usage before creating services; actual usage can cost more.

## Demo data and privacy

Use a fresh hosted database and synthetic demo accounts for a public presentation. The local SQL dumps at the repository root are intentionally ignored by Git because they may contain personal account or faculty data. Never turn on public database access for normal operation; keep the database on the hosting provider's private network and use its protected connection flow only when importing the schema.

This is currently a project demo, not a production identity system: sign-up is protected by one shared campus code, and the app does not verify enrollment with the institution. Keep the code private and use only fictional student data. Before using it with real students, add individual enrollment verification, stronger account recovery, and request throttling.

## Local development after deployment configuration

Run the Spring Boot backend on port 8080 and the Vite dev server on port 5173 as described in `README.md`. Vite proxies `/api` requests to the local backend.
