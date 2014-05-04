package com.dreamteam.vicam.view;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import com.dreamteam.camera.R;
import com.dreamteam.vicam.model.database.CameraDAO;
import com.dreamteam.vicam.model.database.DAOFactory;
import com.dreamteam.vicam.model.database.PresetDAO;
import com.dreamteam.vicam.model.events.CameraChangedEvent;
import com.dreamteam.vicam.model.events.DeletePresetsEvent;
import com.dreamteam.vicam.model.events.EditPresetEvent;
import com.dreamteam.vicam.model.events.OnDrawerCloseEvent;
import com.dreamteam.vicam.model.events.PresetChangedEvent;
import com.dreamteam.vicam.model.events.SavePresetEvent;
import com.dreamteam.vicam.model.pojo.Camera;
import com.dreamteam.vicam.model.pojo.CameraState;
import com.dreamteam.vicam.model.pojo.Focus;
import com.dreamteam.vicam.model.pojo.Position;
import com.dreamteam.vicam.model.pojo.Preset;
import com.dreamteam.vicam.model.pojo.Zoom;
import com.dreamteam.vicam.presenter.CameraServiceManager;
import com.dreamteam.vicam.presenter.network.camera.CameraFacade;
import com.dreamteam.vicam.presenter.utility.Dagger;
import com.dreamteam.vicam.view.custom.CameraArrayAdapter;
import com.dreamteam.vicam.view.custom.CameraSpinnerItemListener;
import com.dreamteam.vicam.view.custom.DrawerItemClickListener;
import com.dreamteam.vicam.view.custom.DrawerMultiChoiceListener;
import com.dreamteam.vicam.view.custom.DrawerToggle;
import com.dreamteam.vicam.view.custom.PresetArrayAdapter;
import com.dreamteam.vicam.view.custom.SavePresetDialogFragment;
import com.dreamteam.vicam.view.custom.SeekBarChangeListener;
import com.dreamteam.vicam.view.custom.TouchpadTouchListener;

import de.greenrobot.event.EventBus;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class MainActivity extends Activity {

  @Inject
  EventBus mEventBus;
  @Inject
  DAOFactory mDAOFactory;

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
  @InjectView(R.id.one_touch_autofocus)
  Button mAutofocusButton;
  @InjectView(R.id.autofocus_switch)
  Switch mAutofocusSwitch;

  private Camera mCurrentCamera;
  private CharSequence mTitle;
  private List<Preset> mPresets;
  private ActionBarDrawerToggle mDrawerToggle;
  private CameraArrayAdapter mCameraAdapter;
  private PresetArrayAdapter mPresetAdapter;
  private SavePresetDialogFragment mSavePresetDialogFragment;
  private DrawerMultiChoiceListener mMultiChoiceListener;

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

    mPresets = new ArrayList<>();
    mPresetAdapter = new PresetArrayAdapter(this, mPresets);

    mDrawerList.setAdapter(mPresetAdapter);
    mDrawerList.setOnItemClickListener(new DrawerItemClickListener(this));
    mDrawerToggle = new DrawerToggle(this, mDrawerLayout);
    mDrawerLayout.setDrawerListener(mDrawerToggle);

    mFocusSeekBar.setOnSeekBarChangeListener(
        new SeekBarChangeListener(this, SeekBarChangeListener.Type.FOCUS));
    mZoomSeekBar.setOnSeekBarChangeListener(
        new SeekBarChangeListener(this, SeekBarChangeListener.Type.ZOOM));

    mTouchpad.setOnTouchListener(new TouchpadTouchListener(this));

    CameraDAO cameraDao = getCameraDAO();
    List<Camera> cameras = cameraDao.getCameras();
    if (cameras == null) {
      cameras = new ArrayList<>();
    }
    if (cameras.isEmpty()) {
      cameraDao.insertCamera(new Camera("127.0.0.1", "Camera 1", null));
      cameraDao.insertCamera(new Camera("localhost", "Camera 2", null));
      cameraDao.insertCamera(new Camera("localhost", "Camera 3", null));
      cameras = cameraDao.getCameras();
    }
    mCameraAdapter = new CameraArrayAdapter(this, cameras);

    // Init. Save Preset Dialog
    mSavePresetDialogFragment = new SavePresetDialogFragment(this);
    mSavePresetDialogFragment.onCreateDialog(savedInstanceState);

    // Init. value of loading spinner
    mLoaderSpinner.setVisibility(View.GONE);

    mDrawerList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
    mMultiChoiceListener = new DrawerMultiChoiceListener(this, mDrawerList);
    mDrawerList.setMultiChoiceModeListener(mMultiChoiceListener);

    // TODO: restore selected camera position from shared preferences
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
      spinner.setOnItemSelectedListener(new CameraSpinnerItemListener());
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
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        mSavePresetDialogFragment.show(ft, "Alert Dialog");
        return true;
      case R.id.action_sync_presets:
        mLoaderSpinner.setVisibility(View.VISIBLE);
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
    mEventBus.register(this);
  }

  @Override
  protected void onPause() {
    super.onPause();
    mEventBus.unregister(this);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    mDAOFactory.close();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    // TODO: save selected camera in shared preferences
  }

  public CameraFacade getFacade() {
    return CameraServiceManager.getFacadeFor(mCurrentCamera);
  }

  public void showToast(String msg, int length) {
    Toast.makeText(this, msg, length).show();
  }

  @OnClick(R.id.one_touch_autofocus)
  @SuppressWarnings("unused")
  public void OneTouchAutofocusClick(Button button) {
    getFacade()
        .oneTouchFocus()
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
            showToast("Error", Toast.LENGTH_SHORT);
          }
        }
    );

  }

  @OnClick(R.id.autofocus_switch)
  @SuppressWarnings("unused")
  public void AutofocusClick(Switch switchButton) {
    boolean on = switchButton.isChecked();

    getFacade()
        .setAF(on)
        .flatMap(new Func1<String, Observable<CameraState>>() {
          @Override
          public Observable<CameraState> call(String s) {
            // after AF has changed we fetch the new state from camera
            // (focus has probably changed since last time we got its information)
            return getFacade().getCameraState();
          }
        })
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeOn(Schedulers.newThread()).subscribe(
        new Action1<CameraState>() {
          @Override
          public void call(CameraState cameraState) {
            showToast("debugstop", Toast.LENGTH_SHORT);
          }
        }, new Action1<Throwable>() {
          @Override
          public void call(Throwable throwable) {
            showToast("AF", Toast.LENGTH_SHORT);
          }
        }
    );
    mAutofocusButton.setEnabled(!on);
    mFocusSeekBar.setEnabled(!on);

  }

  public void closeDrawer() {
    mDrawerLayout.closeDrawer(mDrawerList);
  }

  private void updateWithCameraState(CameraState cameraState) {
    mFocusSeekBar.setProgress(cameraState.getFocus().getLevel());
    mZoomSeekBar.setProgress(cameraState.getZoom().getLevel());
    mAutofocusSwitch.setChecked(cameraState.isAF());
  }

  private void updateCameraState() {
    getFacade()
        .getCameraState()
        .subscribeOn(Schedulers.newThread())
        .observeOn(AndroidSchedulers.mainThread()).subscribe(
        new Action1<CameraState>() {
          @Override
          public void call(CameraState cameraState) {
            updateWithCameraState(cameraState);
          }
        },
        new Action1<Throwable>() {
          @Override
          public void call(Throwable throwable) {
            showToast("Failed getting latest state from camera", Toast.LENGTH_SHORT);
            // TODO for GUI: use some indication for failed request
          }
        }
    );
  }

  @SuppressWarnings("unused")
  public void onEventMainThread(CameraChangedEvent e) {
    mCurrentCamera = e.camera;
    List<Preset> presets = getPresetDAO().getPresetsForCamera(mCurrentCamera);
    mPresets.clear();
    mPresets.addAll(presets);
    mPresetAdapter.notifyDataSetChanged();
    updateCameraState();
    showToast("Current Camera: " + e.camera, Toast.LENGTH_SHORT);
  }

  @SuppressWarnings("unused")
  public void onEventMainThread(PresetChangedEvent e) {
    showToast("Selected Preset: " + e.preset, Toast.LENGTH_SHORT);
    final CameraState cameraState = e.preset.getCameraState();
    getFacade()
        .setCameraState(cameraState)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeOn(Schedulers.newThread()).subscribe(
        new Action1<Boolean>() {
          @Override
          public void call(Boolean b) {
            showToast("debugstop", Toast.LENGTH_SHORT);
            updateWithCameraState(cameraState);
          }
        }, new Action1<Throwable>() {
          @Override
          public void call(Throwable throwable) {
            showToast("Error", Toast.LENGTH_SHORT);
          }
        }
    );
  }

  @SuppressWarnings("unused")
  public void onEventMainThread(OnDrawerCloseEvent e) {
    mMultiChoiceListener.close();
  }

  @SuppressWarnings("unused")
  public void onEventMainThread(final SavePresetEvent e) {
    getFacade()
        .getCameraState()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeOn(Schedulers.newThread()).subscribe(
        new Action1<CameraState>() {
          @Override
          public void call(CameraState cameraState) {
            insertPreset(new Preset(e.name, mCurrentCamera, cameraState));
          }
        },
        new Action1<Throwable>() {
          @Override
          public void call(Throwable throwable) {
            showToast("Failed getting state from camera when saving preset", Toast.LENGTH_SHORT);
            // TODO Remove when done with debugging
            insertPreset(new Preset(e.name, mCurrentCamera, new CameraState(
                new Position(0x5000, 0x5000),
                new Zoom(0x666),
                new Focus(0x777, true))));
          }
        }
    );
  }

  @SuppressWarnings("unused")
  public void onEventMainThread(DeletePresetsEvent e) {
    deletePresets(e.presets);
  }

  @SuppressWarnings("unused")
  public void onEventMainThread(EditPresetEvent e) {
    // TODO: show dialog for editing preset
  }

  private CameraDAO getCameraDAO() {
    return mDAOFactory.getCameraDAO();
  }

  private PresetDAO getPresetDAO() {
    return mDAOFactory.getPresetDAO();
  }

  public void insertPreset(Preset preset) {
    PresetDAO presetDao = getPresetDAO();
    presetDao.insertPreset(preset);
    mPresets.add(preset);
    mPresetAdapter.notifyDataSetChanged();
  }

  public void updatePreset(Preset preset) {
    PresetDAO presetDao = getPresetDAO();
    presetDao.updatePreset(preset);
    for (int i = 0; i < mPresets.size(); i++) {
      if (mPresets.get(i).getId() == preset.getId()) {
        mPresets.set(i, preset);
        break;
      }
    }
    mPresetAdapter.notifyDataSetChanged();
  }

  public void deletePresets(List<Preset> presets) {
    PresetDAO presetDao = getPresetDAO();
    for (Preset p : presets) {
      presetDao.deletePreset(p.getId());
      for (int i = 0; i < mPresets.size(); i++) {
        if (mPresets.get(i).getId() == p.getId()) {
          mPresets.remove(i);
          break;
        }
      }
    }
    mPresetAdapter.notifyDataSetChanged();
  }
}
