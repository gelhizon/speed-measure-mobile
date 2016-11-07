package com.gel.speedmeasure;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;

public class MainActivity extends Activity {
	protected static final String TAG = "MainActivity.java";

	// Settings
	private int previewWidth;
	private int previewHeight;
	private int size;
	private double distance;
	private boolean isGray;
	private boolean isColor;
	private int grayThreshold;
	private int colorThreshold;
	private int pixelTimeLimit;
	private boolean grayOrColor;
	private boolean cameraVisible;
	private int minBlobSize;
	private int maxBlobSize;
	private int minSpeed;
	private int maxSpeed;
	private String unit;

	// Components / GUI Elements
	private TextView fpsTextView;
	private TextView tvStatus;
	private TextView infoTextView;
	private TextView speedTextView;
	private SurfaceView cameraSurfaceView;
	private ImageView cameraImageView;
	private ImageView btnSettings;
	// settings area
	private Spinner unitSpinner;
	private EditText distanceTextView;
	private SeekBar graySeekBar;
	private SeekBar colorSeekBar;
	private SeekBar minOMSeekBar;
	private SeekBar maxOMSeekBar;
	private TextView grayTextView;
	private TextView colorTextView;
	private TextView minOMTextView;
	private TextView maxOMTextView;
	private TextView minSpeedTextView;
	private TextView maxSpeedTextView;
	private SeekBar minSpeedSeekBar;
	private SeekBar maxSpeedSeekBar;
	private RadioGroup radioGroup;
	private RadioButton anyRadioButton;
	private RadioButton bothRadioButton;
	private RadioButton grayButton;
	private RadioButton colorButton;
	private Button hideButton;

	// Resources
	private CameraPreview camPreview;
	private SurfaceHolder mCamHolder;

	// Background Subtraction
	private BackgroundSubtraction bgSub;

	// Background Processing
	private BackgroundProcessing bgProcessing;

	// Dialogs
	private Dialog distanceDialog;
	private Dialog settingsDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		// Screen Always On
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		// Initialize Settings (Default)
		previewWidth = 320;
		previewHeight = 240;
		size = previewWidth * previewHeight;
		distance = 31;
		grayOrColor = false;
		isGray = false;
		isColor = true;
		cameraVisible = true;
		grayThreshold = 30;
		colorThreshold = 20;
		pixelTimeLimit = 3000;
		minBlobSize = 750;
		maxBlobSize = size / 2;
		minSpeed = 5;
		maxSpeed = 200;
		unit = "in";

		// Initialize components
		unitSpinner = (Spinner) findViewById(R.id.spinnerUnit);
		distanceTextView = (EditText) findViewById(R.id.tvDistance);
		fpsTextView = (TextView) findViewById(R.id.fpsTextView);
		tvStatus = (TextView) findViewById(R.id.tvStatus);
		infoTextView = (TextView) findViewById(R.id.tvInfo);
		speedTextView = (TextView) findViewById(R.id.tvSpeed);
		cameraSurfaceView = (SurfaceView) findViewById(R.id.cameraSurfaceView);
		cameraImageView = (ImageView) findViewById(R.id.cameraImageView);
		btnSettings = (ImageView) findViewById(R.id.btnSettings);
		hideButton = (Button) findViewById(R.id.btnHide);

		// Background Subtraction instance
		bgSub = new BackgroundSubtraction(size, previewWidth, grayThreshold, colorThreshold, pixelTimeLimit, isGray, isColor, grayOrColor);

		// Image Processing Initialization
		bgProcessing = new BackgroundProcessing(previewWidth, previewHeight, distance, bgSub, minBlobSize, maxBlobSize, minSpeed, maxSpeed, unit, cameraImageView, fpsTextView, infoTextView, speedTextView, tvStatus);

		// Camera Preview Initialization
		camPreview = new CameraPreview(previewWidth, previewHeight, bgProcessing);
		mCamHolder = cameraSurfaceView.getHolder();
		mCamHolder.addCallback(camPreview);
		mCamHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		// Settings
		settingsDialog = new Dialog(this);
		settingsDialog.setContentView(R.layout.settings);
		settingsDialog.setTitle("Settings");

		// settings area
		radioGroup = (RadioGroup) findViewById(R.id.rg);
		anyRadioButton = (RadioButton) findViewById(R.id.btnAny);
		bothRadioButton = (RadioButton) findViewById(R.id.btnBoth);
		grayButton = (RadioButton) findViewById(R.id.btnGray);
		colorButton = (RadioButton) findViewById(R.id.btnColor);
		graySeekBar = (SeekBar) settingsDialog.findViewById(R.id.sbGray);
		colorSeekBar = (SeekBar) settingsDialog.findViewById(R.id.sbColor);
		minOMSeekBar = (SeekBar) settingsDialog.findViewById(R.id.sbMinOM);
		maxOMSeekBar = (SeekBar) settingsDialog.findViewById(R.id.sbMaxOM);
		minSpeedSeekBar = (SeekBar) settingsDialog.findViewById(R.id.sbMinSpeed);
		maxSpeedSeekBar = (SeekBar) settingsDialog.findViewById(R.id.sbMaxSpeed);
		grayTextView = (TextView) settingsDialog.findViewById(R.id.tvGray);
		colorTextView = (TextView) settingsDialog.findViewById(R.id.tvColor);
		minOMTextView = (TextView) settingsDialog.findViewById(R.id.tvMinOM);
		maxOMTextView = (TextView) settingsDialog.findViewById(R.id.tvMaxOM);
		minSpeedTextView = (TextView) settingsDialog.findViewById(R.id.tvMinSpeed);
		maxSpeedTextView = (TextView) settingsDialog.findViewById(R.id.tvMaxSpeed);

		// init settings
		unitSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				bgProcessing.setUnit(unitSpinner.getSelectedItem().toString());
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub
				
			}
		});
		distanceTextView.setText(String.valueOf(bgProcessing.getDistance()));
		if (bgSub.isGray() && bgSub.isColor()) {
			if (bgSub.isGrayOrColor()) {
				anyRadioButton.setChecked(true);
			} else {
				bothRadioButton.setChecked(true);
			}
		} else if (bgSub.isGray()) {
			grayButton.setChecked(true);
		} else {
			colorButton.setChecked(true);
		}
		graySeekBar.setProgress(bgSub.getGrayThreshold());
		colorSeekBar.setProgress(bgSub.getColorThreshold());
		minOMSeekBar.setProgress(bgProcessing.getMinBlobSize());
		maxOMSeekBar.setProgress(bgProcessing.getMaxBlobSize());
		minSpeedSeekBar.setProgress(bgProcessing.getMinSpeed());
		maxSpeedSeekBar.setProgress(bgProcessing.getMaxSpeed());
		grayTextView.setText(String.valueOf(bgSub.getGrayThreshold()));
		colorTextView.setText(String.valueOf(bgSub.getColorThreshold()));
		minOMTextView.setText(String.valueOf(bgProcessing.getMinBlobSize()));
		maxOMTextView.setText(String.valueOf(bgProcessing.getMaxBlobSize()));
		minSpeedTextView.setText(String.valueOf(bgProcessing.getMinSpeed()));
		maxSpeedTextView.setText(String.valueOf(bgProcessing.getMaxSpeed()));

		// Listeners
		hideButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				if (cameraVisible) {
					cameraImageView.setVisibility(View.INVISIBLE);
					cameraVisible = false;
				} else {
					cameraImageView.setVisibility(View.VISIBLE);
					cameraVisible = true;
				}
			}
		});
		distanceTextView.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
				// TODO Auto-generated method stub
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
				// TODO Auto-generated method stub

			}

			@Override
			public void afterTextChanged(Editable arg0) {
				if (arg0.length() > 0 && !arg0.equals(".") && !arg0.equals("")) {
					bgProcessing.setDistance(Double.parseDouble(arg0.toString()));
				}
			}
		});
		final EditText txtUrl = new EditText(this);
		distanceTextView.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View arg0) {

				return false;
			}
		});
		cameraImageView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				if (bgProcessing.isPause()) {
					bgProcessing.setPause(false);
				} else {
					bgProcessing.setPause(true);
				}
			}
		});
		btnSettings.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				settingsDialog.show();
			}
		});
		btnSettings.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View arg0, MotionEvent arg1) {
				if (btnSettings.isPressed()) {
					btnSettings.setImageResource(R.drawable.settings_big_clicked);
				} else {
					btnSettings.setImageResource(R.drawable.settings_big);
				}
				return false;
			}
		});
		radioGroup.setOnCheckedChangeListener(new android.widget.RadioGroup.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				switch (checkedId) {
				case R.id.btnAny:
					bgSub.setGray(true);
					bgSub.setColor(true);
					bgSub.setGrayOrColor(true);
					break;

				case R.id.btnBoth:
					bgSub.setGray(true);
					bgSub.setColor(true);
					bgSub.setGrayOrColor(false);
					break;

				case R.id.btnGray:
					bgSub.setGray(true);
					bgSub.setColor(false);
					break;
				case R.id.btnColor:
					bgSub.setColor(true);
					bgSub.setGray(false);
					break;
				}
			}
		});
		graySeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar arg0) {
				bgSub.setGrayThreshold(arg0.getProgress());
			}

			@Override
			public void onStartTrackingTouch(SeekBar arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
				grayTextView.setText(String.valueOf(arg1));
			}
		});
		colorSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar arg0) {
				bgSub.setColorThreshold(arg0.getProgress());
			}

			@Override
			public void onStartTrackingTouch(SeekBar arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
				colorTextView.setText(String.valueOf(arg1));
			}
		});

		minOMSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar arg0) {
				bgProcessing.setMinBlobSize(arg0.getProgress());
			}

			@Override
			public void onStartTrackingTouch(SeekBar arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
				minOMTextView.setText(String.valueOf(arg1));
			}
		});

		maxOMSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar arg0) {
				bgProcessing.setMaxBlobSize(arg0.getProgress());
			}

			@Override
			public void onStartTrackingTouch(SeekBar arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
				maxOMTextView.setText(String.valueOf(arg1));
			}
		});
		minSpeedSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar arg0) {
				bgProcessing.setMinSpeed(arg0.getProgress());
			}

			@Override
			public void onStartTrackingTouch(SeekBar arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
				minSpeedTextView.setText(String.valueOf(arg1));
			}
		});
		maxSpeedSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar arg0) {
				bgProcessing.setMaxSpeed(arg0.getProgress());
			}

			@Override
			public void onStartTrackingTouch(SeekBar arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
				maxSpeedTextView.setText(String.valueOf(arg1));
			}
		});

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
