# Glossary

| Jagex name    | RSPS community name(s) | Description                                                                 |
|---------------|------------------------|-----------------------------------------------------------------------------|
|               | V1                     | A middle-endian byte order used on [Honeywell Series 16][hs16] computers    |
| Alt3          | V2                     | A middle-endian byte order used on [PDP-11][pdp11] computers                |
| Ap            |                        | An op that trigers when you approach (rather than reach) the target         |
| Area          |                        | A 104x104x4 group of tiles                                                  |
| BAS           | Stance                 | Base animation set                                                          |
| CS1           |                        | Client script version 1                                                     |
| CS2           |                        | Client script version 2                                                     |
| Coord         | Position               |                                                                             |
| Entity        | Animable, Renderable   |                                                                             |
| Enum          |                        |                                                                             |
| Flo           |                        | Floor overlay                                                               |
| Flu           |                        | Floor underlay                                                              |
| GoSubFrame    | StackFrame             |                                                                             |
| IDK           | IdentityKit            | Identikit                                                                   |
| Inv           | Inventory              |                                                                             |
| JS5           |                        | Jagex store version 5?                                                      |
| Loc           | Object                 |                                                                             |
| MSI           |                        | Map scenery icon                                                            |
| MapSquare     | Map                    | A 64x64x4 group of tiles                                                    |
| Mel           |                        | Map element                                                                 |
| MultiLoc      |                        | Transforms a loc into another based on a varp or varbit                     |
| MultiNPC      |                        | Transforms an NPC into another based on a varp or varbit                    |
| Obj           | Item                   |                                                                             |
| ObjStack      | GroundItem             |                                                                             |
| Op            | Option, Action         | Operation                                                                   |
| Packet        | Buffer, Stream         |                                                                             |
| Packet.Bit    |                        | Extends the underlying buffer type with ISAAC encryption and bit packing    |
| PathingEntity | Character, Entity, Mob | A player or NPC                                                             |
| Seq           | Animation              | Sequence                                                                    |
| Skill         | Skill                  | A type of skill                                                             |
| Smart         |                        | A data type encoded as a shorter or longer type for bandwidth efficiency    |
| SpotAnim      | Graphic                | Spot animation, a combination of a model and a sequence                     |
| Stat          |                        | The player's base level, boosted level and experience in a particular skill |
| StockMarket   |                        | Grand exchange                                                              |
| Struct        |                        |                                                                             |
| Varbit        | ConfigByFile           | Player bit variable                                                         |
| Varc          |                        | Client variable                                                             |
| Varcstr       |                        | Client string variable                                                      |
| Varp          | Config                 | Player variable                                                             |
| Zone          | Region                 | An 8x8x1 group of tiles                                                     |

## Loc layers

| ID | Jagex name  |
|---:|-------------|
|  0 | Wall        |
|  1 | WallDecor   |
|  2 | Scenery     |
|  3 | GroundDecor |

[hs16]: https://en.wikipedia.org/wiki/Endianness#Honeywell_Series_16
[pdp11]: https://en.wikipedia.org/wiki/Endianness#PDP-endian
