// Alter the files for writing fft and normal data
// check for flag such that if one fft is being written, other doesn't get
// hindered
// collect data irrespective of anything
// just add window partition

package com.resl.sensors;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.math.complex.Complex;
import org.apache.commons.math.transform.FastFourierTransformer;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

public class ServiceSensors extends Service
{
	public static final int TIMER_INTERVAL = 100;

	public static final String KEY_READINGS = "readings";

	public static final String GYROSCOPE_HEADER_RAW = "GYROSCOPE_X, GYROSCOPE_Y, GYROSCOPE_Z";
	public static final String GYROSCOPE_HEADER_ROTATED = "GYROSCOPE_X_ROTATED, GYROSCOPE_Y_ROTATED, GYROSCOPE_Z_ROTATED";

	public static final String ACCELEROMETER_HEADER_RAW = "ACCELEROMETER_X, ACCELEROMETER_Y, ACCELEROMETER_Z";
	public static final String ACCELEROMETER_HEADER_ROTATED = "ACCELEROMETER_X_ROTATED, ACCELEROMETER_Y_ROTATED, ACCELEROMETER_Z_ROTATED";

	public static final String LOCATION_HEADER_RAW = "LATITUDE, LONGITUDE";

	public static final int WINDOW_NONE = -1;
	public static final int WINDOW_MAX_SIZE = 10; // Max allowed time limit in
													// milliseconds
	public static final int MAX_DATA_SAMPLES = 8192;// WINDOW_MAX_SIZE * 900 /
													// 1000; // Assuming 900
													// samples per 1000
													// milliseconds

	public static final int MIN_ALLOWED_FREQUENCIES = 5;
	public static final int MAX_ALLOWED_FREQUENCIES = 150;

	// public static final String FILE_HEADER =
	// "ACCELEROMETER_X, ACCELEROMETER_Y, ACCELEROMETER_Z\n";

	public static final String[] KEY_DELAY_RATES =
	{ "DELAY_FASTEST", "DELAY_GAME", "DELAY_UI", "DELAY_NORMAL" };

	// Get window size in times
	// private int windowSize;

	private boolean flagCollectingData;

	private SensorManager sensorManager = null;
	private LocationManager locationManager = null;

	private ServiceSensorListener mListener;

	private long startTime, stopTime;

	private long totalReadings;

	// private File fileFFT;
	private File fileRaw;

	private FileWriter fileWriterFFT;
	private FileWriter fileWriterRaw;

	private float[] inverseRotationMatrix;

	private Location currentLocation;

	private float[] rawGyroReadings;
	private float[] rotatedGyroReadings;

	private float[] rawAccReadings;
	private float[] rotatedAccReadings;

	private double[] readDataOne_X;
	private double[] readDataTwo_X;

	private double[] readDataOne_Y;
	private double[] readDataTwo_Y;

	private boolean fileLockFFT;

	private boolean switchArrays;

	private Timer mTimer;
	private TimerTask mTimerTask;

	// Remaining time in seconds
	private long timerRemainingTime;
	private long timerIntervalTime;
	private boolean isTimerEnabled;

	private IBinder mBinder;

	private boolean isAllowedToChange;

	private String windowStartTime;

	private int dataCounter;

	org.apache.commons.math.transform.FastFourierTransformer fourierTransformer;

	SharedPreferences sharedPreferences;

	boolean isGyroscopeRecorded;
	boolean isAccelerometerRecorded;
	boolean isLocationRecorded;

	boolean isGyroscopeRotated;
	boolean isAccelerometerRotated;

	int delayGyroscope;
	int delayAccelerometer;

	String[] arraySensorType;
	String[] arrayDelayRate;

	PowerManager powerManager;
	PowerManager.WakeLock wakeLock;

	@Override
	public void onCreate()
	{
		super.onCreate();

		sharedPreferences = getSharedPreferences(Constants.PREFERENCES_KEY_APPLICATION, Activity.MODE_PRIVATE);

		// Get all the settings
		arraySensorType = getResources().getStringArray(R.array.array_sensor_type);
		arrayDelayRate = getResources().getStringArray(R.array.array_rate);

		powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SERVICE_SENSORS");

		mBinder = new LocalBinder();

		inverseRotationMatrix = new float[]
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0 };

		rawGyroReadings = new float[]
		{ 0, 0, 0 };
		rotatedGyroReadings = new float[]
		{ 0, 0, 0 };

		rawAccReadings = new float[]
		{ 0, 0, 0 };
		rotatedAccReadings = new float[]
		{ 0, 0, 0 };

		// Initialize flag to collect data
		flagCollectingData = false;

		// Initialize window size to -1
		// windowSize = -1;

		// Initialize change flag
		isAllowedToChange = true;

		// Initialize arrays switch
		switchArrays = false;

		// Get fourier transformer
		fourierTransformer = new FastFourierTransformer();
	}

	@Override
	public void onDestroy()
	{
		stopCollectingData();

		Log.e("SENSORS_SERVICE", "Service Destroyed");

		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent)
	{
		Log.i("SERVICE_SENSORS", "Service Bounded");

		return mBinder;
	}

	@Override
	public boolean onUnbind(Intent intent)
	{
		Log.i("SERVICE_SENSORS", "Service Unbounded");

		return true;
	}

	public class LocalBinder extends Binder
	{
		public ServiceSensors getService()
		{
			return ServiceSensors.this;
		}
	}

	private void getSettings()
	{
		String sensorType = sharedPreferences.getString(Constants.KEY_SENSOR_TYPE, arraySensorType[0]);

		// Check if not gyroscope only
		if (sensorType.compareTo(arraySensorType[0]) != 0)
		{
			// This is a accelerometer
			isAccelerometerRecorded = true;

			// Find out the delay rate
			for (int i = 0; i < arrayDelayRate.length; i++)
			{
				if (sharedPreferences.getString(Constants.KEY_RATE_ACCELEROMETER, arrayDelayRate[0]).compareTo(arrayDelayRate[i]) == 0)
				{
					delayAccelerometer = i;
					break;
				}
			}

			isAccelerometerRotated = sharedPreferences.getBoolean(Constants.KEY_ROTATED_ACCELEROMETER, false);
		}
		else
		{
			// This is accelerometer
			isAccelerometerRecorded = false;
		}

		// Check if not accelerometer only
		if (sensorType.compareTo(arraySensorType[1]) != 0)
		{
			// This is a gyroscope
			isGyroscopeRecorded = true;

			for (int i = 0; i < arrayDelayRate.length; i++)
			{
				if (sharedPreferences.getString(Constants.KEY_RATE_GYROSCOPE, arrayDelayRate[0]).compareTo(arrayDelayRate[i]) == 0)
				{
					delayGyroscope = i;
					break;
				}
			}

			isGyroscopeRotated = sharedPreferences.getBoolean(Constants.KEY_ROTATED_GYROSCOPE, false);
		}
		else
		{
			// This is accelerometer
			isGyroscopeRecorded = false;
		}

		if (sharedPreferences.getBoolean(Constants.KEY_TIMER, false))
		{
			isTimerEnabled = true;
			timerIntervalTime = sharedPreferences.getLong(Constants.KEY_TIMER_TIME, 0); // Convert
																						// time
																						// into
																						// seconds
		}
		else
		{
			isTimerEnabled = false;
		}

		isLocationRecorded = sharedPreferences.getBoolean(Constants.KEY_LOCATION, false);

		// Test all boolean values here
		Log.e("SERVICE_SENSORS", "Gyro Recorded : " + isGyroscopeRecorded + ", Gyro Rotated : " + isGyroscopeRotated + ", Delay Type : "
				+ delayGyroscope);
		Log.e("SERVICE_SENSORS", "Acc Recorded : " + isAccelerometerRecorded + ", Acc Rotated : " + isAccelerometerRotated
				+ ", Delay Type : " + delayAccelerometer);
	}

	public boolean isCollectingData()
	{
		return flagCollectingData;
	}

	public void setOnServiceSensorListener(ServiceSensorListener mListener)
	{
		this.mListener = mListener;
	}

	public void removeOnServiceSensorListener()
	{
		if (mListener != null)
		{
			this.mListener = null;
		}
	}

	public void startCollectingData(boolean isSaved, String username, String name, String activity, String label)
	{
		ServiceSensors.this.flagCollectingData = true;

		Log.e("SENSORS_SERVICE", "Acquiring Partial Wake Lock");

		wakeLock.acquire();

		Log.e("SENSORS_SERVICE", "Partial Wake Lock Acquired");

		// Get settings
		getSettings();

		// ServiceSensors.this.windowSize = windowSize;

		// long time = System.currentTimeMillis() + 6000;
		// ServiceSensors.this.windowEndTime = time - (time % 1000);

		// Initialize file lock
		fileLockFFT = false;

		// Initialize location
		currentLocation = new Location(LocationManager.GPS_PROVIDER);

		// Check if data is to be saved or not
		if (isSaved)
		{
			try
			{
				File directory = new File(Environment.getExternalStorageDirectory() + "/RESL_Data/" + username + "/" + activity + "/");

				if (!directory.exists())
				{
					directory.mkdir();
				}

				Date date = new Date();
				String fileName = android.text.format.DateFormat.format("yyyy_MM_dd_hh_mm_ss", date).toString();
				String header = "";

				header += "Profile name :," + username + "\nName :," + name + "\nActivity :," + activity + "\nLabel :," + label
						+ "\nStart Time :," + date.toString() + "\nEnable Gyroscope :," + isGyroscopeRecorded + "\nRotate Gyroscope :,"
						+ isGyroscopeRotated + "\nRate for Gyroscope :," + KEY_DELAY_RATES[delayGyroscope] + "\nEnable Accelerometer :,"
						+ isAccelerometerRecorded + "\nRotate Accelerometer :," + isAccelerometerRotated + "\nRate for Accelerometer :,"
						+ KEY_DELAY_RATES[delayAccelerometer] + "\nEnable Location :," + isLocationRecorded + "\n\n";

				fileRaw = new File(Environment.getExternalStorageDirectory() + "/RESL_Data/" + username + "/" + activity + "/" + fileName
						+ "_raw.csv");
				/*
				 * fileFFT = new File(Environment.getExternalStorageDirectory()
				 * + "/RESL_Data/" + username + "/" + activity + "/" + fileName
				 * + "_fft.csv");
				 * 
				 * if (!fileFFT.exists())
				 * {
				 * fileFFT.createNewFile();
				 * }
				 */

				if (!fileRaw.exists())
				{
					fileRaw.createNewFile();
				}

				// fileWriterFFT = new FileWriter(fileFFT);
				fileWriterRaw = new FileWriter(fileRaw);

				if (fileWriterRaw != null)
				{
					fileWriterRaw.write(header);

					if (isGyroscopeRecorded)
					{
						fileWriterRaw.write(GYROSCOPE_HEADER_RAW + ",,");

						if (isGyroscopeRotated)
						{
							fileWriterRaw.write(GYROSCOPE_HEADER_ROTATED + ",,");
						}
					}

					if (isAccelerometerRecorded)
					{
						fileWriterRaw.write(ACCELEROMETER_HEADER_RAW + ",,");

						if (isAccelerometerRotated)
						{
							fileWriterRaw.write(ACCELEROMETER_HEADER_ROTATED + ",,");
						}
					}

					if (isLocationRecorded)
					{
						fileWriterRaw.write(LOCATION_HEADER_RAW + ",,");
					}

					fileWriterRaw.write("\n");
				}

				if (fileWriterFFT != null)
				{
					fileWriterFFT.write(header);
				}

				// Initialize array to save the data
				// Large enough to hold data worth 10 seconds
				readDataOne_X = new double[MAX_DATA_SAMPLES];
				readDataTwo_X = new double[MAX_DATA_SAMPLES];

				readDataOne_Y = new double[MAX_DATA_SAMPLES];
				readDataTwo_Y = new double[MAX_DATA_SAMPLES];

				// Initialize data counter
				dataCounter = 0;
			}
			catch (Exception e)
			{
				Log.e("SERVICE_SENSORS", "Error opening the file : " + fileRaw.getAbsolutePath());
			}
		}

		sensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);

		if (isGyroscopeRecorded)
		{
			// Register listener for requested Sensor Type
			sensorManager.registerListener(sensorListener, sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), delayGyroscope);
		}

		if (isAccelerometerRecorded)
		{
			// Register listener for requested Sensor Type
			sensorManager.registerListener(sensorListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), delayAccelerometer);
		}

		if (isAccelerometerRotated || isGyroscopeRotated)
		{
			// Register listener for Rotation Matrix
			sensorManager.registerListener(sensorListener, sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
					SensorManager.SENSOR_DELAY_FASTEST);
		}

		if (isLocationRecorded)
		{
			locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

			// Register listener for Location Services
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 10.0f, locationListener);
		}

		if (isTimerEnabled)
		{
			timerRemainingTime = timerIntervalTime;

			mListener.onTimerTimeUpdate(timerRemainingTime);
		}
		else
		{
			timerRemainingTime = 0;
		}

		// Create new timer
		mTimer = new Timer();

		// Create new timer task
		mTimerTask = new TimerTask()
		{
			@Override
			public void run()
			{
				if (isTimerEnabled)
				{
					timerRemainingTime -= 1;
				}
				else
				{
					timerRemainingTime += 1;
				}

				if (mListener != null)
				{
					mListener.onTimerTimeUpdate(timerRemainingTime);
				}

				if (isTimerEnabled)
				{
					if (timerRemainingTime == 0)
					{
						mTimer.cancel();
					}
				}
			}
		};

		// Initialize Timer
		mTimer.schedule(mTimerTask, 0, 1000);

		// Initialize counters
		totalReadings = 0;

		startTime = System.currentTimeMillis();
	}

	public void stopCollectingData()
	{
		flagCollectingData = false;

		Log.e("SENSORS_SERVICE", "Releasing Partial Wake Lock");

		wakeLock.release();

		Log.e("SENSORS_SERVICE", "Partial Wake Lock Released");

		mTimer.cancel();

		sensorManager.unregisterListener(sensorListener);

		if (locationManager != null)
		{
			locationManager.removeUpdates(locationListener);
		}

		stopTime = System.currentTimeMillis();

		try
		{
			if (fileWriterFFT != null)
			{
				fileWriterFFT.write("\n\nTime Taken : " + (stopTime - startTime));
				fileWriterFFT.write("\nActual Readings : " + totalReadings);
				fileWriterFFT.write("\nActual Sampling Rate : " + (1000.0f * totalReadings) / (stopTime - startTime));

				fileWriterFFT.close();
			}

			if (fileWriterRaw != null)
			{
				fileWriterRaw.write("Time Taken : " + (stopTime - startTime));
				fileWriterRaw.write("\nActual Readings : " + totalReadings);
				fileWriterRaw.write("\nActual Sampling Rate : " + (1000.0f * totalReadings) / (stopTime - startTime));

				fileWriterRaw.close();
			}
		}
		catch (IOException e)
		{
			Log.e("SERVICE_SENSORS", "Error closing the file : " + e.getMessage());
		}

		// mListener
		// .onMessage((int) ((1000 * totalReadings) / (stopTime - startTime)));

		Log.e("SERVICE_SENSORS", "Time Taken : " + (stopTime - startTime));
		Log.e("SERVICE_SENSORS", "Total Number of Readings : " + totalReadings);
	}

	private void calculateRotation(SensorEvent event)
	{
		SensorManager.getRotationMatrixFromVector(inverseRotationMatrix, event.values);
	}

	private void calculateFFT(double[] readDataX, double[] readDataY, boolean threadOne)
	{
		long time = System.currentTimeMillis();

		Complex[] fftX = fourierTransformer.transform(readDataX);
		Complex[] fftY = fourierTransformer.transform(readDataY);

		float sum_fftX = 0;
		float sum_fftY = 0;

		int fft_length = 0;

		Log.e("SERVICE_SENSORS", "Writing FFT at : " + time);

		if (!fileLockFFT)
		{
			fileLockFFT = true;

			// Insert a break between windows. Write BEGIN
			if (fileWriterFFT != null)
			{
				try
				{
					fileWriterFFT.write("\n" + windowStartTime + ",,");
				}
				catch (IOException e)
				{
					Log.e("SERVICE_SENSORS", "Error writing to the FFT file. Error : " + e.getMessage());
				}
			}

			// Calculate the FFT sum for normalization
			fft_length = fftX.length / 2;

			for (int i = 0; i < fft_length; i++)
			{
				sum_fftX += fftX[i].abs();
				sum_fftY += fftY[i].abs();
			}

			// Write fftX
			if (fileWriterFFT != null)
			{
				try
				{
					// Write fftX only until limit to get rid of high
					// frequencies
					for (int i = MIN_ALLOWED_FREQUENCIES; i < MAX_ALLOWED_FREQUENCIES; i++)
					{
						fileWriterFFT.write(((float) (fftX[i].abs() / sum_fftX)) + ",");
					}
				}
				catch (IOException e)
				{
					Log.e("SERVICE_SENSORS", "Error writing FFT_X to the file. Error : " + e.getMessage());
				}
			}

			// Write fftY
			if (fileWriterFFT != null)
			{
				try
				{
					// Write fftY only until limit to get rid of high
					// frequencies
					for (int i = MIN_ALLOWED_FREQUENCIES; i < MAX_ALLOWED_FREQUENCIES; i++)
					{
						fileWriterFFT.write(((float) (fftY[i].abs() / sum_fftY)) + ",");
					}
				}
				catch (IOException e)
				{
					Log.e("SERVICE_SENSORS", "Error writing FFT_Y to the file. Error : " + e.getMessage());
				}
			}

			fileLockFFT = false;
		}

		long time2 = System.currentTimeMillis();

		Log.e("SERVICE_SENSORS", "Finished Writing FFT at " + time2);
		Log.e("SERVICE_SENSORS", "Time Taken : " + (time2 - time));
	}

	private void recordReadingsAcc(SensorEvent event)
	{
		int size = (int) Math.sqrt(inverseRotationMatrix.length);
		String data = "";

		rawAccReadings[0] = event.values[0];
		rawAccReadings[1] = event.values[1];
		rawAccReadings[2] = event.values[2];

		for (int i = 0; i < size; i++)
		{
			for (int j = 0; j < 1; j++)
			{
				rotatedAccReadings[i] = 0;

				for (int k = 0; k < event.values.length; k++)
				{
					rotatedAccReadings[i] += inverseRotationMatrix[i * size + k] * event.values[k];
				}
			}
		}

		if (fileWriterRaw != null)
		{
			if (isGyroscopeRecorded)
			{
				data += rawGyroReadings[0] + "," + rawGyroReadings[1] + "," + rawGyroReadings[2] + ",,";

				if (isGyroscopeRotated)
				{
					data += rotatedGyroReadings[0] + "," + rotatedGyroReadings[1] + "," + rotatedGyroReadings[2] + ",,";
				}
			}

			if (isAccelerometerRecorded)
			{
				data += rawAccReadings[0] + "," + rawAccReadings[1] + "," + rawAccReadings[2] + ",,";

				if (isAccelerometerRotated)
				{
					data += rotatedAccReadings[0] + "," + rotatedAccReadings[1] + "," + rotatedAccReadings[2] + ",,";
				}
			}

			if (isLocationRecorded)
			{
				data += currentLocation.getLatitude() + "," + currentLocation.getLongitude();
			}
		}

		// Log.i("SE0NSOR_READINGS", "Rotated Gyro Readings : " + data);

		if (fileWriterRaw != null)
		{
			try
			{
				fileWriterRaw.write(data + "\n");
			}
			catch (IOException e)
			{
				Log.e("SERVICE_SENSORS", "Error writing to the Raw file. Error : " + e.getMessage());
			}
		}
	}

	private void recordReadingsGyro(SensorEvent event)
	{
		int size = (int) Math.sqrt(inverseRotationMatrix.length);
		String data = "";

		rawGyroReadings[0] = event.values[0];
		rawGyroReadings[1] = event.values[1];
		rawGyroReadings[2] = event.values[2];

		for (int i = 0; i < size; i++)
		{
			for (int j = 0; j < 1; j++)
			{
				rotatedGyroReadings[i] = 0;

				for (int k = 0; k < event.values.length; k++)
				{
					rotatedGyroReadings[i] += inverseRotationMatrix[i * size + k] * event.values[k];
				}
			}
		}

		/*
		 * // Log.i("SERVICE_SENSORS", "Gyro    Values : " + event.values[0] +
		 * // " , "+ event.values[1] + " , " + event.values[2]);
		 * // Log.i("SERVICE_SENSORS", "Rotated Values : " +
		 * rotatedGyroReadings[0]
		 * // + " , "+ rotatedGyroReadings[1] + " , " + rotatedGyroReadings[2]);
		 * 
		 * 
		 * // if (windowSize > WINDOW_NONE)
		 * // {
		 * // Log.e("SERVICE_SENSORS", "Test Time : " + windowEndTime +
		 * // " , Current Time : " + currentTime);
		 * 
		 * // Check if new window is to be formed
		 * // Does not record FFT until we have desired number of samples
		 * if (dataCounter == MAX_DATA_SAMPLES)
		 * {
		 * Log.e("SERVICE_SENSORS", "Start");
		 * 
		 * // Update the window time
		 * // windowEndTime = currentTime + windowSize;
		 * 
		 * if (switchArrays)
		 * {
		 * new Thread()
		 * {
		 * 
		 * @Override
		 * public void run()
		 * {
		 * Log.e("SERVICE_SENSORS", "Beginning Thread One");
		 * 
		 * calculateFFT(readDataOne_X, readDataOne_Y, switchArrays);// ,
		 * // ServiceSensors.this.windowEndTime);
		 * 
		 * Log.e("SERVICE_SENSORS", "Ending Thread One");
		 * }
		 * }.start();
		 * }
		 * else
		 * {
		 * new Thread()
		 * {
		 * 
		 * @Override
		 * public void run()
		 * {
		 * Log.e("SERVICE_SENSORS", "Beginning Thread Two");
		 * 
		 * calculateFFT(readDataTwo_X, readDataTwo_Y, switchArrays);// ,
		 * // ServiceSensors.this.windowEndTime);
		 * 
		 * Log.e("SERVICE_SENSORS", "Ending Thread Two");
		 * }
		 * }.start();
		 * }
		 * 
		 * dataCounter = 0;
		 * 
		 * // (ServiceSensors.this.windowEndTime =
		 * ServiceSensors.this.windowEndTime + 10000;
		 * 
		 * switchArrays = !switchArrays;
		 * }
		 * 
		 * if (dataCounter == 0)
		 * {
		 * Date date = new Date();
		 * windowStartTime =
		 * android.text.format.DateFormat.format("MM/dd/yy hh:mm:ss",
		 * date).toString() + "."
		 * + String.valueOf(System.currentTimeMillis() % 1000);
		 * }
		 * 
		 * if (switchArrays)
		 * {
		 * readDataOne_X[dataCounter] = rotatedGyroReadings[0];
		 * readDataOne_Y[dataCounter++] = rotatedGyroReadings[1];
		 * }
		 * else
		 * {
		 * readDataTwo_X[dataCounter] = rotatedGyroReadings[0];
		 * readDataTwo_Y[dataCounter++] = rotatedGyroReadings[1];
		 * }
		 */

		if (fileWriterRaw != null)
		{
			if (isGyroscopeRecorded)
			{
				data += rawGyroReadings[0] + "," + rawGyroReadings[1] + "," + rawGyroReadings[2] + ",,";

				if (isGyroscopeRotated)
				{
					data += rotatedGyroReadings[0] + "," + rotatedGyroReadings[1] + "," + rotatedGyroReadings[2] + ",,";
				}
			}

			if (isAccelerometerRecorded)
			{
				data += rawAccReadings[0] + "," + rawAccReadings[1] + "," + rawAccReadings[2] + ",,";

				if (isAccelerometerRotated)
				{
					data += rotatedAccReadings[0] + "," + rotatedAccReadings[1] + "," + rotatedAccReadings[2] + ",,";
				}
			}

			if (isLocationRecorded)
			{
				data += currentLocation.getLatitude() + "," + currentLocation.getLongitude();
			}
		}

		// Log.i("SE0NSOR_READINGS", "Rotated Gyro Readings : " + data);

		if (fileWriterRaw != null)
		{
			try
			{
				fileWriterRaw.write(data + "\n");
			}
			catch (IOException e)
			{
				Log.e("SERVICE_SENSORS", "Error writing to the Raw file. Error : " + e.getMessage());
			}
		}
	}

	private SensorEventListener sensorListener = new SensorEventListener()
	{
		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy)
		{

		}

		@Override
		public void onSensorChanged(SensorEvent event)
		{
			synchronized (this)
			{
				totalReadings++;

				if (isAllowedToChange)
				{
					// Log.e("SERVICE_SENSORS", "Coming");

					// Assume previous condition and this to occur
					// simultaneously
					isAllowedToChange = false;

					if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE)
					{
						recordReadingsGyro(event);
					}
					if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
					{
						recordReadingsAcc(event);
					}
					else if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR)
					{
						calculateRotation(event);
					}

					isAllowedToChange = true;
				}
			}
		}
	};

	private LocationListener locationListener = new LocationListener()
	{

		@Override
		public void onLocationChanged(Location location)
		{
			currentLocation = new Location(location);
		}

		@Override
		public void onProviderDisabled(String provider)
		{

		}

		@Override
		public void onProviderEnabled(String provider)
		{

		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras)
		{

		}
	};

	public interface ServiceSensorListener
	{
		public void onMessage(int message);

		public void onAccelerometerReadings(int test);

		public void onGyroscopeReadings(int serviceCode);

		public void onCompassReadings(int readings);

		public void onTimerTimeUpdate(long time);
	}
}
