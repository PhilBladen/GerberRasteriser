package main;

public class Vector2d
{
	public double x, y;
	
	public Vector2d()
	{
	}

	public Vector2d(double x, double y)
	{
		set(x, y);
	}

	public Vector2d add(Vector2d v)
	{
		return add(v, new Vector2d());
	}
	//
	public Vector2d add(Vector2d v, Vector2d result)
	{
		result.x = x + v.x;
		result.y = y + v.y;
		return result;
	}

	public Vector2d subtract(Vector2d v)
	{
		return subtract(v, new Vector2d());
	}
	//
	public Vector2d subtract(Vector2d v, Vector2d result)
	{
		result.x = x - v.x;
		result.y = y - v.y;
		return result;
	}

	public Vector2d multiply(Vector2d v)
	{
		return new Vector2d(x * v.x, y * v.x);
	}
	//
	public Vector2d multiply(double s)
	{
		return new Vector2d(x * s, y * s);
	}

	public void set(Vector2d v)
	{
		this.x = v.x;
		this.y = v.y;
	}

	public void set(double x, double y)
	{
		this.x = x;
		this.y = y;
	}

	@Override
	public String toString()
	{
		return String.format("[%f, %f]", x, y);
	}
}
