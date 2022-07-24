# Create protocol

## Downstream

| Opcode | Length | Jagex name           | Description                          |
|-------:|-------:|----------------------|--------------------------------------|
|      2 |      0 | `OK`                 | Create successful                    |
|      3 |      0 | Unknown              | Create server offline                |
|      7 |      0 | `SERVER_FULL`        | Server full                          |
|      9 |      0 | `IP_LIMIT`           | Too many connections from IP address |
|     10 |      0 | Unknown              | Date of birth invalid                |
|     11 |      0 | Unknown              | Date of birth in future              |
|     12 |      0 | Unknown              | Date of birth this year              |
|     13 |      0 | Unknown              | Date of birth last year              |
|     14 |      0 | Unknown              | Country invalid                      |
|     20 |      0 | Unknown              | Name unavailable                     |
|     21 | Custom | Unknown              | Name suggestions                     |
|     22 |      0 | Unknown              | Name invalid                         |
|     30 |      0 | Unknown              | Password invalid                     |
|     31 |      0 | Unknown              | Password invalid                     |
|     32 |      0 | Unknown              | Password guessable                   |
|     33 |      0 | Unknown              | Password guessable                   |
|     34 |      0 | Unknown              | Password too similar to username     |
|     35 |      0 | Unknown              | Password too similar to username     |
|     36 |      0 | Unknown              | Password too similar to username     |
|     37 |      0 | `CLIENT_OUT_OF_DATE` | Client out of date                   |
|     38 |      0 | Unknown              | Cannot create at this time           |
|     41 |      0 | Unknown              | Email invalid                        |
|     42 |      0 | Unknown              | Email invalid                        |
|     43 |      0 | Unknown              | Email invalid                        |

### 2 (`OK`)

### 3 (Create server offline)

**Message:** There was an error contacting the account creation server. Please
try again.

### 7 (`SERVER_FULL`)

**Message:** The server is currently very busy. Please try again shortly.

### 9 (`IP_LIMIT`)

**Message:** You cannot create an account at this time. Please try again later.

### 10 (Date of birth invalid)

**Message:** Please make sure you have provided a valid date of birth.

### 11 (Date of birth in future)

**Message:** The date of birth is invalid, as it is in the future.

### 12 (Date of birth this year)

**Message:** The date of birth is invalid, as it was this year.

### 13 (Date of birth last year)

**Message:** The date of birth is invalid, as it was last year.

### 14 (Country invalid)

**Mesage:** Please make sure you have provided a valid country.

### 20 (Name unavailable)

**Mesage:** That username is unavailable. Please choose again.

### 21 (Name suggestions)

| Data type    | Description                                                 |
|--------------|-------------------------------------------------------------|
| UnsignedByte | Suggested name count (n, must be between 0 and 5 inclusive) |
| Long\[n\]    | Base37-encoded suggested names                              |

**Message (if n is non-zero):** That username is unavailable. Possible
alternatives: \<comma-separated list of suggested names\>.

**Message (if n is zero):** That username is unavailable. We could not suggest a
suitable alternative account name. Please choose again.

### 22 (Name invalid)

**Message:** Please supply a valid username.

### 30 (Password invalid)

**Message:** Please supply a valid password.

### 31 (Password invalid)

**Message:** Please supply a valid password.

### 32 (Password guessable)

**Message:** Your password is too easy to guess.

### 33 (Password guessable)

**Message:** Your password is too easy to guess.

### 34 (Password too similar to username)

**Message:** Your password is too similar to your username.

### 35 (Password too similar to username)

**Message:** Your password is too similar to your username.

### 36 (Password too similar to username)

**Message:** Your password is too similar to your username.

### 37 (`CLIENT_OUT_OF_DATE`)

**Message:** RuneScape has been updated. Please reload this page.

### 38 (Cannot create at this time)

**Message:** You cannot create an account at this time. Please try again later.

### 41 (Email invalid)

**Message:** Please supply a valid email address.

### 42 (Email invalid)

**Message:** Please supply a valid email address.

### 43 (Email invalid)

**Message:** Please supply a valid email address.
