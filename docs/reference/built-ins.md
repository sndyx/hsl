Docs / Reference / [Built-Ins](built-ins.md)

---

# Built-Ins

Built-in functions map 1:1 with housing actions.

<!--- TOC -->

- [Apply Layout](#apply-layout)
- [Potion Effect](#potion-effect)
- [Balance Player Team](#balance-player-team)
- [Cancel Event](#cancel-event)
- [Change Player Group](#change-player-group)
- [Clear Effects](#clear-effects)
- [Close Menu](#close-menu)
- [Action Bar](#action-bar)
- [Display Menu](#display-menu)
- [Display Title](#display-title)
- [Enchant Held Item](#enchant-held-item)
- [Exit](#exit)
- [Fail Parkour](#fail-parkour)
- [Full Heal](#full-heal)
- [Give Experience Levels](#give-experience-levels)
- [Give Item](#give-item)
- [Spawn](#spawn)
- [Kill](#kill)
- [Send to Parkour Checkpoint](#send-to-parkour-checkpoint)
- [Pause](#pause)
- [Play Sound](#play-sound)
- [Send Message](#send-message)
- [Reset Inventory](#reset-inventory)
- [Remove Item](#remove-item)
- [Set Player Team](#set-player-team)
- [Use Held Item](#use-held-item)
- [Set Gamemode](#set-gamemode)
- [Set Compass Target](#set-compass-target)
- [Teleport Player](#teleport-player)
- [Send to Lobby](#send-to-lobby)

<!--- END -->

### Apply Layout

```rust
set_layout("my_layout")
```

### Potion Effect

```rust
// effect, duration, level, override_existing_effects
effect("strength", 100, 2, false)
```

### Balance Player Team

```rust
balance_team()
```

### Cancel Event

```rust
cancel_event()
```

### Change Player Group

```rust
// group, protect_demotion
set_group("my_group", true)
```

### Clear Effects

```rust
clear_effects()
```

### Close Menu

```rust
close_menu()
```

### Action Bar

```rust
action_bar("My message!")
```

### Display Menu

```rust
open_menu("my_menu")
```

### Display Title

```rust
// title, subtitle, fadein, stay, fadeout
title("My title", "My subtitle", 1, 1, 1)
```

### Enchant Held Item

```rust
// enchantment, level
enchant_held_item("sharpness", 5)
```

### Exit

```rust
exit()
```

### Fail Parkour

```rust
fail_parkour("My message!")
```

### Full Heal

```rust
heal()
```

### Give Experience Levels

```rust
// levels
give_exp_levels(3)
```

### Give Item

```rust
// item, allow_multiple, inventory_slot, replace_existing
give_item(my_item, false, 0, false)
```

### Spawn

```rust
spawn()
```

### Kill

```rust
kill()
```

### Send to Parkour Checkpoint

```rust
parkour_checkpoint()
```

### Pause

```rust
// ticks
pause(20)
```

### Play Sound

```rust
// sound, volume, pitch, location
sound("Note Pling", 1.0, 1.0, "invokers_location")
```

### Send Message

```rust
message("My message!")
```

### Reset Inventory

```rust
reset_inventory()
```

### Remove Item

```rust
remove_item(my_item)
```

### Set Player Team

```rust
set_team("my_team")
```

### Use Held Item

```rust
remove_held_item()
```

### Set Gamemode

```rust
set_gamemode("Creative")
```

### Set Compass Target

```rust
// location
set_compass_target(<1, 2, 3>)
```

### Teleport Player

```rust
// location
tp(<1, 2, 3>)
```

### Send to Lobby

```rust
// lobby
send_to_lobby("UHC Champions")
```
