# Changelog

## 1.0.0 (2026-10-30)

Needs CommandDeck 1.1.0 or later.

- **33 ready-made cheat buttons** in CommandDeck's Macros tab (`/deck`), in seven categories:
  - **Start here:** turn cheats on or off (`/op self`, which singleplayer needs once per world; pressing it again turns operator status off).
  - **Items:** iron and adamantite loadouts, workbench upgrade materials, all ability items, and three bundles (Builder's stack, Ore and bars, Provisions).
  - **Progress:** unlock all memories, fill signature energy, max out health and stamina, a bigger backpack.
  - **Movement:** back to spawn, unstuck, set home and go home, and spectator flight on and off.
  - **Time and weather:** morning, midnight, endless noon, start the clock again, clear skies, normal weather.
  - **Health and game mode:** heal, creative and adventure mode, back to your bed, no lives limit.
  - **World:** peaceful, monsters back, freeze and unfreeze creatures.
- **Every button shows the commands it runs**, and descriptions say whether a button affects only you or the whole world.
- **Permission `cheatmenu.use`:** operators and the owner of a singleplayer world can use the buttons; on a server, the operator decides who else can. Players without it don't see the buttons, and every button also starts with `/cheatmenu check`, which stops it before anything else runs. The game still checks every command. In singleplayer, press "Turn cheats on or off" (or type `/op self`) once first: the game's own commands are refused until you're an operator. It's a switch, so server operators shouldn't press it.
