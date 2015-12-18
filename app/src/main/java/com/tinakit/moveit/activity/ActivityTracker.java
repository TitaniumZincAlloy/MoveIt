package com.tinakit.moveit.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.tinakit.moveit.db.FitnessDBHelper;
import com.tinakit.moveit.fragment.ActivityChooser;
import com.tinakit.moveit.fragment.ActivityHistory;
import com.tinakit.moveit.fragment.MapFragment;
import com.tinakit.moveit.model.ActivityDetail;
import com.tinakit.moveit.model.UnitSplit;
import com.tinakit.moveit.model.User;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.tinakit.moveit.R;
import com.tinakit.moveit.model.UserActivity;
import com.tinakit.moveit.api.Accelerometer;
import com.tinakit.moveit.api.GoogleApi;
import com.tinakit.moveit.api.LocationApi;
import com.tinakit.moveit.utility.CalorieCalculator;
import com.tinakit.moveit.utility.ChronometerUtility;
import com.tinakit.moveit.utility.DialogUtility;
import com.tinakit.moveit.utility.UnitConverter;

public class ActivityTracker extends AppCompatActivity {

    //DEBUG
    private static final String LOG = "ACTIVITY_TRACKER";
    private static final boolean DEBUG = true;

    //CONSTANTS
    public static final String ACTIVITY_TRACKER_BROADCAST_RECEIVER = "TRACKER_RECEIVER";
    public static final String ACTIVITY_TRACKER_STARTED = "ACTIVITY_TRACKER_STARTED";
    public static final String ACTIVITY_TRACKER_INTENT = "ACTIVITY_TRACKER_INTENT";

    private static final float FEET_COIN_CONVERSION = 1.0f;//0.05f;  //20 feet = 1 coin
    private static long STOP_SERVICE_TIME_LIMIT = 30 * 60 * 1000 * 60; // 30 minutes in seconds

    //save all location points during location updates
    private List<UnitSplit> mUnitSplitList = new ArrayList<>();
    private int mTotalPoints = 0;
    protected static boolean mRequestedService = false;
    private long mTimeElapsed = 0; //in seconds
    private boolean mIsTimeLimit = false;
    private Intent mIntent;

    // APIs
    private LocationApi mLocationApi;
    private Accelerometer mAccelerometer;
    private MapFragment mMapFragment;

    //GOOGLE PLAY SERVICES
    private GoogleApi mGoogleApi;
    // Request code to use when launching the resolution activity
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    private boolean mResolvingError = false;
    private static final String STATE_RESOLVING_ERROR = "resolving_error";

    //UI widgets
    protected LinearLayout mCounterLayout;
    private static Button mStartButton;
    private static Button mStopButton;
    private static Button mPauseButton;
    private static Button mSaveButton;
    private static Button mResumeButton;
    private static Button mCancelButton;
    private static LinearLayout mButtonLinearLayout;
    private static Chronometer mChronometer;
    private static ChronometerUtility mChronometerUtility;
    private TextView mDistance;
    private TextView mCoins;
    private TextView mFeetPerMinute;
    private TextView mMessage;
    private ViewGroup mContainer;

    // INSTANCE FIELDS
    private ActivityDetail mActivityDetail;
    private long mTimeWhenStopped;
    private boolean mSaveLocationData = false;
    private boolean mHasMapFragment = false;

    //database
    FitnessDBHelper mDatabaseHelper;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        if (DEBUG) Log.d(LOG, "onCreateView()");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracker);
        setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // check whether previous screen was ActivityChooser
        mIntent = getIntent();
        if (mIntent.hasExtra(ActivityChooser.USER_ACTIVITY_LIST)) {

            initializeUI();

            initializeData();

            bindApi(savedInstanceState);
        }

    }

    protected void bindApi(@Nullable Bundle savedInstanceState ){

        //end the activity if Google Play Services is not present
        //redirect user to Google Play Services
        mGoogleApi = new GoogleApi(this);

        if (!mGoogleApi.servicesAvailable())
            finish();
        else
            mGoogleApi.buildGoogleApiClient();

        // location listener
        mLocationApi = new LocationApi(this, mGoogleApi.client());
        mLocationApi.initialize();

        // accelerometer
        mAccelerometer = new Accelerometer(this);

        //check savedInstanceState not null
        mResolvingError = savedInstanceState != null
                && savedInstanceState.getBoolean(STATE_RESOLVING_ERROR, false);
    }

    protected void initializeData(){

        //get databaseHelper instance
        mDatabaseHelper = FitnessDBHelper.getInstance(this);

        // get user list from intent
        mActivityDetail = new ActivityDetail();
        ArrayList<UserActivity> userActivityList = getIntent().getParcelableArrayListExtra(ActivityChooser.USER_ACTIVITY_LIST);
        mActivityDetail.setUserActivityList(userActivityList);

    }

    protected void initializeUI(){

        mCounterLayout = (LinearLayout)findViewById(R.id.counterLayout);
        mStartButton = (Button) findViewById(R.id.startButton);
        mStopButton = (Button) findViewById(R.id.stopButton);
        mPauseButton = (Button) findViewById(R.id.pauseButton);
        mSaveButton = (Button) findViewById(R.id.saveButton);
        mResumeButton = (Button) findViewById(R.id.resumeButton);
        mCancelButton = (Button)findViewById(R.id.cancelButton);
        mButtonLinearLayout = (LinearLayout)findViewById(R.id.buttonLayout);
        mChronometer = (Chronometer) findViewById(R.id.chronometer);
        mDistance = (TextView) findViewById(R.id.distance);
        mCoins = (TextView) findViewById(R.id.coins);
        mFeetPerMinute = (TextView) findViewById(R.id.feetPerMinute);
        mMessage = (TextView) findViewById(R.id.message);

        setButtonOnClickListeners();
    }

    //**********************************************************************************************
    //  setButtonOnClickListeners
    //**********************************************************************************************

    protected void setButtonOnClickListeners(){

        mStartButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                // close MainActivity so ActivityChooser values are not saved
                // once tracking is saved or cancelled, MainActivity will be called
                // send message to indicate there is new location data
                Intent intent = new Intent(ACTIVITY_TRACKER_INTENT);
                intent.putExtra(MainActivity.MAIN_ACTIVITY_BROADCAST_RECEIVER, ACTIVITY_TRACKER_INTENT);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);

                startRun();

                //set flag to save location data
                mSaveLocationData = true;

                //get timestamp of start
                mActivityDetail.setStartDate(new Date());

                //set visibility
                mMapFragment.setVisibility(View.GONE);

                //Restart
                mCancelButton.setVisibility(View.GONE);

                //clear out error message
                mMessage.setText("");

                mStartButton.setVisibility(View.GONE);
                mStopButton.setVisibility(View.VISIBLE);
                mPauseButton.setVisibility(View.VISIBLE);

            }
        });

        mStopButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {

                stopRun();

                //get timestamp of end
                mActivityDetail.setEndDate(new Date());

                //set button visibility
                mStopButton.setVisibility(View.GONE);
                mPauseButton.setVisibility(View.GONE);
                mResumeButton.setVisibility(View.GONE);

                //save Activity Detail data
                if (mUnitSplitList.size() > 1) {

                    mCancelButton.setVisibility(View.VISIBLE);
                    mSaveButton.setVisibility(View.VISIBLE);

                    //display number of coins
                    displayResults();

                } else {

                    //not enough data
                    mStartButton.setVisibility(View.VISIBLE);
                    mStartButton.setText(getResources().getString(R.string.restart));

                    mCancelButton.setVisibility(View.VISIBLE);

                    //message:  no data to display
                    mMessage.setText("No location data was collected. " + getResources().getString(R.string.restart) + "?");
                    playSound();

                }
            }
        });

        mPauseButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {

                pauseTracking();

                //set button visibility
                mPauseButton.setVisibility(View.GONE);
                mResumeButton.setVisibility(View.VISIBLE);
                mStopButton.setVisibility(View.VISIBLE);
                mCancelButton.setVisibility(View.GONE);

            }
        });

        mSaveButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {

                finishTracking(getString(R.string.activity_saved));

                //save activity data to database on separate background thread
                new SaveToDB().run();


            }

        });

        mResumeButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {

                resumeTracking();

                //set button visibility
                mResumeButton.setVisibility(View.GONE);
                mStopButton.setVisibility(View.VISIBLE);
                mPauseButton.setVisibility(View.VISIBLE);

            }
        });

        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                finishTracking(getString(R.string.activity_cancelled));
            }
        });
    }

    private void pauseTracking(){

        //set the flag to not save location data
        mSaveLocationData = false;

        //stop timer
        mChronometerUtility.stop();

        //save current time
        mTimeWhenStopped = mChronometerUtility.getTime() - SystemClock.elapsedRealtime();
    }

    private void resumeTracking(){

        //set flag to save location data
        mSaveLocationData = true;

        //start accelerometer listener, after a delay of ACCELEROMETER_DELAY
        mAccelerometer.start();

        //chronometer settings, set base time to time when paused ChronometerUtility.elapsedTime()
        mChronometerUtility.resume(mTimeWhenStopped);
    }

    private void finishTracking(String message){

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        finish();
        startActivity(new Intent(this, MainActivity.class));
    }

    //**********************************************************************************************
    //  Control methods
    //**********************************************************************************************

    private void startRun(){

        mRequestedService = true;

        //startServices(mGoogleApi.client());
        mLocationApi.start();
        mAccelerometer.start();

        //register api intents with BroadcastReceiver
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(LocationApi.LOCATION_API_INTENT));
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(Accelerometer.ACCELEROMETER_INTENT));

        //initialize ChronometerUtility, start timer
        mChronometerUtility = new ChronometerUtility (mChronometer);
        mChronometerUtility.start();

        //display counters
        mCounterLayout.setVisibility(View.VISIBLE);

        //TODO:  START STEP COUNTING UNTIL FIRST LOCATION API CONNECTION IS MADE AND PERIODS OF LOST CONNECTION

        //display map of starting point
        mMapFragment.displayStartMap();

    }

    private void stopRun(){

        //stopServices(mGoogleApi.client());
        mLocationApi.stop();
        mAccelerometer.stop();

        // unregister intents with BroadcastReceiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);

        //stop chronometer
        mChronometerUtility.stop();

        //save elapsed time
        mTimeElapsed = Math.round(mChronometerUtility.getTimeByUnits(mChronometer.getText().toString(), 0));

    }

    private void playSound(){

        MediaPlayer mp;
        mp = MediaPlayer.create(this, R.raw.cat_meow);
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.reset();
                mp.release();
                mp=null;
            }

        });
        mp.start();
    }

    //**********************************************************************************************
    //  Data methods
    //**********************************************************************************************

    private void saveToDB(FitnessDBHelper databaseHelper, List<UnitSplit> unitSplitList, ActivityDetail activityDetail, int totalPoints, float distance){

        //save Activity Detail (overall stats)
        long activityId = databaseHelper.insertActivity((float) unitSplitList.get(0).getLocation().getLatitude()
                , (float) unitSplitList.get(0).getLocation().getLongitude()
                , activityDetail.getStartDate()
                , activityDetail.getEndDate()
                , distance
                , unitSplitList.size() > 1 ? unitSplitList.get(0).getBearing() : 0);

        if (activityId != -1){

            //track participants for this activity: save userIds for this activityId
            int rowsAffected = databaseHelper.insertActivityUsers(activityId, activityDetail.getUserActivityList());

            for ( int i = 0; i < unitSplitList.size(); i++) {

                databaseHelper.insertActivityLocationData(activityId
                        , activityDetail.getStartDate()
                        , unitSplitList.get(i).getLocation().getLatitude()
                        , unitSplitList.get(i).getLocation().getLongitude()
                        , unitSplitList.get(i).getLocation().getAltitude()
                        , unitSplitList.get(i).getLocation().getAccuracy()
                        , unitSplitList.get(i).getBearing()
                        , unitSplitList.get(i).getSpeed());
            }
        }

        //update points for each user
        for (UserActivity userActivity : activityDetail.getUserActivityList()){

            User user = userActivity.getUser();
            user.setPoints(totalPoints + user.getPoints());
            databaseHelper.updateUser(user);
        }

    }

    //**********************************************************************************************
    //  updateCache()   /* saves data to cache*/
    //**********************************************************************************************

    private void  updateCache(Location location) {
        if (DEBUG) Log.d(LOG, "updateCache()");

        UnitSplit unitSplit = new UnitSplit(location);

        mUnitSplitList.add(unitSplit);

        //save time elapsed
        //get time from Chronometer
        mTimeElapsed = Math.round(mChronometerUtility.getTimeByUnits(mChronometer.getText().toString(), 0));
    }


    private void displayResults(){

        mMapFragment.displayMap(mUnitSplitList, getDistance(1));

        //TODO:  why does sound get truncated?
        playSound();

    }

    //**********************************************************************************************
    //  onStart()
    //**********************************************************************************************

    @Override
    public void onStart() {
        if (DEBUG) Log.d(LOG, "onStart");
        super.onStart();

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(GoogleApi.GOOGLE_API_INTENT));
    }

    //**********************************************************************************************
    //  onStop()
    //**********************************************************************************************
    @Override
    public void onStop() {
        if (DEBUG) Log.d(LOG, "onStop");
        super.onStop();
    }

    //**********************************************************************************************
    //  BroadcastReceiver mMessageReceiver
    //**********************************************************************************************

    // Handler for received Intents. This will be called whenever an Intent
    // with an action named GOOGLE_API_INTENT is broadcasted.
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra(ACTIVITY_TRACKER_BROADCAST_RECEIVER);

            if (DEBUG) Log.d(LOG, "BroadcastReceiver - onReceive(): message: " + message);

            // message to indicate Google API Client connection
            if(message.equals(GoogleApi.GOOGLE_API_INTENT)){

                // only show Start button after connecting to Google Api Client
                mStartButton.setVisibility(View.VISIBLE);

                //add map once
                if (mHasMapFragment == false){
                    mMapFragment = new MapFragment(getSupportFragmentManager(), mGoogleApi);
                    mMapFragment.addMap(mContainer);
                    mHasMapFragment = true;
                }

            }
            else if (message.equals(LocationApi.LOCATION_API_INTENT)){

                //only track data when it has high level of accuracy && not Pause mode
                if (mSaveLocationData){
                    //update cache
                    updateCache(mLocationApi.location());

                    refreshData();
                }

                //TODO: do we still want a time limit?
                if(mChronometerUtility.getTimeByUnits(mChronometer.getText().toString(), 0) > STOP_SERVICE_TIME_LIMIT && !mIsTimeLimit){
                    mIsTimeLimit = true;
                    reachedTimeLimit();
                    stopRun();
                }
            }
            else if (message.equals(Accelerometer.ACCELEROMETER_INTENT)){

                playSound();

                pauseTracking();

                //disable accelerometer listener
                mAccelerometer.stop();

                //display warning message that no movement has been detected
                DialogUtility.displayAlertDialog(context, getResources().getString(R.string.warning), getResources().getString(R.string.no_movement), getResources().getString(R.string.ok));
            }
        }
    };

    //**********************************************************************************************
    //  onPause() - Activity is partially obscured by another app but still partially visible and not the activity in focus
    //**********************************************************************************************

    @Override
    public void onPause() {
        if (DEBUG) Log.d(LOG, "onPause");

        //maintain Location tracking, we want to continue to collect location data until user clicks Stop button
        //other apps may run concurrently, such as music player
        super.onPause();
    }

    //**********************************************************************************************
    //  onResume()
    //**********************************************************************************************

    @Override
    public void onResume() {
        if (DEBUG) Log.d(LOG, "onResume");
        super.onResume();

        //ensures that if the user returns to the running app through some other means,
        //such as through the back button, the check is still performed.
        //end the activity if Google Play Services is not present
        //redirect user to Google Play Services
        if (!mGoogleApi.servicesAvailable()) {
            finish();
        }

    }

    private void refreshData(){


        mTimeElapsed = Math.round(mChronometerUtility.getTimeByUnits(mChronometer.getText().toString(), 0));

        //save location data in mLocationList
        displayCurrent();
    }

    //**********************************************************************************************
    //  onDestroy()
    //**********************************************************************************************

    @Override
    public void onDestroy() {
        if (DEBUG) Log.d(LOG, "onDestroy");
        super.onDestroy();

        mAccelerometer.stop();
    }

    //**********************************************************************************************
    //  Message methods
    //**********************************************************************************************

    private void reachedTimeLimit(){

        displayAlertDialog(getString(R.string.time_limit), getString(R.string.reached_time_limit_30_minutes));
        stopRun();

        //display number of coins earned
        displayResults();
    }

    private void displayAlertDialog(String title, String message){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set title
        alertDialogBuilder.setTitle(title);

        // set dialog message
        alertDialogBuilder
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        // if this button is clicked, close
                        // current activity
                    }
                })
                ;

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();

    }

    private void displayCurrent(){

        //TODO: replace references of mLocationList with mLocationTimeList
        //if (DEBUG) Log.d(LOG, "displayCurrent: intervalCount" + mLocationList.size());
        if (DEBUG) Log.d(LOG, "displayCurrent: intervalCount" + mUnitSplitList.size());

        if (mUnitSplitList.size() > 1){

            //update distance textview
            float distanceFeet = getDistance(1);
            mDistance.setText(String.format("%d", (int)distanceFeet));

            //update distance in cache
            mActivityDetail.setDistanceInFeet(Math.round(distanceFeet));

            //update speed feet/minute
            //float elapsedMinutes = (float)(SystemClock.elapsedRealtime() - mChronometer.getBase())/(1000 * 60);
            float elapsedMinutes = mChronometerUtility.getTimeByUnits(mChronometer.getText().toString(), 1);
            float speed = distanceFeet/elapsedMinutes;
            if (speed > 0)
                mFeetPerMinute.setText(String.format("%.0f", speed));
            else
                mFeetPerMinute.setText("0.0");

            //TODO: move this somewhere else, where business rules are updated, not UI update
            //update the UnitSplitCalorie list with calorie and speed values
            refreshUnitSplitAndTotalCalorie();

            //number of coins earned based on distance traveled
            int totalPoints =  Math.round(mActivityDetail.getDistanceInFeet() * FEET_COIN_CONVERSION);

            //compare previous totalCoins to current one
            //TODO:  for now, the points earned is based on distance, so each user will earn the same amount of coins
            //for now, use points earned of first user to represent the points earned for each user
            float delta = totalPoints - mTotalPoints;

            if(delta > 0 ){
                playSound();
            }

            //update coins
            mCoins.setText(String.format("%d", totalPoints));

            //save latest total number of coins
           mTotalPoints = totalPoints;

        }
    }


    private void refreshUnitSplitAndTotalCalorie(){

        //TODO: how to handle the first split, first data point is captured up to 4 seconds after the run starts.

        for ( int i = 0 ; i < mUnitSplitList.size() - 1; i++ ){

            float minutesElapsed = (mUnitSplitList.get(i+1).getLocation().getTime() - mUnitSplitList.get(i).getLocation().getTime()) / (1000f * 60f) ;
            float miles = UnitConverter.convertMetersToMiles(mUnitSplitList.get(i + 1).getLocation().distanceTo(mUnitSplitList.get(i).getLocation()));
            float hoursElapsed = minutesElapsed/60f;
            float milesPerHour = miles / hoursElapsed;

            //calculate calorie for each participant for their specific activity
            //update their total calorie count and points for this activity
            for (int j = 0; j < mActivityDetail.getUserActivityList().size(); j++){

                User user = mActivityDetail.getUserActivityList().get(j).getUser();
                float currentCalorie = mActivityDetail.getUserActivityList().get(j).getCalories();
                mActivityDetail.getUserActivityList().get(j).setCalories(currentCalorie + getCalorieByActivity(user.getWeight(), minutesElapsed, milesPerHour, mActivityDetail.getUserActivityList().get(j).getActivityType().getActivityTypeId()));
                mActivityDetail.getUserActivityList().get(j).setPoints(mTotalPoints);
            }
            //calculate bearing
            float bearing = mUnitSplitList.get(i).getLocation().bearingTo(mUnitSplitList.get(i+1).getLocation());

            //save calorie, speed, bearing in list
            //mUnitSplitCalorieList.get(i).setCalories(calorie);
            mUnitSplitList.get(i).setSpeed(milesPerHour);
            mUnitSplitList.get(i).setBearing(bearing);

        }
    }



    private float getDistance(int units){

        float[] intervalDistance = new float[3];
        float totalDistance = 0.0f;

        //DEBUG
        //StringBuilder stringBuilder = new StringBuilder();

        for (int i = 0 ; i < mUnitSplitList.size() - 1 ; i++){
            Location.distanceBetween(mUnitSplitList.get(i).getLocation().getLatitude(), mUnitSplitList.get(i).getLocation().getLongitude(), mUnitSplitList.get(i+1).getLocation().getLatitude(), mUnitSplitList.get(i + 1).getLocation().getLongitude(), intervalDistance);
            totalDistance += Math.abs(intervalDistance[0]);
            //DEBUG
            //stringBuilder.append("\n" + i + ": " + intervalDistance[0] + " meters");

        }

        switch(units){
            case 0:
                //convert meters to miles
                totalDistance = UnitConverter.convertMetersToMiles(totalDistance);
                break;
            case 1:
                //convert to feet
                totalDistance = UnitConverter.convertMetersToFeet(totalDistance);
                break;

            default:
                //do nothing, units are in meters already
                break;

        }

        return totalDistance;
    }

    private float getCalorieByActivity(float weight, float minutes, float speed, int activityId){

        float calorie = 0f;

        switch (activityId){

            case 1:
                calorie = CalorieCalculator.getCalorieByRun(weight, minutes, speed);
                break;

            case 2:
                calorie = CalorieCalculator.getCalorieByScooter(weight, minutes);
                break;

            case 3:
                calorie = CalorieCalculator.getCalorieByBike(weight, minutes, speed);
                break;

            case 4:
                calorie = CalorieCalculator.getCalorieByRun(weight, minutes, speed);
                break;

            default:
                calorie = CalorieCalculator.getCalorieByRun(weight, minutes, speed);
                break;
        }

        return calorie;
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_RESOLVE_ERROR) {
            mResolvingError = false;
            if (resultCode == this.RESULT_OK) {
                // Make sure the app is not already connected or attempting to connect
                if (!mGoogleApi.client().isConnecting() &&
                        !mGoogleApi.client().isConnected()) {
                    mGoogleApi.client().connect();
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
            case R.id.action_rewards:
                EditRewards();
                return true;
            case R.id.action_settings:
                //TODO:  openSettings();
                return true;

            default:
        }

        return super.onOptionsItemSelected(item);

    }

    private void EditRewards(){

        //TODO: replace this with a call to EditRewardFragment
        //Intent intent = new Intent(mFragmentActivity, EditReward.class);
        //startActivity(intent);
    }


    private class SaveToDB implements Runnable {

        public void run() {

            saveToDB(mDatabaseHelper, mUnitSplitList, mActivityDetail, mTotalPoints, getDistance(1));
        }
    }

}
