package codechicken.chunkloader;

import java.io.File;

import codechicken.core.ConfigFile;
import codechicken.packager.Packager;
import codechicken.packager.SrcPackager;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.PreInit;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.Mod.Init;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.Mod.ServerStarting;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

@SrcPackager(getMappedDirectories = {"ChickenChunks"}, getName = "ChickenChunks")
@Packager(
		getBaseDirectories = {"ChickenChunks"}, 
		getName = "ChickenChunks")
@Mod(name="ChickenChunks", version="1.2.1.1", useMetadata = false, modid = "ChickenChunks", dependencies = "required-after:Forge@[6.4.1.413,)", acceptedMinecraftVersions = "[1.4.5,)")
public class ChickenChunks
{
	@SidedProxy(clientSide="codechicken.chunkloader.ChunkLoaderClientProxy", serverSide="codechicken.chunkloader.ChunkLoaderProxy")
	public static ChunkLoaderProxy proxy;
	
	public static ConfigFile config;

	public static BlockChunkLoader blockChunkLoader;

	@Instance(value="ChickenChunks")
	public static ChickenChunks instance;
	
	@PreInit
	public void preInit(FMLPreInitializationEvent event)
	{
		config = new ConfigFile(new File(event.getModConfigurationDirectory(), "ChickenChunks.cfg"))
		.setComment("ChunkLoader Configuration File:Deleting any element will restore it to it's default value:Block ID's will be automatically generated the first time it's run");
	}
	
	@Init
	public void initialize(FMLInitializationEvent event)
	{
		proxy.load();
	}

	@ServerStarting
	public void serverStarting(FMLServerStartingEvent event) 
	{
		proxy.registerCommands(event);
	}
}
