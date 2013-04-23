package edu.bu.segrelab.comets.util;

public class Point3d
{
	private int x,
				y,
				z;
	
	public Point3d(int x, int y)
	{
		this.x = x;
		this.y = y;
		z = 0;		// redundant, since new ints are init'd to 0, 
					// but here for clarity
	}
	
	public Point3d(int x, int y, int z)
	{
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public void setX(int x)
	{
		this.x = x;
	}
	
	public void setY(int y)
	{
		this.y = y;
	}
	
	public void setZ(int z)
	{
		this.z = z;
	}
	
	public int getX()
	{
		return x;
	}
	
	public int getY()
	{
		return y;
	}
	
	public int getZ()
	{
		return z;
	}
}
