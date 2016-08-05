package com.zireck.remotecraft;

public class NetworkProtocolHelper {
	// Separators
	public static final String SEPARATOR_COMMAND = ":";
	public static final String SEPARATOR_ARGS = "_";
	
	// Game Difficulty
    public static final int DIFFICULTY_PEACEFUL_INT = 0;
    public static final int DIFFICULTY_EASY_INT = 1;
    public static final int DIFFICULTY_NORMAL_INT = 2;
    public static final int DIFFICULTY_HARD_INT = 3;
	
	// NetworkDiscovery
	public static final String DISCOVERY_REQUEST = "REMOTECRAFT_DISCOVERY_REQUEST";
	public static final String DISCOVERY_RESPONSE = "REMOTECRAFT_DISCOVERY_RESPONSE";
	
	// 1 arg
	public static final String COMMAND_QUIT = "REMOTECRAFT_COMMAND_QUIT";
	public static final String COMMAND_GETWORLDINFO = "REMOTECRAFT_COMMAND_GETWORLDINFO";
	public static final String COMMAND_SETWEATHER = "REMOTECRAFT_COMMAND_SETWEATHER";
	public static final String COMMAND_GETSCREENSHOT = "REMOTECRAFT_COMMAND_GETSCREENSHOT";
	public static final String COMMAND_GETINVENTORY = "REMOTECRAFT_COMMAND_GETINVENTORY";
	public static final String COMMAND_GETENDERCHEST = "REMOTECRAFT_COMMAND_GETENDERCHEST";
	public static final String COMMAND_RECORD_STOP = "REMOTECRAFT_COMMAND_RECORD_STOP";
	
	// 2 args
	public static final String COMMAND_SETGAMEMODE = "REMOTECRAFT_COMMAND_SETGAMEMODE";
	public static final String COMMAND_SETTIME = "REMOTECRAFT_COMMAND_SETTIME";
	public static final String COMMAND_SETHEALTH = "REMOTECRAFT_COMMAND_SETHEALTH";
	public static final String COMMAND_SETHUNGER = "REMOTECRAFT_COMMAND_SETHUNGER";
	public static final String COMMAND_SETEXPLVL = "REMOTECRAFT_COMMAND_SETEXPLVL";
	public static final String COMMAND_TELEPORT = "REMOTECRAFT_COMMAND_TELEPORT";
	public static final String COMMAND_REDSTONE_BUTTON = "REMOTECRAFT_COMMAND_REDSTONE_BUTTON";
	public static final String COMMAND_REDSTONE_LEVER = "REMOTECRAFT_COMMAND_REDSTONE_LEVER";
	public static final String COMMAND_RECORD_PLAY = "REMOTECRAFT_COMMAND_RECORD_PLAY";
	public static final String COMMAND_SETDIFFICULTY = "REMOTECRAFT_COMMAND_SETDIFFICULTY";
	public static final String COMMAND_SETCLOCKTIME = "REMOTECRAFT_COMMAND_SETCLOCKTIME";
	
	// Player Info
	public static final String INFO_PLAYERNAME = "REMOTECRAFT_INFO_PLAYERNAME";
	public static final String INFO_GAMEMODE = "REMOTECRAFT_INFO_GAMEMODE";
	public static final String INFO_HEALTH = "REMOTECRAFT_INFO_HEALTH";
	public static final String INFO_HUNGER = "REMOTECRAFT_INFO_HUNGER";
	public static final String INFO_ARMOR = "REMOTECRAFT_INFO_ARMOR";
	public static final String INFO_EXPLVL = "REMOTECRAFT_INFO_EXPLVL";
	public static final String INFO_COORDX = "REMOTECRAFT_INFO_COORDX";
	public static final String INFO_COORDY = "REMOTECRAFT_INFO_COORDY";
	public static final String INFO_COORDZ = "REMOTECRAFT_INFO_COORDZ";
	public static final String INFO_BIOME = "REMOTECRAFT_INFO_BIOME";
	public static final String INFO_CURRENTITEM_NULL = "REMOTECRAFT_INFO_CURRENTITEM_NULL";
	public static final String INFO_CURRENTITEM = "REMOTECRAFT_INFO_CURRENTITEM";
	
	// World Info
	public static final String INFO_WORLDNAME = "REMOTECRAFT_INFO_WORLDNAME";
	public static final String INFO_SEED = "REMOTECRAFT_INFO_SEED";
	public static final String INFO_DIFFICULTY = "REMOTECRAFT_INFO_DIFFICULTY";
	public static final String INFO_DAYTIME = "REMOTECRAFT_INFO_DAYTIME";
	public static final String INFO_TIME = "REMOTECRAFT_INFO_TIME";
	public static final String INFO_RAINING = "REMOTECRAFT_INFO_RAINING";
	public static final String INFO_THUNDERING = "REMOTECRAFT_INFO_THUNDERING";
	
	// Screenshots
	public static final String COMMAND_SCREENSHOT_SEND = "REMOTECRAFT_COMMAND_SCREENSHOT_SEND";
	public static final String COMMAND_SCREENSHOT_ERROR = "REMOTECRAFT_COMMAND_SCREENSHOT_ERROR";
	public static final String COMMAND_SCREENSHOT_FINISHED = "REMOTECRAFT_COMMAND_SCREENSHOT_FINISHED";
	
	
	public static final String INFO_INVENTORY_ITEM = "REMOTECRAFT_INFO_INVENTORY_ITEM";
	public static final String INFO_ENDERCHEST_ITEM = "REMOTECRAFT_INFO_ENDERCHEST_ITEM";
	public static final String INFO_SPAWNPOINT = "REMOTECRAFT_INFO_SPAWNPOINT";
	
	// MISC
	public static final String DIMENSION_OVERWORLD = "OVERWORLD";
	public static final String DIMENSION_NETHER = "NETHER";
	public static final String DIMENSION_END = "END";
	
	// Deprecated
	public static String formatCommand(String arg1, String arg2) {
		return arg1 + NetworkProtocolHelper.SEPARATOR_COMMAND + arg2;
	}
}