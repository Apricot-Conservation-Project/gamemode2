package siege;

import arc.*;
import arc.util.*;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.mod.*;

public class SiegePlugin extends Plugin {

    @Override
    public void init() {
        System.out.println("SiegePlugin loaded");

        Gamedata.reset();
        Setup.reset();

        Events.run(EventType.Trigger.update, () -> {
            alwaysUpdate();

            if (!Gamedata.gameStarted()) {
                Setup.update();
            } else if (!Gamedata.gameOver) {
                gameUpdate();
            }
        });

        Events.on(EventType.PlayerConnect.class, event -> {
            //
        });

        Events.on(EventType.PlayerJoin.class, event -> {
            PersistentPlayer.fromPlayer(event.player).online = true;
            joinMessage(event.player);
        });

        Events.on(EventType.PlayerLeave.class, event -> {
            PersistentPlayer.fromPlayer(event.player).online = false;
            PersistentPlayer.fromPlayer(event.player).lastSeen = System.currentTimeMillis();
        });
    }

    /**
     * Ends the current game and loads the next.
     * @param winner The game's winner. 0 if the Citadel wins, otherwise is the ID of the winning team. -1 if the game is ended without a winner.
     */
    public void endGame(int winner) {
        Gamedata.gameOver = true;

        if (winner == 0) {
            announce("[accent]The [green]Citadel[] has won the game!");
        } else if (winner == -1) {
            announce("[accent]Game ended without a winner.");
        } else {
            for (RaiderTeam team : Gamedata.raiderTeams) {
                if (team.id == winner) {
                    announce("[accent]Team " + team.stringID + " has won the game!");
                    break;
                }
            }
        }

        announce("Map change and game reset not implemented.");
        // TODO: Change map
    }

    private static void joinMessage(Player player) {
        if (RaiderTeam.getTeam(PersistentPlayer.fromPlayer(player)) != null) {
            return;
        }
        if (Gamedata.gameStarted()) {
            SiegePlugin.announce("[sky]Welcome to Siege! Siege is developed and hosted by the Apricot Conservation Project. You have joined after the beginning of the game, meaning you are on the Citadel team. To learn more about the Siege gamemode, run /siege. Have fun, and good luck!");
        } else if (!Gamedata.teamSetupPhase()) {
            SiegePlugin.announce("[sky]Welcome to Siege! Siege is developed and hosted by the Apricot Conservation Project. You have joined after teams were determined, meaning you are on the Citadel team. The game will begin in " + (-Gamedata.elapsedTimeSeconds()) + " seconds. To learn more about the Siege gamemode, run /siege. Have fun, and good luck!");
        } else {
            SiegePlugin.announce("[sky]Welcome to Siege! Siege is developed and hosted by the Apricot Conservation Project. Team setup is currently ongoing, if you would like to create or join a team, run /team. Team setup will end in " + (-Gamedata.elapsedTimeSeconds() - Constants.CORE_PLACEMENT_TIME_SECONDS) + " seconds. To learn more about the Siege gamemode, run /siege. Have fun, and good luck!");
        }
    }

    // Manages constant processes that happen always
    private void alwaysUpdate() {
        if (!Gamedata.gameOver) {
            checkTeams();
        }
    }

    // Manages constant processes during the course of a game (does not run during setup or during game over)
    private void gameUpdate() {
        if (!Setup.gameBegun) {
            Setup.beginGame();
        }
    }

    private void checkTeams() {
        Gamedata.raiderTeams.removeIf(team -> team.players.isEmpty());

        // Setup handles in case of team setup phase
        if (!Gamedata.teamSetupPhase()) {
            for (Player player : Groups.player) {
                PersistentPlayer persistentPlayer = PersistentPlayer.fromPlayer(player);
                RaiderTeam team = RaiderTeam.getTeam(persistentPlayer);
                if (team != null) {
                    if (team.mindustryTeam == null) {
                        continue;
                    }
                    if (player.team().id != team.mindustryTeam.id) {
                        player.team(team.mindustryTeam);
                    }
                } else if (Gamedata.gameStarted()) {
                    player.team(Team.green);
                } else {
                    player.team(Team.blue);
                }
            }
        }

        if (Gamedata.gameStarted()) {
            // Gamedata.raiderTeams may be modified inside this loop.
            RaiderTeam[] teams = Gamedata.raiderTeams.toArray(new RaiderTeam[0]);
            for (RaiderTeam team : teams) {
                if (team.mindustryTeam.cores().size == 0) {
                    team.destroy();
                }
            }

            if (Gamedata.raiderTeams.isEmpty()) {
                endGame(0);
            }
        }
    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        RaiderTeam.Commands.registerCommands(handler);
        handler.<Player>register("siege", "Explain the Siege gamemode", (args, player) -> siegeHelp(player));
    }

    @Override
    public void registerServerCommands(CommandHandler handler) {
        /*base.register_server(handler);
        handler.removeCommand("host");
        handler.removeCommand("gameover");
        handler.removeCommand("runwave");
        handler.removeCommand("shuffle");
        handler.removeCommand("nextmap");
        handler.register("host", "[map(index)]", "Host the Siege game mode", args -> {
            if (!Vars.state.is(GameState.State.menu)) {
                Log.err("Stop the server first.");
                return;
            }
            mapReset(args);
        });

        handler.register("gameover", "[map(index)]", "End the Siege game", args -> {
            Call.sendMessage("[scarlet]Server[accent] has ended the plague game. Ending in 10 seconds...");
            Log.info("Game ended.");
            // TODO end the game
        });*/
    }

    private static void siegeHelp(Player executor) {
        executor.sendMessage("Not implemented");
        // TODO write a brief text to explain siege
    }



    public static void announce(String message) {
        System.out.println("Announced: " + message);
        for (Player player : Groups.player) {
            player.sendMessage(message);
        }
    }
}
