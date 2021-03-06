package mekanism.common.block;

import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.tile.IEnergyTile;

import java.util.List;
import java.util.Random;

import mekanism.api.energy.IEnergizedItem;
import mekanism.api.transmitters.ITransmitter;
import mekanism.client.ClientProxy;
import mekanism.common.IEnergyCube;
import mekanism.common.ISustainedInventory;
import mekanism.common.ItemAttacher;
import mekanism.common.Mekanism;
import mekanism.common.Tier.EnergyCubeTier;
import mekanism.common.item.ItemBlockEnergyCube;
import mekanism.common.tileentity.TileEntityBasicBlock;
import mekanism.common.tileentity.TileEntityElectricBlock;
import mekanism.common.tileentity.TileEntityEnergyCube;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Icon;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import buildcraft.api.tools.IToolWrench;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Block class for handling multiple energy cube block IDs. 
 * 0: Basic Energy Cube
 * 1: Advanced Energy Cube
 * 2: Elite Energy Cube
 * @author AidanBrady
 *
 */
public class BlockEnergyCube extends BlockContainer
{
	public Icon[][] icons = new Icon[256][256];
	
	public BlockEnergyCube(int id)
	{
		super(id, Material.iron);
		setHardness(2F);
		setResistance(4F);
		setCreativeTab(Mekanism.tabMekanism);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void registerIcons(IconRegister register) {}
	
	@Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase entityliving, ItemStack itemstack)
    {
    	TileEntityBasicBlock tileEntity = (TileEntityBasicBlock)world.getBlockTileEntity(x, y, z);
        int side = MathHelper.floor_double((double)(entityliving.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
        int height = Math.round(entityliving.rotationPitch);
        int change = 3;
        
        if(height >= 65)
        {
        	change = 1;
        }
        else if(height <= -65)
        {
        	change = 0;
        }
        else {
	        switch(side)
	        {
	        	case 0: change = 2; break;
	        	case 1: change = 5; break;
	        	case 2: change = 3; break;
	        	case 3: change = 4; break;
	        }
        }
        
        tileEntity.setFacing((short)change);
    }
    
    @Override
    public int quantityDropped(Random random)
    {
    	return 0;
    }
    
    @Override
    public int idDropped(int i, Random random, int j)
    {
    	return 0;
    }
    
	@Override
	@SideOnly(Side.CLIENT)
	public void getSubBlocks(int i, CreativeTabs creativetabs, List list)
	{
		for(EnergyCubeTier tier : EnergyCubeTier.values())
		{
			ItemStack discharged = new ItemStack(this);
			discharged.setItemDamage(100);
			((ItemBlockEnergyCube)discharged.getItem()).setEnergyCubeTier(discharged, tier);
			list.add(discharged);
			ItemStack charged = new ItemStack(this);
			((ItemBlockEnergyCube)charged.getItem()).setEnergyCubeTier(charged, tier);
			((ItemBlockEnergyCube)charged.getItem()).setEnergy(charged, tier.MAX_ELECTRICITY);
			list.add(charged);
		};
	}
    
    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer entityplayer, int i1, float f1, float f2, float f3)
    {
		if(ItemAttacher.canAttach(entityplayer.getCurrentEquippedItem()))
		{
			return false;
		}
		
        if(world.isRemote)
        {
            return true;
        }
        else {
        	TileEntityEnergyCube tileEntity = (TileEntityEnergyCube)world.getBlockTileEntity(x, y, z);
        	int metadata = world.getBlockMetadata(x, y, z);
        	
        	if(entityplayer.getCurrentEquippedItem() != null)
        	{
        		Item tool = entityplayer.getCurrentEquippedItem().getItem();
        		
    	    	if(tool instanceof IToolWrench && !tool.getUnlocalizedName().contains("omniwrench"))
    	    	{
    	    		if(((IToolWrench)tool).canWrench(entityplayer, x, y, z))
    	    		{
	    	    		if(entityplayer.isSneaking())
	    	    		{
	    	    			dismantleBlock(world, x, y, z, false);
	    	    			return true;
	    	    		}
	    	    		
	    	    		((IToolWrench)tool).wrenchUsed(entityplayer, x, y, z);
	    	    		
	    	    		int change = 0;
	    	    		
	    	    		switch(tileEntity.facing)
	    	    		{
	    	    			case 3:
	    	    				change = 5;
	    	    				break;
	    	    			case 5:
	    	    				change = 2;
	    	    				break;
	    	    			case 2:
	    	    				change = 4;
	    	    				break;
	    	    			case 4:
	    	    				change = 1;
	    	    				break;
	    	    			case 1:
	    	    				change = 0;
	    	    				break;
	    	    			case 0:
	    	    				change = 3;
	    	    				break;
	    	    		}
	    	    		
	    	    		tileEntity.setFacing((short)change);
	    	    		world.notifyBlocksOfNeighborChange(x, y, z, blockID);
	    	    		return true;
    	    		}
    	    	}
        	}
        	
            if(tileEntity != null)
            {
            	if(!entityplayer.isSneaking())
            	{
            		entityplayer.openGui(Mekanism.instance, 8, world, x, y, z);
            		return true;
            	}
            }
        }
        return false;
    }
    
    @Override
    public boolean removeBlockByPlayer(World world, EntityPlayer player, int x, int y, int z)
    {
    	if(!player.capabilities.isCreativeMode && !world.isRemote && canHarvestBlock(player, world.getBlockMetadata(x, y, z)))
    	{
            float motion = 0.7F;
            double motionX = (world.rand.nextFloat() * motion) + (1.0F - motion) * 0.5D;
            double motionY = (world.rand.nextFloat() * motion) + (1.0F - motion) * 0.5D;
            double motionZ = (world.rand.nextFloat() * motion) + (1.0F - motion) * 0.5D;
            
            EntityItem entityItem = new EntityItem(world, x + motionX, y + motionY, z + motionZ, getPickBlock(null, world, x, y, z));
	        
	        world.spawnEntityInWorld(entityItem);
    	}
    	
        return world.setBlockToAir(x, y, z);
    }
	
	@Override
	public TileEntity createNewTileEntity(World world)
	{
		return new TileEntityEnergyCube();
	}
	
	@Override
	public boolean renderAsNormalBlock()
	{
		return false;
	}
	
	@Override
	public boolean isOpaqueCube()
	{
		return false;
	}
	
	@Override
	public int getRenderType()
	{
		return -1;
	}
	
	@Override
	public ItemStack getPickBlock(MovingObjectPosition target, World world, int x, int y, int z)
	{
    	TileEntityEnergyCube tileEntity = (TileEntityEnergyCube)world.getBlockTileEntity(x, y, z);
    	ItemStack itemStack = new ItemStack(Mekanism.EnergyCube);
        
        IEnergyCube energyCube = (IEnergyCube)itemStack.getItem();
        energyCube.setEnergyCubeTier(itemStack, tileEntity.tier);
        
        IEnergizedItem energizedItem = (IEnergizedItem)itemStack.getItem();
        energizedItem.setEnergy(itemStack, tileEntity.electricityStored);
        
        ISustainedInventory inventory = (ISustainedInventory)itemStack.getItem();
        inventory.setInventory(((ISustainedInventory)tileEntity).getInventory(), itemStack);
        
        return itemStack;
	}
	
	@Override
	public void onBlockAdded(World world, int x, int y, int z)
	{
		TileEntity tileEntity = world.getBlockTileEntity(x, y, z);

		if(!world.isRemote)
		{
			((TileEntityElectricBlock)tileEntity).register();
		}
	}

	public ItemStack dismantleBlock(World world, int x, int y, int z, boolean returnBlock) 
	{
		ItemStack itemStack = getPickBlock(null, world, x, y, z);
        
        world.setBlockToAir(x, y, z);
        
        if(!returnBlock)
        {
            float motion = 0.7F;
            double motionX = (world.rand.nextFloat() * motion) + (1.0F - motion) * 0.5D;
            double motionY = (world.rand.nextFloat() * motion) + (1.0F - motion) * 0.5D;
            double motionZ = (world.rand.nextFloat() * motion) + (1.0F - motion) * 0.5D;
            
            EntityItem entityItem = new EntityItem(world, x + motionX, y + motionY, z + motionZ, itemStack);
	        
            world.spawnEntityInWorld(entityItem);
        }
        
        return itemStack;
	}
	
	@Override
	public boolean hasComparatorInputOverride()
    {
        return true;
    }

	@Override
    public int getComparatorInputOverride(World world, int x, int y, int z, int par5)
    {	
        TileEntityEnergyCube tileEntity = (TileEntityEnergyCube)world.getBlockTileEntity(x, y, z);
        return tileEntity.getRedstoneLevel();
    }
}
