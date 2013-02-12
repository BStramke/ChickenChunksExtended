package codechicken.chunkloader;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

import org.lwjgl.opengl.GL11;

import codechicken.chunkloader.TileChunkLoader;

public class GuiChunkLoader extends GuiScreen
{
	public GuiButton laserButton;
	public GuiButton shapeButton;
	public TileChunkLoader tile;
	private boolean isPersonalChunkLoader = false;
	
	public GuiChunkLoader(TileChunkLoader tile)
	{
		this.tile = tile;
		if(tile instanceof TilePersonalChunkLoader)
			this.isPersonalChunkLoader = true;
	}

	@SuppressWarnings("unchecked")
	public void initGui()
	{
		controlList.clear();
		
		controlList.add(new GuiButton(1, width / 2 - 20, height / 2 - 45, 20, 20, "+"));
	 	controlList.add(new GuiButton(2, width / 2 - 80, height / 2 - 45, 20, 20, "-"));
	 	controlList.add(laserButton = new GuiButton(3, width / 2 + 7, height / 2 - 60, 75, 20, "-"));
	 	controlList.add(shapeButton = new GuiButton(4, width / 2 + 7, height / 2 - 37, 75, 20, "-"));
		laserButton.displayString = tile.renderInfo.showLasers ? "Hide Lasers" : "Show Lasers";
		shapeButton.displayString = tile.shape.name;
		
		super.initGui();
	}

	public void updateScreen()
	{
		if(mc.theWorld.getBlockTileEntity(tile.xCoord, tile.yCoord, tile.zCoord) != tile)//tile changed
		{
			mc.currentScreen = null;
    		mc.setIngameFocus();
		}
		laserButton.displayString = tile.renderInfo.showLasers ? "Hide Lasers" : "Show Lasers";
		shapeButton.displayString = tile.shape.name;
		super.updateScreen();
	}
	
	public void drawScreen(int i, int j, float f)
	{
		drawDefaultBackground();		
		drawContainerBackground();
		
		super.drawScreen(i, j, f);//buttons
		
		GL11.glDisable(2896 /* GL_LIGHTING */);
		GL11.glDisable(2929 /* GL_DEPTH_TEST */);
		
		String name = "Chunk Loader";
		fontRenderer.drawString(name, width / 2 - fontRenderer.getStringWidth(name) / 2 - 40, height / 2 - 74, 0x303030);
		if(tile.owner != null)
			fontRenderer.drawString(tile.owner, width / 2 - fontRenderer.getStringWidth(tile.owner) / 2 + 44, height / 2 - 72, 0x801080);
		fontRenderer.drawString("Radius", width / 2 - fontRenderer.getStringWidth("Radius") / 2 - 40,  height / 2 - 57, 0x404040);
		
		String sradius = ""+tile.radius;

		fontRenderer.drawString(sradius, width / 2 - fontRenderer.getStringWidth(sradius) / 2 - 40, height / 2 - 39, 0xFFFFFF);
		
		int chunks = tile.countLoadedChunks();
		sradius = chunks+(chunks == 1 ? " Chunk" : " Chunks");
		fontRenderer.drawString(sradius, width / 2 - fontRenderer.getStringWidth(sradius) / 2-39, height / 2 - 21, 0x108000);
		
		//TODO: sradius = "Total "+ChunkLoaderManager.activeChunkLoaders+"/"+ChunkLoaderManager.allowedChunkloaders+" Chunks";
		//fontRenderer.drawString(sradius, width / 2 - fontRenderer.getStringWidth(sradius) / 2, height / 2 - 8, 0x108000);

		GL11.glEnable(2896 /* GL_LIGHTING */);
		GL11.glEnable(2929 /* GL_DEPTH_TEST */);
	}
	
	int button;
	@Override
	protected void mouseClicked(int par1, int par2, int par3)
	{
	    button = par3;
	    if(par3 == 1)
	        par3 = 0;
	    super.mouseClicked(par1, par2, par3);
	}
	
	protected void actionPerformed(GuiButton guibutton)
	{
		if(guibutton.id == 1)
			ChunkLoaderClientPacketHandler.sendShapeChange(tile, tile.shape, tile.radius+1);
		if(guibutton.id == 2 && tile.radius > 1)
			ChunkLoaderClientPacketHandler.sendShapeChange(tile, tile.shape, tile.radius-1);
		if(guibutton.id == 3)
			tile.renderInfo.showLasers = !tile.renderInfo.showLasers;
		if(guibutton.id == 4)
			ChunkLoaderClientPacketHandler.sendShapeChange(tile, button == 1 ? tile.shape.prev() : tile.shape.next(), tile.radius);
	}

	private void drawContainerBackground()
	{
		int i = mc.renderEngine.getTexture("/codechicken/chunkloader/guiSmall.png");
	 	GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
	 	mc.renderEngine.bindTexture(i);
	 	int posx = width / 2 - 88;
		int posy = height / 2 - 83;
		drawTexturedModalRect(posx, posy, 0, 0, 176, 166);
	}
	
	public boolean doesGuiPauseGame()
	{
		return false;
	}
}
