# Auto Speer

Client-side Fabric mod for **Minecraft 26.3** that automates the bucket-swap Lunge with the spear.

Hold **F**: the mod charges your attack on the water bucket, then switches to the spear and stabs in the same tick. The Lunge enchantment fires, and the mod switches straight back to the bucket. This repeats about every 0.35–0.4 s while you hold the key.

> [!WARNING]
> Many servers ban macros and automated inputs. Their anti-cheat can detect the stab in the same tick as the slot switch. Use the mod at your own risk, and only where it is allowed.

## Features

- Hold a key (default **F**) for continuous Lunges
- Finds the water bucket and spear in the hotbar automatically and prefers a spear with Lunge
- Randomized spear hold time so the timing varies from lunge to lunge
- Restores your previously selected slot when you release the key
- Client-side only: does not need to be installed on the server and works on vanilla servers

## How it works

A spear stab requires a full attack charge (`minimum_attack_charge = 1.0`). Minecraft measures that charge with the attack speed of the item that was held **in the last tick**.

- The water bucket has no attack speed modifier and is fully charged after 5 ticks.
- The spear itself needs much longer.
- If you switch to the spear and stab **in the same tick**, the stab still uses the bucket's charge.

The mod does exactly that, in one tick:

1. Hold the water bucket until the attack is charged
2. Switch to the spear and stab (Lunge fires)
3. Hold the spear for 0–1 extra ticks, then switch back to the bucket

After switching back, the bucket needs 6 ticks to charge again. One cycle therefore takes at least 7 ticks (0.35 s).

## Requirements

| Component | Version |
|---|---|
| Minecraft | 26.3 |
| Fabric Loader | 0.19.5 or newer |
| [Fabric API](https://modrinth.com/mod/fabric-api) | 0.164.0+26.3 |
| Java | 25 |

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) for Minecraft 26.3
2. Put `auto-speer-1.0.0.jar` and Fabric API into the `.minecraft/mods` folder
3. Start Minecraft with the Fabric profile

## Usage

1. Put a **water bucket** and a **spear** (ideally with Lunge) in your hotbar
2. Hold **F**

If one of the two items is missing, a message appears above the hotbar.

### Notes

- **F** is also the vanilla key for "Swap Item With Offhand". While both use the same key, the mod blocks the offhand swap. You can change the key under *Options → Controls → Key Binds → Auto Speer*.
- Lunge only works with at least 7 food points (survival), not in water, not with an elytra and not while riding.
- Every Lunge costs durability and hunger.

## Adjusting the timing

The timing is set by constants at the top of [`AutoSpeerClient.java`](src/main/java/com/autospeer/AutoSpeerClient.java), in ticks (20 ticks = 1 s):

| Constant | Default | Meaning |
|---|---|---|
| `SPEAR_HOLD_MIN` / `SPEAR_HOLD_MAX` | 0 / 1 | Extra ticks the spear stays in hand after the stab |
| `REACTION_MIN` / `REACTION_MAX` | 0 / 0 | Extra wait after the bucket is charged, before the stab |

One cycle takes `7 + spear hold + reaction` ticks. After changing them, rebuild the mod.

## Building from source

Requires JDK 25.

```bash
./gradlew build
```

The jar ends up in `build/libs/`. To test in a development client:

```bash
./gradlew runClient
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

## Project structure

```
src/main/java/com/autospeer/
├── AutoSpeerClient.java        # Keybind, tick logic, slot search
└── mixin/MinecraftInvoker.java # Access to Minecraft#startAttack
src/main/resources/
├── fabric.mod.json
├── autospeer.mixins.json
└── assets/autospeer/lang/      # en_us, de_de
```

## License

[MIT](LICENSE)
