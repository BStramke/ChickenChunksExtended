package codechicken.chunkloader;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetServerHandler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import codechicken.core.PacketCustom;
import codechicken.core.PacketCustom.ICustomPacketHandler.IServerPacketHandler;

public class ChunkLoaderServerPacketHandler implements IServerPacketHandler
{
	public static String channel = "ChickenChunks";

	@Override
	public void handlePacket(PacketCustom packet, NetServerHandler nethandler, EntityPlayerMP sender)
	{
		switch(packet.getType())
		{
			case 1:
				PlayerChunkViewerManager.instance().closeViewer(sender.username);
				break;
			case 2:
				handleChunkLoaderChangePacket(sender.worldObj, packet);
				break;
			
		}
	}

	private void handleChunkLoaderChangePacket(World world, PacketCustom packet)
	{
		TileEntity tile = world.getBlockTileEntity(packet.readInt(), packet.readInt(), packet.readInt());
		if(tile instanceof TileChunkLoader)
		{		
			TileChunkLoader ctile = (TileChunkLoader)tile;
			ctile.setShapeAndRadius(ChunkLoaderShape.values()[packet.readUnsignedByte()], packet.readUnsignedByte());
		}
	}
}
