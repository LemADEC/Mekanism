package mekanism.client.render.tileentity;

import java.util.Arrays;
import java.util.HashMap;

import mekanism.api.Object3D;
import mekanism.api.gas.EnumGas;
import mekanism.api.gas.GasTransmission;
import mekanism.api.gas.ITubeConnection;
import mekanism.client.model.ModelTransmitter;
import mekanism.client.model.ModelTransmitter.Size;
import mekanism.client.render.MekanismRenderer;
import mekanism.client.render.MekanismRenderer.BooleanArray;
import mekanism.client.render.MekanismRenderer.DisplayInteger;
import mekanism.client.render.MekanismRenderer.Model3D;
import mekanism.common.Mekanism;
import mekanism.common.tileentity.TileEntityGasTank;
import mekanism.common.tileentity.TileEntityPressurizedTube;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.fluids.Fluid;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RenderPressurizedTube extends TileEntitySpecialRenderer
{
	private ModelTransmitter model = new ModelTransmitter(Size.SMALL);
	
	private boolean[] connectable;
	
	private HashMap<BooleanArray, HashMap<EnumGas, DisplayInteger>> cachedCenterGasses = new HashMap<BooleanArray, HashMap<EnumGas, DisplayInteger>>();
	
	private static final double offset = 0.015;
	
	@Override
	public void renderTileEntityAt(TileEntity tileEntity, double x, double y, double z, float partialTick)
	{
		renderAModelAt((TileEntityPressurizedTube)tileEntity, x, y, z, partialTick);
	}

	@SuppressWarnings("incomplete-switch")
	public void renderAModelAt(TileEntityPressurizedTube tileEntity, double x, double y, double z, float partialTick)
	{
		bindTexture(MekanismUtils.getResource(ResourceType.RENDER, "PressurizedTube.png"));
		GL11.glPushMatrix();
		GL11.glTranslatef((float)x + 0.5F, (float)y + 1.5F, (float)z + 0.5F);
		GL11.glScalef(1.0F, -1F, -1F);
		GL11.glDisable(GL11.GL_CULL_FACE);
		
		connectable = new boolean[] {false, false, false, false, false, false};
		
		ITubeConnection[] connections = GasTransmission.getConnections(tileEntity);
		
		for(ITubeConnection connection : connections)
		{
			if(connection != null)
			{
				int side = Arrays.asList(connections).indexOf(connection);
				
				if(connection.canTubeConnect(ForgeDirection.getOrientation(side).getOpposite()))
				{
					connectable[side] = true;
				}
			}
		}
		
		model.renderCenter(connectable);
		
		for(int i = 0; i < 6; i++)
		{
			TileEntity sideTile = Object3D.get(tileEntity).getFromSide(ForgeDirection.getOrientation(i)).getTileEntity(tileEntity.worldObj);
			
			if(sideTile instanceof TileEntityGasTank && i != 0 && i != 1)
			{
				GL11.glPushMatrix();
				
				switch(ForgeDirection.getOrientation(i))
				{
					case NORTH:
						GL11.glScalef(1, 1, 1.7f);
						GL11.glTranslatef(0, 0, -.077f);
						break;
					case SOUTH:
						GL11.glScalef(1, 1, 1.7f);
						GL11.glTranslatef(0, 0, .077f);
						break;
					case WEST:
						GL11.glScalef(1.7f, 1, 1);
						GL11.glTranslatef(.077f, 0, 0);
						break;
					case EAST:
						GL11.glScalef(1.7f, 1, 1);
						GL11.glTranslatef(-.077f, 0, 0);
						break;
				}
				
				model.renderSide(ForgeDirection.getOrientation(i), connectable[i]);
				
				GL11.glPopMatrix();
			}
			else {
				model.renderSide(ForgeDirection.getOrientation(i), connectable[i]);
			}
		}
		
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glPopMatrix();
		
		EnumGas gasType = tileEntity.getTransmitterNetwork().refGas;
		float scale = tileEntity.getTransmitterNetwork().gasScale;
	
		if(scale > 0 && gasType != null && gasType.hasTexture())
		{
			push();
			
			GL11.glColor4f(1.0F, 1.0F, 1.0F, scale);
			bindTexture(MekanismRenderer.getBlocksTexture());
			GL11.glTranslatef((float)x, (float)y, (float)z);
			
			for(int i = 0; i < 6; i++)
			{
				if(connectable[i])
				{
					Block b = Block.blocksList[Object3D.get(tileEntity).getFromSide(ForgeDirection.getOrientation(i)).getBlockId(tileEntity.worldObj)];
					getListAndRender(ForgeDirection.getOrientation(i), gasType, b).render();
				}
			}
			
			getListAndRender(ForgeDirection.UNKNOWN, gasType, null).render();
			
			pop();
		}
	}
	
	private void pop()
	{
		GL11.glPopAttrib();
		GL11.glPopMatrix();
	}
	
	private void push()
	{
		GL11.glPushMatrix();
		GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
	}
	
	@SuppressWarnings("incomplete-switch")
	private DisplayInteger getListAndRender(ForgeDirection side, EnumGas type, Block block)
	{
		if(side == ForgeDirection.UNKNOWN)
		{
			if(cachedCenterGasses.containsKey(side) && cachedCenterGasses.get(side).containsKey(type))
			{
				return cachedCenterGasses.get(new BooleanArray(connectable)).get(type);
			}
			
			Model3D toReturn = new Model3D();
			toReturn.baseBlock = Block.waterStill;
			
			toReturn.setTexture(type.gasIcon);
			
			toReturn.minX = 0.3 + offset;
			toReturn.minY = 0.3 + offset;
			toReturn.minZ = 0.3 + offset;
			
			toReturn.maxX = 0.7 - offset;
			toReturn.maxY = 0.7 - offset;
			toReturn.maxZ = 0.7 - offset;
			
			for(ForgeDirection face : ForgeDirection.VALID_DIRECTIONS)
			{
				toReturn.setSideRender(face, !connectable[face.ordinal()]);
			}
			
			DisplayInteger display = DisplayInteger.createAndStart();
			MekanismRenderer.renderObject(toReturn);
			DisplayInteger.endList();
			
			if(cachedCenterGasses.containsKey(side))
			{
				cachedCenterGasses.get(side).put(type, display);
			}
			else {
				HashMap<EnumGas, DisplayInteger> map = new HashMap<EnumGas, DisplayInteger>();
				map.put(type, display);
				
				cachedCenterGasses.put(new BooleanArray(connectable), map);
			}
			
			return display;
		}
		
		Model3D toReturn = new Model3D();
		toReturn.baseBlock = Block.waterStill;
		toReturn.setTexture(type.gasIcon);
		
		toReturn.setSideRender(side, false);
		toReturn.setSideRender(side.getOpposite(), false);
		
		DisplayInteger display = DisplayInteger.createAndStart();
				
		switch(side)
		{
			case DOWN:
			{
				toReturn.minX = 0.3 + offset;
				toReturn.minY = 0.0 - (1-block.getBlockBoundsMaxY());
				toReturn.minZ = 0.3 + offset;
				
				toReturn.maxX = 0.7 - offset;
				toReturn.maxY = 0.3 + offset;
				toReturn.maxZ = 0.7 - offset;
				break;
			}
			case UP:
			{
				toReturn.minX = 0.3 + offset;
				toReturn.minY = 0.7 - offset;
				toReturn.minZ = 0.3 + offset;
				
				toReturn.maxX = 0.7 - offset;
				toReturn.maxY = 1.0 + block.getBlockBoundsMinY();
				toReturn.maxZ = 0.7 - offset;
				break;
			}
			case NORTH:
			{
				toReturn.minX = 0.3 + offset;
				toReturn.minY = 0.3 + offset;
				toReturn.minZ = 0.0 - (1-block.getBlockBoundsMaxZ());
				
				toReturn.maxX = 0.7 - offset;
				toReturn.maxY = 0.7 - offset;
				toReturn.maxZ = 0.3 + offset;
				break;
			}
			case SOUTH:
			{
				toReturn.minX = 0.3 + offset;
				toReturn.minY = 0.3 + offset;
				toReturn.minZ = 0.7 - offset;
				
				toReturn.maxX = 0.7 - offset;
				toReturn.maxY = 0.7 - offset;
				toReturn.maxZ = 1.0 + block.getBlockBoundsMinZ();
				break;
			}
			case WEST:
			{
				toReturn.minX = 0.0 - (1-block.getBlockBoundsMaxX());
				toReturn.minY = 0.3 + offset;
				toReturn.minZ = 0.3 + offset;
				
				toReturn.maxX = 0.3 + offset;
				toReturn.maxY = 0.7 - offset;
				toReturn.maxZ = 0.7 - offset;
				break;
			}
			case EAST:
			{
				toReturn.minX = 0.7 - offset;
				toReturn.minY = 0.3 + offset;
				toReturn.minZ = 0.3 + offset;
				
				toReturn.maxX = 1.0 + block.getBlockBoundsMinX();
				toReturn.maxY = 0.7 - offset;
				toReturn.maxZ = 0.7 - offset;
				break;
			}
		}
		
		MekanismRenderer.renderObject(toReturn);
		DisplayInteger.endList();
		
		return display;
	}
}
