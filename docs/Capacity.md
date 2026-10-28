# Capacity

First connected CHEST of the largest tier contributes its full bytes. Each extra CHEST adds **half of its own tier**. Recalculated when you add or remove a CHEST. Over capacity → **locked** (extract only).

## Byte cost

- First item of a stack: 16 bytes
- Each extra item: 1 byte
- Tools and weapons: 16 bytes each

Display units step at **2048** of the current unit: B → KB → MB → GB → TB → PB.

## CHEST tiers

| CHEST | Bytes |
|---|---:|
| Stone | 512 |
| Copper | 1024 |
| Iron | 2048 |
| Gold | 3072 |
| Quartz | 4096 |
| Amethyst | 5120 |
| Diamond | 6144 |
| Netherite | 9216 |
| Admin | 101376 (op-configurable) |

Admin default is 101376 bytes. Change it with [`/raidadmin`](Commands).
