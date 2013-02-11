package codechicken.chunkloader;

import codechicken.core.commands.PlayerCommand;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.command.ICommandSender;
import net.minecraft.world.WorldServer;

public class CommandChunkLoaders extends PlayerCommand
{
	@Override
	public String getCommandName()
	{
		return "chunkloaders";
	}

	@Override
	public String getCommandUsage(ICommandSender var1)
	{
		return "chunkloaders";
	}

	@Override
	public void handleCommand(WorldServer world, EntityPlayerMP player, String[] args)
	{
		if(PlayerChunkViewerManager.instance().isViewerOpen(player.username))
		{
			player.sendChatToPlayer("Chunk Viewer already open.");
			return;
		}
		if(!ChunkLoaderManager.allowChunkViewer(player.username))
		{
			player.sendChatToPlayer("You are not allowed to use the ChunkViewer.");
			return;
		}
		PlayerChunkViewerManager.instance().addViewers.add(player.username);
	}
	
	@Override
	public void printHelp(ICommandSender listener)
	{
		listener.sendChatToPlayer("/chunkloaders");
	}

	@Override
	public boolean OPOnly()
	{
		return false;
	}
	
	@Override
	public int minimumParameters()
	{
		return 0;
	}
}
