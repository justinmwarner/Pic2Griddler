package com.pic2griddler.awesomeness;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ViewFlipper;
public class CreateGriddlerActivity extends Activity implements OnClickListener, OnItemSelectedListener {

	private static final int CAMERA_REQUEST_CODE = 1888, FILE_SELECT_CODE = 1337;
	private static final String TAG = "CreateGriddlerActivity";
	private EditText etURL;
	private boolean isOriginal = true;
	private ImageView ivPicture, ivAfter;
	// griddlerDifficulty;
	private int numColors, yNum, xNum;
	private Bitmap oldPicture, newPicture;
	private String solution = "", current = "", griddlerName, griddlerTags;// ,
	private Spinner sX, sY, sColor, sDiff;
	private ViewFlipper vf;;

	private void alterPhoto() {
		if (oldPicture != null) {
			// Touch this up. It's a bit messy.
			solution = ""; //Change back to nothing.
			numColors = Integer.parseInt(sColor.getSelectedItem().toString());
			yNum = Integer.parseInt(sY.getSelectedItem().toString());
			xNum = Integer.parseInt(sX.getSelectedItem().toString());
			Bitmap scaled = Bitmap.createScaledBitmap(oldPicture, xNum * 10, yNum * 10, false);
			ivPicture.setImageBitmap(scaled);
			Bitmap alter = Bitmap.createScaledBitmap(oldPicture, xNum, yNum, false);
			// Set pixels = to each pixel in the scaled image (Easier to find
			// values, and smaller!)
			int pixels[] = new int[xNum * yNum];
			alter.getPixels(pixels, 0, alter.getWidth(), 0, 0, alter.getWidth(), alter.getHeight());
			for (int i = 0; i < pixels.length; i++) {
				int r = (pixels[i]) >> 16 & 0xff;
				int g = (pixels[i]) >> 8 & 0xff;
				int b = (pixels[i]) & 0xff;
				pixels[i] = (r + g + b) / 3; // Convert the values in pixels to
												// be grey values. Or normalize
												// them.
			}
			int pix[][] = new int[yNum][xNum]; // Height, then width per a
												// height.
			int run = 0;
			for (int i = 0; i < pix.length; i++) {
				for (int j = 0; j < pix[i].length; j++) {
					pix[i][j] = pixels[run++];
				}
			}
			for (int i = 0; i < alter.getWidth(); i++) {
				for (int j = 0; j < alter.getHeight(); j++) {
					if (pix[j][i] >= 256 / numColors) {
						alter.setPixel(i, j, Color.WHITE); // Change color in an
															// array. Get to it
															// later.
						pix[j][i] = 0;
					} else {
						alter.setPixel(i, j, Color.BLACK);
						pix[j][i] = 1;
					}
				}
			}
			// Set up "solution" for when it's submitted, this requires us to go
			for (int i = 0; i < pix.length; i++) {
				for (int j = 0; j < pix[i].length; j++) {
					solution += pix[i][j];
					current += "0";
				}
			}
			alter = Bitmap.createScaledBitmap(alter, xNum * 10, yNum * 10, false);
			newPicture = alter;
			changePictures();
		} else {
			print("We need a valid photo first.");
		}
	}

	private void changePictures() {
		if (isOriginal) // Go to the new picture.
		{
			if (newPicture != null) {
				ivPicture.setImageBitmap(newPicture);
				isOriginal = false;
			}
		} else
		// Go to the old picture.
		{
			ivPicture.setImageBitmap(oldPicture);
			isOriginal = true;
		}
	}

	@Override
	protected void onActivityResult(int request, int result, Intent data) {
		if (request == CAMERA_REQUEST_CODE) {
			if (result == Activity.RESULT_OK) {
				Bitmap photo = (Bitmap) data.getExtras().get("data");
				ivPicture.setImageBitmap(photo);
				oldPicture = photo;
				vf.setDisplayedChild(2);
			} else {
				print("Aww, we wanted your picture =(");
			}
		} else if (request == FILE_SELECT_CODE) {
			if (result == Activity.RESULT_OK) {
				Uri uri = data.getData();
				Bitmap bi = readBitmap(uri);
				ivPicture.setImageBitmap(bi);
				oldPicture = bi;
				vf.setDisplayedChild(2);
			} else {
				print("Aww, we wanted your picture =(");
			}
		}
	}

	public void onClick(View v) {
		// "Hide" the keyboard when you move steps.
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(((EditText) findViewById(R.id.etNameA)).getWindowToken(), 0);
		if (v.getId() == R.id.bLeft) {
			vf.setInAnimation(this, R.anim.in_from_left);
			vf.setOutAnimation(this, R.anim.out_to_right);
			if (vf.getDisplayedChild() == 0) {
				// Exit.
				finish();
			} else if (vf.getDisplayedChild() == 2) {
				vf.setDisplayedChild(0);
			} else {
				vf.showPrevious();
			}
		} else if (v.getId() == R.id.bRight) {
			vf.setInAnimation(this, R.anim.in_from_right);
			vf.setOutAnimation(this, R.anim.out_to_left);
			if (vf.getDisplayedChild() == 0) // On the first, don't do
												// anything, must select a
												// button.
			{

			} else if (vf.getDisplayedChild() == 1) // On the URL.
			{
				new Thread(new Runnable() {
					public void run() {
						try {
							etURL = (EditText) findViewById(R.id.etURLA);
							URL url = new URL(etURL.getText().toString());
							HttpURLConnection conn = (HttpURLConnection) url.openConnection();
							conn.setDoInput(true);
							conn.connect();
							InputStream in;
							in = conn.getInputStream();
							final Bitmap bm = BitmapFactory.decodeStream(in);
							oldPicture = bm;
							ivPicture.post(new Runnable() {
								public void run() {
									ivPicture.setImageBitmap(bm);
									oldPicture = bm;
								}

							});
						} catch (IOException e) {
							e.printStackTrace();
							// print(e.toString());
						}
					}

				}).start();
				print("Saved picture from url.");
				vf.showNext();
			} else if (vf.getDisplayedChild() == 2) // Doing spinner stuff.
			{
				// Make sure 2(3) spinners are valid.
				if (sX.getSelectedItemPosition() != AdapterView.INVALID_POSITION) {
					if (sY.getSelectedItemPosition() != AdapterView.INVALID_POSITION) {
						Toast.makeText(this, "X: " + sX.getSelectedItem().toString() + " Y: " + sY.getSelectedItem().toString(), Toast.LENGTH_LONG).show();
						vf.showNext();
					} else {
						print("You need a height!");
					}
				} else {
					print("You need a width!");
				}
			} else if (vf.getDisplayedChild() == 3) // Doing name and tag stuff.
			{
				// Make sure valid name, tags, and difficulty.
				griddlerName = ((EditText) findViewById(R.id.etNameA)).getText().toString();
				griddlerTags = ((EditText) findViewById(R.id.etTagA)).getText().toString();
				if (griddlerName.length() > 0) {
					if (griddlerTags.length() > 0) {
						vf.showNext();
					} else {
						print("We require at least one tag.  Please make it useful or your Griddler could be deleted.");
					}
				} else {
					print("We require a name for your Griddler.  Make it relevant or your Griddler could be deleted.");
				}
			} else {
				// Now save.
				Intent returnIntent = new Intent();
				returnIntent.putExtra("solution", solution);
				String username = "justinwarner";
				returnIntent.putExtra("author", username);
				returnIntent.putExtra("name", griddlerName);
				returnIntent.putExtra("rank", 1 + "");
				returnIntent.putExtra("difficulty", sDiff.getItemAtPosition(sDiff.getSelectedItemPosition()).toString());
				returnIntent.putExtra("width", xNum + "");
				returnIntent.putExtra("height", yNum + "");
				returnIntent.putExtra("tags", griddlerTags);
				setResult(RESULT_OK, returnIntent);
				finish();
			}
		} else if (v.getId() == R.id.bCameraA) {
			Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
			this.startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
		} else if (v.getId() == R.id.bFileA) {
			// File stuff.
			Intent fileIntent = new Intent(Intent.ACTION_GET_CONTENT);
			fileIntent.setType("*/*");
			fileIntent.addCategory(Intent.CATEGORY_OPENABLE); // Shows openable
																// files! =)
			this.startActivityForResult(fileIntent, FILE_SELECT_CODE);
		} else if (v.getId() == R.id.bURLA) {
			vf.setDisplayedChild(1);
		} else if (v.getId() == R.id.ivGriddlerCreate) {
			changePictures();
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_create_advanced);
		Button photoButton = (Button) findViewById(R.id.bCameraA);
		Button fileButton = (Button) findViewById(R.id.bFileA);
		Button urlButton = (Button) findViewById(R.id.bURLA);
		Button leftButton = (Button) findViewById(R.id.bLeft);
		Button rightButton = (Button) findViewById(R.id.bRight);

		ivPicture = (ImageView) findViewById(R.id.ivGriddlerCreate);
		photoButton.setOnClickListener(this);
		fileButton.setOnClickListener(this);
		urlButton.setOnClickListener(this);
		leftButton.setOnClickListener(this);
		rightButton.setOnClickListener(this);
		ivPicture.setOnClickListener(this);
		vf = (ViewFlipper) findViewById(R.id.vfContainer);

		// Add items to spinners... Might be a better way to do this, seriously,
		// this is idiotic.
		sX = (Spinner) findViewById(R.id.spinWidth);
		sY = (Spinner) findViewById(R.id.spinHeight);
		sColor = (Spinner) findViewById(R.id.spinColor);
		sDiff = (Spinner) findViewById(R.id.spinDiffA);
		sX.setOnItemSelectedListener(this);
		sY.setOnItemSelectedListener(this);
		sColor.setOnItemSelectedListener(this);
		String colorNumbers[] = new String[9];
		String xyNumbers[] = new String[20]; // Support more than 20 for
												// multi-griddlers in future.
		String difficulties[] = { "Easy", "Medium", "Hard", "Extreme" };
		for (int i = 1; i < 21; i++)
			xyNumbers[i - 1] = "" + i;
		for (int i = 2; i < 11; i++)
			colorNumbers[i - 2] = "" + i;
		ArrayAdapter xy = new ArrayAdapter(this, android.R.layout.simple_spinner_item, xyNumbers);
		ArrayAdapter cols = new ArrayAdapter(this, android.R.layout.simple_spinner_item, colorNumbers);
		ArrayAdapter diffs = new ArrayAdapter(this, android.R.layout.simple_spinner_item, difficulties);
		sX.setAdapter(xy);
		sY.setAdapter(xy);
		sColor.setAdapter(cols);
		sDiff.setAdapter(diffs);

		// Check if we're getting data from a share.
		Intent intent = getIntent();
		String action = intent.getAction();
		String type = intent.getType();
		if (Intent.ACTION_SEND.equals(action) && type != null) {
			if (type.startsWith("image/")) {
				Uri uri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
				Bitmap bi = readBitmap(uri);
				ivPicture.setImageBitmap(bi);
				oldPicture = bi;
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
		// When item is changed, update.'
		Log.d(TAG, pos + "");
		if (pos >= 1) {
			alterPhoto();
		}
	}

	public void onNothingSelected(AdapterView<?> arg0) {
	}

	private void print(String t) {
		Toast.makeText(this, t, Toast.LENGTH_SHORT).show();
	}

	// Read bitmap - From
	// http://tutorials-android.blogspot.co.il/2011/11/outofmemory-exception-when-decoding.html
	public Bitmap readBitmap(Uri selectedImage) {
		Bitmap bm = null;
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inSampleSize = 5;
		AssetFileDescriptor fileDescriptor = null;
		try {
			fileDescriptor = this.getContentResolver().openAssetFileDescriptor(selectedImage, "r");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			try {
				bm = BitmapFactory.decodeFileDescriptor(fileDescriptor.getFileDescriptor(), null, options);
				fileDescriptor.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return bm;
	}

}
