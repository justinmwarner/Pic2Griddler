
package com.picogram.awesomeness;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class PicogramCommentAdapter extends ArrayAdapter<PicogramComment> {
	private static final String TAG = "CommentAdapter";
	private Context context;
	ArrayList<PicogramComment> comments = new ArrayList<PicogramComment>();

	public PicogramCommentAdapter(final Context context, final int resource) {
		super(context, resource);
		this.context = context;
	}

	public PicogramCommentAdapter(final Context context, final int resource,
			final ArrayList<PicogramComment> objects) {
		super(context, resource, objects);
		this.context = context;
		this.comments = objects;
	}

	@Override
	public void add(final PicogramComment object) {
		super.add(object);
		this.comments.add(object);
		this.notifyDataSetChanged();
	}

	@Override
	public void clear() {
		super.clear();
		this.comments.clear();
	}

	public void delete(final String author, final String comment)
	{
		for (int i = 0; i != this.comments.size(); ++i)
		{
			final PicogramComment pc = this.comments.get(i);
			if (pc.getAuthor().equals(author) && pc.getComment().equals(comment))
			{
				this.comments.remove(i);
				this.notifyDataSetChanged();
				return;
			}
		}
	}

	@Override
	public int getCount() {
		return this.comments.size();
	}

	@Override
	public PicogramComment getItem(final int position) {
		return this.comments.get(position);
	}

	@Override
	public View getView(final int position, final View convertView,
			final ViewGroup parent) {
		if (this.context == null) {
			this.context = this.getContext();
		}
		final PicogramComment comment = this.comments.get(position);

		Log.d(TAG, "Creating list item: " + position + " " + comment.getComment());
		final LayoutInflater inflater = (LayoutInflater) this.context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		final View item = inflater.inflate(R.layout.comment_menu_choice_item,
				parent, false);
		final TextView tvAuthor = (TextView) item.findViewById(R.id.tvCommentAuthor);
		final TextView tvPicogramComment = (TextView) item.findViewById(R.id.tvComment);
		tvAuthor.setText(comment.getAuthor());
		tvAuthor.setTextColor(Color.LTGRAY);
		tvPicogramComment.setText(comment.getComment());
		Log.d(TAG, "A: " + comment.getAuthor() + " " + Util.id(this.context));
		if (Util.id(this.context).equals(comment.getAuthor()))
		{
			// This is the person, so change background.
			item.setBackgroundColor(context.getResources().getColor(R.color.light_yellow));
		}
		item.invalidate();
		return item;
	}

}
