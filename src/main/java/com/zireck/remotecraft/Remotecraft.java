package com.zireck.remotecraft;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

@Mod(modid=Remotecraft.MODID, name=Remotecraft.MODNAME, version=Remotecraft.VERSION)
public class Remotecraft {
	
    public static final String MODID = "remotecraft";
    public static final String MODNAME = "Remotecraft";
    public static final String VERSION = "1.0";
	
    // The instance of your mod that Forge uses.
    @Instance(value = Remotecraft.MODID)
    public static Remotecraft instance;
    
    // Says where the client and server 'proxy' code is loaded.
    @SidedProxy(clientSide="com.zireck.remotecraft.ClientProxy", serverSide="com.zireck.remotecraft.CommonProxy")
    public static CommonProxy proxy;

    @EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.initialize();
    }

    @EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new RemotecraftCommand());
    }
}
