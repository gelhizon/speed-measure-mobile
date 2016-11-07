package com.gel.speedmeasure;

import java.util.ArrayList;
import java.util.Collections;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;

public class BackgroundProcessing implements Runnable {
	private final String TAG = "BackgroundProcessing.java";

	// Not done processing?
	private boolean done;
	private boolean pause;

	// Settings
	private int previewWidth;
	private int previewHeight;
	private int previewWidthHalf;
	private int size;
	private double distance;
	private int minBlobSize;
	private int maxBlobSize;
	private int minSpeed;
	private int maxSpeed;
	private String unit;

	// Matcher Settings
	private ArrayList<Double> speedData;
	private boolean isRight;

	// Frames
	private byte[] frame;
	private byte[] curFrame;
	private int[] output;

	// Timers
	private long prevTime;
	private long curTime;
	private long timeDifference;
	private long totalTimeFinished;
	private int frameCount;
	private double fps;
	private long pauseBlinker;
	private long speedTimerBeforeClearing;

	// Image Processing
	private BackgroundSubtraction bgSub;
	private BlobFinder bf;
	private ArrayList<BlobFinder.Blob> blobList;
	private BlobFinder.Blob prevBlob;
	private BlobFinder.Blob curBlob;
	private Bitmap resultBitmap;
	private Canvas canvas;
	private Paint greenPaint;
	private Paint redPaint;
	private Paint greenFontPaint;
	private ImageView cameraImageView;
	private TextView fpsTextView;
	private TextView infoTextView;
	private TextView speedTextView;
	private String info;
	private TextView statusTextView;

	public BackgroundProcessing(int previewWidth, int previewHeight, double distance, BackgroundSubtraction bgSub, int minBlobSize, int maxBlobSize, int minSpeed, int maxSpeed, String unit, ImageView cameraImageView, TextView fpsTextView, TextView infoTextView, TextView speedTextView, TextView statusTextView) {

		// Initialization
		done = true;
		pause = false;
		pauseBlinker = 1000;
		speedTimerBeforeClearing = 0;
		prevTime = System.currentTimeMillis();
		curTime = System.currentTimeMillis();
		timeDifference = 0L;
		totalTimeFinished = 0L;
		frameCount = 0;
		fps = 0.0D;

		// Settings
		this.previewWidth = previewWidth;
		this.previewHeight = previewHeight;
		this.size = previewWidth * previewHeight;
		this.previewWidthHalf = previewWidth / 2;
		this.distance = distance;
		this.speedTextView = speedTextView;
		this.minBlobSize = minBlobSize;
		this.maxBlobSize = maxBlobSize;
		this.minSpeed = minSpeed;
		this.maxSpeed = maxSpeed;
		this.unit = unit;
		this.output = new int[previewWidth * previewHeight];

		// Matcher Settings
		this.speedData = new ArrayList<Double>();
		this.isRight = false;

		// Image Processing
		this.bgSub = bgSub;
		bf = new BlobFinder(previewWidth, previewHeight);
		blobList = new ArrayList<BlobFinder.Blob>();
		prevBlob = null;
		curBlob = null;
		resultBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
		canvas = new Canvas(resultBitmap);
		cameraImageView.setImageBitmap(resultBitmap);
		greenPaint = new Paint();
		greenPaint.setColor(Color.GREEN);
		greenPaint.setStrokeWidth(1);
		greenPaint.setStyle(Style.STROKE);
		greenPaint.setTextSize(20);
		redPaint = new Paint();
		redPaint.setColor(Color.RED);
		redPaint.setStrokeWidth(1);
		redPaint.setStyle(Style.STROKE);
		redPaint.setTextSize(20);
		greenFontPaint = new Paint();
		greenFontPaint.setColor(Color.GREEN);
		greenFontPaint.setStrokeWidth(1);
		greenFontPaint.setStyle(Style.FILL);
		greenFontPaint.setTextSize(20);
		this.cameraImageView = cameraImageView;
		this.fpsTextView = fpsTextView;
		this.infoTextView = infoTextView;
		this.statusTextView = statusTextView;

	}

	@Override
	public void run() {
		// done = false;

		// Frames Per Second Counter
		prevTime = curTime;
		curTime = System.currentTimeMillis();
		timeDifference = curTime - prevTime;
		totalTimeFinished += timeDifference;
		frameCount++;
		if (totalTimeFinished > 5000) {
			double seconds = totalTimeFinished / 1000.0;
			fps = (frameCount) / seconds;
			totalTimeFinished = 0;
			frameCount = 0;
		}

		// Current Frame
		curFrame = frame;

		// Background Subtraction
		// long timer = (System.currentTimeMillis());
		bgSub.subtract(curFrame, output, timeDifference);
		// info = "BGSub: " + (System.currentTimeMillis() - timer);

		// Draw Binary Image to Bitmap
		// timer = System.currentTimeMillis();
		resultBitmap.setPixels(output, 0, previewWidth, 0, 0, previewWidth, previewHeight);
		// timer = (System.currentTimeMillis() - timer);
		// info = info + "\nDrawImg: " + timer;

		// Blob Detection
		// timer = System.currentTimeMillis();
		bf.detectBlobs(output, minBlobSize, maxBlobSize, 0xffffffff, blobList);
		// timer = (System.currentTimeMillis() - timer);
		// info = info + "\nBlobD: " + timer;

		// Find Largest Blobs
		// timer = System.currentTimeMillis();
		int min = Integer.MIN_VALUE;
		for (BlobFinder.Blob b : blobList) {
			if (b.mass > min) {
				curBlob = b;
				min = b.mass;
			}
		}
		// timer = (System.currentTimeMillis() - timer);
		// info = info + "\nLargeBlob: " + timer;

		// Match Large Blobs
		// timer = System.currentTimeMillis();
		// if still same blob as previous blob no change
		if (curBlob != prevBlob) {
			if (prevBlob == null) {
				prevBlob = curBlob;

				// if blob show on right side of preview.use xMin
				if (curBlob.xMin > previewWidthHalf) {
					isRight = true;
				} else {
					isRight = false;
				}
			} else {

				// get left or right of the blob. more accurate than getting center
				double result = 0;
				if (isRight && curBlob.xMin > 0) {
					result = Math.abs(curBlob.xMin - prevBlob.xMin);
				} else if (curBlob.xMax < previewWidth - 1) {
					result = Math.abs(curBlob.xMax - prevBlob.xMax);
				} else {
					result = 0.0;
				}
				double converted = result * distance / previewWidth;
				double seconds = ((double) timeDifference / 1000.0);
				double speed = converted / seconds;
				double rounded = Math.round((speed) * 100) / 100.0;
				canvas.drawText(String.valueOf(rounded), curBlob.xMin, curBlob.yMin - 5, greenFontPaint);
				canvas.drawRect(curBlob.xMin, curBlob.yMin, curBlob.xMax, curBlob.yMax, greenPaint);

				// pass current blob to previous blob
				prevBlob = curBlob;

				// should only calculate if speed reaches limit
				if (speed >= minSpeed && speed <= maxSpeed && !pause) {
					speedData.add(speed);
					Collections.sort(speedData);

					double mean;
					double sum = 0;
					for (Double d : speedData) {
						sum += d;
					}
					mean = sum / speedData.size();

					double median;
					if (speedData.size() % 2 == 0) {
						int center1 = 0;
						int center2 = 0;
						center1 = (speedData.size() / 2) - 1;
						center2 = (speedData.size() / 2);
						median = (speedData.get(center1) + speedData.get(center2)) / 2.0;
					} else {
						median = speedData.get((speedData.size() / 2));
					}

					long popularity1 = 0;
					long popularity2 = 0;
					long popularity_item = 0, array_item = 0;
					ArrayList<Long> modes = new ArrayList<Long>();

					for (int i = 0; i < speedData.size(); i++) {
						array_item = Math.round(speedData.get(i));
						for (int j = i; j < speedData.size(); j++) {
							if (array_item == Math.round(speedData.get(j))) {
								popularity1++;
							} else {
								break;
							}
						}
						if (popularity1 > popularity2) {
							popularity_item = array_item;
							modes.clear();
							modes.add(popularity_item);
							popularity2 = popularity1;
						} else if (popularity1 == popularity2) {
							modes.add(array_item);
						}
						popularity1 = 0;

					}
					String modesString = "";
					for (Long l : modes) {
						modesString = modesString + ", " + (l);
					}
					modesString = modesString.substring(2);

					speedTextView.setText("\nSpeed: " + Math.round(mean * 100) / 100.0 + unit + "/s"  );
					// canvas.drawRect(curBlob.xMin, curBlob.yMin, curBlob.xMax, curBlob.yMax, greenPaint);
					// statusTextView.setText("x: " + curBlob.xMin + " y: " + curBlob.yMin);
				}
			}
			speedTimerBeforeClearing = 0;
		} else {
			// no blob is detected clear list. or create new one
			speedData.clear();
			prevBlob = null;
			speedTimerBeforeClearing += timeDifference;
			if (speedTimerBeforeClearing > 5000) {
				speedTimerBeforeClearing = 0;
				speedTextView.setText("\nSpeed:\nMean: 0\nMedian: 0\nMode: 0");
			}
		}

		// clear current bloblist for next blob detection
		blobList.clear();

		// timer = (System.currentTimeMillis() - timer);
		// info = info + "\nMatching: " + timer;

		// Invalidate
		// timer = System.currentTimeMillis();
		cameraImageView.invalidate();
		// timer = (System.currentTimeMillis() - timer);
		// info = info + "\nInvalidate: " + timer;

		// Pause Speed
		if (pause) {
			statusTextView.setText("Pause");
		} else {
			statusTextView.setText("");
		}

		// Set FPS on TextView
		fpsTextView.setText("fps: " + String.valueOf(Math.round(fps)));
		infoTextView.setText(info);

		// done = true;
	}

	public boolean isDone() {
		return done;
	}

	public void setFrame(byte[] frame) {
		this.frame = frame;
	}

	public int getMinBlobSize() {
		return minBlobSize;
	}

	public void setMinBlobSize(int minBlobSize) {
		this.minBlobSize = minBlobSize;
	}

	public int getMaxBlobSize() {
		return maxBlobSize;
	}

	public void setMaxBlobSize(int maxBlobSize) {
		this.maxBlobSize = maxBlobSize;
	}

	public int getMinSpeed() {
		return minSpeed;
	}

	public void setMinSpeed(int minSpeed) {
		this.minSpeed = minSpeed;
	}

	public int getMaxSpeed() {
		return maxSpeed;
	}

	public void setMaxSpeed(int maxSpeed) {
		this.maxSpeed = maxSpeed;
	}

	public double getDistance() {
		return distance;
	}

	public void setDistance(double distance) {
		this.distance = distance;
	}

	public boolean isPause() {
		return pause;
	}

	public void setPause(boolean pause) {
		this.pause = pause;
	}

	public String getUnit() {
		return unit;
	}

	public void setUnit(String unit) {
		this.unit = unit;
	}

}
