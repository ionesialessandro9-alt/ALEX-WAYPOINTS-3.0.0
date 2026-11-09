# ALEX WAYPOINTS 3.0 — Dawn Client / Minecraft Java 1.21.8

This project contains a Fabric client mod and a separate Windows companion app. The app shows a live capture of the Minecraft client window and draws the nearest waypoint marker on that copy.

## Commands in Minecraft
- `/alexwp add Casa` — save your current position.
- `/alexwp addpos Casa 100 70 200` — save explicit coordinates.
- `/alexwp list`
- `/alexwp remove Casa`

## Install
1. Build with GitHub Actions.
2. Install the `ALEX-Waypoints-Fabric-1.21.8` JAR in the active Dawn profile's `mods` folder.
3. Launch Dawn with Fabric on Minecraft 1.21.8 and enter a world.
4. Run `ALEX-Waypoints.exe` from the desktop artifact.
5. Select the Minecraft window in the app if needed.

The companion searches Dawn profiles for `.minecraft/config/alex-waypoints-live.json`. If multiple profiles contain that file, the first match is used.

The marker projection currently assumes a 70-degree FOV. Change the 70 value in `overlay/alex_overlay.py` to match your Minecraft FOV for better alignment.

The companion captures the visible Minecraft window using desktop capture. For OBS, capture only the Minecraft game window and test a recording; do not use full Display Capture if you want the companion window excluded.
