package main;

public class Vector2i
{
	public int x, y;
	
	public Vector2i()
	{
	}

	public Vector2i(int x, int y)
	{
		set(x, y);
	}
	
	public Vector2i(double x, double y)
	{
		set((int) x, (int) y);
	}

	public Vector2i add(Vector2i v)
	{
		return add(v, new Vector2i());
	}
	//
	public Vector2i add(Vector2i v, Vector2i result)
	{
		result.x = x + v.x;
		result.y = y + v.y;
		return result;
	}

	public Vector2i subtract(Vector2i v)
	{
		return subtract(v, new Vector2i());
	}
	//
	public Vector2i subtract(Vector2i v, Vector2i result)
	{
		result.x = x - v.x;
		result.y = y - v.y;
		return result;
	}

	public Vector2i multiply(Vector2i v)
	{
		return new Vector2i(x * v.x, y * v.x);
	}
	//
	public Vector2i multiply(int s)
	{
		return new Vector2i(x * s, y * s);
	}

	public void set(Vector2i v)
	{
		this.x = v.x;
		this.y = v.y;
	}

	public void set(int x, int y)
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
