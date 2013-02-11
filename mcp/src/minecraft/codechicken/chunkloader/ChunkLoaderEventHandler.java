package codechicken.chunkloader;

import java.util.EnumSet;

import codechicken.chunkloader.PlayerChunkViewerManager.DimensionChange;
import codechicken.chunkloader.PlayerChunkViewerManager.TicketChange;
import codechicken.core.ServerUtils;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeChunkManager.ForceChunkEvent;
import net.minecraftforge.common.ForgeChunkManager.UnforceChunkEvent;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.event.world.WorldEvent.Load;
import net.minecraftforge.event.world.WorldEvent.Save;
import net.minecraftforge.event.world.WorldEvent.Unload;

import cpw.mods.fml.common.IPlayerTracker;
import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;

public class ChunkLoaderEventHandler implements ITickHandler, IPlayerTracker
{
    @Override
    public void tickStart(EnumSet<TickType> type, Object... tickData)
    {
    }

    @Override
    public void tickEnd(EnumSet<TickType> type, Object... tickData)
    {
        if(type.contains(TickType.SERVER))
        {
            PlayerChunkViewerManager.instance().update();
        }
        if(type.contains(TickType.WORLD))
        {
            ChunkLoaderManager.tickEnd((WorldServer) tickData[0]);
            PlayerChunkViewerManager.instance().calculateChunkChanges((WorldServer) tickData[0]);
        }
    }

    @Override
    public EnumSet<TickType> ticks()
    {
        return EnumSet.of(TickType.PLAYER, TickType.SERVER, TickType.WORLD);
    }

    @Override
    public String getLabel()
    {
        return "ChickenChunks";
    }

    @Override
    public void onPlayerLogin(EntityPlayer player)
    {
        ChunkLoaderManager.playerLogin(player.username);
    }

    @Override
    public void onPlayerLogout(EntityPlayer player)
    {
        PlayerChunkViewerManager.instance().logouts.add(player.username);
        ChunkLoaderManager.playerLogout(player.username);
    }

    @Override
    public void onPlayerChangedDimension(EntityPlayer player)
    {
    }

    @Override
    public void onPlayerRespawn(EntityPlayer player)
    {
    }

    @ForgeSubscribe
    public void onChunkDataLoad(ChunkDataEvent.Load event)
    {
        ChunkLoaderManager.load((WorldServer) event.world);
    }
    
    @ForgeSubscribe
    public void onWorldLoad(Load event)
    {
        if(!event.world.isRemote)
        {
            ChunkLoaderManager.load((WorldServer) event.world);
            ChunkLoaderManager.loadWorld((WorldServer) event.world);
            PlayerChunkViewerManager.instance().dimChanges.add(new DimensionChange((WorldServer) event.world, true));
        }
    }

    @ForgeSubscribe
    public void onWorldUnload(Unload event)
    {
        if(!event.world.isRemote)
        {
            if(ServerUtils.mc().isServerRunning())
            {
                PlayerChunkViewerManager.instance().dimChanges.add(new DimensionChange((WorldServer) event.world, false));
            }
            else
            {
                PlayerChunkViewerManager.serverShutdown();
                ChunkLoaderManager.serverShutdown();
            }
        }
    }

    @ForgeSubscribe
    public void onWorldSave(Save event)
    {
        ChunkLoaderManager.save((WorldServer) event.world);
    }

    @ForgeSubscribe
    public void onChunkForce(ForceChunkEvent event)
    {
        PlayerChunkViewerManager.instance().ticketChanges.add(new TicketChange(event.ticket, event.location, true));
    }

    @ForgeSubscribe
    public void onChunkUnForce(UnforceChunkEvent event)
    {
        PlayerChunkViewerManager.instance().ticketChanges.add(new TicketChange(event.ticket, event.location, false));
    }
}
