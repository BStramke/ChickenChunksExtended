package codechicken.chunkloader;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.MinecraftForgeClient;
import codechicken.core.PacketCustom;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.client.registry.RenderingRegistry;

public class ChunkLoaderClientProxy  extends ChunkLoaderProxy
{
	@Override
	public void load()
	{
		super.load();
        
        PacketCustom.assignHandler(ChunkLoaderClientPacketHandler.channel, 0, 255, new ChunkLoaderClientPacketHandler());
        
        MinecraftForgeClient.preloadTexture("/codechicken/chunkloader/block.png");
        ClientRegistry.bindTileEntitySpecialRenderer(TileChunkLoader.class, new TileChunkLoaderRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(TileSpotLoader.class, new TileChunkLoaderRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(TilePersonalChunkLoader.class, new TileChunkLoaderRenderer());
        RenderingRegistry.registerBlockHandler(new ChunkLoaderSBRH());
		
	}
	
	@Override
	public void openGui(TileChunkLoader tile)
	{
		Minecraft.getMinecraft().displayGuiScreen(new GuiChunkLoader(tile));
	}
}
