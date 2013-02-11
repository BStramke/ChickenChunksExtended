package codechicken.chunkloader;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeChunkManager.Ticket;
import codechicken.chunkloader.PlayerChunkViewerManager.ChunkChange;
import codechicken.chunkloader.PlayerChunkViewerManager.TicketChange;
import codechicken.core.CommonUtils;
import codechicken.core.PacketCustom;
import codechicken.core.ServerUtils;
import codechicken.core.Vector3;
import static codechicken.chunkloader.ChunkLoaderServerPacketHandler.channel;

public class PlayerChunkViewerTracker
{
	private final PlayerChunkViewerManager manager;
	public final EntityPlayer owner;
	private HashSet<Integer> knownTickets = new HashSet<Integer>();
	public PlayerChunkViewerTracker(EntityPlayer player, PlayerChunkViewerManager manager)
	{
		owner = player;
		this.manager = manager;
		
		PacketCustom packet = new PacketCustom(ChunkLoaderServerPacketHandler.channel, 1);
		ServerUtils.sendPacketTo(player, packet.toPacket());
		
		for(WorldServer world : DimensionManager.getWorlds())
			loadDimension(world);
	}
	
	public void writeTicketToPacket(PacketCustom packet, Ticket ticket, Collection<ChunkCoordIntPair> chunkSet)
	{
		packet.writeInt(manager.ticketIDs.get(ticket));
		packet.writeString(ticket.getModId());
		String player = ticket.getPlayerName();
		packet.writeBoolean(player != null);
		if(player != null)
			packet.writeString(player);
		packet.writeByte(ticket.getType().ordinal());
		Entity entity = ticket.getEntity();
		if(entity != null)
			packet.writeInt(entity.entityId);
		packet.writeShort(chunkSet.size());
		for(ChunkCoordIntPair chunk : chunkSet)
		{
			packet.writeInt(chunk.chunkXPos);
			packet.writeInt(chunk.chunkZPos);
		}
        
        knownTickets.add(manager.ticketIDs.get(ticket));
	}
		
	@SuppressWarnings("unchecked")
    public void loadDimension(WorldServer world)
	{
		PacketCustom packet = new PacketCustom(channel, 2);
		int dim = CommonUtils.getDimension(world);
		packet.writeInt(dim);
				
		List<Chunk> allchunks = world.theChunkProviderServer.loadedChunks;		
		packet.writeShort(allchunks.size());
		for(Chunk chunk : allchunks)
		{
			packet.writeInt(chunk.xPosition);
			packet.writeInt(chunk.zPosition);
		}
		
		Map<Ticket, Collection<ChunkCoordIntPair>> tickets = ForgeChunkManager.getPersistentChunksFor(world).inverse().asMap();
		packet.writeShort(tickets.size());
		for(Entry<Ticket, Collection<ChunkCoordIntPair>> entry : tickets.entrySet())
			writeTicketToPacket(packet, entry.getKey(), entry.getValue());
		
		ServerUtils.sendPacketTo(owner, packet.toPacket());
	}

	public void unloadDimension(int dim)
	{
		PacketCustom packet = new PacketCustom(channel, 3);
		packet.writeInt(dim);
		
		ServerUtils.sendPacketTo(owner, packet.toPacket());
	}

	public void sendChunkChange(ChunkChange change)
	{
		PacketCustom packet = new PacketCustom(channel, 4);
		packet.writeInt(change.dimension);
		packet.writeInt(change.chunk.chunkXPos);
		packet.writeInt(change.chunk.chunkZPos);
		packet.writeBoolean(change.add);

		ServerUtils.sendPacketTo(owner, packet.toPacket());
	}

	public void sendTicketChange(TicketChange change)
	{
	    int ticketID = manager.ticketIDs.get(change.ticket);
	    if(!knownTickets.contains(ticketID))
	        addTicket(change.dimension, change.ticket);
	    
		PacketCustom packet = new PacketCustom(channel, 5);
		packet.writeInt(change.dimension);
		packet.writeInt(ticketID);
		packet.writeInt(change.chunk.chunkXPos);
		packet.writeInt(change.chunk.chunkZPos);
		packet.writeBoolean(change.force);
		
		ServerUtils.sendPacketTo(owner, packet.toPacket());
	}

	public void updatePlayer(EntityPlayer player)
	{
		PacketCustom packet = new PacketCustom(channel, 6);
		packet.writeString(player.username);
		packet.writeInt(player.dimension);
		Vector3 pos = Vector3.fromEntity(player);
		packet.writeFloat((float) pos.x);
		packet.writeFloat((float) pos.y);
		packet.writeFloat((float) pos.z);
		
		ServerUtils.sendPacketTo(owner, packet.toPacket());
	}
	
	public void removePlayer(String username)
	{
		PacketCustom packet = new PacketCustom(channel, 7);
		packet.writeString(username);
		
		ServerUtils.sendPacketTo(owner, packet.toPacket());
	}
	
	@SuppressWarnings("unchecked")
    public void addTicket(int dimension, Ticket ticket)
	{
		PacketCustom packet = new PacketCustom(channel, 8);
		packet.writeInt(dimension);
		writeTicketToPacket(packet, ticket, ticket.getChunkList());
		
		ServerUtils.sendPacketTo(owner, packet.toPacket());
	}
}
