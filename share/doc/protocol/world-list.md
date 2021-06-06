# World list

## Downstream

| Opcode | Length         | Jagex name                  | Description                        |
|-------:|---------------:|-----------------------------|------------------------------------|
|      0 | Variable short | Unknown                     | World list                         |

### 0 (World list)

| Data type    | Description             |
|--------------|-------------------------|
| UnsignedByte | Version, must be 1      |
| Boolean      | Worlds updated          |

The following fields are only present if the worlds updated flag is set:

| Data type          | Description             |
|--------------------|-------------------------|
| UnsignedShortSmart | Number of countries (n) |
| Country\[n\]       | Countries (see below)   |
| UnsignedShortSmart | Minimum world ID        |
| UnsignedShortSmart | Maximum world ID        |
| UnsignedShortSmart | Number of worlds (o)    |
| World\[o\]         | Worlds (see below)      |
| Int                | Checksum                |

The following fields are always present:

| Data type          | Description               |
|--------------------|---------------------------|
| PlayerCount\[o\]   | Player counts (see below) |

#### Country

| Data type          | Description |
|--------------------|-------------|
| UnsignedShortSmart | ID          |
| VersionedString    | Name        |

#### World

| Data type          | Description                                  |
|--------------------|----------------------------------------------|
| UnsignedShortSmart | Offset (world ID minus the minimum world ID) |
| UnsignedByte       | Country index (in the list in the packet)    |
| Int                | Flags (see below)                            |
| VersionedString    | Activity                                     |
| VersionedString    | Hostname                                     |

##### Flags

| Flag | Description  |
|-----:|--------------|
|  0x1 | Members only |
|  0x2 | Quick chat   |
|  0x4 | PvP          |
|  0x8 | Loot share   |

### Player count

| Data type          | Description                                  |
|--------------------|----------------------------------------------|
| UnsignedShortSmart | Offset (world ID minus the minimum world ID) |
| UnsignedShort      | Players (-1 indicates the world is offline)  |
