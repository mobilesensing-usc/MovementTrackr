package com.resl.sensors;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.SharedPreferences.Editor;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.Toast;

import com.resl.plotter.DataViewer;

public class ActivityBrowse extends ListActivity
{
	private static String TAG = "ACTIVITY_BROWSE";

	public static String KEY_DISPLAY_FOLDERS = "key_display_folders";
	public static String KEY_FOLDER_NAME = "key_folder_name";
	public static String KEY_DELETING = "key_deleting";

	private String folderPath;
	private boolean isDisplayingFolders;
	private boolean isDeleting;

	private ProgressDialog pDialog;

	Button buttonDelete;

	ArrayList<String> mList;
	ArrayList<Integer> mListChecked;
	ArrayAdapter<String> adapter;

	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_browse);

		Bundle extras = getIntent().getExtras();

		if (extras != null)
		{
			isDisplayingFolders = extras.getBoolean(KEY_DISPLAY_FOLDERS);
			folderPath = extras.getString(KEY_FOLDER_NAME);
			isDeleting = extras.getBoolean(KEY_DELETING);
		}

		mList = new ArrayList<String>();
		mListChecked = new ArrayList<Integer>();

		if (!isDeleting)
		{
			adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mList);
		}
		else
		{
			adapter = new ArrayAdapter<String>(this, R.layout.simple_checkbox_item, mList);
			buttonDelete = (Button) findViewById(R.id.button_browse_delete);

			buttonDelete.setVisibility(View.VISIBLE);

			buttonDelete.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					// Delete all the files included in the delete list
					for (int i = mListChecked.size() - 1; i >= 0; i--)
					{
						String fileName = folderPath + "/" + mList.get(mListChecked.get(i));

						Utility.deleteRecursively(fileName, false);

						File file = new File(fileName);
						file.delete();
					}

					ActivityBrowse.this.finish();
				}
			});
		}

		setListAdapter(adapter);
	}

	@Override
	public void onResume()
	{
		super.onResume();

		File mFiles = new File(folderPath);

		mList.clear();
		mListChecked.clear();

		if (mFiles.exists())
		{
			File[] children = mFiles.listFiles();

			for (int i = 0; i < children.length; i++)
			{
				// Check if folders have to be displayed
				if (children[i].isDirectory() && isDisplayingFolders)
				{
					mList.add(children[i].getName());
				}
				// Check if files have to be displayed
				else if (children[i].isFile() && !isDisplayingFolders)
				{
					mList.add(children[i].getName());
				}
			}
		}
		else
		{
			Log.e(TAG, "Profile Folder does not exist");
		}

		adapter.notifyDataSetChanged();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		if (!isDeleting)
		{
			MenuInflater inflater = getMenuInflater();
			inflater.inflate(R.menu.menu_browse, menu);
		}

		return true;
	}

	// Not using context menus anymore
	/*
	 * 
	 * @Override
	 * public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo
	 * menuInfo)
	 * {
	 * super.onCreateContextMenu(menu, v, menuInfo);
	 * MenuInflater inflater = getMenuInflater();
	 * inflater.inflate(R.menu.menu_browse_context, menu);
	 * }
	 * 
	 * @Override
	 * public boolean onContextItemSelected(MenuItem item)
	 * {
	 * AdapterContextMenuInfo info = (AdapterContextMenuInfo)
	 * item.getMenuInfo();
	 * 
	 * switch (item.getItemId())
	 * {
	 * case R.id.menu_browse_display_sensor_data:
	 * 
	 * final String fileName = folderPath + "/" + mList.get(info.position);
	 * 
	 * pDialog = ProgressDialog.show(ActivityBrowse.this, "",
	 * "Please wait while we fetch file details !", true);
	 * pDialog.setIndeterminate(true);
	 * pDialog.setCancelable(true);
	 * 
	 * new Thread()
	 * {
	 * 
	 * @Override
	 * public void run()
	 * {
	 * // Read the file and get readings to display
	 * if (!getReadingOptions(fileName))
	 * {
	 * runOnUiThread(new Runnable()
	 * {
	 * 
	 * @Override
	 * public void run()
	 * {
	 * Toast.makeText(ActivityBrowse.this,
	 * "Not enough samples to display the graph.", Toast.LENGTH_SHORT)
	 * .show();
	 * }
	 * });
	 * }
	 * }
	 * }.start();
	 * 
	 * return true;
	 * 
	 * case R.id.menu_browse_display_gps_data:
	 * 
	 * // Open maps activity
	 * String fileNameMaps = folderPath + "/" + mList.get(info.position);
	 * 
	 * Intent mapsIntent = new Intent(ActivityBrowse.this, ActivityMaps.class);
	 * mapsIntent.putExtra(ActivityMaps.KEY_FILE_NAME, fileNameMaps);
	 * 
	 * startActivity(mapsIntent);
	 * 
	 * return true;
	 * default:
	 * return super.onContextItemSelected(item);
	 * }
	 * }
	 */

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Handle item selection
		switch (item.getItemId())
		{
			case R.id.menu_browse_delete_all:

				final AlertDialog.Builder builder = new AlertDialog.Builder(ActivityBrowse.this);

				builder.setMessage("This will delete all files and folders in the current folder. Are you sure you want to delete?")
						.setCancelable(false).setPositiveButton("Yes", new DialogInterface.OnClickListener()
						{
							public void onClick(DialogInterface dialog, int id)
							{
								// Delete whole folder
								Utility.deleteRecursively(folderPath, false);

								// Finish up this activity
								ActivityBrowse.this.finish();
							}
						}).setNegativeButton("No", new DialogInterface.OnClickListener()
						{
							public void onClick(DialogInterface dialog, int id)
							{
								dialog.cancel();
							}
						});

				builder.show();

				return true;

			case R.id.menu_browse_delete_selected:

				Intent intentChart = new Intent(ActivityBrowse.this, ActivityBrowse.class);

				// Check if displaying a folder already or not
				if (isDisplayingFolders)
				{
					intentChart.putExtra(ActivityBrowse.KEY_DISPLAY_FOLDERS, true);
				}
				else
				{
					intentChart.putExtra(ActivityBrowse.KEY_DISPLAY_FOLDERS, false);
				}
				intentChart.putExtra(ActivityBrowse.KEY_DELETING, true);
				intentChart.putExtra(ActivityBrowse.KEY_FOLDER_NAME, folderPath);
				startActivity(intentChart);

				return true;

			default:

				return super.onOptionsItemSelected(item);
		}
	}

	private boolean getReadingOptions(final String name)
	{
		final FileData fileData = new FileData();

		FileInputStream fis;
		BufferedInputStream bis;
		DataInputStream dis;

		int fileLength = 0;

		try
		{
			fis = new FileInputStream(name);

			bis = new BufferedInputStream(fis);
			dis = new DataInputStream(bis);

			String splitString[] = null;

			splitString = dis.readLine().split(",");
			fileData.setProfileName((splitString.length > 1) ? splitString[1] : "");

			splitString = dis.readLine().split(",");
			fileData.setUserName((splitString.length > 1) ? splitString[1] : "");

			splitString = dis.readLine().split(",");
			fileData.setActivityType((splitString.length > 1) ? splitString[1] : "");

			splitString = dis.readLine().split(",");
			fileData.setLabel((splitString.length > 1) ? splitString[1] : "");

			splitString = dis.readLine().split(",");
			fileData.setStartTime((splitString.length > 1) ? splitString[1] : "");

			splitString = dis.readLine().split(",");
			fileData.setGyroscopePresent((splitString.length > 1) ? Boolean.parseBoolean(splitString[1]) : false);

			splitString = dis.readLine().split(",");
			fileData.setGyroscopeRotated((splitString.length > 1) ? Boolean.parseBoolean(splitString[1]) : false);

			splitString = dis.readLine().split(",");
			fileData.setDelayGyroscope((splitString.length > 1) ? splitString[1] : "");

			splitString = dis.readLine().split(",");
			fileData.setAccelerometerPresent((splitString.length > 1) ? Boolean.parseBoolean(splitString[1]) : false);

			splitString = dis.readLine().split(",");
			fileData.setAccelerometerRotated((splitString.length > 1) ? Boolean.parseBoolean(splitString[1]) : false);

			splitString = dis.readLine().split(",");
			fileData.setDelayAccelerometer((splitString.length > 1) ? splitString[1] : "");

			splitString = dis.readLine().split(",");
			fileData.setLoggingEnabled((splitString.length > 1) ? Boolean.parseBoolean(splitString[1]) : false);

			fileLength += DataViewer.HEADER_SIZE;

			while (dis.readLine() != null)
			{
				fileLength++;
			}

			Log.e(TAG, "Length : " + fileLength);

			int downSampleLength = ((fileLength - (2 * DataViewer.INITIAL_OFFSET) - DataViewer.HEADER_SIZE) / DataViewer.DOWNSAMPLE_RATE);

			if (downSampleLength <= 0)
			{
				if (pDialog != null)
				{
					if (pDialog.isShowing())
					{
						pDialog.dismiss();
					}
				}

				return false;
			}

			fileData.setDownSampleLength(downSampleLength);

			// Close the file
			dis.close();
			bis.close();
			fis.close();

			// Generate options
			int counter = 0;

			counter += fileData.isGyroscopePresent() ? 1 : 0;
			counter += fileData.isGyroscopeRotated() ? 1 : 0;
			counter += fileData.isAccelerometerPresent() ? 1 : 0;
			counter += fileData.isAccelerometerRotated() ? 1 : 0;

			final String[] options = new String[3 * counter];
			final boolean[] optionsChecked = new boolean[3 * counter];

			// Initialize checked options
			// Set all to 1
			for (int i = 0; i < optionsChecked.length; i++)
			{
				optionsChecked[i] = true;
			}

			counter = 0;

			if (fileData.isGyroscopePresent())
			{
				options[counter++] = "Gyroscope X";
				options[counter++] = "Gyroscope Y";
				options[counter++] = "Gyroscope Z";
			}

			if (fileData.isGyroscopeRotated())
			{
				options[counter++] = "Rotated Gyroscope X";
				options[counter++] = "Rotated Gyroscope Y";
				options[counter++] = "Rotated Gyroscope Z";
			}

			if (fileData.isAccelerometerPresent())
			{
				options[counter++] = "Accelerometer X";
				options[counter++] = "Accelerometer Y";
				options[counter++] = "Accelerometer Z";
			}

			if (fileData.isAccelerometerRotated())
			{
				options[counter++] = "Rotated Accelerometer X";
				options[counter++] = "Rotated Accelerometer Y";
				options[counter++] = "Rotated Accelerometer Z";
			}

			runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					if (pDialog != null)
					{
						if (pDialog.isShowing())
						{
							pDialog.dismiss();
						}
					}

					AlertDialog.Builder builder = new AlertDialog.Builder(ActivityBrowse.this);
					builder.setTitle("Pick a color");

					builder.setMultiChoiceItems(options, optionsChecked, new OnMultiChoiceClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which, boolean isChecked)
						{
						}

					}).setPositiveButton("Display", new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							ListView list = ((AlertDialog) dialog).getListView();

							long[] checkedOptions = list.getCheckItemIds();

							fileData.setOptionsSelected(checkedOptions);

							Intent intentChart = new DataViewer(name, fileData).execute(ActivityBrowse.this);

							if (intentChart != null)
							{
								startActivity(intentChart);
							}
						}
					}).setNegativeButton("Cancel", new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							dialog.dismiss();
						}
					});

					AlertDialog alert = builder.create();

					alert.show();
				}
			});

			return true;
		}
		catch (Exception e)
		{
			Log.e(TAG, "Error Reading File. Error : " + e.getMessage());
		}
		finally
		{
			if (pDialog != null)
			{
				if (pDialog.isShowing())
				{
					pDialog.dismiss();
				}
			}
		}

		return false;
	}

	@Override
	protected void onListItemClick(ListView l, View v, final int position, long id)
	{
		// Check that the file is not to be deleted
		if (!isDeleting)
		{
			// If not displaying the folders
			if (!isDisplayingFolders)
			{
				final CharSequence[] items =
				{ "Display Sensor Data", "Display GPS Data", "E-Mail File as Attachment", "Delete File", "Cancel" };

				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle("Choose Action...");

				builder.setItems(items, new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int item)
					{
						dialog.dismiss();

						final String fileName = folderPath + "/" + mList.get(position);

						switch (item)
						{
							case 0:

								pDialog = ProgressDialog.show(ActivityBrowse.this, "", "Please wait while we fetch file details !", true);
								pDialog.setIndeterminate(true);
								pDialog.setCancelable(true);

								new Thread()
								{
									@Override
									public void run()
									{
										// Read the file and get readings to
										// display
										if (!getReadingOptions(fileName))
										{
											runOnUiThread(new Runnable()
											{
												@Override
												public void run()
												{
													Toast.makeText(ActivityBrowse.this, "Not enough samples to display the graph.",
															Toast.LENGTH_SHORT).show();
												}
											});
										}
									}
								}.start();

								break;

							case 1:

								// Open maps activity
								Intent mapsIntent = new Intent(ActivityBrowse.this, ActivityMaps.class);
								mapsIntent.putExtra(ActivityMaps.KEY_FILE_NAME, fileName);

								startActivity(mapsIntent);

								break;

							case 2:
								// Send file as attachment
								Intent intentEmail = new Intent(Intent.ACTION_SEND);
								intentEmail.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								intentEmail.setType("text/csv");
								intentEmail.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + fileName));
								intentEmail.putExtra(android.content.Intent.EXTRA_SUBJECT, "Movement Trackr: File - " + mList.get(position));
								startActivity(Intent.createChooser(intentEmail, "Send mail..."));

								break;
							case 3:

								final AlertDialog.Builder builder = new AlertDialog.Builder(ActivityBrowse.this);

								builder.setMessage("Are you sure you want to delete the file : " + mList.get(position) + " ? ")
										.setCancelable(false).setPositiveButton("Yes", new DialogInterface.OnClickListener()
										{
											public void onClick(DialogInterface dialog, int id)
											{
												// Delete the file
												File file = new File(fileName);

												if (file.exists())
												{
													file.delete();
												}

												// Delete the file from the list
												mList.remove(position);

												adapter.notifyDataSetChanged();
											}
										}).setNegativeButton("No", new DialogInterface.OnClickListener()
										{
											public void onClick(DialogInterface dialog, int id)
											{
												dialog.cancel();
											}
										});

								AlertDialog alert = builder
								.create();
								
								alert.show();

								break;

							default:
								break;
						}
					}
				});

				AlertDialog alertContextOptions = builder.create();

				alertContextOptions.show();
			}
			else
			{
				// Explore the content inside
				Intent intentChart = new Intent(ActivityBrowse.this, ActivityBrowse.class);
				intentChart.putExtra(ActivityBrowse.KEY_DISPLAY_FOLDERS, false);
				intentChart.putExtra(ActivityBrowse.KEY_DELETING, false);
				intentChart.putExtra(ActivityBrowse.KEY_FOLDER_NAME, folderPath + "/" + mList.get(position));
				startActivity(intentChart);

				// Not displaying the folders, so display the file content
				// final String fileName = folderPath + "/" +
				// mList.get(position);
				//
				// pDialog = ProgressDialog.show(ActivityBrowse.this, "",
				// "Please wait while we fetch file details !", true);
				// pDialog.setIndeterminate(true);
				// pDialog.setCancelable(true);
				//
				// new Thread()
				// {
				// @Override
				// public void run()
				// {
				// // Read the file and get readings to display
				// if (!getReadingOptions(fileName))
				// {
				// runOnUiThread(new Runnable()
				// {
				// @Override
				// public void run()
				// {
				// Toast.makeText(ActivityBrowse.this,
				// "Not enough samples to display the graph.",
				// Toast.LENGTH_SHORT)
				// .show();
				// }
				// });
				// }
				// }
				// }.start();
				//
			}
		}
		else
		{
			// Delete the item in the list

			CheckedTextView checkedTextView = (CheckedTextView) v;

			if (checkedTextView != null)
			{
				if (checkedTextView.isChecked())
				{
					if (mListChecked.contains(position))
					{
						mListChecked.remove((Integer) position);
					}

					checkedTextView.setChecked(false);
				}
				else
				{
					checkedTextView.setChecked(true);

					if (!mListChecked.contains(position))
					{
						mListChecked.add((Integer) position);
					}
					else
					{
						Log.e(TAG, "Error : Item to be deleted already is in delete list");
					}
				}
			}

			checkedTextView = null;
		}
	}
}
