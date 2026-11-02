# Java_Project

**Student Course Registration and Timetable Generator** — A full-stack web application for student and faculty login, course registration, faculty availability management, and automatic timetable generation. Built with React, Spring Boot, and MySQL.

## Features

- Student and faculty sign-in
- Student account sign-up with academic profile details
- Course catalogue and six-course registration
- Faculty availability management
- Automatic weekly timetable generation
- Course unit and topic details
- MySQL persistence

## Technology

- Frontend: React, Vite
- Backend: Java 25, Spring Boot, Maven
- Database: MySQL

## Run locally on Windows

### 1. Set up the database

Create the `student_registration` database. In MySQL Workbench, run these schema files in order:

1. `database/schema/schema.sql`
2. `database/schema/course_details.sql`
3. `database/schema/registration_rules.sql`
4. `database/schema/timetable_rules.sql`

These files define the database structure and do not contain the local student, user, or faculty records.

### 2. Start the backend

Open PowerShell in the `backend` folder and set your local values in that window:

```powershell
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "your-mysql-password"
$env:CAMPUS_CODE = "your-campus-code"
mvn spring-boot:run
```

### 3. Start the frontend

Open another PowerShell window:

```powershell
cd "path\to\Java_Project\frontend"
npm install
npm run dev
```

Open the local address printed by Vite, usually `http://localhost:5173/`.

> Database credentials and the campus code belong in local environment variables, not in this repository. A fresh database needs course, faculty, availability, registration-policy, and student records before it can be used.
