package mekanism.api;

import java.util.ArrayList;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;
import universalelectricity.core.vector.Vector3;

import com.google.common.io.ByteArrayDataInput;

public class Object3D extends Vector3
{
	public int dimensionId;

	public Object3D(int x, int y, int z)
	{
		this.x = x;
		this.y = y;
		this.z = z;

		dimensionId = 0;
	}

	public Object3D(int x, int y, int z, int dimension)
	{
		this.x = x;
		this.y = y;
		this.z = z;

		dimensionId = dimension;
	}

	public int getMetadata(IBlockAccess world)
	{
		return world.getBlockMetadata(intX(), intY(), intZ());
	}

	public int getBlockId(IBlockAccess world)
	{
		return world.getBlockId(intX(), intY(), intZ());
	}

	public TileEntity getTileEntity(IBlockAccess world)
	{
		if (!(world instanceof World && ((World) world).blockExists(intX(), intY(), intZ())))
		{
			return null;
		}

		return world.getBlockTileEntity(intX(), intY(), intZ());
	}

	public NBTTagCompound write(NBTTagCompound nbtTags)
	{
		nbtTags.setInteger("intX()", intX());
		nbtTags.setInteger("intY()", intY());
		nbtTags.setInteger("intZ()", intZ());
		nbtTags.setInteger("dimensionId", dimensionId);

		return nbtTags;
	}

	public void write(ArrayList data)
	{
		data.add(intX());
		data.add(intY());
		data.add(intZ());
	}

	public Object3D translate(int x, int y, int z)
	{
		this.translate(new Vector3(x, y, z));
		return this;
	}

	public Object3D getFromSide(ForgeDirection side)
	{
		return new Object3D(intX() + side.offsetX, intY() + side.offsetY, intZ() + side.offsetZ, dimensionId);
	}

	public static Object3D get(TileEntity tileEntity)
	{
		return new Object3D(tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord, tileEntity.worldObj.provider.dimensionId);
	}

	public static Object3D read(NBTTagCompound nbtTags)
	{
		return new Object3D(nbtTags.getInteger("intX()"), nbtTags.getInteger("intY()"), nbtTags.getInteger("intZ()"), nbtTags.getInteger("dimensionId"));
	}

	public static Object3D read(ByteArrayDataInput dataStream)
	{
		return new Object3D(dataStream.readInt(), dataStream.readInt(), dataStream.readInt());
	}

	public Object3D difference(Object3D other)
	{
		return new Object3D(intX() - other.intX(), intY() - other.intY(), intZ() - other.intZ());
	}

	public ForgeDirection sideDifference(Object3D other)
	{
		Object3D diff = difference(other);

		for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS)
		{
			if (side.offsetX == diff.intX() && side.offsetY == diff.intY() && side.offsetZ == diff.intZ())
			{
				return side;
			}
		}

		return ForgeDirection.UNKNOWN;
	}

	public int distanceTo(Object3D obj)
	{
		int subX = intX() - obj.intX();
		int subY = intY() - obj.intY();
		int subZ = intZ() - obj.intZ();
		return (int) MathHelper.sqrt_double(subX * subX + subY * subY + subZ * subZ);
	}

	public boolean sideVisible(ForgeDirection side, IBlockAccess world)
	{
		return world.getBlockId(intX() + side.offsetX, intY() + side.offsetY, intZ() + side.offsetZ) == 0;
	}

	public Object3D step(ForgeDirection side)
	{
		return translate(side.offsetX, side.offsetY, side.offsetZ);
	}

	@Override
	public String toString()
	{
		return "[Object3D: " + intX() + ", " + intY() + ", " + intZ() + "]";
	}

	@Override
	public boolean equals(Object obj)
	{
		return obj instanceof Object3D && ((Object3D) obj).intX() == intX() && ((Object3D) obj).intY() == intY() && ((Object3D) obj).intZ() == intZ() && ((Object3D) obj).dimensionId == dimensionId;
	}

	@Override
	public int hashCode()
	{
		int code = 1;
		code = 31 * code + intX();
		code = 31 * code + intY();
		code = 31 * code + intZ();
		code = 31 * code + dimensionId;
		return code;
	}
}