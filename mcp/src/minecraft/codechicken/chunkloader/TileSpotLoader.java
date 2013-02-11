package codechicken.chunkloader;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.network.packet.Packet;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import codechicken.core.PacketCustom;

public class TileSpotLoader extends TileChunkLoaderBase implements IChickenChunkLoader
{
	public static void handleDescriptionPacket(PacketCustom packet, World world)
	{
		TileEntity tile = world.getBlockTileEntity(packet.readInt(), packet.readInt(), packet.readInt());
		if(tile instanceof TileSpotLoader)
		{
			TileSpotLoader ctile = (TileSpotLoader)tile;
			ctile.active = packet.readBoolean();
			if(packet.readBoolean())
				ctile.owner = packet.readString();
		}
	}
	
	public Packet getDescriptionPacket()
	{
		PacketCustom packet = new PacketCustom(ChunkLoaderServerPacketHandler.channel, 11);
		packet.setChunkDataPacket();
		packet.writeCoord(xCoord, yCoord, zCoord);
		packet.writeBoolean(active);
        packet.writeBoolean(owner != null);
		if(owner != null)
			packet.writeString(owner);
		return packet.toPacket();
	}
	
	@Override
	public Collection<ChunkCoordIntPair> getChunks()
	{
		return Arrays.asList(getChunkPosition());
	}
	
	public static HashSet<ChunkCoordIntPair> getContainedChunks(ChunkLoaderShape shape, int xCoord, int zCoord, int radius)
	{
		return shape.getLoadedChunks(xCoord >> 4, zCoord >> 4, radius - 1);
	}
}
