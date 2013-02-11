package codechicken.chunkloader;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.Collection;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraftforge.client.MinecraftForgeClient;

import org.lwjgl.opengl.GL11;

import codechicken.core.Quat;
import codechicken.core.Vector3;

public class TileChunkLoaderRenderer extends TileEntitySpecialRenderer
{
    public static class RenderInfo
    {
        int activationCounter;
        boolean showLasers;
        
        public void update(TileChunkLoaderBase chunkLoader)
        {
            if(activationCounter < 20 && chunkLoader.active)
                activationCounter++;
            else if(activationCounter > 0 && !chunkLoader.active)
                activationCounter--;
        }
    }
    
	static
	{
		MinecraftForgeClient.preloadTexture("/codechicken/enderstorage/hedronmap.png");
	}
	
	@Override
	public void renderTileEntityAt(TileEntity tile, double d, double d1, double d2, float f)
	{
        OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GL11.glDisable(3553 /*GL_TEXTURE_2D*/);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GL11.glEnable(32826 /*GL_RESCALE_NORMAL_EXT*/);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		
        double rot = (tile.worldObj.getWorldTime() + f)*2;
        double height;
        double size;
        float updown = ((tile.worldObj.getWorldTime() + f)%50) / 25F;
        
        updown = (float) Math.sin(updown*3.141593);
        updown *= 0.2;
        
        TileChunkLoaderBase chunkLoader = (TileChunkLoaderBase)tile;
        if(chunkLoader instanceof TileChunkLoader)
        {
            TileChunkLoader ctile = (TileChunkLoader)chunkLoader;
            rot /= Math.pow(ctile.radius, 0.2);
            height = 0.9;
            size = 0.08;
        }
        else if(chunkLoader instanceof TileSpotLoader)
        {
            height = 0.5;
            size = 0.05;
        }
        else
            return;
        
        RenderInfo renderInfo = chunkLoader.renderInfo;
        double active = (renderInfo.activationCounter)/20D;
        if(chunkLoader.active && renderInfo.activationCounter < 20)
            active += f/20D;
        else if(!chunkLoader.active && renderInfo.activationCounter > 0)
            active -= f/20D;
        
        if(renderInfo.showLasers)
        {	        
	        GL11.glDisable(GL11.GL_TEXTURE_2D);
	        GL11.glDisable(GL11.GL_LIGHTING);
	        GL11.glDisable(GL11.GL_FOG);
	        drawRays(d, d1, d2, rot, updown, tile.xCoord, tile.yCoord, tile.zCoord, chunkLoader.getChunks());
	        GL11.glEnable(GL11.GL_TEXTURE_2D);
	        GL11.glEnable(GL11.GL_LIGHTING);
	        GL11.glEnable(GL11.GL_FOG);
		}
		rot = (tile.worldObj.getWorldTime() + f)*active / 3F;
    	Quat pearlrot = Quat.aroundAxis(0, 0, 1, Math.atan(1/phi));
    	pearlrot.multiply(Quat.aroundAxis(0, 1, 0, rot));
        renderIcosahedron(new Vector3(d, d1, d2), 1, new Vector3(0.5, height+(updown + 0.3)*active, 0.5), pearlrot, size, 0);
	}	
	
	public Point2D.Double findIntersection(Line2D line1, Line2D line2)
	{
		// calculate differences  
		double xD1 = line1.getX2() - line1.getX1();
		double yD1 = line1.getY2() - line1.getY1();
		double xD2 = line2.getX2() - line2.getX1();
		double yD2 = line2.getY2() - line2.getY1();

		double xD3 = line1.getX1() - line2.getX1();
		double yD3 = line1.getY1() - line2.getY1();

		// find intersection Pt between two lines    
		Point2D.Double pt = new Point2D.Double(0, 0);
		double div = yD2 * xD1 - xD2 * yD1;
		if(div == 0)//lines are parallel
			return null;
		double ua = (xD2 * yD3 - yD2 * xD3) / div;
		pt.x = line1.getX1() + ua * xD1;
		pt.y = line1.getY1() + ua * yD1;

		if(ptOnLineInSegment(pt, line1) && ptOnLineInSegment(pt, line2))
			return pt;

		return null;
	}
	
	public boolean ptOnLineInSegment(Point2D point, Line2D line)
	{
		return point.getX() >= Math.min(line.getX1(), line.getX2()) && 
				point.getX() <= Math.max(line.getX1(), line.getX2()) &&
				point.getY() >= Math.min(line.getY1(), line.getY2()) && 
				point.getY() <= Math.max(line.getY1(), line.getY2());
	}
		
	public void drawRays(double d, double d1, double d2, double rotationAngle, double updown, int x, int y, int z, Collection<ChunkCoordIntPair> chunkSet)
	{
        int cx = (x >> 4) << 4;
        int cz = (z >> 4) << 4;  
        
		GL11.glPushMatrix();
        GL11.glTranslated(d+cx-x+8, d1 + updown + 2, d2+cz-z+8);
        GL11.glRotatef((float) rotationAngle, 0, 1, 0);
        
        double[] distances = new double[4];
        
        Point2D.Double center = new Point2D.Double(cx+8, cz+8);
        
        final int[][] coords = new int[][]{{0,0},{16,0},{16,16},{0,16}};

        Point2D.Double[] absRays = new Point2D.Double[4];
        
        for(int ray = 0; ray < 4; ray++)
        {
        	double rayAngle = Math.toRadians(rotationAngle+90*ray);
        	absRays[ray] = new Point2D.Double(Math.sin(rayAngle), Math.cos(rayAngle));
        }
        
        Line2D.Double[] rays = new Line2D.Double[]{
        		new Line2D.Double(center.x, center.y,center.x+1600*absRays[0].x,center.y+1600*absRays[0].y),
        		new Line2D.Double(center.x, center.y,center.x+1600*absRays[1].x,center.y+1600*absRays[1].y),
        		new Line2D.Double(center.x, center.y,center.x+1600*absRays[2].x,center.y+1600*absRays[2].y),
        		new Line2D.Double(center.x, center.y,center.x+1600*absRays[3].x,center.y+1600*absRays[3].y)};            
        
        for(ChunkCoordIntPair pair : chunkSet)
        {
        	int chunkBlockX = pair.chunkXPos<<4;
        	int chunkBlockZ = pair.chunkZPos<<4;
        	for(int side = 0; side < 4; side++)
        	{
            	int[] offset1 = coords[side];
            	int[] offset2 = coords[(side+1)%4];
            	Line2D.Double line1 = new Line2D.Double(chunkBlockX+offset1[0], chunkBlockZ+offset1[1], chunkBlockX+offset2[0], chunkBlockZ+offset2[1]);
            	for(int ray = 0; ray < 4; ray++)
            	{
            		Point2D.Double isct = findIntersection(line1, rays[ray]);
            		if(isct == null)
            			continue;
            		
            		isct.setLocation(isct.x-center.x, isct.y-center.y);
            		
            		double lenPow2 = isct.x*isct.x+isct.y*isct.y;
            		if(lenPow2 > distances[ray])
            			distances[ray] = lenPow2;
            	}
        	}
        }

        GL11.glColor4d(0.9, 0, 0, 1);
        for(int ray = 0; ray < 4; ray++)
        {
        	distances[ray] = Math.sqrt(distances[ray]);
        	GL11.glRotatef(90, 0, 1, 0);
        	Render.renderAABB(AxisAlignedBB.getBoundingBox(0, -0.05, -0.05, distances[ray], 0.05, 0.05));
        }
        GL11.glPopMatrix();
        
		GL11.glPushMatrix();
        GL11.glTranslated(d+cx-x+8, d1-y, d2+cz-z+8);
        for(int ray = 0; ray < 4; ray++)
        {
    		GL11.glPushMatrix();
    		GL11.glTranslated(absRays[ray].x*distances[ray], 0, absRays[ray].y*distances[ray]);
        	Render.renderAABB(AxisAlignedBB.getBoundingBox(-0.05, 0, -0.05, 0.05, 256, 0.05));
        	GL11.glPopMatrix();
        }
        GL11.glPopMatrix();
        
        double toCenter = Math.sqrt((cx+7.5-x)*(cx+7.5-x)+0.8*0.8+(cz+7.5-z)*(cz+7.5-z));
        
        GL11.glPushMatrix();
        GL11.glColor4d(0, 0.9, 0, 1);
        GL11.glTranslated(d+0.5, d1+1.2+updown, d2+0.5);
        GL11.glRotatef((float) (Math.atan2((cx+7.5-x), (cz+7.5-z))*180/3.1415)+90, 0, 1, 0);
        GL11.glRotatef((float) (-Math.asin(0.8/toCenter)*180/3.1415), 0, 0, 1);
        Render.renderAABB(AxisAlignedBB.getBoundingBox(-toCenter, -0.03, -0.03, 0, 0.03, 0.03));
        GL11.glPopMatrix();
	}
	
	public void renderIcosahedron(Vector3 tilepos, float light, Vector3 relpos, Quat rotation, double scale, int colour)
    {
    	Tessellator tessellator = Tessellator.instance;
    	Vector3[] verts = new Vector3[12];

    	verts[0] = new Vector3(-1, phi, 0);
    	verts[1] = new Vector3( 1, phi, 0);
    	verts[2] = new Vector3( 1,-phi, 0);
    	verts[3] = new Vector3(-1,-phi, 0);

    	verts[4] = new Vector3(0,-1, phi);
    	verts[5] = new Vector3(0, 1, phi);
    	verts[6] = new Vector3(0, 1,-phi);
    	verts[7] = new Vector3(0,-1,-phi);

    	verts[8] =  new Vector3( phi, 0,-1);
    	verts[9] =  new Vector3( phi, 0, 1);
    	verts[10] = new Vector3(-phi, 0, 1);
    	verts[11] = new Vector3(-phi, 0,-1);
    	
    	for(int i = 0; i < 12; i++)
    	{
    		verts[i].multiply(scale);
    		rotation.rotate(verts[i]);
    		verts[i].add(relpos);
    		verts[i].add(tilepos);
    	}
    	
    	tessellator.startDrawingQuads();
    	
    	bindTextureByName("/codechicken/enderstorage/hedronmap.png");
    	tessellator.setColorOpaque_F(light, light, light);
    	//top
    	drawTriangle(verts[1], 0.5F, 0, verts[0], 0, 0.25F, verts[5], 1, 0.25F);
    	drawTriangle(verts[1], 0.5F, 0, verts[5], 0, 0.25F, verts[9], 1, 0.25F);
    	drawTriangle(verts[1], 0.5F, 0, verts[9], 0, 0.25F, verts[8], 1, 0.25F);
    	drawTriangle(verts[1], 0.5F, 0, verts[8], 0, 0.25F, verts[6], 1, 0.25F);
    	drawTriangle(verts[1], 0.5F, 0, verts[6], 0, 0.25F, verts[0], 1, 0.25F);
    	//centre 1vert top
    	drawTriangle(verts[0], 0.5F, 0.25F, verts[11],0, 0.75F, verts[10],1, 0.75F);
    	drawTriangle(verts[5], 0.5F, 0.25F, verts[10],0, 0.75F, verts[4], 1, 0.75F);
    	drawTriangle(verts[9], 0.5F, 0.25F, verts[4], 0, 0.75F, verts[2], 1, 0.75F);
    	drawTriangle(verts[8], 0.5F, 0.25F, verts[2], 0, 0.75F, verts[7], 1, 0.75F);
    	drawTriangle(verts[6], 0.5F, 0.25F, verts[7], 0, 0.75F, verts[11],1, 0.75F);
    	//centre 1vert bottom
    	drawTriangle(verts[2], 0.5F, 0.75F, verts[8], 0, 0.25F, verts[9], 1, 0.25F);
    	drawTriangle(verts[7], 0.5F, 0.75F, verts[6], 0, 0.25F, verts[8], 1, 0.25F);
    	drawTriangle(verts[11],0.5F, 0.75F, verts[0], 0, 0.25F, verts[6], 1, 0.25F);
    	drawTriangle(verts[10],0.5F, 0.75F, verts[5], 0, 0.25F, verts[0], 1, 0.25F);
    	drawTriangle(verts[4], 0.5F, 0.75F, verts[9], 0, 0.25F, verts[5], 1, 0.25F);
    	//bottom
    	drawTriangle(verts[3], 0.5F, 1F, verts[2], 0, 0.75F, verts[4], 1, 0.75F);
    	drawTriangle(verts[3], 0.5F, 1F, verts[7], 0, 0.75F, verts[2], 1, 0.75F);
    	drawTriangle(verts[3], 0.5F, 1F, verts[11],0, 0.75F, verts[7], 1, 0.75F);
    	drawTriangle(verts[3], 0.5F, 1F, verts[10],0, 0.75F, verts[11],1, 0.75F);
    	drawTriangle(verts[3], 0.5F, 1F, verts[4], 0, 0.75F, verts[10],1, 0.75F);
    	
    	tessellator.draw();
	}
    
	public static final double phi = 1.618034;
    
    public static void drawTriangle(Vector3 vec1, float texx1, float texy1, 
    		Vector3 vec2, float texx2, float texy2, 
    		Vector3 vec3, float texx3, float texy3)
    {
    	Tessellator.instance.addVertexWithUV(vec1.x, vec1.y, vec1.z, texx1, texy1);
    	Tessellator.instance.addVertexWithUV(vec2.x, vec2.y, vec2.z, texx2, texy2);
    	Tessellator.instance.addVertexWithUV(vec3.x, vec3.y, vec3.z, texx3, texy3);
    	Tessellator.instance.addVertexWithUV(vec3.x, vec3.y, vec3.z, texx3, texy3);
    }
}
