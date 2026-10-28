# CHEST

*A terminal for the RAID Chassis. The Chassis stores it. The CHEST pretends it was always a chest.*

A CHEST talks to at most one Chassis, through data cables. It does not store items itself.

## Status

| Screen | Meaning |
|---|---|
| Error | Two or more Chassis on the same cable graph |
| Disconnected | No Chassis |
| Locked | Stored bytes exceed CHEST capacity (extract only) |
| Low space | Fewer than 16 bytes remaining |
| OK | Insert and extract |

Error, disconnected, and locked win over low space.

## Ports and sounds

Cables attach on the **back** face only. The front faces the player when placed.

Open/close use the custom samples at pitch 1.0. Distance falloff is 16 blocks (`attenuation_distance`). Variable-range events keep Sound Physics Remastered wall muffling.

Netherite and Amethyst **block** step/break sounds stay as the vanilla material.

## Insert / extract

There is no digitizer slot.

- Click the upper panel while holding an item, or shift-click from your inventory, to digitize. Leftovers stay in your inventory.
- Left-click a stored item: 1 into inventory.
- Shift-left: one full stack into inventory (Shift is read from the keyboard).
- Right-click: every matching stored item, filling inventory until full.

Only **empty** shulkers, bundles, and RAID Chassis can be digitized. Filled containers are refused. Traveler’s Backpacks never digitize.

## Upgrades

Order: Copper → Iron → Gold → Quartz → Amethyst → Diamond → Netherite.

Sneak-right-click a placed CHEST with the next kit, or shapeless-craft a CHEST with every kit in between (no skipping). Netherite is smithing only. Admin CHEST is creative / `/raidadmin` only.
