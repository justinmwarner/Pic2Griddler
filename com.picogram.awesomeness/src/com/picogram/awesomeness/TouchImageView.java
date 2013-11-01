
package com.picogram.awesomeness;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageView;

import java.util.ArrayList;

public class TouchImageView extends ImageView {

	private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
		@Override
		public boolean onScale(final ScaleGestureDetector detector) {
			float mScaleFactor = detector.getScaleFactor();
			final float origScale = TouchImageView.this.saveScale;
			TouchImageView.this.saveScale *= mScaleFactor;
			if (TouchImageView.this.saveScale > TouchImageView.this.maxScale) {
				TouchImageView.this.saveScale = TouchImageView.this.maxScale;
				mScaleFactor = TouchImageView.this.maxScale / origScale;
			} else if (TouchImageView.this.saveScale < TouchImageView.this.minScale) {
				TouchImageView.this.saveScale = TouchImageView.this.minScale;
				mScaleFactor = TouchImageView.this.minScale / origScale;
			}

			if (((TouchImageView.this.origWidth * TouchImageView.this.saveScale) <= TouchImageView.this.viewWidth)
					|| ((TouchImageView.this.origHeight * TouchImageView.this.saveScale) <= TouchImageView.this.viewHeight)) {
				TouchImageView.this.matrix.postScale(mScaleFactor, mScaleFactor,
						TouchImageView.this.viewWidth / 2, TouchImageView.this.viewHeight / 2);
			} else {
				TouchImageView.this.matrix.postScale(mScaleFactor, mScaleFactor,
						detector.getFocusX(), detector.getFocusY());
			}
			TouchImageView.this.fixTrans();
			return true;
		}

		@Override
		public boolean onScaleBegin(final ScaleGestureDetector detector) {
			TouchImageView.this.mode = ZOOM;
			return true;
		}
	}

	public interface WinnerListener {
		public void win();
	}

	Matrix matrix;
	// We can be in one of these 3 states
	static final int NONE = 0;
	static final int DRAG = 1;
	static final int ZOOM = 2;
	int mode = NONE;
	// Remember some things for zooming
	PointF last = new PointF();
	PointF start = new PointF();
	float minScale = 0.9f;

	float maxScale = 3f;
	float[] m;

	int viewWidth, viewHeight;
	static final int CLICK = 3;
	protected static final String TAG = "TouchImageView";
	float saveScale = 1f;

	protected float origWidth, origHeight;

	int oldMeasuredWidth, oldMeasuredHeight;

	ScaleGestureDetector mScaleDetector;

	Context context;
	// Control whether we're moving around or in actual gameplay mode.
	boolean isGameplay = false;
	Handler h = new Handler();
	int lastTouchX = 0;
	int lastTouchY = 0;
	char colorCharacter = '0';

	// These take a long time to calculate and don't change. Only do it once.
	ArrayList<String[]> topHints;
	ArrayList<String> sideHints;
	ArrayList<String> columns;
	ArrayList<String> rows;
	int longestSide, longestTop;
	Bitmap bm;
	Canvas canvasBitmap;
	Paint paintBitmap;

	// Griddler specifics.
	String gCurrent, gSolution;

	int gWidth, gHeight, gId, lTop, lSide, cellWidth, cellHeight;

	int[] gColors;

	/*
	 * Interface and such to see if we win.
	 */
	private WinnerListener winListener;

	OnTouchListener touchListener = new OnTouchListener() {

		public boolean onTouch(final View v, final MotionEvent event) {
			if (!TouchImageView.this.isGameplay) {
				TouchImageView.this.mScaleDetector.onTouchEvent(event);
				final PointF curr = new PointF(event.getX(), event.getY());

				switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
						TouchImageView.this.last.set(curr);
						TouchImageView.this.start.set(TouchImageView.this.last);
						TouchImageView.this.mode = DRAG;
						break;

					case MotionEvent.ACTION_MOVE:
						if (TouchImageView.this.mode == DRAG) {
							final float deltaX = curr.x - TouchImageView.this.last.x;
							final float deltaY = curr.y - TouchImageView.this.last.y;
							final float fixTransX = TouchImageView.this.getFixDragTrans(deltaX,
									TouchImageView.this.viewWidth,
									TouchImageView.this.origWidth
											* TouchImageView.this.saveScale);
							final float fixTransY = TouchImageView.this.getFixDragTrans(deltaY,
									TouchImageView.this.viewHeight,
									TouchImageView.this.origHeight
											* TouchImageView.this.saveScale);
							TouchImageView.this.matrix.postTranslate(fixTransX, fixTransY);
							TouchImageView.this.fixTrans();
							TouchImageView.this.last.set(curr.x, curr.y);
						}
						break;

					case MotionEvent.ACTION_UP:
						TouchImageView.this.mode = NONE;
						final int xDiff = (int) Math.abs(curr.x - TouchImageView.this.start.x);
						final int yDiff = (int) Math.abs(curr.y - TouchImageView.this.start.y);
						if ((xDiff < CLICK) && (yDiff < CLICK)) {
							TouchImageView.this.performClick();
						}
						break;

					case MotionEvent.ACTION_POINTER_UP:
						TouchImageView.this.mode = NONE;
						break;
				}

				TouchImageView.this.setImageMatrix(TouchImageView.this.matrix);
				TouchImageView.this.invalidate();
			} else {
				if ((event.getAction() == MotionEvent.ACTION_MOVE)
						|| (event.getAction() == MotionEvent.ACTION_DOWN)) {
					TouchImageView.this.matrix.getValues(TouchImageView.this.m);
					final float transX = TouchImageView.this.m[Matrix.MTRANS_X] * -1;
					final float transY = TouchImageView.this.m[Matrix.MTRANS_Y] * -1;
					final float scaleX = TouchImageView.this.m[Matrix.MSCALE_X];
					final float scaleY = TouchImageView.this.m[Matrix.MSCALE_Y];
					TouchImageView.this.lastTouchX = (int) ((event.getX() + transX) / scaleX);
					TouchImageView.this.lastTouchY = (int) ((event.getY() + transY) / scaleY);
					TouchImageView.this.lastTouchX = Math.abs(TouchImageView.this.lastTouchX);
					TouchImageView.this.lastTouchY = Math.abs(TouchImageView.this.lastTouchY);
					final int indexX = (int) Math
							.floor((TouchImageView.this.lastTouchX - (TouchImageView.this.cellWidth * TouchImageView.this.lSide))
									/ TouchImageView.this.cellWidth);
					final int indexY = (int) Math
							.floor((TouchImageView.this.lastTouchY - (TouchImageView.this.cellHeight * TouchImageView.this.lTop))
									/ TouchImageView.this.cellHeight);
					if ((TouchImageView.this.lastTouchX < (TouchImageView.this.cellWidth * TouchImageView.this.lSide))
							|| (TouchImageView.this.lastTouchY < (TouchImageView.this.cellHeight * TouchImageView.this.lTop))
							|| (TouchImageView.this.lastTouchX > TouchImageView.this.getWidth())
							|| (TouchImageView.this.lastTouchY > ((TouchImageView.this.lTop + TouchImageView.this.gHeight) * TouchImageView.this.cellHeight))) {
						// If we're on the hints, just get out of there.
						// Don't do anything.
						if (TouchImageView.this.lastTouchY > ((TouchImageView.this.lTop + TouchImageView.this.gHeight) * TouchImageView.this.cellHeight)) {
							Log.d(TAG,
									"4 "
											+ TouchImageView.this.lastTouchY
											+ " "
											+ ((TouchImageView.this.lTop + TouchImageView.this.gHeight) * TouchImageView.this.cellHeight));
						}
						return true;
					}
					final char[] temp = TouchImageView.this.gCurrent.toCharArray();
					final String past = TouchImageView.this.gCurrent;
					if (((indexY * TouchImageView.this.gWidth) + indexX) < temp.length) {
						temp[(indexY * TouchImageView.this.gWidth) + indexX] = TouchImageView.this.colorCharacter;
						TouchImageView.this.gCurrent = String.valueOf(temp);
						if (!past.equals(TouchImageView.this.gCurrent)) {
							new Thread(new Runnable() {

								public void run() {
									TouchImageView.this.h.post(new Runnable() {

										public void run() {
											TouchImageView.this.bitmapFromCurrent();
										}

									});
								}

							}).start();
						}
					}
					if (TouchImageView.this.gCurrent.equals(TouchImageView.this.gSolution)) {
						Log.d(TAG, "WIN!");
						if (TouchImageView.this.winListener != null) {
							TouchImageView.this.winListener.win();
						} else {
							try {
								throw new Exception("No WinListener!");
							} catch (final Exception e) {
								// Should never get here.
								e.printStackTrace();
								return false;
							}
						}
					}
				}

			}
			return true; // indicate event was handled
		}

	};

	public TouchImageView(final Context context) {
		super(context);
		this.sharedConstructing(context);
	}

	public TouchImageView(final Context context, final AttributeSet attrs) {
		super(context, attrs);
		this.sharedConstructing(context);
	}

	// Convert current String to a bitmap that's drawable. This will draw everything: grid, numbers, and onclicks.
	private void bitmapFromCurrent() {
		// Get a 2D array of "current" griddler.
		final char current2D[][] = this.solutionTo2DArray();
		// Create bitmap based on the current. Make a int array with pixel colors.
		// Because of how we're making the top hints, it needs its own method.
		if (this.topHints == null) {
			this.rows = this.getRows(current2D);
			this.columns = this.getColumns(current2D);
			this.sideHints = this.getSideHints(this.rows);
			this.topHints = this.getTopHints(this.columns);
			this.longestTop = this.topHints.size();
			this.longestSide = this.getLongest(this.sideHints); // Get widest "layer"
			// Since this is layered, we just need number of layers.
			this.lTop = this.longestTop;
			this.lSide = this.longestSide;
			this.bm = Bitmap.createBitmap((this.gWidth + this.longestSide) * 50,
					(this.gHeight + this.longestTop) * 50, Bitmap.Config.RGB_565);
			this.canvasBitmap = new Canvas(this.bm);
			this.paintBitmap = new Paint();
		}
		this.drawOnCanvas();
		// Change canvas and it'll reflect on the bm.
		this.setImageBitmap(this.bm);
	}

	public void clearGame() {
		this.gCurrent.replace("1", "0");
		this.bitmapFromCurrent();
	}

	// Site
	// http://stackoverflow.com/questions/8629202/fast-conversion-from-one-dimensional-array-to-two-dimensional-in-java
	private char[][] convertOneDimensionalToTwoDimensional(final int numberOfRows,
			final int rowSize, final char[] srcMatrix) {

		final int srcMatrixLength = srcMatrix.length;
		int srcPosition = 0;

		final char[][] returnMatrix = new char[numberOfRows][];
		for (int i = 0; i < numberOfRows; i++) {
			final char[] row = new char[rowSize];
			final int nextSrcPosition = srcPosition + rowSize;
			if (srcMatrixLength >= nextSrcPosition) {
				// Copy the data from the file if it has been written before.
				// Otherwise we just keep row empty.
				System.arraycopy(srcMatrix, srcPosition, row, 0, rowSize);
			}
			returnMatrix[i] = row;
			srcPosition = nextSrcPosition;
		}
		return returnMatrix;

	}

	private void drawGame() {
		final int heightTrim = this.canvasBitmap.getHeight() % (this.gHeight + this.longestTop);
		final int widthTrim = this.canvasBitmap.getWidth() % (this.gWidth + this.longestSide);
		this.paintBitmap.setColor(Color.RED);
		final int widthOffset = (this.canvasBitmap.getWidth() - widthTrim)
				/ (this.longestSide + this.gWidth);
		final int heightOffset = (this.canvasBitmap.getHeight() - heightTrim)
				/ (this.gHeight + this.longestTop);
		this.cellWidth = widthOffset;
		this.cellHeight = heightOffset;
		int row = -1, column = 0;
		for (int i = 0; i != this.gCurrent.length(); ++i) {
			if ((i % (this.gWidth)) == 0) {
				column = 0;
				++row;
			}
			final Rect r = new Rect(widthOffset * (this.longestSide + column), heightOffset
					* (this.longestTop + row), widthOffset * (this.longestSide + column + 1),
					heightOffset * (this.longestTop + row + 1));
			// INFO: This is where we draw the board.
			this.paintBitmap
					.setColor(this.gColors[Integer.parseInt(this.gCurrent.charAt(i) + "")]);
			this.canvasBitmap.drawRect(r, this.paintBitmap);
			/*
			 * if (i != 0) { if (this.gCurrent.charAt(i) != this.gCurrent.charAt(i - 1)) { if (this.gCurrent.charAt(i) == '0') { this.paintBitmap.setColor(Color.WHITE); } else { this.paintBitmap.setColor(Color.BLACK); } } } else { if (this.gCurrent.charAt(i) == '0') { this.paintBitmap.setColor(Color.WHITE); } else { this.paintBitmap.setColor(Color.BLACK); } }
			 */
			// this.paintBitmap.setColor(this.gColors[Integer.parseInt(this.colorCharacter + "")]);
			// this.canvasBitmap.drawRect(r, this.paintBitmap);
			++column;
		}
	}

	private void drawGridlines() {
		this.paintBitmap.setStrokeWidth(3);
		final int heightTrim = this.canvasBitmap.getHeight() % (this.gHeight + this.longestTop);
		final int widthTrim = this.canvasBitmap.getWidth() % (this.gWidth + this.longestSide);
		// Up down.
		this.paintBitmap.setColor(this.getResources().getColor(R.color.foreground));
		final int widthOffset = (this.canvasBitmap.getWidth() - widthTrim)
				/ (this.longestSide + this.gWidth);
		final int heightOffset = (this.canvasBitmap.getHeight() - heightTrim)
				/ (this.gHeight + this.longestTop);

		for (int i = this.longestSide; i != ((this.gWidth + this.longestSide) + 1); ++i) {
			if ((i % 5) == 0) {
				this.paintBitmap.setStrokeWidth(this.paintBitmap.getStrokeWidth() + 4);
			}
			this.canvasBitmap.drawLine(widthOffset * i, 0, widthOffset * i,
					this.canvasBitmap.getHeight(), this.paintBitmap);
			if ((i % 5) == 0) {
				this.paintBitmap.setStrokeWidth(this.paintBitmap.getStrokeWidth() - 4);
			}
		}
		// Side side.
		for (int i = this.longestTop; i != ((this.gHeight + this.longestTop) + 1); ++i) {
			if ((i % 5) == 0) {
				this.paintBitmap.setStrokeWidth(this.paintBitmap.getStrokeWidth() + 4);
			}
			this.canvasBitmap.drawLine(0, heightOffset * i, this.canvasBitmap.getWidth(),
					heightOffset * i, this.paintBitmap);
			if ((i % 5) == 0) {
				this.paintBitmap.setStrokeWidth(this.paintBitmap.getStrokeWidth() - 4);
			}
		}
	}

	private void drawHints() {
		this.paintBitmap.setAntiAlias(true);
		this.paintBitmap.setColor(this.getResources().getColor(R.color.foreground));
		this.paintBitmap.setStrokeWidth(1);
		final int widthOffset = this.canvasBitmap.getWidth() / (this.longestSide + this.gWidth);
		final int heightOffset = this.canvasBitmap.getHeight() / (this.gHeight + this.longestTop);
		this.paintBitmap.setTextSize(heightOffset / 2);
		// Draw top hints.
		for (int i = 0; i != this.longestTop; ++i) {
			for (int j = 0; j != this.topHints.get(i).length; ++j) {
				this.canvasBitmap
						.drawText(this.topHints.get(i)[j], ((this.longestSide * widthOffset)
								+ (widthOffset / 2) + (j * widthOffset)) - 5,
								(this.longestTop * heightOffset) - (heightOffset * i) - 5,
								this.paintBitmap);
			}
		}
		// Draw side hints.
		this.paintBitmap.setTextAlign(Align.RIGHT);
		this.paintBitmap.setTextSize(widthOffset / 2);
		for (int i = 0; i != this.sideHints.size(); ++i) {
			// The 2 * heightOffset/3 is for balance issues.
			this.canvasBitmap.drawText(this.sideHints.get(i), (this.longestSide * widthOffset) - 5,
					(this.longestTop * heightOffset) + (i * heightOffset)
							+ ((2 * heightOffset) / 3), this.paintBitmap);

		}
	}

	private void drawOnCanvas() {
		// White out whole canvas.
		this.drawWhiteCanvas();
		// Draw game surface.
		this.drawGame();
		// Draw gridlines and hints
		this.drawGridlines();
		this.drawHints();
		this.paintBitmap.setColor(Color.RED);
		this.canvasBitmap.drawCircle(this.lastTouchX, this.lastTouchY, 5, this.paintBitmap);
	}

	private void drawWhiteCanvas() {
		final Bitmap draw = BitmapFactory.decodeResource(this.getResources(), R.drawable.grid);
		final Shader old = this.paintBitmap.getShader();
		this.paintBitmap.setShader(new BitmapShader(draw, TileMode.REPEAT, TileMode.REPEAT));
		this.paintBitmap.setColor(this.getResources().getColor(R.color.background));
		this.canvasBitmap.drawRect(0, 0, this.canvasBitmap.getWidth(),
				this.canvasBitmap.getHeight(), this.paintBitmap);
		this.paintBitmap.setShader(old);
	}

	void fixTrans() {
		this.matrix.getValues(this.m);
		final float transX = this.m[Matrix.MTRANS_X];
		final float transY = this.m[Matrix.MTRANS_Y];

		final float fixTransX = this.getFixTrans(transX, this.viewWidth, this.origWidth
				* this.saveScale);
		final float fixTransY = this.getFixTrans(transY, this.viewHeight, this.origHeight
				* this.saveScale);

		if ((fixTransX != 0) || (fixTransY != 0)) {
			this.matrix.postTranslate(fixTransX, fixTransY);
		}
	}

	private ArrayList<String> getColumns(final char[][] current2d) {
		final ArrayList<String> result = new ArrayList<String>();
		for (int i = 0; i != current2d[0].length; ++i) {
			String temp = "";
			for (int j = 0; j != current2d.length; ++j) {
				temp += current2d[j][i];
			}
			result.add(temp);
		}
		return result;
	}

	float getFixDragTrans(final float delta, final float viewSize, final float contentSize) {
		if (contentSize <= viewSize) {
			return 0;
		}
		return delta;
	}

	float getFixTrans(final float trans, final float viewSize, final float contentSize) {
		float minTrans, maxTrans;

		if (contentSize <= viewSize) {
			minTrans = 0;
			maxTrans = viewSize - contentSize;
		} else {
			minTrans = viewSize - contentSize;
			maxTrans = 0;
		}

		if (trans < minTrans) {
			return -trans + minTrans;
		}
		if (trans > maxTrans) {
			return -trans + maxTrans;
		}
		return 0;
	}

	private int getLongest(final ArrayList<?> list) {
		int longest = 0;
		for (final Object o : list) {
			final String temp[] = o.toString().replaceAll(" +", " ").split(" ");
			if (temp.length > longest) {
				longest = temp.length;
			}
		}
		return longest;
	}

	private int[] getPixelArrayFromString(final String from, final int length) {
		final int[] colors = new int[length];
		for (int i = 0; i != colors.length; ++i) {
			if (from.charAt(i) == '0') {
				colors[i] = Color.WHITE;
			} else {
				colors[i] = Color.BLACK;
			}
		}
		return colors;
	}

	private ArrayList<String> getRows(final char[][] current2d) {
		final ArrayList<String> result = new ArrayList<String>();

		for (int i = 0; i != current2d.length; ++i) {
			String temp = "";
			for (int j = 0; j != current2d[i].length; ++j) {
				temp += current2d[i][j];
			}
			result.add(temp);
		}
		return result;
	}

	// regex:
	// http://stackoverflow.com/questions/15101577/split-string-when-character-changes-possible-regex-solution
	private ArrayList<String> getSideHints(final ArrayList<String> rows) {
		final ArrayList<String> result = new ArrayList<String>();
		for (final String row : rows) {
			String temp = "";
			row.replaceFirst("^0+(?=[^0])", ""); // Remove leading 0's.
			final String nums[] = row.split("0+|(?<=([1-9]))(?=[1-9])(?!\\1)");
			for (final String item : nums) {
				temp += item + " ";
			}
			result.add(temp);
		}
		final ArrayList<String> lengths = this.listToLengths(result);
		result.clear();

		return lengths;
	}

	private ArrayList<String[]> getTopHints(final ArrayList<String> columns) {
		final ArrayList<String[]> result = new ArrayList<String[]>();
		final ArrayList<String> parsed = this.getSideHints(columns);
		for (int i = 0; i != parsed.size(); ++i) {
			String temp = parsed.get(i);
			final String[] split = temp.split(" ");
			for (int j = 0; j != split.length; ++j)
			{
				split[j] = new StringBuilder(split[j]).reverse().toString();
			}
			String emp = "";
			for (final String s : split) {
				emp += s + " ";
			}
			temp = emp.substring(0, emp.length() - 1); // Minus 1 to get rid of
														// space.
			temp = new StringBuilder(temp).reverse().toString();
			parsed.set(i, temp);
		}
		final int longest = this.getLongest(parsed);
		for (int i = 0; i != longest; ++i) {
			String temp = "";
			for (int j = 0; j != parsed.size(); ++j) {
				final String split[] = parsed.get(j).split(" ");
				if (i >= split.length) {
					temp += " ,";
				} else {
					temp += split[i] + ",";
				}
			}
			// Using a , split for double digit numbers, things can get big ;).
			result.add(temp.split(","));
		}
		// Note: result needs to be flipped when actually printed, or printed
		// upside down.
		return result;
	}

	private ArrayList<String> listToLengths(final ArrayList<String> list) {
		final ArrayList<String> result = new ArrayList<String>();
		for (int i = 0; i != list.size(); ++i) {
			String temp = "";
			final String parse[] = list.get(i).split(" +");
			for (final String p : parse) {
				if (p.length() != 0) {
					temp += p.length() + " ";
				}
			}
			if (temp.length() == 0) {
				result.add("0");
			} else {
				result.add(temp.substring(0, temp.length() - 1));
			}
		}
		return result;
	}

	@Override
	protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		this.viewWidth = MeasureSpec.getSize(widthMeasureSpec);
		this.viewHeight = MeasureSpec.getSize(heightMeasureSpec);

		//
		// Rescales image on rotation
		//
		if (((this.oldMeasuredHeight == this.viewWidth) && (this.oldMeasuredHeight == this.viewHeight))
				|| (this.viewWidth == 0) || (this.viewHeight == 0)) {
			return;
		}
		this.oldMeasuredHeight = this.viewHeight;
		this.oldMeasuredWidth = this.viewWidth;

		if (this.saveScale == 1) {
			// Fit to screen.
			float scale;

			final Drawable drawable = this.getDrawable();
			if ((drawable == null) || (drawable.getIntrinsicWidth() == 0)
					|| (drawable.getIntrinsicHeight() == 0)) {
				return;
			}
			final int bmWidth = drawable.getIntrinsicWidth();
			final int bmHeight = drawable.getIntrinsicHeight();

			Log.d("bmSize", "bmWidth: " + bmWidth + " bmHeight : " + bmHeight);

			final float scaleX = (float) this.viewWidth / (float) bmWidth;
			final float scaleY = (float) this.viewHeight / (float) bmHeight;
			scale = Math.min(scaleX, scaleY);
			this.matrix.setScale(scale, scale);

			// Center the image
			float redundantYSpace = this.viewHeight - (scale * bmHeight);
			float redundantXSpace = this.viewWidth - (scale * bmWidth);
			redundantYSpace /= 2;
			redundantXSpace /= 2;

			this.matrix.postTranslate(redundantXSpace, redundantYSpace);

			this.origWidth = this.viewWidth - (2 * redundantXSpace);
			this.origHeight = this.viewHeight - (2 * redundantYSpace);
			this.setImageMatrix(this.matrix);
		}
		this.fixTrans();
	}

	// Just add on fluff area for the hints on the top and on the side.
	private int[] resizeBitMapsForHints(final int[] colors, final int longestTop,
			final int longestSide) {
		final int result[] = new int[(longestTop * (longestSide + this.gWidth)) + colors.length
				+ (this.gHeight * longestSide)];
		int runner;
		// Fill up the top with blank white.
		for (runner = 0; runner != (longestTop * (longestSide + this.gWidth)); ++runner) {
			result[runner] = Color.WHITE;
		}
		// Fill side hints with white, and the image with what was in it
		// previously.
		int colorRunner = 0; // Used to run through original colors.
		for (int i = 0; i != this.gHeight; ++i) {
			// Draw side for hints.
			for (int j = 0; j != longestSide; ++j) {
				result[runner++] = Color.WHITE;
			}
			// Add in the array/picture.
			for (int j = 0; j != this.gWidth; ++j) {
				result[runner++] = colors[colorRunner++];
			}
		}
		return result;
	}

	// Get bundled info and set it for use.
	public void setGriddlerInfo(final Bundle savedInstanceState) {
		this.gCurrent = savedInstanceState.getString("current");
		this.gHeight = Integer.parseInt(savedInstanceState.getString("height"));
		this.gWidth = Integer.parseInt(savedInstanceState.getString("width"));
		this.gId = Integer.parseInt(savedInstanceState.getString("id"));
		this.gSolution = savedInstanceState.getString("solution");
		this.gColors = savedInstanceState.getIntArray("colors");
		this.bitmapFromCurrent();
	}

	public void setMaxZoom(final float x) {
		this.maxScale = x;
	}

	public void setWinListener(final WinnerListener winListener) {
		this.winListener = winListener;
	}

	private void sharedConstructing(final Context context) {
		super.setClickable(true);
		this.context = context;
		this.mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
		this.matrix = new Matrix();
		this.m = new float[9];
		this.setImageMatrix(this.matrix);
		this.setScaleType(ScaleType.MATRIX);

		this.setOnTouchListener(this.touchListener);
	}

	private char[][] solutionTo2DArray() {
		final char[][] result = new char[this.gHeight][this.gWidth];
		int runner = 0;
		for (int i = 0; i != result.length; ++i) {
			for (int j = 0; j != result[i].length; ++j) {
				result[i][j] = this.gSolution.charAt(runner++);
			}
		}
		return result;
	}
}
