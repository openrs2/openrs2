# Glossary

| Jagex name        | RSPS community name(s)     | Description                                                                  |
|-------------------|----------------------------|------------------------------------------------------------------------------|
|                   | V1                         | A middle-endian byte order used on [Honeywell Series 16][hs16] computers     |
|                   | Viewport                   | A 1x32x32 group of tiles (inside which other players and NPcs are visible)   |
| Alt3              | V2                         | A middle-endian byte order used on [PDP-11][pdp11] computers                 |
| Ap                |                            | An op that trigers when you approach (rather than reach) the target          |
| BAS               | Stance                     | Base animation set                                                           |
| BuildArea         |                            | A 104x104 group of tiles (held in memory by the client)                      |
| CS1               |                            | Client script version 1                                                      |
| CS2               |                            | Client script version 2                                                      |
| Component         |                            | A user interface element (examples include buttons, text boxes, icons, etc.) |
| CoordFine         |                            | A position more precise than a grid coord (128 units per tile in build 550)  |
| CoordGrid         | Position                   | The position of a tile                                                       |
| Entity            | Animable, Renderable       |                                                                              |
| Enum              |                            | A bidirectional map between integer keys and integer or string values        |
| Flo               |                            | Floor overlay                                                                |
| Flu               |                            | Floor underlay                                                               |
| GoSubFrame        | StackFrame                 |                                                                              |
| IDK               | IdentityKit                | Identikit                                                                    |
| Interface         |                            | A group of components                                                        |
| Inv               | Inventory                  |                                                                              |
| JS5               |                            | Jagex store version 5?                                                       |
| Js5Index          | ReferenceTable             |                                                                              |
| Js5MasterIndex    | ChecksumTable, update keys |                                                                              |
| Loc               | Object                     |                                                                              |
| MSI               |                            | Map scenery icon                                                             |
| MapSquare         | Map                        | A 4x64x64 group of tiles (the granularity of map files in the cache)         |
| Mel               |                            | Map element                                                                  |
| Modal             |                            | An interface that can be (manually or automatically) closed by the client    |
| MultiLoc          |                            | Transforms a loc into another based on a varp or varbit                      |
| MultiNPC          |                            | Transforms an NPC into another based on a varp or varbit                     |
| Obj               | Item                       |                                                                              |
| ObjStack          | GroundItem                 |                                                                              |
| Op                | Option, Action             | Operation                                                                    |
| Overlay           |                            | An interface that can only be closed by the server                           |
| Packet            | Buffer, Stream             |                                                                              |
| Packet.Bit        |                            | Extends the underlying buffer type with ISAAC encryption and bit packing     |
| Param             |                            | A key used to add a extra attributes to locs, NPCs, objs and structs         |
| PathingEntity     | Character, Entity, Mob     | A player or NPC                                                              |
| ProjAnim          | Projectile                 |                                                                              |
| Region            | Instance                   |                                                                              |
| ScriptVarType     |                            | An enum representing types that can be used in a client script               |
| Seq               | Animation                  | Sequence                                                                     |
| Skill             | Skill                      | A type of skill                                                              |
| Smart             |                            | A data type encoded as a shorter or longer type for bandwidth efficiency     |
| SpotAnim          | Graphic                    | Spot animation, a combination of a model and a sequence                      |
| StaffModLevel     | Rights, Privilege, Rank    |                                                                              |
| Stat              |                            | The player's base level, boosted level and experience in a particular skill  |
| StockMarket       |                            | Grand exchange                                                               |
| Struct            |                            | A map of param keys to integer or string values                              |
| SubInterface      |                            | An interface attached to a component in a parent interface                   |
| TopLevelInterface |                            | An interface without a parent                                                |
| Varbit            | ConfigByFile               | Player bit variable                                                          |
| Varc              |                            | Client variable                                                              |
| Varcstr           |                            | Client string variable                                                       |
| Varp              | Config                     | Player variable                                                              |
| Zone              | Region, Chunk              | A 1x8x8 group of tiles (the granularity used by various packets)             |

## Loc layers

|  ID | Jagex name  |
|----:|-------------|
|   0 | Wall        |
|   1 | WallDecor   |
|   2 | Scenery     |
|   3 | GroundDecor |

## Loc shapes

|  ID | Jagex name                    |
|----:|-------------------------------|
|   0 | `WALL_STRAIGHT`               |
|   1 | `WALL_DIAGONALCORNER`         |
|   2 | `WALL_L`                      |
|   3 | `WALL_SQUARECORNER`           |
|   4 | `WALLDECOR_STRAIGHT_NOOFFSET` |
|   5 | `WALLDECOR_STRAIGHT_OFFSET`   |
|   6 | `WALLDECOR_DIAGONAL_OFFSET`   |
|   7 | `WALLDECOR_DIAGONAL_NOOFFSET` |
|   8 | `WALLDECOR_DIAGONAL_BOTH`     |
|   9 | `WALL_DIAGONAL`               |
|  10 | `CENTREPIECE_STRAIGHT`        |
|  11 | `CENTREPIECE_DIAGONAL`        |
|  12 | `ROOF_STRAIGHT`               |
|  13 | `ROOF_DIAGONAL_WITH_ROOFEDGE` |
|  14 | `ROOF_DIAGONAL`               |
|  15 | `ROOF_L_CONCAVE`              |
|  16 | `ROOF_L_CONVEX`               |
|  17 | `ROOF_FLAT`                   |
|  18 | `ROOFEDGE_STRAIGHT`           |
|  19 | `ROOFEDGE_DIAGONALCORNER`     |
|  20 | `ROOFEDGE_L`                  |
|  21 | `ROOFEDGE_SQUARECORNER`       |
|  22 | `GROUNDDECOR`                 |

[hs16]: https://en.wikipedia.org/wiki/Endianness#Honeywell_Series_16
[pdp11]: https://en.wikipedia.org/wiki/Endianness#PDP-endian
