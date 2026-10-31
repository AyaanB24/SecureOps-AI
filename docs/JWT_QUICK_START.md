# JWT Authentication Quick Start Guide

## Setup

### 1. Start the Backend
```bash
export APP_JWT_SECRET="my-super-secret-key-32-characters-minimum!"
cd secureops
java -jar target/secureops-0.0.1-SNAPSHOT.jar
```

Backend will start on `http://localhost:8081`

### 2. Start the Frontend
```bash
cd frontend-app
npm run dev
```

Frontend will start on `http://localhost:5173`

## API Endpoints

### Register (Create New Account)
```bash
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "newuser@example.com",
    "name": "New User",
    "password": "SecurePassword123"
  }'
```

**Response**:
```json
{
  "id": "uuid-here",
  "email": "newuser@example.com",
  "name": "New User",
  "role": "USER",
  "enabled": true,
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1dWlkLWhlcmUiLCJlbWFpbCI6Im5ld3VzZXJAZXhhbXBsZS5jb20iLCJyb2xlIjoiVVNFUiIsImlhdCI6MTY5ODUxNjAwMCwiZXhwIjoxNjk4NTE5NjAwfQ.signature-here",
  "message": "User registered successfully"
}
```

### Login (Existing Account)
```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "newuser@example.com",
    "password": "SecurePassword123"
  }'
```

**Response**: Same as register (includes JWT token)

### Use Token for Protected Endpoints
Store the token from registration/login response, then include it in the `Authorization` header:

```bash
curl -X GET http://localhost:8081/api/projects \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

## Token Structure

### JWT Format
A JWT has 3 parts separated by dots:
```
header.payload.signature
```

### Example Payload (decoded)
```json
{
  "sub": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "role": "USER",
  "iat": 1698516000,
  "exp": 1698519600
}
```

**Fields**:
- `sub`: User ID (subject)
- `email`: User email
- `role`: User role (USER, ADMIN, etc.)
- `iat`: Issued at (Unix timestamp)
- `exp`: Expiration time (Unix timestamp) - **1 hour from issue**

## Error Responses

### Invalid Credentials
```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"wrong@example.com","password":"wrong"}'
```

**Response** (401):
```json
{
  "code": "INVALID_CREDENTIALS",
  "message": "Invalid email or password"
}
```

### Missing JWT Token
```bash
curl -X GET http://localhost:8081/api/projects
# No Authorization header
```

**Response** (401):
```
Unauthorized: JWT token required
```

### Expired Token
```bash
# After 1 hour has passed since token issue
curl -X GET http://localhost:8081/api/projects \
  -H "Authorization: Bearer expired-token-here"
```

**Response** (401):
```
Unauthorized: JWT token required
```

### Invalid Token Format
```bash
curl -X GET http://localhost:8081/api/projects \
  -H "Authorization: Bearer invalid"
```

**Response** (401):
```
Unauthorized: JWT token required
```

## Common Issues

### Issue: "Cannot load property 'APP_JWT_SECRET'"
**Solution**: Set the environment variable before starting the app
```bash
export APP_JWT_SECRET="your-secret-key"
java -jar target/secureops-0.0.1-SNAPSHOT.jar
```

### Issue: Token works then stops working
**Cause**: Token expired (1 hour lifetime)
**Solution**: Login again to get a new token

### Issue: Getting 401 on protected endpoints
**Possible causes**:
1. No `Authorization` header in request
2. Token not prefixed with "Bearer " (with space)
3. Token is expired
4. Token was generated with different secret

### Issue: "VALIDATION_ERROR" on registration
**Cause**: Invalid input data
**Check**:
- Email format is valid (must have @domain.com)
- Password is at least 8 characters
- All required fields are provided

## Testing with Postman

### 1. Set up Environment Variable
```
{{jwt_token}} = [from register/login response]
{{base_url}} = http://localhost:8081
```

### 2. Register Request
```
POST {{base_url}}/api/auth/register
Content-Type: application/json

{
  "email": "postman@test.com",
  "name": "Postman User",
  "password": "TestPass123"
}
```

Then extract token from response and save to `{{jwt_token}}`

### 3. Use Token in Protected Request
```
GET {{base_url}}/api/projects
Authorization: Bearer {{jwt_token}}
```

## Public Endpoints (No Token Needed)
- `POST /api/auth/register` - Create account
- `POST /api/auth/login` - Get token
- `GET /api/health` - Health check

## Protected Endpoints (JWT Required)
- `GET /api/projects` - List projects
- `POST /api/projects` - Create project
- `GET /api/policies` - List policies
- `POST /api/policies` - Create policy
- All other `/api/**` endpoints

## Security Best Practices

1. **Store Token Securely**: Use httpOnly cookies in production, not localStorage
2. **Include Bearer Prefix**: Always use `Bearer <token>`, not just `<token>`
3. **Use HTTPS**: Always use HTTPS in production (this guide uses HTTP for local testing)
4. **Rotate Secrets**: Change `APP_JWT_SECRET` periodically
5. **Don't Log Tokens**: Never log full tokens in production
6. **Set Short Expiration**: 1 hour is reasonable; adjust based on security requirements

## Token Lifetime

- **Generated**: When user registers or logs in
- **Valid For**: 1 hour (3600000 milliseconds)
- **After Expiration**: Must login again to get new token
- **Not Refreshable**: No refresh token in Phase 4 (coming Phase 6)

---

**Next Steps**:
- Phase 5: Add role-based access control (different permissions for ADMIN vs USER)
- Phase 6: Add refresh tokens and logout
- Phase 7: Add Jenkins API key authentication
