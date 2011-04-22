package com.resl.sensors;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.resl.sensors.ServiceSensors.ServiceSensorListener;

public class ActivityDataCollection extends Activity
{
	private static int BUTTON_CLICK_TIME = 5000;

	private ServiceSensors mService;

	private TextView tvProfileName;
	private TextView tvFirstName;
	private TextView tvLastName;
	private ImageView ivUserPicture;
	private Spinner spinnerActivity;
	private EditText etLabel;
	private LinearLayout llStartStop;
	private TextView tvStartStopButton;
	private TextView tvStartStopButtonMessage;
	private ImageButton ibVibration;
	private ImageButton ibSound;
	private ImageView ivTimerCheckbox;
	private TextView tvTimer;

	private String timerString = "";

	private boolean isVibrationEnabled;
	private boolean isSoundEnabled;
	private boolean isFirstCheck;

	private boolean hasStarted;

	private Profile mProfile;

	private static MediaPlayer mMediaPlayer;
	private static Vibrator vibrator;

	ArrayList<String> activities;

	long mStartTime = 0L;

	String mainPath = Environment.getExternalStorageDirectory() + "/RESL_Data";

	private SharedPreferences sharedPreferences;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_data_collection);

		// Get shared preferences
		sharedPreferences = getSharedPreferences(Constants.PREFERENCES_KEY_APPLICATION, Activity.MODE_PRIVATE);

		// Initialize if not already initialized
		if (!sharedPreferences.contains(Constants.KEY_SENSOR_TYPE))
		{
			Editor editor = getApplicationContext().getSharedPreferences(Constants.PREFERENCES_KEY_APPLICATION, Context.MODE_PRIVATE)
					.edit();
			String[] arraySensorTypes = getResources().getStringArray(R.array.array_sensor_type);
			String[] arrayDelayOptions = getResources().getStringArray(R.array.array_rate);

			// Use first element as default
			editor.putString(Constants.KEY_SENSOR_TYPE, arraySensorTypes[0]);
			editor.putString(Constants.KEY_RATE_GYROSCOPE, arrayDelayOptions[0]);
			editor.putString(Constants.KEY_RATE_ACCELEROMETER, arrayDelayOptions[0]);
			editor.putBoolean(Constants.KEY_ROTATED_GYROSCOPE, false);
			editor.putBoolean(Constants.KEY_ROTATED_ACCELEROMETER, false);

			// Commit the changes
			editor.commit();
		}

		// Get UI elements
		tvProfileName = (TextView) findViewById(R.id.textview_data_collection_profilename);
		tvFirstName = (TextView) findViewById(R.id.textview_data_collection_firstname);
		tvLastName = (TextView) findViewById(R.id.textview_data_collection_lastname);
		ivUserPicture = (ImageView) findViewById(R.id.imageview_data_collection_user_picture);
		spinnerActivity = (Spinner) findViewById(R.id.spinner_data_collection_activity);
		etLabel = (EditText) findViewById(R.id.edittext_data_collection_label);
		llStartStop = (LinearLayout) findViewById(R.id.linearlayout_data_collection_start_stop);
		tvStartStopButton = (TextView) findViewById(R.id.textview_button_start_stop);
		tvStartStopButtonMessage = (TextView) findViewById(R.id.textview_button_start_stop_message);
		ibVibration = (ImageButton) findViewById(R.id.imagebutton_data_collection_vibration);
		ibSound = (ImageButton) findViewById(R.id.imagebutton_data_collection_sound);
		tvTimer = (TextView) findViewById(R.id.textview_data_collection_timer);
		ivTimerCheckbox = (ImageView) findViewById(R.id.imageview_data_collection_timer_checkbox);

		initializeClickListeners();

		isVibrationEnabled = getApplicationContext().getSharedPreferences(Constants.PREFERENCES_KEY_APPLICATION, Context.MODE_PRIVATE)
				.getBoolean(Constants.KEY_VIBRATION, true);
		isSoundEnabled = getApplicationContext().getSharedPreferences(Constants.PREFERENCES_KEY_APPLICATION, Context.MODE_PRIVATE)
				.getBoolean(Constants.KEY_SOUND, true);
		isFirstCheck = true;

		vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

		updateVibrationIcon();
		updateSoundIcon();

		startService();
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		setSpinner();

		// Check if a profile is present or not
		String profile = getApplicationContext().getSharedPreferences(Constants.PREFERENCES_KEY_APPLICATION, Context.MODE_PRIVATE)
				.getString(Constants.KEY_PROFILE, Constants.KEY_NO_PROFILE);

		if (profile.compareTo(Constants.KEY_NO_PROFILE) == 0)
		{
			Intent intent = new Intent(ActivityDataCollection.this, ActivityProfile.class);
			startActivity(intent);
		}
		else
		{
			loadProfile(profile);
		}
	}

	@Override
	public void onDestroy()
	{
		unbindFromService();

		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_main, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Handle item selection
		switch (item.getItemId())
		{
			case R.id.menu_select_profile:

				Intent intent = new Intent(ActivityDataCollection.this, ActivityProfile.class);
				startActivity(intent);

				return true;

			case R.id.menu_view_data:

				Intent intentChart = new Intent(ActivityDataCollection.this, ActivityBrowse.class);
				intentChart.putExtra(ActivityBrowse.KEY_DISPLAY_FOLDERS, true);
				intentChart.putExtra(ActivityBrowse.KEY_DELETING, false);
				intentChart.putExtra(ActivityBrowse.KEY_FOLDER_NAME, mProfile.getProfilePath());
				startActivity(intentChart);

				return true;

			case R.id.menu_settings:

				Intent intentSettings = new Intent(ActivityDataCollection.this, ActivitySettings.class);
				startActivity(intentSettings);

				return true;

			case R.id.menu_send_feedback:

				final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
				String[] emailAdresses =
				{ "ankit@usc.edu" };
				emailIntent.setType("plain/text");
				emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Movement Trackr: Feedback / Comment");
				emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, emailAdresses);
				startActivity(Intent.createChooser(emailIntent, "Send mail..."));

				return true;

			case R.id.menu_exit:

				this.finish();

				return true;

			default:

				return super.onOptionsItemSelected(item);
		}
	}

	private void setSpinner()
	{
		activities = new ArrayList<String>();

		String stringActivities = sharedPreferences.getString(Constants.KEY_ADD_ACTIVTY, "");

		String[] predefinedActivities = getResources().getStringArray(R.array.array_activities);
		String[] customActivities = stringActivities.split(",");

		// Add predefined activities
		for (int i = 0; i < predefinedActivities.length; i++)
		{
			activities.add(predefinedActivities[i]);
		}

		// Add custom activities
		for (int i = 0; i < customActivities.length; i++)
		{
			if (customActivities[i].length() > 0)
			{
				activities.add(customActivities[i]);
			}
		}

		ArrayAdapter<String> activityAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, activities);

		spinnerActivity.setAdapter(activityAdapter);

		System.gc();
	}

	private void loadProfile(String profileName)
	{
		File profile = new File(mainPath + "/" + profileName + "/.profile");
		String fullText = "";
		String line = "";

		if (profile.exists())
		{
			FileInputStream fstream;
			try
			{
				fstream = new FileInputStream(profile);

				DataInputStream in = new DataInputStream(fstream);
				BufferedReader br = new BufferedReader(new InputStreamReader(in));

				while ((line = br.readLine()) != null)
				{
					fullText += line;
				}

				fstream.close();

				mProfile = new Profile(profileName, fullText);
			}
			catch (Exception e)
			{
				Log.e("ACTIVITY_PROFILE", "Error reading profile. Error : " + e.getMessage());
			}
		}
		else
		{
			Toast.makeText(ActivityDataCollection.this,
					"Error loading profile. Profile does not exists. Please select a profile using Menu > Select Profile.",
					Toast.LENGTH_LONG).show();
			Log.e("ACTIVITY_PROFILE", "Profile '" + profileName + "' does not exists. Creating new.");
		}

		displayProfile();

		// Load profile avatar
		File file = new File(mainPath + "/" + profileName + "/avatar.jpg");
		if (file.exists())
		{
			Drawable drawable = new BitmapDrawable(mainPath + "/" + profileName + "/avatar.jpg");
			ivUserPicture.setImageDrawable(drawable);
		}
		else
		{
			ivUserPicture.setImageResource(R.drawable.userdetails_avatarplaceholder);
		}
	}

	private void displayProfile()
	{
		tvProfileName.setText(mProfile.getProfileName());
		tvFirstName.setText(mProfile.getFirstName());
		tvLastName.setText(mProfile.getLastName());
	}

	private void initializeClickListeners()
	{
		llStartStop.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if (mService != null)
				{
					if (mService.isCollectingData())
					{
						stopCollectingData();
					}
					else
					{
						if (isFirstCheck)
						{
							isFirstCheck = false;
							hasStarted = false;

							llStartStop.setBackgroundResource(R.drawable.background_confirm);

							tvStartStopButton.setText("Confirm");

							new Thread()
							{
								@Override
								public void run()
								{
									int check_interval = 100;
									int ticks = BUTTON_CLICK_TIME / check_interval;

									// Check if 5 seconds are over or not
									while (!isFirstCheck && (ticks > 0))
									{
										// Display the message if exacts to a
										// second
										if ((ticks % 10) == 0)
										{
											final int milliSeconds = ticks;
											runOnUiThread(new Runnable()
											{
												public void run()
												{
													tvStartStopButtonMessage.setVisibility(View.VISIBLE);
													tvStartStopButtonMessage.setText("Click within " + (milliSeconds / 10)
															+ " seconds to start");
												}
											});
										}

										try
										{
											Thread.sleep(check_interval);
										}
										catch (InterruptedException e)
										{
										}

										ticks--;
									}

									isFirstCheck = true;

									// Check if Service is not collecting the
									// data
									if ((mService != null) && (!hasStarted))
									{
										if (!mService.isCollectingData())
										{
											runOnUiThread(new Runnable()
											{
												public void run()
												{
													hasStarted = false;

													try
													{
														if (tvStartStopButton != null)
														{
															tvStartStopButton.setText("Start");
														}
														if (tvStartStopButtonMessage != null)
														{
															tvStartStopButtonMessage.setVisibility(View.GONE);
														}
														if (llStartStop != null)
														{
															llStartStop.setBackgroundResource(R.drawable.background_start);
														}

													}
													catch (Exception ex)
													{

													}
												}
											});
										}
									}
								}
							}.start();
						}
						else
						{
							isFirstCheck = true;
							hasStarted = true;
							tvStartStopButtonMessage.setVisibility(View.GONE);
							startCollectingData();

							if (isVibrationEnabled)
							{
								vibrate(1500);
							}

							if (isSoundEnabled)
							{
								playAudio(ActivityDataCollection.this, R.raw.beep);
							}
						}
					}
				}
			}
		});

		ibVibration.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				isVibrationEnabled = !isVibrationEnabled;

				Editor editor = getApplicationContext().getSharedPreferences(Constants.PREFERENCES_KEY_APPLICATION, Context.MODE_PRIVATE)
						.edit();
				editor.putBoolean(Constants.KEY_VIBRATION, isVibrationEnabled);
				editor.commit();

				updateVibrationIcon();
			}
		});

		ibSound.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				isSoundEnabled = !isSoundEnabled;

				Editor editor = getApplicationContext().getSharedPreferences(Constants.PREFERENCES_KEY_APPLICATION, Context.MODE_PRIVATE)
						.edit();
				editor.putBoolean(Constants.KEY_SOUND, isSoundEnabled);
				editor.commit();

				updateSoundIcon();
			}
		});

		if (sharedPreferences.getBoolean(Constants.KEY_TIMER, false))
		{
			long time = sharedPreferences.getLong(Constants.KEY_TIMER_TIME, 0);

			int seconds = (int) (time % 60);
			time /= 60;
			int minutes = (int) (time % 60);
			time /= 60;
			int hours = (int) time;

			final String secondsString = (seconds < 10) ? "0" + String.valueOf(seconds) : String.valueOf(seconds);
			final String minutesString = (minutes < 10) ? "0" + String.valueOf(minutes) : String.valueOf(minutes);

			timerString = hours + ":" + minutesString + ":" + secondsString;

			tvTimer.setText(timerString);

			ivTimerCheckbox.setImageResource(R.drawable.checkbox_on);
		}
		else
		{
			tvTimer.setText("0:00:00");

			ivTimerCheckbox.setImageResource(R.drawable.checkbox_off);
		}

		ivTimerCheckbox.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				final Dialog dialog = new Dialog(ActivityDataCollection.this);

				dialog.setContentView(R.layout.timer_picker);
				dialog.setTitle("Custom Dialog");

				Button buttonEnable = (Button) dialog.findViewById(R.id.button_dialog_enable);
				Button buttonDisable = (Button) dialog.findViewById(R.id.button_dialog_disable);

				final NumberPicker npHours = (NumberPicker) dialog.findViewById(R.id.numberpicker_dialog_hours);
				final NumberPicker npMinutes = (NumberPicker) dialog.findViewById(R.id.numberpicker_dialog_minutes);
				final NumberPicker npSeconds = (NumberPicker) dialog.findViewById(R.id.numberpicker_dialog_seconds);

				npHours.setRange(0, 24);

				if (sharedPreferences.getBoolean(Constants.KEY_TIMER, false))
				{
					long time = sharedPreferences.getLong(Constants.KEY_TIMER_TIME, 0);

					int seconds = (int) (time % 60);
					time /= 60;
					int minutes = (int) (time % 60);
					time /= 60;
					int hours = (int) time;

					npHours.setCurrent(hours);
					npMinutes.setCurrent(minutes);
					npSeconds.setCurrent(seconds);
				}
				else
				{
					npHours.setCurrent(0);
					npMinutes.setCurrent(0);
					npSeconds.setCurrent(0);
				}

				buttonDisable.setOnClickListener(new OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						Editor editor = getApplicationContext().getSharedPreferences(Constants.PREFERENCES_KEY_APPLICATION,
								Context.MODE_PRIVATE).edit();

						// Use first element as default
						editor.putBoolean(Constants.KEY_TIMER, (Boolean) false);

						// Commit the changes
						editor.commit();

						ivTimerCheckbox.setImageResource(R.drawable.checkbox_off);

						timerString = "0:00:00";
						tvTimer.setText(timerString);

						dialog.dismiss();
					}
				});

				buttonEnable.setOnClickListener(new OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						if ((npHours.getCurrent() * 3600 + npMinutes.getCurrent() * 60 + npSeconds.getCurrent()) <= 0)
						{
							Toast.makeText(ActivityDataCollection.this, "Time can not be 0", Toast.LENGTH_SHORT).show();
						}
						else
						{
							long time = npHours.getCurrent() * 3600 + npMinutes.getCurrent() * 60 + npSeconds.getCurrent();

							Editor editor = getApplicationContext().getSharedPreferences(Constants.PREFERENCES_KEY_APPLICATION,
									Context.MODE_PRIVATE).edit();

							editor.putBoolean(Constants.KEY_TIMER, true);

							// Save the time
							editor.putLong(Constants.KEY_TIMER_TIME, time);

							// Commit the changes
							editor.commit();

							final String secondsString = (npSeconds.getCurrent() < 10) ? "0" + String.valueOf(npSeconds.getCurrent())
									: String.valueOf(npSeconds.getCurrent());
							final String minutesString = (npMinutes.getCurrent() < 10) ? "0" + String.valueOf(npMinutes.getCurrent())
									: String.valueOf(npMinutes.getCurrent());

							timerString = npHours.getCurrent() + ":" + minutesString + ":" + secondsString;
							tvTimer.setText(timerString);

							ivTimerCheckbox.setImageResource(R.drawable.checkbox_on);

							dialog.dismiss();
						}
					}
				});

				dialog.show();
			}
		});
	}

	private void updateSoundIcon()
	{
		if (isSoundEnabled)
		{
			ibSound.setBackgroundResource(R.drawable.sound_on);
		}
		else
		{
			ibSound.setBackgroundResource(R.drawable.sound_off);
		}
	}

	private void updateVibrationIcon()
	{
		if (isVibrationEnabled)
		{
			ibVibration.setBackgroundResource(R.drawable.vibration_on);
		}
		else
		{
			ibVibration.setBackgroundResource(R.drawable.vibration_off);
		}
	}

	private static void playAudio(Context context, int resource)
	{
		mMediaPlayer = MediaPlayer.create(context, resource);
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					mMediaPlayer.start();

					while (mMediaPlayer.isPlaying())
					{
						Thread.sleep(2000);
					}

					releaseAudio();
				}
				catch (Exception e)
				{
					Log.e("Sound", e.toString());
				}
			}
		}).start();
	}

	/**
	 * Releases audio player instance created
	 */
	public static void releaseAudio()
	{
		mMediaPlayer.release();
	}

	private void vibrate(int milliseconds)
	{
		vibrator.vibrate(milliseconds);
	}

	private void startService()
	{
		// Start the service (even if already started
		Intent intent = new Intent(ActivityDataCollection.this, ServiceSensors.class);
		startService(intent);

		// Bind to the service to get sensor values
		new Thread()
		{
			@Override
			public void run()
			{
				bindService(new Intent(ActivityDataCollection.this, ServiceSensors.class), mConnection, Context.BIND_AUTO_CREATE);
			}
		}.start();
	}

	private void unbindFromService()
	{
		// Check if service is not null
		if (mService != null)
		{
			// Check if data is still being collected
			if (mService.isCollectingData())
			{
				// Stop Collecting Data
				mService.stopCollectingData();
			}

			mService.removeOnServiceSensorListener();
		}

		// Check if connection is not null
		if (mConnection != null)
		{
			// Unbind the service
			unbindService(mConnection);
		}

		mConnection = null;
		mService = null;
	}

	public void startCollectingData()
	{
		if (mService != null)
		{
			tvStartStopButton.setText("Stop");
			llStartStop.setBackgroundResource(R.drawable.background_stop);
			tvStartStopButtonMessage.setText("");
			spinnerActivity.setEnabled(false);
			etLabel.setEnabled(false);

			String activity = (String) spinnerActivity.getSelectedItem();
			String label = etLabel.getEditableText().toString();
			String name = mProfile.getLastName() + "_" + mProfile.getFirstName();

			activity = activity.replace(' ', '_');

			mService.startCollectingData(true, mProfile.getProfileName(), name, activity, label);
		}
	}

	public void stopCollectingData()
	{
		if (mService != null)
		{
			if (timerString.length() > 0)
			{
				tvTimer.setText(timerString);
			}
			else
			{
				tvTimer.setText("0:00:00");
			}
			tvStartStopButton.setText("Start");
			llStartStop.setBackgroundResource(R.drawable.background_start);
			spinnerActivity.setEnabled(true);
			etLabel.setEnabled(true);

			mService.stopCollectingData();
		}
	}

	private final ServiceSensorListener mListener = new ServiceSensorListener()
	{

		@Override
		public void onMessage(int message)
		{
			Toast.makeText(ActivityDataCollection.this, "Sampling Rate : " + message, Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onGyroscopeReadings(int serviceCode)
		{
			// TODO Auto-generated method stub
		}

		@Override
		public void onCompassReadings(int readings)
		{
			// TODO Auto-generated method stub
		}

		@Override
		public void onAccelerometerReadings(int test)
		{
			// TODO Auto-generated method stub
		}

		@Override
		public void onTimerTimeUpdate(long time)
		{
			final long timeOriginal = time;
			int seconds = (int) (time % 60);
			time /= 60;
			int minutes = (int) (time % 60);
			time /= 60;
			final int hours = (int) (time % 60);

			final String secondsString = (seconds < 10) ? "0" + String.valueOf(seconds) : String.valueOf(seconds);
			final String minutesString = (minutes < 10) ? "0" + String.valueOf(minutes) : String.valueOf(minutes);

			runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					tvTimer.setText(hours + ":" + minutesString + ":" + secondsString);

					if (timeOriginal == 0)
					{
						stopCollectingData();
					}
				}
			});
		}
	};

	private ServiceConnection mConnection = new ServiceConnection()
	{
		@Override
		public void onServiceConnected(ComponentName className, IBinder service)
		{
			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service. Because we have bound to a explicit
			// service that we know is running in our own process, we can
			// cast its IBinder to a concrete class and directly access it.
			mService = ((ServiceSensors.LocalBinder) service).getService();

			// Create a connection to the service
			mService.setOnServiceSensorListener(mListener);

			Log.i("ACTIVITY_DATA_COLLECTION", "Service bounded");
		}

		@Override
		public void onServiceDisconnected(ComponentName className)
		{
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			// Because it is running in our same process, we should never
			// see this happen.
			mService = null;

			Log.e("ACTIVTY_DATA_COLLECTION", "Service crashed unexpectedly.");
		}
	};

}