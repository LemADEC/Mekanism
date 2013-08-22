package mekanism.common;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import org.lwjgl.opengl.GL11;

import buildcraft.api.power.IPowerReceptor;
import buildcraft.api.power.PowerHandler;
import buildcraft.api.power.PowerHandler.PowerReceiver;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Icon;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;
import mekanism.api.ITransmitter;
import mekanism.api.Object3D;
import mekanism.api.TransmissionType;
import mekanism.client.RenderUniversalCable;
import codechicken.lib.lighting.LazyLightMatrix;
import codechicken.lib.render.CCModel;
import codechicken.lib.render.IUVTransformation;
import codechicken.lib.render.IconTransformation;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.TransformationList;
import codechicken.lib.vec.Translation;
import codechicken.lib.vec.Vector3;
import codechicken.multipart.JCuboidPart;
import codechicken.multipart.JNormalOcclusion;
import codechicken.multipart.NormalOcclusionTest;
import codechicken.multipart.PartMap;
import codechicken.multipart.TFacePart;
import codechicken.multipart.TMultiPart;
import codechicken.multipart.TSlottedPart;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class PartTransmitter extends JCuboidPart implements JNormalOcclusion, TSlottedPart, IEnergyTransmitter, IPowerReceptor
{

	private EnergyNetwork theNetwork;
	private PowerHandler powerHandler;
	private float energyScale;

	@Override
	public Cuboid6 getBounds()
	{
		return new Cuboid6(0.3, 0.3, 0.3, 0.7, 0.7, 0.7);
	}

	@Override
	public String getType()
	{
		return "mek_transmitter";
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void renderStatic(Vector3 pos, LazyLightMatrix mat, int pass)
	{
		CCModel cc = CCModel.quadModel(24).generateBox(0, 5, 5, 5, 6, 6, 6, 0, 0, 64, 64, 16);
        TransformationList tl = new TransformationList();
        tl.with(new Translation(pos));

        IUVTransformation uv = new IconTransformation(Mekanism.Transmitter.getIcon(0, 0));
        cc.render(0, cc.verts.length, tl, uv, null);
	}

	@Override
	public Iterable<Cuboid6> getOcclusionBoxes()
	{
		return Collections.singletonList(getBounds());
	}
	
	@Override
	public boolean occlusionTest(TMultiPart other)
	{
		return NormalOcclusionTest.apply(this, other);
	}

	@Override
	public int getSlotMask()
	{
		return PartMap.CENTER.mask;
	}

	@Override
	public TransmissionType getTransmissionType()
	{
		return TransmissionType.ENERGY;
	}
	
	@Override
	public EnergyNetwork getNetwork(boolean createIfNull)
	{
		if(theNetwork == null && createIfNull)
		{
			TileEntity[] adjacentCables = CableUtils.getConnectedCables(getTile());
			HashSet<EnergyNetwork> connectedNets = new HashSet<EnergyNetwork>();
			
			for(TileEntity cable : adjacentCables)
			{
				if(MekanismUtils.checkTransmissionType(cable, TransmissionType.ENERGY) && ((ITransmitter<EnergyNetwork>)cable).getNetwork(false) != null)
				{
					connectedNets.add(((ITransmitter<EnergyNetwork>)cable).getNetwork());
				}
			}
			
			if(connectedNets.size() == 0 || getWorld().isRemote)
			{
				theNetwork = new EnergyNetwork((Collection<ITransmitter<EnergyNetwork>>)Collections.singletonList((ITransmitter<EnergyNetwork>)getTile()));
			}
			else if(connectedNets.size() == 1)
			{
				theNetwork = connectedNets.iterator().next();
				theNetwork.transmitters.add((ITransmitter<EnergyNetwork>)getTile());
			}
			else {
				theNetwork = new EnergyNetwork(connectedNets);
				theNetwork.transmitters.add((ITransmitter<EnergyNetwork>)getTile());
			}
		}
		
		return theNetwork;
	}
	
	@Override
	public void fixNetwork()
	{
		getNetwork().fixMessedUpNetwork((ITransmitter<EnergyNetwork>)getTile());
	}
	
	@Override
	public void onWorldJoin()
	{
		if(!getWorld().isRemote)
		{
			refreshNetwork();
		}
	}
	
	@Override
	public void onNeighborChanged()
	{
		if(!getWorld().isRemote)
		{
			refreshNetwork();
		}
	}
	
	@Override
	public void onWorldSeparate()
	{
		if(!getWorld().isRemote)
		{
			getNetwork().split((ITransmitter<EnergyNetwork>)getTile());
		}
		
		super.onWorldSeparate();
	}
	
	@Override
	public void removeFromNetwork()
	{
		if(theNetwork != null)
		{
			theNetwork.removeTransmitter((ITransmitter<EnergyNetwork>)getTile());
		}
	}

	@Override
	public void refreshNetwork() 
	{
		if(!getWorld().isRemote)
		{
			for(ForgeDirection side : ForgeDirection.VALID_DIRECTIONS)
			{
				TileEntity tileEntity = Object3D.get(getTile()).getFromSide(side).getTileEntity(getWorld());
				
				if(MekanismUtils.checkTransmissionType(tileEntity, TransmissionType.ENERGY))
				{
					getNetwork().merge(((ITransmitter<EnergyNetwork>)tileEntity).getNetwork());
				}
			}
			
			getNetwork().refresh();
		}
	}

	@Override
	public PowerReceiver getPowerReceiver(ForgeDirection side) 
	{
		return powerHandler.getPowerReceiver();
	}
	
	public World getWorld()
	{
		return getTile().getWorldObj();
	}

	@Override
	public void doWork(PowerHandler workProvider) {}
	
	public void setCachedEnergy(double scale)
	{
		energyScale = (float)scale;
	}
	
	public float getEnergyScale()
	{
		return (float)energyScale;
	}
	
	@Override
	public void setNetwork(EnergyNetwork network)
	{
		if(network != theNetwork)
		{
			removeFromNetwork();
			theNetwork = network;
		}
	}
	
	@Override
	public boolean areNetworksEqual(TileEntity tileEntity)
	{
		return tileEntity instanceof ITransmitter && getTransmissionType() == ((ITransmitter)tileEntity).getTransmissionType();
	}
	
	@Override
	public EnergyNetwork getNetwork()
	{
		return getNetwork(true);
	}

}
