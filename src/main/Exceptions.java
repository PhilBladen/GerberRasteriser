package main;

public class Exceptions
{
	public static class GerberCommandException extends RuntimeException
	{
		private static final long serialVersionUID = 6854591200728635508L;

		public GerberCommandException(String message)
		{
			super(message);
		}
	}
}
