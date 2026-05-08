# Crystal Match - Match-3 Game

Crystal Match is an Android match-3 game built with Kotlin.

The player combines three or more matching crystals, scores points, unlocks levels, and progresses through a city map. The project was created as an Android game prototype with animated transitions, saved progress, and several planned expansion points.

## Installation and Running

1. Clone the repository.

   ```bash
   git clone <repository-url>
   cd ThreeInRowWithHistory
   ```

2. Open the project in Android Studio.

   - Start Android Studio.
   - Select "Open an existing project".
   - Choose the project folder.

3. Run the game.

   - Connect an Android device or start an emulator.
   - Press Run in Android Studio.
   - Select a target device.

## Game Features

### Core Gameplay

- Classic match-3 mechanics.
- Psychedelic visual effects.
- Level system with increasing difficulty.
- Timer for each level.

### Additional Features

- Animated transitions.
- Progress saving.
- City map navigation.
- Inventory with a crystal collection.

### Goal

- Match three or more identical crystals.
- Score points and complete levels.
- Unlock new locations on the city map.
- Collect unique crystals in the inventory.

## Technical Details

### Technologies

- Kotlin.
- Android SDK.
- Material Design.
- SharedPreferences for progress saving.

### Project Structure

```text
app/
|-- src/
|   |-- main/
|   |   |-- java/
|   |   |   +-- com/example/threeinrowwithhistory/
|   |   |       |-- MainActivity.kt
|   |   |       |-- GameActivity.kt
|   |   |       |-- LevelMapActivity.kt
|   |   |       +-- CityMapActivity.kt
|   |   +-- res/
|   |       |-- layout/
|   |       |-- drawable/
|   |       +-- anim/
```

## How It Works

1. Main menu.
   - Start the game.
   - Choose a mode:
     - Crystal Farm - main mode.
     - Inventory - crystal collection, in development.
     - City Map - location navigation, in development.

2. Gameplay.
   - Select a level on the map.
   - Solve the match-3 puzzle.
   - Match crystals.
   - Score points and unlock new levels.

3. Progress system.
   - Save achievements.
   - Unlock new locations.
   - Collect crystals.
   - Improve player progress.

## Purpose

The main idea is to create an entertaining game that trains logical thinking, has a colorful visual style, and saves player progress.

## System Requirements

- Android 6.0 or newer.
- At least 2 GB RAM.
- 100 MB of free storage.

## License

MIT License. Free use and modification.

## Author

Roman Shably, ShablySoft.
