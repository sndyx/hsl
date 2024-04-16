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
- [Teleport](#teleport-player)
- [Send to Lobby](#send-to-lobby)

<!--- END -->

### Apply Layout

```rust
apply_layout("my_layout")
```

### Potion Effect*

```rust
// effect, duration, level, override_existing_effects
potion_effect("strength", 100, 2, false)
```

*currently not implemented

### Balance Player Team

```rust
balance_player_team()
```

### Cancel Event

```rust
cancel_event()
```

### Change Player Group

```rust
// group, protect_demotion
change_player_group("my_group", true)
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
display_menu("my_menu")
```

### Display Title

```rust
// title, subtitle, fadein, stay, fadeout
display_title("My title", "My subtitle", 1, 1, 1)
```

### Enchant Held Item*

```rust
// enchantment, level
enchant_held_item("sharpness", 5)
```

*currently not implemented

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
full_heal()
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

### Play Sound*

*currently not implemented

### Send Message

```rust
send_message("My message!")
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
set_player_team("my_team")
```

### Use Held Item

```rust
use_held_item()
```

### Set Gamemode

```rust
set_gamemode("Creative")
```

### Set Compass Target*

*currently not implemented

### Teleport

*currently not implemented

### Send to Lobby

```rust
// lobby
send_to_lobby("UHC Champions")
```