# Coordinate system

## Summary

| Name                   | Levels | Width (tiles) | Height (tiles) |
|------------------------|-------:|--------------:|---------------:|
| World                  |      4 |        16,384 |         16,384 |
| BuildArea              | 1 or 4 |           104 |            104 |
| MapSquare              |      4 |            64 |             64 |
| MapSquare (underwater) |      1 |            64 |             64 |
| Viewport (?)           |      1 |            32 |             32 |
| Zone                   |      1 |             8 |              8 |

## Introduction

RuneScape is a three-dimensional tile-based world. The entire world is made up
of 16,384x16,384 tiles on 4 levels of elevation (ranging from 0 to 3
inclusive), including the ground level. The X and Z axes range from 0 to
16,383 inclusive.

The Y axis represents the level of elevation, with 0 representing ground
level. The X and Z axes represent coordinates on the plane - when the world map
or mini map is plotted in two dimensions, the X axis is the horizontal axis
(with X increasing from left to right), and the Z axis is the vertical axis
(with Z increasing from bottom to top).

## Map squares

The world is split into map squares, each of which contains 64x64 tiles across
4 levels. Map squares are the granularity of the maps stored in the client's
cache.

Each map square has up to five files in the client's cache:

| File name | Name            | Description                                                           |
|-----------|-----------------|-----------------------------------------------------------------------|
| `mX_Z`    | Map             | Contains floor underlays, overlays, tile heights and flags            |
| `lX_Z`    | Locs            | Contains locs (walls, wall decoration, scenery and ground decoration) |
| `umX_Z`   | Underwater map  | Contains underwater equivalent of the map file                        |
| `ulX_Z`   | Underwater locs | Contains underwater equivalent of the locs file                       |
| `nX_Z`    | NPC spawns      | Contains NPC spawns for the animated login screen                     |

The underwater files contain a single virtual -1 level used to represent the
tiles and locs that can be seen beneath translucent water tiles in HD mode. The
server does not interact with this level, it purely exists for decorative
purposes.

The X and Z coordinates of a map are the coordinates of the origin tile divided
by 64. Internally, the client combines these two values into a single ID by
calculating `(X << 8) | Z`.

## Zones

Each map square is split into zones, each of which contains 8x8 tiles across a
single level. Zones are the granularity used for most packets that interact with
the world - for example, spawning locs and obj stacks. An entire zone may be
reset and populated with dynamic locs and obj stacks in two packets.

Zones are also the granularity used to build instances, such as player-owned
houses.

The left-hand side of the world (for tiles ranging from X=0 to X=6399 inclusive)
is used for the static map squares stored in the cache. The right-hand side of
the world (for tiles from X=6400 above) is used for instances, which are
dynamically constructed by copying zones from the left side of the world and
moving/rotating them.

## Dungeons

Dungeons are typically, but not always, 6400 Z units above the equivalent
ground-level areas. 6400 was likely chosen as this corresponds to 100 map
squares, meaning a single "1" digit needs to be added to or removed from the
map's file name to switch between ground level and the corresponding dungeon.

## Build area

The build area represents the 104x104 group of tiles (or 13x13 group of zones)
held in the client's memory. Depending on its settings, the client only retains
the player's current level or retains all four levels.

The build area is always zone-aligned. When the area is initially built, it is
centred around the player's current zone.

When the player is within 16 tiles of the edge of the current build area, it is
rebuilt.

## Viewport

The viewport (note: unlike the other names in this document, we do not know the
official Jagex name) is the 32x32 group of tiles, always centred around the
current player, within which other players and NPCs are visible. Players can
only see other players and NPCs on the same level as them.

## Coordinates

When used in a script, the coordinates of a tile are packed into a 32-bit
integer as follows:

| **Field** | (unused) | Level | X  | Z  |
|-----------|----------|------:|---:|---:|
| **Bits**  |        2 |     2 | 14 | 14 |

The two unused bits are set to zero.

Literals in scripts are represented with the X and Z coordinates split into the
map coordinates and the local coordinates within each map:
`<level>_<map_x>_<map_z>_<x>_<z>`. For example, (0, 3094, 3107) is written as
`0_48_48_22_35` in a script.

The teleport command uses a similar syntax:
`::tele <level>,<map_x>,<map_z>,<x>,<z>`.
