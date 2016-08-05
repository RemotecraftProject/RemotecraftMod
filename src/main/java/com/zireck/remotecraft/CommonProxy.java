package com.zireck.remotecraft;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.FMLCommonHandler;

public class CommonProxy {
	public void initialize() {
		FMLCommonHandler.instance().bus().register(new Core(Minecraft.getMinecraft()));
	}
}
