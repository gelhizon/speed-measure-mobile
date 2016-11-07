package com.gel.speedmeasure;

import java.io.IOException;
import java.util.List;

import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.SurfaceHolder;

public class CameraPreview implements SurfaceHolder.Callback, Camera.PreviewCallback {
	private static final String TAG = null;

	private int previewWidth;
	private int previewHeight;
	private Camera mCamera;
	private BackgroundProcessing bgProcessing;
	private Handler mHandler;

	// Buffers
	private byte[] buffer;
	private byte[] buffer2;

	public CameraPreview(int previewWidth, int previewHeight, BackgroundProcessing bgProcessing) {
		this.previewWidth = previewWidth;
		this.previewHeight = previewHeight;
		this.bgProcessing = bgProcessing;
		mHandler = new Handler(Looper.getMainLooper());

		// Buffers
		buffer = new byte[(previewHeight * previewWidth) + ((previewHeight * previewWidth) / 2)];
		buffer2 = new byte[(previewHeight * previewWidth) + ((previewHeight * previewWidth) / 2)];
	}

	@Override
	public void onPreviewFrame(byte[] frame, Camera camera) {
		bgProcessing.setFrame(frame);
		mHandler.post(bgProcessing);
		camera.addCallbackBuffer(frame);
	}

	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
		Parameters parameters = mCamera.getParameters();
		parameters.setPreviewSize(previewWidth, previewHeight);

		List<int[]> fpsRange = parameters.getSupportedPreviewFpsRange();
		int range[] = new int[2];
		range[0] = 0;
		range[1] = 0;
		for (int[] g : fpsRange) {
			if (g[0] > range[0]) {
				range[0] = g[0];
			}
			if (g[1] > range[1]) {
				range[1] = g[1];
			}
		}

		parameters.setPreviewFpsRange(range[0], range[1]);
		// parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
		// if (parameters.isVideoStabilizationSupported()) {
		// parameters.setVideoStabilization(true);
		// }
		// if (parameters.isAutoExposureLockSupported()) {
		// parameters.setExposureCompensation(0);
		// parameters.setAutoExposureLock(true);
		// }
		// if (parameters.isAutoWhiteBalanceLockSupported()) {
		// parameters.setAutoWhiteBalanceLock(true);
		// }
		parameters.setRecordingHint(true);

		mCamera.setParameters(parameters);

		// try with one buffer
		mCamera.addCallbackBuffer(buffer);
		// mCamera.addCallbackBuffer(buffer2);
		mCamera.setPreviewCallbackWithBuffer(this);

		// mCamera.setPreviewCallback(this);
		mCamera.startPreview();
	}

	@Override
	public void surfaceCreated(SurfaceHolder mHolder) {
		mCamera = Camera.open();
		try {
			// If did not set the SurfaceHolder, the preview area will be black.
			mCamera.setPreviewDisplay(mHolder);

		} catch (IOException e) {
			// release camera resource
			mCamera.release();
			mCamera = null;
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder mHolder) {
		// mCamera.setPreviewCallback(null);
		// try with buffer
		mCamera.setPreviewCallbackWithBuffer(null);
		mCamera.stopPreview();
		mCamera.release();
		mCamera = null;
	}
}
