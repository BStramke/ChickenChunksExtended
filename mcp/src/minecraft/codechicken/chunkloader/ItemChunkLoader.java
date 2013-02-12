package codechicken.chunkloader;

import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

public class ItemChunkLoader extends ItemBlock
{
    public ItemChunkLoader(int par1)
    {
        super(par1);
        setHasSubtypes(true);
    }
    
    @Override
    public int getMetadata(int par1)
    {
        return par1;
    }
    
    @Override
    public String getItemNameIS(ItemStack stack)
    {
        switch(stack.getItemDamage())
        {
            case 0:
                return "chickenChunkLoader";
            case 1:
                return "chickenSpotLoader";
            case 2:
                return "PersonalChunkLoader";
        }
        return null;
    }
}
