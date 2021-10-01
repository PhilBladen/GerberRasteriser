package main;

import static java.lang.Math.*;

public class Matrix2D
{
	private double m00, m01, m02;
	private double m10, m11, m12;
	
	public Matrix2D()
	{
		clear();
	}
	
	/** Resets the matrix to the identity matrix. **/
	public void clear()
	{
		m00 = 1; m01 = 0; m02 = 0;
		m10 = 0; m11 = 1; m12 = 0;
		//    0        0        1
	}
	
	/** A concatenated matrix rotation, implemented most efficiently. **/
	public Matrix2D rotate(double angle)
	{
		double c = cos(angle);
		double s = sin(angle);
		
		double n00 = m00 * c + m01 * s;
		double n01 = m01 * s - m00 * s;
		double n10 = m10 * c + m11 * s;
		double n11 = m11 * c - m10 * s;
		
		m00 = n00;
		m01 = n01;
		m10 = n10;
		m11 = n11;
		
		return this;
	}
	
	/** A concatenated matrix translation, implemented most efficiently. **/
	public Matrix2D translate(double x, double y)
	{
		m02 += m00 * x + m01 * y;
		m12 += m10 * x + m11 * y;
		
		return this;
	}
	
	public Vector2i transform(Vector2i v)
	{
		return transform(v, new Vector2i());
	}
	//
	public Vector2i transform(Vector2i v, Vector2i result)
	{
		result.x = (int) (m00 * v.x + m01 * v.y + m02);
		result.y = (int) (m10 * v.x + m11 * v.y + m12);
		return result;
	}
	
	public Vector2d transform(Vector2d v)
	{
		return transform(v, new Vector2d());
	}
	//
	public Vector2d transform(Vector2d v, Vector2d result)
	{
		result.x = m00 * v.x + m01 * v.y + m02;
		result.y = m10 * v.x + m11 * v.y + m12;
		return result;
	}
	
	public static Matrix2D rotation(double angle)
	{
		return new Matrix2D().rotate(angle);
	}
}
