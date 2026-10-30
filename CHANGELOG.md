# D&J RAID Storage — patch notes

## 0.1.0
- First playable: RAID Chassis, CHEST terminals, data cables, digitize/extract, shared per-array storage.

## 0.1.1–0.1.4
- Prism jar workflow, digitizer slot, capacity bar, Chassis persist (`raid:storage` on the item).

## 0.1.5
- Shift-right-click upgrade kits work on placed CHESTs (`Item.useOn`).

## 0.1.6
- CHEST block sounds match the material they are based on.

## 0.1.7
- CHEST/RAID facing the player; cables only on the back port; animated fronts; HOPPER-style thin cables.

## 0.1.8
- Cable lighting: no full-block shadows (`cutout`, no occlusion).

## 0.1.9
- Cables are a uniform 6×6 tube (no hub bump) with the orange covered/core textures.

## 0.1.10
- Vanilla hoppers can insert into valid data cables (one RAID, room left).

## 0.1.11
- Netherite CHEST upgrade is smithing only (Netherite template + Amethyst upgrade + netherite ingot).

## 0.1.12
- Shapeless CHEST promotion: current CHEST + every upgrade kit in between.

## 0.1.13
- CHEST GUI: capacity bar no longer overhangs the item grid (bar width 162, grid moved down).

## 0.1.15
- Tools and weapons cost 16 bytes each.
- Capacity bar uses formatted units (B/KB/MB/GB/TB/PB).

## 0.1.16
- Netherite CHEST capacity 9216 B.

## 0.1.17
- Admin CHEST (101376 B, netherite look, creative-only). CHEST tooltips show storage size.

## 0.1.18
- `/raidadmin` SavedData + sync for Admin CHEST capacity.

## 0.1.19
- `/raidadmin <amount> [B|KB|MB|GB]` (op). Admin CHEST tooltip shows that command.

## 0.1.20
- Capacity display steps units at **2048** (2048 B → KB, and so on).

## 0.1.21
- HOPPER block (item extractor): filter slot, hopper speed (2.5/s), left output / right RAID-in textures.

## 0.1.22
- All addon blocks hardness 1. CHEST blast resistance = max(2× material, previous tier); Chassis 2400.

## 0.1.23
- Cable block sounds (break/step/place/hit/fall) and CHEST open/close (no pitch shift).
- RAID Chassis explosion resistance 2400.

## 0.1.24
- HOPPER pulls RAID items into its slot, then pushes extras out the output face.
- Cables attach on the HOPPER output face and can feed chests/furnaces through the pipe.

## 0.1.25
- Temporary HOPPER left/right swap (reverted in 0.1.27).

## 0.1.26
- Cable break sound no longer uses `cableBreak2`.

## 0.1.27
- HOPPER ports restored: RAID cable on the **right** (0009), items out the **left** (0008).

## 0.1.28
- HOPPER recipe: 8 emeralds around a vanilla hopper.

## 0.1.29
- Breaking a HOPPER drops whatever is in its slot.

## 0.1.30
- Only **empty** shulkers, bundles, and RAID Chassis can be digitized. Filled containers are refused.

## 0.1.31
- Display name was briefly DoHeal’s RAID Storage (superseded by 0.1.32).

## 0.1.32
- Display name: **D&J RAID Storage**.

## 0.1.33
- Mod description: “Digital storage without all that energy stuff”.

## 0.1.34
- Data cable recipe uses iron and copper **ingots** instead of nuggets (still crafts 8).

## 0.1.35
- Traveler’s Backpacks cannot be digitized (empty or filled).

## 0.1.36
- Search box can be clicked again after picking a search result.

## 0.1.37
- CHEST terminal groups items by creative-tab type (Building, Ingredients, Food & Drinks, Tools, Combat, …) with section headers.

## 0.1.38
- Click the upper CHEST panel while holding an item to digitize it (not only the dark input slot).
- Left-click a stored item: 1 onto the cursor. Shift-left-click: up to 64 onto the cursor. Right-click still pulls 1 into the inventory.
- Data cables no longer attach to a furnace/smoker/blast furnace **bottom** (output). Use a hopper on that face.

## 0.1.39
- Colored Blocks header is **Decoration Blocks**.
- Crops, seeds, sugarcane, and food sit under **Farming and Food**.

## 0.1.40
- Group headers drop “Blocks” (Building, Decoration, Redstone, Crafting, …).
- Crafting stations (table, smithing, loom, brewing, furnace, …) split out of Functional.
- Banners (including ominous) and decorated pots go under **Decoration**.
- **Ores**, **Ingots**, and **Gems** are their own groups.

## 0.1.41
- CHEST clicks use 26.3 mouse buttons: left/shift pull onto the cursor, right-click pulls 1 into the inventory.

## 0.1.42
- Left-click a stored item: 1 into the inventory. Right-click: a full stack into the inventory.

## 0.1.43
- Doors and boats are **Functional**. String is **Ingredients**.
- Enchanted books have their own group.
- Each non-vanilla mod (including RAID) has its own group.

## 0.1.44
- Shift-click a stored item: one full stack into the inventory. Right-click: every matching item into the inventory.

## 0.1.45
- Shift is read from the keyboard, not the mouse packet — shift-left pulls a full stack.
- Right-click pulls the stored count (e.g. 6000 dirt), filling the inventory until it is full.

## 0.1.46
- RAID blocks mine faster with a pickaxe (`minecraft:mineable/pickaxe`).

## 0.1.47
- Low space warning when fewer than 16 bytes remain.
- RAID tooltip lore uses `#240DBD`.
- HOPPER filter holds 1 item; matching items extract straight to the output.
- Sort headers are 12px bars, not item boxes.
- Digitizer slot removed; shift-click and dropping into the upper panel insert into RAID.
- CHEST open/close use 16-block attenuation (volume unchanged).
- Flower pots, decorated pots, and potted plants are **Decoration**.
- Iron/gold/copper/netherite blocks are **Ingots**.

## 0.1.48
- CHEST/Chassis byte counts are italic.
- Diamond/emerald/lapis/amethyst/quartz blocks are **Gems**.
- Partially off-screen terminal rows stay visible (clipped).
- CHEST open/close use linear falloff out to 16 blocks.

## 0.1.49
- Terminal item counts are smaller and use k/M so large numbers do not run into the next slot.

## 0.1.50
- CHEST open/close keep 16-block falloff and use variable-range events so Sound Physics Remastered can muffle through walls again.
- README is a short pointer; docs live on the GitHub wiki.

## 0.1.51
- **AUTO-CRAFTER**: Automated Universal Terminal for Outputting Crafted Resources Automatically, Fulfilling Technological Engineering Requests.
- Top 3×3 remembers a recipe. Right-click a cell to clear it. Click the side to toggle crafting. Invalid recipes show the error face.
- Pulls ingredients from RAID (cable on the right). Pushes the result out the left. Ingredients only enter RAID via hoppers.
- Recipe: shapeless Amethyst CHEST + vanilla Crafter.
- Hoppers can insert into RAID (cables, Chassis, CHESTs, and HOPPERs).

## 0.1.52
- Right-click the AUTO-CRAFTER to open a crafting menu. An On/Off button toggles auto-crafting.

## 0.1.53
- AUTO-CRAFTER grid items sit upright in the squares and turn to face the nearest player within 3 blocks.

## 0.1.54
- Cable footsteps caption is **Footsteps**.
- AUTO-CRAFTER error face when on with no RAID or a full array. Recipe is polished granite around a crafter, Quartz CHEST bottom-middle.
- Machine GUIs play on/off SFX when opened and closed (HOPPER, AUTO-CRAFTER, TREE-FARM, COLLECTOR, LOOKER).
- **TREE-FARM**, **COLLECTOR**, and **LOOKER** added.

## 0.1.55
- LOOKER watches **left** only and pulses redstone **right** only. GUI labels are Current Count / Count Threshold / Updates per Increment, with the number to the right of `+`.
- TREE-FARM accepts poplar saplings.
- Unstackable items (filled buckets, tools, armor, anything with max stack 1) cost **16 bytes each**.
- CHEST capacity bar and item list refresh while the terminal is open.
- **Nether TREE-FARM** for crimson/warped fungi.
- Emerald HOPPER textures. Emissive overlays on Chassis, LOOKER, and other multi-state fronts.

## 0.1.56
- LOOKER GUI rows no longer overlap: label, then `-` `+` and the number to the right of `+`.
- Machine front emissives use the same 10-tick frametime as the base textures.
- LOOKER blink is 0002 again (not a fast 0001/0002 strobe). Error glow stays on the front; the back is blank.

## 0.1.57
- LOOKER faces remapped from the pack: standby 0001 / blink 0002, detect 0003–0004, pulse 0005–0006, error 0007–0008, top 0009, left 0010, back/bottom 0011 (no emissive), right 0012 / 0013 while powered.
- Pulse resets the counter, shows 0005–0006 and right 0013, and outputs 15 for **exactly 2 ticks**.

## 0.1.58
- LOOKER detect plays **0003** for 5 ticks then **0004** for 5 ticks, then standby. Threshold skips detect and plays **0005** then **0006** (quarter second each). Redstone is still 2 ticks with right **0013**.
- Nether TREE-FARM uses its own textures and GUI open/close sounds.

## 0.1.59
- Nether TREE-FARM faces: 0001–0006 front (same as overworld), **0007** bottom, **0008** right, **0009** left, **0010** top, **0011** back.

## 0.1.60
- Generic emissive overlays sit next to each machine’s textures (`front_ok_e` / `front_off_e` / `front_error_e`), same layout as Chassis and LOOKER. Shared `textures/block/emissive/` dump removed.

## 0.1.61
- BetterNether saplings (and tree seeds) work in the Nether TREE-FARM when BetterNether is installed.
- **End TREE-FARM** appears when BetterEnd is installed: Thallasium ingots around a stonecutter, Quartz CHEST bottom-middle. BetterEnd tree saplings grow their matching logs.

## 0.1.62
- Left/right are from looking at the front. Default RAID-in (and LOOKER emit) is your **right**; LOOKER watches your **left**. Top LOOKER arrow points at emit.
- HOPPER, AUTO-CRAFTER, TREE-FARM, Nether TREE-FARM, End TREE-FARM, COLLECTOR, and LOOKER GUIs have **Swap Sides**.
- LOOKER still counts while 0003–0006 play. Threshold is **>=**.

## 0.1.63
- Nether TREE-FARM default left/right art restored. Swap Sides still flips it.

## 0.1.64
- Swap Sides sits in the far top-right on every machine GUI, with **In - Out** / **Out - In** under it.
- HOPPER RAID-in / item-out mechanics swapped; textures unchanged.
- AUTO-CRAFTER default left/right swapped. No output attached → error and it will not craft (result is not stuffed back into RAID).

## 0.1.65
- Nether TREE-FARM Swap Sides also flips the top and back (LOOKER-style 180°).
- HOPPER and TREE-FARM GUIs grown upward so Swap Sides is not on the filter/sapling.
- AUTO-CRAFTER will not dump into a RAID array. HOPPER and AUTO-CRAFTER output round-robin to every valid inventory on the output graph.

## 0.1.66
- AUTO-CRAFTER outputs to RAID arrays again, including its own, still round-robin with other valid inventories.

## 0.1.67
- Nether TREE-FARM Swap Sides flips front and back longways (pink/teal swap, red stays on the bottom). Top is still 180°. The screen LED mirrors with the front.

## 0.1.68
- TREE-FARM (all variants) no longer shows the In - Out label under Swap Sides.

## 0.1.69
- TREE-FARM GUIs (all variants) widened so the title does not overlap Swap Sides.

## 0.1.70
- Replaced all machine and cable SFX from the updated sounds pack. Cable break now also uses break2.

## 0.1.71
- TREE-FARM recipe is now logs + bone block + stonecutter + diorite + Quartz CHEST. End TREE-FARM uses thallasium, bone block, stonecutter, phantom membrane, Quartz CHEST. Nether TREE-FARM top-middle is a bone block.
- CHEST search matches item tags as well as names. Stone (and other building-tab blocks) group under Building, not Natural.
- New overworld and End TREE-FARM textures. Nether TREE-FARM Swap Sides no longer flips front/back/top; left/right use 0008/0008B and 0009/0009B.

## 0.1.72
- End TREE-FARM faces: left 0008, right 0010, back 0008, bottom 0009, top 0007. Swap Sides only swaps left/right.
