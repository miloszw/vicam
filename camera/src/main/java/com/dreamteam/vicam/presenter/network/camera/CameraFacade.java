package com.dreamteam.vicam.presenter.network.camera;


import com.dreamteam.vicam.model.pojo.CameraState;
import com.dreamteam.vicam.model.pojo.Focus;
import com.dreamteam.vicam.model.pojo.Position;
import com.dreamteam.vicam.model.pojo.Speed;
import com.dreamteam.vicam.model.pojo.Zoom;
import com.dreamteam.vicam.presenter.utility.Utils;

import rx.Observable;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.functions.Func3;

/**
 * Created by fsommar on 2014-04-01.
 */
public class CameraFacade {

  private CameraCommands cameraCommands;

  public CameraFacade(CameraService cameraService) {
    this.cameraCommands = new CameraCommands(cameraService);
  }

  public Observable<String> moveStart(Speed speed) {
    return cameraCommands.panTilt(speed.getX(), speed.getY());
  }

  public Observable<String> moveStop() {
    return cameraCommands.panTilt(Speed.STOP, Speed.STOP);
  }

  public Observable<String> zoomStart(int speed) {
    Utils.rangeCheck(speed, Speed.LOWER_BOUND, Speed.UPPER_BOUND);
    return cameraCommands.zoom(speed);
  }

  public Observable<String> zoomStop() {
    return cameraCommands.zoom(Speed.STOP);
  }

  public Observable<String> oneTouchFocus() {
    return cameraCommands.oneTouchAutofocus();
  }

  public Observable<String> moveAbsolute(Position pos) {
    return cameraCommands.panTiltAbsolute(pos.getPan(), pos.getTilt());
  }

  public Observable<String> zoomAbsolute(int level) {
    Utils.rangeCheck(level, Zoom.LOWER_BOUND, Zoom.UPPER_BOUND);
    return cameraCommands.zoomAbsolute(level);
  }

  public Observable<String> focusAbsolute(int level) {
    Utils.rangeCheck(level, Focus.LOWER_BOUND, Focus.UPPER_BOUND);
    return cameraCommands.focusAbsolute(level);
  }

  public Observable<CameraState> getCameraState() {
    return Observable.zip(
        cameraCommands.getPanTilt(), getZoom(), getFocus(),
        new Func3<Position, Zoom, Focus, CameraState>() {
          @Override
          public CameraState call(Position position, Zoom zoom, Focus focus) {
            return new CameraState(position, zoom, focus);
          }
        }
    );
  }

  public Observable<Focus> getFocus() {
    return Observable.zip(
        cameraCommands.getFocusLevel(), cameraCommands.getAF(),
        new Func2<Integer, Boolean, Focus>() {
          @Override
          public Focus call(Integer level, Boolean AF) {
            if (level != null && AF != null) {
              return new Focus(level, AF);
            }
            return null;
          }
        }
    );
  }

  public Observable<Zoom> getZoom() {
    return cameraCommands.getZoomLevel().map(new Func1<Integer, Zoom>() {
      @Override
      public Zoom call(Integer level) {
        if (level != null) {
          return new Zoom(level);
        }
        return null;
      }
    });
  }

}
