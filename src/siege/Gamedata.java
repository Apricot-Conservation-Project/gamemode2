package siege;

import arc.math.Mathf;
import arc.math.geom.Point2;
import mindustry.content.Blocks;
import mindustry.game.Team;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.storage.CoreBlock;

import java.util.ArrayList;
import java.util.List;

import static mindustry.Vars.world;

public final class Gamedata {
    public static List<RaiderTeam> deadRaiderTeams = new ArrayList<>();
    public static List<RaiderTeam> raiderTeams = new ArrayList<>();
    public static long startTime = 0;
    public static boolean gameStarted = false; // Owned by Setup
    public static boolean gameOver = false;
    //public int mapIndex;

    // All true for setup, then cache set up when game begins
    // Should not be read or used at all during setup.
    private static boolean[][] deadZoneCache;

    public static void reset() {
        startTime = System.currentTimeMillis() + 1000 * Constants.SETUP_TIME_SECONDS;
        raiderTeams = new ArrayList<>();
        deadRaiderTeams = new ArrayList<>();
        gameOver = false;
    }

    /**
     * @return Whether the game should have started yet. Prefer its variable form for most cases, as this is based only on time and could desync.
     */
    public static boolean gameStartTime() {
        return elapsedTimeSeconds() >= 0;
    }

    /**
     * @return Whether team configuration is still allowed
     */
    public static boolean teamSetupPhase() {
        return elapsedTimeSeconds() < -Constants.CORE_PLACEMENT_TIME_SECONDS;
    }

    /**
     * @return The number of seconds since the game's start time (end of setup). Will be negative if setup is still ongoing.
     */
    public static long elapsedTimeSeconds() {
        return (System.currentTimeMillis() - startTime) / 1000;
    }

    /**
     * @return All the player cores in the game
     */
    public static CoreBlock.CoreBuild[] getAllCores() {
        ArrayList<CoreBlock.CoreBuild> cores = Team.green.cores().list();

        for (RaiderTeam team : raiderTeams) {
            cores.addAll(List.of(team.getCores()));
        }

        return cores.toArray(new CoreBlock.CoreBuild[0]);
    }

    /**
     * @param coreType The type of core to search for
     * @return All player cores of the given type
     */
    public static CoreBlock.CoreBuild[] getAllCores(Block coreType) {
        CoreBlock.CoreBuild[] allCores = getAllCores();
        ArrayList<CoreBlock.CoreBuild> cores = new ArrayList<>();

        for (CoreBlock.CoreBuild core : allCores) {
            if (core.block == coreType) {
                cores.add(core);
            }
        }

        return cores.toArray(new CoreBlock.CoreBuild[0]);
    }

    /**
     * Get the radius that the given core reveals around the dead zone
     * @param core
     * @return
     */
    public static float getCoreSafetyRadius(Block core) {
        if (core == Blocks.coreShard) {
            return Constants.SHARD_DEAD_ZONE_RADIUS;
        } else if (core == Blocks.coreFoundation) {
            return Constants.FOUNDATION_DEAD_ZONE_RADIUS;
        } else if (core == Blocks.coreNucleus) {
            return Constants.NUCLEUS_DEAD_ZONE_RADIUS;
        }

        throw new IllegalArgumentException("Unknown core type: " + core.toString());
    }

    public static float getCoreSafetyRadius2(Block core) {
        return Mathf.sqr(getCoreSafetyRadius(core));
    }

    /**
     * Finds if a given point is in the dead zone.
     * @param tile The world grid tile to check
     * @return Whether the tile is in the dead zone
     */
    public static boolean getDeadZone(Tile tile) {
        return deadZoneCache[tile.x][tile.y];
    }

    /**
     * Finds if a given point is in the dead zone.
     * @param point The world grid point to check
     * @return Whether the tile is in the dead zone
     */
    public static boolean getDeadZone(Point2 point) {
        return deadZoneCache[point.x][point.y];
    }

    /**
     * Finds if a given point is in the dead zone.
     * Expensive operation as compared to getDeadZone.
     * Writes result to cache.
     * @param tile The world grid tile to check
     * @return Whether the tile is in the dead zone
     */
    public static boolean hardGetDeadZone(Point2 tile) {
        if (deadZoneCache == null) {
            deadZoneCache = new boolean[world.width()][world.height()];
            for (int x = 0; x < world.width(); x ++) {
                for (int y = 0; y < world.height(); y++) {
                    deadZoneCache[x][y] = true;
                }
            }
        }

        CoreBlock.CoreBuild[] cores = getAllCores();

        boolean result = true;
        for (CoreBlock.CoreBuild core : cores) {
            if (tile.dst2(core.tileX(), core.tileY()) < getCoreSafetyRadius2(core.block)) {
                result = false;
                break;
            }
        }

        deadZoneCache[tile.x][tile.y] = result;
        return result;
    }

    public static void reloadCore(CoreBlock.CoreBuild core) {
        // Reload deadzone cache around core
        float radius = Gamedata.getCoreSafetyRadius(core.block);

        int minX = Mathf.floor(core.tileX() - radius - 1);
        int minY = Mathf.floor(core.tileY() - radius - 1);
        int maxX = Mathf.ceil(core.tileX() + radius + 1);
        int maxY = Mathf.ceil(core.tileY() + radius + 1);
        for (int x = minX; x < maxX; x ++) {
            for (int y = minY; y < maxY; y++) {
                Gamedata.hardGetDeadZone(new Point2(x, y));
            }
        }
    }

    public static void dataDump() {
        System.out.println("\nGamedata and Setup dump\n");
        System.out.println("deadRaiderTeams: " + deadRaiderTeams.toString());
        System.out.println("raiderTeams: " + raiderTeams.toString());
        System.out.println("startTime: " + startTime);
        System.out.println("gameStarted: " + gameStarted);
        System.out.println("gameOver: " + gameOver);
        System.out.println("elapsedTimeSeconds(): " + elapsedTimeSeconds());
        System.out.println("--- Setup ---");
        Setup.dataDump();
        System.out.println("\n");
    }
}
