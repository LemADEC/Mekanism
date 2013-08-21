package mekanism.common;

import java.util.HashSet;

import scala.Function1;
import net.minecraft.tileentity.TileEntity;
import mekanism.api.ITransmitter;
import mekanism.api.TransmissionType;
import codechicken.multipart.TMultiPart;
import codechicken.multipart.TileMultipart;

public class TEnergyTransmitter extends TileMultipart implements IEnergyTransmitter
{

	@Override
	public TransmissionType getTransmissionType()
	{
		return TransmissionType.ENERGY;
	}

	@Override
	public EnergyNetwork getNetwork()
	{
		return getNetwork(true);
	}

	@Override
	public EnergyNetwork getNetwork(boolean createIfNull)
	{
		for(TMultiPart p : jPartList())
		{
			if(p instanceof IEnergyTransmitter)
			{
				EnergyNetwork n = ((IEnergyTransmitter) p).getNetwork(createIfNull);
				if(n != null)
				{
					return n;
				}
			}
		}
		return null;
	}

	@Override
	public void setNetwork(EnergyNetwork network)
	{
		for(TMultiPart p : jPartList())
		{
			if(p instanceof IEnergyTransmitter)
			{
				((IEnergyTransmitter) p).setNetwork(network);
			}
		}
	}

	@Override
	public void refreshNetwork()
	{
		for(TMultiPart p : jPartList())
		{
			if(p instanceof IEnergyTransmitter)
			{
				((IEnergyTransmitter) p).refreshNetwork();
			}
		}
	}

	@Override
	public void removeFromNetwork()
	{
		for(TMultiPart p : jPartList())
		{
			if(p instanceof IEnergyTransmitter)
			{
				((IEnergyTransmitter) p).removeFromNetwork();
			}
		}
	}

	@Override
	public void fixNetwork()
	{
		for(TMultiPart p : jPartList())
		{
			if(p instanceof IEnergyTransmitter)
			{
				((IEnergyTransmitter) p).fixNetwork();
			}
		}
	}

	@Override
	public boolean areNetworksEqual(TileEntity tileEntity)
	{
		for(TMultiPart p : jPartList())
		{
			System.out.println(p);
			if(p instanceof IEnergyTransmitter && ((IEnergyTransmitter) p).areNetworksEqual(tileEntity))
			{
				return true;
			}
		}
		return false;
	}

}
