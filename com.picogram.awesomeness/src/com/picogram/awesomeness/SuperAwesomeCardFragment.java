package com.picogram.awesomeness;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ListView;

import com.flurry.android.FlurryAgent;
import com.picogram.awesomeness.DialogMaker.OnDialogResultListener;
import com.stackmob.sdk.api.StackMobQuery;
import com.stackmob.sdk.callback.StackMobCallback;
import com.stackmob.sdk.callback.StackMobModelCallback;
import com.stackmob.sdk.callback.StackMobQueryCallback;
import com.stackmob.sdk.exception.StackMobException;
import com.stackmob.sdk.model.StackMobModel;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

public class SuperAwesomeCardFragment extends Fragment implements
		OnItemClickListener, OnItemLongClickListener {

	private static final String ARG_POSITION = "position";
	private static final String TAG = "SuperAwesomeCardFragment";
	public static final int CREATE_RESULT = 100;
	public static final int GAME_RESULT = 1337;

	public static SuperAwesomeCardFragment newInstance(final int position) {
		final SuperAwesomeCardFragment f = new SuperAwesomeCardFragment();
		final Bundle b = new Bundle();
		b.putInt(ARG_POSITION, position);
		f.setArguments(b);
		return f;
	}

	private int position;

	PicogramListAdapter myAdapter;
	Handler h = new Handler();
	SQLitePicogramAdapter sql = null;

	public void clearAdapter() {
		this.myAdapter.clear();
		this.myAdapter.notifyDataSetChanged();
	}

	public void getMyPuzzles(final FragmentActivity a) {
		if (this.sql == null) {
			this.sql = new SQLitePicogramAdapter(a, "Picograms", null, 1);
		}
		final String[][] PicogramsArray = this.sql.getPicograms();
		final SharedPreferences prefs = Util.getPreferences(a);
		for (int i = 0; i < PicogramsArray.length; i++) {
			final String temp[] = PicogramsArray[i];
			final String id = temp[0];
			final String name = temp[2];
			final String rate = temp[3];
			final String width = temp[7];
			final String height = temp[8];
			final String current = temp[5];
			final String solution = temp[4];
			final String diff = temp[6];
			final String author = temp[1];
			final String status = temp[9];
			final String personalRank = temp[12];
			final String isUploaded = temp[13];
			int numColors = 0;
			String colors = null;
			if ((temp[10] != null) && (temp[11] != null)) {
				numColors = Integer.parseInt(temp[10]);
				colors = temp[11];
			}
			boolean isAdd = true;

			if (prefs != null) {
				if (prefs.getBoolean("wonvisible", false)) {
					if (status.equals("1")) {
						isAdd = false;
					}
				}
			}
			if (isAdd) {
				final PicogramOne tempPicogram = new PicogramOne(id, status,
						name, diff, rate, 0, author, width, height, solution,
						current, numColors, colors, isUploaded, personalRank);
				if (status.equals("2") || !Util.isOnline()) {
					a.runOnUiThread(new Runnable() {

						public void run() {
							SuperAwesomeCardFragment.this.myAdapter
									.add(tempPicogram);
							SuperAwesomeCardFragment.this.myAdapter
									.notifyDataSetChanged();
						}

					});

				} else {
					// Get data from online about the Picogram for its updated
					// rating.
					// These variables should be removed.
					final String cols = colors;
					final int nc = numColors;
					final String oldStatus = status; // Don't get rid of this.
					final String oldCurrent = current;
					final PicogramOne g = new PicogramOne();
					g.setID(id);
					// Add the Puzzle, then update in the adapter later on.
					myAdapter.add(tempPicogram);

					g.fetch(new StackMobModelCallback() {
						@Override
						public void failure(final StackMobException arg0) {
							// Don't do anything, we already added it.
						}

						@Override
						public void success() {
							if (!SuperAwesomeCardFragment.this.myAdapter
									.existsById(g.getID())) {
								// TODO Update the ranking.
								a.runOnUiThread(new Runnable() {
									public void run() {
										// TODO Test this, should update rating.
										for (int i = 0; i != myAdapter
												.getCount(); ++i) {

											if (myAdapter.get(i).getID() == g
													.getID()) {
												myAdapter.remove(tempPicogram);
												g.setStatus(oldStatus);
												g.setCurrent(oldCurrent);
												myAdapter.add(g);
												return;
											}
										}
										SuperAwesomeCardFragment.this.myAdapter
												.notifyDataSetChanged();
									}
								});
							}
						}
					});
				}
			}
		}

	}

	public void getRecentPuzzles(final Activity a) {
		StackMobModel.query(
				PicogramOne.class,
				new StackMobQuery().isInRange(0, 9).fieldIsOrderedBy(
						"createddate", StackMobQuery.Ordering.DESCENDING),
				new StackMobQueryCallback<PicogramOne>() {

					@Override
					public void failure(final StackMobException arg0) {
						Crouton.makeText(a,
								"Error fetching data: " + arg0.toString(),
								Style.ALERT);

					}

					@Override
					public void success(final List<PicogramOne> gs) {
						for (final PicogramOne g : gs) {
							SuperAwesomeCardFragment.this.myAdapter.add(g);
							SuperAwesomeCardFragment.this.myAdapter
									.notifyDataSetChanged();
						}
					}
				});
	}

	public void getSortedPuzzles(final Activity a, final String sort) {
		this.myAdapter.clear();
		StackMobModel.query(
				PicogramOne.class,
				new StackMobQuery().isInRange(0, 9).fieldIsOrderedBy(sort,
						StackMobQuery.Ordering.DESCENDING),
				new StackMobQueryCallback<PicogramOne>() {

					@Override
					public void failure(final StackMobException arg0) {
						Crouton.makeText(a,
								"Error fetching data: " + arg0.toString(),
								Style.ALERT);
						SuperAwesomeCardFragment.this.myAdapter
								.notifyDataSetChanged();
					}

					@Override
					public void success(final List<PicogramOne> gs) {
						for (final PicogramOne g : gs) {
							a.runOnUiThread(new Runnable() {

								public void run() {
									SuperAwesomeCardFragment.this.myAdapter
											.add(g);
									SuperAwesomeCardFragment.this.myAdapter
											.notifyDataSetChanged();
								}

							});
						}
					}
				});
	}

	public void getTagPuzzles(final Activity a, final String tag,
			final boolean isSortByRate) {
		this.myAdapter.clear();
		final StackMobQuery smq = new StackMobQuery().fieldIsEqualTo("tag",
				tag.toLowerCase());
		StackMobModel.query(PicogramTag.class, smq,
				new StackMobQueryCallback<PicogramTag>() {

					@Override
					public void failure(final StackMobException arg0) {
						Crouton.makeText(a,
								"Error fetching data: " + arg0.toString(),
								Style.ALERT);

					}

					@Override
					public void success(final List<PicogramTag> gts) {
						final ArrayList<String> ids = new ArrayList();
						for (final PicogramTag gt : gts) {
							ids.add(gt.getID());
						}

						final StackMobQuery smqInner = new StackMobQuery()
								.isInRange(0, 9).fieldIsIn("Picogramone_id",
										ids);

						if (isSortByRate) {
							smq.fieldIsOrderedBy("rate",
									StackMobQuery.Ordering.DESCENDING);
						} else {
							smq.fieldIsOrderedBy("createddate",
									StackMobQuery.Ordering.DESCENDING);
						}
						StackMobModel.query(PicogramOne.class, smqInner,
								new StackMobQueryCallback<PicogramOne>() {

									@Override
									public void failure(
											final StackMobException arg0) {

										Crouton.makeText(
												a,
												"Error fetching data: "
														+ arg0.toString(),
												Style.ALERT);
									}

									@Override
									public void success(
											final List<PicogramOne> gs) {

										for (final PicogramOne g : gs) {
											a.runOnUiThread(new Runnable() {

												public void run() {
													SuperAwesomeCardFragment.this.myAdapter
															.add(g);
													SuperAwesomeCardFragment.this.myAdapter
															.notifyDataSetChanged();
												}
											});
										}
									}
								});

					}
				});
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.position = this.getArguments().getInt(ARG_POSITION);
	}

	@Override
	public View onCreateView(final LayoutInflater inflater,
			final ViewGroup container, final Bundle savedInstanceState) {
		final LayoutParams params = new LayoutParams(
				android.view.ViewGroup.LayoutParams.MATCH_PARENT,
				android.view.ViewGroup.LayoutParams.MATCH_PARENT);

		final FrameLayout fl = new FrameLayout(this.getActivity());
		fl.setLayoutParams(params);

		final int margin = (int) TypedValue.applyDimension(
				TypedValue.COMPLEX_UNIT_DIP, 8, this.getResources()
						.getDisplayMetrics());

		final ListView v = new ListView(this.getActivity());
		params.setMargins(margin, margin, margin, margin);
		v.setLayoutParams(params);
		v.setLayoutParams(params);
		v.setBackgroundResource(R.drawable.background_card);
		// final List<String> items = new ArrayList();
		this.myAdapter = new PicogramListAdapter(this.getActivity(),
				R.id.tvName);
		if (this.position == MenuActivity.TITLES.indexOf("My")) {
			this.getMyPuzzles(this.getActivity());
		} else if (this.position == MenuActivity.TITLES.indexOf("Top")) {
			this.getSortedPuzzles(this.getActivity(), "rate");
		} else if (this.position == MenuActivity.TITLES.indexOf("Recent")) {
			this.getSortedPuzzles(this.getActivity(), "createddate");
		} else if (this.position == MenuActivity.TITLES.indexOf("Search")) {
			// Don't load anything on start.
			// this.getTagPuzzles(this.getActivity(), "", true);
		} else if (this.position == MenuActivity.TITLES.indexOf("Prefs")) {
			return new View(this.getActivity());
		} else {
			for (int i = 0; i != 20; ++i) {
				final PicogramOne obj = new PicogramOne("=/", "0",
						"We had an error. You shouldn't see this " + i, "0",
						"0", 1, "Justin", "1", "1", "1", "0", 2, Color.BLACK
								+ " " + Color.RED, "1", "0");
				obj.setID(i + "" + this.position);
				this.myAdapter.add(obj);
				// items.add(MenuActivity.TITLES.get(this.position) + " " +
				// this.position + " " + i);
			}
		}
		v.setAdapter(this.myAdapter);
		v.setOnItemClickListener(this);
		v.setLongClickable(true);
		v.setOnItemLongClickListener(this);

		fl.addView(v);
		return fl;
	}

	@Override
	public void onDestroy() {
		if (this.sql != null) {
			this.sql.close();
		}
		super.onDestroy();
	}

	public void onItemClick(final AdapterView<?> parent, final View v,
			final int pos, final long id) {
		if (pos >= 0) // If valid position to select.
		{
			if ((this.position == MenuActivity.TITLES.indexOf("My"))
					&& ((pos == 0) || pos == 1)) {
				// Can this be the Creating or Random?
				this.sql.close();
				if (pos == 0) {
					final Intent createIntent = new Intent(this.getActivity(),
							CreatePicogramActivity.class);
					this.getActivity().startActivityForResult(createIntent,
							MenuActivity.CREATE_CODE);
				} else if (pos == 1) {
					generateRandomGame();
				}
			} else {
				// If this Picogram doesn't exists for the person, add it.

				PicogramOne Picogram = this.myAdapter.get(pos);
				if (sql == null)
					this.sql = new SQLitePicogramAdapter(this.getActivity(),
							"Picograms", null, 1);
				if (!sql.doesPuzzleExist(Picogram)) {
					sql.addUserPicogram(Picogram);
				}
				this.startGame(Picogram);
			}
		}
	}

	private void startGame(PicogramOne go) {
		FlurryAgent.logEvent("UserPlayGame");
		// Intent gameIntent = new Intent(this, AdvancedGameActivity.class);
		final Intent gameIntent = new Intent(this.getActivity(),
				AdvancedGameActivity.class);
		gameIntent.putExtra("solution", go.getSolution());
		gameIntent.putExtra("current", go.getCurrent());
		gameIntent.putExtra("width", go.getWidth());
		gameIntent.putExtra("height", go.getHeight());
		gameIntent.putExtra("id", go.getID());
		gameIntent.putExtra("name", go.getName());
		gameIntent.putExtra("colors", go.getColors());
		this.getActivity().startActivityForResult(gameIntent,
				MenuActivity.GAME_CODE);
	}

	public void generateRandomGame() {
		FragmentTransaction ft = getChildFragmentManager().beginTransaction();
		// Create and show the dialog.
		Bundle bundle = new Bundle();
		bundle.putInt("layoutId", R.layout.dialog_random_Picogram);
		DialogMaker newFragment = new DialogMaker();
		newFragment.setArguments(bundle);
		newFragment.show(ft, "dialog");
		newFragment.setOnDialogResultListner(new OnDialogResultListener() {

			public void onDialogResult(Bundle result) {
				// Add to personal.
				String current = "", cols = "";
				for (int i = 0; i != result.getString("solution").length(); ++i)
					current += "0";
				for (int i = 0; i != result.getInt("numColors"); ++i)
					if (i == 0)
						cols += Color.TRANSPARENT + ",";
					else
						cols += Color.rgb((int) (Math.random() * 255),
								(int) (Math.random() * 255),
								(int) (Math.random() * 255))
								+ ",";
				cols = cols.substring(0, cols.length() - 1);
				int w = result.getInt("width"), h = result.getInt("height"), nc = result
						.getInt("numColors");
				final PicogramOne go = new PicogramOne(result.getString(
						"solution").hashCode()
						+ "", "0", result.getString("name"),
						((int) ((w * h) / 1400)) + "", "3", 1, "computer", w
								+ "", h + "", result.getString("solution"),
						current, nc, cols, "0", "5");
				// TODO make sure go gets saved.
				myAdapter.add(go);
				myAdapter.notifyDataSetChanged();
				go.setCurrent(null);
				go.save(new StackMobCallback() {

					@Override
					public void failure(StackMobException arg0) {
						sql.addUserPicogram(go);
					}

					@Override
					public void success(String arg0) {
						go.setIsUploaded("1");
						sql.addUserPicogram(go);
					}
				});
				PicogramTag gt = new PicogramTag("random");
				gt.setID(go.getID());
				gt.save();
				// Start to play.
			}
		});
	}

	public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
			final int position, long arg3) {
		if (position == 0 || position == 1) {
			// Create or Random, should just ignore this?
			return false;
		} else {
			FragmentTransaction ft = getChildFragmentManager()
					.beginTransaction();
			// Create and show the dialog.
			Bundle bundle = new Bundle();
			bundle.putInt("layoutId", R.layout.dialog_listview_contextmenu);
			DialogMaker newFragment = new DialogMaker();
			newFragment.setArguments(bundle);
			newFragment.show(ft, "dialog");
			newFragment.setOnDialogResultListner(new OnDialogResultListener() {

				public void onDialogResult(Bundle res) {
					if (res == null) {
						return;
					}
					int result = res.getInt("resultInt");
					if (result == 0) {
						// Nothing
						// TODO
					} else if (result == 1) {
						// Clear.
						String newCurrent = "";
						for (int i = 0; i != myAdapter.get(position)
								.getCurrent().length(); ++i) {
							newCurrent += "0";
						}
						myAdapter.updateCurrentById(myAdapter.get(position)
								.getID(), newCurrent, "0");
						sql.updateCurrentPicogram(myAdapter.get(position)
								.getID(), "0", newCurrent);
					} else if (result == 2) {
						// Delete.
						sql.deletePicogram(myAdapter.get(position).getID());
						myAdapter.removeById(myAdapter.get(position).getID());
					}
					myAdapter.notifyDataSetChanged();
				}
			});

		}
		return true;
	}
}