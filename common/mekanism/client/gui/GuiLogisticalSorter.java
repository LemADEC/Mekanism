package mekanism.client.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mekanism.api.Object3D;
import mekanism.client.render.MekanismRenderer;
import mekanism.common.PacketHandler;
import mekanism.common.PacketHandler.Transmission;
import mekanism.common.inventory.container.ContainerNull;
import mekanism.common.network.PacketLogisticalSorterGui;
import mekanism.common.network.PacketLogisticalSorterGui.SorterGuiPacket;
import mekanism.common.network.PacketTileEntity;
import mekanism.common.tileentity.TileEntityLogisticalSorter;
import mekanism.common.transporter.TItemStackFilter;
import mekanism.common.transporter.TOreDictFilter;
import mekanism.common.transporter.TransporterFilter;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiLogisticalSorter extends GuiMekanism
{
	public TileEntityLogisticalSorter tileEntity;
	
	public boolean isDragging = false;
	
	public int dragOffset = 0;
	
	public int stackSwitch = 0;
	
	public Map<TOreDictFilter, StackData> oreDictStacks = new HashMap<TOreDictFilter, StackData>();
	
	public float scroll;
	
	public GuiLogisticalSorter(EntityPlayer player, TileEntityLogisticalSorter tentity)
	{
		super(new ContainerNull(player, tentity));
		tileEntity = tentity;
		guiElements.add(new GuiRedstoneControl(this, tileEntity, MekanismUtils.getResource(ResourceType.GUI, "GuiLogisticalSorter.png")));
	}
	
	public int getScroll()
	{
		return Math.max(Math.min((int)(scroll*123), 123), 0);
	}
	
	public int getFilterIndex()
	{
		if(tileEntity.filters.size() <= 4)
		{
			return 0;
		}
		
		return (int)((tileEntity.filters.size()*scroll) - ((4F/(float)tileEntity.filters.size()))*scroll);
	}
	
	@Override
	public void updateScreen()
	{
		super.updateScreen();
		
		if(stackSwitch > 0)
		{
			stackSwitch--;
		}
		
		if(stackSwitch == 0)
		{
			for(Map.Entry<TOreDictFilter, StackData> entry : oreDictStacks.entrySet())
			{
				if(entry.getValue().iterStacks != null && entry.getValue().iterStacks.size() > 0)
				{
					if(entry.getValue().stackIndex == -1 || entry.getValue().stackIndex == entry.getValue().iterStacks.size()-1)
					{
						entry.getValue().stackIndex = 0;
					}
					else if(entry.getValue().stackIndex < entry.getValue().iterStacks.size()-1)
					{
						entry.getValue().stackIndex++;
					}
					
					entry.getValue().renderStack = entry.getValue().iterStacks.get(entry.getValue().stackIndex);
				}
			}
			
			stackSwitch = 20;
		}
		else {
			for(Map.Entry<TOreDictFilter, StackData> entry : oreDictStacks.entrySet())
			{
				if(entry.getValue().iterStacks != null && entry.getValue().iterStacks.size() == 0)
				{
					entry.getValue().renderStack = null;
				}
			}
		}
		
		Set<TOreDictFilter> filtersVisible = new HashSet<TOreDictFilter>();
		
		for(int i = 0; i < 4; i++)
		{
			if(tileEntity.filters.get(getFilterIndex()+i) instanceof TOreDictFilter)
			{
				filtersVisible.add((TOreDictFilter)tileEntity.filters.get(getFilterIndex()+i));
			}
		}
		
		for(TransporterFilter filter : tileEntity.filters)
		{
			if(filter instanceof TOreDictFilter && !filtersVisible.contains(filter))
			{
				if(oreDictStacks.containsKey(filter))
				{
					oreDictStacks.remove(filter);
				}
			}
		}
	}
	
	@Override
	public void mouseClicked(int mouseX, int mouseY, int button)
	{
		super.mouseClicked(mouseX, mouseY, button);
		
		if(button == 0)
		{
			int xAxis = (mouseX - (width - xSize) / 2);
			int yAxis = (mouseY - (height - ySize) / 2);
			
			if(xAxis >= 154 && xAxis <= 166 && yAxis >= getScroll()+18 && yAxis <= getScroll()+18+15)
			{
				dragOffset = yAxis - (getScroll()+18);
				isDragging = true;
			}
			
			for(int i = 0; i < 4; i++)
			{
				if(tileEntity.filters.get(getFilterIndex()+i) != null)
				{
					int yStart = i*29 + 18;
					
					if(xAxis >= 56 && xAxis <= 152 && yAxis >= yStart && yAxis <= yStart+29)
					{
						TransporterFilter filter = tileEntity.filters.get(getFilterIndex()+i);
						
						if(filter instanceof TItemStackFilter)
						{
							mc.sndManager.playSoundFX("random.click", 1.0F, 1.0F);
							PacketHandler.sendPacket(Transmission.SERVER, new PacketLogisticalSorterGui().setParams(SorterGuiPacket.SERVER_INDEX, Object3D.get(tileEntity), 1, getFilterIndex()+i));
						}
						else if(filter instanceof TOreDictFilter)
						{
							mc.sndManager.playSoundFX("random.click", 1.0F, 1.0F);
							PacketHandler.sendPacket(Transmission.SERVER, new PacketLogisticalSorterGui().setParams(SorterGuiPacket.SERVER_INDEX, Object3D.get(tileEntity), 2, getFilterIndex()+i));
						}
					}
				}
			}
			
			if(xAxis >= 13 && xAxis <= 29 && yAxis >= 137 && yAxis <= 153)
			{
				ArrayList data = new ArrayList();
				data.add(0);
				
				PacketHandler.sendPacket(Transmission.SERVER, new PacketTileEntity().setParams(Object3D.get(tileEntity), data));
	           	mc.sndManager.playSoundFX("mekanism:etc.Ding", 1.0F, 1.0F);
			}
			
			if(xAxis >= 12 && xAxis <= 26 && yAxis >= 110 && yAxis <= 124)
			{
				ArrayList data = new ArrayList();
				data.add(1);
				
				PacketHandler.sendPacket(Transmission.SERVER, new PacketTileEntity().setParams(Object3D.get(tileEntity), data));
				mc.sndManager.playSoundFX("random.click", 1.0F, 1.0F);
			}
			
			if(xAxis >= 12 && xAxis <= 26 && yAxis >= 84 && yAxis <= 98)
			{
				ArrayList data = new ArrayList();
				data.add(2);
				
				PacketHandler.sendPacket(Transmission.SERVER, new PacketTileEntity().setParams(Object3D.get(tileEntity), data));
				mc.sndManager.playSoundFX("random.click", 1.0F, 1.0F);
			}
		}
	}
	
	@Override
	protected void mouseClickMove(int mouseX, int mouseY, int button, long ticks)
	{
		super.mouseClickMove(mouseX, mouseY, button, ticks);
		
		int xAxis = (mouseX - (width - xSize) / 2);
		int yAxis = (mouseY - (height - ySize) / 2);
		
		if(isDragging)
		{
			scroll = Math.min(Math.max((float)(yAxis-18-dragOffset)/123F, 0), 1);
		}
	}
	
	@Override
	protected void mouseMovedOrUp(int x, int y, int type)
	{
		super.mouseMovedOrUp(x, y, type);
		
		if(type == 0 && isDragging)
		{
			dragOffset = 0;
			isDragging = false;
		}
	}
	
	@Override
	public void initGui()
	{
		super.initGui();
		
        int guiWidth = (width - xSize) / 2;
        int guiHeight = (height - ySize) / 2;
		
		buttonList.clear();
		buttonList.add(new GuiButton(0, guiWidth + 56, guiHeight + 136, 54, 20, "ItemStack"));
		buttonList.add(new GuiButton(1, guiWidth + 110, guiHeight + 136, 43, 20, "OreDict"));
	}
	
	@Override
	protected void actionPerformed(GuiButton guibutton)
	{
		super.actionPerformed(guibutton);
		
		if(guibutton.id == 0)
		{
			PacketHandler.sendPacket(Transmission.SERVER, new PacketLogisticalSorterGui().setParams(SorterGuiPacket.SERVER, Object3D.get(tileEntity), 1));
		}
		else if(guibutton.id == 1)
		{
			PacketHandler.sendPacket(Transmission.SERVER, new PacketLogisticalSorterGui().setParams(SorterGuiPacket.SERVER, Object3D.get(tileEntity), 2));
		}
	}
	
	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
    {
		int xAxis = (mouseX - (width - xSize) / 2);
		int yAxis = (mouseY - (height - ySize) / 2);
		
		fontRenderer.drawString("Logistical Sorter", 43, 6, 0x404040);
		
		fontRenderer.drawString("Filters:", 11, 19, 0x00CD00);
		fontRenderer.drawString("T: " + tileEntity.filters.size(), 11, 28, 0x00CD00);
		fontRenderer.drawString("IS: " + getItemStackFilters().size(), 11, 37, 0x00CD00);
		fontRenderer.drawString("OD: " + getOreDictFilters().size(), 11, 46, 0x00CD00);
		
		fontRenderer.drawString("RR:", 12, 74, 0x00CD00);
		fontRenderer.drawString(tileEntity.roundRobin ? "On" : "Off", 27, 86, 0x00CD00);
		
		fontRenderer.drawString("Auto:", 12, 100, 0x00CD00);
		fontRenderer.drawString(tileEntity.autoEject ? "On" : "Off", 27, 112, 0x00CD00);
		
		fontRenderer.drawString("Default:", 12, 126, 0x00CD00);
		
		for(int i = 0; i < 4; i++)
		{
			if(tileEntity.filters.get(getFilterIndex()+i) != null)
			{
				TransporterFilter filter = tileEntity.filters.get(getFilterIndex()+i);
				int yStart = i*29 + 18;
				
				if(filter instanceof TItemStackFilter)
				{
					TItemStackFilter itemFilter = (TItemStackFilter)filter;
					
					if(itemFilter.itemType != null)
					{
						GL11.glPushMatrix();
						GL11.glEnable(GL11.GL_LIGHTING);
						itemRenderer.renderItemAndEffectIntoGUI(fontRenderer, mc.getTextureManager(), itemFilter.itemType, 59, yStart + 3);
						GL11.glDisable(GL11.GL_LIGHTING);
						GL11.glPopMatrix();
					}
					
					fontRenderer.drawString("Item Filter", 78, yStart + 2, 0x404040);
					fontRenderer.drawString(filter.color != null ? filter.color.getName() : "None", 78, yStart + 11, 0x404040);
				}
				else if(filter instanceof TOreDictFilter)
				{
					TOreDictFilter oreFilter = (TOreDictFilter)filter;
					
					if(!oreDictStacks.containsKey(oreFilter))
					{
						updateStackList(oreFilter);
					}
					
					if(oreDictStacks.get(filter).renderStack != null)
					{
						GL11.glPushMatrix();
						GL11.glEnable(GL11.GL_LIGHTING);
						itemRenderer.renderItemAndEffectIntoGUI(fontRenderer, mc.getTextureManager(), oreDictStacks.get(filter).renderStack, 59, yStart + 3);
						GL11.glDisable(GL11.GL_LIGHTING);
						GL11.glPopMatrix();
					}
					
					fontRenderer.drawString("OreDict Filter", 78, yStart + 2, 0x404040);
					fontRenderer.drawString(filter.color != null ? filter.color.getName() : "None", 78, yStart + 11, 0x404040);
				}
			}
		}
		
		if(tileEntity.color != null)
		{
			GL11.glPushMatrix();
			GL11.glColor4f(1, 1, 1, 1);
	        GL11.glEnable(GL11.GL_LIGHTING);
	        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
	        
	        mc.getTextureManager().bindTexture(MekanismRenderer.getBlocksTexture());
			itemRenderer.renderIcon(13, 137, MekanismRenderer.getColorIcon(tileEntity.color), 16, 16);
			
			GL11.glDisable(GL11.GL_LIGHTING);
			GL11.glPopMatrix();
		}
		
		if(xAxis >= 13 && xAxis <= 29 && yAxis >= 137 && yAxis <= 153)
		{
			if(tileEntity.color != null)
			{
				drawCreativeTabHoveringText(tileEntity.color.getName(), xAxis, yAxis);
			}
			else {
				drawCreativeTabHoveringText("None", xAxis, yAxis);
			}
		}
		
		if(xAxis >= 12 && xAxis <= 26 && yAxis >= 110 && yAxis <= 124)
		{
			drawCreativeTabHoveringText("Auto-eject", xAxis, yAxis);
		}
		
		if(xAxis >= 12 && xAxis <= 26 && yAxis >= 84 && yAxis <= 98)
		{
			drawCreativeTabHoveringText("Round robin", xAxis, yAxis);
		}
		
		super.drawGuiContainerForegroundLayer(mouseX, mouseY);
    }

	@Override
    protected void drawGuiContainerBackgroundLayer(float partialTick, int mouseX, int mouseY)
    {
		super.drawGuiContainerBackgroundLayer(partialTick, mouseX, mouseY);
		
		mc.renderEngine.bindTexture(MekanismUtils.getResource(ResourceType.GUI, "GuiLogisticalSorter.png"));
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        int guiWidth = (width - xSize) / 2;
        int guiHeight = (height - ySize) / 2;
        drawTexturedModalRect(guiWidth, guiHeight, 0, 0, xSize, ySize);
        
		drawTexturedModalRect(guiWidth + 154, guiHeight + 18 + getScroll(), 232, 0, 12, 15);
		
		int xAxis = (mouseX - (width - xSize) / 2);
		int yAxis = (mouseY - (height - ySize) / 2);
		
		for(int i = 0; i < 4; i++)
		{
			if(tileEntity.filters.get(getFilterIndex()+i) != null)
			{
				TransporterFilter filter = tileEntity.filters.get(getFilterIndex()+i);
				int yStart = i*29 + 18;
				
				boolean mouseOver = xAxis >= 56 && xAxis <= 152 && yAxis >= yStart && yAxis <= yStart+29;
				
				if(filter instanceof TItemStackFilter)
				{
					drawTexturedModalRect(guiWidth + 56, guiHeight + yStart, mouseOver ? 0 : 96, 166, 96, 29);
				}
				else if(filter instanceof TOreDictFilter)
				{
					drawTexturedModalRect(guiWidth + 56, guiHeight + yStart, mouseOver ? 0 : 96, 195, 96, 29);
				}
			}
		}
		
		if(xAxis >= 12 && xAxis <= 26 && yAxis >= 110 && yAxis <= 124)
		{
			drawTexturedModalRect(guiWidth + 12, guiHeight + 110, 176, 0, 14, 14);
		}
		else {
			drawTexturedModalRect(guiWidth + 12, guiHeight + 110, 176, 14, 14, 14);
		}
		
		if(xAxis >= 12 && xAxis <= 26 && yAxis >= 84 && yAxis <= 98)
		{
			drawTexturedModalRect(guiWidth + 12, guiHeight + 84, 176 + 14, 0, 14, 14);
		}
		else {
			drawTexturedModalRect(guiWidth + 12, guiHeight + 84, 176 + 14, 14, 14, 14);
		}
    }
	
	public ArrayList getItemStackFilters()
	{
		ArrayList list = new ArrayList();
		
		for(TransporterFilter filter : tileEntity.filters)
		{
			if(filter instanceof TItemStackFilter)
			{
				list.add(filter);
			}
		}
		
		return list;
	}
	
	public ArrayList getOreDictFilters()
	{
		ArrayList list = new ArrayList();
		
		for(TransporterFilter filter : tileEntity.filters)
		{
			if(filter instanceof TOreDictFilter)
			{
				list.add(filter);
			}
		}
		
		return list;
	}
	
	private void updateStackList(TOreDictFilter filter)
	{
		if(!oreDictStacks.containsKey(filter))
		{
			oreDictStacks.put(filter, new StackData());
		}
		
       	if(oreDictStacks.get(filter).iterStacks == null)
    	{
       		oreDictStacks.get(filter).iterStacks = new ArrayList<ItemStack>();
    	}
    	else {
    		oreDictStacks.get(filter).iterStacks.clear();
    	}
    	
    	List<String> keys = new ArrayList<String>();
    	
    	for(String s : OreDictionary.getOreNames())
    	{
    		if(filter.oreDictName.equals(s) || filter.oreDictName.equals("*"))
    		{
    			keys.add(s);
    		}
    		else if(filter.oreDictName.endsWith("*") && !filter.oreDictName.startsWith("*"))
    		{
    			if(s.startsWith(filter.oreDictName.substring(0, filter.oreDictName.length()-1)))
    			{
    				keys.add(s);
    			}
    		}
    		else if(filter.oreDictName.startsWith("*") && !filter.oreDictName.endsWith("*"))
    		{
    			if(s.endsWith(filter.oreDictName.substring(1)))
    			{
    				keys.add(s);
    			}
    		}
    		else if(filter.oreDictName.startsWith("*") && filter.oreDictName.endsWith("*"))
    		{
    			if(s.contains(filter.oreDictName.substring(1, filter.oreDictName.length()-1)))
    			{
    				keys.add(s);
    			}
    		}
    	}
    	
    	for(String key : keys)
    	{
    		for(ItemStack stack : OreDictionary.getOres(key))
    		{
    			ItemStack toAdd = stack.copy();
    			
    			if(!oreDictStacks.get(filter).iterStacks.contains(stack))
    			{
    				oreDictStacks.get(filter).iterStacks.add(stack.copy());
    			}
    		}
    	}
    	
    	stackSwitch = 0;
    	updateScreen();
    	oreDictStacks.get(filter).stackIndex = -1;
    }
	
	public static class StackData
	{
		public List<ItemStack> iterStacks;
		public int stackIndex;
		public ItemStack renderStack;
	}
}