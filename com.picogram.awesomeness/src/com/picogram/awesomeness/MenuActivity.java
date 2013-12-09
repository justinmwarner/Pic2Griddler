
package com.picogram.awesomeness;

import android.app.ActivityGroup;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;

import com.flurry.android.FlurryAdListener;
import com.flurry.android.FlurryAdSize;
import com.flurry.android.FlurryAdType;
import com.flurry.android.FlurryAds;
import com.flurry.android.FlurryAgent;
import com.stackmob.android.sdk.common.StackMobAndroid;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

@SuppressWarnings("deprecation")
public class MenuActivity extends ActivityGroup implements FlurryAdListener {
	protected static final String TAG = "MenuActivity";
	private TabHost th;
	public static String PREFS_FILE = "com.picogram.awesomeness_preferences";
	Handler h = new Handler();
	LinearLayout mBanner;
	SharedPreferences prefs = null;

	public void onAdClicked(final String arg0) {
	}

	public void onAdClosed(final String arg0) {
	}

	public void onAdOpened(final String arg0) {
	}

	public void onApplicationExit(final String arg0) {
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		Util.setTheme(this);
		this.setContentView(R.layout.activity_menu);
		this.prefs = this.getSharedPreferences(
				MenuActivity.PREFS_FILE, MODE_PRIVATE);
		final String user = this.prefs.getString("username", "N/A");
		StackMobAndroid.init(this.getApplicationContext(), 0,
				"f077e098-c678-4256-b7a2-c3061d9ff0c2");// Change to production.

		this.th = (TabHost) this.findViewById(R.id.thMain);
		this.th.setup(this.getLocalActivityManager());

		final TabSpec userSpec = this.th.newTabSpec("User");
		userSpec.setIndicator("User",
				this.getResources().getDrawable(R.drawable.icon_user_tab));
		final Intent userIntent = new Intent(this, UserGriddlers.class);
		userSpec.setContent(userIntent);

		final TabSpec worldSpec = this.th.newTabSpec("World");
		worldSpec.setIndicator("World",
				this.getResources().getDrawable(R.drawable.icon_world_tab));
		final Intent worldIntent = new Intent(this, WorldGriddlers.class);
		worldSpec.setContent(worldIntent);

		final TabSpec settingsSpec = this.th.newTabSpec("Settings");
		settingsSpec.setIndicator("Settings",
				this.getResources().getDrawable(R.drawable.icon_settings_tab));
		final Intent settingsIntent = new Intent(this, SettingsActivity.class);
		settingsSpec.setContent(settingsIntent);

		if (this.th != null) {
			this.th.addTab(userSpec);
			this.th.addTab(worldSpec);
			this.th.addTab(settingsSpec);
		}
		FlurryAgent.onStartSession(this, this.getResources().getString(R.string.flurry));
		FlurryAgent.setCaptureUncaughtExceptions(true);
		FlurryAgent.setLogEnabled(!this.prefs.getBoolean("analytics", false));
		FlurryAgent.setLogEvents(!this.prefs.getBoolean("logging", false));

		FlurryAgent.logEvent("App Started");
		this.mBanner = (LinearLayout) this.findViewById(R.id.flurryBanner);
		// allow us to get callbacks for ad events
		FlurryAds.setAdListener(this);
		FlurryAds.enableTestAds(true);

	}

	public void onRenderFailed(final String arg0) {
	}

	@Override
	public void onStart() {
		super.onStart();
		FlurryAgent.onStartSession(this, this.getResources().getString(R.string.flurry));
		// fetch and prepare ad for this ad space. won�t render one yet
		this.mBanner.setVisibility(!this.prefs.getBoolean("advertisements", false) ? View.VISIBLE
				: View.GONE);
		if (!this.prefs.getBoolean("advertisements", false)) {
			FlurryAds.fetchAd(this, "MainScreen", this.mBanner, FlurryAdSize.BANNER_BOTTOM);
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		FlurryAgent.onEndSession(this);
	}

	public void onVideoCompleted(final String arg0) {
	}

	public boolean shouldDisplayAd(final String arg0, final FlurryAdType arg1) {
		return true;
	}

	public void spaceDidFailToReceiveAd(final String arg0) {
	}

	public void spaceDidReceiveAd(final String adSpace) {
		FlurryAds.displayAd(this, adSpace, this.mBanner);
	}

	public void switchTab(final int tab) {
		FlurryAgent.logEvent("Switched to tab " + tab);
		if ((tab == 1) && !Util.isOnline())
		{
			Crouton.makeText(this, "Must be connected to the internet to use social aspects",
					Style.INFO).show();
			return;
		}
		this.th.setCurrentTab(tab);
	}

}
