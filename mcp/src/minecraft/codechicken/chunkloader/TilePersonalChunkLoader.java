package codechicken.chunkloader;

import java.util.HashSet;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.packet.Packet3Chat;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.ChunkCoordIntPair;
import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.common.network.Player;

public class TilePersonalChunkLoader extends TileChunkLoader implements IChickenChunkLoader
{
	public long nextUnloadTime = 0;
	@Override
    public void updateEntity()
    {
        if(!worldObj.isRemote)
        {
        	if(System.currentTimeMillis() < nextUnloadTime)
        	{
        		if(!active)
        			activate();
        	}
        	else
        	{
        		if(active) {
        			deactivate();
        			if(owner!=null)
        			{
	        			EntityPlayerMP epmp = MinecraftServer.getServerConfigurationManager(MinecraftServer.getServer()).getPlayerForUsername(owner);
						if (epmp != null) {
							PacketDispatcher.sendPacketToPlayer(new Packet3Chat("The Loader at x=" + xCoord + ", y=" + yCoord + ", z=" + zCoord + " has stopped loading."), (Player) epmp);
						}
        			}
        		}
        	}
        }
        else
        {
            renderInfo.update(this);
        }
    }
	
	public void writeToNBT(NBTTagCompound tag)
	{
		super.writeToNBT(tag);
		tag.setLong("nextUnloadTime ", nextUnloadTime);
	}
	
	@Override
	public void readFromNBT(NBTTagCompound tag)
	{
		super.readFromNBT(tag);
		nextUnloadTime = tag.getLong("nextUnloadTime");
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
}
