# Login protocol

## Upstream

| Opcode | Length         | Jagex name                  | Description                        |
|-------:|---------------:|-----------------------------|------------------------------------|
|     14 |              1 | `INIT_GAME_CONNECTION`      | Set username hash                  |
|     15 |              4 | `INIT_JS5REMOTE_CONNECTION` | Switch to JS5 mode                 |
|     16 | Variable short | `GAMELOGIN`                 | Login (new session)                |
|     17 |              0 | Unknown                     | Switch to JAGGRAB mode             |
|     18 | Variable short | Unknown                     | Reconnect (existing session)       |
|     20 |              6 | Unknown                     | Check date of birth and country    |
|     21 |              8 | `CREATE_CHECK_NAME`         | Check username availability        |
|     22 |  Variable byte | `CREATE_ACCOUNT`            | Create account                     |
|     23 |              4 | `REQUEST_WORLDLIST`         | Request world list                 |
|     24 |  Variable byte | `CHECK_WORLD_SUITABILITY`   | Request most suitable world number |

A curious oddity is that Old School RuneScape server processes upstream login
packets in a loop, rather than only permitting a single login packet to be sent
during the handshake process, which is how most current private servers are
currently implemented.

For example, it is possible to send an `INIT_GAME_CONNECTION` packet followed by
an `INIT_JS5REMOTE_CONNECTION` packet. The connection will successfully switch
to JS5 mode, even though this is not the normal sequence of packets sent by the
client.

### 14 (`INIT_GAME_CONNECTION`)

| Data type    | Description   |
|--------------|---------------|
| UnsignedByte | Username hash |

The following algorithm computes the username hash:

    usernameHash = (encodedUsername >> 16) & 0x1F

where `encodedUsername` is the player's Base37-encoded username.

The consensus in the community is that Jagex's implementation uses the username
hash to load balance between login servers, but this has not been confirmed.

### 15 (`INIT_JS5REMOTE_CONNECTION`)

| Data type | Description         |
|-----------|---------------------|
| Int       | Client build number |

### 16 (`GAMELOGIN`)

| Data type     | Description                                  |
|---------------|----------------------------------------------|
| Int           | Client build number                          |
| Byte          | Unknown (hard-coded to `0` in client script) |
| Boolean       | Advert suppressed                            |
| Boolean       | Client signed                                |
| UnsignedByte  | Display mode                                 |
| UnsignedShort | Canvas width                                 |
| UnsignedShort | Canvas height                                |
| UnsignedByte  | Anti-aliasing mode                           |
| Byte\[24\]    | UID                                          |
| String        | Settings cookie                              |
| Int           | Affiliate ID                                 |
| Int           | Preferences                                  |
| Short         | TODO                                         |
| Int\[29\]     | JS5 archive checksums                        |
| UnsignedByte  | RSA-encrypted payload length (n)             |
| Byte\[n\]     | RSA-encrypted payload                        |

The unknown byte hard-coded to `0` in a client script might represent the
language. It is consistent with the ID for English. We can infer that there were
language-specific versions of the cache, as the surviving copy does not contain
translations.

The structure of the plaintext payload is described below:

| Data type    | Description                    |
|--------------|--------------------------------|
| UnsignedByte | Must be `10`                   |
| Int          | ISAAC cipher key (bits 0-31)   |
| Int          | ISAAC cipher key (bits 32-63)  |
| Int          | ISAAC cipher key (bits 64-95)  |
| Int          | ISAAC cipher key (bits 96-127) |
| Long         | Base37-encoded username        |
| String       | Password                       |

### 17 (Switch to JAGGRAB mode)

### 18 (Reconnect)

The packet is identical to `GAMELOGIN` in all but one way: the opcode of this
packet indicates the client is reconnecting due to connection loss, rather than
logging in from the login screen.

### 20 (Check date of birth and country)

| Data type     | Description                  |
|---------------|------------------------------|
| UnsignedByte  | Day                          |
| UnsignedByte  | Month                        |
| UnsignedShort | Year                         |
| UnsignedShort | Country ID                   |

### 21 (`CREATE_CHECK_NAME`)

| Data type | Description                  |
|-----------|------------------------------|
| Long      | Base37-encoded username      |

### 22 (`CREATE_ACCOUNT`)

| Data type       | Description                                           |
|-----------------|-------------------------------------------------------|
| UnsignedShort   | Client build number                                   |
| UnsignedByte    | RSA-encrypted payload length (n)                      |
| Byte\[n\]       | RSA-encrypted payload                                 |
| Byte\[len-n-3\] | XTEA-encrypted payload                                |

The structure of the RSA-decrypted payload is described below:

| Data type     | Description                    |
|---------------|--------------------------------|
| UnsignedByte  | Must be `10`                   |
| UnsignedShort | Flags (see below)              |
| Long          | Base37-encoded username        |
| Int           | XTEA key (bits 0-31)           |
| String        | Password                       |
| Int           | XTEA key (bits 32-63)          |
| UnsignedShort | Affiliate ID                   |
| UnsignedByte  | Day                            |
| UnsignedByte  | Month                          |
| Int           | XTEA key (bits 64-95)          |
| UnsignedShort | Year                           |
| UnsignedShort | Country ID                     |
| Int           | XTEA key (bits 96-127)         |

| Flag  | Description                          |
|------:|--------------------------------------|
| `0x1` | Receive RuneScape newsletters        |
| `0x2` | Receive Other newsletters            |
| `0x4` | Share details with business partners |

The structure of the XTEA-decrypted payload is described below:

| Data type   | Description   |
|-------------|---------------|
| String      | Email address |
| Byte\[0-7\] | Padding       |

### 23 (`REQUEST_WORLDLIST`)

| Data type | Description                  |
|-----------|------------------------------|
| Int       | Previous world list checksum |

The previous world list checksum is set to 0 if the client has not fetched the
world list before. It is used to save bandwidth if the world list has not
changed when the "Refresh" button is clicked: if checksum has not changed, the
server only sends the player counts and not the full world list.

Given the use of CRC-32 elsewhere in the client, it is probably the CRC-32
checksum of the encoded world list (excluding player counts), but this has not
been confirmed.

### 24 (`CHECK_WORLD_SUITABILITY`)

| Data type     | Description                               |
|---------------|-------------------------------------------|
| UnsignedShort | Client build number                       |
| UnsignedByte  | RSA-encrypted payload length (n)          |
| Byte\[n\]     | RSA-encrypted payload                     |

The structure of the plaintext payload is described below:

| Data type    | Description             |
|--------------|-------------------------|
| UnsignedByte | Must be `10`            |
| Int          | Random integer          |
| Long         | Base37-encoded username |
| Int          | Random integer          |
| String       | Password                |
| Int          | Random integer          |

## Downstream

| Opcode | Length | Jagex name                       | Description                            |
|-------:|-------:|----------------------------------|----------------------------------------|
|      0 |      8 | Unknown                          | Exchange session key                   |
|      1 |      0 | Unknown                          | Display video advertisement            |
|      2 |     14 | `OK`                             | Login successful                       |
|      3 |      0 | `INVALID_USERNAME_OR_PASSWORD`   | Invalid username or password           |
|      4 |      0 | `BANNED`                         | Account banned                         |
|      5 |      0 | `DUPLICATE`                      | Already logged in                      |
|      6 |      0 | `CLIENT_OUT_OF_DATE`             | Client out of date                     |
|      7 |      0 | `SERVER_FULL`                    | Server full                            |
|      8 |      0 | `LOGINSERVER_OFFLINE`            | Login server offline                   |
|      9 |      0 | `IP_LIMIT`                       | Too many connections from IP address   |
|     10 |      0 | Unknown                          | Bad session ID                         |
|     11 |      0 | `FORCE_PASSWORD_CHANGE`          | Password is weak                       |
|     12 |      0 | `NEED_MEMBERS_ACCOUNT`           | World is members-only                  |
|     13 |      0 | `INVALID_SAVE`                   | Could not complete login               |
|     14 |      0 | `UPDATE_IN_PROGRESS`             | Update in progress                     |
|     15 |      0 | `RECONNECT_OK`                   | Reconnect successful                   |
|     16 |      0 | `TOO_MANY_ATTEMPTS`              | Too many login attemts from IP address |
|     17 |      0 | Unknown                          | Account in members-only area           |
|     18 |      0 | `LOCKED`                         | Account locked                         |
|     19 |      0 | Unknown                          | Fullscreen is members-only             |
|     20 |      0 | Unknown                          | Invalid login server requested         |
|     21 |      1 | `HOP_BLOCKED`                    | Wait for profile transfer              |
|     22 |      0 | `INVALID_LOGIN_PACKET`           | Malformed login packet                 |
|     23 |      0 | Unknown                          | No reply from login server             |
|     24 |      0 | `LOGINSERVER_LOAD_ERROR`         | Error loading profile                  |
|     25 |      0 | `UNKNOWN_REPLY_FROM_LOGINSERVER` | Unknown reply from login server        |
|     26 |      0 | `IP_BLOCKED`                     | IP address banned                      |
|     27 |      0 | Unknown                          | Service unavailable                    |
|     29 |      1 | `DISALLOWED_BY_SCRIPT`           | Disallowed by script                   |
|     30 |      0 | Unknown                          | Client is members-only                 |
|    101 |      2 | Unknown                          | Switch world                           |

### 0 (Exchange session key)

| Data type | Description                    |
|-----------|--------------------------------|
| Long      | ISAAC cipher key (bits 64-127) |

### 1 (Display video advertisement)

After the client has finished displaying the advertisement, it sends an empty
packet with opcode 17. The opcode is encrypted with ISAAC.

### 2 (`OK`)

| Data type     | Description                    |
|---------------|--------------------------------|
| UnsignedByte  | Staff moderator level          |
| UnsignedByte  | Player moderator level         |
| Boolean       | Player is underage             |
| Boolean       | Parental chat consent          |
| Boolean       | Parental advertisement consent |
| Boolean       | Quick chat world               |
| Boolean       | Record mouse movement          |
| UnsignedShort | Player index                   |
| Boolean       | Player is a member             |
| Boolean       | Members-only world             |

### 3 (`INVALID_USERNAME_OR_PASSWORD`)

**Message:** Invalid username or password. If you have forgotten your password,
click here.

### 4 (`BANNED`)

**Message:** Your account has been disabled. Please <u>click here</u> to check
your Message Centre for details.

### 5 (`DUPLICATE`)

**Message:** Your account has not logged out from its last session. Try again in
a few minutes.

### 6 (`CLIENT_OUT_OF_DATE`)

**Message:** RuneScape has been updated! Please reload this page.

### 7 (`SERVER_FULL`)

**Message:** This world is full. Please use a different world.

### 8 (`LOGINSERVER_OFFLINE`)

**Message:** Unable to connect: login server offline.

### 9 (`IP_LIMIT`)

**Message:** Login limit exceeded: too many connections from your address.

### 10 (Bad session ID)

**Message:** Unable to connect: bad session ID.

### 11 (`FORCE_PASSWORD_CHANGE`)

**Message:** Your password is an extremely common choice, and is very weak. You
must change it before you can login. <u>Click here</u>

### 12 (`NEED_MEMBERS_ACCOUNT`)

**Message:** You need a members' account to log in to this world. Please
subscribe or use a different world.

### 13 (`INVALID_SAVE`)

**Message:** Could not complete login. Please try using a different world.

### 14 (`UPDATE_IN_PROGRESS`)

**Message:** The server is being updated. Please wait a few minutes and try
again.

### 15 (`RECONNECT_OK`)

### 16 (`TOO_MANY_ATTEMPTS`)

**Message:** Too many incorrect logins from your address. Please wait 5 minutes
before trying again.

### 17 (Account in members-only area)

**Message:** You are standing in a members-only area. To play on this world,
move to a free area first.

### 18 (`LOCKED`)

**Message:** Your account has been locked as we suspect it has been stolen.
Click here to recover your account.

### 19 (Fullscreen is members-only)

**Message:** Fullscreen is currently a members-only feature. To log in, either
return to the main menu and exit fullscreen or use a members' account.

### 20 (Invalid login server requested)

**Message:** Invalid loginserver requested. Please try using a different world.

### 21 (`HOP_BLOCKED`)

| Data type    | Description             |
|--------------|-------------------------|
| UnsignedByte | Hop time                |

**Message:** You have only just left another world. Your profile will be
transferred in <n> seconds.

The number of remaining seconds is calculated using the following formula:
(hopTime * 60 + 180) / 50

### 22 (`INVALID_LOGIN_PACKET`)

**Message:** Malformed login packet. Please try again.

### 23 (No reply from login server)

**Message:** No reply from login server. Please wait a minute and try again.

### 24 (`LOGINSERVER_LOAD_ERROR`)

**Message:** Error loading your profile. Please contact Customer Support.

### 25 (`UNKNOWN_REPLY_FROM_LOGINSERVER`)

**Message:** Unexpected loginserver response. Please try using a different
world.

### 26 (`IP_BLOCKED`)

**Message:** This comptuer's address has been blocked as it was used to break
our rules.

### 27 (Service unavailable)

Service unavailable.

### 29 (`DISALLOWED_BY_SCRIPT`)

| Data type    | Description             |
|--------------|-------------------------|
| UnsignedByte | Reason                  |

| Reason | Message                                                                               |
|-------:|---------------------------------------------------------------------------------------|
| 0      | You must have a Combat Level of at least 20 (without Summoning) to enter a PvP world. |
| 1      | You are currently carrying lent items and cannot enter a PvP world.                   |
| 2      | You must be standing in the Wilderness or Edgeville to enter this Bounty world.       |
| Other  | Unexpected server response. Please try using a different world.                       |

### 30 (Client is members-only)

This is not a member's account; please choose the 'Free Users' option from the
website to play on this account.

### 101 (Switch world)

| Data type     | Description             |
|---------------|-------------------------|
| UnsignedShort | World number            |
