package com.picogram.awesomeness;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.AdapterView.OnItemLongClickListener;

public class GriddlerListAdapter extends ArrayAdapter<GriddlerOne> {
	private static final String TAG = "GriddlerListAdapter";
	private Context context;
	ArrayList<GriddlerOne> griddlers = new ArrayList<GriddlerOne>();

	public GriddlerListAdapter(final Context context,
			final int textViewResourceId) {
		super(context, textViewResourceId);
	}

	public GriddlerListAdapter(final Context context, final int resource,
			final ArrayList<GriddlerOne> items) {
		super(context, resource, items);
		this.context = context;
		this.griddlers = items;
	}

	@Override
	public void add(final GriddlerOne object) {
		super.add(object);
		this.griddlers.add(object);
		// this.notifyDataSetChanged();
		// this.notifyDataSetInvalidated();
	}

	@Override
	public void clear() {
		super.clear();
		this.griddlers.clear();
	}

	public boolean existsById(final String id) {
		for (final GriddlerOne g : this.griddlers) {
			if (g.getID().equals(id)) {
				return true;
			}
		}

		return false;
	}

	public GriddlerOne get(final int pos) {
		return this.griddlers.get(pos);
	}

	@Override
	public int getCount() {
		return this.griddlers.size();
	}

	@Override
	public View getView(final int position, final View convertView,
			final ViewGroup parent) {
		// This is expensive!!
		if (this.context == null) {
			this.context = this.getContext();
		}
		final LayoutInflater inflater = (LayoutInflater) this.context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		final View item = inflater.inflate(R.layout.griddler_menu_choice_item,
				parent, false);
		final TextView rate = (TextView) item.findViewById(R.id.tvRating);
		final TextView diff = (TextView) item.findViewById(R.id.tvDiff);
		final TextView name = (TextView) item.findViewById(R.id.tvName);
		final ImageView iv = (ImageView) item.findViewById(R.id.ivCurrent);
		iv.setVisibility(0);
		final int height = Integer.parseInt(this.griddlers.get(position)
				.getHeight());
		final int width = Integer.parseInt(this.griddlers.get(position)
				.getWidth());
		String curr = this.griddlers.get(position).getCurrent();
		int run = 0;
		if (curr == null) {
			curr = "";
			for (int i = 0; i != this.griddlers.get(position).getSolution()
					.length(); ++i) {
				curr += "0";
			}
		}
		Bitmap bm;
		if ((height > 0) && (width > 0)) {
			bm = BitmapFactory.decodeResource(this.context.getResources(),
					R.drawable.ic_launcher);
			bm = Bitmap.createScaledBitmap(bm, width, height, true);
			String[] colors = this.griddlers.get(position).getColors()
					.split(",");
			for (int i = 0; i < height; i++) {
				for (int j = 0; j < width; j++) {
					if (curr.charAt(run) == 'x') {
						bm.setPixel(j, i, Color.WHITE);
					} else {
						bm.setPixel(
								j,
								i,
								Integer.parseInt(colors[Integer.parseInt(""
										+ curr.charAt(run))]));
					}
					run++;
				}
			}
		} else {
			if (position == 0)
				bm = BitmapFactory.decodeResource(this.context.getResources(),
						R.drawable.create);
			else if (position == 1)
				bm = BitmapFactory.decodeResource(this.context.getResources(),
						R.drawable.random);
			else
				// Just show the icon if something is wrong =/.
				bm = BitmapFactory.decodeResource(this.context.getResources(),
						R.drawable.ic_launcher);
			bm = Bitmap.createScaledBitmap(bm, 100, 100, true);
		}
		bm = Bitmap.createScaledBitmap(bm, 100, 100, false);
		iv.setImageBitmap(bm);
		iv.setVisibility(View.VISIBLE);
		if (this.griddlers.get(position).getNumberOfRatings() != 0) {
			String rateText = "Rating: "
					+ (Double.parseDouble(this.griddlers.get(position)
							.getRating()));
			int index = rateText.indexOf('.');
			if (index != -1)
				if (rateText.length() > index + 3)
					rate.setText(rateText.substring(0,
							rateText.indexOf('.') + 3));
				else
					rate.setText(rateText.substring(0,
							rateText.indexOf('.') + 2));

			else
				rate.setText(rateText);
		}
		diff.setText("Difficulty: " + this.griddlers.get(position).getDiff());
		try {
			int i = Integer.parseInt(this.griddlers.get(position).getDiff());
			if (i == 0) {
				diff.setText("Difficulty: Easy");
			} else if (i == 1)
				diff.setText("Difficulty: Normal");
			else if (i == 2)
				diff.setText("Difficulty: Hard");
			else if (i == 3)
				diff.setText("Difficulty: eXtreme!");
			else
				diff.setText("");
		} catch (Exception e) {
			// Ignore if not a number.
			Log.d(TAG, "Poop");
		}
		name.setText(this.griddlers.get(position).getName());
		// Change color if user has beaten level.
		int status = 0;
		try {
			status = Integer.parseInt(this.griddlers.get(position).getStatus());
		} catch (final NumberFormatException e) {
		}
		final RelativeLayout rl = (RelativeLayout) item
				.findViewById(R.id.rlMenuHolder);
		final Drawable gd = rl.getBackground().mutate();
		item.setBackgroundResource(R.drawable.griddler_menu_choice_border_red);
		if (status == 0) {
			// In progress.
			item.setBackgroundResource(R.drawable.griddler_menu_choice_border_red);
		} else if (status == 1) {
			// Won.
			item.setBackgroundResource(R.drawable.griddler_menu_choice_border_green);
		} else {
			// Other (Custom, special levels, etc.).
			item.setBackgroundResource(R.drawable.griddler_menu_choice_border_other);
		}
		gd.invalidateSelf();
		item.invalidate();
		rl.invalidate();
		((ViewGroup) item)
				.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);

		return item;
	}

	public void setGriddlers(final ArrayList<GriddlerOne> g) {
		this.griddlers = g;
	}

	public void removeById(String id) {
		for (int i = 0; i != griddlers.size(); ++i) {
			if (griddlers.get(i).getID().equals(id)) {
				Log.d(TAG, "REMOVE SUCCSS");
				griddlers.remove(i);
				return;
			}
		}
	}

	public void updateCurrentById(String id, String newCurrent, String status) {
		for (int i = 0; i != griddlers.size(); ++i) {
			if (griddlers.get(i).getID().equals(id)) {
				Log.d(TAG, "REMOVE SUCCSS");
				GriddlerOne go = griddlers.get(i);
				go.setCurrent(newCurrent);
				go.setStatus(status);
				griddlers.set(i, go);
				return;
			}
		}
	}
}
