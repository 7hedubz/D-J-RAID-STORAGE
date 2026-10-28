# HOPPER

*Hardware Output Port for Payload Extraction and Routing.*

Filter-only: the slot holds **1** item. Matching items extract from RAID **straight to the output** at vanilla hopper speed (8 ticks).

## Ports

Facing the front of the HOPPER:

- **Right** (texture 0009): RAID in (data cable)
- **Left** (texture 0008): items out

If the left neighbor is an inventory, items go there. If it is a data cable, the HOPPER walks cables and inserts into a vanilla inventory. It will not digitize items back into RAID.

Two or more RAID Chassis on the graph blocks pull and push. Locked arrays still extract.

On break, the filter item drops.

Recipe: 8 emeralds around a vanilla hopper.
