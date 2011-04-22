package com.resl.sensors;

import java.io.Serializable;

public class FileData implements Serializable
{
	private String profileName;
	private String userName;
	private String activityType;
	private String label;
	private String startTime;
	private boolean isGyroscopePresent;
	private boolean isGyroscopeRotated;
	private String delayGyroscope;
	private boolean isAccelerometerPresent;
	private boolean isAccelerometerRotated;
	private boolean isLoggingEnabled;
	private String delayAccelerometer;
	private int downSampleLength;
	private long[] optionsSelected;

	public String getProfileName()
	{
		return profileName;
	}

	public void setProfileName(String profileName)
	{
		this.profileName = profileName;
	}

	public String getUserName()
	{
		return userName;
	}

	public void setUserName(String userName)
	{
		this.userName = userName;
	}

	public String getActivityType()
	{
		return activityType;
	}

	public void setActivityType(String activityType)
	{
		this.activityType = activityType;
	}

	public String getLabel()
	{
		return label;
	}

	public void setLabel(String label)
	{
		this.label = label;
	}

	public String getStartTime()
	{
		return startTime;
	}

	public void setStartTime(String startTime)
	{
		this.startTime = startTime;
	}

	public boolean isGyroscopePresent()
	{
		return isGyroscopePresent;
	}

	public void setGyroscopePresent(boolean isGyroscopePresent)
	{
		this.isGyroscopePresent = isGyroscopePresent;
	}

	public boolean isGyroscopeRotated()
	{
		return isGyroscopeRotated;
	}

	public void setGyroscopeRotated(boolean isGyroscopeRotated)
	{
		this.isGyroscopeRotated = isGyroscopeRotated;
	}

	public String getDelayGyroscope()
	{
		return delayGyroscope;
	}

	public void setDelayGyroscope(String delayGyroscope)
	{
		this.delayGyroscope = delayGyroscope;
	}

	public boolean isAccelerometerPresent()
	{
		return isAccelerometerPresent;
	}

	public void setAccelerometerPresent(boolean isAccelerometerPresent)
	{
		this.isAccelerometerPresent = isAccelerometerPresent;
	}

	public boolean isAccelerometerRotated()
	{
		return isAccelerometerRotated;
	}

	public void setAccelerometerRotated(boolean isAccelerometerRotated)
	{
		this.isAccelerometerRotated = isAccelerometerRotated;
	}

	public String getDelayAccelerometer()
	{
		return delayAccelerometer;
	}

	public void setDelayAccelerometer(String delayAccelerometer)
	{
		this.delayAccelerometer = delayAccelerometer;
	}
	
	public boolean isLoggingEnabled()
	{
		return isLoggingEnabled;
	}

	public void setLoggingEnabled(boolean isLoggingEnabled)
	{
		this.isLoggingEnabled = isLoggingEnabled;
	}

	public int getDownSampleLength()
	{
		return downSampleLength;
	}

	public void setDownSampleLength(int downSampleLength)
	{
		this.downSampleLength = downSampleLength;
	}

	public long[] getOptionsSelected()
	{
		return optionsSelected;
	}

	public void setOptionsSelected(long[] optionsSelected)
	{
		int offset = 0;
		
		this.optionsSelected = new long[optionsSelected.length];
		
		if (!isGyroscopePresent())
		{
			offset += 3;
			
			if (!isGyroscopeRotated)
			{
				offset += 3;
			}
		}
		
		for (int i = 0; i < optionsSelected.length; i++)
		{
			this.optionsSelected[i] = (long) (offset + optionsSelected[i]);
		}		
		
		return;
	}
}
