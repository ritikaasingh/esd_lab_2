# PROMPTS.md — AI Prompt Log

Record every prompt you send to the AI **starting from Slice 2**.
Slice 1 is done manually — no AI. See `README.md` for the prompt template structure.
> **Documentation note:** The S2–S5 entries below are retrospective reconstructions from the implementation and conversation, not verbatim contemporaneous prompt records.


> **Rules:**
> - One prompt (+ follow-ups) per slice
> - Use the structured template from the README — free-form prompts are not allowed
> - Do not back-fill this file after the lab

---

## Example — How a good S1 prompt looks (for reference only, S1 is done manually)

```
CONTEXT
Spring Boot 3.x, Spring Security 6, Java 17+, jjwt 0.12.x. No other libraries.
Package layout:
  com.esd.lab2
  ├── config/       SecurityConfig.java, AdminSeeder.java
  ├── controller/   CourseController.java
  ├── service/      CourseService.java
  ├── dto/          CourseRequest.java, CourseResponse.java
  ├── entity/       User.java, Role.java (enum), Course.java
  ├── repository/   UserRepository.java, CourseRepository.java
  ├── exception/    ApiError.java, GlobalExceptionHandler.java,
  │                 DuplicateResourceException.java, InvalidCredentialsException.java,
  │                 ResourceNotFoundException.java
  └── security/     JsonAuthenticationEntryPoint.java, JsonAccessDeniedHandler.java


TASK
Implement only POST /api/auth/signup.
Request: SignupRequest { email (valid email, required), password (required, 8–64 chars),
         fullName (required, not blank, max 100) }.
Response: 201 with UserResponse { id, email, fullName, role, createdAt }.
The new user's role is always STUDENT, even if the request contains a role field.
Store email trimmed and lowercased. Hash the password with the existing PasswordEncoder bean.
If the email already exists, throw DuplicateResourceException.

ALLOWED TO CREATE OR CHANGE
dto/SignupRequest.java, dto/UserResponse.java,
service/AuthService.java (signup method only),
controller/AuthController.java (signup endpoint only)

RULES
- Do not create or modify any file not listed above.
- Use the existing exceptions in com.esd.lab2.exception. Do not create new error classes.
- Use DTOs for request and response. Never return an entity from a controller.
- Use jakarta.validation annotations for input validation.
- Use only APIs that exist in Spring Security 6 and jjwt 0.12. No deprecated methods.
- If something is unclear, add a TODO comment instead of guessing.

OUTPUT
Each file in full, with its path. Then a list of anything you assumed.
```

---

## Slice 2 — Login & JWT (S2)

**Prompt 1:**

```
CONTEXT
Spring Boot 3.x, Spring Security 6, Java 17+, JJWT 0.12.x. No new dependencies.
Use the existing User, Role, UserRepository, PasswordEncoder, exception classes and package layout.

TASK
Implement only login and JWT issuing. Login accepts email and password and returns HTTP 200 with LoginResponse { accessToken, tokenType: "Bearer", expiresIn }. Normalize email; unknown email and wrong password must return the same 401 message, "Invalid email or password". JWT claims must contain sub=email, role, random UUID jti, iat and exp and be signed with HS256.

ALLOWED TO CREATE OR CHANGE
LoginRequest.java, LoginResponse.java, JwtService.java, AuthService.java (login only), AuthController.java (login only).

RULES
Use DTOs and existing exceptions. Validate input. Use the JJWT 0.12 API and the configured secret. Do not modify unrelated files.

OUTPUT
Each changed file in full, with its path, assumptions and review notes.
```

**Follow-up (if any):**

```
Retrospective follow-up summary: the response property was aligned to accessToken, and JWT generation was updated to include a UUID jti and explicit HS256 signing.
```

---

## Slice 3 — JWT Filter & Protected API (S3)

**Prompt 1:**

```
CONTEXT
Spring Boot 3.x, Spring Security 6, Java 17+, JJWT 0.12.x. Reuse the existing JSON authentication entry point and access-denied handler.

TASK
Implement JWT parsing and verification, a OncePerRequestFilter that handles Bearer tokens and sets ROLE_ authorities, stateless security configuration, and GET /api/users/me returning UserResponse. Keep POST signup, POST login and GET /api/courses public for this slice; protect other endpoints.

ALLOWED TO CREATE OR CHANGE
JwtService.java, JwtAuthenticationFilter.java, SecurityConfig.java, UserController.java.

RULES
Use SecurityFilterChain and the lambda DSL. Verify token signatures and expiry. On invalid tokens clear the context and continue the chain. Do not return User entities or add dependencies.

OUTPUT
Each changed file in full, with its path, assumptions and review notes.
```

**Follow-up (if any):**

```
Retrospective follow-up summary: after access testing, the existing JSON entry point and access-denied handler were explicitly wired so authentication failures return 401 and authenticated-but-forbidden requests return 403.
```

---

## Slice 4 — Role Restriction (S4)

**Prompt 1:**

```
CONTEXT
The existing Spring Security 6 filter provides ROLE_ authorities and CourseController implements GET and POST /api/courses.

TASK
Restrict POST /api/courses to ROLE_ADMIN while keeping GET /api/courses public for this slice. Keep unauthenticated requests at 401 and authenticated non-admin requests at 403.

ALLOWED TO CREATE OR CHANGE
SecurityConfig.java only, unless a minimal handler wiring change is required.

RULES
Use requestMatchers(HttpMethod.POST, "/api/courses").hasRole("ADMIN") before anyRequest().authenticated(). Preserve the existing JSON handlers and unrelated configuration.

OUTPUT
Changed file in full, followed by assumptions and a list of verification cases.
```

**Follow-up (if any):**

```
Retrospective follow-up summary: student course creation initially returned 401 instead of 403. The security configuration was reviewed and the existing JsonAuthenticationEntryPoint and JsonAccessDeniedHandler were wired. Retesting produced 403 for students and 201 for admins.
```

---

## Slice 5 — Logout (stretch, S5)

**Prompt 1 — retrospective reconstruction, not verbatim history:**

```text
CONTEXT
Spring Boot 3.2.5, Spring Security 6, Java 17+, PostgreSQL, Maven and
JJWT 0.12.x. Do not add dependencies. Reuse the existing com.esd.lab2
authentication classes, JwtService, JwtAuthenticationFilter and JSON
security handlers.

TASK
Implement POST /api/auth/logout and server-side JWT revocation. Verify
the Bearer JWT, extract jti and expiry from verified claims, and revoke
the token in a thread-safe in-memory denylist. Logout returns 204;
reusing the revoked token on a protected endpoint must return 401.

ALLOWED TO CREATE OR CHANGE
- security/TokenDenylist.java
- controller/AuthController.java (logout endpoint)
- security/JwtAuthenticationFilter.java (denylist check)

RULES
Use ConcurrentHashMap. Implement revoke(jti, expiry), isRevoked(jti),
and cleanExpired(). Key revocation by jti, not email or the full JWT.
Preserve signup and login. Add no dependencies. Report test results honestly.

OUTPUT
List changed files, assumptions, limitations, and actual test results.
```

**Follow-up — retrospective reconstruction, not verbatim history:**

```text
Compare the implementation with the README. The initial denylist used
the complete JWT as its key. Change it to use jti and add expired-entry
cleanup. Verify logout returns 204 and reuse of the revoked token returns
401. Report only tests that were actually run.
```

> Documentation note: These prompts are retrospective reconstructions from the implementation and conversation, not verbatim historical prompts.
