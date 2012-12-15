package com.pic2griddler.awesomeness;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import com.google.analytics.tracking.android.EasyTracker;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class UserGriddlers extends Activity implements OnTouchListener, OnItemClickListener
{
	protected static final String TAG = "UserGriddlers";
	private SharedPreferences.Editor edit;
	private final String FILENAME = "USER_GRIDDLERS", SETTINGS = "USER_SETTINGS";
	private GriddlerMenuAdapter gma;
	private ArrayList<Griddler> griddlers = new ArrayList<Griddler>();
	// private String[] ids = null, statuses = null, names = null, diffs = null,
	// rates = null, infos = null;
	private ListView lv;
	private SharedPreferences settings;
	private SQLiteGriddlerAdapter sql;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_user_griddlers);
		EasyTracker.getInstance().setContext(this);
		EasyTracker.getInstance().activityStart(this);
		lv = (ListView) findViewById(R.id.lvUser);
		// Grab all the Griddlers on local drive.
		// IE: The ones the user started on.
		// Also show the create a Griddler and Tutorial Griddler.
		sql = new SQLiteGriddlerAdapter(this.getApplicationContext(), "Griddlers", null, 1);
		loadGriddlers();
		lv.setOnItemClickListener(this);

	}

	private void loadGriddlers()
	{
		GriddlerListAdapter adapter = new GriddlerListAdapter(this, R.id.lvUser);
		griddlers.clear(); // Clear all old info.
		adapter.setGriddlers(griddlers);

		lv.setAdapter(null);
		String[][] griddlers = sql.getGriddlers();
		Griddler tempGriddler = new Griddler();
		for (int i = 0; i < griddlers.length; i++)
		{
			String temp[] = griddlers[i];
			String id = temp[0];
			String name = temp[2];
			String rate = temp[3];
			String width = temp[7];
			String height = temp[8];
			String current = temp[4];
			String solution = temp[5];
			String diff = temp[6];
			String author = temp[1];
			Log.d(TAG, "Author: " + author);
			String status;
			if (temp[4].equals(temp[5]))
			{
				if (name.equals("Create a Griddler"))
				{
					status = 2 + "";
				}
				else
				{
					status = 1 + "";
				}
			}
			else
			{
				status = 0 + "";
			}
			tempGriddler = new Griddler(id, status, name, diff, rate, author, width, height, solution, current);
			this.griddlers.add(tempGriddler);
		}
		lv.setAdapter(adapter);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		// These could be compiled in to one, but for now, just keep it as is
		// for simplicity.
		if (resultCode == RESULT_OK)
		{
			// New Girddler, add to database.
			final String id = data.getStringExtra("solution").hashCode() + "";
			final String status = "0";
			final String solution = data.getStringExtra("solution");
			final String author = data.getStringExtra("author");
			final String name = data.getStringExtra("name");
			final String rank = data.getStringExtra("rank");
			final String difficulty = data.getStringExtra("difficulty");
			final String width = data.getStringExtra("width");
			final String height = data.getStringExtra("height");
			final String tags = data.getStringExtra("tags");
			sql.addUserGriddler(id, author, name, rank, solution, difficulty, width, height, status);
			loadGriddlers();
			// Now submit it to the online network.

			Thread t = new Thread(new Runnable()
			{

				public void run()
				{
					try
					{
						HttpClient hc = new DefaultHttpClient();
						String url = "http:// www.pic2griddler.appspot.com/create?id=" + id + "&author=" + author + "&name=" + name + "&rank=" + rank + "&diff=" + difficulty + "&width=" + width
								+ "&height=" + height + "&solution=" + solution + "&tags=" + tags.toLowerCase();
						url = url.replace(" ", "");
						HttpGet hg = new HttpGet(url);
						HttpResponse r = hc.execute(hg);

						StatusLine sl = r.getStatusLine();
						if (sl.getStatusCode() == HttpStatus.SC_OK)
						{
							ByteArrayOutputStream out = new ByteArrayOutputStream();
							r.getEntity().writeTo(out);
							out.close();
							Log.d(TAG, out.toString());
						}
						else
						{
							r.getEntity().getContent().close();
							Log.d(TAG, sl.getReasonPhrase().toString());
						}
					}
					catch (ClientProtocolException e)
					{
						e.printStackTrace();
					}
					catch (IOException e)
					{
						e.printStackTrace();
					}
				}
			});
			t.start();
		}
		else if (resultCode == 2)
		{
			// Back button pushed or won.
			String id = data.getStringExtra("ID");
			String status = data.getStringExtra("status");
			String current = data.getStringExtra("current");
			sql.updateCurrentGriddler(id, status, current);
			loadGriddlers();
		}
		else
		{
			// Nothing added.
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.activity_user_griddlers, menu);
		return true;
	}

	public void onItemClick(AdapterView<?> parent, View v, int pos, long id)
	{
		Log.d(TAG, "You selected: " + parent.getSelectedItemPosition());
		Log.d(TAG, "or you selected: " + pos);
		if (pos >= 0)
		{
			if (pos == 0)
			{
				// Start Create.
				Intent createIntent = new Intent(this, CreateGriddlerActivity.class);
				sql.close();
				this.startActivityForResult(createIntent, 1);
			}
			else
			{
				// Start game with info!
				Intent gameIntent = new Intent(this, GameActivity.class);
				sql.close();
				gameIntent.putExtra("solution", griddlers.get(pos).getSolution());
				gameIntent.putExtra("current", griddlers.get(pos).getCurrent());
				gameIntent.putExtra("width", griddlers.get(pos).getWidth());
				gameIntent.putExtra("height", griddlers.get(pos).getHeight());
				gameIntent.putExtra("id", griddlers.get(pos).getId());
				this.startActivityForResult(gameIntent, 2);
			}
		}
	}

	int yPrev;

	public boolean onTouch(View v, MotionEvent me)
	{
		Log.d(TAG, "Touched: " + lv.pointToPosition((int) me.getX(), (int) me.getY()));
		if (me.getAction() == MotionEvent.ACTION_DOWN)
		{
			yPrev = new Date().getSeconds();
		}
		if (me.getAction() == MotionEvent.ACTION_UP)
		{
			if (yPrev + 20 < me.getY() || yPrev - 20 > me.getY())
			{
				int pos = lv.pointToPosition((int) me.getX(), (int) me.getY());
				if (pos >= 0)
				{
					if (pos == 0)
					{
						// Start Create.
						Intent createIntent = new Intent(this, CreateGriddlerActivity.class);
						sql.close();
						this.startActivityForResult(createIntent, 1);
						return false;
					}
					else
					{
						// Start game with info!
						Intent gameIntent = new Intent(this, GameActivity.class);
						sql.close();
						gameIntent.putExtra("solution", griddlers.get(pos).getSolution());
						gameIntent.putExtra("current", griddlers.get(pos).getCurrent());
						gameIntent.putExtra("width", griddlers.get(pos).getWidth());
						gameIntent.putExtra("height", griddlers.get(pos).getHeight());
						gameIntent.putExtra("id", griddlers.get(pos).getId());
						this.startActivityForResult(gameIntent, 2);
						return false;
					}
				}
				return false;
			}
			else
			{
				return false;
			}
		}
		else
		{
			return false;
		}
	}

	@Override
	public void onStart()
	{
		super.onStart();
		// Add this method.
		Log.d(TAG, "*******************************Start: " + EasyTracker.getTracker().getTrackingId());
	}

	@Override
	public void onStop()
	{
		super.onStop();
		EasyTracker.getInstance().activityStop(this); // Add this method.
		Log.d(TAG, "Stop: " + EasyTracker.getTracker().getTrackingId());
	}

}
