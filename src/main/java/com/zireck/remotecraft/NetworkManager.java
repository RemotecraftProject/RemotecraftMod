package com.zireck.remotecraft;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import net.minecraft.client.Minecraft;

public class NetworkManager implements Runnable {
	
	private final static int PORT = 9999;
	
	// Server Socket
	private ServerSocket mServerSocket;
	private Socket mClientSocket = null;
	private ObjectOutputStream out;
	private ObjectInputStream in;
	private String msg;
	
	// Self Thread
	private Thread mThread;

	// This class needs to stop running
	private boolean mKeepRunning;
	
	// Tells you if there's a currently established socket connection
	private boolean mConnectivity;
	
	// Reference to Core class
	private INetworkManager mCallback;
	
	/**
	 * INetworkManager
	 * @author Zireck
	 *
	 */
	public interface INetworkManager {
		//public boolean isWorldLoaded();
		
		public void sendEverything();
		
		public void setHealth(int mHealth);
		public void setHunger(int mHunger);
		public void setExpLvl(int mExpLvl);
		public void toggleGameMode();
		public void setGameMode(String gamemode);
		public void getInventory();
		public void getEnderchest();
		public void stopRecord();

		public void setWorldTime(String dayOrNight);
		public void setWorldWeather();
		public void enableScreenShot();
		public void teleportTo(int mDim, int x, int y, int z);
		public void toggleButton(int mDim, int x, int y, int z);
		public void toggleLever(int mDim, int x, int y, int z);
		public void playRecord(String record);
		public void setDifficulty(int difficulty);
		public void setClockTime(int hour, int minute);
		
		public void forceSendWorldInfo();
	}
	
	/**
	 * Constructor
	 * @param core Core reference
	 */
	public NetworkManager(Core core) {
		System.out.println("k9d3 creating NetowkrManager");
		// Make sure that Core class implements INetworkListener
		try {
			mCallback = (INetworkManager) core;
		} catch (ClassCastException e) {
			e.printStackTrace();
		}
		
		setKeepRunning(true);
		setConnectivity(false);
		
		// Start running thread
		mThread = new Thread(this);
		mThread.start();
	}
	
	/**
	 * Get keep running flag
	 * @return Keep Running flag
	 */
	private boolean getKeepRunning() {
		return mKeepRunning;
	}
	
	/**
	 * Set keep running flag
	 * @param keepRunning Keep Running flag
	 */
	private void setKeepRunning(boolean keepRunning) {
		mKeepRunning = keepRunning;
	}
	
	/**
	 * Get connectivity flag
	 * @return Connectivity flag
	 */
	private boolean getConnectivity() {
		return mConnectivity;
	}
	
	/**
	 * Set connectivity flag
	 * @param connectivity Connectivity flag
	 */
	private void setConnectivity(boolean connectivity) {
		mConnectivity = connectivity;
	}

	/**
	 * Network communication thread
	 */
	public void run() {

		while (!Thread.currentThread().isInterrupted() && getKeepRunning()) {
		
			try {
				// Starting the server (reuse & bind)
				System.out.println("k9d3 Starting the server (reuse & bind)");
				mServerSocket = new ServerSocket();
				mServerSocket.setReuseAddress(true);
				mServerSocket.bind(new InetSocketAddress(NetworkManager.PORT));
				
				// Waiting for a client to connect
				System.out.println("k9d3 Waiting for a client to connect");
				mClientSocket = mServerSocket.accept();
				setConnectivity(true);
				
				// IO setup
				System.out.println("k9d3 IO setup");
				in = new ObjectInputStream(mClientSocket.getInputStream());
				out = new ObjectOutputStream(mClientSocket.getOutputStream());
				out.flush();
	            
	            // The first time a client is connected, you need to send him everything over the socket
				System.out.println("k9d3 The first time a client is connected, you need to send him everything over the socket");
	            if (!mThread.isInterrupted() && getKeepRunning() && getConnectivity()) {
	            	mCallback.sendEverything();
	            }
	            
	            // Socket communication main loop
	            System.out.println("k9d3 Socket communication main loop");
	            while (!Thread.currentThread().isInterrupted() && getKeepRunning() && getConnectivity()) {
	            	try {
	            		msg = (String) in.readObject();
	            		processMessage(msg);
	            	} catch (ClassNotFoundException e) {
	            		e.printStackTrace();
	            	} catch (EOFException e) {
	            		e.printStackTrace();
	            		setConnectivity(false);
	            	} catch (SocketException e) {
	            		e.printStackTrace();
	            		setConnectivity(false);
	            	}
				}
	            
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					if (out!=null) {
						synchronized (out) {
							out.close();
							out = null;
						}
					}
					
					if (in != null) {
						in.close();
						in = null;
					}
					
					if (mClientSocket != null) {
						mClientSocket.close();
						mClientSocket = null;
					}
					
					if (mServerSocket != null) {
						mServerSocket.close();
						mServerSocket = null;
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				setConnectivity(false);
			} // finally
		} // while
	} // run
	
	/**
	 * Send message to the client
	 * @param msg Message
	 */
	public void sendMessage(String msg) {
		
		if (getKeepRunning() && getConnectivity()) {
			if (out != null) {
				synchronized (out) {
					try {
						out.writeObject(msg);
						out.flush();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		
	}
	
	/**
	 * Shutdown the Server, kind of like a class destructor
	 */
	public void shutdown() {
		sendMessage(NetworkProtocolHelper.COMMAND_QUIT);
		
		setConnectivity(false);
		setKeepRunning(false);
		
		mThread.interrupt();
		
		try {

			if (out!=null) {
				synchronized (out) {
					out.close();
				}
			}
			
			if (in != null)
				in.close();
			
			if (mClientSocket != null)
				mClientSocket.close();
			
			if (mServerSocket != null)
				mServerSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		mThread = null;
		
	}
	
	/**
	 * Process message received from socket
	 * @param msg Message received
	 */
	private void processMessage(String msg) {
		
		if (msg.equals(NetworkProtocolHelper.COMMAND_QUIT)) {
			setConnectivity(false);
		} else {
			String[] msg_split = msg.split(NetworkProtocolHelper.SEPARATOR_COMMAND);
			if (msg_split == null || msg_split.length < 1) {
				return;
			}
			
			String command = msg_split[0];
		
			// Single parameter
			if (command.equals(NetworkProtocolHelper.COMMAND_GETWORLDINFO)) {
				// Everytime the WorldFragment is created in the Android app
				mCallback.forceSendWorldInfo();
			} else if (command.equals(NetworkProtocolHelper.COMMAND_SETWEATHER)) {
				mCallback.setWorldWeather();
			} else if (command.equals(NetworkProtocolHelper.COMMAND_GETSCREENSHOT)) {
				mCallback.enableScreenShot();
			}  else if (command.equals(NetworkProtocolHelper.COMMAND_GETINVENTORY)) {
				mCallback.getInventory();
			} else if (command.equals(NetworkProtocolHelper.COMMAND_GETENDERCHEST)) {
				mCallback.getEnderchest();
			} else if (command.equals(NetworkProtocolHelper.COMMAND_RECORD_STOP)) {
				mCallback.stopRecord();
			}

			// More than 1 parameter
			if (msg_split.length < 2) {
				return;
			}
			String[] args = msg_split[1].split(NetworkProtocolHelper.SEPARATOR_ARGS);
			
			if (command.equals(NetworkProtocolHelper.COMMAND_SETGAMEMODE)) {
				String gamemode = args[0];
				mCallback.setGameMode(gamemode);
			} else if (command.equals(NetworkProtocolHelper.COMMAND_SETTIME)) {
				String dayOrNight = args[0];
				mCallback.setWorldTime(dayOrNight);
			} else if (command.equals(NetworkProtocolHelper.COMMAND_SETHEALTH)) {
				int health = Integer.parseInt(args[0]);
				mCallback.setHealth(health);
			} else if (command.equals(NetworkProtocolHelper.COMMAND_SETHUNGER)) {
				int hunger = Integer.parseInt(args[0]);
				mCallback.setHunger(hunger);
			} else if (command.equals(NetworkProtocolHelper.COMMAND_SETEXPLVL)) {
				int expLvl = Integer.parseInt(args[0]);
				mCallback.setExpLvl(expLvl);
			} else if (command.equals(NetworkProtocolHelper.COMMAND_TELEPORT)) {
				String dimension = args[0];
				int dim;
				if (dimension.equalsIgnoreCase(NetworkProtocolHelper.DIMENSION_NETHER)) {
					dim = -1;
				} else if (dimension.equalsIgnoreCase(NetworkProtocolHelper.DIMENSION_END)) {
					dim = 1;
				} else {
					dim = 0;
				}
				int x = Integer.parseInt(args[1]);
				int y = Integer.parseInt(args[2]);
				int z = Integer.parseInt(args[3]);
				mCallback.teleportTo(dim, x, y, z);
			} else if (command.equals(NetworkProtocolHelper.COMMAND_REDSTONE_BUTTON)) {
				String dimension = args[0];
				int dim;
				if (dimension.equalsIgnoreCase(NetworkProtocolHelper.DIMENSION_NETHER)) {
					dim = -1;
				} else if (dimension.equalsIgnoreCase(NetworkProtocolHelper.DIMENSION_END)) {
					dim = 1;
				} else {
					dim = 0;
				}
				int x = Integer.parseInt(args[1]);
				int y = Integer.parseInt(args[2]);
				int z = Integer.parseInt(args[3]);
				mCallback.toggleButton(dim, x, y, z);
			} else if (command.equals(NetworkProtocolHelper.COMMAND_REDSTONE_LEVER)) {
				String dimension = args[0];
				int dim = 0;
				if (dimension.equalsIgnoreCase(NetworkProtocolHelper.DIMENSION_NETHER)) {
					dim = -1;
				} else if (dimension.equalsIgnoreCase(NetworkProtocolHelper.DIMENSION_END)) {
					dim = 1;
				} else {
					dim = 0;
				}
				int x = Integer.parseInt(args[1]);
				int y = Integer.parseInt(args[2]);
				int z = Integer.parseInt(args[3]);
				mCallback.toggleLever(dim, x, y, z);
			} else if (command.equals(NetworkProtocolHelper.COMMAND_RECORD_PLAY)) {
				mCallback.playRecord(args[0]);
			} else if (command.equals(NetworkProtocolHelper.COMMAND_SETDIFFICULTY)) {
				int difficulty = Integer.parseInt(args[0]);
				mCallback.setDifficulty(difficulty);
			} else if (command.equals(NetworkProtocolHelper.COMMAND_SETCLOCKTIME)) {
				int hour = Integer.parseInt(args[0]);
				int minute = Integer.parseInt(args[1]);
				mCallback.setClockTime(hour, minute);
			}
		
		}
	}
	
	// Player Info
	
	/**
	 * Send player name
	 * @param playerName Player name
	 */
	public void sendPlayername(String playerName) {
		sendMessage(NetworkProtocolHelper.INFO_PLAYERNAME + NetworkProtocolHelper.SEPARATOR_COMMAND + playerName);
	}
	
	/**
	 * Send player gamemode
	 * @param gamemode Player gamemode
	 */
	public void sendGamemode(String gamemode) {
		sendMessage(NetworkProtocolHelper.INFO_GAMEMODE + NetworkProtocolHelper.SEPARATOR_COMMAND + gamemode);
	}
	
	/**
	 * Send player health value
	 * @param health Player health
	 */
	public void sendHealth(int health) {
		sendMessage(NetworkProtocolHelper.INFO_HEALTH + NetworkProtocolHelper.SEPARATOR_COMMAND + health);
	}
	
	/**
	 * Send player hunger value
	 * @param hunger Player hunger
	 */
	public void sendHunger(int hunger) {
		sendMessage(NetworkProtocolHelper.INFO_HUNGER + NetworkProtocolHelper.SEPARATOR_COMMAND + hunger);
	}
	
	/**
	 * Send player armor value
	 * @param armor Player armor
	 */
	public void sendArmor(int armor) {
		sendMessage(NetworkProtocolHelper.INFO_ARMOR + NetworkProtocolHelper.SEPARATOR_COMMAND + armor);
	}
	
	/**
	 * Send player experience level
	 * @param expLvl Player experience level
	 */
	public void sendExpLevel(int expLvl) {
		sendMessage(NetworkProtocolHelper.INFO_EXPLVL + NetworkProtocolHelper.SEPARATOR_COMMAND + expLvl);
	}
	
	/**
	 * Send player X coordinate
	 * @param x Player X coord
	 */
	public void sendCoordX(int x) {
		String coordX = Integer.toString(x);
		sendMessage(NetworkProtocolHelper.INFO_COORDX + NetworkProtocolHelper.SEPARATOR_COMMAND + coordX);
	}
	
	/**
	 * Send player Y coordinate
	 * @param y Player Y coord
	 */
	public void sendCoordY(int y) {
		String coordY = Integer.toString(y);
		sendMessage(NetworkProtocolHelper.INFO_COORDY + NetworkProtocolHelper.SEPARATOR_COMMAND + coordY);
	}
	
	/**
	 * Send player Z coordinate
	 * @param z Player Z coord
	 */
	public void sendCoordZ(int z) {
		String coordZ = Integer.toString(z);
		sendMessage(NetworkProtocolHelper.INFO_COORDZ + NetworkProtocolHelper.SEPARATOR_COMMAND + coordZ);
	}
	
	/**
	 * Send player biome
	 * @param biome Player biome
	 */
	public void sendBiome(String biome) {
		sendMessage(NetworkProtocolHelper.INFO_BIOME + NetworkProtocolHelper.SEPARATOR_COMMAND + biome);
	}
	
	/**
	 * Send player current item
	 * @param currentItem Player current item
	 */
	public void sendCurrentItem(String currentItem) {
		sendMessage(NetworkProtocolHelper.INFO_CURRENTITEM + NetworkProtocolHelper.SEPARATOR_COMMAND + currentItem);
	}
	
	/**
	 * Send player no current item
	 */
	public void sendNoCurrentItem() {
		sendMessage(NetworkProtocolHelper.INFO_CURRENTITEM_NULL);
	}
	
	// World Info
	
	/**
	 * Send world name
	 * @param worldName World name
	 */
	public void sendWorldName(String worldName) {
		sendMessage(NetworkProtocolHelper.INFO_WORLDNAME + NetworkProtocolHelper.SEPARATOR_COMMAND + worldName);
	}
	
	/**
	 * Send world seed
	 * @param seed Seed long value
	 */
	public void sendSeed(long seed) {
		System.out.println("k9d3 sending seed = " + seed);
		sendMessage(NetworkProtocolHelper.INFO_SEED + NetworkProtocolHelper.SEPARATOR_COMMAND + Long.toString(seed));
	}
	
	/**
	 * Send world difficulty
	 * @param difficulty Difficulty int value
	 */
	public void sendDifficulty(int difficulty) {
		sendMessage(NetworkProtocolHelper.INFO_DIFFICULTY + NetworkProtocolHelper.SEPARATOR_COMMAND + Integer.toString(difficulty));
	}
	
	/**
	 * Send daytime
	 * @param daytime Daytime true or false
	 */
	public void sendDaytime(boolean daytime) {
		if (daytime) {
			sendMessage(NetworkProtocolHelper.INFO_DAYTIME + NetworkProtocolHelper.SEPARATOR_COMMAND + "TRUE");
		} else {
			sendMessage(NetworkProtocolHelper.INFO_DAYTIME + NetworkProtocolHelper.SEPARATOR_COMMAND + "FALSE");
		}
	}
	
	/**
	 * Send world time
	 * @param hour World hour
	 * @param minute World minute
	 */
	public void sendTime(int hour, int minute) {
		String finalHour = Integer.toString(hour);
		String finalMinute;
		if (minute < 10) {
			finalMinute = "0"+Integer.toString(minute);
		} else {
			finalMinute = Integer.toString(minute);
		}
		String mTime = finalHour + NetworkProtocolHelper.SEPARATOR_ARGS + finalMinute;
		sendMessage(NetworkProtocolHelper.INFO_TIME + NetworkProtocolHelper.SEPARATOR_COMMAND + mTime);
	}
	
	/**
	 * Send raining
	 * @param isRaining
	 */
	public void sendRaining(boolean isRaining) {
		if (isRaining) {
			sendMessage(NetworkProtocolHelper.INFO_RAINING + NetworkProtocolHelper.SEPARATOR_COMMAND + "TRUE");
		} else {
			sendMessage(NetworkProtocolHelper.INFO_RAINING + NetworkProtocolHelper.SEPARATOR_COMMAND + "FALSE");
		}
	}
	
	/**
	 * Send thundering
	 * @param isThundering
	 */
	public void sendThundering(boolean isThundering) {
		if (isThundering) {
			sendMessage(NetworkProtocolHelper.INFO_THUNDERING + NetworkProtocolHelper.SEPARATOR_COMMAND + "TRUE");
		} else {
			sendMessage(NetworkProtocolHelper.INFO_THUNDERING + NetworkProtocolHelper.SEPARATOR_COMMAND + "FALSE");
		}
	}
	
	/**
	 * Send screenshot
	 * @param fileName File name
	 * @param mSeed World seed
	 * @throws IOException
	 */
	public void sendScreenShot(String fileName, long mSeed) throws IOException {
		synchronized (out) {
			sendMessage(NetworkProtocolHelper.COMMAND_SCREENSHOT_SEND + NetworkProtocolHelper.SEPARATOR_COMMAND + String.valueOf(mSeed));
			
			// File to send
			File myFile = new File(Minecraft.getMinecraft().mcDataDir.getCanonicalPath() + File.separator + "screenshots" + File.separator, fileName);
			int fSize = (int) myFile.length();
			if (fSize < myFile.length()) {
				System.out.println("File is too big");
				sendMessage(NetworkProtocolHelper.COMMAND_SCREENSHOT_ERROR);
				return;
			}
			
			// Send the file's size
			byte[] bSize = new byte[4];
			bSize[0] = (byte) ((fSize & 0xff000000) >> 24);
		    bSize[1] = (byte) ((fSize & 0x00ff0000) >> 16);
		    bSize[2] = (byte) ((fSize & 0x0000ff00) >> 8);
		    bSize[3] = (byte) (fSize & 0x000000ff);
		    // 4 bytes containing the file size
		    try {
		    	out.write(bSize, 0, 4);
		    } catch (IOException e) {
		    	e.printStackTrace();
				sendMessage(NetworkProtocolHelper.COMMAND_SCREENSHOT_ERROR);
		    	return;
		    }
		    
		    boolean noMemoryLimitation = false;
		    
		    FileInputStream fis = null;
		    BufferedInputStream bis = null;
		    try {
		    	fis = new FileInputStream(myFile);
		    	bis = new BufferedInputStream(fis);
		    	
		    	if (noMemoryLimitation) {
		    
			    	byte[] outBuffer = new byte[fSize];
			    	int bRead = bis.read(outBuffer, 0, outBuffer.length);
			    	out.write(outBuffer, 0, bRead);
		    	
		    	} else {
		    		
		    		int bRead = 0;
		    		byte[] outBuffer = new byte[8*1024];
		    		while ( (bRead = bis.read(outBuffer, 0, outBuffer.length)) > 0 ) {
		    			out.write(outBuffer, 0, bRead);
		    		}
		    		
		    	}
		    	out.flush();
		    } catch (IOException e) {
		    	e.printStackTrace();
				sendMessage(NetworkProtocolHelper.COMMAND_SCREENSHOT_ERROR);
		    	return;
		    } finally {
		    	try {
		    		bis.close();
		    	} catch (IOException e) {
		    		e.printStackTrace();
		    	} catch (NullPointerException e) {
		    		e.printStackTrace();
		    	}
		    	sendMessage(NetworkProtocolHelper.COMMAND_SCREENSHOT_FINISHED + NetworkProtocolHelper.SEPARATOR_COMMAND + String.valueOf(mSeed));
		    }
		    
		}
		
	}
	
	/**
	 * Send inventory item
	 * @param item Item name
	 * @param amount Item amount
	 */
	public void sendInventoryItem(String item, int amount) {
		sendMessage(NetworkProtocolHelper.INFO_INVENTORY_ITEM + NetworkProtocolHelper.SEPARATOR_COMMAND + item + NetworkProtocolHelper.SEPARATOR_ARGS + String.valueOf(amount));
	}
	
	/**
	 * Send enderchest item
	 * @param item Item name
	 * @param amount Item amount
	 */
	public void sendEnderchestItem(String item, int amount) {
		sendMessage(NetworkProtocolHelper.INFO_ENDERCHEST_ITEM + NetworkProtocolHelper.SEPARATOR_COMMAND + item + NetworkProtocolHelper.SEPARATOR_ARGS + String.valueOf(amount));
	}
	
	/**
	 * Send spawn point coordinates
	 * @param x X coord
	 * @param y Y coord
	 * @param z Z coord
	 */
	public void sendSpawnPoint(int x, int y, int z) {
		sendMessage(NetworkProtocolHelper.INFO_SPAWNPOINT + NetworkProtocolHelper.SEPARATOR_COMMAND + String.valueOf(x) + NetworkProtocolHelper.SEPARATOR_ARGS + String.valueOf(y) + NetworkProtocolHelper.SEPARATOR_ARGS + String.valueOf(z));
	}

}
