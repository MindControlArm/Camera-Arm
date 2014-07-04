/*
 * Copyright 2013 Sony Corporation
 */

package com.example.sony.cameraremote;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.accessibleremote.AccessoryConnector;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * An Activity class of Sample Camera screen.
 */
public class SampleCameraActivity extends Activity {

    private static final String TAG = SampleCameraActivity.class
            .getSimpleName();


	private final int PAN_UP_START= 25;
	private final int PAN_UP_STOP= 26;
	private final int PAN_DOWN_START= 27;
	private final int PAN_DOWN_STOP= 28;
	private final int PAN_LEFT_START= 29;
	private final int PAN_LEFT_STOP= 30;
	private final int PAN_RIGHT_START= 31;
	private final int PAN_RIGHT_STOP= 32;

    
    // SHOULD BE REMOVE
    private int m_servoLR = 30;
    private int m_servoUD = 30;
    private final int m_max_servoUD = 60;
    private final int m_min_servoUD = 0;
    private final int m_max_servoLR = 60;
    private final int m_min_servoLR = 0;
    private final int CONSTANT_DEGREE_JUMP = 5;
	private boolean mIsBoundArduino = false;
	private AccessoryConnector mBoundServiceArduino;
	   
	private static final String ACTION_USB_PERMISSION = "com.curiousjason.accessiblekeyboard.action.USB_PERMISSION";

	private PendingIntent mPermissionIntent;
	private boolean mPermissionRequestPending;

	UsbManager mUsbManager;
	UsbAccessory mAccessory = null;
	public boolean accessoryOpen = false;
    	    
    private ImageView mImagePictureWipe;
    private Spinner mSpinnerShootMode;
    private Button mButtonTakePicture;
    private Button mButtonRecStartStop;
    
    private Button mButtonPanUp;
    private Button mButtonPanDown;
    private Button mButtonPanRight;
    private Button mButtonPanLeft;
    
    private Button mButtonZoomIn;
    private Button mButtonZoomOut;
    private TextView mTextCameraStatus;

    private ServerDevice mTargetServer;
    private SimpleRemoteApi mRemoteApi;
    private SimpleLiveviewSurfaceView mLiveviewSurface;
    private SimpleCameraEventObserver mEventObserver;
    private final Set<String> mAvailableApiSet = new HashSet<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_sample_camera);

        //Register to receive USB Accessory connected events
		mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
		mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(
				ACTION_USB_PERMISSION), 0);
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
		registerReceiver(mUsbReceiver, filter);
        
        SampleApplication app = (SampleApplication) getApplication();
        mTargetServer = app.getTargetServerDevice();
        mRemoteApi = new SimpleRemoteApi(mTargetServer);
        mEventObserver = new SimpleCameraEventObserver(this, mRemoteApi);

        
        mImagePictureWipe = (ImageView) findViewById(R.id.image_picture_wipe);
        mSpinnerShootMode = (Spinner) findViewById(R.id.spinner_shoot_mode);
        mButtonTakePicture = (Button) findViewById(R.id.button_take_picture);
        mButtonRecStartStop = (Button) findViewById(R.id.button_rec_start_stop);
        
        mButtonPanUp = (Button) findViewById(R.id.button_pan_up);
        mButtonPanDown = (Button) findViewById(R.id.button_pan_down);
        mButtonPanRight = (Button) findViewById(R.id.button_pan_right);
        mButtonPanLeft = (Button) findViewById(R.id.button_pan_left);
        
        mButtonZoomIn = (Button) findViewById(R.id.button_zoom_in);
        mButtonZoomOut = (Button) findViewById(R.id.button_zoom_out);
        mTextCameraStatus = (TextView) findViewById(R.id.text_camera_status);
        mLiveviewSurface = (SimpleLiveviewSurfaceView) findViewById(R.id.surfaceview_liveview);
        mLiveviewSurface.bindRemoteApi(mRemoteApi);

        mSpinnerShootMode.setEnabled(false);

        Log.d(TAG, "onCreate() completed.");
        
        // Create the service in charge for the connection to the arudino 
        doBindServiceArduino();
        
    }
    
//    public void pan_callback_left(View v)
//    {
//
//    	m_servoLR -= CONSTANT_DEGREE_JUMP;
//    	
//        if (m_servoLR <= m_min_servoLR)
//        	m_servoLR = m_min_servoLR;
//        	
//        mBoundServiceArduino.sendCommand(24, m_servoLR);
//    }
//    
//    public void pan_callback_right(View v)
//    {
//
//    	m_servoLR += CONSTANT_DEGREE_JUMP;
//    	
//    	if (m_servoLR >= m_max_servoLR)
//        	m_servoLR = m_max_servoLR;
//        	
//        mBoundServiceArduino.sendCommand(24, m_servoLR);
//
//    }
//    public void pan_callback_down(View v)
//    {
//
//    	m_servoUD -= CONSTANT_DEGREE_JUMP;
//    	
//        if (m_servoUD <= m_min_servoUD)
//        	m_servoUD = m_min_servoUD;
//        
//    	mBoundServiceArduino.sendCommand(23, m_servoUD);
//    }
//    public void pan_callback_up(View v)
//    {
//
//    	m_servoUD += CONSTANT_DEGREE_JUMP;
//    	
//        if (m_servoUD >= m_max_servoUD)
//        	m_servoUD = m_max_servoUD;
//        	
//        
//    	mBoundServiceArduino.sendCommand(23, m_servoUD);
//    }
    
    @Override
    protected void onDestroy() {
    	// TODO Auto-generated method stub
    	super.onDestroy();
    	Log.v(TAG,"onDestroy()");
    	unregisterReceiver(mUsbReceiver);
    	doUnbindServiceArduino();
    }

    @Override
    protected void onResume() {
        super.onResume();
        
		if (!mIsBoundArduino) 
		{
			Log.v(TAG, "onResume()");
			
			UsbAccessory[] accessories = mUsbManager.getAccessoryList();
			UsbAccessory accessory = (accessories == null ? null : accessories[0]);
			if (accessory != null) {
				if (mUsbManager.hasPermission(accessory)) {
					mAccessory = accessory;
					doBindServiceArduino();
				} else {
					synchronized (mUsbReceiver) {
						if (!mPermissionRequestPending) {
							mUsbManager.requestPermission(accessory,
									mPermissionIntent);
							mPermissionRequestPending = true;
						}
					}
				}
			} else {
				Log.d(TAG, "mAccessory is null");
			}

		}

   


        mSpinnerShootMode.setFocusable(false);

        mButtonTakePicture.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                takeAndFetchPicture();
            }
        });
        mButtonRecStartStop.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if ("MovieRecording".equals(mEventObserver.getCameraStatus())) {
                    stopMovieRec();
                } else if ("IDLE".equals(mEventObserver.getCameraStatus())) {
                    startMovieRec();
                }
            }
        });
        mEventObserver
                .setEventChangeListener(new SimpleCameraEventObserver.ChangeListener() {

                    @Override
                    public void onShootModeChanged(String shootMode) {
                        Log.d(TAG, "onShootModeChanged() called: " + shootMode);
                        refreshUi();
                    }

                    @Override
                    public void onCameraStatusChanged(String status) {
                        Log.d(TAG, "onCameraStatusChanged() called: " + status);
                        refreshUi();
                    }

                    @Override
                    public void onApiListModified(List<String> apis) {
                        Log.d(TAG, "onApiListModified() called");
                        synchronized (mAvailableApiSet) {
                            mAvailableApiSet.clear();
                            for (String api : apis) {
                                mAvailableApiSet.add(api);
                            }
                            if (!mEventObserver.getLiveviewStatus()
                                    && isApiAvailable("startLiveview")) {
                                if (!mLiveviewSurface.isStarted()) {
                                    mLiveviewSurface.start();
                                }
                            }
                            if (isApiAvailable("actZoom")) {
                                Log.d(TAG,
                                        "onApiListModified(): prepareActZoomButtons()");
                                prepareActZoomButtons(true);
                            } else {
                                prepareActZoomButtons(false);
                            }
                        }
                    }

                    @Override
                    public void onZoomPositionChanged(int zoomPosition) {
                        Log.d(TAG, "onZoomPositionChanged() called = " + zoomPosition);
                        if (zoomPosition == 0) {
                            mButtonZoomIn.setEnabled(true);
                            mButtonZoomOut.setEnabled(false);
                        } else if (zoomPosition == 100) {
                            mButtonZoomIn.setEnabled(false);
                            mButtonZoomOut.setEnabled(true);
                        } else {
                            mButtonZoomIn.setEnabled(true);
                            mButtonZoomOut.setEnabled(true);
                        }
                    }

                    @Override
                    public void onLiveviewStatusChanged(boolean status) {
                        Log.d(TAG, "onLiveviewStatusChanged() called = " + status);
                    }
                });
        mImagePictureWipe.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mImagePictureWipe.setVisibility(View.INVISIBLE);
            }
        });

        mButtonPanUp.setOnTouchListener(new View.OnTouchListener() {
			
            public boolean onTouch(View v, MotionEvent event) {

                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                	mBoundServiceArduino.sendCommand(PAN_UP_START, 0);
                } else if (event.getAction() == MotionEvent.ACTION_UP ||
                		   event.getAction() == MotionEvent.ACTION_CANCEL) {
                	mBoundServiceArduino.sendCommand(PAN_UP_STOP, 0);
                }
                
                return false;
            }
		});
        
        mButtonPanDown.setOnTouchListener(new View.OnTouchListener() {
			
            public boolean onTouch(View v, MotionEvent event) {

                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                	mBoundServiceArduino.sendCommand(PAN_DOWN_START, 0);
                } else if (event.getAction() == MotionEvent.ACTION_UP ||
                		   event.getAction() == MotionEvent.ACTION_CANCEL) {
                	mBoundServiceArduino.sendCommand(PAN_DOWN_STOP, 0);
                }
                
                return false;
            }
		});
        
        
        mButtonPanRight.setOnTouchListener(new View.OnTouchListener() {
			
            public boolean onTouch(View v, MotionEvent event) {

                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                	mBoundServiceArduino.sendCommand(PAN_RIGHT_START, 0);
                } else if (event.getAction() == MotionEvent.ACTION_UP ||
                		   event.getAction() == MotionEvent.ACTION_CANCEL) {
                	mBoundServiceArduino.sendCommand(PAN_RIGHT_STOP, 0);
                }
                
                return false;
            }
		});
        

        mButtonPanLeft.setOnTouchListener(new View.OnTouchListener() {
			
            public boolean onTouch(View v, MotionEvent event) {

                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                	mBoundServiceArduino.sendCommand(PAN_LEFT_START, 0);
                } else if (event.getAction() == MotionEvent.ACTION_UP ||
                		   event.getAction() == MotionEvent.ACTION_CANCEL) {
                	mBoundServiceArduino.sendCommand(PAN_LEFT_STOP, 0);
                }
                
                return false;
            }
		});
        
        
//        mButtonPanUp.setOnClickListener(new View.OnClickListener() {
//			
//			@Override
//			public void onClick(View v) {
//				Log.i(TAG, "Pan UP!!!");
//			}
//		});
        
        mButtonZoomIn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                actZoom("in", "1shot");
            }
        });

        mButtonZoomOut.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                actZoom("out", "1shot");
            }
        });

        mButtonZoomIn.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View arg0) {
                actZoom("in", "start");
                return true;
            }
        });

        mButtonZoomOut.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View arg0) {
                actZoom("out", "start");
                return true;
            }
        });

        mButtonZoomIn.setOnTouchListener(new View.OnTouchListener() {

            long downTime = -1;

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (System.currentTimeMillis() - downTime > 500) {
                        actZoom("in", "stop");
                    }
                }
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    downTime = System.currentTimeMillis();
                }
                return false;
            }
        });

        mButtonZoomOut.setOnTouchListener(new View.OnTouchListener() {

            long downTime = -1;

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (System.currentTimeMillis() - downTime > 500) {
                        actZoom("out", "stop");
                    }
                }
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    downTime = System.currentTimeMillis();
                }
                return false;
            }
        });

        openConnection();

        Log.d(TAG, "onResume() completed.");
    }

    @Override
    protected void onPause() {
        super.onPause();
        closeConnection();

        Log.d(TAG, "onPause() completed.");
    }

    // Open connection to the camera device to start monitoring Camera events
    // and showing liveview.
    private void openConnection() {
        setProgressBarIndeterminateVisibility(true);
        new Thread() {

            @Override
            public void run() {
                Log.d(TAG, "openConnection(): exec.");
                try {
                    JSONObject replyJson = null;

                    // getAvailableApiList
                    replyJson = mRemoteApi.getAvailableApiList();
                    loadAvailableApiList(replyJson);

                    // check version of the server device
                    if (isApiAvailable("getApplicationInfo")) {
                        Log.d(TAG, "openConnection(): getApplicationInfo()");
                        replyJson = mRemoteApi.getApplicationInfo();
                        if (!isSupportedServerVersion(replyJson)) {
                            toast(R.string.msg_error_non_supported_device);
                            SampleCameraActivity.this.finish();
                            return;
                        }
                    } else {
                        // never happens;
                        return;
                    }

                    // startRecMode if necessary.
                    if (isApiAvailable("startRecMode")) {
                        Log.d(TAG, "openConnection(): startRecMode()");
                        replyJson = mRemoteApi.startRecMode();

                        // Call again.
                        replyJson = mRemoteApi.getAvailableApiList();
                        loadAvailableApiList(replyJson);
                    }

                    // getEvent start
                    if (isApiAvailable("getEvent")) {
                        Log.d(TAG, "openConnection(): EventObserver.start()");
                        mEventObserver.start();
                    }

                    // Liveview start
                    if (isApiAvailable("startLiveview")) {
                        Log.d(TAG, "openConnection(): LiveviewSurface.start()");
                        mLiveviewSurface.start();
                    }

                    // prepare UIs
                    if (isApiAvailable("getAvailableShootMode")) {
                        Log.d(TAG,
                                "openConnection(): prepareShootModeSpinner()");
                        prepareShootModeSpinner();
                        // Note: hide progress bar on title after this calling.
                    }

                    // prepare UIs
                    if (isApiAvailable("actZoom")) {
                        Log.d(TAG,
                                "openConnection(): prepareActZoomButtons()");
                        prepareActZoomButtons(true);
                    } else {
                        prepareActZoomButtons(false);
                    }

                    Log.d(TAG, "openConnection(): completed.");
                } catch (IOException e) {
                    Log.w(TAG, "openConnection: IOException: " + e.getMessage());
                    setProgressIndicator(false);
                    toast(R.string.msg_error_connection);
                }
            }
        }.start();
    }

    // Close connection to stop monitoring Camera events and showing liveview.
    private void closeConnection() {
        new Thread() {

            @Override
            public void run() {
                Log.d(TAG, "closeConnection(): exec.");
                try {
                    // Liveview stop
                    Log.d(TAG, "closeConnection(): LiveviewSurface.stop()");
                    mLiveviewSurface.stop();

                    // getEvent stop
                    Log.d(TAG, "closeConnection(): EventObserver.stop()");
                    mEventObserver.stop();

                    // stopRecMode if necessary.
                    if (isApiAvailable("stopRecMode")) {
                        Log.d(TAG, "closeConnection(): stopRecMode()");
                        mRemoteApi.stopRecMode();
                    }

                    Log.d(TAG, "closeConnection(): completed.");
                } catch (IOException e) {
                    Log.w(TAG,
                            "closeConnection: IOException: " + e.getMessage());
                }
            }
        }.start();
    }

    // Refresh UI appearance along current "cameraStatus" and "shootMode".
    private void refreshUi() {
        String cameraStatus = mEventObserver.getCameraStatus();
        String shootMode = mEventObserver.getShootMode();

        // CameraStatus TextView
        mTextCameraStatus.setText(cameraStatus);

        // Recording Start/Stop Button
        if ("MovieRecording".equals(cameraStatus)) {
            mButtonRecStartStop.setEnabled(true);
            mButtonRecStartStop.setText(R.string.button_rec_stop);
        } else if ("IDLE".equals(cameraStatus) && "movie".equals(shootMode)) {
            mButtonRecStartStop.setEnabled(true);
            mButtonRecStartStop.setText(R.string.button_rec_start);
        } else {
            mButtonRecStartStop.setEnabled(false);
        }

        // Take picture Button
        if ("still".equals(shootMode) && "IDLE".equals(cameraStatus)) {
            mButtonTakePicture.setEnabled(true);
        } else {
            mButtonTakePicture.setEnabled(false);
        }

        // Picture wipe Image
        if (!"still".equals(shootMode)) {
            mImagePictureWipe.setVisibility(View.INVISIBLE);
        }

        // Shoot Mode Buttons
        if ("IDLE".equals(cameraStatus) || "MovieRecording".equals(cameraStatus)) {
            mSpinnerShootMode.setEnabled(true);
            selectionShootModeSpinner(mSpinnerShootMode, shootMode);
        } else {
            mSpinnerShootMode.setEnabled(false);
        }
    }

    // Retrieve a list of APIs that are available at present.
    private void loadAvailableApiList(JSONObject replyJson) {
        synchronized (mAvailableApiSet) {
            mAvailableApiSet.clear();
            try {
                JSONArray resultArrayJson = replyJson.getJSONArray("result");
                JSONArray apiListJson = resultArrayJson.getJSONArray(0);
                for (int i = 0; i < apiListJson.length(); i++) {
                    mAvailableApiSet.add(apiListJson.getString(i));
                }
            } catch (JSONException e) {
                Log.w(TAG, "loadAvailableApiList: JSON format error.");
            }
        }
    }

    // Check if the indicated API is available at present.
    private boolean isApiAvailable(String apiName) {
        boolean isAvailable = false;
        synchronized (mAvailableApiSet) {
            isAvailable = mAvailableApiSet.contains(apiName);
        }
        return isAvailable;
    }

    // Check if the version of the server is supported in this application.
    private boolean isSupportedServerVersion(JSONObject replyJson) {
        try {
            JSONArray resultArrayJson = replyJson.getJSONArray("result");
            String version = resultArrayJson.getString(1);
            String[] separated = version.split("\\.");
            int major = Integer.valueOf(separated[0]);
            if (2 <= major) {
                return true;
            }
        } catch (JSONException e) {
            Log.w(TAG, "isSupportedServerVersion: JSON format error.");
        } catch (NumberFormatException e) {
            Log.w(TAG, "isSupportedServerVersion: Number format error.");
        }
        return false;
    }

    // Check if the shoot mode is supported in this application.
    private boolean isSupportedShootMode(String mode) {
        if ("still".equals(mode) || "movie".equals(mode)) {
            return true;
        }
        return false;
    }

    // Prepare for Spinner to select "shootMode" by user.
    private void prepareShootModeSpinner() {
        new Thread() {

            @Override
            public void run() {
                Log.d(TAG, "prepareShootModeSpinner(): exec.");
                JSONObject replyJson = null;
                try {
                    replyJson = mRemoteApi.getAvailableShootMode();

                    JSONArray resultsObj = replyJson.getJSONArray("result");
                    final String currentMode = resultsObj.getString(0);
                    JSONArray availableModesJson = resultsObj.getJSONArray(1);
                    final ArrayList<String> availableModes = new ArrayList<String>();

                    for (int i = 0; i < availableModesJson.length(); i++) {
                        String mode = availableModesJson.getString(i);
                        if (!isSupportedShootMode(mode)) {
                            mode = "";
                        }
                        availableModes.add(mode);
                    }
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                           prepareShootModeSpinnerUi(
                                    availableModes.toArray(new String[0]),
                                    currentMode);
                            // Hide progress indeterminate on title bar.
                            setProgressBarIndeterminateVisibility(false);
                        }
                    });
                } catch (IOException e) {
                    Log.w(TAG, "prepareShootModeRadioButtons: IOException: "
                            + e.getMessage());
                } catch (JSONException e) {
                    Log.w(TAG,
                            "prepareShootModeRadioButtons: JSON format error.");
                }
            };
        }.start();
    }

    // Selection for Spinner UI of Shoot Mode.
    private void selectionShootModeSpinner(Spinner spinner, String mode) {
        if (!isSupportedShootMode(mode)) {
            mode = "";
        }
        @SuppressWarnings("unchecked")
        ArrayAdapter<String> adapter = (ArrayAdapter<String>)spinner.getAdapter();
        if (adapter != null) {
            mSpinnerShootMode.setSelection(adapter.getPosition(mode));
        }
    }

    // Prepare for Spinner UI of Shoot Mode.
    private void prepareShootModeSpinnerUi(String[] availableShootModes,
            String currentMode) {

        ArrayAdapter<String> adapter
            = new ArrayAdapter<String>(this,
                    android.R.layout.simple_spinner_item, availableShootModes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerShootMode.setAdapter(adapter);
        mSpinnerShootMode.setPrompt(getString(R.string.prompt_shoot_mode));
        selectionShootModeSpinner(mSpinnerShootMode, currentMode);
        mSpinnerShootMode.setOnItemSelectedListener(new OnItemSelectedListener(){
            // selected Spinner dropdown item
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Spinner spinner = (Spinner)parent;
                if (spinner.isFocusable() == false) {
                    // ignored the first call, because shoot mode has not changed
                    spinner.setFocusable(true);
                } else {
                    String mode = spinner.getSelectedItem().toString();
                    String currentMode = mEventObserver.getShootMode();
                    if (mode.isEmpty()) {
                        toast(R.string.msg_error_no_supported_shootmode);
                        // now state that can not be changed
                        selectionShootModeSpinner(spinner, currentMode);
                    } else {
                        if ("IDLE".equals(mEventObserver.getCameraStatus())
                                && !mode.equals(currentMode)) {
                            setShootMode(mode);
                        } else {
                            // now state that can not be changed
                            selectionShootModeSpinner(spinner, currentMode);
                        }
                    }
                }
            }
            // not selected Spinner dropdown item
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

    }

    // Prepare for Button to select "actZoom" by user.
    private void prepareActZoomButtons(final boolean flag) {
        new Thread() {

            @Override
            public void run() {
                Log.d(TAG, "prepareActZoomButtons(): exec.");
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        prepareActZoomButtonsUi(flag);
                    }
                });
            };
        }.start();
    }

    // Prepare for ActZoom Button UI.
    private void prepareActZoomButtonsUi(boolean flag) {
        if (flag) {
            mButtonZoomOut.setVisibility(View.VISIBLE);
            mButtonZoomIn.setVisibility(View.VISIBLE);
        } else {
            mButtonZoomOut.setVisibility(View.GONE);
            mButtonZoomIn.setVisibility(View.GONE);
        }
    }

    // Call setShootMode
    private void setShootMode(final String mode) {
        new Thread() {

            @Override
            public void run() {
                try {
                    JSONObject replyJson = mRemoteApi.setShootMode(mode);
                    JSONArray resultsObj = replyJson.getJSONArray("result");
                    int resultCode = resultsObj.getInt(0);
                    if (resultCode == 0) {
                        // Success, but no refresh UI at the point.
                    } else {
                        Log.w(TAG, "setShootMode: error: " + resultCode);
                        toast(R.string.msg_error_api_calling);
                    }
                } catch (IOException e) {
                    Log.w(TAG, "setShootMode: IOException: " + e.getMessage());
                } catch (JSONException e) {
                    Log.w(TAG, "setShootMode: JSON format error.");
                }
            }
        }.start();
    }

    // Take a picture and retrieve the image data.
    private void takeAndFetchPicture() {
        if (!mLiveviewSurface.isStarted()) {
            toast(R.string.msg_error_take_picture);
            return;
        }

        new Thread() {

            @Override
            public void run() {
                try {
                    JSONObject replyJson = mRemoteApi.actTakePicture();
                    JSONArray resultsObj = replyJson.getJSONArray("result");
                    JSONArray imageUrlsObj = resultsObj.getJSONArray(0);
                    String postImageUrl = null;
                    if (1 <= imageUrlsObj.length()) {
                        postImageUrl = imageUrlsObj.getString(0);
                    }
                    if (postImageUrl == null) {
                        Log.w(TAG,
                                "takeAndFetchPicture: post image URL is null.");
                        toast(R.string.msg_error_take_picture);
                        return;
                    }
                    setProgressIndicator(true); // Show progress indicator
                    URL url = new URL(postImageUrl);
                    InputStream istream = new BufferedInputStream(
                            url.openStream());
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 4; // irresponsible value
                    final Drawable pictureDrawable = new BitmapDrawable(
                            getResources(), BitmapFactory.decodeStream(istream,
                                    null, options));
                    istream.close();
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            mImagePictureWipe.setVisibility(View.VISIBLE);
                            mImagePictureWipe.setImageDrawable(pictureDrawable);
                        }
                    });

                } catch (IOException e) {
                    Log.w(TAG, "IOException while closing slicer: " + e.getMessage());
                    toast(R.string.msg_error_take_picture);
                } catch (JSONException e) {
                    Log.w(TAG, "JSONException while closing slicer");
                    toast(R.string.msg_error_take_picture);
                } finally {
                    setProgressIndicator(false);
                }
            }
        }.start();
    }

    // Call startMovieRec
    private void startMovieRec() {
        new Thread() {

            @Override
            public void run() {
                try {
                    Log.d(TAG, "startMovieRec: exec.");
                    JSONObject replyJson = mRemoteApi.startMovieRec();
                    JSONArray resultsObj = replyJson.getJSONArray("result");
                    int resultCode = resultsObj.getInt(0);
                    if (resultCode == 0) {
                        toast(R.string.msg_rec_start);
                    } else {
                        Log.w(TAG, "startMovieRec: error: " + resultCode);
                        toast(R.string.msg_error_api_calling);
                    }
                } catch (IOException e) {
                    Log.w(TAG, "startMovieRec: IOException: " + e.getMessage());
                } catch (JSONException e) {
                    Log.w(TAG, "startMovieRec: JSON format error.");
                }
            }
        }.start();
    }

    // Call stopMovieRec
    private void stopMovieRec() {
        new Thread() {

            @Override
            public void run() {
                try {
                    Log.d(TAG, "stopMovieRec: exec.");
                    JSONObject replyJson = mRemoteApi.stopMovieRec();
                    JSONArray resultsObj = replyJson.getJSONArray("result");
                    String thumbnailUrl = resultsObj.getString(0);
                    if (thumbnailUrl != null) {
                        toast(R.string.msg_rec_stop);
                    } else {
                        Log.w(TAG, "stopMovieRec: error");
                        toast(R.string.msg_error_api_calling);
                    }
                } catch (IOException e) {
                    Log.w(TAG, "stopMovieRec: IOException: " + e.getMessage());
                } catch (JSONException e) {
                    Log.w(TAG, "stopMovieRec: JSON format error.");
                }
            }
        }.start();
    }

    // Call actZoom
    private void actZoom(final String direction, final String movement) {
        new Thread() {

            @Override
            public void run() {
                try {
                    JSONObject replyJson = mRemoteApi.actZoom(direction, movement);
                    JSONArray resultsObj = replyJson.getJSONArray("result");
                    int resultCode = resultsObj.getInt(0);
                    if (resultCode == 0) {
                        // Success, but no refresh UI at the point.
                    } else {
                        Log.w(TAG, "actZoom: error: " + resultCode);
                        toast(R.string.msg_error_api_calling);
                    }
                } catch (IOException e) {
                    Log.w(TAG, "actZoom: IOException: " + e.getMessage());
                } catch (JSONException e) {
                    Log.w(TAG, "actZoom: JSON format error.");
                }
            }
        }.start();
    }

    // Show or hide progress indicator on title bar
    private void setProgressIndicator(final boolean visible) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                setProgressBarIndeterminateVisibility(visible);
            }
        });
    }

    // show toast
    private void toast(final int msgId) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                Toast.makeText(SampleCameraActivity.this, msgId,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    
	
	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			Log.d(TAG,"in BroadcastReceiver, about to open accessory");
			if (UsbManager.ACTION_USB_ACCESSORY_ATTACHED.equals(action)) {
				synchronized (this) {
					mAccessory = (UsbAccessory)intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
					if (mAccessory!=null && intent.getBooleanExtra(
							UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
						//openAccessory(mAccessory);
			        	//mBoundServiceArduino.openAccessory(mAccessory);
						Log.d(TAG,"in BroadcastReceiver, opened accessory");
					} else {
						Log.d(TAG, "permission denied for accessory " + mAccessory);
					}
					//mPermissionRequestPending = false;
				}
			} else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
				UsbAccessory accessory = (UsbAccessory)intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
				if (accessory != null && accessory.equals(mAccessory)) {
					mBoundServiceArduino.closeAccessory();
					accessoryOpen = false;
				}
			}
		}
	};
	
	// Pass the work of opening the accessory to the service
	//private void openAccessory(UsbAccessory accessory) {
	//	if (mIsBoundArduino)
	//	else
	//		doBindServiceArduino();
	//}

    void doBindServiceArduino() {
		// Establish a connection with the service for communicating with the arduino.
		// We use an explicit class name because we want a specific service implementation that
		// we know will be running in our own process (and thus won't be
		// supporting component replacement by other applications).
		Log.v(TAG,"About to bind arduino service");
		bindService(new Intent(SampleCameraActivity.this, 
				AccessoryConnector.class), mConnectionArduino, Context.BIND_AUTO_CREATE);
//		mIsBoundArduino = true;
	}

	void doUnbindServiceArduino() {
		if (mIsBoundArduino) {
			// Detach our existing connection.
			unbindService(mConnectionArduino);
			Log.v(TAG,"doUnbindServiceArduino");
			mIsBoundArduino = false;
		}
	}

	private ServiceConnection mConnectionArduino = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service.  Because we have bound to a explicit
			// service that we know is running in our own process, we can
			// cast its IBinder to a concrete class and directly access it.
			mBoundServiceArduino = ((AccessoryConnector.LocalBinder)service).getService();
			Log.v(TAG,"Arduino service is connected");
			
//			mBoundServiceArduino.sendCommand(0x23, 1);
			
			Log.v(TAG,"onServiceConnected() : accessoryOpen ");
			if(!accessoryOpen && mAccessory != null)
			{
				Log.v(TAG,"onServiceConnected() : enters if.. ");
			//openAccessory(mAccessory);
	        	mBoundServiceArduino.openAccessory(mAccessory);

			}
			
		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			// Because it is running in our same process, we should never
			// see this happen.
			mBoundServiceArduino = null;
		}
	};



}
