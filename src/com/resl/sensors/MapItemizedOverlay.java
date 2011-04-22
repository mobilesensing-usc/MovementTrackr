package com.resl.sensors;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;

import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.OverlayItem;

public class MapItemizedOverlay extends ItemizedOverlay
{
	private ArrayList<OverlayItem> mOverlays;
	Context mContext;

	public MapItemizedOverlay(Drawable defaultMarker)
	{
		super(boundCenterBottom(defaultMarker));

		mOverlays = new ArrayList<OverlayItem>();
	}

	public MapItemizedOverlay(Drawable defaultMarker, Context context)
	{
		this(defaultMarker);

		mContext = context;
	}

	@Override
	protected OverlayItem createItem(int arg0)
	{
		return mOverlays.get(arg0);
	}

	public void addOverlay(OverlayItem overlay)
	{
		mOverlays.add(overlay);
		populate();
	}

	@Override
	public int size()
	{
		return mOverlays.size();
	}
	
	public void remove(OverlayItem item)
	{
		mOverlays.remove(item);
	}
}

