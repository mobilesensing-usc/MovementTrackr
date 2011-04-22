package com.resl.sensors;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class ActivityProfile extends Activity
{
	protected static final int ACTIVITY_SELECT_IMAGE = 0;
	protected static final int ACTIVITY_IMAGE_CAPTURE = 1;

	protected static final int MIN_AGE = 5;
	protected static final int MAX_AGE = 80;

	protected static final int MIN_HEIGHT = 3;
	protected static final int MAX_HEIGHT = 7;

	protected static final int MIN_WEIGHT = 45;
	protected static final int MAX_WEIGHT = 265;

	protected static final int MIN_LEG_LENGTH = 1;
	protected static final int MAX_LEG_LENGTH = 3;

	protected static final int AVATAR_WIDTH = 200;
	protected static final int AVATAR_HEIGHT = 200;

	protected static final String CREATE_NEW = "Create New ...";

	protected static final String DO_NOT_DISCLOSE = "Do Not Disclose...";

	protected static final String NOT_DISCLOSED = "Not Disclosed";
	protected static final String NOT_SPECIFIED = "Not Specified";

	Profile mProfile;

	Spinner sProfileSelector;

	ImageView ivAvatar;

	Button bUseExisting;
	Button bClickNew;
	Button bRevertBack;

	EditText etFirstName;
	EditText etLastName;
	TextView tvAge;
	TextView tvHeight;
	TextView tvWeight;
	TextView tvGender;
	TextView tvLegLength;
	TextView tvRace;

	Button bSaveProfile;
	Button bDeleteProfile;

	String[] itemsProfiles = null;

	String[] itemsAge = null;
	String[] itemsHeight = null;
	String[] itemsWeight = null;
	String[] itemsSex = null;
	String[] itemsLegLength = null;
	String[] itemsRace = null;

	String mainPath = Environment.getExternalStorageDirectory() + "/RESL_Data";
	
	int profilePosition;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_profile);

		// Get UI elements
		sProfileSelector = (Spinner) findViewById(R.id.spinner_profile_select_profile);

		ivAvatar = (ImageView) findViewById(R.id.imageview_profile_edit_avatar);

		bUseExisting = (Button) findViewById(R.id.button_profile_edit_use_existing);
		bClickNew = (Button) findViewById(R.id.button_profile_edit_click_new);
		bRevertBack = (Button) findViewById(R.id.button_profile_edit_cancel);

		etFirstName = (EditText) findViewById(R.id.edittext_profile_first_name);
		etLastName = (EditText) findViewById(R.id.edittext_profile_last_name);

		tvAge = (TextView) findViewById(R.id.textview_profile_age);
		tvHeight = (TextView) findViewById(R.id.textview_profile_height);
		tvWeight = (TextView) findViewById(R.id.textview_profile_weight);
		tvGender = (TextView) findViewById(R.id.textview_profile_gender);
		tvLegLength = (TextView) findViewById(R.id.textview_profile_leg_length);
		tvRace = (TextView) findViewById(R.id.textview_profile_race);

		bSaveProfile = (Button) findViewById(R.id.button_profile_save_changes);
		bDeleteProfile = (Button) findViewById(R.id.button_profile_delete);

		// Search for profile and populate itemsProfile list
		searchDirectories();

		ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(ActivityProfile.this, android.R.layout.simple_spinner_item,
				itemsProfiles);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		sProfileSelector.setAdapter(adapter);

		String profile = getProfileFromPreferences();

		if (profile.compareTo(Constants.KEY_NO_PROFILE) != 0)
		{
			loadProfile(profile);

			profilePosition = adapter.getPosition(profile);

			sProfileSelector.setSelection(profilePosition);
		}
		else
		{
			Toast.makeText(ActivityProfile.this, "No Profile Selected. Please select a profile !", Toast.LENGTH_SHORT).show();
		}

		// Populate other item lists
		populateItems();

		// Initialize click listeners
		initializeClickListeners();
	}

	private void initializeClickListeners()
	{
		bRevertBack.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				loadOriginalAvatar();
			}
		});

		bUseExisting.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
				startActivityForResult(intent, ACTIVITY_SELECT_IMAGE);
			}
		});
		bClickNew.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
				intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(mProfile.getProfilePath() + "/avatar.jpg")));
				startActivityForResult(intent, ACTIVITY_IMAGE_CAPTURE);
			}
		});

		tvGender.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				showDialog(v, getResources().getStringArray(R.array.array_gender), "Please select your gender");
			}
		});

		tvRace.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				showDialog(v, getResources().getStringArray(R.array.array_race), "Please select your race");
			}
		});

		tvAge.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				showDialog(v, itemsAge, "Please select your age");
			}
		});

		tvHeight.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				showDialog(v, itemsHeight, "Please select your height");
			}
		});

		tvWeight.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				showDialog(v, itemsWeight, "Please select your weight");
			}
		});

		tvLegLength.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				showDialog(v, itemsLegLength, "Please select your leg length");
			}
		});

		sProfileSelector.setOnItemSelectedListener(new OnItemSelectedListener()
		{
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3)
			{
				if (itemsProfiles[arg2].compareTo(ActivityProfile.CREATE_NEW) == 0)
				{
					getNewUserName();
				}
				else
				{
					loadProfile(itemsProfiles[arg2]);
					saveProfileToPreferences(itemsProfiles[arg2]);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0)
			{
			}
		});

		bDeleteProfile.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				deleteProfile();
			}
		});

		bSaveProfile.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				getInfo();

				// Remove previous profile
				File file = new File(mProfile.getProfilePath() + "/.profile");
				file.delete();

				// Add new file
				generateProfileFile(file);
			}
		});
	}

	private String getProfileFromPreferences()
	{
		return getApplicationContext().getSharedPreferences(Constants.PREFERENCES_KEY_APPLICATION, Context.MODE_PRIVATE).getString(
				Constants.KEY_PROFILE, Constants.KEY_NO_PROFILE);
	}

	private boolean saveProfileToPreferences(String profileName)
	{
		if (profileName != null)
		{
			Editor editor = getApplicationContext().getSharedPreferences(Constants.PREFERENCES_KEY_APPLICATION, Context.MODE_PRIVATE).edit();
			editor.putString(Constants.KEY_PROFILE, profileName);
			return editor.commit();
		}
		else
		{
			Editor editor = getApplicationContext().getSharedPreferences(Constants.PREFERENCES_KEY_APPLICATION, Context.MODE_PRIVATE).edit();
			editor.clear();
			return editor.commit();
		}
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
			mProfile = new Profile(profileName);

			generateProfileFile(profile);

			Log.e("ACTIVITY_PROFILE", "Profile '" + profileName + "' does not exists. Creating new.");
		}

		displayProfile();

		// Load profile avatar
		Drawable drawable = new BitmapDrawable(mainPath + "/" + profileName + "/avatar.jpg");
		ivAvatar.setImageDrawable(drawable);
	}

	private void createNewProfile(String profileName)
	{
		mProfile = new Profile(profileName);

		File profile = new File(mainPath + "/" + mProfile.getProfileName() + "/.profile");

		generateProfileFile(profile);

		displayProfile();

		ivAvatar.setImageResource(R.drawable.userdetails_avatarplaceholder);
	}

	private void generateProfileFile(File file)
	{
		try
		{
			// Create new profile file
			file.createNewFile();

			// Write data to the file
			FileWriter fstream = new FileWriter(mProfile.getProfilePath() + "/.profile");
			BufferedWriter out = new BufferedWriter(fstream);
			out.write(mProfile.toString());
			out.close();
		}
		catch (IOException e)
		{
			Log.e("ACTIVITY_PROFILE", "Unable to create profile : '" + mProfile.getProfileName() + "'");
		}
	}

	public void getNewUserName()
	{
		AlertDialog.Builder builder;
		final AlertDialog alertDialog;

		Context mContext = ActivityProfile.this;
		LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.dialog_input_profile_name, (ViewGroup) findViewById(R.id.linearlayout_dialog_root));

		final EditText etProfileName = (EditText) layout.findViewById(R.id.edittext_dialog_profile_name);
		Button bDialogOK = (Button) layout.findViewById(R.id.button_dialog_ok);
		Button bDialogCancel = (Button) layout.findViewById(R.id.button_dialog_cancel);

		builder = new AlertDialog.Builder(mContext);
		builder.setView(layout);
		alertDialog = builder.create();
		alertDialog.setCancelable(false);
		alertDialog.show();

		bDialogCancel.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if (sProfileSelector.getAdapter().getCount() < 2)
				{
					Toast.makeText(ActivityProfile.this, "You do not have any profile. Please create a new profile.", Toast.LENGTH_SHORT)
							.show();
				}
				else
				{
					// Dismiss the dialog
					alertDialog.dismiss();

					if (mProfile == null)
					{
						// Select the first position as default
						sProfileSelector.setSelection(1);
					}
					else
					{
						sProfileSelector.setSelection(profilePosition);
					}
				}
			}
		});

		bDialogOK.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				alertDialog.dismiss();

				String profileName = etProfileName.getEditableText().toString();

				saveProfileToPreferences(profileName);

				File file = new File(mainPath + "/" + profileName);

				file.mkdir();

				createNewProfile(profileName);

				ActivityProfile.this.finish();

				Intent intent = new Intent(ActivityProfile.this, ActivityProfile.class);
				startActivity(intent);
			}
		});
	}

	private void displayProfile()
	{
		etFirstName.setText(mProfile.getFirstName());
		etLastName.setText(mProfile.getLastName());

		tvAge.setText(mProfile.getAge());
		tvHeight.setText(mProfile.getHeight());
		tvWeight.setText(mProfile.getWeight());
		tvRace.setText(mProfile.getRace());
		tvLegLength.setText(mProfile.getLegLength());
		tvGender.setText(mProfile.getGender());
	}

	private void getInfo()
	{
		mProfile.setFirstName(etFirstName.getText().toString());
		mProfile.setLastName(etLastName.getText().toString());

		mProfile.setAge(tvAge.getText().toString());
		mProfile.setHeight(tvHeight.getText().toString());
		mProfile.setRace(tvRace.getText().toString());
		mProfile.setWeight(tvWeight.getText().toString());
		mProfile.setGender(tvGender.getText().toString());
		mProfile.setLeg_length(tvLegLength.getText().toString());
	}

	private void searchDirectories()
	{
		File mDirectory = new File(mainPath);

		if (!mDirectory.exists())
		{
			mDirectory.mkdir();
		}

		if (mDirectory.isDirectory())
		{
			String[] list = mDirectory.list();
			File child;
			int counter = 0;

			// Get the number of profiles present
			for (int i = 0; i < list.length; i++)
			{
				child = new File(mainPath + "/" + list[i]);

				if (child.isDirectory())
				{
					counter++;
				}
			}

			itemsProfiles = new String[counter + 1];

			counter = 0;

			itemsProfiles[counter++] = CREATE_NEW;

			// Get all profile names
			for (int i = 0; i < list.length; i++)
			{
				child = new File(mainPath + "/" + list[i]);

				if (child.isDirectory())
				{
					itemsProfiles[counter++] = list[i];
				}
			}
		}
		else
		{
			Log.e("ACTIVITY_PROFILE", "Folder RESL_Data does not exists");
		}
	}

	private void populateItems()
	{
		// Populate age items
		itemsAge = new String[MAX_AGE - MIN_AGE + 1];

		itemsAge[0] = DO_NOT_DISCLOSE;

		for (int i = 0; i < itemsAge.length - 1; i++)
		{
			itemsAge[i + 1] = String.valueOf(i + MIN_AGE) + " years";
		}

		// Populate weight items
		itemsWeight = new String[MAX_WEIGHT - MIN_WEIGHT + 1];

		itemsWeight[0] = DO_NOT_DISCLOSE;

		for (int i = 0; i < itemsWeight.length - 1; i++)
		{
			itemsWeight[i + 1] = String.valueOf(i + MIN_WEIGHT) + " pounds";
		}

		// Populate age height
		itemsHeight = new String[(MAX_HEIGHT - MIN_HEIGHT + 1 ) * 12];

		itemsHeight[0] = DO_NOT_DISCLOSE;

		for (int i = 0; i < MAX_HEIGHT - MIN_HEIGHT + 1; i++)
		{
			for (int j = 0; j < 12; j++)
			{
				if (j == 0)
				{
					itemsHeight[i * 12 + j] = String.valueOf(i + MIN_HEIGHT) + " ft.";
				}
				else
				{
					itemsHeight[i * 12 + j] = String.valueOf(i + MIN_HEIGHT) + " ft. and " + String.valueOf(j) + " inches.";
				}
			}
		}

		// Populate leg length
		itemsLegLength = new String[(MAX_LEG_LENGTH - MIN_LEG_LENGTH + 1 ) * 12];

		itemsLegLength[0] = DO_NOT_DISCLOSE;

		for (int i = 0; i < MAX_LEG_LENGTH - MIN_LEG_LENGTH + 1; i++)
		{
			for (int j = 0; j < 12; j++)
			{
				if (j == 0)
				{
					itemsLegLength[i * 12 + j] = String.valueOf(i + MIN_LEG_LENGTH) + " ft.";
				}
				else
				{
					itemsLegLength[i * 12 + j] = String.valueOf(i + MIN_LEG_LENGTH) + " ft. and " + String.valueOf(j) + " inches.";
				}
			}
		}
	}

	public void showDialog(final View v, final String[] items, String title)
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(title);
		builder.setItems(items, new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int item)
			{
				if (items[item].compareTo(DO_NOT_DISCLOSE) == 0)
				{
					((TextView) v).setText(NOT_DISCLOSED);
				}
				else
				{
					((TextView) v).setText(items[item]);
				}
			}
		});
		AlertDialog alert = builder.create();
		alert.show();
	}

	private void loadOriginalAvatar()
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Are you sure you want to delete your profile picture?").setCancelable(false)
				.setPositiveButton("Yes", new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int id)
					{
						ivAvatar.setImageResource(R.drawable.userdetails_avatarplaceholder);

						File file = new File(mProfile.getProfilePath() + "/avatar.jpg");
						if (file.exists())
						{
							file.delete();
						}
					}
				}).setNegativeButton("No", new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int id)
					{
						dialog.cancel();
					}
				});
		AlertDialog alert = builder.create();
		alert.show();
	}

	private void deleteProfile()
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Are you sure you want to delete this profile. You will lose all your data once you delete your profile ?")
				.setCancelable(false).setPositiveButton("Yes", new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int id)
					{
						Utility.deleteRecursively(mProfile.getProfilePath(), true);

						File file = new File(mProfile.getProfilePath());

						file.delete();

						ActivityProfile.this.finish();

						saveProfileToPreferences(null);
					}
				}).setNegativeButton("No", new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int id)
					{
						dialog.cancel();
					}
				});
		AlertDialog alert = builder.create();
		alert.show();
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent)
	{
		super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
		switch (requestCode)
		{
			case ACTIVITY_SELECT_IMAGE:
				if (resultCode == RESULT_OK)
				{
					Uri selectedImage = imageReturnedIntent.getData();

					String[] filePathColumn =
					{ MediaStore.Images.Media.DATA };

					Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);

					cursor.moveToFirst();

					int columnIndex = cursor.getColumnIndex(filePathColumn[0]);

					String imageFileName = cursor.getString(columnIndex);

					cursor.close();

					Bitmap bitmapOriginal = BitmapFactory.decodeFile(imageFileName);
					Bitmap bitmap = Bitmap.createScaledBitmap(bitmapOriginal, AVATAR_WIDTH, AVATAR_HEIGHT, false);

					// Write the image to the file
					File file = new File(mProfile.getProfilePath() + "/avatar.jpg");
					
					// Delete previous file if any
					if (file.exists())
					{
						file.delete();
					}
					
					try
					{
						file.createNewFile();
						FileOutputStream ostream = new FileOutputStream(file);
						bitmap.compress(CompressFormat.JPEG, 100, ostream);
						ostream.close();
					}
					catch (FileNotFoundException e)
					{
						Log.e("ACTIVTY_PROFILE", "Unable to open file : " + e.getMessage());
					}
					catch (IOException e)
					{
						Log.e("ACTIVTY_PROFILE", "Unable to write to file : " + e.getMessage());
					}

					Drawable drawable = new BitmapDrawable(imageFileName);
					ivAvatar.setImageDrawable(drawable);
				}
				break;

			case ACTIVITY_IMAGE_CAPTURE:
				if (resultCode == RESULT_OK)
				{
					// Load image file into bitmap
					String imageFileName = mProfile.getProfilePath() + "/avatar.jpg";
					Bitmap bitmapOriginal = BitmapFactory.decodeFile(imageFileName);
					Bitmap bitmap = Bitmap.createScaledBitmap(bitmapOriginal, AVATAR_WIDTH, AVATAR_HEIGHT, false);

					// Delete the original large file
					File file = new File(imageFileName);
					if (file.exists())
					{
						file.delete();
					}

					// Write new small file;
					try
					{
						file.createNewFile();
						FileOutputStream ostream = new FileOutputStream(file);
						bitmap.compress(CompressFormat.JPEG, 100, ostream);
						ostream.close();
					}
					catch (FileNotFoundException e)
					{
						Log.e("ACTIVTY_PROFILE", "Unable to open file : " + e.getMessage());
					}
					catch (IOException e)
					{
						Log.e("ACTIVTY_PROFILE", "Unable to write to file : " + e.getMessage());
					}

					Drawable drawable = new BitmapDrawable(bitmap);
					ivAvatar.setImageDrawable(drawable);
				}
				break;
		}
	}
}
