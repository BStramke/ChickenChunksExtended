package codechicken.chunkloader;

import java.util.Collection;

import codechicken.core.BlockCoord;

import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;

public abstract interface IChickenChunkLoader
{
	public String getOwner();
	public Object getMod();
	public World getWorld();
	public BlockCoord getPosition();
	public void deactivate();
    public Collection<ChunkCoordIntPair> getChunks();
}
