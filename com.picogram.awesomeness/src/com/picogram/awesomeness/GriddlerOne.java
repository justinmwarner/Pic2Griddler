
package com.picogram.awesomeness;

import android.content.Context;
import android.graphics.Color;

import com.stackmob.sdk.model.StackMobModel;

public class GriddlerOne extends StackMobModel implements Comparable {
	private String status, name, diff, rate, author, width, height, solution,
	current, numberOfColors, colors, personalRank, isUploaded;
	private int numberOfRatings;
	private long highscore = 0;

	public GriddlerOne() {
		super(GriddlerOne.class);
	}

	public GriddlerOne(final String id, final String status, final String name,
			final String difficulty, final String rank,
			final int numberOfRatings, final String author, final String width,
			final String height, final String solution, final String current,
			final int numColors, final String colors, final String isUploaded,
			final String personalRank) {
		super(GriddlerOne.class);
		this.id = id;
		this.status = status;
		this.name = name;
		this.diff = difficulty;
		this.rate = rank;
		this.author = author;
		this.width = width;
		this.height = height;
		this.current = current;
		this.solution = solution;
		this.colors = colors;
		this.numberOfColors = numColors + "";
		this.numberOfRatings = numberOfRatings;
		this.personalRank = personalRank;
		this.isUploaded = isUploaded;
	}

	public GriddlerOne(final String[] arr) {
		super(GriddlerOne.class);
		final String id = arr[0];
		final String author = arr[1];
		final String name = arr[2];
		final String rate = arr[3];
		final String solution = arr[4];
		String current = arr[5];
		final String diff = arr[6];
		final String width = arr[7];
		final String height = arr[8];
		final String status = arr[9];
		final String personalRank = arr[12];
		final String isUploaded = arr[13];
		int numColors = 0;
		final int nor = 0;
		String colors = null;
		if ((arr[10] != null) && (arr[11] != null)) {
			numColors = Integer.parseInt(arr[10]);
			colors = arr[11];
		}
		if (current == null) {
			current = "";
			for (int i = 0; i != solution.length(); ++i) {
				current += "0";
			}
		}
		this.id = id;
		this.status = status;
		this.name = name;
		this.diff = diff;
		this.rate = rate;
		this.numberOfRatings = nor;
		this.author = author;
		this.width = width;
		this.height = height;
		this.solution = solution;
		this.current = current;
		this.numberOfColors = numColors + "";
		this.colors = colors;
		this.isUploaded = isUploaded;
		this.personalRank = personalRank;
	}

	public int compareTo(final Object g) {
		// equal is equivlant to making it less than, so no 0 needed.
		return (Integer.parseInt(this.rate) >= Integer
				.parseInt(((GriddlerOne) g).rate)) == true ? 1 : -1;
	}

	public String getAuthor() {
		return this.author;
	}

	public String getColors() {
		return this.colors;
	}

	public String getCurrent() {
		return this.current;
	}

	public String getDiff() {
		return this.diff;
	}

	public String getHeight() {
		return this.height;
	}

	public long getHighscore() {
		return this.highscore;
	}

	public String getIsUploaded() {
		return this.isUploaded;
	}

	/*
	 * public String getId() { return this.id; }
	 */
	public String getName() {
		return this.name;
	}

	public String getNumberOfColors() {
		return this.numberOfColors;
	}

	public int getNumberOfRatings() {
		return this.numberOfRatings;
	}

	public String getPersonalRank() {
		return this.personalRank;
	}

	public String getRating() {
		return this.rate;
	}

	public String getSolution() {
		return this.solution;
	}

	public String getStatus() {
		return this.status;
	}

	public String getWidth() {
		return this.width;
	}

	public GriddlerOne nullsToValue(final Context a) {
		if (this.id == null) {
			if (this.solution != null) {
				this.id = this.solution.hashCode() + "";
			} else {
				this.id = "0";
			}
		}
		if (this.status == null) {
			this.status = "0";
		}
		if (this.name == null) {
			this.name = "N/A";
		}
		if (this.diff == null) {
			this.diff = "Easy";
		}
		if (this.rate == null) {
			this.rate = "0";
		}
		if (this.author == null) {
			this.author = "N/A";
		}
		if (this.width == null) {
			this.width = "0";
		}
		if (this.height == null) {
			this.height = "0";
		}
		if (this.solution == null) {
			this.solution = "";
		}
		if (this.current == null) {
			this.current = "";
			for (int i = 0; i != (Integer.parseInt(this.width)
					* Integer.parseInt(this.height)); ++i) {
				this.current += "0";
			}
		}
		if (this.numberOfColors == null) {
			this.numberOfColors = "2";
		}
		if (this.colors == null) {
			this.colors = Color.TRANSPARENT + "," + Color.BLACK;
		}
		if (this.numberOfRatings == 0) {
			this.numberOfRatings = 0;
		}
		// Now update the rating database, if it already exists, this will
		// return nothing.
		final SQLiteRatingAdapter sra = new SQLiteRatingAdapter(a, "Rating", null, 2);
		sra.insertOnOpenOnlineGame(this.getID());
		sra.close();
		return this;
	}

	@Override
	public void save() {
		// Server doesn't care about progress or rank.
		this.current = null;
		this.personalRank = null;
		super.save();
	}

	public void setAuthor(final String author) {
		this.author = author;
	}

	public void setColors(final String colors) {
		this.colors = colors;
	}

	public void setCurrent(final String current) {
		this.current = current;
	}

	public void setDiff(final String diff) {
		this.diff = diff;
	}

	public void setHeight(final String height) {
		this.height = height;
	}

	public void setHighscore(final long highscore) {
		this.highscore = highscore;
	}

	public void setIsUploaded(final String isUploaded) {
		this.isUploaded = isUploaded;
	}

	/*
	 * public void setId(final String id) { this.id = id; }
	 */
	public void setName(final String name) {
		this.name = name;
	}

	public void setNumberOfColors(final String numberOfColors) {
		this.numberOfColors = numberOfColors;
	}

	public void setNumberOfRatings(final int numberOfRatings) {
		this.numberOfRatings = numberOfRatings;
	}

	public void setPersonalRank(final String personalRank) {
		this.personalRank = personalRank;
	}

	public void setRating(final String rank) {
		this.rate = rank;
	}

	public void setSolution(final String solution) {
		this.solution = solution;
	}

	public void setStatus(final String status) {
		this.status = status;
	}

	public void setWidth(final String width) {
		this.width = width;
	}

	@Override
	public String toString() {
		return this.id + " " + this.status + " " + this.name + " " + this.diff
				+ " " + this.rate + " " + this.author + " " + this.width + " "
				+ this.height + " " + this.solution + " " + this.current + " "
				+ this.numberOfColors + " " + this.colors + " "
				+ this.numberOfRatings;
		/*
		 * id status name diff rating - null author width height solution
		 * current - null numColors colors numberOfRatings - 5
		 */
	}

}
