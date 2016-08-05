package com.zireck.remotecraft;

public class Player {
	private String name;
	private String gamemode;
	private int health;
	private int hunger;
	private int armor;
	private int exp;
	private int x, y, z;
	private String biome;
	private String item;
	
	public Player() {
		reset();
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getGamemode() {
		return gamemode;
	}
	public void setGamemode(String gamemode) {
		this.gamemode = gamemode;
	}
	public int getHealth() {
		return health;
	}
	public void setHealth(int health) {
		this.health = health;
	}
	public int getHunger() {
		return hunger;
	}
	public void setHunger(int hunger) {
		this.hunger = hunger;
	}
	public int getArmor() {
		return armor;
	}
	public void setArmor(int armor) {
		this.armor = armor;
	}
	public int getExp() {
		return exp;
	}
	public void setExp(int exp) {
		this.exp = exp;
	}
	public int getX() {
		return x;
	}
	public void setX(int x) {
		this.x = x;
	}
	public int getY() {
		return y;
	}
	public void setY(int y) {
		this.y = y;
	}
	public int getZ() {
		return z;
	}
	public void setZ(int z) {
		this.z = z;
	}
	public String getBiome() {
		return biome;
	}
	public void setBiome(String biome) {
		this.biome = biome;
	}
	public String getItem() {
		return item;
	}
	public void setItem(String item) {
		this.item = item;
	}
	
	public void reset() {
		name = "";
		health = Integer.MIN_VALUE;
		hunger = Integer.MIN_VALUE;
		armor = Integer.MIN_VALUE;
		exp = Integer.MIN_VALUE;
		x = Integer.MIN_VALUE;
		y = Integer.MIN_VALUE;
		z = Integer.MIN_VALUE;
		biome = "";
		item = "";
	}
	
}
