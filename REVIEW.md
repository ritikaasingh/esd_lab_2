# REVIEW.md — AI Output Review Log

Complete **one review entry per slice** before moving to the next.


---

## Slice 1 — Signup (S1)

### A. Bucket Check
List every class and method the AI produced. Tag each one:
`Controller` / `Service` / `Repository` / `DTO` / `Security` / `Config` / `Exception`

| File / Method | Tag | In Allowed List? |
|---|---|---|
| | | |

> **Important:** Anything **not** in the allowed list → **reject, re-prompt**. Don't keep it because it works.

### B. Contract Check
Compare against API contract row A1 line by line.

| Check | Pass / Fail |
|---|---|
| Path is `POST /api/auth/signup` | |
| Returns **201** | |
| Response shape: `{ id, email, fullName, role, createdAt }` | |
| No `passwordHash` field in response | |
| Role in response is always `STUDENT` | |

### C. Validation & Errors

| Check | Pass / Fail |
|---|---|
| `@Valid` on controller parameter | |
| `@Email`, `@NotBlank`, `@Size` on DTO fields | |
| Duplicate email throws `DuplicateResourceException` | |
| No new error/response wrapper class invented | |
| No stack trace or exception message leaked to client | |

### D. Security Specifics

| Check | Pass / Fail |
|---|---|
| Password hashed via injected `PasswordEncoder` (not `new BCryptPasswordEncoder()`) | |
| `SignupRequest` has no `role` field, or it is ignored | |
| Duplicate check uses normalised (lowercased/trimmed) email | |
| AI did **not** touch `SecurityConfig`, add JWT class, or add `UserDetailsService` | |

### E. Understanding Questions
*Answer in your own words (1–2 lines each)*

1. Where exactly is the password hashed, and why is the same password stored as a different string for two users?

   > The password is hashed in AuthService.signup() using the injected PasswordEncoder. BCrypt generates a salt, so the same password can produce different hashes while both remain verifiable.

2. What would happen if `SignupRequest` accepted a `role` field and the AI used it?

   > A client could submit role ADMIN and gain privileges if the service trusted the request. The service must assign Role.STUDENT on the server and ignore any submitted role.

3. Which class turns `DuplicateResourceException` into a 409 response?

   > GlobalExceptionHandler handles DuplicateResourceException and returns HTTP 409 Conflict.

### F. Decision

| File | Decision | Reason (if modified/rejected) |
|---|---|---|
| | Accepted / Modified / Rejected | |

---

## Slice 2 — Login & JWT (S2)

### A. Bucket Check

| File / Method | Tag | In Allowed List? |
|---|---|---|
| | | |

### B. Contract Check

| Check | Pass / Fail |
|---|---|
| Path is `POST /api/auth/login` | |
| Returns **200** | |
| Response: `{ accessToken, tokenType: "Bearer", expiresIn }` | |
| `expiresIn` matches configured value (not hardcoded) | |

### C. Validation & Errors

| Check | Pass / Fail |
|---|---|
| Same 401 message for unknown email and wrong password | |
| No `try/catch` swallowing exceptions silently | |

### D. Security Specifics

| Check | Pass / Fail |
|---|---|
| JWT secret comes from `@Value`, no string literal secret | |
| Uses jjwt 0.12 API: `.subject()`, `.claim()`, `.id()`, `.issuedAt()`, `.expiration()`, `.signWith(key)` | |
| Rejects deprecated: `setSubject`, `setExpiration`, `signWith(SignatureAlgorithm.HS256, secret)`, `Jwts.parserBuilder()` | |
| `jti` is a random UUID (not the user id) | |
| No password or hash in token claims | |
| Parse/validate method **not** written yet (that is S3) | |

### E. Understanding Questions

1. Why must the error message be identical for "no such email" and "wrong password"?

   > Using the same message prevents an attacker from learning whether an email address is registered by comparing login errors.

2. Who can read the claims inside your JWT? Who can change them?

   > JWT claims are encoded and readable by anyone holding the token; they are not encrypted. Changing a claim invalidates the signature unless the attacker can sign with the trusted secret key.

3. What does `jti` give us that `sub` doesn't? (Think about S5.)

   > sub identifies the user, so multiple tokens for one user share that subject. jti uniquely identifies each token, allowing one token to be revoked without revoking every token belonging to the user.

### F. Decision

| File | Decision | Reason |
|---|---|---|
| | | |

---

## Slice 3 — JWT Filter & Protected API (S3)

### A. Bucket Check

| File / Method | Tag | In Allowed List? |
|---|---|---|
| | | |

### B. Contract Check

| Check | Pass / Fail |
|---|---|
| `GET /api/courses` works **without** token | |
| `GET /api/users/me` returns **401** with no token | |
| `GET /api/users/me` returns **200** with valid token | |
| 401 body is an `ApiError` JSON object | |

### C. Validation & Errors

| Check | Pass / Fail |
|---|---|
| Filter does not write a response on error — continues chain | |
| Filter clears SecurityContext on bad token | |

### D. Security Specifics

| Check | Pass / Fail |
|---|---|
| Spring Security 6 style: lambda DSL, `requestMatchers`, `authorizeHttpRequests` | |
| Rejected: `WebSecurityConfigurerAdapter`, `antMatchers`, `authorizeRequests`, `.and()` | |
| `permitAll` covers only the three specific public endpoints | |
| `GET /api/courses` is public but `POST /api/courses` is NOT (watch for broad matcher) | |
| Signature + expiry verified with `Jwts.parser().verifyWith(key).build().parseSignedClaims(token)` | |
| Authorities use `ROLE_` prefix | |
| Entry point and access-denied handler wired via `exceptionHandling(...)` | |
| No `UserDetailsService`, `AuthenticationManager`, sessions or CORS added | |
| `GET /api/users/me` returns `UserResponse`, not the entity | |

### E. Understanding Questions

1. Why is CSRF protection disabled here, and when would disabling it be wrong?

   > CSRF is disabled because this API uses stateless Bearer tokens supplied in the Authorization header rather than automatically attached session cookies. Disabling CSRF can be unsafe when browser cookies or session authentication are used.

2. Walk through what happens to a request with an expired token, step by step, until the client sees 401.

   > JwtService verifies the token signature and expiration while parsing claims. An expired token causes a JWT exception; the filter clears the security context and continues. Protected-route authorization then invokes the authentication entry point, returning 401.

3. Why don't 401/403 errors from the filter chain reach `GlobalExceptionHandler`?

   > GlobalExceptionHandler handles exceptions from the MVC/controller layer. Authentication and authorization failures in the Spring Security filter chain are handled by the configured authentication entry point and access-denied handler.

### F. Decision

| File | Decision | Reason |
|---|---|---|
| | | |

---

## Slice 4 — Role Restriction (S4)

### A. Bucket Check

| File / Method | Tag | In Allowed List? |
|---|---|---|
| | | |

### B. Contract Check

| Check | Pass / Fail |
|---|---|
| `POST /api/courses` with no token → **401** | |
| `POST /api/courses` with STUDENT token → **403** | |
| `POST /api/courses` with ADMIN token → **201** | |
| 403 body is an `ApiError` JSON object | |

### D. Security Specifics

| Check | Pass / Fail |
|---|---|
| Role naming consistent: `hasRole("ADMIN")` ↔ authority `ROLE_ADMIN` | |
| If `@PreAuthorize` used: `@EnableMethodSecurity` is present | |
| Rule placed **before** `anyRequest().authenticated()` if in SecurityConfig | |
| Diff is small — AI did not rewrite the whole config | |

### E. Understanding Questions

1. A STUDENT calls `POST /api/courses`. Why does the server return 403 and not 401?

   > The student is authenticated but lacks ROLE_ADMIN, so access is forbidden and the response is 403. A 401 means authentication is missing or invalid.

2. Where does the role in the JWT come from, and can the client change it?

   > JwtService creates the role claim from the persisted user's role when issuing the token. A client cannot change a signed claim and still pass signature validation. The filter also derives its granted authority from the user's stored database role.

### F. Decision

| File | Decision | Reason |
|---|---|---|
| | | |

---

## Slice 5 — Logout (S5) *(stretch)*

### A. Bucket Check

| File / Method | Tag | In Allowed List? |
|---|---|---|
| | | |

### B. Contract Check

| Check | Pass / Fail |
|---|---|
| `POST /api/auth/logout` returns **204** | |
| Same token returns **401** after logout | |
| Fresh login after logout returns a new working token | |

### D. Security Specifics

| Check | Pass / Fail |
|---|---|
| Logout adds entry to server-side denylist (not just tells client to delete token) | |
| Denylist keyed by **jti**, not email | |
| Map is `ConcurrentHashMap` (thread-safe) | |
| Logout endpoint is NOT under `permitAll` | |
| Filter checks the denylist and treats denylisted jti as invalid | |
| (Bonus) Expired entries are removed from the map | |

### E. Understanding Questions

1. Why does logout need server-side state if JWT is "stateless"?

   > JWT authentication does not normally keep a server-side session, so the server needs a denylist to reject a token before its natural expiry after logout.

2. What happens to the denylist when the server restarts? What if there are two server instances?

   > An in-memory denylist is cleared when the process restarts. Multiple server instances have separate maps, so consistent revocation across instances requires shared storage.

3. Why store the expiry time with each jti?

   > The expiry lets the application remove expired denylist entries because tokens that have already expired cannot be used. It also prevents expired entries from accumulating indefinitely.

### F. Decision

| File | Decision | Reason |
|---|---|---|
| | | |

---

## No-AI Change Tasks

**Files changed and purpose:**

| File | What changed and why |
|---|---|
| `src/main/resources/application.yml` | Set JWT expiry to 900 seconds (15 minutes). The login response now reports the configured duration. |
| `src/main/java/com/esd/lab2/controller/CourseController.java` | Added `GET /api/courses/{id}` using the existing service lookup; a missing course uses `ResourceNotFoundException` and returns 404. |
| `src/main/java/com/esd/lab2/controller/UserController.java` | Added `PUT /api/users/me` for the authenticated user's profile. The request updates only `fullName`. |
| `src/main/java/com/esd/lab2/dto/UpdateProfileRequest.java` | Added a validated request DTO for `fullName`. |
| `src/main/java/com/esd/lab2/dto/SignupRequest.java` | Added password validation requiring at least one digit, with a field-level validation error. |
| `src/main/java/com/esd/lab2/config/SecurityConfig.java` | Removed public access for the course collection endpoint while retaining the public individual-course endpoint and ADMIN-only course creation. |

**Explanation and test impact:**

These changes were made with AI assistance during this chat; this record does not claim they were completed independently. The course collection now returns 401 without a token and should return 200 to an authenticated user. Tests that previously expected anonymous `GET /api/courses` to succeed must change. `GET /api/courses/{id}` remains public; existing and missing IDs should yield 200 and 404 respectively. Profile update requires authentication, password-without-digit signup returns 400, and the JWT lifetime is 900 seconds.

Review this section against your actual source changes before submission.

## Final-state clarification

The S3 review records the behavior at that stage, when `GET /api/courses` was public. The later Task 5 change altered the final access rule: `GET /api/courses` now requires a valid token, while `GET /api/courses/{id}` remains public. The final runtime checks confirmed 401 without a token and 200 with a valid token for the course list.

**Process note:** The five additional changes described in the No-AI section were completed with AI assistance in this conversation. This review records the implemented behavior and observed test results; it does not claim those changes were completed independently.
