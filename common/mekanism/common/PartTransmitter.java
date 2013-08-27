package mekanism.common;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import buildcraft.api.power.IPowerReceptor;
import buildcraft.api.power.PowerHandler;
import buildcraft.api.power.PowerHandler.PowerReceiver;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;
import mekanism.api.transmitters.ITransmitter;
import mekanism.api.transmitters.TransmissionType;
import mekanism.api.Object3D;
import mekanism.client.render.tileentity.RenderUniversalCable;
import mekanism.common.tileentity.TileEntityUniversalCable;
import mekanism.common.util.CableUtils;
import codechicken.lib.raytracer.IndexedCuboid6;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Vector3;
import codechicken.multipart.JCuboidPart;
import codechicken.multipart.JNormalOcclusion;
import codechicken.multipart.NormalOcclusionTest;
import codechicken.multipart.PartMap;
import codechicken.multipart.TMultiPart;
import codechicken.multipart.TSlottedPart;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class PartTransmitter extends JCuboidPart implements JNormalOcclusion, TSlottedPart, ITransmitter<EnergyNetwork>, IPowerReceptor
{

	private EnergyNetwork theNetwork;
	private PowerHandler powerHandler;
	private float energyScale = 0.5F;
	public static RenderUniversalCable renderer = null;
	public static IndexedCuboid6[] sides = new IndexedCuboid6[7];
	
	public PartTransmitter()
	{
		super();
		if(renderer == null) renderer = (RenderUniversalCable) TileEntityRenderer.instance.specialRendererMap.get(TileEntityUniversalCable.class);
		sides[0] = new IndexedCuboid6(0, new Cuboid6(0.3, 0.0, 0.3, 0.7, 0.3, 0.7));
		sides[1] = new IndexedCuboid6(1, new Cuboid6(0.3, 0.7, 0.3, 0.7, 1.0, 0.7));
		sides[2] = new IndexedCuboid6(2, new Cuboid6(0.3, 0.3, 0.0, 0.7, 0.7, 0.3));
		sides[3] = new IndexedCuboid6(3, new Cuboid6(0.3, 0.3, 0.7, 0.7, 0.7, 1.0));
		sides[4] = new IndexedCuboid6(4, new Cuboid6(0.0, 0.3, 0.3, 0.3, 0.7, 0.7));
		sides[5] = new IndexedCuboid6(5, new Cuboid6(0.7, 0.3, 0.3, 1.0, 0.7, 0.7));
		sides[6] = new IndexedCuboid6(6, new Cuboid6(0.3, 0.3, 0.3, 0.7, 0.7, 0.7));
	}

	@Override
	public Cuboid6 getBounds()
	{
		double minx = 0.3, miny = 0.3, minz = 0.3;
		double maxx = 0.7, maxy = 0.7, maxz = 0.7;
		boolean[] connections = CableUtils.getConnections(getTile());
		if(connections[0]) miny = 0.0;
		if(connections[1]) miny = 1.0;
		if(connections[2]) minz = 0.0;
		if(connections[3]) maxz = 1.0;
		if(connections[4]) minx = 0.0;
		if(connections[5]) maxx = 1.0;
		return new Cuboid6(minx, miny, minz, maxx, maxy, maxz);
	}
	
	@Override
	public Iterable<IndexedCuboid6> getSubParts()
	{
		Set<IndexedCuboid6> subParts = new HashSet<IndexedCuboid6>();
		boolean[] connections = CableUtils.getConnections(getTile());
		for(ForgeDirection side : ForgeDirection.VALID_DIRECTIONS)
		{
			int ord = side.ordinal();
			if(connections[ord]) subParts.add(sides[ord]);
		}
		subParts.add(sides[6]);
		return subParts;
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
		return Collections.singletonList((Cuboid6)sides[6]);
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
