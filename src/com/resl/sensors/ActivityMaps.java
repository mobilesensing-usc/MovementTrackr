package com.resl.sensors;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.ProgressDialog;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

public class ActivityMaps extends MapActivity
{
	public final static String KEY_FILE_NAME = "key_file_name";

	private final static String TAG = "ACTIVITY_MAPS";

	private final static int FILE_OFFSET = 9;

	private MapView mapView;

	private ArrayList<Double> mListLatitude;
	private ArrayList<Double> mListLongitude;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_maps);

		Bundle extras = getIntent().getExtras();

		if ((extras == null) || (extras.getString(KEY_FILE_NAME) == null))
		{
			Toast.makeText(ActivityMaps.this, "No filename passed, Unable to display data", Toast.LENGTH_SHORT).show();

			this.finish();
		}
		else
		{
			FileInputStream fis = null;
			BufferedInputStream bis = null;
			DataInputStream dis = null;

			try
			{
				String line;
				String[] splitString = null;

				float distance[] =
				{ 0.0f, 0.0f, 0.0f };

				fis = new FileInputStream(extras.getString(KEY_FILE_NAME));
				bis = new BufferedInputStream(fis);
				dis = new DataInputStream(bis);

				double lat = 0.0f;
				double lon = 0.0f;

				double lastLat = 0.0f;
				double lastLon = 0.0f;

				int counter = 0;

				// Ignore the file header
				while ((dis.readLine() != null) && (counter < FILE_OFFSET))
				{
					counter++;
				}

				// Check if GPS data is recorded in the file
				dis.readLine();
				splitString = dis.readLine().split(",");

				if (!Boolean.parseBoolean(splitString[1]))
				{
					Toast.makeText(ActivityMaps.this, "File does not contain GPS data.", Toast.LENGTH_SHORT).show();

					this.finish();
				}
				else
				{
					mListLatitude = new ArrayList<Double>();
					mListLongitude = new ArrayList<Double>();

					// Ignore two blank lines
					dis.readLine();
					dis.readLine();

					// Read the GPS data
					while ((line = dis.readLine()) != null)
					{
						splitString = line.split(",");

						lat = Double.parseDouble(splitString[splitString.length - 2]);
						lon = Double.parseDouble(splitString[splitString.length - 1]);

						// Check that the lat / lon should not be zero
						if  ( !(((lat > -0.001f) && (lat < 0.001f)) && ((lon > -0.001f) && (lon < 0.001f))))
						{
							Location.distanceBetween(lat, lon, lastLat, lastLon, distance);

							if (distance[0] > 10.0f)
							{
								lastLat = lat;
								lastLon = lon;
								mListLatitude.add(lat);
								mListLongitude.add(lon);
							}
						}
					}
				}
			}
			catch (Exception e)
			{
				Log.e(TAG, "Error Reading File. Error : " + e.getMessage());
			}
			finally
			{
				try
				{
					dis.close();
					bis.close();
					fis.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}

		mapView = (MapView) findViewById(R.id.mapview);
		mapView.setBuiltInZoomControls(true);

		if ((mListLatitude != null) && (mListLatitude.size() > 0))
		{
			List<Overlay> mapOverlays = mapView.getOverlays();

			MapItemizedOverlay itemizedOverlay = new MapItemizedOverlay(this.getResources().getDrawable(R.drawable.maps_stop),
					ActivityMaps.this);

			OverlayItem overlayitem = null;

			MapController mapController = mapView.getController();
			
				mapController.animateTo(new GeoPoint((int) (mListLatitude.get(0) * 1000000), (int) (mListLongitude.get(0) * 1000000)));
				mapController.setZoom(17);
				//mapController.zoomToSpan((int) (mListLatitude.get(0) * 1000000), (int) (mListLongitude.get(0) * 1000000));	

			for (int i = 0; i < mListLatitude.size(); i++)
			{
				GeoPoint point = new GeoPoint((int) (mListLatitude.get(i) * 1000000), (int) (mListLongitude.get(i) * 1000000));

				overlayitem = new OverlayItem(point, null, null);
				
				itemizedOverlay.addOverlay(overlayitem);
			}

			mapOverlays.add(itemizedOverlay);
		}
	}

	@Override
	protected boolean isRouteDisplayed()
	{
		return false;
	}
}
