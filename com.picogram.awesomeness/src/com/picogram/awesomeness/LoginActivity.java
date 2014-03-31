
package com.picogram.awesomeness;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;

import com.facebook.LoggingBehavior;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.Settings;
import com.google.android.gms.games.Player;
import com.google.example.games.basegameutils.BaseGameActivity;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class LoginActivity extends BaseGameActivity implements OnClickListener {
	private class SessionStatusCallback implements Session.StatusCallback {
		public void call(final Session session, final SessionState state, final Exception exception) {
			Log.d("LoginUsingLoginFragmentActivity", String.format("New session state: %s", state.toString()));
			LoginActivity.this.updateView();
			if (state == SessionState.OPENED) {
				Util.getPreferences(LoginActivity.this.a).edit().putBoolean("hasLoggedInSuccessfully", true).commit();
			}
		}
	}
	final Activity a = this;

	private static final String TAG = "LoginActivity";
	AutoCompleteTextView editTextLogin;
	boolean hasTriedGoogle = false;
	private final Session.StatusCallback statusCallback = new SessionStatusCallback();

	Button facebook;

	@Override
	public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
	}

	public void onClick(final View v) {
		if (v.getId() == R.id.bFacebookLogin) {
			Session session = Session.getActiveSession();
			if (session == null) {
				session = new Session(this);
				Session.setActiveSession(session);
				if (session.getState().equals(SessionState.CREATED_TOKEN_LOADED)) {
					session.openForRead(new Session.OpenRequest(this).setCallback(this.statusCallback));
				}
			}
			session = Session.getActiveSession();
			if (!session.isOpened() && !session.isClosed()) {
				session.openForRead(new Session.OpenRequest(this).setCallback(this.statusCallback));
			} else {
				Session.openActiveSession(this, true, this.statusCallback);
			}
		}
		else if (v.getId() == R.id.bGoogleLogin) {
			this.hasTriedGoogle = true;
			this.beginUserInitiatedSignIn();
		}
		else if (v.getId() == R.id.bLogin) {
			final String un = this.editTextLogin.getText().toString();
			Util.getPreferences(this).edit().putString("username", un).commit();
			Crouton.makeText(this, "You're assigned the username " + un, Style.CONFIRM).show();
			// this.finish();
		}
	}

	private void onClickLogin() {
		final Session session = Session.getActiveSession();
		if (!session.isOpened() && !session.isClosed()) {
			session.openForRead(new Session.OpenRequest(this).setCallback(this.statusCallback));
		} else {
			Session.openActiveSession(this, true, this.statusCallback);
		}
	}

	private void onClickLogout() {
		final Session session = Session.getActiveSession();
		if (!session.isClosed()) {
			session.closeAndClearTokenInformation();
			Util.getPreferences(this).edit().putBoolean("hasLoggedInSuccessfully", false).commit();
		}
	}

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_login);

		this.facebook = (Button) this.findViewById(R.id.bFacebookLogin);
		final Button google = (Button) this.findViewById(R.id.bGoogleLogin);
		final Button login = (Button) this.findViewById(R.id.bLogin);
		Settings.addLoggingBehavior(LoggingBehavior.INCLUDE_ACCESS_TOKENS);

		Session session = Session.getActiveSession();
		if (session == null) {
			if (savedInstanceState != null) {
				session = Session.restoreSession(this, null, this.statusCallback, savedInstanceState);
			}
			if (session == null) {
				session = new Session(this);
			}
			Session.setActiveSession(session);
			if (session.getState().equals(SessionState.CREATED_TOKEN_LOADED)) {
				session.openForRead(new Session.OpenRequest(this).setCallback(this.statusCallback));
			}
		}

		this.updateView();

		this.facebook.setOnClickListener(this);
		this.facebook.setOnClickListener(this);
		google.setOnClickListener(this);
		login.setOnClickListener(this);

		// Get account names on device.
		this.editTextLogin = (AutoCompleteTextView) this.findViewById(R.id.actvUsername);
		final Account[] accounts = AccountManager.get(this).getAccounts();
		final Set<String> emailSet = new HashSet<String>();
		for (final Account account : accounts) {
			String acc = account.name;
			if (acc.contains("@")) {
				acc = acc.substring(0, acc.indexOf('@'));
			}
			emailSet.add(acc.toLowerCase());
		}
		this.editTextLogin.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, new ArrayList<String>(emailSet)));

		Crouton.makeText(this, "Your username is: " + Util.id(this), Style.INFO);
	}

	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		final Session session = Session.getActiveSession();
		Session.saveSession(session, outState);
	}

	public void onSignInFailed() {
		if (this.hasTriedGoogle) {
			Crouton.makeText(this, "Failed to login with Google.", Style.ALERT).show();
		}
	}

	public void onSignInSucceeded() {
		final Player p = this.getGamesClient().getCurrentPlayer();
		Crouton.makeText(this, "Logged in with Google " + p.getDisplayName() + ".", Style.ALERT).show();
		Util.getPreferences(this).edit().putBoolean("hasLoggedInSuccessfully", true).commit();
		Log.d(TAG, p.getDisplayName());
	}
	@Override
	public void onStart() {
		super.onStart();
		Session.getActiveSession().addCallback(this.statusCallback);
	}

	@Override
	public void onStop() {
		super.onStop();
		Session.getActiveSession().removeCallback(this.statusCallback);
	}

	private void updateView() {
		final Session session = Session.getActiveSession();
		if (session.isOpened()) {
			this.facebook.setText("Log out of Facebook");
			this.facebook.setOnClickListener(new OnClickListener() {
				public void onClick(final View view) { LoginActivity.this.onClickLogout(); }
			});
		} else {
			this.facebook.setText("Log in with Facebook");
			this.facebook.setOnClickListener(new OnClickListener() {
				public void onClick(final View view) { LoginActivity.this.onClickLogin(); }
			});
		}
	}
}
