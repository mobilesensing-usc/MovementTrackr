package com.resl.sensors;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;

public class ActivitySettings extends PreferenceActivity
{
	SharedPreferences sharedPreferences;

	Preference lvSensorType;
	Preference lvRateGyroscope;
	Preference lvRateAccelerometers;

	CheckBoxPreference cbRotatedGyroscope;
	CheckBoxPreference cbRotatedAccelerometer;

	CheckBoxPreference cbLocation;

	Preference pAddActivity;
	Preference pMasterReset;

	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.preferences);

		sharedPreferences = getSharedPreferences(
				Constants.PREFERENCES_KEY_APPLICATION, Activity.MODE_PRIVATE);

		lvSensorType = (Preference) findPreference(Constants.KEY_SENSOR_TYPE);
		lvRateGyroscope = (Preference) findPreference(Constants.KEY_RATE_GYROSCOPE);
		lvRateAccelerometers = (Preference) findPreference(Constants.KEY_RATE_ACCELEROMETER);

		cbRotatedGyroscope = (CheckBoxPreference) findPreference(Constants.KEY_ROTATED_GYROSCOPE);
		cbRotatedAccelerometer = (CheckBoxPreference) findPreference(Constants.KEY_ROTATED_ACCELEROMETER);

		cbLocation = (CheckBoxPreference) findPreference(Constants.KEY_LOCATION);

		pAddActivity = (Preference) findPreference(Constants.KEY_ADD_ACTIVTY);
		pMasterReset = (Preference) findPreference(Constants.KEY_MASTER_RESET);

		// Check if gyroscope is not selected
		if ((sharedPreferences.getString(Constants.KEY_SENSOR_TYPE,
				getResources().getStringArray(R.array.array_sensor_type)[0]))
				.compareTo(getResources().getStringArray(
						R.array.array_sensor_type)[0]) != 0)
		{
			// If gyroscope is not selected, then it contains accelerometer for
			// sure
			cbRotatedAccelerometer.setEnabled(true);
			lvRateAccelerometers.setEnabled(true);
		} else
		{
			cbRotatedAccelerometer.setEnabled(false);
			lvRateAccelerometers.setEnabled(false);
		}

		// Check if accelerometer is not selected
		if ((sharedPreferences.getString(Constants.KEY_SENSOR_TYPE,
				getResources().getStringArray(R.array.array_sensor_type)[0]))
				.compareTo(getResources().getStringArray(
						R.array.array_sensor_type)[1]) != 0)
		{
			// If accelerometer is not selected, then it contains gyroscope for
			// sure
			cbRotatedGyroscope.setEnabled(true);
			lvRateGyroscope.setEnabled(true);
		} else
		{
			cbRotatedGyroscope.setEnabled(false);
			lvRateGyroscope.setEnabled(false);
		}

		lvSensorType.setSummary("Current Sensor Type : "
				+ sharedPreferences.getString(Constants.KEY_SENSOR_TYPE,
						getResources()
								.getStringArray(R.array.array_sensor_type)[0]));
		lvRateGyroscope.setSummary("Current Rate : "
				+ sharedPreferences.getString(Constants.KEY_RATE_GYROSCOPE,
						getResources().getStringArray(R.array.array_rate)[0]));
		lvRateAccelerometers.setSummary("Current Rate : "
				+ sharedPreferences.getString(Constants.KEY_RATE_ACCELEROMETER,
						getResources().getStringArray(R.array.array_rate)[0]));

		lvSensorType
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
				{
					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue)
					{
						lvSensorType.setSummary("Current Sensor Type : "
								+ newValue);

						Editor editor = getApplicationContext()
								.getSharedPreferences(
										Constants.PREFERENCES_KEY_APPLICATION,
										Context.MODE_PRIVATE).edit();

						// Use first element as default
						editor.putString(Constants.KEY_SENSOR_TYPE,
								(String) newValue);

						// Commit the changes
						editor.commit();

						// Check if gyroscope is not selected
						if (((String) newValue).compareTo(getResources()
								.getStringArray(R.array.array_sensor_type)[0]) != 0)
						{
							// If gyroscope is not selected, then it contains
							// accelerometer for sure
							cbRotatedAccelerometer.setEnabled(true);
							lvRateAccelerometers.setEnabled(true);
						} else
						{
							cbRotatedAccelerometer.setEnabled(false);
							lvRateAccelerometers.setEnabled(false);
						}

						// Check if gyroscope is not selected
						if (((String) newValue).compareTo(getResources()
								.getStringArray(R.array.array_sensor_type)[1]) != 0)
						{
							// If accelerometer is not selected, then it
							// contains
							// gyroscope for sure
							cbRotatedGyroscope.setEnabled(true);
							lvRateGyroscope.setEnabled(true);
						} else
						{
							cbRotatedGyroscope.setEnabled(false);
							lvRateGyroscope.setEnabled(false);
						}

						return true;
					}
				});

		lvRateGyroscope
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
				{
					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue)
					{
						lvRateGyroscope
								.setSummary("Current Rate : " + newValue);

						Editor editor = getApplicationContext()
								.getSharedPreferences(
										Constants.PREFERENCES_KEY_APPLICATION,
										Context.MODE_PRIVATE).edit();

						// Use first element as default
						editor.putString(Constants.KEY_RATE_GYROSCOPE,
								(String) newValue);

						// Commit the changes
						editor.commit();

						return true;
					}
				});

		lvRateAccelerometers
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
				{
					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue)
					{
						lvRateAccelerometers.setSummary("Current Rate : "
								+ newValue);

						Editor editor = getApplicationContext()
								.getSharedPreferences(
										Constants.PREFERENCES_KEY_APPLICATION,
										Context.MODE_PRIVATE).edit();

						// Use first element as default
						editor.putString(Constants.KEY_RATE_ACCELEROMETER,
								(String) newValue);

						// Commit the changes
						editor.commit();

						return true;
					}
				});

		cbRotatedAccelerometer
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
				{
					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue)
					{
						Editor editor = getApplicationContext()
								.getSharedPreferences(
										Constants.PREFERENCES_KEY_APPLICATION,
										Context.MODE_PRIVATE).edit();

						// Use first element as default
						editor.putBoolean(Constants.KEY_ROTATED_ACCELEROMETER,
								(Boolean) newValue);

						// Commit the changes
						editor.commit();

						return true;
					}
				});

		cbRotatedGyroscope
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
				{
					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue)
					{
						Editor editor = getApplicationContext()
								.getSharedPreferences(
										Constants.PREFERENCES_KEY_APPLICATION,
										Context.MODE_PRIVATE).edit();

						// Use first element as default
						editor.putBoolean(Constants.KEY_ROTATED_GYROSCOPE,
								(Boolean) newValue);

						// Commit the changes
						editor.commit();

						return true;
					}
				});

		cbLocation
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
				{
					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue)
					{
						Editor editor = getApplicationContext()
								.getSharedPreferences(
										Constants.PREFERENCES_KEY_APPLICATION,
										Context.MODE_PRIVATE).edit();

						// Use first element as default
						editor.putBoolean(Constants.KEY_LOCATION,
								(Boolean) newValue);

						// Commit the changes
						editor.commit();

						return true;
					}
				});

		pAddActivity
				.setOnPreferenceClickListener(new OnPreferenceClickListener()
				{
					@Override
					public boolean onPreferenceClick(Preference preference)
					{
						Intent intent = new Intent(ActivitySettings.this,
								ActivityAddNewActivity.class);

						startActivity(intent);

						return true;
					}
				});

		pMasterReset
				.setOnPreferenceClickListener(new OnPreferenceClickListener()
				{
					@Override
					public boolean onPreferenceClick(Preference preference)
					{
						final AlertDialog.Builder builder = new AlertDialog.Builder(
								ActivitySettings.this);

						builder.setMessage(
								"This will delete all your profiles and data. Are you sure you want to reset everything?")
								.setCancelable(false)
								.setPositiveButton("Yes",
										new DialogInterface.OnClickListener()
										{
											public void onClick(
													DialogInterface dialog,
													int id)
											{

												Editor editor = getApplicationContext()
														.getSharedPreferences(
																Constants.PREFERENCES_KEY_APPLICATION,
																Context.MODE_PRIVATE)
														.edit();

												// Use first element as default
												editor.remove(Constants.KEY_PROFILE);

												// Commit the changes
												editor.commit();

												Utility.deleteRecursively(
														Environment
																.getExternalStorageDirectory()
																+ "/RESL_Data",
														true);
											}
										})
								.setNegativeButton("No",
										new DialogInterface.OnClickListener()
										{
											public void onClick(
													DialogInterface dialog,
													int id)
											{
												dialog.cancel();
											}
										});

						AlertDialog alert = builder
							.create();
						alert.show();

						return true;
					}
				});
	}

}
