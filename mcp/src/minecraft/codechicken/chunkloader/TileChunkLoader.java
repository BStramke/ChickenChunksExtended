package codechicken.chunkloader;

import java.util.Collection;
import java.util.HashSet;

import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.packet.Packet;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import codechicken.core.PacketCustom;

public class TileChunkLoader extends TileChunkLoaderBase implements IChickenChunkLoader
{
	public TileChunkLoader()
	{
		radius = 2;
		shape = ChunkLoaderShape.Square;
	}
	
	public static void handleDescriptionPacket(PacketCustom packet, World world)
	{
		TileEntity tile = world.getBlockTileEntity(packet.readInt(), packet.readInt(), packet.readInt());
		if(tile instanceof TileChunkLoader)
		{		
			TileChunkLoader ctile = (TileChunkLoader)tile; 		
			ctile.setShapeAndRadius(ChunkLoaderShape.values()[packet.readUnsignedByte()], packet.readUnsignedByte());
			ctile.active = packet.readBoolean();
			if(packet.readBoolean())
				ctile.owner = packet.readString();
		}
	}
	
	public boolean setShapeAndRadius(ChunkLoaderShape newShape, int newRadius)
	{
		if(worldObj.isRemote)
		{
			radius = newRadius;
			shape = newShape;
			return true;
		}
        Collection<ChunkCoordIntPair> chunks = getContainedChunks(newShape, xCoord, zCoord, newRadius);
        if(chunks.size() > ChunkLoaderManager.maxChunks)
		{
		    return false;
		}
		else if(powered)
		{
            radius = newRadius;
            shape = newShape;
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
            return true;
		}
		else if(ChunkLoaderManager.canLoaderAdd(this, chunks))
		{
			radius = newRadius;
			shape = newShape;
			ChunkLoaderManager.updateLoader(this);
			worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
			return true;
		}
		return false;
	}

	public Packet getDescriptionPacket()
	{
		PacketCustom packet = new PacketCustom(ChunkLoaderServerPacketHandler.channel, 10);
		packet.setChunkDataPacket();
		packet.writeCoord(xCoord, yCoord, zCoord);
		packet.writeByte(shape.ordinal());
		packet.writeByte(radius);
		packet.writeBoolean(active);
        packet.writeBoolean(owner != null);
		if(owner != null)
			packet.writeString(owner);
		return packet.toPacket();
	}
	
	public void writeToNBT(NBTTagCompound tag)
	{
		super.writeToNBT(tag);
		tag.setByte("radius", (byte) radius);
		tag.setByte("shape", (byte) shape.ordinal());
	}
	
	@Override
	public void readFromNBT(NBTTagCompound tag)
	{
		super.readFromNBT(tag);
		radius = tag.getByte("radius");
		shape = ChunkLoaderShape.values()[tag.getByte("shape")];
	}
	
	@Override
	public HashSet<ChunkCoordIntPair> getChunks()
	{
		return getContainedChunks(shape, xCoord, zCoord, radius);
	}
	
	public static HashSet<ChunkCoordIntPair> getContainedChunks(ChunkLoaderShape shape, int xCoord, int zCoord, int radius)
	{
		return shape.getLoadedChunks(xCoord >> 4, zCoord >> 4, radius - 1);
	}
	
    public int countLoadedChunks()
    {
        return getChunks().size();
    }   
	
	public int radius;
	public ChunkLoaderShape shape;
}
