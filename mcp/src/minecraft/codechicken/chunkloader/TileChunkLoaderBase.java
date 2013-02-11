package codechicken.chunkloader;

import codechicken.chunkloader.TileChunkLoaderRenderer.RenderInfo;
import codechicken.core.BlockCoord;
import codechicken.core.BlockCoord;
import net.minecraft.block.Block;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public abstract class TileChunkLoaderBase extends TileEntity implements IChickenChunkLoader
{    
    public void writeToNBT(NBTTagCompound tag)
    {
        super.writeToNBT(tag);
        tag.setBoolean("powered", powered);
        if(owner != null)
            tag.setString("owner", owner);
    }
    
    @Override
    public void readFromNBT(NBTTagCompound tag)
    {
        super.readFromNBT(tag);
        if(tag.hasKey("owner"))
            owner = tag.getString("owner");
        if(tag.hasKey("powered"))
            powered = tag.getBoolean("powered");
        loaded = true;
    }
    
    public void validate()
    {
        super.validate();
        if(!worldObj.isRemote && loaded && !powered)
            activate();
        
        if(worldObj.isRemote)
            renderInfo = new RenderInfo();
    }
    
    public boolean isPowered()
    {
        return isPoweringTo(worldObj, xCoord, yCoord+1, zCoord, 0) ||
                isPoweringTo(worldObj, xCoord, yCoord-1, zCoord, 1) ||
                isPoweringTo(worldObj, xCoord, yCoord, zCoord+1, 2) ||
                isPoweringTo(worldObj, xCoord, yCoord, zCoord-1, 3) ||
                isPoweringTo(worldObj, xCoord+1, yCoord, zCoord, 4) ||
                isPoweringTo(worldObj, xCoord-1, yCoord, zCoord, 5);
    }
    
    public static boolean isPoweringTo(World world, int x, int y, int z, int side)
    {
        int var5 = world.getBlockId(x, y, z);
        return var5 == 0 ? false : Block.blocksList[var5].isProvidingWeakPower(world, x, y, z, side);
    }

    public void invalidate()
    {
        super.invalidate();
        if(!worldObj.isRemote)
            deactivate();
    }

    public void destroyBlock()
    {
        ChickenChunks.blockChunkLoader.dropBlockAsItem(worldObj, xCoord, yCoord, zCoord, 0, 0);
        worldObj.setBlockWithNotify(xCoord, yCoord, zCoord, 0);
    }
    
    public ChunkCoordIntPair getChunkPosition()
    {
        return new ChunkCoordIntPair(xCoord>>4, zCoord>>4);
    }

    public void onBlockPlacedBy(EntityLiving entityliving)
    {
        if(entityliving instanceof EntityPlayer)
            owner = ((EntityPlayer)entityliving).getEntityName();
        if(owner.equals(""))
            owner = null;
        activate();
    }
    
    @Override
    public String getOwner()
    {
        return owner;
    }

    @Override
    public Object getMod()
    {
        return ChickenChunks.instance;
    }

    @Override
    public World getWorld()
    {
        return worldObj;
    }
    
    @Override
    public BlockCoord getPosition()
    {
        return new BlockCoord(this);
    }
    
    @Override
    public void deactivate()
    {
        ChunkLoaderManager.remChunkLoader(this);
        loaded = true;
        active = false;
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }
    
    public void activate()
    {
        ChunkLoaderManager.addChunkLoader(this);
        loaded = true;
        active = true;
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }
    
    @Override
    public void updateEntity()
    {
        if(!worldObj.isRemote)
        {
            boolean nowPowered = isPowered();
            if(powered != nowPowered)
            {
                powered = nowPowered;
                if(powered)
                    deactivate();
                else
                    activate();
            }
        }
        else
        {
            renderInfo.update(this);
        }
    }
    
    public String owner;
    private boolean loaded = false;
    protected boolean powered = false;
    public RenderInfo renderInfo;
    public boolean active = false; 

}
