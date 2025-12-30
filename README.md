# dfgreRankGift

A Minecraft 1.21 plugin developed for players to gift ranks to each other using a custom "Gold" currency.

## Features

* **Book Interface:** All gifting processes are managed through interactive Minecraft books.
* **Confirmation System:** Sent gifts are held until the recipient approves; if rejected or timed out, the gold is refunded.
* **LuckPerms Integration:** Rank definitions are command-based, making it fully compatible with LuckPerms and other permission plugins.
* **Language Support:** Comes with built-in Turkish and English language files.
* **Update Checker:** Notifies administrators when a new version is available on SpigotMC.

## Commands

* `/gift <player>` - Opens the gift menu for the specified player.
* `/gold` - Shows your current gold amount.
* `/goldadmin <add/remove/set> <player> <amount>` - Gold management for administrators.
* `/gift reload` - Reloads the configuration and language files.

## Permissions

* `dfgrerankgift.admin` - Grants access to all admin commands and update notifications.

## Installation

1. Place the `dfgreRankGift-1.0.1.jar` file into your server's `plugins` folder.
2. Start the server and configure ranks and commands in the generated `config.yml`.
3. Set your preferred language (`en` or `tr`) in the `config.yml`.

---

### Developer Note
This plugin was developed by **dfgre** for free.
Discord: **dfgre**
