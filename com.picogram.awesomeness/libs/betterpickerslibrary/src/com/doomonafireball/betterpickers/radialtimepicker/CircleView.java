/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.doomonafireball.betterpickers.radialtimepicker;



import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;
import android.view.View;

import com.doomonafireball.betterpickers.R;
/**
 * Draws a simple white circle on which the numbers will be drawn.
 */
public class CircleView extends View {

	private static final String TAG = "CircleView";

	private final Paint mPaint = new Paint();
	private boolean mIs24HourMode;
	private final int mWhite;
	private final int mBlack;
	private float mCircleRadiusMultiplier;
	private float mAmPmCircleRadiusMultiplier;
	private boolean mIsInitialized;

	private boolean mDrawValuesReady;
	private int mXCenter;
	private int mYCenter;
	private int mCircleRadius;

	public CircleView(final Context context) {
		super(context);

		final Resources res = context.getResources();
		this.mWhite = res.getColor(R.color.white);
		this.mBlack = res.getColor(R.color.numbers_text_color);
		this.mPaint.setAntiAlias(true);

		this.mIsInitialized = false;
	}

	public void initialize(final Context context, final boolean is24HourMode) {
		if (this.mIsInitialized) {
			Log.e(TAG, "CircleView may only be initialized once.");
			return;
		}

		final Resources res = context.getResources();
		this.mIs24HourMode = is24HourMode;
		if (is24HourMode) {
			this.mCircleRadiusMultiplier = Float.parseFloat(
					res.getString(R.string.circle_radius_multiplier_24HourMode));
		} else {
			this.mCircleRadiusMultiplier = Float.parseFloat(
					res.getString(R.string.circle_radius_multiplier));
			this.mAmPmCircleRadiusMultiplier =
					Float.parseFloat(res.getString(R.string.ampm_circle_radius_multiplier));
		}

		this.mIsInitialized = true;
	}


	@Override
	public void onDraw(final Canvas canvas) {
		final int viewWidth = this.getWidth();
		if ((viewWidth == 0) || !this.mIsInitialized) {
			return;
		}

		if (!this.mDrawValuesReady) {
			this.mXCenter = this.getWidth() / 2;
			this.mYCenter = this.getHeight() / 2;
			this.mCircleRadius = (int) (Math.min(this.mXCenter, this.mYCenter) * this.mCircleRadiusMultiplier);

			if (!this.mIs24HourMode) {
				// We'll need to draw the AM/PM circles, so the main circle will need to have
				// a slightly higher center. To keep the entire view centered vertically, we'll
				// have to push it up by half the radius of the AM/PM circles.
				final int amPmCircleRadius = (int) (this.mCircleRadius * this.mAmPmCircleRadiusMultiplier);
				this.mYCenter -= amPmCircleRadius / 2;
			}

			this.mDrawValuesReady = true;
		}

		// Draw the white circle.
		this.mPaint.setColor(this.mWhite);
		canvas.drawCircle(this.mXCenter, this.mYCenter, this.mCircleRadius, this.mPaint);

		// Draw a small black circle in the center.
		this.mPaint.setColor(this.mBlack);
		canvas.drawCircle(this.mXCenter, this.mYCenter, 2, this.mPaint);
	}
}
