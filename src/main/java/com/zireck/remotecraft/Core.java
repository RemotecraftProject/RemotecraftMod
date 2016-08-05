package com.zireck.remotecraft;

import com.zireck.remotecraft.NetworkDiscovery.INetworkDiscovery;
import com.zireck.remotecraft.NetworkManager.INetworkManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockButton;
import net.minecraft.block.BlockLever;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.InventoryEnderChest;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.GameType;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.WorldTickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;
import java.util.Random;

@SideOnly(Side.CLIENT)
public class Core implements INetworkDiscovery, INetworkManager {
	
	private Minecraft mc;
	
	private Player mPlayer;
	private World mWorld;
	
	NetworkDiscovery mDiscovery;
	NetworkManager mManager;
	
	ISound mSongTrack;
	
	boolean mShouldTakeScreenShot;
	boolean mShouldIGoToNether;
	
	// Force-send Flags
	private boolean mForceUpdateDifficulty = false;
	private boolean mForceUpdateBiome = false;
	private boolean mForceUpdateDaytime = false;
	private boolean mForceUpdateWeather = false;
	private boolean mForceUpdateTime = false;
	
	public Core(Minecraft mc) {
		this.mc = mc;
		
		mPlayer = new Player();
		mWorld = new World();
		
		mWorld.setLoaded(false);
		
		mDiscovery = null;
		mManager = null;
		
		resetInfo();
	}

	
	@SubscribeEvent
	public void onTick(ClientTickEvent event) {
		if (mc.theWorld == null) {
			onTickInGUI();
		}
	}
	
	@SubscribeEvent
	public void onTick(WorldTickEvent event) {
		if (event.phase == Phase.START) {
			onTickStart();
		} else if (event.phase == Phase.END) {
			onTickEnd();
		}
	}
	
	@SubscribeEvent
	public void onRender(TickEvent.RenderTickEvent event) {
		// We need to take screenshots from another thread with an initialized OpenGL Context. In this case it's taken from RenderTickEvent instead of the regular TickEvent
		tryGettingScreenshot();
	}
	
	/**
	 * Called at tick start
	 */
	private void onTickStart() {
		// do nothing
	}
	
	/**
	 * Called at tick end
	 */
	private void onTickEnd() {
		onTickInGame();
	}
	
	/**
	 * Called every Game tick
	 */
	private void onTickInGame() {
		if (!mWorld.isLoaded()) {
			mWorld.setLoaded(true);
			
			// Network Discovery
			mDiscovery = NetworkDiscovery.getInstance();
			mDiscovery.setInterface(this);

			// Network Manager
			mManager = new NetworkManager(this);
		}
		
		updateInfo();
	}
	
	/**
	 * Called every GUI tick
	 */
	private void onTickInGUI() {
		// Shutdown the Network Discovery thread
		if (mDiscovery != null) {
			mDiscovery.shutdown();
			mDiscovery = null;
		}
		
		// Shutdown the Network Manager
		if (mManager != null) {
			mManager.shutdown();
			mManager = null;
		}

		// Unload the world
		if (mWorld != null) {
			if (mWorld.isLoaded()) {
				mWorld.setLoaded(false);
			}
		}
	}
	
	/**
	 * Called every game tick. Update info and send when necessary
	 */
	public void updateInfo() {
		if (mc == null || mc.thePlayer == null || mc.theWorld == null || mc.getIntegratedServer() == null) {
			return;
		}
		
		updatePlayername();
		updateGamemode();
		updateHealth();
		updateHunger();
		updateExpLevel();
		updateArmor();
		updateCoords();
		updateBiome();
		updateCurrentItem();
		
		updateWorldname();
		updateSeed();
		updateDifficulty();
		updateDaytime();
		updateTime();
		updateWeather();
	}
	
	/**
	 * Called every GUI tick, reset data
	 */
	public void resetInfo() {
		mPlayer = new Player();
		mWorld = new World();
	}

	/**
	 * Send data through TCP socket (INetworkManager)
	 */
	public void sendEverything() {
		if (mWorld.isLoaded()) {
			mManager.sendPlayername(mPlayer.getName());
			mManager.sendGamemode(mPlayer.getGamemode());
			mManager.sendHealth(mPlayer.getHealth());
			mManager.sendHunger(mPlayer.getHunger());
			mManager.sendArmor(mPlayer.getArmor());
			mManager.sendExpLevel(mPlayer.getExp());
			mManager.sendCoordX(mPlayer.getX());
			mManager.sendCoordY(mPlayer.getY());
			mManager.sendCoordZ(mPlayer.getZ());
			mManager.sendBiome(mPlayer.getBiome());
			mManager.sendCurrentItem(mPlayer.getItem());
			
			mManager.sendWorldName(mWorld.getName());
			mManager.sendSeed(mWorld.getSeed());
			mManager.sendDifficulty(mWorld.getDifficulty());
			mManager.sendDaytime(mWorld.isDaytime());
			mManager.sendTime(mWorld.getHour(), mWorld.getMinute());
			mManager.sendRaining(mWorld.isRaining());
			mManager.sendThundering(mWorld.isThundering());
			
			// Send spawn point
			BlockPos bedPosition = mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getPlayerEntityByName(mc.thePlayer.getCommandSenderEntity().getName()).getBedLocation();
			if (bedPosition != null) {
				mManager.sendSpawnPoint(bedPosition.getX(), bedPosition.getY(), bedPosition.getZ());
			}
		}
	}
	
	/**
	 * Update player name
	 */
	private void updatePlayername() {
		if (mPlayer.getName() != mc.thePlayer.getCommandSenderEntity().getName()) {
			mPlayer.setName(mc.thePlayer.getCommandSenderEntity().getName());
			mManager.sendPlayername(mc.thePlayer.getDisplayNameString());
		}
	}
	
	/**
	 * Update player gamemode
	 */
	private void updateGamemode() {
		String gamemode = "";
		
		if (mc.playerController.isInCreativeMode()) {
			gamemode = "CREATIVE";
		} else if (mc.playerController.isNotCreative()) {
			if (mc.playerController.isSpectator() || mc.playerController.isSpectatorMode()) {
				gamemode = "SPECTATOR";
			} else {
				if (mc.playerController.gameIsSurvivalOrAdventure()) {
					gamemode = "SURVIVAL";
				}
			}
		}
		
		if (mPlayer.getGamemode() != gamemode) {
			mPlayer.setGamemode(gamemode);
			mManager.sendGamemode(gamemode);
		}
	}
	
	/**
	 * Update player health value
	 */
	private void updateHealth() {
		int health = (int) mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getPlayerEntityByName(mc.thePlayer.getCommandSenderEntity().getName()).getHealth();
		if (mPlayer.getHealth() != health) {
			mPlayer.setHealth(health);
			mManager.sendHealth(health);
		}
	}
	
	/**
	 * Update player hunger value
	 */
	private void updateHunger() {
		int hunger = mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getPlayerEntityByName(mc.thePlayer.getCommandSenderEntity().getName()).getFoodStats().getFoodLevel();
		if (mPlayer.getHunger() != hunger) {
			mPlayer.setHunger(hunger);
			mManager.sendHunger(hunger);
		}
	}
	
	/**
	 * Update player armor value
	 */
	private void updateArmor() {
		int armor = mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getPlayerEntityByName(mc.thePlayer.getCommandSenderEntity().getName()).getTotalArmorValue();
		if (mPlayer.getArmor() != armor) {
			mPlayer.setArmor(armor);
			mManager.sendArmor(armor);
		}
	}
	
	/**
	 * Update player experience level
	 */
	private void updateExpLevel() {
		int exp = mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getPlayerEntityByName(mc.thePlayer.getCommandSenderEntity().getName()).experienceLevel;
		if (mPlayer.getExp() != exp) {
			mPlayer.setExp(exp);
			mManager.sendExpLevel(exp);
		}
	}
	
	/**
	 * Update player coords
	 */
	private void updateCoords() {
		int x = MathHelper.floor_double(mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getPlayerEntityByName(mc.thePlayer.getCommandSenderEntity().getName()).posX);
		int y = MathHelper.floor_double(mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getPlayerEntityByName(mc.thePlayer.getCommandSenderEntity().getName()).posY);
		int z = MathHelper.floor_double(mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getPlayerEntityByName(mc.thePlayer.getCommandSenderEntity().getName()).posZ);

		if (mPlayer.getX() != x) {
			mPlayer.setX(x);
			mManager.sendCoordX(x);
		}
		
		if (mPlayer.getY() != y) {
			mPlayer.setY(y);
			mManager.sendCoordY(y);
		}
		
		if (mPlayer.getZ() != z) {
			mPlayer.setZ(z);
			mManager.sendCoordZ(z);
		}
		
	}
	
	/**
	 * Update player biome
	 */
	private void updateBiome() {
		String biome;
		int x = MathHelper.floor_double(mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getPlayerEntityByName(mc.thePlayer.getCommandSenderEntity().getName()).posX);
		int z = MathHelper.floor_double(mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getPlayerEntityByName(mc.thePlayer.getCommandSenderEntity().getName()).posZ);

		BlockPos currentPos = new BlockPos(x, 0, z);
		Chunk chunk = mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getChunkFromBlockCoords(currentPos);
		BlockPos newPos = new BlockPos(x & 15, 0, z & 15);

		biome = chunk.getBiome(newPos, mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getBiomeProvider()).getBiomeName();
		
		if (!mPlayer.getBiome().equals(biome)) {
			mPlayer.setBiome(biome);
			mManager.sendBiome(biome);
		}
		
		if (mForceUpdateBiome) {
			mForceUpdateBiome = false;
			mPlayer.setBiome(biome);
			mManager.sendBiome(mPlayer.getBiome());
		}
	}
	
	/**
	 * Update the player current item
	 */
	private void updateCurrentItem() {
		ItemStack currentItem = mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getPlayerEntityByName(mc.thePlayer.getCommandSenderEntity().getName()).inventory.getCurrentItem();
		if (currentItem == null && !mPlayer.getItem().equals("")) {
			mPlayer.setItem("");
			mManager.sendNoCurrentItem();
		} else if (currentItem != null) {
			if (!currentItem.getDisplayName().equals(mPlayer.getItem())) {
				mPlayer.setItem(currentItem.getDisplayName());
				mManager.sendCurrentItem(currentItem.getDisplayName());
			}
		}
	}
	
	/**
	 * Update world name
	 */
	private void updateWorldname() {
		String worldname;
		if (mc.isIntegratedServerRunning()) {
			worldname = mc.getIntegratedServer().getWorldName();
		} else {
			if (!mc.theWorld.getWorldInfo().getWorldName().isEmpty()) {
				worldname = mc.theWorld.getWorldInfo().getWorldName();
			} else {
				worldname = "";
			}
		}
		
		if (!mWorld.getName().equals(worldname)) {
			mWorld.setName(worldname);
			mManager.sendWorldName(worldname);
		}
	}
	
	/**
	 * Update world seed
	 */
	private void updateSeed() {
		if (mc.thePlayer != null && mc.getIntegratedServer() != null && mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension) != null) {
			long seed = mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getSeed();
			if (mWorld.getSeed() != seed) {
				mWorld.setSeed(seed);
				mManager.sendSeed(seed);
			}
		}
	}
	
	/**
	 * Update world difficulty
	 */
	private void updateDifficulty() {
		int difficulty = mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getDifficulty().getDifficultyId();
		if (mWorld.getDifficulty() != difficulty) {
			mWorld.setDifficulty(difficulty);
			mManager.sendDifficulty(difficulty);
		}
		
		if (mForceUpdateDifficulty) {
			mForceUpdateDifficulty = false;
			mWorld.setDifficulty(difficulty);
			mManager.sendDifficulty(difficulty);
		}
	}

	/**
	 * Update world daytime
	 */
	private void updateDaytime() {
		try {
			long mTime = mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getWorldInfo().getWorldTime() - 24000 * (int) (mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getWorldInfo().getWorldTime() / 24000);
			if (mTime >= 0 && mTime < 12000) {
				if (!mWorld.isDaytime()) {
					mWorld.setDaytime(true);
					mManager.sendDaytime(true);
				}
			} else {
				if (mWorld.isDaytime()) {
					mWorld.setDaytime(false);
					mManager.sendDaytime(false);
				}
			}
			
			if (mForceUpdateDaytime) {
				mForceUpdateDaytime = false;
				mManager.sendDaytime(mWorld.isDaytime());
			}
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Update world weather
	 */
	private void updateWeather() {
		try {
			if (!mWorld.isRaining() && mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getWorldInfo().isRaining()) {
				mWorld.setRaining(true);
				mManager.sendRaining(mWorld.isRaining());
			} else if (mWorld.isRaining() && !mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getWorldInfo().isRaining()) {
				mWorld.setRaining(false);
				mManager.sendRaining(mWorld.isRaining());
			}
			
			if (!mWorld.isThundering() && mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getWorldInfo().isThundering()) {
				mWorld.setThundering(true);
				mManager.sendThundering(mWorld.isThundering());
			} else if (mWorld.isThundering() && !mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getWorldInfo().isThundering()) {
				mWorld.setThundering(false);
				mManager.sendThundering(mWorld.isThundering());
			}
			
			if (mForceUpdateWeather) {
				mForceUpdateWeather = false;
				mManager.sendRaining(mWorld.isRaining());
				mManager.sendThundering(mWorld.isThundering());
			}
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Update world time
	 */
	private void updateTime() {
		try {
			long mTime;
			if (mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getWorldInfo().getWorldTime() > 24000) {
				mTime = mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getWorldInfo().getWorldTime() - 24000 * (int) (mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getWorldInfo().getWorldTime() / 24000);
			} else {
				mTime = mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getWorldInfo().getWorldTime();
			}
			
			int hour, minute;
			
			// Calculate hour
			if ( (((int) mTime / 1000) + 6) > 23 ) {
				hour = (((int) mTime / 1000) + 6) - 24;
			} else {
				hour = ((int) mTime / 1000) + 6;
			}
			
			// Calculate minutes
			if ( ((mTime * 60) / 1000) > 60 ) {
				minute = (int) ((mTime * 60) / 1000) - (60 * ((int) mTime / 1000));
			} else {
				minute = (int) ((mTime * 60) / 1000);
			}
			
			if (mWorld.getHour() != hour || mWorld.getMinute() != minute) {
				mWorld.setHour(hour);
				mWorld.setMinute(minute);
				mManager.sendTime(mWorld.getHour(), mWorld.getMinute());
			}
			
			if (mForceUpdateTime) {
				mForceUpdateTime = false;
				mWorld.setHour(hour);
				mWorld.setMinute(minute);
				mManager.sendTime(mWorld.getHour(), mWorld.getMinute());
			}
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
	}
	
	// NetworkManager Interface
	
	// Set player health
	public void setHealth(int health) {
		mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getPlayerEntityByName(mc.thePlayer.getCommandSenderEntity().getName()).setHealth(health);
	}
	
	/**
	 * Set player hunger
	 * @param hunger Hunger amount
	 */
	public void setHunger(int hunger) {
		mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getPlayerEntityByName(mc.thePlayer.getCommandSenderEntity().getName()).getFoodStats().setFoodLevel(hunger);
	}
	
	/**
	 * Set experience level
	 * @param exp Experience level
	 */
	public void setExpLvl(int exp) {
		int currentLevel = mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getPlayerEntityByName(mc.thePlayer.getCommandSenderEntity().getName()).experienceLevel;
		mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getPlayerEntityByName(mc.thePlayer.getCommandSenderEntity().getName()).addExperienceLevel(-currentLevel);
		mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getPlayerEntityByName(mc.thePlayer.getCommandSenderEntity().getName()).addExperienceLevel(exp);
	}
	
	/**
	 * Toggle GameMode
	 */
	@Deprecated
	public void toggleGameMode() {
		if (mc.thePlayer.capabilities.isCreativeMode) {
			mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getPlayerEntityByName(mc.thePlayer.getCommandSenderEntity().getName()).setGameType(GameType.SURVIVAL);
		} else {
			mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getPlayerEntityByName(mc.thePlayer.getCommandSenderEntity().getName()).setGameType(GameType.CREATIVE);
		}
	}
	
	/**
	 * Sets GameMode
	 * @param gamemode GameMode
	 */
	@Override
	public void setGameMode(String gamemode) {
		if (gamemode.equalsIgnoreCase("Survival")) {
			mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getPlayerEntityByName(mc.thePlayer.getCommandSenderEntity().getName()).setGameType(GameType.SURVIVAL);
		} else if (gamemode.equalsIgnoreCase("Creative")) {
			mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getPlayerEntityByName(mc.thePlayer.getCommandSenderEntity().getName()).setGameType(GameType.CREATIVE);
		}
	}	
	
	/**
	 * Set daytime or nighttime
	 * @param dayOrNight "Day" or "Time"
	 */
	public void setWorldTime(String dayOrNight) {
		if (dayOrNight.equalsIgnoreCase("Day")) {
			mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).setWorldTime(0);
		} else if (dayOrNight.equalsIgnoreCase("Night")) {
			mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).setWorldTime(12500);
		}
	}

	/**
	 * Toggle weather
	 */
	public void setWorldWeather() {
		if (!mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).isRaining()) {
			mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getWorldInfo().setRaining(true);
		} else {
			mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getWorldInfo().setRaining(false);
		}
	}
	
	/**
	 * Turn on the screenshot flag
	 */
	public void enableScreenShot() {
		mShouldTakeScreenShot = true;
	}
	
	/**
	 * Take screenshot
	 * Apparently, it's not possible to call ScreenShotHelper.saveScreenshot() from the NetworkManager thread, so I'm using a flag (mShouldTakeScreenShot)
	 */
	public void tryGettingScreenshot() {
		if (mShouldTakeScreenShot) {
			mShouldTakeScreenShot = false;
			try {
				ITextComponent chatComponent = ScreenShotHelper.saveScreenshot(mc.mcDataDir.getCanonicalFile(), mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
				String unformattedText = chatComponent.getUnformattedText();
				int size = unformattedText.split(" ").length;
				String fileName = unformattedText.split(" ")[size - 1];
				mManager.sendScreenShot(fileName, mWorld.getSeed());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Teleport player to a certain location
	 * @param dim Dimension
	 * @param x X coord
	 * @param y Y coord
	 * @param z Z coord
	 */
	public void teleportTo(int dim, int x, int y, int z) {
		if (mc.thePlayer.dimension == dim) {
			mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getPlayerEntityByName(mc.thePlayer.getCommandSenderEntity().getName()).setPositionAndUpdate((double) x + 0.5D, y, (double) z + 0.5D);
		} else {
			//EntityPlayerMP player = (EntityPlayerMP) mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getPlayerEntityByName(mc.thePlayer.getCommandSenderName());
			EntityPlayerMP player = (EntityPlayerMP) mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getPlayerEntityByName(mc.thePlayer.getCommandSenderEntity().getName());
			// TODO Fix this.
			//player.mcServer.getConfigurationManager().transferPlayerToDimension(player, dim, new MyTeleporter( mc.getIntegratedServer().worldServerForDimension(dim), x, y, z ));
		}
	}
	/**
	 * Activate a redstone button
	 * @param dim Button dimension
	 * @param x Button X coord
	 * @param y Button Y coord
	 * @param z Button Z coord
	 */
	public void toggleButton(int dim, int x, int y, int z) {
		WorldServer world = mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension);
		
		if (dim == mc.thePlayer.dimension) {
			if (world.getBlockState(new BlockPos(x, y, z)) != null) {
				Block block = world.getBlockState(new BlockPos(x, y, z)).getBlock();
				if (block == Blocks.STONE_BUTTON || block == Blocks.WOODEN_BUTTON) {
					// Time period the button will remain active (20 = stone, 30 = wooden)
					int tickRate = block == Blocks.STONE_BUTTON ? 20 : 30;
					
					BlockPos blockPos = new BlockPos(x, y, z);
					
					int i1 = world.getBlockState(new BlockPos(x, y, z)).getBlock().getMetaFromState(world.getBlockState(new BlockPos(x, y, z)));
		            int j1 = i1 & 7;
		            int k1 = 8 - (i1 & 8);
		            if (k1 != 0) {
		            	IBlockState blockState = world.getBlockState(blockPos);
		            	
		            	world.setBlockState(blockPos, blockState.withProperty(BlockButton.POWERED, Boolean.valueOf(true)));
		            	world.markBlockRangeForRenderUpdate(x, y, z, x, y, z);

						// TODO Is this working correctly?
						ResourceLocation resourceLocation = new ResourceLocation("remotecraft", "random.click");
						SoundEvent soundEvent = new SoundEvent(resourceLocation);
						world.playSound( x + 0.5D, y + 0.5D, z + 0.5D, soundEvent, SoundCategory.BLOCKS, 0.3F, 0.6F, false);
		            	world.scheduleUpdate(blockPos, block, tickRate);
		            	
		            	// Notify block update
		            	world.notifyNeighborsOfStateChange(blockPos, block);
	
		            	// Notify the block being directly powered
		                if (j1 == 1) {
		                	world.notifyBlockOfStateChange(new BlockPos(x - 1, y, z), block);
		                } else if (j1 == 2) {
		                	world.notifyNeighborsOfStateChange(new BlockPos(x + 1, y, z), block);
		                } else if (j1 == 3) {
		                	world.notifyNeighborsOfStateChange(new BlockPos(x, y, z - 1), block);
		                } else if (j1 == 4) {
		                	world.notifyNeighborsOfStateChange(new BlockPos(x, y, z + 1), block);
		                } else {
		                	world.notifyNeighborsOfStateChange(new BlockPos(x, y - 1, z), block);
		                }
		            }
					
				} else {
					//mc.ingameGUI.getChatGUI().printChatMessage("[Remotecraft] Error. Block is not a Button.");
					System.out.println("[Remotecraft] Error. Block is not a Button.");
				}
			} else {
				//mc.ingameGUI.getChatGUI().printChatMessage("[Remotecraft] Error. No block found.");
				System.out.println("[Remotecraft] Error. No block found.");
			}
		} else {
			//mc.ingameGUI.getChatGUI().printChatMessage("[Remotecraft] Error. Not in the same dimension.");
			System.out.println("[Remotecraft] Error. Not in the same dimension.");
		}
	}

	/**
	 * Toggle a redstone lever
	 * @param dim Lever dimension
	 * @param x Lever X coord
	 * @param y Lever Y coord
	 * @param z Lever Z coord
	 */
	public void toggleLever(int dim, int x, int y, int z) {
		WorldServer world = mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension);
		
		if (dim == mc.thePlayer.dimension) {
			if (world.getBlockState(new BlockPos(x, y, z)).getBlock() != null) {
				Block block = world.getBlockState(new BlockPos(x, y, z)).getBlock();
				if (block == Blocks.LEVER) {
					
					int i1 = world.getBlockState(new BlockPos(x, y, z)).getBlock().getMetaFromState(world.getBlockState(new BlockPos(x, y, z)));
		            int j1 = i1 & 7;
		            int k1 = 8 - (i1 & 8);
					
					BlockPos blockPos = new BlockPos(x, y, z);
					IBlockState blockState = world.getBlockState(blockPos);
					boolean isPowered = ((Boolean) blockState.getValue(BlockLever.POWERED)).booleanValue();

					world.setBlockState(blockPos, blockState.withProperty(BlockLever.POWERED, Boolean.valueOf(!isPowered)));

					// TODO Is this working correctly?
					ResourceLocation resourceLocation = new ResourceLocation("remotecraft", "random.click");
					SoundEvent soundEvent = new SoundEvent(resourceLocation);
					world.playSound( x + 0.5D, y + 0.5D, z + 0.5D, soundEvent, SoundCategory.BLOCKS, 0.3F, ((Boolean) blockState.getValue(BlockLever.POWERED)).booleanValue() ? 0.6F : 0.5F, false);

					// Notify block update
		            world.notifyNeighborsOfStateChange(new BlockPos(x, y, z), block);
		            
		            // Notify the block being directly powered
		            if (j1 == 1) {
		            	world.notifyNeighborsOfStateChange(new BlockPos(x - 1 , y, z), block);
		            } else if (j1 == 2) {
		            	world.notifyNeighborsOfStateChange(new BlockPos(x + 1, y, z), block);
		            } else if (j1 == 3) {
		            	world.notifyNeighborsOfStateChange(new BlockPos(x, y, z - 1), block);
		            } else if (j1 == 4) {
		            	world.notifyNeighborsOfStateChange(new BlockPos(x, y, z + 1), block);
		            } else if (j1 != 5 && j1 != 6) {
		                if (j1 == 0 || j1 == 7) {
		                	world.notifyNeighborsOfStateChange(new BlockPos(x, y + 1, z), block);
		                }
		            } else {
		            	world.notifyNeighborsOfStateChange(new BlockPos(x, y - 1, z), block);
		            }
		            
				} else {
					//mc.ingameGUI.getChatGUI().printChatMessage("[Remotecraft] Error. Block is not a Lever.");
					System.out.println("[Remotecraft] Error. Block is not a Lever.");
				}
			} else {
				//mc.ingameGUI.getChatGUI().printChatMessage("[Remotecraft] Error. No block found.");
				System.out.println("[Remotecraft] Error. No block found.");
			}
		} else {
			//mc.ingameGUI.getChatGUI().printChatMessage("[Remotecraft] Error. Not in the same dimension.");
			System.out.println("[Remotecraft] Error. Not in the same dimension.");
		}
	}
	
	/**
	 * This method is called everytime the WorldFragment is created in the Android app
	 */
	public void forceSendWorldInfo() {
		mForceUpdateDifficulty = true;
		mForceUpdateBiome = true;
		mForceUpdateDaytime = true;
		mForceUpdateWeather = true;
		mForceUpdateTime = true;
	}
	
	private class MyTeleporter extends Teleporter {
        private Random random;
        int x, y, z;
        
        public MyTeleporter(WorldServer par1WorldServer, int x, int y, int z) {
	        super(par1WorldServer);
	        random = new Random();
	        this.x = x;
	        this.y = y;
	        this.z = z;
        }
        
        // TODO What should I do with this?
        /*
        @Override
        public void placeInPortal(Entity par1Entity, double par2, double par4, double par6, float par8) {
        	par1Entity.setLocationAndAngles(x, y, z, par1Entity.rotationYaw, par1Entity.rotationPitch);
        }
        
        @Override
        public boolean placeInExistingPortal(Entity par1Entity, double par2, double par4, double par6, float par8) {
	        return false;
        }*/

        @Override
        public boolean makePortal(Entity ent) {
	        return true;
        }

        @Override
        public void removeStalePortalLocations(long l) {
	        //
        }
	}
	
	// INetworkListener
	
	@Override
	public void getInventory() {
		ItemStack item;
		
		InventoryPlayer inventoryPlayer = mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getPlayerEntityByName(mc.thePlayer.getCommandSenderEntity().getName()).inventory;
		for (int i=0; i<inventoryPlayer.getSizeInventory(); i++) {
			item = inventoryPlayer.getStackInSlot(i);
			if (item != null) {
				mManager.sendInventoryItem(item.getDisplayName(), item.stackSize);
			}
		}
	}
	
	@Override
	public void getEnderchest() {
		InventoryEnderChest mEnderchest = mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getPlayerEntityByName(mc.thePlayer.getCommandSenderEntity().getName()).getInventoryEnderChest();
		
		ItemStack item;
		for (int i=0; i<mEnderchest.getSizeInventory(); i++) {
			item = mEnderchest.getStackInSlot(i);
			if (item != null) {
				mManager.sendEnderchestItem(item.getDisplayName(), item.stackSize);
			}
		}
	}
	
	@Override
	public void playRecord(String record) {
		stopRecord();
		
		int x = MathHelper.floor_double(mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getPlayerEntityByName(mc.thePlayer.getCommandSenderEntity().getName()).posX);
		int y = MathHelper.floor_double(mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getPlayerEntityByName(mc.thePlayer.getCommandSenderEntity().getName()).posY);
		int z = MathHelper.floor_double(mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getPlayerEntityByName(mc.thePlayer.getCommandSenderEntity().getName()).posZ);
		
		//mSongTrack = PositionedSoundRecord.create(new ResourceLocation("records." + record));
		mSongTrack = PositionedSoundRecord.getMusicRecord(new SoundEvent(new ResourceLocation("records." + record)));
		mc.getSoundHandler().playSound(mSongTrack);
		
		/*
		Item rec = null;
		if (record.equalsIgnoreCase("13")) {
			rec = Items.record_13;
		} else if (record.equalsIgnoreCase("cat")) {
			rec = Items.record_cat;
		} else if (record.equalsIgnoreCase("blocks")) {
			rec = Items.record_blocks;
		} else if (record.equalsIgnoreCase("chirp")) {
			rec = Items.record_chirp;
		} else if (record.equalsIgnoreCase("far")) {
			rec = Items.record_far;
		} else if (record.equalsIgnoreCase("mall")) {
			rec = Items.record_mall;
		} else if (record.equalsIgnoreCase("mellohi")) {
			rec = Items.record_mellohi;
		} else if (record.equalsIgnoreCase("stal")) {
			rec = Items.record_stal;
		} else if (record.equalsIgnoreCase("strad")) {
			rec = Items.record_strad;
		} else if (record.equalsIgnoreCase("ward")) {
			rec = Items.record_ward;
		} else if (record.equalsIgnoreCase("11")) {
			rec = Items.record_11;
		} else if (record.equalsIgnoreCase("wait")) {
			rec = Items.record_wait;
		}
		
		if (rec != null) {
			BlockPos blockPos = new BlockPos(x, y, z);
			mc.theWorld.playAuxSFXAtEntity( (EntityPlayer) null, 1005, blockPos, Item.getIdFromItem(rec));
			System.out.println("k9d3 playan...");
		}*/
	}
	
	@Override
	public void stopRecord() {
		int x = MathHelper.floor_double(mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getPlayerEntityByName(mc.thePlayer.getCommandSenderEntity().getName()).posX);
		int y = MathHelper.floor_double(mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getPlayerEntityByName(mc.thePlayer.getCommandSenderEntity().getName()).posY);
		int z = MathHelper.floor_double(mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getPlayerEntityByName(mc.thePlayer.getCommandSenderEntity().getName()).posZ);
		
		mc.getSoundHandler().stopSound(mSongTrack);
		
		/*
		BlockPos blockPos = new BlockPos(x, y, z);
		mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).playRecord(blockPos, (String) null);*/
	}

	@Override
	public String getWorldName() {
		return mWorld.getName();
	}

	@Override
	public long getWorldSeed() {
		return mWorld.getSeed();
	}
	
	@Override
	public String getPlayerName() {
		return mPlayer.getName();
	}

	@Override
	public void setDifficulty(int difficulty) {
		switch (difficulty) {
			case NetworkProtocolHelper.DIFFICULTY_PEACEFUL_INT:
				mc.theWorld.getWorldInfo().setDifficulty(EnumDifficulty.PEACEFUL);
				break;
			case NetworkProtocolHelper.DIFFICULTY_EASY_INT:
				mc.theWorld.getWorldInfo().setDifficulty(EnumDifficulty.EASY);
				break;
			case NetworkProtocolHelper.DIFFICULTY_NORMAL_INT:
				mc.theWorld.getWorldInfo().setDifficulty(EnumDifficulty.NORMAL);
				break;
			case NetworkProtocolHelper.DIFFICULTY_HARD_INT:
				mc.theWorld.getWorldInfo().setDifficulty(EnumDifficulty.HARD);
				break;
			default:
				mc.theWorld.getWorldInfo().setDifficulty(EnumDifficulty.PEACEFUL);
				break;
		}
	}

	@Override
	public void setClockTime(int hour, int minute) {
		if (hour >= 0 && hour <= 23 && minute >= 0 && minute <= 59) {
			int hourValue;
			if (hour >= 6 && hour <= 23) {
				hourValue = (hour - 6) * 1000;				
			} else {
				int baseValue = 18;
				hourValue = (baseValue + hour) * 1000;
			}
			int minuteValue = minute * (1000/60);
			int totalValue = hourValue + minuteValue;
			
			mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).setWorldTime(totalValue);
		}
	}

}
