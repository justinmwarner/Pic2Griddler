
package com.picogram.awesomeness;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import java.util.ArrayList;

public class BounceListView extends ListView
{
	private static final String TAG = "BounceListView";
	private final Context mContext;

	public BounceListView(final Context context)
	{
		super(context);
		this.mContext = context;
		this.initBounceListView();
	}

	public BounceListView(final Context context, final AttributeSet attrs)
	{
		super(context, attrs);
		this.mContext = context;
		this.initBounceListView();
	}

	public BounceListView(final Context context, final AttributeSet attrs, final int defStyle)
	{
		super(context, attrs, defStyle);
		this.mContext = context;
		this.initBounceListView();
	}

	private void initBounceListView()
	{
	}

	// TODO: Make a better over scroll.
	@Override
	protected boolean overScrollBy(final int deltaX, int deltaY, final int scrollX, final int scrollY, final int scrollRangeX, final int scrollRangeY, final int maxOverScrollX, final int maxOverScrollY, final boolean isTouchEvent)
	{// Get the first and last visible positions.
		final int first = this.getFirstVisiblePosition();
		final int last = this.getLastVisiblePosition();

		// Snap delta to be a maximum of 60 on either sides.
		deltaY = deltaY > 60 ? 60 : deltaY;
		deltaY = deltaY < -60 ? -60 : deltaY;

		final float factor = 1 / (float) (last - first);

		// Rotate half the entries.
		if (deltaY > 0) {
			// Over-scrolled at the bottom.
			for (int i = last - (2 * first), j = 1; i <= (last - first); i++, j++) {
				final View item = this.getChildAt(i);
				if (item != null) {
					this.tilt(item, deltaY * j * factor, 1 == (i % 2));
				}
			}
		} else {
			// Over-scrolled at the top.
			for (int i = first, j = 1; i <= (last); i++, j++) {
				final View item = this.getChildAt(i);
				if (item != null) {
					this.tilt(item, deltaY * (1 - (j * factor)), 1 == (i % 2));
				}
			}
		}
		return true;
	}

	public void tilt(final View view, float deg, final boolean isEven) {
		final ViewGroup vg = (ViewGroup) view;
		final ArrayList<View> views = new ArrayList();
		for (int i = 0; i != vg.getChildCount(); ++i)
		{
			views.add(vg.getChildAt(i));
		}
		for (final View v : views)
		{
			if (deg < 0) {
				deg = deg * 2;
			}
			deg = (int) (deg * (Math.random() * 2)) + 2;
			v.setRotationX(deg);
		}
		final Drawable back = vg.getBackground();
		back.setAlpha(200);
		vg.setBackgroundDrawable(back);
		// Reset the rotation.
		view.postDelayed(new Runnable() {
			public void run() {
				back.setAlpha(255);
				vg.setBackgroundDrawable(back);
				for (final View v : views) {
					v.setRotationX(0);
				}
			}
		}, 10);
	}

}
