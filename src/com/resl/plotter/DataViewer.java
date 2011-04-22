/**
 * Copyright (C) 2009, 2010 SC 4ViewSoft SRL
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.resl.plotter;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import org.achartengine.ChartFactory;
import org.achartengine.chart.PointStyle;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import com.resl.sensors.FileData;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

/**
 * Average temperature demo chart.
 */
public class DataViewer extends AbstractDemoChart
{
	private static String TAG = "ACTIVITY_DATA_VIEWER";
	public static int INITIAL_OFFSET = 50;
	public static int HEADER_SIZE = 13;
	public static int DOWNSAMPLE_RATE = 25;

	private static String[] TITLES_ALL =
	{ "Gyroscope X", "Gyroscope Y", "Gyroscope Z", "Rotated Gyroscope X", "Rotated Gyroscope Y", "Rotated Gyroscope Z", "Accelerometer X",
			"Accelerometer Y", "Accelerometer Z", "Rotated Accelerometer X", "Rotated Accelerometer Y", "Rotated Accelerometer Z" };

	private static int[] COLORS_ALL =
	{ Color.BLUE, Color.GREEN, Color.CYAN, Color.YELLOW, Color.RED, Color.WHITE, Color.LTGRAY, Color.MAGENTA, Color.GRAY, Color.BLUE,
			Color.GREEN, Color.CYAN };

	boolean isSelected;

	String name;
	FileData fileData;
	String desc;

	public DataViewer(String name, FileData fileData)
	{
		this.name = name;
		this.fileData = fileData;
	}

	/**
	 * Returns the chart name.
	 * 
	 * @return the chart name
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Returns the chart description.
	 * 
	 * @return the chart description
	 */
	public String getDesc()
	{
		return "Data Viewer for : " + getName();
	}

	/**
	 * Executes the chart demo.
	 * 
	 * @param context
	 *            the context
	 * @return the built intent
	 */
	public Intent execute(final Context context)
	{
		String[] titles = null;

		List<double[]> x = null;
		List<double[]> values = null;

		try
		{						
			titles = new String[fileData.getOptionsSelected().length];
			
			for (int i = 0; i < fileData.getOptionsSelected().length; i++)
			{
				titles[i] = TITLES_ALL[(int) (fileData.getOptionsSelected()[i])];
			}

			// Generate x values
			x = new ArrayList<double[]>();

			for (int i = 0; i < titles.length; i++)
			{
				double[] x_row = new double[fileData.getDownSampleLength()];

				for (int j = 0; j < x_row.length; j++)
				{
					x_row[j] = j;
				}

				x.add(x_row);
			}

			// Generate y values
			values = new ArrayList<double[]>();

			double[][] y_values = new double[titles.length][fileData.getDownSampleLength()];

			// Skip offset data
			int skipCounter = 0;
			
			FileInputStream fis;
			BufferedInputStream bis;
			DataInputStream dis;
			String line;
			String[] arrayString = null;
			
			// Open the file
			fis = new FileInputStream(getName());
			bis = new BufferedInputStream(fis);
			dis = new DataInputStream(bis);

			// Skip first few readings
			while (skipCounter < INITIAL_OFFSET + HEADER_SIZE + 1)
			{
				dis.readLine();
				skipCounter++;
			}

			for (int i = 0; i < y_values[0].length; i++)
			{
				line = dis.readLine();

				arrayString = line.split(",");

				for (int j = 0; j < titles.length; j++)
				{
					y_values[j][i] = Double.parseDouble(arrayString[j + ((int) (j / 3))]);
					// Since we need to add 1 every time
				}

				// Skip readings to downsample
				skipCounter = 0;

				while (skipCounter < DOWNSAMPLE_RATE - 1)
				{
					dis.readLine();
					skipCounter++;
				}
			}

			for (int i = 0; i < titles.length; i++)
			{
				values.add(y_values[i]);
			}

			// Close the file
			dis.close();
			bis.close();
			fis.close();
		}

		catch (Exception e)
		{
			Log.e(TAG, "Error loading file. Error : " + e.getMessage());
		}

		int[] colors = new int[fileData.getOptionsSelected().length];
		
		for (int i = 0; i < fileData.getOptionsSelected().length; i++)
		{
			colors[i] = COLORS_ALL[(int) fileData.getOptionsSelected()[i]];
		}

		PointStyle[] styles = new PointStyle[titles.length];

		for (int i = 0; i < titles.length; i++)
		{
			styles[i] = PointStyle.POINT;
		}

		XYMultipleSeriesRenderer renderer = buildRenderer(colors, styles);

		int length = renderer.getSeriesRendererCount();
		for (int i = 0; i < length; i++)
		{
			((XYSeriesRenderer) renderer.getSeriesRendererAt(i)).setFillPoints(true);
		}
		setChartSettings(renderer, getName().replace(Environment.getExternalStorageDirectory() + "/" + "RESL_Data/", "")
				.replace(".csv", "").replace(".raw", ""), "Data", "Seconds", -50, fileData.getDownSampleLength(), -10.0, 10.0, Color.LTGRAY, Color.LTGRAY);
		renderer.setXLabels(12);
		renderer.setYLabels(10);
		renderer.setShowGrid(true);
		renderer.setXLabelsAlign(Align.RIGHT);
		renderer.setYLabelsAlign(Align.RIGHT);

		// Provide limits as xStart, xEnd, yStart, yEnd
		renderer.setPanLimits(new double[]
		{ -50, fileData.getDownSampleLength() + 50, -10, 10 });
		renderer.setZoomLimits(new double[]
		{ -100, fileData.getDownSampleLength() + 100, -20, 20 });
		Intent intent = ChartFactory.getLineChartIntent(context, buildDataset(titles, x, values), renderer, "Average temperature");
		return intent;
	}
}
