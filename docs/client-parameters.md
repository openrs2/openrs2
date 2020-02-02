# Client parameters

## Command-line interface

The command-line interface takes four required parameters:

* `worldid`
* `modewhere` (`live`, `rc` or `wip`)
* `lang` (`en`/`english`, `de`/`german`, `fr` or `pt`)
* `game` (`game0` or `game1`)

Each parameter is described in the applet parameter table below. `modewhat` is
always set to `local`. All other parameters share their default value with the
applet.

A typical invocation looks like:

```
java -cp ... client 1 live en game0
```

## Applet

The applet supports the following parameters:

| Name               | Default value | Description                              |
|--------------------|---------------|------------------------------------------|
| `advertsuppressed` | `0`           | Disable adverts                          |
| `affid`            | `0`           | Affiliate ID (see below)                 |
| `cachesubdir`      | `"runescape"` | Cache subdirectory                       |
| `cookiehost`       |               | Settings cookie host                     |
| `cookieprefix`     |               | Settings cookie name prefix              |
| `country`          | `0`           | Country ID                               |
| `crashurl`         | `null`        | Override `error_loader_<x>.ws` URL       |
| `game`             | `0`           | Game ID (see below)                      |
| `haveie6`          | `0`           | Set if browser is Internet Explorer 6    |
| `js`               | `0`           | Set if JavaScript is supported           |
| `lang`             | `0`           | Language ID                              |
| `modewhat`         |               | See below                                |
| `modewhere`        |               | See below                                |
| `objecttag`        | `0`           | Set if applet loaded with `<object>` tag |
| `openwinjs`        | `0`           | Use JavaScript to open URLs in a new tab |
| `pre142url`        | `null`        | Enable pre-Java 1.4.2 check              |
| `settings`         | `""`          | Settings cookie value                    |
| `suppress_sha`     | `null`        | Disable SHA-1 validation in the loader   |
| `unsignedurl`      | `null`        | Enable unsigned applet check             |
| `worldid`          |               | World ID                                 |

Parameters without a default value listed in the table above are required by the
applet. All other parameters are optional.

## Affiliate ID

| ID | Description                                                                      |
|----|----------------------------------------------------------------------------------|
| 0  | No affiliate                                                                     |
| 99 | Enables 'share details with business partners' checkbox in the registration form |

## Games

| ID | Name                   | Command-line name |
|----|------------------------|-------------------|
| 0  | RuneScape              | `game0`           |
| 1  | MechScape/Stellar Dawn | `game1`           |

At the time 550 was released, MechScape used the same engine as RuneScape.
While changing the parameter affects the client in a small number of ways, the
majority of differences between the two games is in the cache.

When `game1` is used, the following changes are made in the client:

* The default cache subdirectory used by the command-line interface is
  `"mechscape"` instead of `"runescape"`.
* The "RuneScape is loading" text changes to "Mechscape is loading".
* "(level: \<x\>)" after a player's username is changed to "(rating: \<x\>)".
* The "Attack" option on players, if sent by the server, does not use combat
  levels to determine its priority dynamically.
* The 3D login screen is always disabled, even in HD mode.
* `::shiftclick` is enabled by default, instead of disabled.
* The default fog colour is black in MechScape.
* MechScape uses a different palette for recolouring players and objs.
* MechScape supports four username prefixes and suffixes.
* The box drawn around the current area in the world overview is white instead
  of red.
* A "Face here" action is added to every menu where there is a "Walk here"
  action.

MechScape was never released so the cache is unavailable. As such, OpenRS2 only
supports `game0`.

## Languages

| ID | Name       | Command-line names |
|----|------------|--------------------|
| 0  | English    | `en`, `english`    |
| 1  | German     | `de`, `german`     |
| 2  | French     | `fr`               |
| 3  | Portuguese | `pt`               |

The language parameter only controls the language of the strings hard-coded in
the loader and client. The majority of strings are stored in the cache and are
not translated. Presumably each language had a different cache at the time.
However, only the English cache was archived. As such, OpenRS2 only supports
English.

## `modewhat`

| ID | Name                   | Command-line name |
|----|------------------------|-------------------|
| 0  | Live                   | `live`            |
| 1  | Release candidate (RC) | `rc`              |
| 2  | Work in progress (WIP) | `wip`             |

`modewhat` is primarily used to select which cache directory is used.

The following table summarises the differences between each `modewhat` value:

|                                                                  | Live | RC | WIP |
|------------------------------------------------------------------|------|----|-----|
| **File store ID**                                                | 32   | 33 | 34  |
| **Allocate and release 100 KiB chunks to test `SoftReference`s** | N    | Y  | Y   |

## `modewhere`

| ID | Name   | Command-line name |
|----|--------|-------------------|
| 0  | Live   | `live`            |
| 1  | Office | `office`          |
| 2  | Local  | `local`           |

The applet only supports the `live` and `office` environments. The command-line
interface always uses the `local` environment. (The command-line names above are
taken from an earlier revision where `modewhere` is specified on the command
line.)

`modewhere` is primarily used to control which server the client connects to.

The following table summarises the differences between each `modewhere` value:

|                                  | Live            | Office          | Local         |
|----------------------------------|-----------------|-----------------|---------------|
| **World hostname**               | Applet codebase | Applet codebase | localhost     |
| **Game server primary port**     | 43594           | 40000 + world   | 40000 + world |
| **Game server secondary port**   | 443             | 50000 + world   | 50000 + world |
| **World web server port**        | 80              | 7000 + world    | 7000 + world  |
| **Website hostname**             | www             | www-wtqa        | www-wtqa      |
| **CS2 errors shown in chat box** | N               | Y               | Y             |
| **FPS shown by default**         | N               | Y               | Y             |
| **`::fps <n>` command enabled**  | N               | Y               | Y             |
| **Advert refreshed**             | Y               | Y               | N             |
