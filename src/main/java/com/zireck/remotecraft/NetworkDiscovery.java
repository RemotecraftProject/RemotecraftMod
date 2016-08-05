package com.zireck.remotecraft;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class NetworkDiscovery implements Runnable {
	
	private static NetworkDiscovery INSTANCE;
	
	private final static int PORT = 9998;
	
	private Thread mThread;
	
	private DatagramSocket mSocket;
	
	private INetworkDiscovery mCallback;
	
	public interface INetworkDiscovery {
		public String getWorldName();
		public long getWorldSeed();
		public String getPlayerName();
	}
	
	/**
	 * Private constructor for singleton
	 */
	private NetworkDiscovery() {
		mThread = new Thread(this);
		mThread.start();
	}

	/**
	 * Instance retriever
	 * @return Class instance
	 */
	public static NetworkDiscovery getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new NetworkDiscovery();
		}
		return INSTANCE;
	}
	
	public void setInterface(Core core) {
		// Make sure that Core class implements INetworkDiscovery
		try {
			mCallback = (INetworkDiscovery) core;
		} catch (ClassCastException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		while (!Thread.currentThread().isInterrupted()) {
			
			try {
				mSocket = new DatagramSocket(NetworkDiscovery.PORT, InetAddress.getByName("0.0.0.0"));
				mSocket.setBroadcast(true);
				
				// Keep an UDP Socket open
				while (!Thread.currentThread().isInterrupted() && mSocket.isBound()) {
					// Ready to receive sockets
					
					// Receive a packet
					byte[] rcvBuff = new byte[15000];
					DatagramPacket packet = new DatagramPacket(rcvBuff, rcvBuff.length);
					mSocket.receive(packet);
					
					// Packet received
					String msg = new String(packet.getData()).trim();
					if (msg.equals(NetworkProtocolHelper.DISCOVERY_REQUEST)) {
						// Attach world seed & world name & player name
						String msgResponse = NetworkProtocolHelper.DISCOVERY_RESPONSE + NetworkProtocolHelper.SEPARATOR_COMMAND + String.valueOf(mCallback.getWorldSeed()) + NetworkProtocolHelper.SEPARATOR_ARGS + mCallback.getWorldName() + NetworkProtocolHelper.SEPARATOR_ARGS + mCallback.getPlayerName();
						byte[] sendData = msgResponse.getBytes();
						
						// Send response
						DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, packet.getAddress(), packet.getPort());
						if (mSocket.isBound()) {
							try {
								mSocket.send(sendPacket);
							} catch (Exception e) {
								e.printStackTrace();
							}
						} else {
							continue;
						}
					}
				}
				
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					mSocket.disconnect();
					mSocket.close();
				} catch (NullPointerException e) {
					e.printStackTrace();
				}
			}
		
		} // while !threadIsInterrupted		
	}
	
	public void shutdown() {
		if (mSocket != null) {
			mSocket.close();
			mSocket = null;
		}
		
		if (mThread != null) {
			mThread.interrupt();
		}
		
		mThread = null;
		
		INSTANCE = null;
	}

}
