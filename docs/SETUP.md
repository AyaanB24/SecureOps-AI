# SecureOps Setup Instructions

## Prerequisites

- **Java 21+** - [Download JDK 21](https://www.oracle.com/java/technologies/downloads/#java21)
- **Maven 3.8+** - [Download Maven](https://maven.apache.org/download.cgi)
- **PostgreSQL 14+** - [Download PostgreSQL](https://www.postgresql.org/download/)
- **Git** - [Download Git](https://git-scm.com/download)

## Database Setup

### Create Database
```bash
psql -U postgres
postgres=# CREATE DATABASE secureops;
postgres=# \q
```

### Verify Connection
```bash
psql -U postgres -d secureops
psql (14.x)
Type "help" for help.

secureops=#
```

## Application Setup

### Clone Repository
```bash
git clone <repository-url>
cd SecureOps-AI
```

### Build Project
```bash
cd secureops
mvn clean package -DskipTests
```

### Run Application
```bash
java -jar target/secureops-0.0.1-SNAPSHOT.jar
```

**Expected output (last few lines):**
```
Tomcat started on port(s): 8080 (http) with context path '/'
Started SecureopsApplication in X.XXX seconds
```

## Verification

### Health Check
```bash
curl http://localhost:8080/api/health
```

**Expected response:**
```json
{"status":"UP","service":"SecureOps","database":"UP"}
```

### Using Postman
1. Import collection (if available)
2. Set environment base URL to `http://localhost:8080`
3. Run `GET /api/health`

## Troubleshooting

### Application won't start
- Check Java 21 is installed: `java -version`
- Check Maven is installed: `mvn -version`
- Rebuild: `mvn clean compile`

### Database connection fails
- Verify PostgreSQL is running: `psql -U postgres`
- Verify database exists: `psql -l | grep secureops`
- Check credentials in `application.properties`

### Port 8080 already in use
- Change port in `application.properties`: `server.port=8081`
- Or kill process: `lsof -i :8080` then `kill -9 <PID>`

## IDE Setup

### IntelliJ IDEA
1. Open project folder
2. File → Open → Select `SecureOps-AI`
3. Maven will auto-detect and index project
4. Run → Edit Configurations → Add Spring Boot config
5. Main class: `com.secureops.SecureopsApplication`

### VS Code
1. Install extensions:
   - Extension Pack for Java (Microsoft)
   - Spring Boot Extension Pack (VMware)
2. Open folder: `SecureOps-AI`
3. Wait for project indexing
4. Use Spring Boot Dashboard to run application

## Environment Variables (Optional)

Create `.env` file in project root:
```
DB_URL=jdbc:postgresql://localhost:5432/secureops
DB_USER=postgres
DB_PASSWORD=postgres
SERVER_PORT=8080
```

## Next Steps
- Read `README.md` for architecture overview
- Check `docs/PHASE_0.md` for implementation details
- Review Phase 1 requirements (coming soon)
