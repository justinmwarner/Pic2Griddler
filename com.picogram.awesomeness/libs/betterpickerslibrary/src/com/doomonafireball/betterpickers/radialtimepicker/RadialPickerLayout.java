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


import com.doomonafireball.betterpickers.R;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.view.ViewHelper;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.os.Vibrator;
import android.support.v4.view.accessibility.AccessibilityManagerCompat;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
public class RadialPickerLayout extends FrameLayout implements OnTouchListener {

	public interface OnValueSelectedListener {

		void onValueSelected(int pickerIndex, int newValue, boolean autoAdvance);
	}

	private static final String TAG = "RadialPickerLayout";
	private final int touchSlop;

	private final int tapTimeout;
	private static final int VISIBLE_DEGREES_STEP_SIZE = 30;
	private static final int HOUR_VALUE_TO_DEGREES_STEP_SIZE = VISIBLE_DEGREES_STEP_SIZE;
	private static final int MINUTE_VALUE_TO_DEGREES_STEP_SIZE = 6;
	private static final int HOUR_INDEX = RadialTimePickerDialog.HOUR_INDEX;
	private static final int MINUTE_INDEX = RadialTimePickerDialog.MINUTE_INDEX;
	private static final int AMPM_INDEX = RadialTimePickerDialog.AMPM_INDEX;
	private static final int ENABLE_PICKER_INDEX = RadialTimePickerDialog.ENABLE_PICKER_INDEX;
	private static final int AM = RadialTimePickerDialog.AM;

	private static final int PM = RadialTimePickerDialog.PM;
	private final Vibrator mVibrator;
	private long mLastVibrate;

	private int mLastValueSelected;
	private OnValueSelectedListener mListener;
	private boolean mTimeInitialized;
	private int mCurrentHoursOfDay;
	private int mCurrentMinutes;
	private boolean mIs24HourMode;
	private boolean mHideAmPm;

	private int mCurrentItemShowing;
	private final CircleView mCircleView;
	private final AmPmCirclesView mAmPmCirclesView;
	private final RadialTextsView mHourRadialTextsView;
	private final RadialTextsView mMinuteRadialTextsView;
	private final RadialSelectorView mHourRadialSelectorView;
	private final RadialSelectorView mMinuteRadialSelectorView;

	private final View mGrayBox;
	private int[] mSnapPrefer30sMap;
	private boolean mInputEnabled;
	private int mIsTouchingAmOrPm = -1;
	private boolean mDoingMove;
	private boolean mDoingTouch;
	private int mDownDegrees;
	private float mDownX;
	private float mDownY;

	private final AccessibilityManager mAccessibilityManager;
	private AnimatorSet mTransition;

	private final Handler mHandler = new Handler();

	public RadialPickerLayout(final Context context, final AttributeSet attrs) {
		super(context, attrs);

		this.setOnTouchListener(this);
		final ViewConfiguration vc = ViewConfiguration.get(context);
		this.touchSlop = vc.getScaledTouchSlop();
		this.tapTimeout = ViewConfiguration.getTapTimeout();
		this.mDoingMove = false;

		this.mCircleView = new CircleView(context);
		this.addView(this.mCircleView);

		this.mAmPmCirclesView = new AmPmCirclesView(context);
		this.addView(this.mAmPmCirclesView);

		this.mHourRadialTextsView = new RadialTextsView(context);
		this.addView(this.mHourRadialTextsView);
		this.mMinuteRadialTextsView = new RadialTextsView(context);
		this.addView(this.mMinuteRadialTextsView);

		this.mHourRadialSelectorView = new RadialSelectorView(context);
		this.addView(this.mHourRadialSelectorView);
		this.mMinuteRadialSelectorView = new RadialSelectorView(context);
		this.addView(this.mMinuteRadialSelectorView);

		// Prepare mapping to snap touchable degrees to selectable degrees.
		this.preparePrefer30sMap();

		this.mVibrator = (Vibrator) context.getSystemService(Service.VIBRATOR_SERVICE);
		this.mLastVibrate = 0;
		this.mLastValueSelected = -1;

		this.mInputEnabled = true;
		this.mGrayBox = new View(context);
		this.mGrayBox.setLayoutParams(new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		this.mGrayBox.setBackgroundColor(this.getResources().getColor(R.color.transparent_black));
		this.mGrayBox.setVisibility(View.INVISIBLE);
		this.addView(this.mGrayBox);

		this.mAccessibilityManager = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);

		this.mTimeInitialized = false;
	}

	/**
	 * Announce the currently-selected time when launched.
	 */
	@Override
	public boolean dispatchPopulateAccessibilityEvent(final AccessibilityEvent event) {
		if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
			// Clear the event's current text so that only the current time will be spoken.
			event.getText().clear();
			final Time time = new Time();
			time.hour = this.getHours();
			time.minute = this.getMinutes();
			final long millis = time.normalize(true);
			int flags = DateUtils.FORMAT_SHOW_TIME;
			if (this.mIs24HourMode) {
				flags |= DateUtils.FORMAT_24HOUR;
			}
			final String timeString = DateUtils.formatDateTime(this.getContext(), millis, flags);
			event.getText().add(timeString);
			return true;
		}
		return super.dispatchPopulateAccessibilityEvent(event);
	}

	/**
	 * Get the item (hours or minutes) that is currently showing.
	 */
	public int getCurrentItemShowing() {
		if ((this.mCurrentItemShowing != HOUR_INDEX) && (this.mCurrentItemShowing != MINUTE_INDEX)) {
			Log.e(TAG, "Current item showing was unfortunately set to " + this.mCurrentItemShowing);
			return -1;
		}
		return this.mCurrentItemShowing;
	}

	/**
	 * If the hours are showing, return the current hour. If the minutes are showing, return the current minute.
	 */
	private int getCurrentlyShowingValue() {
		final int currentIndex = this.getCurrentItemShowing();
		if (currentIndex == HOUR_INDEX) {
			return this.mCurrentHoursOfDay;
		} else if (currentIndex == MINUTE_INDEX) {
			return this.mCurrentMinutes;
		} else {
			return -1;
		}
	}

	/**
	 * Calculate the degrees within the circle that corresponds to the specified coordinates, if the coordinates are
	 * within the range that will trigger a selection.
	 *
	 * @param pointX The x coordinate.
	 * @param pointY The y coordinate.
	 * @param forceLegal Force the selection to be legal, regardless of how far the coordinates are from the actual
	 * numbers.
	 * @param isInnerCircle If the selection may be in the inner circle, pass in a size-1 boolean array here, inside
	 * which the value will be true if the selection is in the inner circle, and false if in the outer circle.
	 * @return Degrees from 0 to 360, if the selection was within the legal range. -1 if not.
	 */
	private int getDegreesFromCoords(final float pointX, final float pointY, final boolean forceLegal,
			final Boolean[] isInnerCircle) {
		final int currentItem = this.getCurrentItemShowing();
		if (currentItem == HOUR_INDEX) {
			return this.mHourRadialSelectorView.getDegreesFromCoords(
					pointX, pointY, forceLegal, isInnerCircle);
		} else if (currentItem == MINUTE_INDEX) {
			return this.mMinuteRadialSelectorView.getDegreesFromCoords(
					pointX, pointY, forceLegal, isInnerCircle);
		} else {
			return -1;
		}
	}

	public int getHours() {
		return this.mCurrentHoursOfDay;
	}

	public int getIsCurrentlyAmOrPm() {
		if (this.mCurrentHoursOfDay < 12) {
			return AM;
		} else if (this.mCurrentHoursOfDay < 24) {
			return PM;
		}
		return -1;
	}

	public int getMinutes() {
		return this.mCurrentMinutes;
	}

	/**
	 * Initialize the Layout with starting values.
	 */
	 public void initialize(final Context context, final int initialHoursOfDay, final int initialMinutes,
			 final boolean is24HourMode) {
		 if (this.mTimeInitialized) {
			 Log.e(TAG, "Time has already been initialized.");
			 return;
		 }
		 this.mIs24HourMode = is24HourMode;
		 this.mHideAmPm = AccessibilityManagerCompat.isTouchExplorationEnabled(this.mAccessibilityManager) ? true : this.mIs24HourMode;

		 // Initialize the circle and AM/PM circles if applicable.
		 this.mCircleView.initialize(context, this.mHideAmPm);
		 this.mCircleView.invalidate();
		 if (!this.mHideAmPm) {
			 this.mAmPmCirclesView.initialize(context, initialHoursOfDay < 12 ? AM : PM);
			 this.mAmPmCirclesView.invalidate();
		 }

		 // Initialize the hours and minutes numbers.
		 final Resources res = context.getResources();
		 final int[] hours = {12, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
		 final int[] hours24 = {0, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23};
		 final int[] minutes = {0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55};
		 final String[] hoursTexts = new String[12];
		 final String[] innerHoursTexts = new String[12];
		 final String[] minutesTexts = new String[12];
		 for (int i = 0; i < 12; i++) {
			 hoursTexts[i] = is24HourMode ?
					 String.format("%02d", hours24[i]) : String.format("%d", hours[i]);
					 innerHoursTexts[i] = String.format("%d", hours[i]);
					 minutesTexts[i] = String.format("%02d", minutes[i]);
		 }
		 this.mHourRadialTextsView.initialize(res,
				 hoursTexts, (is24HourMode ? innerHoursTexts : null), this.mHideAmPm, true);
		 this.mHourRadialTextsView.invalidate();
		 this.mMinuteRadialTextsView.initialize(res, minutesTexts, null, this.mHideAmPm, false);
		 this.mMinuteRadialTextsView.invalidate();

		 // Initialize the currently-selected hour and minute.
		 this.setValueForItem(HOUR_INDEX, initialHoursOfDay);
		 this.setValueForItem(MINUTE_INDEX, initialMinutes);
		 final int hourDegrees = (initialHoursOfDay % 12) * HOUR_VALUE_TO_DEGREES_STEP_SIZE;
		 this.mHourRadialSelectorView.initialize(context, this.mHideAmPm, is24HourMode, true,
				 hourDegrees, this.isHourInnerCircle(initialHoursOfDay));
		 final int minuteDegrees = initialMinutes * MINUTE_VALUE_TO_DEGREES_STEP_SIZE;
		 this.mMinuteRadialSelectorView.initialize(context, this.mHideAmPm, false, false,
				 minuteDegrees, false);

		 this.mTimeInitialized = true;
	 }

	 /**
	  * Check if a given hour appears in the outer circle or the inner circle
	  *
	  * @return true if the hour is in the inner circle, false if it's in the outer circle.
	  */
	 private boolean isHourInnerCircle(final int hourOfDay) {
		 // We'll have the 00 hours on the outside circle.
		 return this.mIs24HourMode && ((hourOfDay <= 12) && (hourOfDay != 0));
	 }

	 /**
	  * Necessary for accessibility, to ensure we support "scrolling" forward and backward in the circle.
	  */
	 @Override
	 public void onInitializeAccessibilityNodeInfo(final AccessibilityNodeInfo info) {
		 super.onInitializeAccessibilityNodeInfo(info);
		 info.addAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
		 info.addAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
	 }

	 /**
	  * Measure the view to end up as a square, based on the minimum of the height and width.
	  */
	 @Override
	 public void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
		 final int measuredWidth = MeasureSpec.getSize(widthMeasureSpec);
		 final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		 final int measuredHeight = MeasureSpec.getSize(heightMeasureSpec);
		 final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		 final int minDimension = Math.min(measuredWidth, measuredHeight);

		 super.onMeasure(MeasureSpec.makeMeasureSpec(minDimension, widthMode),
				 MeasureSpec.makeMeasureSpec(minDimension, heightMode));
	 }

	 @Override
	 public boolean onTouch(final View v, final MotionEvent event) {
		 final float eventX = event.getX();
		 final float eventY = event.getY();
		 int degrees;
		 int value;
		 final Boolean[] isInnerCircle = new Boolean[1];
		 isInnerCircle[0] = false;

		 final long millis = SystemClock.uptimeMillis();

		 switch (event.getAction()) {
			 case MotionEvent.ACTION_DOWN:
				 if (!this.mInputEnabled) {
					 return true;
				 }

				 this.mDownX = eventX;
				 this.mDownY = eventY;

				 this.mLastValueSelected = -1;
				 this.mDoingMove = false;
				 this.mDoingTouch = true;
				 // If we're showing the AM/PM, check to see if the user is touching it.
				 if (!this.mHideAmPm) {
					 this.mIsTouchingAmOrPm = this.mAmPmCirclesView.getIsTouchingAmOrPm(eventX, eventY);
				 } else {
					 this.mIsTouchingAmOrPm = -1;
				 }
				 if ((this.mIsTouchingAmOrPm == AM) || (this.mIsTouchingAmOrPm == PM)) {
					 // If the touch is on AM or PM, set it as "touched" after the tapTimeout
					 // in case the user moves their finger quickly.
					 this.tryVibrate();
					 this.mDownDegrees = -1;
					 this.mHandler.postDelayed(new Runnable() {
						 @Override
						 public void run() {
							 RadialPickerLayout.this.mAmPmCirclesView.setAmOrPmPressed(RadialPickerLayout.this.mIsTouchingAmOrPm);
							 RadialPickerLayout.this.mAmPmCirclesView.invalidate();
						 }
					 }, this.tapTimeout);
				 } else {
					 // If we're in accessibility mode, force the touch to be legal. Otherwise,
					 // it will only register within the given touch target zone.
					 final boolean forceLegal = AccessibilityManagerCompat.isTouchExplorationEnabled(this.mAccessibilityManager);
					 // Calculate the degrees that is currently being touched.
					 this.mDownDegrees = this.getDegreesFromCoords(eventX, eventY, forceLegal, isInnerCircle);
					 if (this.mDownDegrees != -1) {
						 // If it's a legal touch, set that number as "selected" after the
						 // tapTimeout in case the user moves their finger quickly.
						 this.tryVibrate();
						 this.mHandler.postDelayed(new Runnable() {
							 @Override
							 public void run() {
								 RadialPickerLayout.this.mDoingMove = true;
								 final int value = RadialPickerLayout.this.reselectSelector(RadialPickerLayout.this.mDownDegrees, isInnerCircle[0],
										 false, true);
								 RadialPickerLayout.this.mLastValueSelected = value;
								 RadialPickerLayout.this.mListener.onValueSelected(RadialPickerLayout.this.getCurrentItemShowing(), value, false);
							 }
						 }, this.tapTimeout);
					 }
				 }
				 return true;
			 case MotionEvent.ACTION_MOVE:
				 if (!this.mInputEnabled) {
					 // We shouldn't be in this state, because input is disabled.
					 Log.e(TAG, "Input was disabled, but received ACTION_MOVE.");
					 return true;
				 }

				 final float dY = Math.abs(eventY - this.mDownY);
				 final float dX = Math.abs(eventX - this.mDownX);

				 if (!this.mDoingMove && (dX <= this.touchSlop) && (dY <= this.touchSlop)) {
					 // Hasn't registered down yet, just slight, accidental movement of finger.
					 break;
				 }

				 // If we're in the middle of touching down on AM or PM, check if we still are.
				 // If so, no-op. If not, remove its pressed state. Either way, no need to check
				 // for touches on the other circle.
				 if ((this.mIsTouchingAmOrPm == AM) || (this.mIsTouchingAmOrPm == PM)) {
					 this.mHandler.removeCallbacksAndMessages(null);
					 final int isTouchingAmOrPm = this.mAmPmCirclesView.getIsTouchingAmOrPm(eventX, eventY);
					 if (isTouchingAmOrPm != this.mIsTouchingAmOrPm) {
						 this.mAmPmCirclesView.setAmOrPmPressed(-1);
						 this.mAmPmCirclesView.invalidate();
						 this.mIsTouchingAmOrPm = -1;
					 }
					 break;
				 }

				 if (this.mDownDegrees == -1) {
					 // Original down was illegal, so no movement will register.
					 break;
				 }

				 // We're doing a move along the circle, so move the selection as appropriate.
				 this.mDoingMove = true;
				 this.mHandler.removeCallbacksAndMessages(null);
				 degrees = this.getDegreesFromCoords(eventX, eventY, true, isInnerCircle);
				 if (degrees != -1) {
					 value = this.reselectSelector(degrees, isInnerCircle[0], false, true);
					 if (value != this.mLastValueSelected) {
						 this.tryVibrate();
						 this.mLastValueSelected = value;
						 this.mListener.onValueSelected(this.getCurrentItemShowing(), value, false);
					 }
				 }
				 return true;
			 case MotionEvent.ACTION_UP:
				 if (!this.mInputEnabled) {
					 // If our touch input was disabled, tell the listener to re-enable us.
					 Log.d(TAG, "Input was disabled, but received ACTION_UP.");
					 this.mListener.onValueSelected(ENABLE_PICKER_INDEX, 1, false);
					 return true;
				 }

				 this.mHandler.removeCallbacksAndMessages(null);
				 this.mDoingTouch = false;

				 // If we're touching AM or PM, set it as selected, and tell the listener.
				 if ((this.mIsTouchingAmOrPm == AM) || (this.mIsTouchingAmOrPm == PM)) {
					 final int isTouchingAmOrPm = this.mAmPmCirclesView.getIsTouchingAmOrPm(eventX, eventY);
					 this.mAmPmCirclesView.setAmOrPmPressed(-1);
					 this.mAmPmCirclesView.invalidate();

					 if (isTouchingAmOrPm == this.mIsTouchingAmOrPm) {
						 this.mAmPmCirclesView.setAmOrPm(isTouchingAmOrPm);
						 if (this.getIsCurrentlyAmOrPm() != isTouchingAmOrPm) {
							 this.mListener.onValueSelected(AMPM_INDEX, this.mIsTouchingAmOrPm, false);
							 this.setValueForItem(AMPM_INDEX, isTouchingAmOrPm);
						 }
					 }
					 this.mIsTouchingAmOrPm = -1;
					 break;
				 }

				 // If we have a legal degrees selected, set the value and tell the listener.
				 if (this.mDownDegrees != -1) {
					 degrees = this.getDegreesFromCoords(eventX, eventY, this.mDoingMove, isInnerCircle);
					 if (degrees != -1) {
						 value = this.reselectSelector(degrees, isInnerCircle[0], !this.mDoingMove, false);
						 if ((this.getCurrentItemShowing() == HOUR_INDEX) && !this.mIs24HourMode) {
							 final int amOrPm = this.getIsCurrentlyAmOrPm();
							 if ((amOrPm == AM) && (value == 12)) {
								 value = 0;
							 } else if ((amOrPm == PM) && (value != 12)) {
								 value += 12;
							 }
						 }
						 this.setValueForItem(this.getCurrentItemShowing(), value);
						 this.mListener.onValueSelected(this.getCurrentItemShowing(), value, true);
					 }
				 }
				 this.mDoingMove = false;
				 return true;
			 default:
				 break;
		 }
		 return false;
	 }

	 /**
	  * When scroll forward/backward events are received, jump the time to the higher/lower discrete, visible value on
	  * the circle.
	  */
	 @SuppressLint("NewApi")
	 @Override
	 public boolean performAccessibilityAction(final int action, final Bundle arguments) {
		 if (super.performAccessibilityAction(action, arguments)) {
			 return true;
		 }

		 int changeMultiplier = 0;
		 if (action == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) {
			 changeMultiplier = 1;
		 } else if (action == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) {
			 changeMultiplier = -1;
		 }
		 if (changeMultiplier != 0) {
			 int value = this.getCurrentlyShowingValue();
			 int stepSize = 0;
			 final int currentItemShowing = this.getCurrentItemShowing();
			 if (currentItemShowing == HOUR_INDEX) {
				 stepSize = HOUR_VALUE_TO_DEGREES_STEP_SIZE;
				 value %= 12;
			 } else if (currentItemShowing == MINUTE_INDEX) {
				 stepSize = MINUTE_VALUE_TO_DEGREES_STEP_SIZE;
			 }

			 int degrees = value * stepSize;
			 degrees = this.snapOnly30s(degrees, changeMultiplier);
			 value = degrees / stepSize;
			 int maxValue = 0;
			 int minValue = 0;
			 if (currentItemShowing == HOUR_INDEX) {
				 if (this.mIs24HourMode) {
					 maxValue = 23;
				 } else {
					 maxValue = 12;
					 minValue = 1;
				 }
			 } else {
				 maxValue = 55;
			 }
			 if (value > maxValue) {
				 // If we scrolled forward past the highest number, wrap around to the lowest.
				 value = minValue;
			 } else if (value < minValue) {
				 // If we scrolled backward past the lowest number, wrap around to the highest.
				 value = maxValue;
			 }
			 this.setItem(currentItemShowing, value);
			 this.mListener.onValueSelected(currentItemShowing, value, false);
			 return true;
		 }

		 return false;
	 }

	 /**
	  * Split up the 360 degrees of the circle among the 60 selectable values. Assigns a larger selectable area to each
	  * of the 12 visible values, such that the ratio of space apportioned to a visible value : space apportioned to a
	  * non-visible value will be 14 : 4. E.g. the output of 30 degrees should have a higher range of input associated
	  * with it than the output of 24 degrees, because 30 degrees corresponds to a visible number on the clock circle (5
	  * on the minutes, 1 or 13 on the hours).
	  */
	 private void preparePrefer30sMap() {
		 // We'll split up the visible output and the non-visible output such that each visible
		 // output will correspond to a range of 14 associated input degrees, and each non-visible
		 // output will correspond to a range of 4 associate input degrees, so visible numbers
		 // are more than 3 times easier to get than non-visible numbers:
		 // {354-359,0-7}:0, {8-11}:6, {12-15}:12, {16-19}:18, {20-23}:24, {24-37}:30, etc.
		 //
		 // If an output of 30 degrees should correspond to a range of 14 associated degrees, then
		 // we'll need any input between 24 - 37 to snap to 30. Working out from there, 20-23 should
		 // snap to 24, while 38-41 should snap to 36. This is somewhat counter-intuitive, that you
		 // can be touching 36 degrees but have the selection snapped to 30 degrees; however, this
		 // inconsistency isn't noticeable at such fine-grained degrees, and it affords us the
		 // ability to aggressively prefer the visible values by a factor of more than 3:1, which
		 // greatly contributes to the selectability of these values.

		 // Our input will be 0 through 360.
		 this.mSnapPrefer30sMap = new int[361];

		 // The first output is 0, and each following output will increment by 6 {0, 6, 12, ...}.
		 int snappedOutputDegrees = 0;
		 // Count of how many inputs we've designated to the specified output.
		 int count = 1;
		 // How many input we expect for a specified output. This will be 14 for output divisible
		 // by 30, and 4 for the remaining output. We'll special case the outputs of 0 and 360, so
		 // the caller can decide which they need.
		 int expectedCount = 8;
		 // Iterate through the input.
		 for (int degrees = 0; degrees < 361; degrees++) {
			 // Save the input-output mapping.
			 this.mSnapPrefer30sMap[degrees] = snappedOutputDegrees;
			 // If this is the last input for the specified output, calculate the next output and
			 // the next expected count.
			 if (count == expectedCount) {
				 snappedOutputDegrees += 6;
				 if (snappedOutputDegrees == 360) {
					 expectedCount = 7;
				 } else if ((snappedOutputDegrees % 30) == 0) {
					 expectedCount = 14;
				 } else {
					 expectedCount = 4;
				 }
				 count = 1;
			 } else {
				 count++;
			 }
		 }
	 }

	 /**
	  * For the currently showing view (either hours or minutes), re-calculate the position for the selector, and redraw
	  * it at that position. The input degrees will be snapped to a selectable value.
	  *
	  * @param degrees Degrees which should be selected.
	  * @param isInnerCircle Whether the selection should be in the inner circle; will be ignored if there is no inner
	  * circle.
	  * @param forceToVisibleValue Even if the currently-showing circle allows for fine-grained selection (i.e. minutes),
	  * force the selection to one of the visibly-showing values.
	  * @param forceDrawDot The dot in the circle will generally only be shown when the selection is on non-visible
	  * values, but use this to force the dot to be shown.
	  * @return The value that was selected, i.e. 0-23 for hours, 0-59 for minutes.
	  */
	 private int reselectSelector(int degrees, final boolean isInnerCircle,
			 final boolean forceToVisibleValue, final boolean forceDrawDot) {
		 if (degrees == -1) {
			 return -1;
		 }
		 final int currentShowing = this.getCurrentItemShowing();

		 int stepSize;
		 final boolean allowFineGrained = !forceToVisibleValue && (currentShowing == MINUTE_INDEX);
		 if (allowFineGrained) {
			 degrees = this.snapPrefer30s(degrees);
		 } else {
			 degrees = this.snapOnly30s(degrees, 0);
		 }

		 RadialSelectorView radialSelectorView;
		 if (currentShowing == HOUR_INDEX) {
			 radialSelectorView = this.mHourRadialSelectorView;
			 stepSize = HOUR_VALUE_TO_DEGREES_STEP_SIZE;
		 } else {
			 radialSelectorView = this.mMinuteRadialSelectorView;
			 stepSize = MINUTE_VALUE_TO_DEGREES_STEP_SIZE;
		 }
		 radialSelectorView.setSelection(degrees, isInnerCircle, forceDrawDot);
		 radialSelectorView.invalidate();

		 if (currentShowing == HOUR_INDEX) {
			 if (this.mIs24HourMode) {
				 if ((degrees == 0) && isInnerCircle) {
					 degrees = 360;
				 } else if ((degrees == 360) && !isInnerCircle) {
					 degrees = 0;
				 }
			 } else if (degrees == 0) {
				 degrees = 360;
			 }
		 } else if ((degrees == 360) && (currentShowing == MINUTE_INDEX)) {
			 degrees = 0;
		 }

		 int value = degrees / stepSize;
		 if ((currentShowing == HOUR_INDEX) && this.mIs24HourMode && !isInnerCircle && (degrees != 0)) {
			 value += 12;
		 }
		 return value;
	 }

	 /**
	  * Set the internal value as either AM or PM, and update the AM/PM circle displays.
	  */
	 public void setAmOrPm(final int amOrPm) {
		 this.mAmPmCirclesView.setAmOrPm(amOrPm);
		 this.mAmPmCirclesView.invalidate();
		 this.setValueForItem(AMPM_INDEX, amOrPm);
	 }

	 /**
	  * Set either minutes or hours as showing.
	  *
	  * @param animate True to animate the transition, false to show with no animation.
	  */
	 public void setCurrentItemShowing(final int index, final boolean animate) {
		 if ((index != HOUR_INDEX) && (index != MINUTE_INDEX)) {
			 Log.e(TAG, "TimePicker does not support view at index " + index);
			 return;
		 }

		 final int lastIndex = this.getCurrentItemShowing();
		 this.mCurrentItemShowing = index;

		 if (animate && (index != lastIndex)) {
			 final ObjectAnimator[] anims = new ObjectAnimator[4];
			 if (index == MINUTE_INDEX) {
				 anims[0] = this.mHourRadialTextsView.getDisappearAnimator();
				 anims[1] = this.mHourRadialSelectorView.getDisappearAnimator();
				 anims[2] = this.mMinuteRadialTextsView.getReappearAnimator();
				 anims[3] = this.mMinuteRadialSelectorView.getReappearAnimator();
			 } else if (index == HOUR_INDEX) {
				 anims[0] = this.mHourRadialTextsView.getReappearAnimator();
				 anims[1] = this.mHourRadialSelectorView.getReappearAnimator();
				 anims[2] = this.mMinuteRadialTextsView.getDisappearAnimator();
				 anims[3] = this.mMinuteRadialSelectorView.getDisappearAnimator();
			 }

			 if ((this.mTransition != null) && this.mTransition.isRunning()) {
				 this.mTransition.end();
			 }
			 this.mTransition = new AnimatorSet();
			 this.mTransition.playTogether(anims);
			 this.mTransition.start();
		 } else {
			 final int hourAlpha = (index == HOUR_INDEX) ? 255 : 0;
			 final int minuteAlpha = (index == MINUTE_INDEX) ? 255 : 0;
			 ViewHelper.setAlpha(this.mHourRadialTextsView, hourAlpha);
			 ViewHelper.setAlpha(this.mHourRadialSelectorView, hourAlpha);
			 ViewHelper.setAlpha(this.mMinuteRadialTextsView, minuteAlpha);
			 ViewHelper.setAlpha(this.mMinuteRadialSelectorView, minuteAlpha);
		 }

	 }

	 /**
	  * Set either the hour or the minute. Will set the internal value, and set the selection.
	  */
	 private void setItem(final int index, final int value) {
		 if (index == HOUR_INDEX) {
			 this.setValueForItem(HOUR_INDEX, value);
			 final int hourDegrees = (value % 12) * HOUR_VALUE_TO_DEGREES_STEP_SIZE;
			 this.mHourRadialSelectorView.setSelection(hourDegrees, this.isHourInnerCircle(value), false);
			 this.mHourRadialSelectorView.invalidate();
		 } else if (index == MINUTE_INDEX) {
			 this.setValueForItem(MINUTE_INDEX, value);
			 final int minuteDegrees = value * MINUTE_VALUE_TO_DEGREES_STEP_SIZE;
			 this.mMinuteRadialSelectorView.setSelection(minuteDegrees, false, false);
			 this.mMinuteRadialSelectorView.invalidate();
		 }
	 }

	 public void setOnValueSelectedListener(final OnValueSelectedListener listener) {
		 this.mListener = listener;
	 }

	 public void setTime(final int hours, final int minutes) {
		 this.setItem(HOUR_INDEX, hours);
		 this.setItem(MINUTE_INDEX, minutes);
	 }

	 /**
	  * Set the internal value for the hour, minute, or AM/PM.
	  */
	 private void setValueForItem(final int index, final int value) {
		 if (index == HOUR_INDEX) {
			 this.mCurrentHoursOfDay = value;
		 } else if (index == MINUTE_INDEX) {
			 this.mCurrentMinutes = value;
		 } else if (index == AMPM_INDEX) {
			 if (value == AM) {
				 this.mCurrentHoursOfDay = this.mCurrentHoursOfDay % 12;
			 } else if (value == PM) {
				 this.mCurrentHoursOfDay = (this.mCurrentHoursOfDay % 12) + 12;
			 }
		 }
	 }

	 /**
	  * Returns mapping of any input degrees (0 to 360) to one of 12 visible output degrees (all multiples of 30), where
	  * the input will be "snapped" to the closest visible degrees.
	  *
	  * @param degrees The input degrees
	  * @param forceAboveOrBelow The output may be forced to either the higher or lower step, or may be allowed to snap
	  * to whichever is closer. Use 1 to force strictly higher, -1 to force strictly lower, and 0 to snap to the closer
	  * one.
	  * @return output degrees, will be a multiple of 30
	  */
	 private int snapOnly30s(int degrees, final int forceHigherOrLower) {
		 final int stepSize = HOUR_VALUE_TO_DEGREES_STEP_SIZE;
		 int floor = (degrees / stepSize) * stepSize;
		 final int ceiling = floor + stepSize;
		 if (forceHigherOrLower == 1) {
			 degrees = ceiling;
		 } else if (forceHigherOrLower == -1) {
			 if (degrees == floor) {
				 floor -= stepSize;
			 }
			 degrees = floor;
		 } else {
			 if ((degrees - floor) < (ceiling - degrees)) {
				 degrees = floor;
			 } else {
				 degrees = ceiling;
			 }
		 }
		 return degrees;
	 }

	 /**
	  * Returns mapping of any input degrees (0 to 360) to one of 60 selectable output degrees, where the degrees
	  * corresponding to visible numbers (i.e. those divisible by 30) will be weighted heavier than the degrees
	  * corresponding to non-visible numbers. See {@link #preparePrefer30sMap()} documentation for the rationale and
	  * generation of the mapping.
	  */
	 private int snapPrefer30s(final int degrees) {
		 if (this.mSnapPrefer30sMap == null) {
			 return -1;
		 }
		 return this.mSnapPrefer30sMap[degrees];
	 }

	 /**
	  * Set touch input as enabled or disabled, for use with keyboard mode.
	  */
	 public boolean trySettingInputEnabled(final boolean inputEnabled) {
		 if (this.mDoingTouch && !inputEnabled) {
			 // If we're trying to disable input, but we're in the middle of a touch event,
			 // we'll allow the touch event to continue before disabling input.
			 return false;
		 }
		 this.mInputEnabled = inputEnabled;
		 this.mGrayBox.setVisibility(inputEnabled ? View.INVISIBLE : View.VISIBLE);
		 return true;
	 }

	 /**
	  * Try to vibrate. To prevent this becoming a single continuous vibration, nothing will happen if we have vibrated
	  * very recently.
	  */
	 public void tryVibrate() {
		 if (this.mVibrator != null) {
			 final long now = SystemClock.uptimeMillis();
			 // We want to try to vibrate each individual tick discretely.
			 if ((now - this.mLastVibrate) >= 125) {
				 this.mVibrator.vibrate(5);
				 this.mLastVibrate = now;
			 }
		 }
	 }
}
