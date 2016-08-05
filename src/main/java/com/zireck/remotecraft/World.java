package com.zireck.remotecraft;

public class World {
	private boolean isLoaded;
	private String name;
	private long seed;
	private int difficulty;
	private boolean isDaytime;
	private int hour;
	private int minute;
	private String timezone;
	private boolean isRaining;
	private boolean isThundering;
	
	public World() {
		reset();
	}
	
	public boolean isLoaded() {
		return isLoaded;
	}
	public void setLoaded(boolean isLoaded) {
		this.isLoaded = isLoaded;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public long getSeed() {
		return seed;
	}
	public void setSeed(long seed) {
		this.seed = seed;
	}
	public int getDifficulty() {
		return difficulty;
	}
	public void setDifficulty(int difficulty) {
		this.difficulty = difficulty;
	}
	public boolean isDaytime() {
		return isDaytime;
	}
	public void setDaytime(boolean isDaytime) {
		this.isDaytime = isDaytime;
	}
	public int getHour() {
		return hour;
	}
	public void setHour(int hour) {
		this.hour = hour;
	}
	public int getMinute() {
		return minute;
	}
	public void setMinute(int minute) {
		this.minute = minute;
	}
	public String getTimezone() {
		return timezone;
	}
	public void setTimezone(String timezone) {
		this.timezone = timezone;
	}
	public boolean isRaining() {
		return isRaining;
	}
	public void setRaining(boolean isRaining) {
		this.isRaining = isRaining;
	}
	public boolean isThundering() {
		return isThundering;
	}
	public void setThundering(boolean isThundering) {
		this.isThundering = isThundering;
	}
	
	public void reset() {
		isLoaded = false;
		name = "";
		seed = Long.MIN_VALUE;
		isDaytime = false;
		hour = Integer.MIN_VALUE;
		minute = Integer.MIN_VALUE;
		timezone = "";
		isRaining = false;
		isThundering = false;
	}
	
}
