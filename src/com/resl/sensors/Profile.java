package com.resl.sensors;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Environment;
import android.util.Log;

public class Profile
{
	private static String PROFILE_NAME = "username";
	private static String LAST_NAME = "last_name";
	private static String FIRST_NAME = "first_name";
	private static String GENDER = "gender";
	private static String RACE = "race";
	private static String AGE = "age";
	private static String HEIGHT = "height";
	private static String WEIGHT = "weight";
	private static String LEG_LENGTH = "leg_length";

	private JSONObject mProfile;
	
	private String profile_path;

	public Profile(String profileName)
	{
		// Setup profile path
		profile_path = Environment.getExternalStorageDirectory() + "/RESL_Data/" + profileName;
		
		mProfile = new JSONObject();

		setProfileName(profileName);
		setFirstName("");
		setLastName("");
		setAge(ActivityProfile.NOT_SPECIFIED);
		setHeight(ActivityProfile.NOT_SPECIFIED);
		setWeight(ActivityProfile.NOT_SPECIFIED);
		setLeg_length(ActivityProfile.NOT_SPECIFIED);
		setRace(ActivityProfile.NOT_SPECIFIED);
		setGender(ActivityProfile.NOT_SPECIFIED);
	}

	public Profile(String profileName, String jsonString)
	{	
		// Setup profile path
		profile_path = Environment.getExternalStorageDirectory() + "/RESL_Data/" + profileName;
		
		try
		{
			mProfile = new JSONObject(jsonString);
		}
		catch (JSONException e)
		{
			Log.e("PROFILE", "Error parsing JSON. Error : " + e.getMessage());
		}
		
		if (profileName.compareTo(getProfileName()) != 0)
		{
			Log.e("PROFILE", "Error Reading Profile Profile");
		}
	}

	public String toString()
	{
		return mProfile.toString();
	}
	
	public String getProfilePath()
	{
		return profile_path;
	}
	
	public String getProfileName()
	{
		String profileName = null;

		try
		{
			profileName = mProfile.getString(PROFILE_NAME);
		}
		catch (JSONException e)
		{
			Log.e("PROFILE", "Error parsing JSON. Error : " + e.getMessage());
		}

		return profileName;
	}

	public void setProfileName(String username)
	{
		try
		{
			mProfile.put(PROFILE_NAME, username);
		}
		catch (JSONException e)
		{
			Log.e("PROFILE", "Error parsing JSON. Error : " + e.getMessage());
		}
	}

	public String getLastName()
	{
		String last_name = null;

		try
		{
			last_name = mProfile.getString(LAST_NAME);
		}
		catch (JSONException e)
		{
			Log.e("PROFILE", "Error parsing JSON. Error : " + e.getMessage());
		}

		return last_name;
	}

	public void setLastName(String lastName)
	{
		try
		{
			mProfile.put(LAST_NAME, lastName);
		}
		catch (JSONException e)
		{
			Log.e("PROFILE", "Error parsing JSON. Error : " + e.getMessage());
		}
	}

	public String getFirstName()
	{
		String first_name = null;

		try
		{
			first_name = mProfile.getString(FIRST_NAME);
		}
		catch (JSONException e)
		{
			Log.e("PROFILE", "Error parsing JSON. Error : " + e.getMessage());
		}

		return first_name;
	}

	public void setFirstName(String firstName)
	{
		try
		{
			mProfile.put(FIRST_NAME, firstName);
		}
		catch (JSONException e)
		{
			Log.e("PROFILE", "Error parsing JSON. Error : " + e.getMessage());
		}
	}

	public String getGender()
	{
		String gender = null;

		try
		{
			gender = mProfile.getString(GENDER);
		}
		catch (JSONException e)
		{
			Log.e("PROFILE", "Error parsing JSON. Error : " + e.getMessage());
		}

		return gender;
	}

	public void setGender(String gender)
	{
		try
		{
			mProfile.put(GENDER, gender);
		}
		catch (JSONException e)
		{
			Log.e("PROFILE", "Error parsing JSON. Error : " + e.getMessage());
		}
	}

	public String getRace()
	{
		String race = null;

		try
		{
			race = mProfile.getString(RACE);
		}
		catch (JSONException e)
		{
			Log.e("PROFILE", "Error parsing JSON. Error : " + e.getMessage());
		}

		return race;
	}

	public void setRace(String race)
	{
		try
		{
			mProfile.put(RACE, race);
		}
		catch (JSONException e)
		{
			Log.e("PROFILE", "Error parsing JSON. Error : " + e.getMessage());
		}
	}

	public String getAge()
	{
		String age = null;

		try
		{
			age = mProfile.getString(AGE);
		}
		catch (JSONException e)
		{
			Log.e("PROFILE", "Error parsing JSON. Error : " + e.getMessage());
		}

		return age;
	}

	public void setAge(String age)
	{
		try
		{
			mProfile.put(AGE, age);
		}
		catch (JSONException e)
		{
			Log.e("PROFILE", "Error parsing JSON. Error : " + e.getMessage());
		}
	}

	public String getHeight()
	{
		String height = null;

		try
		{
			height = mProfile.getString(HEIGHT);
		}
		catch (JSONException e)
		{
			Log.e("PROFILE", "Error parsing JSON. Error : " + e.getMessage());
		}

		return height;
	}

	public void setHeight(String height)
	{
		try
		{
			mProfile.put(HEIGHT, height);
		}
		catch (JSONException e)
		{
			Log.e("PROFILE", "Error parsing JSON. Error : " + e.getMessage());
		}
	}

	public String getWeight()
	{
		String weight = null;

		try
		{
			weight = mProfile.getString(WEIGHT);
		}
		catch (JSONException e)
		{
			Log.e("PROFILE", "Error parsing JSON. Error : " + e.getMessage());
		}

		return weight;
	}

	public void setWeight(String weight)
	{
		try
		{
			mProfile.put(WEIGHT, weight);
		}
		catch (JSONException e)
		{
			Log.e("PROFILE", "Error parsing JSON. Error : " + e.getMessage());
		}
	}

	public String getLegLength()
	{
		String legLength = null;

		try
		{
			legLength = mProfile.getString(LEG_LENGTH);
		}
		catch (JSONException e)
		{
			Log.e("PROFILE", "Error parsing JSON. Error : " + e.getMessage());
		}

		return legLength;
	}

	public void setLeg_length(String leg_length)
	{
		try
		{
			mProfile.put(LEG_LENGTH, leg_length);
		}
		catch (JSONException e)
		{
			Log.e("PROFILE", "Error parsing JSON. Error : " + e.getMessage());
		}
	}
}
