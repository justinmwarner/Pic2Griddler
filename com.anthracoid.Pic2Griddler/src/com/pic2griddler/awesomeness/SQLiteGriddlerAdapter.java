package com.pic2griddler.awesomeness;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class SQLiteGriddlerAdapter extends SQLiteOpenHelper
{

	static final String dbName = "Griddlers";
	static final String griddlerTable = "UserGriddlers";
	static final String id = "id";
	static final String author = "Author";
	static final String name = "Name";
	static final String rank = "Rank";
	static final String solution = "Solution";
	static final String current = "Current";
	static final String difficulty = "Difficulty";
	static final String width = "Width";
	static final String height = "Height";
	static final String status = "Status";

	// static final String tags = "Tags";

	public SQLiteGriddlerAdapter(Context context, String name, CursorFactory factory, int version)
	{
		super(context, name, factory, version);
	}

	@Override
	public void onCreate(SQLiteDatabase db)
	{
		// Create the database son.
		String query = "CREATE TABLE " + griddlerTable + " (" + id + " INT(32)," + author + " TEXT," + name + " TEXT," + rank + " INT(32)," + solution + " TEXT," + current + " TEXT," + difficulty
				+ " VARCHAR(16)," + width + " INT(12)," + height + " INT(12)," + status + " INT(12)," + "primary KEY (id));";
		db.execSQL(query);
		insertDefaults(db);
		Log.d("TAG", "Create");

	}

	private void insertDefaults(SQLiteDatabase db)
	{
		Log.d("TAG", "Defaults");
		// Create Custom and Tutorial blocks. Will ALWAYS be there.
		ContentValues cv = new ContentValues();
		cv.put(id, "".hashCode()); // Odd, to me, but nothing will ever have
									// this ;).
		cv.put(author, "justinwarner");
		cv.put(name, "Create a Griddler");
		cv.put(rank, 0);
		cv.put(solution, "0");
		cv.put(current, "0");
		cv.put(difficulty, "0");
		cv.put(width, "0");
		cv.put(height, "0");
		cv.put(status, "2");
		db.insert(griddlerTable, null, cv); // Custom Griddler.
		cv.put(id, "Tutorial".hashCode()); // Now set up the tutorial.
		cv.put(author, "justinwarner");
		cv.put(name, "Tutorial");
		cv.put(rank, 0);
		cv.put(solution, "1111100110011111");
		cv.put(current, "1000100010001000");
		cv.put(difficulty, "0");
		cv.put(width, "4");
		cv.put(height, "4");
		cv.put(status, "0");
		db.insert(griddlerTable, null, cv); // Tutorial Griddler.
	}

	public long addUserGriddler(String info)
	{
		// Do stuff. Unknown so far. Implement later.
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues cv = new ContentValues();

		return db.insert(griddlerTable, null, cv);
	}

	public int updateCurrentGriddler(String info)
	{
		// Info should include hash and new current.
		SQLiteDatabase db = this.getWritableDatabase();
		String[] hash =
		{ info.split(" ")[0] };
		String newCurrent = info.split(" ")[1];
		ContentValues cv = new ContentValues();
		cv.put(current, newCurrent);
		return db.update(griddlerTable, cv, "id=?", hash);
	}

	public int deleteGriddler(String info)
	{
		// Probably won't implement. Not a huge deal (Right now).
		SQLiteDatabase db = this.getWritableDatabase();
		String[] hash =
		{ info.split(" ")[0] };
		return db.delete(griddlerTable, "id=?", hash);
	}

	public String[] getGriddlers(int page)
	{
		// Page is the page of Griddlers to get. Might change.
		// Returns String array of Griddler infos to be processed internally.
		// Maybe change this so it's easier to process?
		int numItemsPerPage = 6; // This should be passed, will implement later
									// on.
		SQLiteDatabase db = this.getWritableDatabase();
		String query = "";
		query = "SELECT * FROM " + griddlerTable + " LIMIT " + (page * numItemsPerPage);
		Cursor c = db.rawQuery(query, null);
		Log.d("TAG", "Getting: " + query);
		if (c.moveToFirst())
		{
			Log.d("TAG", "Something exists!");
			String[] result = new String[c.getCount()];
			for (int i = 0; i < result.length; i++)
			{
				Log.d("TAG", "Length: " + result.length);
				String info = "";
				for (int j = 0; j < c.getColumnCount(); j++)
				{
					Log.d("TAG", "Adding");
					info += c.getString(j) + " ";
				}
				result[i] = info;
				c.moveToNext();
			}
			c.close();
			return result;
		}
		else
		{
			Log.d("TAG", "Nithin!");
			c.close();
			return new String[]
			{}; // Should never happen because tutorial and custom will be
				// there.
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldV, int newV)
	{
		// Don't do anything... Yet. Need to read up on what/how this works.
	}

}
