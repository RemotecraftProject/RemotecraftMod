package com.zireck.remotecraft;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;

/**
 * Created by Zireck on 07/08/16.
 */
public class RemotecraftCommand extends CommandBase {

    private final String COMMAND_NAME = "remotecraft";
    private final String COMMAND_HELP = "Help for Remotecraft";

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return COMMAND_HELP;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        Minecraft.getMinecraft().displayGuiScreen(new RemotecraftGui());
    }
}
