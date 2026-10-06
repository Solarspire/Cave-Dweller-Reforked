# Natural spawning

The modern ports retain the natural-spawn logic from Cadentem's
`cave_dweller-1.20.1-1.6.4.jar`. Fabric's start-server-tick event and NeoForge's
pre-server-tick event both invoke `CaveDweller.serverTick`, which selects an
eligible player, advances the dimension timer, and calls `Utils.trySpawnMob`.
There is no movement or AFK check.

With the default `config/cave_dweller.json`, an eligible player must be alive,
in Survival or Adventure, in the Overworld, at Y <= 40, and underground.
Creative and Spectator players are excluded. Surface spawning is disabled.
The sky-light limit is 8 and block-light limit is 15; torchlight alone does
not disqualify a player at the default block-light setting.

If no player meets these conditions, both the spawn and noise timers pause.
The spawn timer normally lasts 300-600 seconds, with a 40% chance of choosing
a 1200-second cooldown instead. After it expires, each tick has a 0.5% chance
of attempting a spawn. This is eligible, unpaused game time; it is not a
guaranteed deadline. Closing/reopening the world resets its timers.

Each attempt tries 40 random positions within 35 blocks horizontally and
6 blocks vertically of the selected player. The mob needs a suitable floor,
room for its body, at least 16 blocks from nearby players, and a navigable
path to the victim. A tiny sealed AFK room can prevent all candidates from
passing. Existing loaded dwellers also count toward the maximum of 5.
New natural spawns start invisible, as in the original JAR.

For a normal AFK check, use Survival in a roomy underground cave at Y <= 40,
leave paths open to surrounding cave space, and keep the game unpaused.
Wait at least 20 minutes of eligible game time. To detect a loaded dweller
without relying on seeing its initial invisible phase, enable commands and use:

```mcfunction
/execute if entity @e[type=cave_dweller:cave_dweller] run say A Cave Dweller exists
```

For a quick diagnostic, stop the world/server, set the following values in
its existing JSON config, restart, and repeat the cave check. Restore the
normal values afterwards. All player, distance, lighting and path checks
remain active.

```json
{
  "can_spawn_min": 5,
  "can_spawn_max": 5,
  "can_spawn_cooldown_chance": 0.0,
  "spawn_chance_per_tick": 1.0
}
```

`tools/smoke_natural.py` uses these accelerated timers in a disposable
localhost production server and connects a real development client. It
checks Creative/Spectator/surface exclusions, then observes an AFK Survival
player in a covered cave at Y=-60. It uses the packaged mod with bundled
GeckoLib and never summons a dweller or uses a spawn egg. See VALIDATION.md
for the results and remaining limitations.
