package com.dreamteam.vicam.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Toast;

import com.dreamteam.camera.R;
import com.dreamteam.vicam.model.database.CameraDAO;
import com.dreamteam.vicam.model.database.DatabaseOrmLiteHelper;
import com.dreamteam.vicam.model.database.PresetDAO;
import com.dreamteam.vicam.model.events.CameraChangedEvent;
import com.dreamteam.vicam.model.events.PresetChangedEvent;
import com.dreamteam.vicam.model.pojo.Camera;
import com.dreamteam.vicam.model.pojo.Preset;
import com.dreamteam.vicam.model.pojo.Speed;
import com.dreamteam.vicam.presenter.CameraServiceManager;
import com.dreamteam.vicam.presenter.network.camera.CameraFacade;
import com.dreamteam.vicam.presenter.utility.Dagger;
import com.dreamteam.vicam.view.custom.CameraArrayAdapter;
import com.dreamteam.vicam.view.custom.PresetArrayAdapter;
import com.dreamteam.vicam.view.custom.SeekBarChangeListener;
import com.j256.ormlite.android.apptools.OpenHelperManager;

import de.greenrobot.event.EventBus;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class MainActivity extends Activity {

  @Inject
  EventBus eventBus;

  private Camera mCurrentCamera;
  private CharSequence mTitle;
  private List<Preset> mPresets;

  private ActionBarDrawerToggle mDrawerToggle;
  private CameraArrayAdapter mCameraAdapter;
  private PresetArrayAdapter mPresetAdapter;
  private AlertDialog mDialogSavePreset;
  private DatabaseOrmLiteHelper mDatabaseHelper;

  @InjectView(R.id.sync_loader)
  RelativeLayout mLoaderSpinner;
  @InjectView(R.id.drawer_layout)
  DrawerLayout mDrawerLayout;
  @InjectView(R.id.navigation_drawer)
  ListView mDrawerList;
  @InjectView(R.id.focus_seekbar)
  SeekBar mFocusSeekBar;
  @InjectView(R.id.zoom_seekbar)
  SeekBar mZoomSeekBar;
  @InjectView(R.id.camera_touchpad)
  ImageView mTouchpad;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    Dagger.inject(this);
    ButterKnife.inject(this);
    // Sets default values defined in camera_preferences if empty
    PreferenceManager.setDefaultValues(this, R.xml.camera_preferences, false);
    // Get set camera_preferences
    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

    mTitle = getString(R.string.app_name);

    getActionBar().setDisplayHomeAsUpEnabled(true);
    getActionBar().setHomeButtonEnabled(true);
    getActionBar().setDisplayShowTitleEnabled(true);

    PresetDAO presetDao = getDatabase().getPresetDAO();
    List<Preset> presets = presetDao.getPresets();
    if (presets != null) {
      mPresets = presets;
    } else {
      mPresets = new ArrayList<Preset>();
    }

    mPresetAdapter = new PresetArrayAdapter(this, mPresets);
    mDrawerList.setAdapter(mPresetAdapter);
    mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

    mDrawerToggle = new DrawerToggle(this, mDrawerLayout);
    mDrawerLayout.setDrawerListener(mDrawerToggle);

    mFocusSeekBar.setOnSeekBarChangeListener(new SeekBarChangeListener(this, SeekBarChangeListener.Type.FOCUS));
    mZoomSeekBar.setOnSeekBarChangeListener(new SeekBarChangeListener(this, SeekBarChangeListener.Type.ZOOM));

    mTouchpad.setOnTouchListener(new View.OnTouchListener() {
      @Override
      public boolean onTouch(View view, MotionEvent motionEvent) {
        float eventX = motionEvent.getX();
        float eventY = motionEvent.getY();

        int normX = (int) (eventX / mTouchpad.getWidth() * Speed.UPPER_BOUND + Speed.LOWER_BOUND);
        int normY = (int) (eventY / mTouchpad.getHeight() * Speed.UPPER_BOUND + Speed.LOWER_BOUND);

        if (normX < Speed.LOWER_BOUND || normX > Speed.UPPER_BOUND
            || normY < Speed.LOWER_BOUND || normY > Speed.UPPER_BOUND) {
          return false;
        }

        switch (motionEvent.getAction()) {
          case MotionEvent.ACTION_DOWN:
          case MotionEvent.ACTION_MOVE:
            getFacade()
                .moveStart(new Speed(normX, normY))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread())
                .subscribe(
                    new Action1<String>() {
                      @Override
                      public void call(String s) {
                        showToast("debug", Toast.LENGTH_SHORT);
                      }
                    }, new Action1<Throwable>() {
                      @Override
                      public void call(Throwable throwable) {
                        showToast("ERRRR", Toast.LENGTH_SHORT);
                      }
                    }
                );
            return true;
          case MotionEvent.ACTION_UP:
            getFacade()
                .moveStop()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread()).subscribe(
                new Action1<String>() {
                  @Override
                  public void call(String s) {
                    showToast("debugstop", Toast.LENGTH_SHORT);
                  }
                }, new Action1<Throwable>() {
                  @Override
                  public void call(Throwable throwable) {
                    showToast("ERRRRopp", Toast.LENGTH_SHORT);
                  }
                }
            );
            return true;
          default:
            return false;
        }
      }
    });

    CameraDAO cameraDao = getDatabase().getCameraDAO();
    List<Camera> cameras = cameraDao.getCameras();
    if (cameras == null) {
      cameras = new ArrayList<Camera>();
    }
    if (cameras.isEmpty()) {
      cameraDao.insertCamera(new Camera("127.0.0.1", "Camera 1", null));
      cameraDao.insertCamera(new Camera("localhost", "Camera 2", null));
      cameraDao.insertCamera(new Camera("localhost", "Camera 3", null));
      cameras = cameraDao.getCameras();
    }
    mCameraAdapter = new CameraArrayAdapter(this, cameras);

    AlertDialog.Builder builderSavePreset = new AlertDialog.Builder(this);
    builderSavePreset.setTitle(R.string.dialog_save_preset_title);

    // Set an EditText view to get user input
    final EditText input = new EditText(this);
    builderSavePreset.setView(input);

    builderSavePreset.setPositiveButton(
        R.string.dialog_ok,
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int id) {
            mLoaderSpinner.setVisibility(View.GONE);
          }
        }
    );
    builderSavePreset.setNegativeButton(
        R.string.dialog_cancel,
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int id) {
            // User cancelled the dialog
          }
        }
    );
    mDialogSavePreset = builderSavePreset.create();
    mLoaderSpinner.setVisibility(View.GONE);

  }

  @OnClick(R.id.one_touch_autofocus)
  public void OneTouchAutofocusClick(Button button) {

  }

  public void insertPreset(Preset preset) {
    PresetDAO presetDao = getDatabase().getPresetDAO();
    presetDao.insertPreset(preset);
    mPresets.add(preset);
    mPresetAdapter.notifyDataSetChanged();
  }

  public void updatePreset(Preset preset) {
    PresetDAO presetDao = getDatabase().getPresetDAO();
    presetDao.updatePreset(preset);
    for (int i = 0; i < mPresets.size(); i++) {
      if (mPresets.get(i).getId() == preset.getId()) {
        mPresets.set(i, preset);
        break;
      }
    }
    mPresetAdapter.notifyDataSetChanged();
  }

  public void deletePreset(Preset preset) {
    PresetDAO presetDao = getDatabase().getPresetDAO();
    presetDao.deletePreset(preset.getId());
    for (int i = 0; i < mPresets.size(); i++) {
      if (mPresets.get(i).getId() == preset.getId()) {
        mPresets.remove(i);
        break;
      }
    }
    mPresetAdapter.notifyDataSetChanged();
  }

  // Load to loader spinner
  public void load(View view) {
    mLoaderSpinner.setVisibility(View.VISIBLE);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.main, menu);

    MenuItem cameraSpinner = menu.findItem(R.id.action_change_camera);
    View view = cameraSpinner.getActionView();
    if (view instanceof Spinner) {
      Spinner spinner = (Spinner) view;
      spinner.setAdapter(mCameraAdapter);

      spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
          Camera camera = (Camera) parent.getItemAtPosition(position);
          changeCamera(camera);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
      });
    }
    return true;
  }

  @Override
  protected void onPostCreate(Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);
    // Sync the toggle state after onRestoreInstanceState has occurred.
    mDrawerToggle.syncState();
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    mDrawerToggle.onConfigurationChanged(newConfig);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Pass the event to ActionBarDrawerToggle, if it returns
    // true, then it has handled the app icon touch event
    if (mDrawerToggle.onOptionsItemSelected(item)) {
      return true;
    }
    // Handle menu items
    switch (item.getItemId()) {
      case R.id.action_settings:
        startActivity(new Intent(this, SettingsActivity.class));
        return true;
      case R.id.action_save_preset:
        mDialogSavePreset.show();
        return true;
      case R.id.action_sync_presets:
        load(mLoaderSpinner);
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override
  public void setTitle(CharSequence title) {
    mTitle = title;
    getActionBar().setTitle(mTitle);
  }

  @Override
  protected void onResume() {
    super.onResume();
    eventBus.register(this);
  }

  @Override
  protected void onPause() {
    super.onPause();
    eventBus.unregister(this);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (mDatabaseHelper != null) {
      OpenHelperManager.releaseHelper();
      mDatabaseHelper = null;
    }
  }

  private DatabaseOrmLiteHelper getDatabase() {
    if (mDatabaseHelper == null) {
      mDatabaseHelper = OpenHelperManager.getHelper(this, DatabaseOrmLiteHelper.class);
    }
    return mDatabaseHelper;
  }

  public CameraFacade getFacade() {
    return CameraServiceManager.getFacadeFor(mCurrentCamera);
  }

  public void showToast(String msg, int length) {
    Toast.makeText(this, msg, length).show();
  }

  private void changeCamera(Camera camera) {
    if (camera == null) {
      throw new IllegalArgumentException("Camera cannot be null!");
    }
    mCurrentCamera = camera;
    eventBus.post(new CameraChangedEvent(camera));
  }

  public void onEventMainThread(CameraChangedEvent e) {
    showToast("Current Camera: " + e.camera, Toast.LENGTH_SHORT);
  }

  public void onEventMainThread(PresetChangedEvent e) {
    showToast("Selected Preset: " + e.preset, Toast.LENGTH_SHORT);
  }

  private class DrawerItemClickListener implements ListView.OnItemClickListener {

    @Override
    public void onItemClick(AdapterView parent, View view, int position, long id) {
      Preset preset = (Preset) parent.getItemAtPosition(position);
      mDrawerLayout.closeDrawer(mDrawerList);
      eventBus.post(new PresetChangedEvent(preset));
    }
  }

  private class DrawerToggle extends ActionBarDrawerToggle {

    private DrawerToggle(Activity activity, DrawerLayout drawerLayout) {
      super(activity, drawerLayout, R.drawable.ic_drawer, R.string.drawer_open,
            R.string.drawer_close);
    }

    /**
     * Called when a drawer has settled in a completely closed state.
     */
    @Override
    public void onDrawerClosed(View view) {
      getActionBar().setTitle(getString(R.string.app_name));
    }

    /**
     * Called when a drawer has settled in a completely open state.
     */
    @Override
    public void onDrawerOpened(View view) {
      getActionBar().setTitle(getString(R.string.change_preset));
    }
  }
}