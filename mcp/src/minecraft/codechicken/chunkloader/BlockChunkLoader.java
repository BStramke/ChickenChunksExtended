package codechicken.chunkloader;

import java.util.List;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import net.minecraft.block.BlockContainer;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.IBlockAccess;
import net.minecraft.item.ItemStack;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;

public class BlockChunkLoader extends BlockContainer
{
	public BlockChunkLoader(int ID)
	{
		super(ID, Material.rock);
		setHardness(20F);
		setResistance(100F);
		setStepSound(soundStoneFootstep);
		blockIndexInTexture = 0;
	}
	
	@Override
	public boolean isBlockNormalCube(World world, int x, int y, int z)
	{
	    return false;
	}
	
	@Override
	public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z)
	{
	    setBlockBoundsForItemRender(world.getBlockMetadata(x, y, z));
	}
	
	@Override
	public boolean isBlockSolidOnSide(World world, int x, int y, int z, ForgeDirection side)
	{
	    if(world.getBlockMetadata(x, y, z) == 1)
	        return false;
	    
	    return side == ForgeDirection.DOWN;
	}
	
	@Override
	public boolean canConnectRedstone(IBlockAccess world, int x, int y, int z, int side)
	{
	    return true;
	}

    public void setBlockBoundsForItemRender(int metadata)
    {
        switch(metadata)
        {
            case 0:
            case 2:	
                setBlockBounds(0, 0, 0, 1, 0.75F, 1);
                break;
            case 1:
                setBlockBounds(0.25F, 0, 0.25F, 0.75F, 0.4375F, 0.75F);
                break;
        }
    }
	
    @Override
    public int getBlockTextureFromSideAndMetadata(int side, int meta)
    {
    	if(meta==2)
    		return (side > 2 ? 2 : side); //treat it as if its meta=0
    	else
    		return (side > 2 ? 2 : side) + meta*3;
    }
	
	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int par6, float par7, float par8, float par9)
	{
        int meta = world.getBlockMetadata(x, y, z);
               
		if(meta == 1 || player.isSneaking())
		    return false;
		
		
		TileChunkLoader tile = (TileChunkLoader) world.getBlockTileEntity(x, y, z);
	    if(world.isRemote)
	    {
			if(tile.owner == null || tile.owner.equals(player.username))
				ChickenChunks.proxy.openGui(tile);
			else
				player.addChatMessage("This Chunkloader does not belong to you.");
	    }
	    else
	    {
	    	if(tile instanceof TilePersonalChunkLoader && (tile.owner == null || tile.owner.equals(player.username))) 
	    		((TilePersonalChunkLoader) tile).nextUnloadTime =  System.currentTimeMillis() + ChickenChunks.LoaderActiveTime;
	    }
        return true;
	}
	
	@Override
	public void onBlockPlacedBy(World world, int i, int j, int k, EntityLiving entityliving)
	{
		if(world.isRemote)
			return;
		
		TileChunkLoaderBase ctile = (TileChunkLoaderBase)world.getBlockTileEntity(i, j, k);
		ctile.onBlockPlacedBy(entityliving);
	}
	
	@Override
	public TileEntity createNewTileEntity(World world, int meta)
	{
		if(meta == 0)
			return new TileChunkLoader();
		else if(meta == 1)
		    return new TileSpotLoader();
		else if(meta == 2)
			return new TilePersonalChunkLoader();
		else
			return null;
	}
	
	@Override
	public TileEntity createNewTileEntity(World world)
	{
		return null;
	}
	
	@Override
	public boolean isOpaqueCube()
	{
		return false;
	}
	
	@Override
	public String getTextureFile()
	{
		return "/codechicken/chunkloader/block.png";
	}
	
	@SuppressWarnings({"unchecked", "rawtypes"})
    @Override
	public void getSubBlocks(int par1, CreativeTabs par2CreativeTabs, List list)
	{
        list.add(new ItemStack(this, 1, 0));
        list.add(new ItemStack(this, 1, 1));
        list.add(new ItemStack(this, 1, 2));
	}
	
	@Override
    public int damageDropped(int par1)
    {
        return par1;
    }
	
	@Override
	@SideOnly(Side.CLIENT)
	public int getRenderType()
	{
	    return ChunkLoaderSBRH.renderID;
	}
}
