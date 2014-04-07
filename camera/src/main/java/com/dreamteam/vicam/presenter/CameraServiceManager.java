package com.dreamteam.vicam.presenter;

import com.dreamteam.vicam.model.pojo.Camera;
import com.dreamteam.vicam.presenter.network.camera.CameraFacade;
import com.dreamteam.vicam.presenter.network.camera.CameraService;

import java.util.HashMap;
import java.util.Map;

import retrofit.RestAdapter;

/**
 * Created by fsommar on 2014-04-07.
 */
public class CameraServiceManager {

  private static Map<String, CameraFacade> cameraFacades = new HashMap<>();

  public static CameraFacade geFacadeFor(Camera c) {
    String key = c.getIp() + ":" + Short.toString(c.getPort());

    if (cameraFacades.containsKey(key)) {
      return cameraFacades.get(key);
    }
    return cameraFacades.put(key, new CameraFacade(createServiceFor(c)));
  }

  public static CameraService createServiceFor(Camera c) {
    return new RestAdapter
        .Builder()
        .setEndpoint(c.getIp() + ":" + Short.toString(c.getPort()))
        .build()
        .create(CameraService.class);
  }
}
