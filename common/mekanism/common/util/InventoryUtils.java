package mekanism.common.util;

import mekanism.api.EnumColor;
import mekanism.api.IConfigurable;
import mekanism.common.tileentity.TileEntityBin;
import mekanism.common.tileentity.TileEntityLogisticalSorter;
import mekanism.common.transporter.InvStack;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.InventoryLargeChest;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraftforge.common.ForgeDirection;

public final class InventoryUtils 
{
	public static IInventory checkChestInv(IInventory inv)
	{
		if(inv instanceof TileEntityChest)
		{
			TileEntityChest main = (TileEntityChest)inv;
			TileEntityChest adj = null;
			
			if(main.adjacentChestXNeg != null)
			{
				adj = main.adjacentChestXNeg;
			}
			else if(main.adjacentChestXPos != null)
			{
				adj = main.adjacentChestXPos;
			}
			else if(main.adjacentChestZNeg != null)
			{
				adj = main.adjacentChestZNeg;
			}
			else if(main.adjacentChestZPosition != null)
			{
				adj = main.adjacentChestZPosition;
			}
			
			if(adj != null)
			{
				return new InventoryLargeChest("", main, adj);
			}
		}
		
		return inv;
	}

	public static ItemStack putStackInInventory(IInventory inventory, ItemStack itemStack, int side, boolean force) 
	{
		if(force && inventory instanceof TileEntityLogisticalSorter)
		{
			return ((TileEntityLogisticalSorter)inventory).sendHome(itemStack.copy());
		}
		
		ItemStack toInsert = itemStack.copy();
		
		if(!(inventory instanceof ISidedInventory))
		{
			inventory = checkChestInv(inventory);
			
			for(int i = 0; i <= inventory.getSizeInventory() - 1; i++)
			{
				if(!force)
				{
					if(!inventory.isItemValidForSlot(i, toInsert)) 
					{
						continue;
					}
				}
				
				ItemStack inSlot = inventory.getStackInSlot(i);
	
				if(inSlot == null)
				{
					inventory.setInventorySlotContents(i, toInsert);
					return null;
				} 
				else if(inSlot.isItemEqual(toInsert) && inSlot.stackSize < inSlot.getMaxStackSize()) 
				{
					if(inSlot.stackSize + toInsert.stackSize <= inSlot.getMaxStackSize()) 
					{
						ItemStack toSet = toInsert.copy();
						toSet.stackSize += inSlot.stackSize;
	
						inventory.setInventorySlotContents(i, toSet);
						return null;
					} 
					else {
						int rejects = (inSlot.stackSize + toInsert.stackSize) - inSlot.getMaxStackSize();
	
						ItemStack toSet = toInsert.copy();
						toSet.stackSize = inSlot.getMaxStackSize();
	
						ItemStack remains = toInsert.copy();
						remains.stackSize = rejects;
	
						inventory.setInventorySlotContents(i, toSet);
						
						toInsert = remains;
					}
				}
			}
		} 
		else {
			ISidedInventory sidedInventory = (ISidedInventory)inventory;
			int[] slots = sidedInventory.getAccessibleSlotsFromSide(ForgeDirection.getOrientation(side).getOpposite().ordinal());
	
			if(slots != null && slots.length != 0)
			{
				if(force && sidedInventory instanceof TileEntityBin && ForgeDirection.getOrientation(side).getOpposite().ordinal() == 0)
				{
					slots = sidedInventory.getAccessibleSlotsFromSide(1);
				}
				
				for(int get = 0; get <= slots.length - 1; get++) 
				{
					int slotID = slots[get];
	
					if(!force)
					{
						if(!sidedInventory.isItemValidForSlot(slotID, toInsert) && !sidedInventory.canInsertItem(slotID, toInsert, ForgeDirection.getOrientation(side).getOpposite().ordinal())) 
						{
							continue;
						}
					}
					
					ItemStack inSlot = inventory.getStackInSlot(slotID);
	
					if(inSlot == null) 
					{
						inventory.setInventorySlotContents(slotID, toInsert);
						return null;
					} 
					else if(inSlot.isItemEqual(toInsert) && inSlot.stackSize < inSlot.getMaxStackSize())
					{
						if(inSlot.stackSize + toInsert.stackSize <= inSlot.getMaxStackSize()) 
						{
							ItemStack toSet = toInsert.copy();
							toSet.stackSize += inSlot.stackSize;
	
							inventory.setInventorySlotContents(slotID, toSet);
							return null;
						} 
						else {
							int rejects = (inSlot.stackSize + toInsert.stackSize) - inSlot.getMaxStackSize();
	
							ItemStack toSet = toInsert.copy();
							toSet.stackSize = inSlot.getMaxStackSize();
	
							ItemStack remains = toInsert.copy();
							remains.stackSize = rejects;
	
							inventory.setInventorySlotContents(slotID, toSet);
							
							toInsert = remains;
						}
					}
				}
			}
		}
	
		return toInsert;
	}
	
	public static ItemStack takeTopItemFromInventory(IInventory inventory, int side)
	{
		if(!(inventory instanceof ISidedInventory))
		{
			for(int i = inventory.getSizeInventory() - 1; i >= 0; i--)
			{
				if(inventory.getStackInSlot(i) != null)
				{
					ItemStack toSend = inventory.getStackInSlot(i).copy();
					toSend.stackSize = 1;

					inventory.decrStackSize(i, 1);

					return toSend;
				}
			}
		}
		else {
			ISidedInventory sidedInventory = (ISidedInventory) inventory;
			int[] slots = sidedInventory.getAccessibleSlotsFromSide(side);

			if(slots != null)
			{
				for(int get = slots.length - 1; get >= 0; get--)
				{
					int slotID = slots[get];

					if(sidedInventory.getStackInSlot(slotID) != null)
					{
						ItemStack toSend = sidedInventory.getStackInSlot(slotID);
						toSend.stackSize = 1;

						if(sidedInventory.canExtractItem(slotID, toSend, side))
						{
							sidedInventory.decrStackSize(slotID, 1);

							return toSend;
						}
					}
				}
			}
		}

		return null;
	}

	public static InvStack takeDefinedItem(IInventory inventory, int side, ItemStack type, int min, int max)
	{
		InvStack ret = new InvStack(inventory);
		
		if(!(inventory instanceof ISidedInventory)) 
		{
			inventory = checkChestInv(inventory);
			
			for(int i = inventory.getSizeInventory() - 1; i >= 0; i--) 
			{
				if(inventory.getStackInSlot(i) != null && inventory.getStackInSlot(i).isItemEqual(type)) 
				{
					ItemStack stack = inventory.getStackInSlot(i);
					int current = ret.getStack() != null ? ret.getStack().stackSize : 0;
					
					if(current+stack.stackSize <= max)
					{
						ret.appendStack(i, stack.copy());
					}
					else {
						ItemStack copy = stack.copy();
						copy.stackSize = max-current;
						ret.appendStack(i, copy);
					}
	
					if(ret.getStack() != null && ret.getStack().stackSize == max)
					{
						return ret;
					}
				}
			}
		} 
		else {
			ISidedInventory sidedInventory = (ISidedInventory)inventory;
			int[] slots = sidedInventory.getAccessibleSlotsFromSide(ForgeDirection.getOrientation(side).getOpposite().ordinal());
	
			if(slots != null && slots.length != 0) 
			{
				for(int get = slots.length - 1; get >= 0; get--) 
				{
					int slotID = slots[get];
	
					if(sidedInventory.getStackInSlot(slotID) != null && inventory.getStackInSlot(slotID).isItemEqual(type)) 
					{
						ItemStack stack = sidedInventory.getStackInSlot(slotID);
						int current = ret.getStack() != null ? ret.getStack().stackSize : 0;
						
						if(current+stack.stackSize <= max)
						{
							ItemStack copy = stack.copy();
							
							if(sidedInventory.canExtractItem(slotID, copy, ForgeDirection.getOrientation(side).getOpposite().ordinal())) 
							{
								ret.appendStack(slotID, copy);
							}
						}
						else {
							ItemStack copy = stack.copy();
							
							if(sidedInventory.canExtractItem(slotID, copy, ForgeDirection.getOrientation(side).getOpposite().ordinal())) 
							{
								copy.stackSize = max-current;
								ret.appendStack(slotID, copy);
							}
						}
	
						if(ret.getStack() != null && ret.getStack().stackSize == max)
						{
							return ret;
						}
					}
				}
			}
		}
		
		if(ret != null && ret.getStack() != null && ret.getStack().stackSize >= min)
		{
			return ret;
		}
	
		return null;
	}

	public static InvStack takeTopStack(IInventory inventory, int side) 
	{
		if(!(inventory instanceof ISidedInventory)) 
		{
			inventory = checkChestInv(inventory);
			
			for(int i = inventory.getSizeInventory() - 1; i >= 0; i--) 
			{
				if(inventory.getStackInSlot(i) != null) 
				{
					ItemStack toSend = inventory.getStackInSlot(i).copy();
					return new InvStack(inventory, i, toSend);
				}
			}
		} 
		else {
			ISidedInventory sidedInventory = (ISidedInventory)inventory;
			int[] slots = sidedInventory.getAccessibleSlotsFromSide(ForgeDirection.getOrientation(side).getOpposite().ordinal());
	
			if(slots != null && slots.length != 0) 
			{
				for(int get = slots.length - 1; get >= 0; get--) 
				{
					int slotID = slots[get];
	
					if(sidedInventory.getStackInSlot(slotID) != null) 
					{
						ItemStack toSend = sidedInventory.getStackInSlot(slotID);
	
						if(sidedInventory.canExtractItem(slotID, toSend, ForgeDirection.getOrientation(side).getOpposite().ordinal())) 
						{
							return new InvStack(inventory, slotID, toSend);
						}
					}
				}
			}
		}
	
		return null;
	}

    public static boolean canInsert(TileEntity tileEntity, EnumColor color, ItemStack itemStack, int side, boolean force)
    {
    	if(!(tileEntity instanceof IInventory))
    	{
    		return false;
    	}
    	
    	if(force && tileEntity instanceof TileEntityLogisticalSorter)
    	{
    		return ((TileEntityLogisticalSorter)tileEntity).canSendHome(itemStack);
    	}
    	
    	if(!force && tileEntity instanceof IConfigurable)
    	{
    		IConfigurable config = (IConfigurable)tileEntity;
    		int tileSide = config.getOrientation();
    		EnumColor configColor = config.getEjector().getInputColor(ForgeDirection.getOrientation(MekanismUtils.getBaseOrientation(side, tileSide)).getOpposite());
    		
    		if(config.getEjector().hasStrictInput() && configColor != null && configColor != color)
    		{
    			return false;
    		}
    	}
    	
    	IInventory inventory = (IInventory)tileEntity;
    	
    	if(!(inventory instanceof ISidedInventory))
		{
    		inventory = InventoryUtils.checkChestInv(inventory);
    		
			for(int i = 0; i <= inventory.getSizeInventory() - 1; i++)
			{
				if(!force)
				{
					if(!inventory.isItemValidForSlot(i, itemStack)) 
					{
						continue;
					}
				}
				
				ItemStack inSlot = inventory.getStackInSlot(i);

				if(inSlot == null)
				{
					return true;
				} 
				else if(inSlot.isItemEqual(itemStack) && inSlot.stackSize < inSlot.getMaxStackSize()) 
				{
					if(inSlot.stackSize + itemStack.stackSize <= inSlot.getMaxStackSize()) 
					{
						return true;
					} 
					else {
						int rejects = (inSlot.stackSize + itemStack.stackSize) - inSlot.getMaxStackSize();

						if(rejects < itemStack.stackSize)
						{
							return true;
						}
					}
				}
			}
		} 
		else {
			ISidedInventory sidedInventory = (ISidedInventory)inventory;
			int[] slots = sidedInventory.getAccessibleSlotsFromSide(ForgeDirection.getOrientation(side).getOpposite().ordinal());

			if(slots != null && slots.length != 0)
			{
				if(force && sidedInventory instanceof TileEntityBin && ForgeDirection.getOrientation(side).getOpposite().ordinal() == 0)
				{
					slots = sidedInventory.getAccessibleSlotsFromSide(1);
				}
				
				for(int get = 0; get <= slots.length - 1; get++) 
				{
					int slotID = slots[get];
	
					if(!force)
					{
						if(!sidedInventory.isItemValidForSlot(slotID, itemStack) || !sidedInventory.canInsertItem(slotID, itemStack, ForgeDirection.getOrientation(side).getOpposite().ordinal())) 
						{
							continue;
						}
					}
					
					ItemStack inSlot = inventory.getStackInSlot(slotID);

					if(inSlot == null) 
					{
						return true;
					} 
					else if(inSlot.isItemEqual(itemStack) && inSlot.stackSize < inSlot.getMaxStackSize())
					{
						if(inSlot.stackSize + itemStack.stackSize <= inSlot.getMaxStackSize()) 
						{
							return true;
						} 
						else {
							int rejects = (inSlot.stackSize + itemStack.stackSize) - inSlot.getMaxStackSize();
							
							if(rejects < itemStack.stackSize)
							{
								return true;
							}
						}
					}
				}
			}
		}
    	
    	return false;
    }
}
