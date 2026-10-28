# D&J RAID Storage

NeoForge **26.3** (NeoForge `26.3.0.95`) mod.

## What it does

- **RAID Chassis** holds all digitized items (shared by every player on that array).
- **CHEST** is the terminal. It must touch a **data cable** that reaches exactly one Chassis.
- Two Chassis on the same cable graph → every CHEST on that graph shows the **error** screen.
- No Chassis → **disconnected** screen.
- Capacity too small for what’s stored → **locked** (extract only).

Byte math: first item of a stack = 16 bytes, every extra item = 1 byte. Tools and weapons cost 16 bytes each. Display uses B until 2048, then KB / MB / GB / TB / PB.  
Capacity: largest connected CHEST is full bytes; every extra CHEST adds **half of its own tier**.

| CHEST        | Bytes |
|--------------|------:|
| Stone        | 512 |
| Copper       | 1024 |
| Iron         | 2048 |
| Gold         | 3072 |
| Quartz       | 4096 |
| Amethyst     | 5120 |
| Diamond      | 6144 |
| Netherite    | 9216 |
| Admin        | 101376 |

## Crafting

- Chassis: 8 stone + iron ingot center
- Stone CHEST: 8 cobblestone + iron ingot center
- Cable: string corners, iron ingots top/bottom middle, copper ingots in the center row → 8 cables
- Upgrades: 8 of the resource + redstone center, except Netherite (smithing table: Netherite Upgrade template + Amethyst CHEST Upgrade + Netherite Ingot). Apply in order with **shift-right-click** on a placed CHEST, or shapeless-craft a lower CHEST with every kit in between (stone + all kits through diamond → diamond CHEST, without placing it).

## Notes

- Cables are a **persistent graph** (works through unloaded chunks). WorldEdit that deletes blocks without events can desync it.
- Opening a CHEST will load the Chassis chunk so storage can be read.
- Breaking or replacing a Chassis keeps digitized items on the dropped block (shulker-style). Place it again to restore the array; if connected CHEST capacity is too small, it stays **locked** until you extract or add drives.
- Destroying or replacing CHESTs recalculates capacity the same way. Over capacity → locked (extract only).
- Storage is per Chassis, not world-global. Multiplayer-shared on that array.

## Credits

The code for this mod was written with heavy AI assistance. All artwork and sound design are human-made.

Mojang mappings license: https://github.com/NeoForged/NeoForm/blob/main/Mojang.md
