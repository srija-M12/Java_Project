# Student Registration Backend

Spring Boot backend starter for the Student Course Registration and Timetable Generator.

## Requirements

- JDK 25
- Apache Maven 3.6.3 or later
- MySQL 8 with the `student_registration` database created from `../database/schema/schema.sql`

## Configure and run (PowerShell)

Set the MySQL credentials in the current PowerShell window (replace the example with your local values):

```powershell
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "your-mysql-password"
$env:CAMPUS_CODE = "your-campus-code"
mvn spring-boot:run
```

Run those commands from this folder, the one containing `pom.xml`.