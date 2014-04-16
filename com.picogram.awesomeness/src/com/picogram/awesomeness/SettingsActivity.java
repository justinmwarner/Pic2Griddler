
package com.picogram.awesomeness;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TableLayout;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.facebook.Session;
import com.facebook.widget.UserSettingsFragment;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.kopfgeldjaeger.ratememaybe.RateMeMaybe;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import de.psdev.licensesdialog.LicensesDialog;

public class SettingsActivity extends SherlockPreferenceActivity implements
		OnPreferenceChangeListener, OnPreferenceClickListener {
	private static final String TAG = "SettingsActivity";
	SharedPreferences prefs;
	boolean continueMusic = true;

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.setContentView(R.layout.activity_settings);

		this.addPreferencesFromResource(R.xml.preferences);

		this.prefs = this.getSharedPreferences("Picogram", 0);
		this.findPreference("music").setOnPreferenceChangeListener(this);
		this.findPreference("email").setOnPreferenceClickListener(this);
		this.findPreference("advertisements").setOnPreferenceClickListener(this);
		this.findPreference("changelog").setOnPreferenceClickListener(this);
		this.findPreference("analytics").setOnPreferenceClickListener(this);
		this.findPreference("logging").setOnPreferenceClickListener(this);
		this.findPreference("crashes").setOnPreferenceClickListener(this);
		this.findPreference("licenses").setOnPreferenceClickListener(this);
		this.findPreference("rateapp").setOnPreferenceClickListener(this);
		this.findPreference("statistics").setOnPreferenceClickListener(this);
		this.findPreference("logoutgoogle").setOnPreferenceClickListener(this);
		this.findPreference("logoutfacebook").setOnPreferenceClickListener(this);
		this.findPreference("resetusername").setOnPreferenceClickListener(this);

		final ActionBar ab = this.getSupportActionBar();
		if (ab != null) {
			ab.show();
			ab.setDisplayHomeAsUpEnabled(true);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		return super.onCreateOptionsMenu(menu);
	}

	public void onDisconnected() {
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {

		super.onOptionsItemSelected(item);

		switch (item.getItemId()) {
			case android.R.id.home:
				this.finish();
				break;
		}
		return true;
	}

	@Override
	public void onPause() {
		super.onPause();
		if (!this.continueMusic) {
			MusicManager.pause();
		}
	}

	public boolean onPreferenceChange(final Preference preference,
			final Object newValue) {
		if (preference.getKey().equals("music")) {
			this.prefs.edit().putString("music", newValue.toString()).commit();
			MusicManager.start(this, newValue.toString(), true);
		}
		return true;
	}

	public boolean onPreferenceClick(final Preference preference) {
		if (preference.getKey().equals("statistics")) {
			//TODO
			Crouton.makeText(this, "This is not yet implemented", Style.INFO).show();
			final AlertDialog dialog = new AlertDialog.Builder(this).create();
			final String[] scoresTitles = new String[] {
					"Games Played", "Games Won", "Taps", "Taps per Puzzle", "Tapes per Minute", "Times Played"
			};
			final int gamesPlayed = 0, gamesWon = 0, taps = 0, tapsPerPuzzle = 0, tapsPerMinute = 0, timePlayed = 0;
			final int[] scores = {
					gamesPlayed, gamesWon, taps, tapsPerPuzzle, tapsPerMinute, timePlayed
			};
			// TODO: Implement the preferences and what not.
			final LinearLayout ll = new LinearLayout(this);
			ll.setOrientation(LinearLayout.VERTICAL);
			for (int i = 0; i != scores.length; ++i)
			{
				final LinearLayout sub = new LinearLayout(this);
				sub.setLayoutParams(new LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.FILL_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
				sub.setOrientation(LinearLayout.HORIZONTAL);
				TextView tv = new TextView(this);
				tv.setLayoutParams(new TableLayout.LayoutParams(android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
				tv.setText(scoresTitles[i]);
				sub.addView(tv);
				tv = new TextView(this);
				tv.setLayoutParams(new TableLayout.LayoutParams(android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
				tv.setText(scores[i] + "");
				sub.addView(tv);
				ll.addView(sub);
			}
			dialog.setView(ll);
			dialog.setButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(final DialogInterface dialog, final int which) {
					dialog.dismiss();
				}
			});
			dialog.getWindow().getAttributes().windowAnimations = R.style.DialogTheme;
			dialog.show();
			dialog.dismiss();

			return true;
		} else if (preference.getKey().equals("changelog")) {
			// Launch change log dialog
			final ChangeLogDialog _ChangelogDialog = new ChangeLogDialog(this);
			_ChangelogDialog.show();
		} else if (preference.getKey().equals("licenses")) {
			// Launch the licenses stuff.
			Dialog ld = new LicensesDialog(this, R.raw.licenses, false, false).create();
			ld.getWindow().getAttributes().windowAnimations = R.style.DialogTheme;
			ld.show();
		} else if (preference.getKey().equals("email")) {
			final String email = "warner.73+Picogram@wright.edu";
			final String subject = "Picogram - <SUBJECT>";
			final String message = "Picogram,\n\n<MESSAGE>";
			// Contact me.
			final Intent emailIntent = new Intent(Intent.ACTION_SEND);
			emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] {
					email
			});
			emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
			emailIntent.putExtra(Intent.EXTRA_TEXT, message);
			emailIntent.setType("message/rfc822");
			this.startActivity(Intent.createChooser(emailIntent,
					"Send Mail Using :"));
			overridePendingTransition(R.anim.fadein, R.anim.exit_left);
		} else if (preference.getKey().equals("rateapp")) {
			// TODO fix this when we publish.
			this.startActivity(new Intent(Intent.ACTION_VIEW,
					Uri.parse("market://details?id=Picogram")));
			overridePendingTransition(R.anim.fadein, R.anim.exit_left);
			final Editor editor = this.prefs.edit();
			editor.putBoolean(RateMeMaybe.PREF.DONT_SHOW_AGAIN, true);
			editor.commit();
		}
		else if (preference.getKey().equals("logoutgoogle"))
		{
			//TODO
			Crouton.makeText(this, "This is not currently supported.", Style.INFO).show();
		}
		else if (preference.getKey().equals("logoutfacebook"))
		{
			//TODO
			Crouton.makeText(this, "This is not currently supported.", Style.INFO).show();
		}
		else if (preference.getKey().equals("resetusername"))
		{
			Util.getPreferences(this).edit().putString("username", "").commit();
			Util.getPreferences(this).edit().putBoolean("hasLoggedInUsername", false).commit();
		}
		return false;
	}

	@Override
	public void onResume() {
		super.onResume();
		Util.updateFullScreen(this);
		this.continueMusic = false;
		MusicManager.start(this);
	}

	public void optOut() {
	}
}
