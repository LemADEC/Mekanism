package mekanism.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import mekanism.api.transmitters.DynamicNetwork;
import mekanism.api.transmitters.ITransmitter;
import mekanism.api.transmitters.TransmissionType;
import mekanism.common.tileentity.TileEntityMechanicalPipe;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.Event;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidHandler;
import cpw.mods.fml.common.FMLCommonHandler;

public class FluidNetwork extends DynamicNetwork<IFluidHandler, FluidNetwork>
{
	public int transferDelay = 0;
	
	public float fluidScale;
	public float prevFluidScale;
	
	public Fluid refFluid = null;
	
	public FluidNetwork(ITransmitter<FluidNetwork>... varPipes)
	{
		transmitters.addAll(Arrays.asList(varPipes));
		register();
	}
	
	public FluidNetwork(Collection<ITransmitter<FluidNetwork>> collection)
	{
		transmitters.addAll(collection);
		register();
	}
	
	public FluidNetwork(Set<FluidNetwork> networks)
	{
		for(FluidNetwork net : networks)
		{
			if(net != null)
			{
				addAllTransmitters(net.transmitters);
				net.deregister();
			}
		}
		
		refresh();
		register();
	}
	
	public synchronized int getTotalNeeded(List<TileEntity> ignored)
	{
		int toReturn = 0;
		
		for(IFluidHandler handler : possibleAcceptors)
		{
			ForgeDirection side = acceptorDirections.get(handler).getOpposite();
			
			for(Fluid fluid : FluidRegistry.getRegisteredFluids().values())
			{
				int filled = handler.fill(side, new FluidStack(fluid, Integer.MAX_VALUE), false);
				
				toReturn += filled;
				break;
			}
		}
		
		return toReturn;
	}
	
	public synchronized int emit(FluidStack fluidToSend, boolean doTransfer, TileEntity emitter)
	{
		if(refFluid != null && refFluid != fluidToSend.getFluid())
		{
			return 0;
		}
		
		List availableAcceptors = Arrays.asList(getAcceptors(fluidToSend).toArray());
		
		Collections.shuffle(availableAcceptors);
		
		int fluidSent = 0;
		
		if(!availableAcceptors.isEmpty())
		{
			int divider = availableAcceptors.size();
			int remaining = fluidToSend.amount % divider;
			int sending = (fluidToSend.amount-remaining)/divider;
			
			for(Object obj : availableAcceptors)
			{
				if(obj instanceof IFluidHandler && obj != emitter)
				{
					IFluidHandler acceptor = (IFluidHandler)obj;
					int currentSending = sending;
					
					if(remaining > 0)
					{
						currentSending++;
						remaining--;
					}
					
					fluidSent += acceptor.fill(acceptorDirections.get(acceptor), new FluidStack(fluidToSend.fluidID, currentSending), doTransfer);
				}
			}
		}
		
		if(doTransfer && fluidSent > 0 && FMLCommonHandler.instance().getEffectiveSide().isServer())
		{
			FluidStack sendStack = fluidToSend.copy();
			sendStack.amount = fluidSent;
			
			if(sendStack.getFluid() == refFluid)
			{
				fluidScale = Math.min(1, fluidScale+((float)sendStack.amount/1000F));
			}
			else if(refFluid == null)
			{
				refFluid = sendStack.getFluid();
				fluidScale = Math.min(1, ((float)sendStack.amount/1000F));
			}
			
			transferDelay = 2;
		}
		
		return fluidSent;
	}
	
	@Override
	public void tick()
	{
		super.tick();
		
		if(FMLCommonHandler.instance().getEffectiveSide().isServer())
		{
			if(transferDelay == 0)
			{
				if(fluidScale > 0)
				{
					fluidScale -= .02;
				}
				else {
					refFluid = null;
				}
			}
			else {
				transferDelay--;
			}
			
			if(fluidScale != prevFluidScale)
			{
				MinecraftForge.EVENT_BUS.post(new FluidTransferEvent(this, refFluid != null ? refFluid.getID() : -1, fluidScale));
			}
			
			prevFluidScale = fluidScale;
		}
	}
	
	@Override
	public synchronized Set<IFluidHandler> getAcceptors(Object... data)
	{
		FluidStack fluidToSend = (FluidStack)data[0];
		Set<IFluidHandler> toReturn = new HashSet<IFluidHandler>();
		
		for(IFluidHandler acceptor : possibleAcceptors)
		{
			if(acceptor.canFill(acceptorDirections.get(acceptor).getOpposite(), fluidToSend.getFluid()))
			{
				toReturn.add(acceptor);
			}
		}
		
		return toReturn;
	}
 
	@Override
	public synchronized void refresh()
	{
		Set<ITransmitter<FluidNetwork>> iterPipes = (Set<ITransmitter<FluidNetwork>>)transmitters.clone();
		Iterator it = iterPipes.iterator();
		
		possibleAcceptors.clear();
		acceptorDirections.clear();

		while(it.hasNext())
		{
			ITransmitter<FluidNetwork> conductor = (ITransmitter<FluidNetwork>)it.next();

			if(conductor == null || ((TileEntity)conductor).isInvalid())
			{
				it.remove();
				transmitters.remove(conductor);
			}
			else {
				conductor.setTransmitterNetwork(this);
			}
		}
		
		for(ITransmitter<FluidNetwork> pipe : iterPipes)
		{
			if(pipe instanceof TileEntityMechanicalPipe && ((TileEntityMechanicalPipe)pipe).isActive) continue;
			
			IFluidHandler[] acceptors = PipeUtils.getConnectedAcceptors((TileEntity)pipe);
		
			for(IFluidHandler acceptor : acceptors)
			{
				if(acceptor != null && !(acceptor instanceof ITransmitter))
				{
					possibleAcceptors.add(acceptor);
					acceptorDirections.put(acceptor, ForgeDirection.getOrientation(Arrays.asList(acceptors).indexOf(acceptor)));
				}
			}
		}
	}

	@Override
	public synchronized void merge(FluidNetwork network)
	{
		if(network != null && network != this)
		{
			Set<FluidNetwork> networks = new HashSet<FluidNetwork>();
			networks.add(this);
			networks.add(network);
			FluidNetwork newNetwork = new FluidNetwork(networks);
			newNetwork.refresh();
		}
	}
	
	public static class FluidTransferEvent extends Event
	{
		public final FluidNetwork fluidNetwork;
		
		public final int fluidType;
		public final float fluidScale;
		
		public FluidTransferEvent(FluidNetwork network, int type, float scale)
		{
			fluidNetwork = network;
			fluidType = type;
			fluidScale = scale;
		}
	}
		
	@Override
	public String toString()
	{
		return "[FluidNetwork] " + transmitters.size() + " transmitters, " + possibleAcceptors.size() + " acceptors.";
	}
	
	@Override
	protected FluidNetwork create(ITransmitter<FluidNetwork>... varTransmitters) 
	{
		return new FluidNetwork(varTransmitters);
	}

	@Override
	protected FluidNetwork create(Collection<ITransmitter<FluidNetwork>> collection) 
	{
		return new FluidNetwork(collection);
	}

	@Override
	protected FluidNetwork create(Set<FluidNetwork> networks) 
	{
		return new FluidNetwork(networks);
	}
	
	@Override
	public TransmissionType getTransmissionType()
	{
		return TransmissionType.FLUID;
	}

	@Override
	public String getNeeded()
	{
		return "Fluid needed (any type): " + (float)getTotalNeeded(new ArrayList())/1000F + " buckets";
	}
	
	@Override
	public String getFlow()
	{
		return "Not defined yet for Fluid networks";
	}
}
