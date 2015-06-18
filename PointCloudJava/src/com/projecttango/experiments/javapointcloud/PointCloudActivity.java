/*
 * copyright 2014 google inc. all rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.projecttango.experiments.javapointcloud;

import java.nio.FloatBuffer;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.Tango.OnTangoUpdateListener;
import com.google.atap.tangoservice.TangoCameraIntrinsics;
import com.google.atap.tangoservice.TangoConfig;
import com.google.atap.tangoservice.TangoCoordinateFramePair;
import com.google.atap.tangoservice.TangoErrorException;
import com.google.atap.tangoservice.TangoEvent;
import com.google.atap.tangoservice.TangoInvalidException;
import com.google.atap.tangoservice.TangoOutOfDateException;
import com.google.atap.tangoservice.TangoPoseData;
import com.google.atap.tangoservice.TangoXyzIjData;

/**
 * Main Activity class for the Point Cloud Sample. Handles the connection to the {@link Tango}
 * service and propagation of Tango XyzIj data to OpenGL and Layout views. OpenGL rendering logic is
 * delegated to the {@link PCrenderer} class.
 */
public class PointCloudActivity extends Activity implements OnClickListener {

	private static final String TAG = PointCloudActivity.class.getSimpleName();
	private Tango mTango;
	private TangoConfig mConfig;

	private PCRenderer mRenderer;
	private GLSurfaceView mGLView;

	private int count;
	private int mPreviousPoseStatus;
	private int mPointCount;
	private float mDeltaTime;
	private float mPosePreviousTimeStamp;
	private float mXyIjPreviousTimeStamp;
	private float mCurrentTimeStamp;
	private float mPointCloudFrameDelta;
	private String mServiceVersion;
	private boolean mIsTangoServiceConnected;
	private TangoPoseData mPose;    
	public static Object poseLock = new Object();
	public static Object depthLock = new Object();
	private SharedData dataShared;
	private double camCx;
	private double camCy;
	private double camFx;
	private double camFy;
   
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_jpoint_cloud);
		setTitle(R.string.app_name);

		mTango = new Tango(this);
		mConfig = new TangoConfig();
		mConfig = mTango.getConfig(TangoConfig.CONFIG_TYPE_CURRENT);
		mConfig.putBoolean(TangoConfig.KEY_BOOLEAN_DEPTH, true);

		int maxDepthPoints = mConfig.getInt("max_point_cloud_elements");
		mRenderer = new PCRenderer(maxDepthPoints);
		dataShared = new SharedData(mRenderer.getModelMatCalculator());   //________________________
		mGLView = (GLSurfaceView) findViewById(R.id.gl_surface_view);
		mGLView.setEGLContextClientVersion(2);
		mGLView.setRenderer(mRenderer);

		/**
		camCx = camIntrins.cx;
		camCy = camIntrins.cy;
		camFx = camIntrins.fx;
		camFy = camIntrins.fy;
		
		System.out.println("camCx" + camCx);
		System.out.println("camCy" + camCy);
		System.out.println("camFx" + camFx);
		System.out.println("camFy" + camFy);
		
		System.out.println("width: " + camIntrins.width);
		dataShared.createCameraTransformMatrix(camCx, camCy, camFx, camFy);
		dataShared.setDistortion(camIntrins.distortion);
		dataShared.createScreen(camIntrins.width, camIntrins.height);
		*/

		PackageInfo packageInfo;
		try {
			packageInfo = this.getPackageManager().getPackageInfo(this.getPackageName(), 0);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}

		// Display the version of Tango Service
		mServiceVersion = mConfig.getString("tango_service_library_version");
		mIsTangoServiceConnected = false;
	}

	@Override
	protected void onPause() {
		super.onPause();
		try {
			mTango.disconnect();
			mIsTangoServiceConnected = false;
		} catch (TangoErrorException e) {;

			Toast.makeText(getApplicationContext(), R.string.TangoError, Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (!mIsTangoServiceConnected) {
			startActivityForResult(
					Tango.getRequestPermissionIntent(Tango.PERMISSIONTYPE_MOTION_TRACKING),
					Tango.TANGO_INTENT_ACTIVITYCODE);
		}
		Log.i(TAG, "onResumed");
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// Check which request we're responding to
		if (requestCode == Tango.TANGO_INTENT_ACTIVITYCODE) {
			Log.i(TAG, "Triggered");
			// Make sure the request was successful
			if (resultCode == RESULT_CANCELED) {
				Toast.makeText(this, R.string.motiontrackingpermission, Toast.LENGTH_LONG).show();
				finish();
				return;
			}
			try {
				setTangoListeners();
			} catch (TangoErrorException e) {
				Toast.makeText(this, R.string.TangoError, Toast.LENGTH_SHORT).show();
			} catch (SecurityException e) {
				Toast.makeText(getApplicationContext(), R.string.motiontrackingpermission,
						Toast.LENGTH_SHORT).show();
			}
			try {
				mTango.connect(mConfig);
				mIsTangoServiceConnected = true;
			} catch (TangoOutOfDateException e) {
				Toast.makeText(getApplicationContext(), R.string.TangoOutOfDateException,
						Toast.LENGTH_SHORT).show();
			} catch (TangoErrorException e) {;

				Toast.makeText(getApplicationContext(), R.string.TangoError, Toast.LENGTH_SHORT)
				.show();
			}
			setUpExtrinsics();
			setUpIntrinsics();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return mRenderer.onTouchEvent(event);
	}

	private void setUpIntrinsics() {

	TangoCameraIntrinsics camIntrins = mTango.getCameraIntrinsics(TangoCameraIntrinsics.TANGO_CAMERA_DEPTH);

		camCx = camIntrins.cx;
		camCy = camIntrins.cy;
		camFx = camIntrins.fx;
		camFy = camIntrins.fy;
		
		System.out.println("camCx" + camCx);
		System.out.println("camCy" + camCy);
		System.out.println("camFx" + camFx);
		System.out.println("camFy" + camFy);
		
		System.out.println("width: " + camIntrins.width);
		System.out.println("height: " + camIntrins.height);
		dataShared.createCameraTransformMatrix(camCx, camCy, camFx, camFy);
		dataShared.setDistortion(camIntrins.distortion);
		dataShared.createScreen(camIntrins.width, camIntrins.height);
	}

	private void setUpExtrinsics() {
		// Set device to imu matrix in Model Matrix Calculator.
		TangoPoseData device2IMUPose = new TangoPoseData();
		TangoCoordinateFramePair framePair = new TangoCoordinateFramePair();
		framePair.baseFrame = TangoPoseData.COORDINATE_FRAME_IMU;
		framePair.targetFrame = TangoPoseData.COORDINATE_FRAME_DEVICE;
		try {
			device2IMUPose = mTango.getPoseAtTime(0.0, framePair);
		} catch (TangoErrorException e) {
			Toast.makeText(getApplicationContext(), R.string.TangoError, Toast.LENGTH_SHORT).show();
		}
		mRenderer.getModelMatCalculator().SetDevice2IMUMatrix(
				device2IMUPose.getTranslationAsFloats(), device2IMUPose.getRotationAsFloats());

		// Set color camera to imu matrix in Model Matrix Calculator.
		TangoPoseData color2IMUPose = new TangoPoseData();

		framePair.baseFrame = TangoPoseData.COORDINATE_FRAME_IMU;
		framePair.targetFrame = TangoPoseData.COORDINATE_FRAME_CAMERA_COLOR;
		try {
			color2IMUPose = mTango.getPoseAtTime(0.0, framePair);
		} catch (TangoErrorException e) {
			Toast.makeText(getApplicationContext(), R.string.TangoError, Toast.LENGTH_SHORT).show();
		}
		mRenderer.getModelMatCalculator().SetColorCamera2IMUMatrix(
				color2IMUPose.getTranslationAsFloats(), color2IMUPose.getRotationAsFloats());
	}

	private void setTangoListeners() {
		// Configure the Tango coordinate frame pair
		final ArrayList<TangoCoordinateFramePair> framePairs = new ArrayList<TangoCoordinateFramePair>();
		framePairs.add(new TangoCoordinateFramePair(
				TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE,
				TangoPoseData.COORDINATE_FRAME_DEVICE));
		// Listen for new Tango data;

		mTango.connectListener(framePairs, new OnTangoUpdateListener() {

			@Override
			public void onPoseAvailable(final TangoPoseData pose) {
				// Make sure to have atomic access to Tango Pose Data so that
				// render loop doesn't interfere while Pose call back is updating
				// the data.
				synchronized (poseLock) {
					mPose = pose;
					// Calculate the delta time from previous pose.
					mDeltaTime = (float) (pose.timestamp - mPosePreviousTimeStamp)
							* 1000;
					mPosePreviousTimeStamp = (float) pose.timestamp;
					if (mPreviousPoseStatus != pose.statusCode) {
						count = 0;
					}
					count++;
					mPreviousPoseStatus = pose.statusCode;
					if(!mRenderer.isValid()){
						return;
					}
					mRenderer.getModelMatCalculator().updateModelMatrix(
							pose.getTranslationAsFloats(), pose.getRotationAsFloats());
					mRenderer.updateViewMatrix();
				}
			}

			@Override
			public void onXyzIjAvailable(final TangoXyzIjData xyzIj) {
				synchronized (depthLock) {
					mCurrentTimeStamp = (float) xyzIj.timestamp;
					mPointCloudFrameDelta = (mCurrentTimeStamp - mXyIjPreviousTimeStamp)
							* 1000;
					mXyIjPreviousTimeStamp = mCurrentTimeStamp;

					try {
						TangoPoseData pointCloudPose = mTango.getPoseAtTime(mCurrentTimeStamp,
								framePairs.get(0));

						mRenderer.getModelMatCalculator().updatePointCloudModelMatrix(
								pointCloudPose.getTranslationAsFloats(), 
								pointCloudPose.getRotationAsFloats());

						if(!mRenderer.isValid()){
							return;
						}
						
						mPointCount = xyzIj.xyzCount;
						dataShared.setPose(pointCloudPose); 
						dataShared.setXyzijCoord(xyzIj.xyz);
						dataShared.updateLastKPointClouds(1);

						mRenderer.getPointCloud().UpdatePoints(dataShared.getLastKPointClouds()); 
//						dataShared.convertTo2D(xyzIj.xyz);


					} catch (TangoErrorException e) {;
				   
						Toast.makeText(getApplicationContext(), R.string.TangoError,
								Toast.LENGTH_SHORT).show();
					} catch (TangoInvalidException e) {
						Toast.makeText(getApplicationContext(), R.string.TangoError,
								Toast.LENGTH_SHORT).show();
					}
				}
			};

			@Override
			public void onTangoEvent(final TangoEvent event) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
					}
				});
			}

			@Override
			public void onFrameAvailable(int arg0) { };
		});
	}

	public void reconstruct() {      
		new Thread(new Runnable() {
			@Override 
			public void run() {
				try {
					synchronized (depthLock) {
						FloatBuffer currentXyzIj = dataShared.getXyzijCoord();
						TangoPoseData currentPose = dataShared.getPose();
					}

					if ((dataShared.isXyzSameAsPrevious() || dataShared.isPoseSameAsPrevious()) ) {
						// TODO: merge currentXyzIf, currentPose into SDF
					}
				} catch (TangoErrorException e) {
					Toast.makeText(getApplicationContext(), R.string.TangoError,
							Toast.LENGTH_SHORT).show();
				}
			}
		}).start();
	}

	@Override
	public void onClick(View v) { }
}