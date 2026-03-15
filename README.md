# Admin Abuse Addon

A Meteor Client addon made for the Admin Abuse clan. Mainly used on 6b8t and 6b6t.

Servers: 6b8t.eagler.host and 6b6t.org

---

## Installation

1. Install Fabric Loader from fabricmc.net
2. Download Meteor Client and put it in your .minecraft/mods/ folder
3. Download the latest jar from the Releases page
4. Put the jar in your .minecraft/mods/ folder
5. Launch Minecraft with the Fabric profile
6. Open Meteor with Right Shift and look for the Admin Abuse category

---

## Modules

### PVP

**Legit Kill Aura** - Aim assist and trigger bot. Slowly moves your aim towards players and auto clicks when youre looking at them.

**Legit Crystal** - Legit auto crystal. Hold end crystals, look at obsidian and hold right click. It will place and explode crystals automatically.

**Backtrack** - Delays your position packets so you appear to teleport on the enemies screen. Only kicks in when players are nearby.

**Auto Buff** - Throws a strength splash pot at your feet when you start fighting someone.

### Grief

**TNT Placer** - Places TNT in a radius around you. Can auto ignite with flint and steel.

### Utility

**Frame Dupe** - Auto dupes items using a frame dupe plugin. Right clicks to place the item, left clicks to trigger the dupe. Has a speed slider, item whitelist, and protects the frame from being broken.

**Hotbar Manager** - Keeps your hotbar stocked. You pick what item goes in each slot and it pulls them from your inventory automatically.

**Auto Login** - Sends your login command when you join a server. You can set the delay and the command.

**Stash Finder** - Scans chunks for stashes. Finds chests, shulkers, ender chests, spawners, barrels and more. Can send alerts to a Discord webhook or log to a file. Skips trial chambers.

**Chat Logger** - Logs everything in chat to a txt file or Discord webhook with timestamps.

**Advertiser** - Sends messages in chat on a timer. Supports multiple messages, random order, and the delay is in seconds.

**Advanced Msg Aura** - Sends messages to nearby players automatically. Has spam mode, random order, per player messaging and a custom message list.

**Auto GG** - Sends messages on kills, totem pops and deaths. You can customize what it says and it supports player name replacement.

### HUD

**Inventory HUD** - Shows your full inventory on screen including armor, offhand and hotbar. You can scale it and move it anywhere.

---

## Building from Source

You need JDK 21. IntelliJ IDEA is recommended.

Clone the repo:
```
git clone https://github.com/overyr8-sys/Admin-abuse-Addon
cd Admin-abuse-Addon
```

Build:
```
.\gradlew.bat build
```

The jar will be in build/libs/

---

## Credits

Made by Plainguy123 for the Admin Abuse clan.
Built using the Meteor Client addon template.

---

## Disclaimer

Use at your own risk. Made for servers where this stuff is allowed.