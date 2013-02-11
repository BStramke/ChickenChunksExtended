package codechicken.chunkloader;

import net.minecraft.command.ICommandSender;
import codechicken.core.commands.CoreCommand;

public class CommandDebugInfo extends CoreCommand
{
	@Override
	public String getCommandName()
	{
		return "ccdebug";
	}

	@Override
	public boolean OPOnly()
	{
		return false;
	}

	@Override
	public void handleCommand(String command, String playername, String[] args, ICommandSender listener)
	{
		
	}

	@Override
	public void printHelp(ICommandSender listener)
	{
		listener.sendChatToPlayer("/ccdebug [dimension]");
	}

	@Override
	public int minimumParameters()
	{
		return 0;
	}
}
