# Game protocol

## Upstream

| Opcode |        Length | Jagex name                    | Description                                                  |
|-------:|--------------:|-------------------------------|--------------------------------------------------------------|
|      0 |            12 | `IF_BUTTOND`                  | Drag component                                               |
|      3 |             7 | `OPOBJ5`                      | ObjStack operation 5                                         |
|      4 |             3 | `OPPLAYER8`                   | Player operation 8                                           |
|      6 |             9 | `OPHELDD`                     | Drag obj                                                     |
|      7 |             6 | `IF_BUTTON6`                  | Component operation 6                                        |
|      8 |             7 | `OPOBJ3`                      | ObjStack operation 3                                         |
|     10 |             6 | `IF_BUTTON10`                 | Component operation 10                                       |
|     12 |             3 | `OPNPC2`                      | NPC operation 2                                              |
|     26 |             8 | `FRIENDLIST_ADD`              | Add friend                                                   |
|     28 |            13 | `OPOBJT`                      | Use component on ObjStack                                    |
|     30 |             6 | `IF_BUTTON3`                  | Component operation 3                                        |
|     33 |             3 | `OPNPC4`                      | NPC operation 4                                              |
|     37 |             3 | `OPNPC3`                      | NPC operation 3                                              |
|     40 |            12 | `IF_BUTTONT`                  | Use component on component                                   |
|     42 | Variable byte | `MESSAGE_QUICKCHAT_PRIVATE`   | Send private quickchat message                               |
|     48 |             7 | `OPLOC2`                      | Loc operation 2                                              |
|     52 |             3 | `OPPLAYER7`                   | Player operation 7                                           |
|     53 |             3 | `OPNPC5`                      | NPC operation 5                                              |
|     54 |             7 | `OPOBJ1`                      | ObjStack operation 1                                         |
|     58 |            16 | `OPHELDU`                     | Use obj on obj                                               |
|     60 |             7 | `OPLOC5`                      | Operation 5 for loc                                          |
|     61 |             6 | `IF_BUTTON4`                  | Component operation 4                                        |
|     65 |             2 | Unknown                       | NPC examine                                                  |
|     66 |             6 | `IF_BUTTON5`                  | Component operation 5                                        |
|     72 |             2 | Unknown                       | Obj examine                                                  |
|     75 |             8 | `CLAN_KICKUSER`               | Kick user from clan chat                                     |
|     77 |             3 | `OPPLAYER6`                   | Player operation 6                                           |
|     78 | Variable byte | `REFLECTION_CHECK_REPLY`      | Send reflection check results                                |
|     81 |             8 | `OPHELD1`                     | Obj operation 1                                              |
|     85 |             4 | `FACE_SQUARE`                 | Face tile                                                    |
|     86 |             6 | `WINDOW_STATUS`               | Send display mode, width, height and anti-aliasing mode      |
|     88 |             8 | `OPHELD10`                    | Obj operation 10                                             |
|     89 |            13 | `OPLOCT`                      | Use component on loc                                         |
|     90 |            10 | `SEND_SNAPSHOT`               | Send abuse report                                            |
|     91 |             0 | Unknown                       | Idle logout                                                  |
|     92 |             7 | `OPLOC1`                      | Loc operation 1                                              |
|     94 |             6 | `IF_BUTTON1`                  | Component operation 1                                        |
|     95 |             8 | `OPHELD8`                     | Obj operation 8                                              |
|     96 |             3 | `OPPLAYER5`                   | Player operation 5                                           |
|     97 |             6 | `IF_BUTTON9`                  | Component operation 9                                        |
|    102 |             8 | `OPHELD9`                     | Obj operation 9                                              |
|    103 |             8 | `OPHELD7`                     | Obj operation 7                                              |
|    105 |             3 | `OPPLAYER4`                   | Player operation 4                                           |
|    108 |             6 | `IF_BUTTON7`                  | Component operation 7                                        |
|    113 |             0 | `MAP_BUILD_COMPLETE`          | Sent after map build complete                                |
|    116 |            15 | `OPLOCU`                      | Use obj on loc                                               |
|    119 |            15 | `OPOBJU`                      | Use obj on ObjStack                                          |
|    120 |             4 | `CLIENT_DETAILOPTIONS_STATUS` | Send graphics and audio options                              |
|    123 |            11 | `OPPLAYERU`                   | Use obj on player                                            |
|    124 |             8 | `OPHELD2`                     | Obj operation 2                                              |
|    134 |             7 | `OPLOC4`                      | Loc operation 4                                              |
|    137 |             0 | `NO_TIMEOUT`                  | Sent periodically to ensure TCP connection does not time out |
|    140 |             4 | `EVENT_CAMERA_POSITION`       | Sent periodically when camera rotates                        |
|    142 |             8 | `IGNORELIST_DEL`              | Delete ignore                                                |
|    145 |             8 | `OPHELD5`                     | Obj operation 5                                              |
|    148 |             3 | `OPPLAYER2`                   | Player operation 2                                           |
|    149 |             8 | `RESUME_P_NAMEDIALOG`         | Enter player name in dialog box                              |
|    155 |             9 | `OPNPCT`                      | Use component on NPC                                         |
|    156 |             2 | `TRANSMITVAR_VERIFYID`        | Sent after verify ID variable changes                        |
|    157 | Variable byte | `MESSAGE_PRIVATE`             | Send private message                                         |
|    158 |             4 | Unknown                       | Click on If1 component                                       |
|    159 |             7 | `OPLOC3`                      | Loc operation 3                                              |
|    160 |            11 | `OPNPCU`                      | Use obj on NPC                                               |
|    164 | Variable byte | `RESUME_P_STRINGDIALOG`       | Enter string in dialog box                                   |
|    172 |             8 | `FRIENDLIST_DEL`              | Delete friend                                                |
|    176 |             2 | Unknown                       | Loc examine                                                  |
|    177 |            14 | `OPHELDT`                     | Use component on obj                                         |
|    178 |             6 | `RESUME_PAUSEBUTTON`          | Click/hit spacebar to continue past dialog box               |
|    186 | Variable byte | `MESSAGE_PUBLIC`              | Send public message                                          |
|    189 |             0 | `CLOSE_MODAL`                 | Sent after modal dialog closed                               |
|    197 |             8 | `IGNORELIST_ADD`              | Add ignore                                                   |
|    199 | Variable byte | `EVENT_MOUSE_MOVE`            | Sent periodically when mouse is moved or clicked             |
|    200 |             6 | `EVENT_MOUSE_CLICK`           | Sent when mouse is clicked                                   |
|    201 |             6 | `IF_BUTTON2`                  | Component operation 2                                        |
|    204 |            10 | `APCOORDT`                    | Use component on tile                                        |
|    205 |             8 | `OPHELD4`                     | Obj operation 4                                              |
|    207 |            20 | `MOVE_MINIMAPCLICK`           | Walk/run to coordinate picked using the minimap              |
|    212 |             3 | `OPPLAYER1`                   | Player operation 1                                           |
|    214 |             8 | `OPHELD6`                     | Obj operation 6                                              |
|    215 |             9 | `FRIEND_SETRANK`              | Set clan member rank                                         |
|    216 | Variable byte | `CLIENT_CHEAT`                | Run :: command                                               |
|    218 |             8 | `CLAN_JOINCHAT_LEAVECHAT`     | Join or leave clan chat                                      |
|    219 |             4 | `RESUME_P_COUNTDIALOG`        | Enter number in dialog box                                   |
|    222 | Variable byte | `MESSAGE_QUICKCHAT_PUBLIC`    | Send public quickchat message                                |
|    223 |             3 | `OPPLAYER3`                   | Player operation 3                                           |
|    224 |             9 | `OPPLAYERT`                   | Use component on player                                      |
|    227 |             7 | `OPOBJ2`                      | ObjStack operation 2                                         |
|    230 |             6 | `MOVE_GAMECLICK`              | Walk/run to coordinate picked using the 3D viewport          |
|    231 |             2 | `RESUME_P_OBJDIALOG`          | Enter obj in dialog box                                      |
|    232 |             7 | `OPOBJ4`                      | ObjStack operation 4                                         |
|    234 |             3 | `SET_CHATFILTERSETTINGS`      | Set public chat, private chat and trade filter settings      |
|    236 |             4 | `DETECT_MODIFIED_CLIENT`      | Sent if the client is running in a window                    |
|    242 |             8 | `OPHELD3`                     | Obj operation 3                                              |
|    243 | Variable byte | `URL_REQUEST`                 | Request server construct and open URL from hostname and path |
|    245 |             3 | `OPNPC1`                      | NPC operation 1                                              |
|    248 |             1 | `EVENT_APPLET_FOCUS`          | Sent after window/applet focus changes                       |
|    250 |             4 | `SOUND_SONGEND`               | Sent after song ends                                         |
|    251 |             4 | `CLICKWORLDMAP`               | Click on world map                                           |
|    255 |             6 | `IF_BUTTON8`                  | Component operation 8                                        |

## Downstream

| Opcode |         Length | Jagex name                             | Description                                                           |
|-------:|---------------:|----------------------------------------|-----------------------------------------------------------------------|
|      1 | Variable short | `IF_SETTEXT`                           | Set component text                                                    |
|      2 | Variable short | `UPDATE_FRIENDCHAT_CHANNEL_FULL`       | Update full list of clan members                                      |
|      4 |              1 | `FRIENDLIST_LOADED`                    | Set friendserver connection status                                    |
|      8 |              1 | `UPDATE_RUNENERGY`                     | Update player's run energy                                            |
|     10 |              6 | `VARP_LARGE`                           | Set player variable (32-bit)                                          |
|     11 |              8 | `IF_SETCOLOUR`                         | Set component colour                                                  |
|     17 |             15 | `MAP_PROJANIM_HALFSQ`                  | Start a ProjAnim using half-tile coordinates                          |
|     18 |              8 | `IF_SETNPCHEAD`                        | Show NPC head on component                                            |
|     19 |             20 | `UPDATE_STOCKMARKET_SLOT`              | Update Grand Exchange slot                                            |
|     23 |              3 | `CHAT_FILTER_SETTINGS`                 | Set public chat, private chat and trade filter settings               |
|     25 | Variable short | `PLAYER_INFO`                          | Player update                                                         |
|     26 |              4 | Unknown                                | Clear objs shown on a component                                       |
|     30 |  Variable byte | `URL_OPEN`                             | Open URL in player's web browser                                      |
|     31 |             12 | `IF_SETTARGETPARAM`                    | Set target param of created componets                                 |
|     33 |              3 | `SET_MAP_FLAG`                         | Set minimap flag coordinates                                          |
|     34 | Variable short | `UPDATE_INV_PARTIAL`                   | Update partial inventory                                              |
|     35 |              9 | `IF_OPENSUB`                           | Open sub-interface                                                    |
|     38 |              3 | `VARP_SMALL`                           | Set player variable (8-bit)                                           |
|     40 |              7 | `IF_SETHIDE`                           | Set component hidden flag                                             |
|     42 |              6 | `CAM_FORCEANGLE`                       | Rotate camera                                                         |
|     43 | Variable short | `UPDATE_IGNORELIST`                    | Update ignore list                                                    |
|     44 |  Variable byte | `MESSAGE_PRIVATE`                      | Received private message                                              |
|     46 |              8 | `CAM_SHAKE`                            | Shake camera                                                          |
|     47 |              7 | `OBJ_REVEAL`                           | Reveal ObjStack belonging to another player to this player            |
|     49 | Variable short | `REBUILD_NORMAL`                       | Rebuild map build area                                                |
|     52 |              2 | `CAM_RESET`                            | Reset camera immediately                                              |
|     53 | Variable short | `UPDATE_ZONE_PARTIAL_ENCLOSED`         | Set current zone and enclose multiple zone update packets             |
|     54 |  Variable byte | `SET_MOVEACTION`                       | Change 'Walk here' text                                               |
|     55 |              1 | `MINIMAP_TOGGLE`                       | Set minimap state (rotation lock, compass blackout and map blackout)  |
|     59 |  Variable byte | `MESSAGE_GAME`                         | Game message                                                          |
|     60 |              5 | `OBJ_ADD`                              | Add ObjStack                                                          |
|     63 |              7 | `LOC_ANIM_SPECIFIC`                    | Animate loc for this player only                                      |
|     65 |             10 | `IF_SETPOSITION`                       | Set position of component                                             |
|     72 |              2 | `UPDATE_REBOOT_TIMER`                  | Show 'System update in' countdown                                     |
|     75 |  Variable byte | `CLIENT_SETVARCSTR_SMALL`              | Set client string variable (<= 250 bytes)                             |
|     78 |              0 | `RESET_CLIENT_VARCACHE`                | Reset all player variables to zero                                    |
|     86 |  Variable byte | `UPDATE_SITESETTINGS`                  | Update website settings cookie                                        |
|     87 |              8 | `IF_SETMODEL`                          | Show model on a component                                             |
|     89 |             10 | `IF_MOVESUB`                           | Move sub-interface to another component                               |
|     90 |             12 | `IF_SETANGLE`                          | Rotate model shown on a component                                     |
|     93 |              8 | `CAM_LOOKAT`                           | Rotate camera to look at coordinates                                  |
|     95 |              2 | `UPDATE_RUNWEIGHT`                     | Update weight of player's inventory                                   |
|     96 |              0 | `TRIGGER_ONDIALOGABORT`                | Close the currently open dialog interface                             |
|     97 |              2 | `UPDATE_ZONE_PARTIAL_FOLLOWS`          | Set current zone                                                      |
|     99 |  Variable byte | `MESSAGE_QUICKCHAT_FRIENDCHANNEL`      | Clan chat quickchat message                                           |
|    100 |              3 | `MIDI_SONG`                            | Play song                                                             |
|    103 |              4 | `LAST_LOGIN_INFO`                      | Set IP address of last login                                          |
|    104 |  Variable byte | `MESSAGE_PRIVATE_ECHO`                 | Sent private message                                                  |
|    105 |             15 | `MAP_PROJANIM`                         | Start a ProjAnim                                                      |
|    111 |             17 | Unknown                                | Start a ProjAnim, adjusting the coordinates based on the source's BAS |
|    113 |             12 | `IF_SET_PLAYERHEAD_IGNOREWORN`         | Show player head (excluding equipment) on component                   |
|    114 |              7 | `OBJ_COUNT`                            | Update ObjStack count                                                 |
|    115 |              3 | `LOC_PREFETCH`                         | Prefetch a loc and its models from the JS5 server                     |
|    116 | Variable short | `CLIENT_SETVARCSTR_LARGE`              | Set client string variable (<= 65,530 bytes)                          |
|    120 |              3 | `OBJ_DEL`                              | Delete ObjStack                                                       |
|    126 |  Variable byte | `SET_PLAYER_OP`                        | Change player op text, cursor and priority                            |
|    130 |              3 | Unknown                                | Teleport player                                                       |
|    131 |              4 | `LOC_ADD_CHANGE`                       | Adds or changes a loc                                                 |
|    133 |             14 | Unknown                                | Attach loc to PathingEntity                                           |
|    135 |              6 | `IF_CLOSESUB`                          | Close sub-interface                                                   |
|    136 | Variable short | `NPC_INFO`                             | NPC update                                                            |
|    137 |              3 | `VARBIT_SMALL`                         | Set player bit variable (up to 8 bits)                                |
|    138 |              6 | `VARBIT_LARGE`                         | Set player bit variable (up to 32 bits)                               |
|    144 |              6 | `SOUND_AREA`                           | Play sound at specified coordinate with radius                        |
|    147 |              2 | `UPDATE_ZONE_FULL_FOLLOWS`             | Set current zone and clear it to prepare for full update              |
|    148 |              8 | `CLIENT_SETVARC_LARGE`                 | Set client variable (32-bit)                                          |
|    150 |              5 | `NPC_ANIM_SPECIFIC`                    | Animate NPC for this player only                                      |
|    151 |              8 | `CAM_MOVETO`                           | Move camera                                                           |
|    152 |             10 | `IF_SPINMODEL`                         | Set rotation speed of model shown on a component                      |
|    153 |              6 | `SYNTH_SOUND`                          | Play sound                                                            |
|    155 | Variable short | `REBUILD_REGION`                       | Rebuild map build area (instanced)                                    |
|    156 |              2 | `CAM_SMOOTHRESET`                      | Reset camera smoothly                                                 |
|    158 |              6 | `MAP_ANIM`                             | Start a SpotAnim                                                      |
|    162 |             14 | `IF_SETCLICKMASK`                      | Set click masks of created components                                 |
|    163 |              0 | Unknown                                | Reset client predictions of all player variables                      |
|    164 |              6 | `UPDATE_STAT`                          | Update player's level and experience in a skill                       |
|    166 |             28 | `UPDATE_UID192`                        | Update the computer/user's unique ID and write to random.dat          |
|    168 |              2 | `UPDATE_INV_STOP_TRANSMIT`             | Delete local copy of inventory used by CS2 scripts                    |
|    172 |              8 | `IF_SETANIM`                           | Animate model shown on a component                                    |
|    173 | Variable short | `REFLECTION_CHECKER`                   | Perform reflection check                                              |
|    174 |  Variable byte | `UPDATE_FRIENDCHAT_CHANNEL_SINGLEUSER` | Update single clan member                                             |
|    176 |              5 | `CLIENT_SETVARC_SMALL`                 | Set client variable (8-bit)                                           |
|    177 |             11 | `HINT_ARROW`                           | Add, change or delete a hint arrow                                    |
|    179 |              4 | `LOC_ANIM`                             | Animate loc                                                           |
|    182 |  Variable byte | `UPDATE_FRIENDLIST`                    | Update single friend                                                  |
|    183 |             12 | `IF_SETOBJ`                            | Show obj on component                                                 |
|    192 |              0 | `LOGOUT`                               | Logout                                                                |
|    194 | Variable short | `UPDATE_INV_FULL`                      | Update full inventory                                                 |
|    199 |              6 | `IF_SETPLAYERHEAD`                     | Show player head (including equipment) on component                   |
|    206 |              6 | `MIDI_JINGLE`                          | Play jingle                                                           |
|    221 |  Variable byte | `MESSAGE_QUICKCHAT_PRIVATE_ECHO`       | Sent private quickchat message                                        |
|    222 |  Variable byte | `MESSAGE_QUICKCHAT_PRIVATE`            | Received private quickchat message                                    |
|    225 |              5 | `IF_SETTOP`                            | Set top-level interface                                               |
|    229 |              0 | `RESET_ANIMS`                          | Stop all player/NPC animations                                        |
|    232 |              2 | `LOC_DEL`                              | Delete loc                                                            |
|    237 |             10 | `SPOTANIM_SPECIFIC`                    | Start a SpotAnim for this player only                                 |
|    242 |              3 | `SETDRAWORDER`                         | Set player/NPC render priority                                        |
|    243 |              8 | `IF_SETSCROLLPOS`                      | Set component scroll position                                         |
|    246 |  Variable byte | `MESSAGE_FRIENDCHANNEL`                | Clan chat message                                                     |
|    253 |  Variable byte | `RUNCLIENTSCRIPT`                      | Run CS2 script                                                        |
