# Phase 6: OAuth & GitHub Signin Implementation

**Status:** Ready for implementation  
**Date:** October 29, 2026  
**Scope:** Add GitHub OAuth signin without breaking existing email/password authentication

---

## Overview

Phase 6 adds OAuth/GitHub signin as an alternative authentication method. The existing email/password flow remains **fully functional and unchanged**.

### Key Principle
**One user can authenticate via multiple methods** (email/password OR GitHub) while maintaining the same application identity and project ownership.

---

## Architecture

### Current System (Phase 5)
```
Registration: email + name + password → User created → JWT issued
Login: email + password → Validate → JWT issued
Dashboard: Token sent in Authorization header
```

### New System (Phase 6)
```
Registration: email + name + password → User created → JWT issued
            OR
Registration: GitHub OAuth → Create/find user → JWT issued

Login: email + password → Validate → JWT issued
    OR
Login: GitHub OAuth → Validate token → JWT issued

Dashboard: Token sent in Authorization header (identical)
```

### Database Changes (Additive)

```sql
-- Add new columns to user table (non-destructive)
ALTER TABLE "user" ADD COLUMN github_id VARCHAR(255) UNIQUE;
ALTER TABLE "user" ADD COLUMN auth_provider VARCHAR(50); -- 'email' or 'github'
ALTER TABLE "user" ADD COLUMN profile_picture_url VARCHAR(500);

-- Create auth_method tracking table (future: for account linking)
CREATE TABLE auth_method (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL,
  provider VARCHAR(50) NOT NULL, -- 'email', 'github', etc
  provider_id VARCHAR(255) NOT NULL, -- github_id, google_id, etc
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE,
  UNIQUE(provider, provider_id)
);
```

---

## Frontend Implementation

### Step 1: Update Login Page

**File:** `frontend-app/src/pages/Login.tsx`

Add GitHub OAuth button:

```typescript
import { useAuth } from '@/contexts/AuthContext'
import { Button } from '@/components/common/Button'

export function Login() {
  const { login, loginWithGitHub } = useAuth()

  const handleGitHubLogin = async () => {
    try {
      await loginWithGitHub()
      // Automatically redirected to Dashboard on success
    } catch (err) {
      console.error('GitHub login failed:', err)
      error('GitHub login failed. Please try again.')
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 to-slate-800">
      {/* ... existing email/password form ... */}

      {/* OAuth Section */}
      <div className="mt-6">
        <div className="relative">
          <div className="absolute inset-0 flex items-center">
            <div className="w-full border-t border-slate-600"></div>
          </div>
          <div className="relative flex justify-center text-sm">
            <span className="px-2 bg-slate-800 text-slate-400">Or continue with</span>
          </div>
        </div>

        <div className="mt-4">
          <Button 
            onClick={handleGitHubLogin}
            className="w-full bg-gray-700 hover:bg-gray-600 text-white flex items-center justify-center gap-2"
          >
            <GitHubIcon size={20} />
            Sign in with GitHub
          </Button>
        </div>
      </div>
    </div>
  )
}
```

### Step 2: Update Register Page

**File:** `frontend-app/src/pages/Register.tsx`

Add GitHub OAuth button to registration:

```typescript
export function Register() {
  const { register, registerWithGitHub } = useAuth()

  const handleGitHubRegister = async () => {
    try {
      await registerWithGitHub()
      // Automatically redirected to Dashboard on success
    } catch (err) {
      console.error('GitHub registration failed:', err)
      error('GitHub registration failed. Please try again.')
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 to-slate-800">
      {/* ... existing email/password form ... */}

      {/* OAuth Section */}
      <div className="mt-6">
        <div className="relative">
          <div className="absolute inset-0 flex items-center">
            <div className="w-full border-t border-slate-600"></div>
          </div>
          <div className="relative flex justify-center text-sm">
            <span className="px-2 bg-slate-800 text-slate-400">Or register with</span>
          </div>
        </div>

        <div className="mt-4">
          <Button 
            onClick={handleGitHubRegister}
            className="w-full bg-gray-700 hover:bg-gray-600 text-white flex items-center justify-center gap-2"
          >
            <GitHubIcon size={20} />
            Sign up with GitHub
          </Button>
        </div>
      </div>
    </div>
  )
}
```

### Step 3: Update AuthContext

**File:** `frontend-app/src/contexts/AuthContext.tsx`

Add OAuth methods:

```typescript
interface AuthContextType {
  user: User | null
  token: string | null
  isAuthenticated: boolean
  isLoading: boolean
  login: (email: string, password: string) => Promise<void>
  register: (email: string, name: string, password: string) => Promise<void>
  loginWithGitHub: () => Promise<void>
  registerWithGitHub: () => Promise<void>
  logout: () => void
}

export function AuthProvider({ children }: { children: ReactNode }) {
  // ... existing code ...

  const loginWithGitHub = async () => {
    try {
      setIsLoading(true)
      
      // Step 1: Redirect to GitHub OAuth (backend handles OAuth flow)
      const state = generateRandomState() // CSRF protection
      localStorage.setItem('oauth_state', state)
      
      const clientId = import.meta.env.VITE_GITHUB_CLIENT_ID
      const redirectUri = `${window.location.origin}/auth/github/callback`
      
      const githubAuthUrl = `https://github.com/login/oauth/authorize?` +
        `client_id=${clientId}&` +
        `redirect_uri=${redirectUri}&` +
        `scope=user:email&` +
        `state=${state}`
      
      window.location.href = githubAuthUrl
    } catch (error) {
      console.error('GitHub login failed:', error)
      setIsLoading(false)
      throw error
    }
  }

  const registerWithGitHub = async () => {
    // Same flow as loginWithGitHub
    // Backend handles auto-creation if user doesn't exist
    return loginWithGitHub()
  }

  return (
    <AuthContext.Provider 
      value={{ 
        user, 
        token, 
        isAuthenticated: !!token, 
        isLoading, 
        login, 
        register, 
        loginWithGitHub,
        registerWithGitHub,
        logout 
      }}
    >
      {children}
    </AuthContext.Provider>
  )
}
```

### Step 4: Add OAuth Callback Route

**File:** `frontend-app/src/pages/AuthGitHubCallback.tsx`

```typescript
import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth } from '@/contexts/AuthContext'
import { useToast } from '@/contexts/ToastContext'
import { LoadingSpinner } from '@/components/common/LoadingSpinner'

export function AuthGitHubCallback() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const { error: errorToast } = useToast()
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const handleCallback = async () => {
      try {
        const code = searchParams.get('code')
        const state = searchParams.get('state')
        const storedState = localStorage.getItem('oauth_state')

        // CSRF protection
        if (state !== storedState) {
          throw new Error('Invalid state parameter')
        }

        if (!code) {
          throw new Error('No authorization code received')
        }

        // Send code to backend for token exchange
        const response = await fetch('/api/auth/github/callback', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ code })
        })

        if (!response.ok) {
          throw new Error('GitHub authentication failed')
        }

        const { token, user } = await response.json()

        // Store auth data (same as email/password flow)
        localStorage.setItem('auth_token', token)
        localStorage.setItem('auth_user', JSON.stringify(user))

        // Redirect to dashboard
        navigate('/dashboard')
      } catch (err) {
        console.error('GitHub callback error:', err)
        setError(err instanceof Error ? err.message : 'Authentication failed')
        errorToast(error)
        
        // Redirect to login after 3 seconds
        setTimeout(() => navigate('/login'), 3000)
      }
    }

    handleCallback()
  }, [searchParams, navigate, errorToast])

  if (error) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-slate-900 to-slate-800 flex items-center justify-center">
        <div className="text-center">
          <h1 className="text-2xl font-bold text-white mb-4">Authentication Error</h1>
          <p className="text-slate-400 mb-4">{error}</p>
          <p className="text-slate-500">Redirecting to login...</p>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 to-slate-800 flex items-center justify-center">
      <div className="text-center">
        <LoadingSpinner />
        <p className="text-white mt-4">Completing GitHub authentication...</p>
      </div>
    </div>
  )
}
```

### Step 5: Add Callback Route to Router

**File:** `frontend-app/src/App.tsx`

```typescript
import { AuthGitHubCallback } from './pages/AuthGitHubCallback'

export function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Existing routes */}
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        
        {/* New OAuth callback */}
        <Route path="/auth/github/callback" element={<AuthGitHubCallback />} />
        
        {/* Protected routes */}
        <Route element={<ProtectedRoute />}>
          <Route path="/dashboard" element={<Dashboard />} />
          {/* ... other protected routes ... */}
        </Route>
      </Routes>
    </BrowserRouter>
  )
}
```

### Step 6: Environment Variables

**File:** `frontend-app/.env.local`

```env
VITE_GITHUB_CLIENT_ID=your_github_client_id_here
VITE_API_BASE_URL=http://localhost:8081/api
```

---

## Backend Implementation

### Step 1: Add GitHub OAuth Dependencies

**File:** `secureops/pom.xml`

```xml
<!-- GitHub OAuth verification -->
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
    <version>4.11.0</version>
</dependency>

<!-- JSON processing -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>
```

### Step 2: Add OAuth Configuration

**File:** `secureops/src/main/resources/application.properties`

```properties
# GitHub OAuth
app.github.client-id=${GITHUB_CLIENT_ID:}
app.github.client-secret=${GITHUB_CLIENT_SECRET:}
app.github.redirect-uri=${GITHUB_REDIRECT_URI:http://localhost:5173/auth/github/callback}
```

### Step 3: Create GitHub OAuth Service

**File:** `secureops/src/main/java/com/secureops/auth/service/GitHubOAuthService.java`

```java
package com.secureops.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class GitHubOAuthService {

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${app.github.client-id}")
    private String clientId;

    @Value("${app.github.client-secret}")
    private String clientSecret;

    /**
     * Exchange GitHub authorization code for access token
     */
    public String exchangeCodeForToken(String code) throws IOException {
        String url = "https://github.com/login/oauth/access_token";

        RequestBody body = new FormBody.Builder()
            .add("client_id", clientId)
            .add("client_secret", clientSecret)
            .add("code", code)
            .build();

        Request request = new Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Accept", "application/json")
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to exchange code for token: " + response.code());
            }

            JsonNode jsonNode = objectMapper.readTree(response.body().string());
            return jsonNode.get("access_token").asText();
        }
    }

    /**
     * Get GitHub user info using access token
     */
    public GitHubUserInfo getUserInfo(String accessToken) throws IOException {
        String url = "https://api.github.com/user";

        Request request = new Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer " + accessToken)
            .addHeader("Accept", "application/vnd.github.v3+json")
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to get user info: " + response.code());
            }

            return objectMapper.readValue(response.body().string(), GitHubUserInfo.class);
        }
    }

    /**
     * Get GitHub user email
     */
    public String getUserEmail(String accessToken) throws IOException {
        String url = "https://api.github.com/user/emails";

        Request request = new Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer " + accessToken)
            .addHeader("Accept", "application/vnd.github.v3+json")
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to get user email: " + response.code());
            }

            JsonNode[] emails = objectMapper.readValue(
                response.body().string(), 
                JsonNode[].class
            );

            // Get primary email or first email
            for (JsonNode email : emails) {
                if (email.get("primary").asBoolean()) {
                    return email.get("email").asText();
                }
            }

            return emails.length > 0 ? emails[0].get("email").asText() : "";
        }
    }

    /**
     * GitHub user info DTO
     */
    public static class GitHubUserInfo {
        public Long id;
        public String login;
        public String name;
        public String avatar_url;
    }
}
```

### Step 4: Update AuthController

**File:** `secureops/src/main/java/com/secureops/auth/AuthController.java`

```java
@PostMapping("/github/callback")
public ResponseEntity<AuthResponse> githubCallback(
    @RequestBody GitHubCallbackRequest request) {
    
    log.info("GitHub OAuth callback received");

    try {
        // Exchange code for token
        String accessToken = gitHubOAuthService.exchangeCodeForToken(request.getCode());

        // Get user info from GitHub
        GitHubOAuthService.GitHubUserInfo githubUser = gitHubOAuthService.getUserInfo(accessToken);
        String email = gitHubOAuthService.getUserEmail(accessToken);

        if (email == null || email.isEmpty()) {
            throw new InvalidCredentialsException("Could not retrieve email from GitHub");
        }

        // Create or find user
        User user = userService.findByEmail(email)
            .orElseGet(() -> {
                // Create new user from GitHub profile
                User newUser = new User(
                    email,
                    githubUser.name != null ? githubUser.name : githubUser.login,
                    UUID.randomUUID().toString(), // Random password (not used)
                    UserRole.USER
                );
                newUser.setGithubId(githubUser.id.toString());
                newUser.setProfilePictureUrl(githubUser.avatar_url);
                return userService.saveUser(newUser);
            });

        // Verify account is enabled
        if (!userService.isUserEnabled(user)) {
            throw new InvalidCredentialsException("Account disabled");
        }

        // Generate JWT token
        String jwtToken = jwtService.generateToken(user.getId(), user.getEmail());

        // Build response
        AuthResponse response = AuthResponse.builder()
            .id(user.getId())
            .email(user.getEmail())
            .name(user.getName())
            .role(user.getRole().toString())
            .enabled(user.getEnabled())
            .token(jwtToken)
            .message("GitHub login successful")
            .build();

        return ResponseEntity.ok(response);

    } catch (IOException e) {
        log.error("GitHub OAuth error:", e);
        throw new InvalidCredentialsException("GitHub authentication failed");
    }
}
```

### Step 5: Update User Entity

**File:** `secureops/src/main/java/com/secureops/user/User.java`

```java
@Entity
@Table(name = "\"user\"")
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // ... existing fields ...

    @Column(unique = true, nullable = true)
    private String githubId;

    @Column(nullable = true)
    private String profilePictureUrl;

    // Getters and setters
    public String getGithubId() {
        return githubId;
    }

    public void setGithubId(String githubId) {
        this.githubId = githubId;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }
}
```

### Step 6: Database Migration

**File:** `secureops/src/main/resources/db/migration/V004__add_oauth_fields.sql`

```sql
-- Add OAuth support to user table (non-destructive)
ALTER TABLE "user" ADD COLUMN github_id VARCHAR(255) UNIQUE;
ALTER TABLE "user" ADD COLUMN profile_picture_url VARCHAR(500);

-- Create index for github_id lookups
CREATE INDEX idx_user_github_id ON "user"(github_id);
```

---

## GitHub OAuth Setup (Prerequisites)

### Step 1: Create GitHub OAuth Application

1. Go to: https://github.com/settings/developers
2. Click "New OAuth App"
3. Fill in:
   - **Application name:** SecureOps AI
   - **Homepage URL:** http://localhost:5173
   - **Authorization callback URL:** http://localhost:5173/auth/github/callback
4. Copy **Client ID** and **Client Secret**

### Step 2: Set Environment Variables

**For Backend:**
```bash
$env:GITHUB_CLIENT_ID = "your_client_id"
$env:GITHUB_CLIENT_SECRET = "your_client_secret"
```

**For Frontend:**
Add to `frontend-app/.env.local`:
```
VITE_GITHUB_CLIENT_ID=your_client_id
```

---

## Testing Phase 6 (OAuth Implementation)

### UI Testing Checklist

- [ ] Login page shows "Sign in with GitHub" button
- [ ] Register page shows "Sign up with GitHub" button
- [ ] Clicking GitHub button redirects to GitHub login
- [ ] After GitHub auth, redirected back to dashboard
- [ ] User logged in successfully
- [ ] Same JWT token used (no difference to app)
- [ ] Projects still work (same as email/password users)
- [ ] Email/password auth still works (no breaking changes)

### Integration Testing

- [ ] User 1 registers via email/password
- [ ] User 2 registers via GitHub
- [ ] Both users can create projects
- [ ] Projects are properly isolated (IDOR still working)
- [ ] New GitHub user auto-created with correct profile
- [ ] Existing email user can NOT switch to GitHub (prevents hijacking)

### Error Handling

- [ ] User denies GitHub authorization → error message
- [ ] GitHub API fails → graceful error
- [ ] Invalid code → error message
- [ ] CSRF state mismatch → error message

---

## Breaking Changes: NONE ✅

✅ Email/password authentication works exactly the same  
✅ JWT tokens are identical  
✅ Projects/ownership/IDOR prevention unchanged  
✅ API contracts unchanged  
✅ No database schema breaking changes  
✅ Old users can continue with email/password  
✅ New users can choose either method

---

## Summary

**Phase 6 adds GitHub OAuth without breaking anything.**

| Aspect | Status |
|--------|--------|
| Email/password auth | ✅ Still works |
| JWT tokens | ✅ Same format |
| Project ownership | ✅ Unchanged |
| IDOR prevention | ✅ Unchanged |
| Authorization checks | ✅ Unchanged |
| Dashboard access | ✅ Same flow |
| API compatibility | ✅ 100% compatible |

