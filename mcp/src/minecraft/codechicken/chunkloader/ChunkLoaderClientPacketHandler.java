package codechicken.chunkloader;

import net.minecraft.client.Minecraft;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.client.multiplayer.NetClientHandler;
import codechicken.core.ClientUtils;
import codechicken.core.PacketCustom;
import codechicken.core.Vector3;
import codechicken.core.PacketCustom.ICustomPacketHandler.IClientPacketHandler;

public class ChunkLoaderClientPacketHandler implements IClientPacketHandler
{
	public static String channel = "ChickenChunks";
	
	@Override
	public void handlePacket(PacketCustom packet, NetClientHandler nethandler, Minecraft mc)
	{
		switch(packet.getType())
		{
			case 1:
				PlayerChunkViewer.openViewer();
				break;
			case 2:
				PlayerChunkViewer.instance().loadDimension(packet, mc.theWorld);
				break;
			case 3:
				PlayerChunkViewer.instance().unloadDimension(packet.readInt());
				break;
			case 4:
				PlayerChunkViewer.instance().handleChunkChange(
						packet.readInt(), 
						new ChunkCoordIntPair(packet.readInt(), packet.readInt()), 
						packet.readBoolean());
				break;
			case 5:
				PlayerChunkViewer.instance().handleTicketChange(
						packet.readInt(), 
						packet.readInt(),
						new ChunkCoordIntPair(packet.readInt(), packet.readInt()), 
						packet.readBoolean());
				break;
			case 6:
				PlayerChunkViewer.instance().handlePlayerUpdate(
						packet.readString(), packet.readInt(),
						new Vector3(packet.readFloat(), packet.readFloat(), packet.readFloat()));
				break;
			case 7:
				PlayerChunkViewer.instance().removePlayer(packet.readString());
				break;
			case 8:
				PlayerChunkViewer.instance().handleNewTicket(packet, mc.theWorld);
				break;
            case 10:
                TileChunkLoader.handleDescriptionPacket(packet, mc.theWorld);
                break;
            case 11:
                TileSpotLoader.handleDescriptionPacket(packet, mc.theWorld);
                break;
				
		}
	}

	public static void sendGuiClosing()
	{
		PacketCustom packet = new PacketCustom(channel, 1);
		ClientUtils.sendPacket(packet.toPacket());
	}

	public static void sendShapeChange(TileChunkLoader tile, ChunkLoaderShape shape, int radius)
	{
		PacketCustom packet = new PacketCustom(channel, 2);
		packet.writeCoord(tile.xCoord, tile.yCoord, tile.zCoord);
		packet.writeByte(shape.ordinal());
		packet.writeByte(radius);
		ClientUtils.sendPacket(packet.toPacket());
	}
}
