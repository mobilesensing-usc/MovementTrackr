package com.resl.sensors;

import java.io.File;

public class Utility
{
	public static void deleteRecursively(String filename, boolean isMasterReset)
	{
		File file = new File(filename);

		if (file.isDirectory())
		{
			String[] list = file.list();

			for (int i = 0; i < list.length; i++)
			{
				File child = new File(filename + "/" + list[i]);

				if (child.isDirectory())
				{
					deleteRecursively(filename + "/" + list[i], isMasterReset);
				}
				
				if (isMasterReset)
				{
					child.delete();
				}
				else
				{
					if ((child.getName().compareTo(".profile") != 0) 
						&& (child.getName().compareTo("avatar.jpg") != 0))
					{
						child.delete();
					}
				}
			}
		}
		else
		{
			file.delete();
		}
	}
}
