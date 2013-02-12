package codechicken.chunkloader;

import net.minecraft.block.Block;
import net.minecraft.command.CommandHandler;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import codechicken.core.CommonUtils;
import codechicken.core.PacketCustom;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.LanguageRegistry;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.relauncher.Side;

public class ChunkLoaderProxy
{
	public void load()
	{
		ChickenChunks.blockChunkLoader = new BlockChunkLoader(ChickenChunks.config.getTag("block.id").getIntValue(CommonUtils.getFreeBlockID(243)));
		ChickenChunks.blockChunkLoader.setBlockName("chickenChunkLoader");
		GameRegistry.registerBlock(ChickenChunks.blockChunkLoader, ItemChunkLoader.class, "chickenChunkLoader");
        LanguageRegistry.addName(new ItemStack(ChickenChunks.blockChunkLoader, 1, 0), "Chunk Loader");
        LanguageRegistry.addName(new ItemStack(ChickenChunks.blockChunkLoader, 1, 1), "Spot Loader");
        LanguageRegistry.addName(new ItemStack(ChickenChunks.blockChunkLoader, 1, 2), "Personal Chunk Loader");
        
        GameRegistry.registerTileEntity(TilePersonalChunkLoader.class, "PersonalChunkLoader");
        GameRegistry.registerTileEntity(TileChunkLoader.class, "ChickenChunkLoader");
        GameRegistry.registerTileEntity(TileSpotLoader.class, "ChickenSpotLoader");
		
        PacketCustom.assignHandler(ChunkLoaderServerPacketHandler.channel, 0, 255, new ChunkLoaderServerPacketHandler());        
		ChunkLoaderManager.initConfig(ChickenChunks.config);
        
        MinecraftForge.EVENT_BUS.register(new ChunkLoaderEventHandler());
		TickRegistry.registerTickHandler(new ChunkLoaderEventHandler(), Side.SERVER);
		GameRegistry.registerPlayerTracker(new ChunkLoaderEventHandler());
		ChunkLoaderManager.registerMod(ChickenChunks.instance);
        
        GameRegistry.addRecipe(new ItemStack(ChickenChunks.blockChunkLoader, 1, 0), 
        	" p ",
        	"ggg",
        	"gEg",
        	'p', Item.enderPearl,
        	'g', Item.ingotGold,
        	'd', Item.diamond,
        	'E', Block.enchantmentTable     	
        );
        
        GameRegistry.addRecipe(new ItemStack(ChickenChunks.blockChunkLoader, 10, 1), 
                "ppp",
                "pcp",
                "ppp",
                'p', Item.enderPearl,
                'c', new ItemStack(ChickenChunks.blockChunkLoader, 1, 0)
        );
	}
	
	public void registerCommands(FMLServerStartingEvent event)
	{
		CommandHandler commandManager = (CommandHandler)event.getServer().getCommandManager();
		commandManager.registerCommand(new CommandChunkLoaders());
		commandManager.registerCommand(new CommandDebugInfo());
	}

	public void openGui(TileChunkLoader tile)
	{
	}
}
