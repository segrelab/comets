package edu.bu.segrelab.comets.util;

import java.awt.geom.Point2D;

public class Circle
{
	private Point2D.Double center;
	private double r;
	
	public Circle(double x, double y, double radius)
	{
		center = new Point2D.Double((double)x, (double)y);
		this.r = radius;
	}
	
	public Point2D.Double getPoint() { return center; }
	
	public double getCenterX() { return center.getX(); }
	
	public double getCenterY() { return center.getY(); }
	
	public double getRadius() { return r; }
	
	public void setCenterX(double x)
	{
		center.setLocation(x, center.getY());
	}
	
	public void setCenterY(double y)
	{
		center.setLocation(center.getX(), y);
	}
	
	public void setRadius(double r)
	{
		if (r > 0)
			this.r = r;
	}
}
