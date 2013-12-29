package com.picogram.awesomeness;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import nonogram.Solver;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.RatingBar.OnRatingBarChangeListener;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.flurry.android.FlurryAgent;
import com.picogram.awesomeness.DialogMaker.OnDialogResultListener;
import com.picogram.awesomeness.TouchImageView.HistoryListener;
import com.picogram.awesomeness.TouchImageView.WinnerListener;
import com.stackmob.sdk.callback.StackMobCallback;
import com.stackmob.sdk.callback.StackMobModelCallback;
import com.stackmob.sdk.exception.StackMobException;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

public class AdvancedGameActivity extends FragmentActivity implements
		OnTouchListener, WinnerListener, View.OnClickListener,
		OnSeekBarChangeListener {
	private static final String TAG = "AdvancedGameActivity";
	TouchImageView tiv;
	Handler handle = new Handler();
	int tutorialStep = 0;
	private static SQLitePicogramAdapter sql;
	int colors[];
	String strColors[];
	ArrayList<ImageView> ivs = new ArrayList<ImageView>();

	String puzzleId;

	boolean isDialogueShowing = false;

	private void doFacebookStuff() {
	}

	private void doTwitterStuff() {
	}

	private int[] getRGB(final int i) {

		final int a = (i >> 24) & 0xff;
		final int r = (i >> 16) & 0xff;
		final int g = (i >> 8) & 0xff;
		final int b = (i & 0xff);
		return new int[] { a, r, g, b };
	}

	@Override
	public void onBackPressed() {
		super.onPause();
		this.returnIntent();
	}

	private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context arg0, Intent intent) {
			// TODO Auto-generated method stub
			// this will give you battery current status
			ImageView ivBattery = (ImageView) findViewById(R.id.ivBattery);
			int level = intent.getIntExtra("level", 0);
			int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
			boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING
					|| status == BatteryManager.BATTERY_STATUS_FULL;
			if (isCharging) {
				if (isLight) {
					ivBattery.setImageBitmap(BitmapFactory.decodeResource(
							getResources(), R.drawable.batterychargingdark));
				} else {
					ivBattery.setImageBitmap(BitmapFactory.decodeResource(
							getResources(), R.drawable.batterycharginglight));
				}
			} else {
				if (level >= 95) {
					if (isLight) {
						ivBattery.setImageBitmap(BitmapFactory.decodeResource(
								getResources(), R.drawable.batteryfulldark));
					} else {
						ivBattery.setImageBitmap(BitmapFactory
								.decodeResource(getResources(),
										R.drawable.batterycharginglight));
					}
				} else if (level >= 30) {
					if (isLight) {
						ivBattery.setImageBitmap(BitmapFactory.decodeResource(
								getResources(), R.drawable.batteryhalfdark));
					} else {
						ivBattery.setImageBitmap(BitmapFactory.decodeResource(
								getResources(), R.drawable.batteryhalflight));
					}
				} else if (level >= 5) {
					if (isLight) {
						ivBattery.setImageBitmap(BitmapFactory.decodeResource(
								getResources(), R.drawable.batterylowdark));
					} else {
						ivBattery.setImageBitmap(BitmapFactory.decodeResource(
								getResources(), R.drawable.batterylowlight));
					}
				} else {
					if (isLight) {
						ivBattery.setImageBitmap(BitmapFactory.decodeResource(
								getResources(), R.drawable.batteryemptydark));
					} else {
						ivBattery.setImageBitmap(BitmapFactory.decodeResource(
								getResources(), R.drawable.batteryemptylight));
					}
				}
			}
		}
	};
	private RefreshHandler mRedrawHandler = new RefreshHandler();

	class RefreshHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			// Do the time
			DateFormat df = new SimpleDateFormat("h:m:ss");
			String curDateTime = df.format(Calendar.getInstance().getTime());
			((TextView) activity.findViewById(R.id.tvTime))
					.setText(curDateTime);
			// Do the battery.
			registerReceiver(mBatInfoReceiver, new IntentFilter(
					Intent.ACTION_BATTERY_CHANGED));
			sleep(100, activity);
		}

		Activity activity;

		public void sleep(long delayMillis, Activity a) {
			this.removeMessages(0);
			this.activity = a;
			sendMessageDelayed(obtainMessage(0), delayMillis);
		}
	}

	boolean isLight;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.setContentView(R.layout.activity_advanced_game);
		this.tiv = (TouchImageView) this.findViewById(R.id.tivGame);
		// Do background stuff.
		RelativeLayout rlMain = (RelativeLayout) this
				.findViewById(R.id.rlGameActivity);
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);

		String bg = Util.getPreferences(this)
				.getString("background", "bgWhite");
		String line = Util.getPreferences(this).getString("lines", "Auto");
		boolean isAnimating = false;
		if (bg.equals("bgWhite")) {
			rlMain.setBackgroundResource(android.R.color.transparent);
			rlMain.setBackgroundColor(Color.WHITE);
			line = Color.BLACK + "";
		} else if (bg.equals("bgBlack")) {
			rlMain.setBackgroundResource(android.R.color.transparent);
			rlMain.setBackgroundColor(Color.BLACK);
			line = Color.WHITE + "";
		} else if (bg.equals("skywave")) {
			isAnimating = true;
			line = Color.BLACK + "";
			rlMain.setBackgroundResource(R.drawable.skywave);
		} else if (bg.equals("darkbridge")) {
			isAnimating = true;
			rlMain.setBackgroundResource(R.drawable.darkbridge);
			line = Color.WHITE + "";
		} else if (bg.equals("spaceman")) {
			isAnimating = true;
			rlMain.setBackgroundResource(R.drawable.spaceman);
			line = Color.WHITE + "";
		} else {
			rlMain.setBackgroundResource(android.R.color.transparent);
			rlMain.setBackgroundColor(Color.WHITE);
			line = Color.BLACK + "";
		}
		if (Util.getPreferences(this).getString("lines", "Auto").equals("Auto")) {
			tiv.gridlinesColor = Integer.parseInt(line);
			if (line.equals("" + Color.WHITE)) {
				isLight = false;
			} else {
				isLight = true;
			}
		} else {
			if (Util.getPreferences(this).getString("lines", "Auto")
					.equals("Light")) {
				tiv.gridlinesColor = Color.BLACK;
				isLight = true;
			} else {
				tiv.gridlinesColor = Color.WHITE;
				isLight = false;
			}
		}

		if (isAnimating) {
			AnimationDrawable progressAnimation = (AnimationDrawable) findViewById(
					R.id.rlGameActivity).getBackground();
			progressAnimation.start();
		}
		Util.setTheme(this);

		// Time
		mRedrawHandler.sleep(100, this);
		if (isLight)
			((TextView) findViewById(R.id.tvTime)).setTextColor(Color.BLACK);
		else
			((TextView) findViewById(R.id.tvTime)).setTextColor(Color.WHITE);

		this.tiv.setWinListener(this);
		tiv.setPicogramInfo(getIntent().getExtras());

		final String name = this.getIntent().getExtras().getString("name");
		final String c = this.getIntent().getExtras().getString("current");
		final String s = this.getIntent().getExtras().getString("solution");
		this.puzzleId = this.getIntent().getExtras().getString("id");
		FlurryAgent.logEvent("UserPlayingGame");
		// Create colors for pallet.
		final String thing = this.getIntent().getExtras().getString("colors");
		strColors = this.getIntent().getExtras().getString("colors").split(",");
		this.colors = new int[strColors.length];
		for (int i = 0; i != strColors.length; ++i) {
			this.colors[i] = Integer.parseInt(strColors[i]);
		}
		// History Bar.
		sbHistory = (SeekBar) findViewById(R.id.sbHistory);
		sbHistory.setOnSeekBarChangeListener(this);
		Button bUndo = (Button) findViewById(R.id.bUndo);
		Button bRedo = (Button) findViewById(R.id.bRedo);
		bUndo.setOnClickListener(this);
		bRedo.setOnClickListener(this);

		final Vibrator myVib = (Vibrator) this
				.getSystemService(VIBRATOR_SERVICE);
		historyListener = new HistoryListener() {

			public void action(String curr) {
				myVib.vibrate(40);
				if (sbHistory.getProgress() != sbHistory.getMax()) {
					for (int i = sbHistory.getProgress(); i != sbHistory
							.getMax(); ++i) {
						history.remove(history.size() - 1);
					}
					sbHistory.setMax(sbHistory.getProgress());
				}
				sbHistory.setMax(sbHistory.getMax() + 1);
				sbHistory.setProgress(sbHistory.getMax());
				history.add(curr);
				isFirstUndo = true;
			}
		};
		tiv.setHistoryListener(historyListener);

		ImageButton tools = (ImageButton) this.findViewById(R.id.ibTools);
		tools.setOnClickListener(this);

		// TODO Check for multiple solutions. If they exist tell the user as a
		// heads up.
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		this.getMenuInflater().inflate(R.menu.activity_advanced_game, menu);
		return true;
	}

	@Override
	public void onPause() {
		super.onPause();
		sql.updateCurrentPicogram(this.tiv.gSolution.hashCode() + "", "0",
				this.tiv.gCurrent);
		sql.close();

		if (!continueMusic) {
			MusicManager.pause();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		Util.updateFullScreen(this);
		sql = new SQLitePicogramAdapter(this.getApplicationContext(),
				"Picograms", null, 1);
		continueMusic = false;
		MusicManager.start(this, MusicManager.MUSIC_MENU);
	}

	public boolean onTouch(final View v, final MotionEvent event) {
		final int index = this.ivs.indexOf(v);
		if (index < 0) {
			this.tiv.isGameplay = false;
		} else {
			this.tiv.isGameplay = true;
			this.tiv.colorCharacter = (index + "").charAt(0);
		}
		return true;
	}

	private void returnIntent() {

		final Intent returnIntent = new Intent();
		returnIntent.putExtra("current", this.tiv.gCurrent);
		if (tiv.gCurrent.equals(tiv.gSolution)) {
			returnIntent.putExtra("status", "1");
		} else
			returnIntent.putExtra("status", "0");
		returnIntent.putExtra("ID", this.tiv.gSolution.hashCode() + "");
		this.setResult(Activity.RESULT_OK, returnIntent);
		this.finish();
	}

	public void win() {
		final Dialog dialog = new Dialog(AdvancedGameActivity.this);
		dialog.setContentView(R.layout.dialog_ranking);
		dialog.setTitle("Rate this Picogram");
		dialog.setCancelable(false);
		final Activity a = this;

		final RatingBar rb = (RatingBar) dialog.findViewById(R.id.rbRate);
		rb.setOnRatingBarChangeListener(new OnRatingBarChangeListener() {

			public void onRatingChanged(final RatingBar ratingBar,
					final float rating, final boolean fromUser) {
				if (fromUser) {
					final GriddlerOne g = new GriddlerOne();
					g.setID(AdvancedGameActivity.this.puzzleId);
					g.fetch(new StackMobModelCallback() {

						@Override
						public void failure(final StackMobException arg0) {
							// If rating failed, do it next time we can, so add
							// to database.
							SQLiteRatingAdapter sorh = new SQLiteRatingAdapter(
									a.getApplicationContext(), "Rating", null,
									1);
							dialog.dismiss();
							AdvancedGameActivity.this.returnIntent();
						}

						@Override
						public void success() {
							double oldRating = Double.parseDouble(g.getRating())
									* g.getNumberOfRatings();
							double newRating = (oldRating + rating)
									/ (g.getNumberOfRatings() + 1);
							g.setRating(newRating + "");
							g.setNumberOfRatings(g.getNumberOfRatings() + 1);
							// TODO: If save fails, let us do it next time app
							// is online.
							g.save(new StackMobCallback() {

								@Override
								public void failure(StackMobException arg0) {
									// Save the rating in the rating table, but
									// we failed, so add it as a no rating yet,
									// then a future rating.
									SQLiteRatingAdapter sorh = new SQLiteRatingAdapter(
											a.getApplicationContext(),
											"Rating", null, 2);
									dialog.dismiss();
									AdvancedGameActivity.this.returnIntent();
								}

								@Override
								public void success(String arg0) {
									// Save the rating in the rating table.
									// If successful, we want to just add the
									// past rating and 0 for future.
									SQLiteRatingAdapter sorh = new SQLiteRatingAdapter(
											a.getApplicationContext(),
											"Rating", null, 2);
									dialog.dismiss();
									AdvancedGameActivity.this.returnIntent();
								}
							});

						}

					});
				}

			}
		});
		if (!this.isDialogueShowing) {
			dialog.show();
			this.isDialogueShowing = !this.isDialogueShowing;
		}
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		tiv.gCurrent = savedInstanceState.getString("current");
		tiv.colorCharacter = savedInstanceState.getChar("drawCharacter");
		tiv.isGameplay = savedInstanceState.getBoolean("isGame");
		this.history = savedInstanceState.getStringArrayList("history");
		sbHistory.setMax(savedInstanceState.getInt("sbMax"));
		sbHistory.setProgress(savedInstanceState.getInt("sbProgress"));
		tiv.bitmapFromCurrent();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString("current", tiv.gCurrent);
		outState.putChar("drawCharacter", tiv.colorCharacter);
		outState.putBoolean("isGame", tiv.isGameplay);
		outState.putStringArrayList("history", history);
		outState.putInt("sbMax", sbHistory.getMax());
		outState.putInt("sbProgress", sbHistory.getProgress());
	}

	boolean isFirstUndo = true;

	public void onClick(View v) {
		char[] curr = tiv.gCurrent.toCharArray();
		if (v.getId() == R.id.bUndo) {
			if (sbHistory.getProgress() == 0) {
				return;
			}
			if (isFirstUndo) {
				isFirstUndo = false;

				history.add(tiv.gCurrent);
				sbHistory.setMax(sbHistory.getMax() + 1);

				sbHistory.setProgress(sbHistory.getProgress() - 1);
				tiv.gCurrent = history.get(sbHistory.getProgress());
			}
		} else if (v.getId() == R.id.bRedo) {
			if (sbHistory.getProgress() == sbHistory.getMax() - 1) {
				return;
			}
			tiv.gCurrent = history.get(sbHistory.getProgress() + 1);
			sbHistory.setProgress(sbHistory.getProgress() + 1);
		} else if (v.getId() == R.id.ibTools) {
			Bundle bundle = new Bundle();
			FragmentTransaction ft = getSupportFragmentManager()
					.beginTransaction();
			bundle.putInt("layoutId", R.layout.dialog_color_choice);
			bundle.putStringArray("colors", strColors);
			final DialogMaker newFragment = new DialogMaker();
			newFragment.setArguments(bundle);
			newFragment.setOnDialogResultListner(new OnDialogResultListener() {

				public void onDialogResult(Bundle result) {
					if (result.containsKey("colors")) {
						tiv.gColors = result.getIntArray("colors");
						tiv.isFirstTime = true;
						tiv.bitmapFromCurrent();
						String[] cols = new String[tiv.gColors.length];
						for (int i = 0; i != cols.length; ++i)
							cols[i] = "" + tiv.gColors[i];
						strColors = cols;
						tiv.colorCharacter = (result.getInt("color") + "")
								.charAt(0);
						tiv.isGameplay = true;
						newFragment.dismiss();
					} else {
						tiv.isGameplay = result.getBoolean("isGameplay");
						tiv.colorCharacter = result.getChar("colorCharacter");
						newFragment.dismiss();
					}
				}
			});
			newFragment.show(ft, "dialog");
			return;
		}
		tiv.bitmapFromCurrent();
	}

	boolean continueMusic = true;

	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		// TODO Auto-generated method stub
		// Ignore if done programmatically.
		if (fromUser) {
			if (!(progress >= history.size())) {
				tiv.gCurrent = history.get(progress);
				tiv.bitmapFromCurrent();
			}
		}
	}

	public void onStartTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub

	}

	public void onStopTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub

	}

	HistoryListener historyListener;
	SeekBar sbHistory;
	ArrayList<String> history = new ArrayList<String>();
}
