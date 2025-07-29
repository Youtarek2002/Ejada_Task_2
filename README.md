# Library Management Console App (Dockerized)

A simple Java console-based Library Management App using **Docker**, **MySQL**, and **Adminer**.

---

## Prerequisites

- Docker
- Docker Compose

---

## Setup Instructions

```bash
docker-compose up --build
```

---

## Environment Variables

- `DB_HOST` = db  
- `DB_PORT` = 3306  
- `DB_USER` = appuser  
- `DB_PASSWORD` = apppassword  
- `DB_NAME` = library_db  

---

## Access

- **Application:** Runs in the console (`docker-compose up` logs)  
- **Adminer:** [http://localhost:8080](http://localhost:8080)  
  - System: MySQL  
  - Server: db  
  - Username: appuser  
  - Password: apppassword  
  - Database: library_db
