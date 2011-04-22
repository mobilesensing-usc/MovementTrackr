package com.resl.sensors;

import java.util.ArrayList;

import android.app.Activity;
import android.app.ListActivity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

public class ActivityAddNewActivity extends ListActivity
{
	ArrayList<String> activities;

	Editor editor;
	SharedPreferences sharedPreferences;

	EditText etAddActivity;
	ImageButton ibAddActivity;

	ArrayAdapter<String> adapter;

	ListView listView;

	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_add_activity);

		etAddActivity = (EditText) findViewById(R.id.edittext_add_activity);
		ibAddActivity = (ImageButton) findViewById(R.id.imagebutton_add_activity);

		sharedPreferences = getSharedPreferences(
				Constants.PREFERENCES_KEY_APPLICATION, Activity.MODE_PRIVATE);
		editor = sharedPreferences.edit();
		activities = new ArrayList<String>();

		getActivities();

		setListAdapter(new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, activities));

		listView = getListView();

		adapter = (ArrayAdapter<String>) listView.getAdapter();

		registerForContextMenu(listView);

		ibAddActivity.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				String text = etAddActivity.getText().toString();

				if (text.length() > 0)
				{
					activities.add(text);

					etAddActivity.setText("");

					adapter.notifyDataSetChanged();
				} else
				{
					Toast.makeText(ActivityAddNewActivity.this,
							"Please write activity name !", Toast.LENGTH_SHORT)
							.show();
				}
			}
		});
	}
	
	public void onDestroy()
	{
		String activityString = "";
		
		// Save data as preferences
		for (int i = 0; i < activities.size(); i++)
		{
			activityString += activities.get(i);
			
			if (i != (activities.size() - 1))
			{
				activityString += ",";
			}
		}
		
		editor.putString(Constants.KEY_ADD_ACTIVTY, activityString);
		editor.commit();
		
		super.onDestroy();
	}
	

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_add_activity_context, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item)
	{
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();

		switch (item.getItemId())
		{
			case R.id.menu_add_activity_delete:
				activities.remove(info.position);
				adapter.notifyDataSetChanged();

			default:
				return super.onContextItemSelected(item);
		}
	}

	private void getActivities()
	{
		if (sharedPreferences != null)
		{
			String combined = sharedPreferences.getString(
					Constants.KEY_ADD_ACTIVTY, "");
			String[] splitString = combined.split(",");

			for (int i = 0; i < splitString.length; i++)
			{
				if (splitString[i].length() > 0)
				{
					activities.add(splitString[i]);
				}
			}
		}
	}
}
