# RAID Chassis

*A RAID Chassis aggregates digitized item data from attached drives. Capacity is limited only by how ridiculous you are willing to be.*

The Chassis is the array. Digitized items live on this block, not in the CHEST. Every player on the same cable graph shares that storage.

- One Chassis per cable graph. Two Chassis on the same cables put every CHEST on that graph into **error**.
- No Chassis in range → **disconnected**.
- Breaking a Chassis keeps items on the dropped block (shulker-style). Place it again to restore the array.
- If connected CHEST capacity is then too small, the array stays **locked** until you extract items or add CHEST capacity.
- Storage is per Chassis, not world-global.
- Opening a CHEST loads the Chassis chunk so storage can be read.

Cables attach on the **back** face only.
