package mekanism.common;

import net.minecraft.world.World;
import codechicken.lib.vec.BlockCoord;
import codechicken.multipart.MultiPartRegistry.IPartConverter;
import codechicken.multipart.MultiPartRegistry.IPartFactory;
import codechicken.multipart.MultiPartRegistry;
import codechicken.multipart.MultipartGenerator;
import codechicken.multipart.TMultiPart;

public class MekanismMultipart implements IPartFactory
{
	@Override
	public TMultiPart createPart(String name, boolean arg1)
	{
		if(name == "mek_transmitter") return new PartTransmitter();
		
		return null;
	}
	
	public void init()
	{
        MultiPartRegistry.registerParts(this, new String[]{
                "mek_transmitter"
            });
        
        MultipartGenerator.registerTrait("mekanism.common.IEnergyTransmitter", "mekanism.common.TEnergyTransmitter");

	}

}