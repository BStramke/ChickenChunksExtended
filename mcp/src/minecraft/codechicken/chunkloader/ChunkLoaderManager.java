package codechicken.chunkloader;

import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Stack;

import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;

import codechicken.core.BlockCoord;
import codechicken.core.BlockCoord;
import codechicken.core.CommonUtils;
import codechicken.core.ConfigFile;
import codechicken.core.ConfigTag;
import codechicken.core.ServerUtils;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.PlayerInstance;
import net.minecraft.server.management.PlayerManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeChunkManager.OrderedLoadingCallback;
import net.minecraftforge.common.ForgeChunkManager.PlayerOrderedLoadingCallback;
import net.minecraftforge.common.ForgeChunkManager.Ticket;
import net.minecraftforge.common.ForgeChunkManager.Type;

public class ChunkLoaderManager
{
    private static class DimChunkCoord
    {
        public final int dimension;
        public final int chunkX;
        public final int chunkZ;

        public DimChunkCoord(int dim, ChunkCoordIntPair coord)
        {
            this(dim, coord.chunkXPos, coord.chunkZPos);
        }

        public DimChunkCoord(int dim, int x, int z)
        {
            dimension = dim;
            chunkX = x;
            chunkZ = z;
        }

        @Override
        public int hashCode()
        {
            return ((chunkX * 31) + chunkZ) * 31 + dimension;
        }

        @Override
        public boolean equals(Object o)
        {
            if(o instanceof DimChunkCoord)
            {
                DimChunkCoord o2 = (DimChunkCoord) o;
                return dimension == o2.dimension && chunkX == o2.chunkX && chunkZ == o2.chunkZ;
            }
            return false;
        }

        public ChunkCoordIntPair getChunkCoord()
        {
            return new ChunkCoordIntPair(chunkX, chunkZ);
        }
    }

    private static abstract class TicketManager
    {
        public HashMap<Integer, Stack<Ticket>> ticketsWithSpace = new HashMap<Integer, Stack<Ticket>>();
        public HashMap<DimChunkCoord, Ticket> heldChunks = new HashMap<DimChunkCoord, Ticket>();

        protected void addChunk(DimChunkCoord coord)
        {
            if(heldChunks.containsKey(coord))
                return;

            Stack<Ticket> freeTickets = ticketsWithSpace.get(coord.dimension);
            if(freeTickets == null)
                ticketsWithSpace.put(coord.dimension, freeTickets = new Stack<Ticket>());

            Ticket ticket;
            if(freeTickets.isEmpty())
                freeTickets.push(ticket = createTicket(coord.dimension));
            else
                ticket = freeTickets.peek();

            ForgeChunkManager.forceChunk(ticket, coord.getChunkCoord());
            heldChunks.put(coord, ticket);
            if(ticket.getChunkList().size() == ticket.getChunkListDepth() && !freeTickets.isEmpty())
                freeTickets.pop();
        }

        protected abstract Ticket createTicket(int dimension);

        protected void remChunk(DimChunkCoord coord)
        {
            Ticket ticket = heldChunks.remove(coord);
            if(ticket == null)
                return;

            ForgeChunkManager.unforceChunk(ticket, coord.getChunkCoord());

            if(ticket.getChunkList().size() == ticket.getChunkListDepth() - 1)
            {
                Stack<Ticket> freeTickets = ticketsWithSpace.get(coord.dimension);
                if(freeTickets == null)
                    ticketsWithSpace.put(coord.dimension, freeTickets = new Stack<Ticket>());
                freeTickets.push(ticket);
            }
        }
    }
    
    private static abstract class ChunkLoaderOrganiser extends TicketManager
    {
        private HashMap<Integer, HashSet<BlockCoord>> dormantLoaders = new HashMap<Integer, HashSet<BlockCoord>>();
        private HashMap<DimChunkCoord, LinkedList<IChickenChunkLoader>> forcedChunksByChunk = new HashMap<DimChunkCoord, LinkedList<IChickenChunkLoader>>();
        private HashMap<IChickenChunkLoader, HashSet<ChunkCoordIntPair>> forcedChunksByLoader = new HashMap<IChickenChunkLoader, HashSet<ChunkCoordIntPair>>();
        
        private boolean reviving;
        private boolean dormant = false;
        
        public boolean canForceNewChunks(int dimension, Collection<ChunkCoordIntPair> chunks)
        {
            if(dormant)
                return true;
            
            int required = 0;
            for(ChunkCoordIntPair coord : chunks)
            {
                LinkedList<IChickenChunkLoader> loaders = forcedChunksByChunk.get(new DimChunkCoord(dimension, coord));
                if(loaders == null || loaders.isEmpty())
                    required++;
            }
            return canForceNewChunks(required, dimension);
        }
        
        public final int numLoadedChunks()
        {
            return forcedChunksByChunk.size();
        }

        public void addChunkLoader(IChickenChunkLoader loader)
        {
            if(reviving)
                return;
            
            int dim = CommonUtils.getDimension(loader.getWorld());
            if(dormant)
            {
                HashSet<BlockCoord> coords = dormantLoaders.get(dim);
                if(coords == null)
                    dormantLoaders.put(dim, coords = new HashSet<BlockCoord>());
                coords.add(loader.getPosition());
            }
            else
            {
                forcedChunksByLoader.put(loader, new HashSet<ChunkCoordIntPair>());
                forceChunks(loader, dim, loader.getChunks());
            }
            setDirty();
        }

        public void remChunkLoader(IChickenChunkLoader loader)
        {
            if(dormant)
            {
                dormantLoaders.remove(loader.getPosition());
            }
            else
            {
                HashSet<ChunkCoordIntPair> chunks = forcedChunksByLoader.remove(loader);
                if(chunks == null)
                    return;
                unforceChunks(loader, CommonUtils.getDimension(loader.getWorld()), chunks, true);
            }
            setDirty();
        }

        private void unforceChunks(IChickenChunkLoader loader, int dim, Collection<ChunkCoordIntPair> chunks, boolean remLoader)
        {
            for(ChunkCoordIntPair coord : chunks)
            {
                DimChunkCoord dimCoord = new DimChunkCoord(dim, coord);
                LinkedList<IChickenChunkLoader> loaders = forcedChunksByChunk.get(dimCoord);
                if(loaders == null || !loaders.remove(loader))
                    continue;

                if(loaders.isEmpty())
                {
                    forcedChunksByChunk.remove(dimCoord);
                    remChunk(dimCoord);
                }
            }

            if(!remLoader)
                forcedChunksByLoader.get(loader).removeAll(chunks);
            setDirty();
        }

        private void forceChunks(IChickenChunkLoader loader, int dim, Collection<ChunkCoordIntPair> chunks)
        {
            for(ChunkCoordIntPair coord : chunks)
            {
                DimChunkCoord dimCoord = new DimChunkCoord(dim, coord);
                LinkedList<IChickenChunkLoader> loaders = forcedChunksByChunk.get(dimCoord);
                if(loaders == null)
                    forcedChunksByChunk.put(dimCoord, loaders = new LinkedList<IChickenChunkLoader>());
                if(loaders.isEmpty())
                    addChunk(dimCoord);

                if(!loaders.contains(loader))
                    loaders.add(loader);
            }

            forcedChunksByLoader.get(loader).addAll(chunks);
            setDirty();
        }
        
        public abstract boolean canForceNewChunks(int newChunks, int dim);

        public abstract void setDirty();
        
        public void updateChunkLoader(IChickenChunkLoader loader)
        {
            HashSet<ChunkCoordIntPair> loaderChunks = forcedChunksByLoader.get(loader);
            if(loaderChunks == null)
            {
                addChunkLoader(loader);
                return;
            }
            HashSet<ChunkCoordIntPair> oldChunks = new HashSet<ChunkCoordIntPair>(loaderChunks);
            HashSet<ChunkCoordIntPair> newChunks = new HashSet<ChunkCoordIntPair>();
            for(ChunkCoordIntPair chunk : loader.getChunks())
                if(!oldChunks.remove(chunk))
                    newChunks.add(chunk);

            int dim = CommonUtils.getDimension(loader.getWorld());
            if(!oldChunks.isEmpty())
                unforceChunks(loader, dim, oldChunks, false);
            if(!newChunks.isEmpty())
                forceChunks(loader, dim, newChunks);
        }

        public void save(DataOutput dataout) throws IOException
        {
            dataout.writeInt(dormantLoaders.size());
            for(Entry<Integer, HashSet<BlockCoord>> entry : dormantLoaders.entrySet())
            {
                dataout.writeInt(entry.getKey());
                HashSet<BlockCoord> coords = entry.getValue();
                dataout.writeInt(coords.size());
                for(BlockCoord coord : coords)
                {
                    dataout.writeInt(coord.x);
                    dataout.writeInt(coord.y);
                    dataout.writeInt(coord.z);
                }
            }
            dataout.writeInt(forcedChunksByLoader.size());
            for(IChickenChunkLoader loader : forcedChunksByLoader.keySet())
            {
                BlockCoord coord = loader.getPosition();
                dataout.writeInt(CommonUtils.getDimension(loader.getWorld()));
                dataout.writeInt(coord.x);
                dataout.writeInt(coord.y);
                dataout.writeInt(coord.z);
            }
        }

        public void load(DataInputStream datain) throws IOException
        {
            int dimensions = datain.readInt();
            for(int i = 0; i < dimensions; i++)
            {
                int dim = datain.readInt();
                HashSet<BlockCoord> coords = new HashSet<BlockCoord>();
                dormantLoaders.put(dim, coords);
                int numCoords = datain.readInt();
                for(int j = 0; j < numCoords; j++)
                {
                    coords.add(new BlockCoord(datain.readInt(), datain.readInt(), datain.readInt()));
                }
            }
            int numLoaders = datain.readInt();
            for(int i = 0; i < numLoaders; i++)
            {
                int dim = datain.readInt();
                HashSet<BlockCoord> coords = dormantLoaders.get(dim);
                if(coords == null)
                    dormantLoaders.put(dim, coords = new HashSet<BlockCoord>());
                coords.add(new BlockCoord(datain.readInt(), datain.readInt(), datain.readInt()));
            }
        }
        
        public void revive()
        {
            if(!dormant)
                return;
            dormant = false;
            for(int dim : dormantLoaders.keySet())
            {
                World world = getWorld(dim, reloadDimensions);
                if(world != null)
                revive(world);
            }
        }

        public void devive()
        {
            if(dormant)
                return;

            for(IChickenChunkLoader loader : new ArrayList<IChickenChunkLoader>(forcedChunksByLoader.keySet()))
            {
                int dim = CommonUtils.getDimension(loader.getWorld());
                HashSet<BlockCoord> coords = dormantLoaders.get(dim);
                if(coords == null)
                    dormantLoaders.put(dim, coords = new HashSet<BlockCoord>());
                coords.add(loader.getPosition());
                remChunkLoader(loader);
            }

            dormant = true;
        }

        public void revive(World world)
        {
            HashSet<BlockCoord> coords = dormantLoaders.get(CommonUtils.getDimension(world));
            if(coords == null)
                return;
            
            for(BlockCoord coord : coords)
            {
                reviving = true;
                TileEntity tile = world.getBlockTileEntity(coord.x, coord.y, coord.z);
                reviving = false;
                if(tile instanceof IChickenChunkLoader)
                {
                    ChunkLoaderManager.addChunkLoader((IChickenChunkLoader) tile);
                }
            }
            coords.clear();
        }

        public void setDormant()
        {
            dormant = true;
        }
    }

    private static class PlayerOrganiser extends ChunkLoaderOrganiser
    {
        private static boolean dirty;
        
        public final String username;

        public PlayerOrganiser(String username)
        {
            this.username = username;
        }

        public boolean canForceNewChunks(int required, int dim)
        {
            return required + numLoadedChunks() < getPlayerChunkLimit(username) && required < ForgeChunkManager.ticketCountAvailableFor(username) * ForgeChunkManager.getMaxChunkDepthFor("ChickenChunks");
        }

        @Override
        public Ticket createTicket(int dimension)
        {
            return ForgeChunkManager.requestPlayerTicket(ChickenChunks.instance, username, DimensionManager.getWorld(dimension), Type.NORMAL);
        }

        @Override
        public void setDirty()
        {
            dirty = true;
        }
    }
    
    private static class ModOrganiser extends ChunkLoaderOrganiser
    {
        public final Object mod;
        public final ModContainer container;
        private boolean dirty;
        
        public ModOrganiser(Object mod, ModContainer container)
        {
            this.mod = mod;
            this.container = container;
        }
        
        @Override
        public boolean canForceNewChunks(int required, int dim)
        {
            return required < ForgeChunkManager.ticketCountAvailableFor(mod, DimensionManager.getWorld(dim)) * ForgeChunkManager.getMaxChunkDepthFor(container.getModId());
        }

        @Override
        public void setDirty()
        {
            dirty = false;
        }

        @Override
        protected Ticket createTicket(int dimension)
        {
            return ForgeChunkManager.requestTicket(mod, DimensionManager.getWorld(dimension), Type.NORMAL);
        }        
    }
    
    private static class DummyLoadingCallback implements OrderedLoadingCallback, PlayerOrderedLoadingCallback
    {
        @Override
        public void ticketsLoaded(List<Ticket> tickets, World world)
        {
        }

        @Override
        public List<Ticket> ticketsLoaded(List<Ticket> tickets, World world, int maxTicketCount)
        {
            return new LinkedList<Ticket>();
        }

        @Override
        public ListMultimap<String, Ticket> playerTicketsLoaded(ListMultimap<String, Ticket> tickets, World world)
        {
            return LinkedListMultimap.create();
        }        
    }
    
    private static enum ReviveChange
    {
        PlayerRevive,
        PlayerDevive,
        ModRevive,
        DimensionRevive;
        
        public LinkedList<Object> list;
        
        public static void load()
        {
            for(ReviveChange change : values())
                change.list = new LinkedList<Object>();
        }
    }


    private static int cleanupTicks;
    private static boolean reloadDimensions = false;
    public static int maxChunks;
    private static HashMap<Object, ModContainer> mods = new HashMap<Object, ModContainer>();
    
    private static boolean loaded = false;
    private static HashMap<String, PlayerOrganiser> playerOrganisers;
    private static HashMap<Object, ModOrganiser> modOrganisers;
    private static File saveDir;

    /**
     * By doing this you are delegating all chunks from your mod to be handled by yours truly.
     */
    public static void registerMod(Object mod)
    {
        ModContainer container = Loader.instance().getModObjectList().inverse().get(mod);
        if(container == null)
            throw new NullPointerException("Mod container not found for: "+mod);
        mods.put(mod, container);
        ForgeChunkManager.setForcedChunkLoadingCallback(mod, new DummyLoadingCallback());
    }
    
    public static void loadWorld(WorldServer world)
    {        
        ReviveChange.DimensionRevive.list.add(world);
    }

    public static World getWorld(int dim, boolean create)
    {
        if(create)
            return MinecraftServer.getServer().worldServerForDimension(dim);
        else
            return DimensionManager.getWorld(dim);
    }

    public static void load(WorldServer world)
    {
        if(loaded)
            return;
        
        loaded = true;
        
        playerOrganisers = new HashMap<String, PlayerOrganiser>();
        modOrganisers = new HashMap<Object, ChunkLoaderManager.ModOrganiser>();
        ReviveChange.load();
        
        try
        {
            saveDir = new File(CommonUtils.getWorldBaseSaveLocation(world), "chickenchunks");
            if(!saveDir.exists())
                saveDir.mkdirs();
            loadPlayerChunks();
            loadModChunks();
        }
        catch(Exception e)
        {
            FMLCommonHandler.instance().raiseException(e, "Error while loading chunk info", true);
        }
    }

    private static void loadModChunks() throws IOException
    {
        for(Entry<Object, ModContainer> entry : mods.entrySet())
        {
            File saveFile = new File(saveDir, entry.getValue().getModId()+".dat");
            if(!saveFile.exists())
                return;

            DataInputStream datain = new DataInputStream(new FileInputStream(saveFile));
            ModOrganiser organiser = getModOrganiser(entry.getKey());
            ReviveChange.ModRevive.list.add(organiser);

            organiser.load(datain);
            datain.close();
        }
    }

    private static void loadPlayerChunks() throws IOException
    {
        File saveFile = new File(saveDir, "players.dat");
        if(!saveFile.exists())
            return;

        DataInputStream datain = new DataInputStream(new FileInputStream(saveFile));
        int organisers = datain.readInt();
        for(int i = 0; i < organisers; i++)
        {
            String username = datain.readUTF();
            PlayerOrganiser organiser = getPlayerOrganiser(username);
            organiser.setDormant();
            if(allowOffline(username))
                ReviveChange.PlayerRevive.list.add(organiser);

            organiser.load(datain);
        }
        datain.close();
    }

    public static int getPlayerChunkLimit(String username)
    {
        ConfigTag config = ChickenChunks.config.getTag("players");
        if(config.containsTag(username))
        {
            int ret = config.getTag(username).getIntValue(0);
            if(ret != 0)
                return ret;
        }

        if(ServerUtils.isPlayerOP(username))
        {
            int ret = config.getTag("OP").getIntValue(0);
            if(ret != 0)
                return ret;
        }

        return config.getTag("DEFAULT").getIntValue(5000);
    }

    public static boolean allowOffline(String username)
    {
        ConfigTag config = ChickenChunks.config.getTag("allowoffline");
        if(config.containsTag(username))
            return config.getTag(username).getBooleanValue(true);

        if(ServerUtils.isPlayerOP(username))
            return config.getTag("OP").getBooleanValue(true);

        return config.getTag("DEFAULT").getBooleanValue(true);
    }

    public static boolean allowChunkViewer(String username)
    {
        ConfigTag config = ChickenChunks.config.getTag("allowchunkviewer");
        if(config.containsTag(username))
            return config.getTag(username).getBooleanValue(true);

        if(ServerUtils.isPlayerOP(username))
            return config.getTag("OP").getBooleanValue(true);

        return config.getTag("DEFAULT").getBooleanValue(true);
    }

    public static void initConfig(ConfigFile config)
    {
        config.getTag("players").setPosition(0).useBraces().setComment("Per player chunk limiting. Values ignored if 0.:Simply add <username>=<value>");
        config.getTag("players.DEFAULT").setComment("Forge gives everyone 12500 by default").getIntValue(5000);
        config.getTag("players.OP").setComment("For server op's only.").getIntValue(5000);
        config.getTag("allowoffline").setPosition(1).useBraces().setComment("If set to false, players will have to be logged in for their chunkloaders to work.:Simply add <username>=<true|false>");
        config.getTag("allowoffline.DEFAULT").getBooleanValue(true);
        config.getTag("allowoffline.OP").getBooleanValue(true);
        config.getTag("allowchunkviewer").setPosition(2).useBraces().setComment("Set to false to deny a player access to the chunk viewer");
        config.getTag("allowchunkviewer.DEFAULT").getBooleanValue(true);
        config.getTag("allowchunkviewer.OP").getBooleanValue(true);
        cleanupTicks = config.getTag("cleanuptime").setComment("The number of ticks to wait between attempting to unload orphaned chunks").getIntValue(1200);
        reloadDimensions = config.getTag("reload-dimensions").setComment("Set to false to disable the automatic reloading of mystcraft dimensions on server restart").getBooleanValue(true);
        maxChunks = config.getTag("maxchunks").setComment("The maximum number of chunks per chunkloader").getIntValue(400);
    }

    public static void addChunkLoader(IChickenChunkLoader loader)
    {
        int dim = CommonUtils.getDimension(loader.getWorld());
        ChunkLoaderOrganiser organiser = getOrganiser(loader);
        if(organiser.canForceNewChunks(dim, loader.getChunks()))
            organiser.addChunkLoader(loader);
        else
            loader.deactivate();
    }

    private static ChunkLoaderOrganiser getOrganiser(IChickenChunkLoader loader)
    {
        String owner = loader.getOwner();
        return owner == null ? getModOrganiser(loader.getMod()) : getPlayerOrganiser(owner);
    }

    public static void remChunkLoader(IChickenChunkLoader loader)
    {
        getOrganiser(loader).remChunkLoader(loader);
    }

    public static void updateLoader(IChickenChunkLoader loader)
    {
        getOrganiser(loader).updateChunkLoader(loader);
    }

    public static boolean canLoaderAdd(IChickenChunkLoader loader, Collection<ChunkCoordIntPair> chunks)
    {
        String owner = loader.getOwner();
        int dim = CommonUtils.getDimension(loader.getWorld());
        if(owner != null)
        {
            return getPlayerOrganiser(owner).canForceNewChunks(dim, chunks);
        }
        else
        {
            return false;
        }
    }

    private static PlayerOrganiser getPlayerOrganiser(String username)
    {
        PlayerOrganiser organiser = playerOrganisers.get(username);
        if(organiser == null)
            playerOrganisers.put(username, organiser = new PlayerOrganiser(username));
        return organiser;
    }

    private static ModOrganiser getModOrganiser(Object mod)
    {
        ModOrganiser organiser = modOrganisers.get(mod);
        if(organiser == null)
        {
            ModContainer container = mods.get(mod);
            if(container == null)
                throw new NullPointerException("Mod not registered with chickenchunks: "+mod);
            modOrganisers.put(mod, organiser = new ModOrganiser(mod, container));
        }
        return organiser;
    }

    public static void serverShutdown()
    {
        loaded = false;
    }

    public static void save(WorldServer world)
    {
        try
        {
            if(PlayerOrganiser.dirty)
            {
                File saveFile = new File(saveDir, "players.dat");
                if(!saveFile.exists())
                    saveFile.createNewFile();
                DataOutputStream dataout = new DataOutputStream(new FileOutputStream(saveFile));
                dataout.writeInt(playerOrganisers.size());
                for(PlayerOrganiser organiser : playerOrganisers.values())
                {
                    dataout.writeUTF(organiser.username);
                    organiser.save(dataout);
                }
                dataout.close();
                PlayerOrganiser.dirty = false;
            }

            
            for(ModOrganiser organiser : modOrganisers.values())
            {
                if(organiser.dirty)
                {
                    File saveFile = new File(saveDir, organiser.container.getModId()+".dat");
                    if(!saveFile.exists())
                        return;

                    DataOutputStream dataout = new DataOutputStream(new FileOutputStream(saveFile));
                    organiser.save(dataout);
                    dataout.close();
                    organiser.dirty = false;
                }
            }
        }
        catch(Exception e)
        {
            FMLCommonHandler.instance().raiseException(e, "Error while saving chunk info", true);
        }
    }

    @SuppressWarnings("unchecked")
    public static void cleanChunks(WorldServer world)
    {
        int dim = CommonUtils.getDimension(world);
        int viewdist = ServerUtils.mc().getConfigurationManager().getViewDistance();

        HashSet<ChunkCoordIntPair> loadedChunks = new HashSet<ChunkCoordIntPair>();
        for(EntityPlayer player : ServerUtils.getPlayersInDimension(dim))
        {
            int playerChunkX = (int) player.posX >> 4;
            int playerChunkZ = (int) player.posZ >> 4;

            for(int cx = playerChunkX - viewdist; cx <= playerChunkX + viewdist; cx++)
                for(int cz = playerChunkZ - viewdist; cz <= playerChunkZ + viewdist; cz++)
                    loadedChunks.add(new ChunkCoordIntPair(cx, cz));
        }

        ImmutableSetMultimap<ChunkCoordIntPair, Ticket> persistantChunks = world.getPersistentChunks();
        PlayerManager manager = world.getPlayerManager();

        for(Chunk chunk : (List<Chunk>) world.theChunkProviderServer.loadedChunks)
        {
            ChunkCoordIntPair coord = chunk.getChunkCoordIntPair();
            if(!loadedChunks.contains(coord) && !persistantChunks.containsKey(coord) && world.theChunkProviderServer.chunkExists(coord.chunkXPos, coord.chunkZPos))
            {
                PlayerInstance instance = manager.getOrCreateChunkWatcher(coord.chunkXPos, coord.chunkZPos, false);
                if(instance == null)
                {
                    world.theChunkProviderServer.unloadChunksIfNotNearSpawn(coord.chunkXPos, coord.chunkZPos);
                }
                else
                {
                    while(instance.playersInChunk.size() > 0)
                    {
                        instance.sendThisChunkToPlayer((EntityPlayerMP) instance.playersInChunk.get(0));
                    }
                }
            }
        }    
        
        if(ServerUtils.getPlayersInDimension(dim).isEmpty() && world.getPersistentChunks().isEmpty() && !DimensionManager.shouldLoadSpawn(dim))
        {
            DimensionManager.unloadWorld(dim);
        }
    }

    public static void tickEnd(WorldServer world)
    {
        if(world.getWorldTime() % cleanupTicks == 0)
            cleanChunks(world);

        revivePlayerLoaders();
    }

    private static void revivePlayerLoaders()
    {
        for(Object organiser : ReviveChange.PlayerRevive.list)
            ((PlayerOrganiser)organiser).revive();
        ReviveChange.PlayerRevive.list.clear();
        
        for(Object organiser : ReviveChange.ModRevive.list)
            ((ModOrganiser)organiser).revive();
        ReviveChange.ModRevive.list.clear();
        
        for(Object world : ReviveChange.DimensionRevive.list)
            for(PlayerOrganiser organiser : playerOrganisers.values())
                organiser.revive((World) world);
        ReviveChange.DimensionRevive.list.clear();        

        for(Object organiser : ReviveChange.PlayerDevive.list)
            ((PlayerOrganiser)organiser).devive();
        ReviveChange.PlayerDevive.list.clear();
    }

    public static void playerLogin(String username)
    {
        ReviveChange.PlayerRevive.list.add(getPlayerOrganiser(username));
    }

    public static void playerLogout(String username)
    {
        if(!allowOffline(username))
            ReviveChange.PlayerDevive.list.add(getPlayerOrganiser(username));
    }
}
