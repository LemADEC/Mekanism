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
import mekanism.api.transmitters.ITransmitter;
import mekanism.api.transmitters.TransmissionType;
import mekanism.api.Object3D;
import mekanism.client.render.tileentity.RenderUniversalCable;
import mekanism.common.tileentity.TileEntityUniversalCable;
import mekanism.common.util.CableUtils;
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
import codechicken.multipart.TMultiPart;
import codechicken.multipart.TSlottedPart;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class PartTransmitter extends JCuboidPart implements JNormalOcclusion, TSlottedPart, IEnergyTransmitter, IPowerReceptor
{

	private EnergyNetwork theNetwork;
	private PowerHandler powerHandler;
	private float energyScale = 0.5F;
	public static RenderUniversalCable renderer = null;
	
	public PartTransmitter()
	{
		super();
		if(renderer == null) renderer = (RenderUniversalCable) TileEntityRenderer.instance.specialRendererMap.get(TileEntityUniversalCable.class);

	}

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
	public void renderDynamic(Vector3 pos, float f, int pass)
	{
		renderer.renderAModelAt(this, pos.x, pos.y, pos.z, f);x();
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
	public EnergyNetwork getTransmitterNetwork(boolean createIfNull)
	{
		if(theNetwork == null && createIfNull)
		{
			TileEntity[] adjacentCables = CableUtils.getConnectedCables(getTile());
			HashSet<EnergyNetwork> connectedNets = new HashSet<EnergyNetwork>();
			
			for(TileEntity cable : adjacentCables)
			{
				if(TransmissionType.checkTransmissionType(cable, TransmissionType.ENERGY) && ((ITransmitter<EnergyNetwork>)cable).getTransmitterNetwork(false) != null)
				{
					connectedNets.add(((ITransmitter<EnergyNetwork>)cable).getTransmitterNetwork());
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
	public void fixTransmitterNetwork()
	{
		getTransmitterNetwork().fixMessedUpNetwork((ITransmitter<EnergyNetwork>)getTile());
	}
	
	@Override
	public void onWorldJoin()
	{
		if(!getWorld().isRemote)
		{
			refreshTransmitterNetwork();
		}
	}
	
	@Override
	public void onNeighborChanged()
	{
		if(!getWorld().isRemote)
		{
			refreshTransmitterNetwork();
		}
	}
	
	@Override
	public void onWorldSeparate()
	{
		if(!getWorld().isRemote)
		{
			getTransmitterNetwork().split((ITransmitter<EnergyNetwork>)getTile());
		}
		
		super.onWorldSeparate();
	}
	
	@Override
	public void removeFromTransmitterNetwork()
	{
		if(theNetwork != null)
		{
			theNetwork.removeTransmitter((ITransmitter<EnergyNetwork>)getTile());
		}
	}

	@Override
	public void refreshTransmitterNetwork() 
	{
		if(!getWorld().isRemote)
		{
			for(ForgeDirection side : ForgeDirection.VALID_DIRECTIONS)
			{
				TileEntity tileEntity = Object3D.get(getTile()).getFromSide(side).getTileEntity(getWorld());
				
				if(TransmissionType.checkTransmissionType(tileEntity, TransmissionType.ENERGY))
				{
					getTransmitterNetwork().merge(((ITransmitter<EnergyNetwork>)tileEntity).getTransmitterNetwork());
				}
			}
			
			getTransmitterNetwork().refresh();
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
	public void setTransmitterNetwork(EnergyNetwork network)
	{
		if(network != theNetwork)
		{
			removeFromTransmitterNetwork();
			theNetwork = network;
		}
	}
	
	@Override
	public boolean areTransmitterNetworksEqual(TileEntity tileEntity)
	{
		return tileEntity instanceof ITransmitter && getTransmissionType() == ((ITransmitter)tileEntity).getTransmissionType();
	}
	
	@Override
	public EnergyNetwork getTransmitterNetwork()
	{
		return getTransmitterNetwork(true);
	}

	@Override
	public int getTransmitterNetworkSize()
	{
		return getTransmitterNetwork().getSize();
	}

	@Override
	public int getTransmitterNetworkAcceptorSize()
	{
		return getTransmitterNetwork().getAcceptorSize();
	}

	@Override
	public String getTransmitterNetworkNeeded()
	{
		return getTransmitterNetwork().getNeeded();
	}

	@Override
	public String getTransmitterNetworkFlow()
	{
		return getTransmitterNetwork().getFlow();
	}

}
