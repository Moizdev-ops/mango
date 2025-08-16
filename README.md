# 🥭 MangoParty - Advanced Minecraft Party System

*Automatically synced with your [v0.dev](https://v0.dev) deployments*

[![Deployed on Vercel](https://img.shields.io/badge/Deployed%20on-Vercel-black?style=for-the-badge&logo=vercel)](https://vercel.com/moiz7865s-projects/v0-minecraft-party-system)
[![Built with v0](https://img.shields.io/badge/Built%20with-v0.dev-black?style=for-the-badge)](https://v0.dev/chat/projects/J9y9uOvca6a)
[![Version](https://img.shields.io/badge/version-1.0.0-brightgreen.svg)](https://github.com/your-repo/mangoparty)
[![Minecraft](https://img.shields.io/badge/minecraft-1.21.4-blue.svg)](https://minecraft.net)
[![License](https://img.shields.io/badge/license-MIT-yellow.svg)](LICENSE)

## Overview

This repository will stay in sync with your deployed chats on [v0.dev](https://v0.dev).
Any changes you make to your deployed app will be automatically pushed to this repository from [v0.dev](https://v0.dev).

**MangoParty** is a comprehensive party system plugin for Minecraft servers that provides advanced PvP functionality, including party management, arena-based matches, queue systems, and party vs party duels.

## Deployment

Your project is live at:

**[https://vercel.com/moiz7865s-projects/v0-minecraft-party-system](https://vercel.com/moiz7865s-projects/v0-minecraft-party-system)**

## Build your app

Continue building your app on:

**[https://v0.dev/chat/projects/J9y9uOvca6a](https://v0.dev/chat/projects/J9y9uOvca6a)**

## How It Works

1. Create and modify your project using [v0.dev](https://v0.dev)
2. Deploy your chats from the v0 interface
3. Changes are automatically pushed to this repository
4. Vercel deploys the latest version from this repository

## 🌟 Features

### 🎉 **Party System**
- **Create & Manage Parties**: Form parties with friends using intuitive commands
- **Party Leadership**: Transfer leadership, invite/kick members, and manage party settings
- **Smart Invitations**: Clickable invite system with 60-second expiration
- **Party Chat**: Dedicated communication channel for party members
- **Match Integration**: Seamless integration with all match types

### ⚔️ **Match Types**

#### **1. Party Split Matches**
- Divide your party into two balanced teams
- Fight against your friends in structured team battles
- Automatic team assignment with spawn point allocation
- **Flow**:
  1. Party leader selects "Split Match" from the match GUI
  2. Players are automatically divided into two teams (Team 1 and Team 2)
  3. A 5-second countdown begins with players frozen in place
  4. When countdown ends, players are unfrozen and the match begins
  5. Players can only damage opponents on the opposite team
  6. Last team with surviving members wins
  7. Arena is automatically regenerated after the match

#### **2. Party FFA (Free For All)**
- Last player standing wins
- All party members fight each other
- Perfect for determining the ultimate champion
- **Flow**:
  1. Party leader selects "FFA Match" from the match GUI
  2. All players are teleported to random spawn points in the arena
  3. A 5-second countdown begins with players frozen in place
  4. When countdown ends, players are unfrozen and the match begins
  5. Every player fights against everyone else
  6. Last player standing wins the match
  7. Arena is automatically regenerated after the match

#### **3. Party vs Party Duels**
- Challenge other parties to epic team battles
- Leader-to-leader challenge system with clickable accept/decline
- 60-second challenge expiration timer
- Automatic team assignment (Party 1 vs Party 2)
- **Flow**:
  1. Party leader challenges another party using `/party challenge <leader>`
  2. Challenged party leader receives clickable accept/decline options
  3. If accepted, all members from both parties are teleported to the arena
  4. Players from the challenging party are assigned to Team 1
  5. Players from the challenged party are assigned to Team 2
  6. A 5-second countdown begins with players frozen in place
  7. When countdown ends, players are unfrozen and the match begins
  8. Players can only damage opponents from the other party
  9. Last party with surviving members wins
  10. Arena is automatically regenerated after the match

#### **4. Duels (1v1)**
- Challenge specific players to one-on-one combat
- Kit selection for balanced gameplay
- Best-of-N rounds system
- **Flow**:
  1. Player challenges another using `/duel <player> <kit> <rounds>`
  2. Challenged player receives clickable accept/decline options
  3. If accepted, both players are teleported to the arena
  4. Players' inventories are saved and replaced with the selected kit
  5. A countdown begins with players frozen in place
  6. When countdown ends, players are unfrozen and the round begins
  7. Winner is determined when one player defeats the other
  8. After each round, players are reset and teleported back to spawns
  9. First player to win the specified number of rounds wins the duel
  10. Players' original inventories are restored after the duel ends
  11. Arena is automatically regenerated after the match

#### **5. Queue System (1v1, 2v2, 3v3)**
- **Ranked Matchmaking**: Join queues for competitive matches
- **Kit-Based Queuing**: Queue with specific kits for balanced gameplay
- **Auto-Matching**: Automatic match creation when enough players queue
- **Live Queue Counts**: See how many players are queued for each kit
- **Smart Team Balancing**: Random team assignment for fair matches
- **Flow**:
  1. Players join queue using `/1v1queue`, `/2v2queue`, or `/3v3queue`
  2. Players select a kit from the GUI
  3. System matches players/teams with similar queue times
  4. When enough players are in queue, a match is automatically created
  5. Players are teleported to the arena and assigned to teams
  6. A countdown begins with players frozen in place
  7. When countdown ends, players are unfrozen and the match begins
  8. Last team with surviving members wins
  9. Arena is automatically regenerated after the match

### 🏟️ **Arena Management**
- **Visual Arena Editor**: GUI-based arena setup with click-to-set locations
- **Schematic System**: Automatic arena regeneration using WorldEdit schematics
- **Multi-Arena Support**: Multiple arenas with automatic availability checking
- **Arena Bounds**: Automatic teleportation when players leave arena boundaries
- **Arena Reservation**: Smart arena allocation to prevent conflicts
- **Arena Duplication**: Automatically create instances of arenas with proper offsets for concurrent matches

### 🎒 **Kit System**
- **Custom Kits**: Create kits from player inventories
- **Kit Rules Engine**: Advanced rule system for each kit
  - Natural health regeneration toggle
  - Block breaking/placing permissions
  - Damage multipliers (e.g., +33% damage)
  - Instant TNT explosion
- **Kit Icons**: Custom item icons for GUI representation
- **Kit Editor**: Visual kit rule configuration through GUIs

### 📊 **Match Features**
- **Live Scoreboards**: Real-time match statistics using FastBoard
- **Kill/Death Tracking**: Comprehensive player statistics
- **Spectator System**: Dead players become invisible spectators with flight
- **Match States**: Proper state management (Preparing → Countdown → Active → Ending)
- **Team Management**: Advanced team assignment and tracking
- **Match Duration**: Automatic match timing and duration tracking

### 🎮 **User Interface**
- **Intuitive GUIs**: Beautiful, easy-to-use graphical interfaces
- **Clickable Messages**: Interactive chat messages with hover effects
- **Visual Feedback**: Clear status indicators and progress displays
- **Responsive Design**: Adaptive GUIs that work with different screen sizes

## 📋 Commands

### **Party Commands**
```
/party create                    - Create a new party
/party invite <player>           - Invite a player to your party
/party join <leader>             - Join a party (if invited)
/party transfer <player>         - Transfer party leadership
/party leave                     - Leave your current party
/party disband                   - Disband your party (leader only)
/party match                     - Open match type selection GUI
/party challenge <leader>        - Challenge another party to a duel
/party acceptduel <challenger>   - Accept a party duel challenge
/party declineduel <challenger>  - Decline a party duel challenge
/party info                      - View party information
```

### **Queue Commands**
```
/1v1queue                        - Join 1v1 queue (opens kit selection)
/2v2queue                        - Join 2v2 queue (opens kit selection)
/3v3queue                        - Join 3v3 queue (opens kit selection)
/leavequeue                      - Leave current queue
```

### **Spectator Commands**
```
/spectate <player>               - Spectate a specific player in your match
```

### **Admin Commands**
```
/mango arena editor              - Open arena list GUI
/mango arena editor <name>       - Open editor for specific arena
/mango arena create <name>       - Create a new arena
/mango arena corner1 <name>      - Set arena corner 1
/mango arena corner2 <name>      - Set arena corner 2
/mango arena center <name>       - Set arena center
/mango arena spawn1 <name>       - Set arena spawn point 1
/mango arena spawn2 <name>       - Set arena spawn point 2
/mango arena save <name>         - Save arena schematic
/mango arena list                - List all arenas
/mango arena delete <name>       - Delete an arena
/mango kit editor                - Open kit management GUI
/mango create kit <name>         - Create kit from current inventory

/mango addkitgui <kit> <mode> [slot] - Add kit to GUI configuration
/mango setspawn                  - Set server spawn location
```

## 🏟️ Arena Duplication System

MangoParty features a powerful arena duplication system that allows for concurrent matches using the same arena design:

1. **How It Works**:
   - When all existing arenas are in use, the system automatically creates a new instance
   - The original arena's schematic is copied to a new location with proper offsets
   - All coordinates (spawn points, corners, center) are recalculated relative to the new center
   - The allowed kits configuration is preserved in the new instance

2. **Offset Configuration**:
   - Default X-axis offset: 200 blocks (configurable)
   - Default Z-axis offset: 0 blocks (configurable)
   - Each instance is numbered sequentially (e.g., arena1_instance1, arena1_instance2)

3. **Benefits**:
   - Unlimited concurrent matches using the same arena design
   - No need to manually create multiple copies of the same arena
   - Efficient use of server resources
   - Automatic cleanup when matches end

## 🧩 Technical Implementation

### **Game Mode Architecture**

#### **1. Duels System**
- **Core Classes**: `Duel.java`, `DuelManager.java`, `DuelListener.java`
- **Implementation Details**:
  - Duels are managed through the `Duel` model class which tracks challenger, target, kit, rounds, and state
  - `DuelManager` handles duel creation, acceptance, and round management
  - `DuelListener` controls damage events, ensuring players can only damage opponents in the same duel
  - Round-based system with win tracking and automatic next round initialization
  - Player inventories are saved and restored using deep copy methods
  - Countdown system freezes players in place during preparation

#### **2. Party Split Matches**
- **Core Classes**: `Match.java`, `MatchManager.java`, `DuelListener.java`
- **Implementation Details**:
  - Uses the `Match` model with a `playerTeams` map to track team assignments
  - Players are divided into Team 1 and Team 2 using the `assignTeams()` method
  - The `isPartySplitMatch` flag enables team-based damage control
  - `DuelListener` checks team affiliation before allowing damage between players
  - 5-second countdown with player movement restriction
  - Arena regeneration occurs after match completion

#### **3. Party FFA**
- **Core Classes**: `Match.java`, `MatchManager.java`, `DuelListener.java`
- **Implementation Details**:
  - Uses the `Match` model without team restrictions
  - All players are teleported to random spawn points
  - No team assignments, allowing all players to damage each other
  - Last player standing detection through elimination tracking
  - 5-second countdown with player movement restriction
  - Arena regeneration occurs after match completion

#### **4. Party vs Party**
- **Core Classes**: `Match.java`, `MatchManager.java`, `DuelListener.java`
- **Implementation Details**:
  - Uses the `Match` model with the `assignPartyVsPartyTeams()` method
  - Challenge system with 60-second expiration timer
  - Team assignments based on party membership (Party 1 = Team 1, Party 2 = Team 2)
  - `DuelListener` checks team affiliation before allowing damage
  - 5-second countdown with player movement restriction
  - Arena regeneration occurs after match completion

#### **5. Queue System**
- **Core Classes**: `Queue.java`, `QueueManager.java`, `MatchManager.java`
- **Implementation Details**:
  - Queue entries stored with player/party information, kit selection, and queue time
  - Automatic matching based on queue duration and team size
  - Kit-specific queues to ensure balanced gameplay
  - Match creation when enough players/teams are available
  - Integration with the match system for actual gameplay

### **Common Components**

#### **Arena Management**
- **Core Classes**: `Arena.java`, `ArenaManager.java`
- **Implementation Details**:
  - WorldEdit integration for schematic saving and loading
  - Arena reservation system to prevent conflicts
  - Automatic regeneration after matches
  - Arena duplication for concurrent matches

#### **Kit System**
- **Core Classes**: `Kit.java`, `KitManager.java`
- **Implementation Details**:
  - Inventory-based kit creation and application
  - Custom rule engine for kit-specific gameplay modifications
  - GUI-based kit selection and management

## 🔧 Installation

1. Download the latest release from the GitHub repository
2. Place the JAR file in your server's `plugins` folder
3. Start or restart your server
4. Configure the plugin settings in the generated configuration files

## 📦 Dependencies

- **Required**:
  - Spigot/Paper 1.21.4+
  - WorldEdit or FastAsyncWorldEdit

- **Optional**:
  - PlaceholderAPI (for additional placeholders)
  - Vault (for economy integration)

## 🛠️ Configuration

### **config.yml**
```yaml
# Debug mode (enables additional logging)
debug: false

# Party settings
party:
  max-size: 8
  invite-timeout: 60

# Match settings
match:
  countdown: 5
  min-players: 2
  max-duration: 300

# Arena settings
arena:
  auto-regenerate: true
  regeneration-delay: 5

# Scoreboard settings
scoreboard:
  update-interval: 20
  title: "&#FFD700&lMangoParty"
```

### **scoreboard.yml**
```yaml
global:
  update-interval: 20
  title: "&#FFD700&lMangoParty"

party_ffa:
  title: "&#FFD700&lParty FFA"
  lines:
    - "&7"
    - "&#FF6347Arena: &f%arena%"
    - "&#FF6347Kit: &f%kit%"
    - "&7"
    - "&#FF6347Players: &f%alive%/%total%"
    - "&#FF6347Kills: &f%kills%"
    - "&7"
    - "&#FF6347Time: &f%time%"
    - "&7"
    - "&#FFD700play.yourserver.com"

party_split:
  title: "&#FFD700&lParty Split"
  lines:
    - "&7"
    - "&#FF6347Arena: &f%arena%"
    - "&#FF6347Kit: &f%kit%"
    - "&7"
    - "&#FF6347Team 1: &f%team1_alive%/%team1_total%"
    - "&#FF6347Team 2: &f%team2_alive%/%team2_total%"
    - "&7"
    - "&#FF6347Your Kills: &f%kills%"
    - "&7"
    - "&#FF6347Time: &f%time%"
    - "&7"
    - "&#FFD700play.yourserver.com"
```

## 🔄 Recent Code Improvements

### **1. Code Organization**
- Reorganized class member variables in `MangoParty.java` for better readability
- Grouped related variables (Managers, GUIs, Listeners) with clear comments

### **2. Method Extraction**
- Extracted initialization logic into separate methods in `MangoParty.java`:
  - `initializeManagers()` - Centralizes manager initialization
  - `initializeGuis()` - Handles GUI component creation
  - `initializeListeners()` - Creates listener instances
  - `registerListeners()` - Registers all event listeners
  - `cleanupManagers()` - Handles proper shutdown

### **3. Command Registration**
- Created a helper method `registerCommand()` to streamline command registration
- Added null checks and error logging for failed command registrations

### **4. Utility Improvements**
- Optimized `HexUtils.java` by moving version check to static initializer
- Added comprehensive Javadoc comments to utility classes

### **5. Scoreboard System Refactoring**
- Improved `ScoreboardManager.java` with helper methods:
  - `getConfigSectionForMatchType()` - Determines scoreboard config section
  - `getScoreboardTitle()` - Retrieves and colorizes titles
  - `createScoreboardsForPlayers()` - Handles bulk scoreboard creation
  - `createScoreboardForPlayer()` - Creates individual player scoreboards
  - `startUpdateTask()` - Manages scoreboard update tasks
  - `getQueueConfigSection()` - Handles queue-specific configurations
  - `updatePlayerMatchScoreboard()` - Updates scoreboard for a specific player
  - `processDuelPlaceholders()` - Processes placeholders for duel scoreboards
  - `processTeamPlaceholders()` - Handles team-specific placeholder processing
  - `formatMatchDuration()` - Formats match duration into readable time string

### **6. Null Safety**
- Added null checks to critical methods to prevent NullPointerExceptions
- Improved parameter validation in public methods

### **7. Code Immutability**
- Declared appropriate fields as `final` to ensure they're assigned only once
- Enhanced code safety and readability

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 👥 Credits

- **Author:** Moiz
- **Contributors:** [List of contributors]
- **Libraries:**
  - FastBoard for scoreboard management
  - WorldEdit/FAWE for arena management

---

© 2023 MangoParty. All rights reserved.
