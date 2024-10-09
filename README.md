# the stalker
omg so spooky 🤯
pretty... terrible fabric mod for 1.20.1,
made because october (not really, i was bored but good timing ig)

mainly made to figure out how to make custom entities/ai, good chance this will not be worked on,
dont expect anything great lol

## states :
- IDLE
- STALKING
- ATTACKING
- HIDDEN
- TAKING_COVER
- STARING

# stalker behaviour (shoutout to claude frfr)

## States
1. **IDLE**
    - Default state when no player is within range
    - May transition to STALKING if a player comes within range

2. **STALKING**
    - Main state for following and observing the player
    - Moves randomly around the player
    - Can transition to:
        - ATTACKING (based on aggression)
        - STARING (10% chance when observed)
        - HIDDEN (50% chance when observed)
        - TAKING_COVER (40% chance when observed)

3. **ATTACKING**
    - Actively attempts to harm the player
    - Triggered by high aggression
    - Resets aggression meter after attack
    - May return to STALKING if aggression check fails

4. **HIDDEN**
    - Becomes invisible
    - Teleports to a random location 15-60 blocks away
    - Remains hidden for up to 200 ticks (10 seconds)

5. **TAKING_COVER**
    - Moves behind obstacles to break line of sight
    - Can transition back to STALKING if unseen

6. **STARING**
    - Directly looks at the player for 60 ticks (3 seconds)
    - Plays spooky sounds every second
    - Returns to STALKING after duration or if player looks away

## Aggression Mechanics
- Increases over time (every 1200 ticks/60 seconds)
- Affected by:
    - Player eating (+1.5)
    - Player taking damage (+2.0)
    - Time of day (13000 ticks: +25, 0 ticks: -25)
    - Being observed (-0.005 per tick)
- Higher aggression increases attack probability

## Special Behaviors
1. **Teleportation**
    - Used when transitioning to HIDDEN state
    - Ensures safe landing spot
    - Maintains distance between 15-60 blocks from player

2. **Observer Awareness**
    - Can detect when player is looking at it
    - Triggers state changes and behavior adjustments

3. **Visual Effects**
    - Spawns end rod particles around itself (for debugging, will prolly be removed later)
    - Plays enderman sounds for ambiance
