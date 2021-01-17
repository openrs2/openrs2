# Glossary

| Jagex name        | RSPS community name(s) | Description                                                                  |
|-------------------|------------------------|------------------------------------------------------------------------------|
|                   | V1                     | A middle-endian byte order used on [Honeywell Series 16][hs16] computers     |
| Alt3              | V2                     | A middle-endian byte order used on [PDP-11][pdp11] computers                 |
| Ap                |                        | An op that trigers when you approach (rather than reach) the target          |
| Area              |                        | A 104x104x4 group of tiles                                                   |
| BAS               | Stance                 | Base animation set                                                           |
| CS1               |                        | Client script version 1                                                      |
| CS2               |                        | Client script version 2                                                      |
| Component         |                        | A user interface element (examples include buttons, text boxes, icons, etc.) |
| CoordFine         |                        | A position more precise than a grid coord (128 units per tile in build 550)  |
| CoordGrid         | Position               | The position of a tile                                                       |
| Entity            | Animable, Renderable   |                                                                              |
| Enum              |                        | A bidirectional map between integer keys and integer or string values        |
| Flo               |                        | Floor overlay                                                                |
| Flu               |                        | Floor underlay                                                               |
| GoSubFrame        | StackFrame             |                                                                              |
| IDK               | IdentityKit            | Identikit                                                                    |
| Interface         |                        | A group of components                                                        |
| Inv               | Inventory              |                                                                              |
| JS5               |                        | Jagex store version 5?                                                       |
| Loc               | Object                 |                                                                              |
| MSI               |                        | Map scenery icon                                                             |
| MapSquare         | Map                    | A 64x64x4 group of tiles                                                     |
| Mel               |                        | Map element                                                                  |
| Modal             |                        | An interface that can be (manually or automatically) closed by the client    |
| MultiLoc          |                        | Transforms a loc into another based on a varp or varbit                      |
| MultiNPC          |                        | Transforms an NPC into another based on a varp or varbit                     |
| Obj               | Item                   |                                                                              |
| ObjStack          | GroundItem             |                                                                              |
| Op                | Option, Action         | Operation                                                                    |
| Overlay           |                        | An interface that can only be closed by the server                           |
| Packet            | Buffer, Stream         |                                                                              |
| Packet.Bit        |                        | Extends the underlying buffer type with ISAAC encryption and bit packing     |
| Param             |                        | A key used to add a extra attributes to locs, NPCs, objs and structs         |
| PathingEntity     | Character, Entity, Mob | A player or NPC                                                              |
| ProjAnim          | Projectile             |                                                                              |
| ScriptVarType     |                        | An enum representing types that can be used in a client script               |
| Seq               | Animation              | Sequence                                                                     |
| Skill             | Skill                  | A type of skill                                                              |
| Smart             |                        | A data type encoded as a shorter or longer type for bandwidth efficiency     |
| SpotAnim          | Graphic                | Spot animation, a combination of a model and a sequence                      |
| Stat              |                        | The player's base level, boosted level and experience in a particular skill  |
| StockMarket       |                        | Grand exchange                                                               |
| Struct            |                        | A map of param keys to integer or string values                              |
| SubInterface      |                        | An interface attached to a component in a parent interface                   |
| TopLevelInterface |                        | An interface without a parent                                                |
| Varbit            | ConfigByFile           | Player bit variable                                                          |
| Varc              |                        | Client variable                                                              |
| Varcstr           |                        | Client string variable                                                       |
| Varp              | Config                 | Player variable                                                              |
| Zone              | Region                 | An 8x8x1 group of tiles                                                      |

## Loc layers

| ID | Jagex name  |
|---:|-------------|
|  0 | Wall        |
|  1 | WallDecor   |
|  2 | Scenery     |
|  3 | GroundDecor |

[hs16]: https://en.wikipedia.org/wiki/Endianness#Honeywell_Series_16
[pdp11]: https://en.wikipedia.org/wiki/Endianness#PDP-endian
