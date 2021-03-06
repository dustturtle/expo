package abi13_0_0.host.exp.exponent.modules.api.components.barcodescanner;

import android.hardware.Camera;
import android.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BarCodeScanner {
  private static BarCodeScanner ourInstance;

  private final HashMap<Integer, CameraInfoWrapper> mCameraInfos;
  private final HashMap<Integer, Integer> mCameraTypeToIndex;
  private List<String> mBarCodeTypes = null;
  private final Map<Number, Camera> mCameras;
  private int mActualDeviceOrientation = 0;
  private int mAdjustedDeviceOrientation = 0;

  public static BarCodeScanner getInstance() {
    return ourInstance;
  }

  public static void createInstance(int deviceOrientation) {
    ourInstance = new BarCodeScanner(deviceOrientation);
  }


  public Camera acquireCameraInstance(int type) {
    if (null == mCameras.get(type) && null != mCameraTypeToIndex.get(type)) {
      try {
        Camera camera = Camera.open(mCameraTypeToIndex.get(type));
        mCameras.put(type, camera);
        adjustPreviewLayout(type);
      } catch (Exception e) {
        Log.e("BarCodeScanner", "acquireCameraInstance failed", e);
      }
    }
    return mCameras.get(type);
  }

  public void releaseCameraInstance(int type) {
    if (null != mCameras.get(type)) {
      mCameras.get(type).release();
      mCameras.remove(type);
    }
  }

  public int getPreviewWidth(int type) {
    CameraInfoWrapper cameraInfo = mCameraInfos.get(type);
    if (null == cameraInfo) {
      return 0;
    }
    return cameraInfo.previewWidth;
  }

  public int getPreviewHeight(int type) {
    CameraInfoWrapper cameraInfo = mCameraInfos.get(type);
    if (null == cameraInfo) {
      return 0;
    }
    return cameraInfo.previewHeight;
  }

  public Camera.Size getBestSize(List<Camera.Size> supportedSizes, int maxWidth, int maxHeight) {
    Camera.Size bestSize = null;
    for (Camera.Size size : supportedSizes) {
      if (size.width > maxWidth || size.height > maxHeight) {
        continue;
      }

      if (bestSize == null) {
        bestSize = size;
        continue;
      }

      int resultArea = bestSize.width * bestSize.height;
      int newArea = size.width * size.height;

      if (newArea > resultArea) {
        bestSize = size;
      }
    }

    return bestSize;
  }

  private Camera.Size getSmallestSize(List<Camera.Size> supportedSizes) {
    Camera.Size smallestSize = null;
    for (Camera.Size size : supportedSizes) {
      if (smallestSize == null) {
        smallestSize = size;
        continue;
      }

      int resultArea = smallestSize.width * smallestSize.height;
      int newArea = size.width * size.height;

      if (newArea < resultArea) {
        smallestSize = size;
      }
    }

    return smallestSize;
  }

  public List<String> getBarCodeTypes() {
    return mBarCodeTypes;
  }

  public void setBarCodeTypes(List<String> barCodeTypes) {
    mBarCodeTypes = barCodeTypes;
  }

  public int getActualDeviceOrientation() {
    return mActualDeviceOrientation;
  }

  public void setAdjustedDeviceOrientation(int orientation) {
    mAdjustedDeviceOrientation = orientation;
  }

  public int getAdjustedDeviceOrientation() {
    return mAdjustedDeviceOrientation;
  }

  public void setActualDeviceOrientation(int actualDeviceOrientation) {
    mActualDeviceOrientation = actualDeviceOrientation;
    adjustPreviewLayout(BarCodeScannerModule.RCT_CAMERA_TYPE_FRONT);
    adjustPreviewLayout(BarCodeScannerModule.RCT_CAMERA_TYPE_BACK);
  }

  public void setTorchMode(int cameraType, int torchMode) {
    Camera camera = mCameras.get(cameraType);
    if (null == camera) {
      return;
    }

    Camera.Parameters parameters = camera.getParameters();
    String value = parameters.getFlashMode();
    switch (torchMode) {
      case BarCodeScannerModule.RCT_CAMERA_TORCH_MODE_ON:
        value = Camera.Parameters.FLASH_MODE_TORCH;
        break;
      case BarCodeScannerModule.RCT_CAMERA_TORCH_MODE_OFF:
        value = Camera.Parameters.FLASH_MODE_OFF;
        break;
    }

    List<String> flashModes = parameters.getSupportedFlashModes();
    if (flashModes != null && flashModes.contains(value)) {
      parameters.setFlashMode(value);
      camera.setParameters(parameters);
    }
  }

  public void setFlashMode(int cameraType, int flashMode) {
    Camera camera = mCameras.get(cameraType);
    if (null == camera) {
      return;
    }

    Camera.Parameters parameters = camera.getParameters();
    String value = parameters.getFlashMode();
    switch (flashMode) {
      case BarCodeScannerModule.RCT_CAMERA_FLASH_MODE_AUTO:
        value = Camera.Parameters.FLASH_MODE_AUTO;
        break;
      case BarCodeScannerModule.RCT_CAMERA_FLASH_MODE_ON:
        value = Camera.Parameters.FLASH_MODE_ON;
        break;
      case BarCodeScannerModule.RCT_CAMERA_FLASH_MODE_OFF:
        value = Camera.Parameters.FLASH_MODE_OFF;
        break;
    }
    List<String> flashModes = parameters.getSupportedFlashModes();
    if (flashModes != null && flashModes.contains(value)) {
      parameters.setFlashMode(value);
      camera.setParameters(parameters);
    }
  }

  public void adjustCameraRotationToDeviceOrientation(int type, int deviceOrientation) {
    Camera camera = mCameras.get(type);
    if (null == camera) {
      return;
    }

    CameraInfoWrapper cameraInfo = mCameraInfos.get(type);
    int rotation;
    int orientation = cameraInfo.info.orientation;
    if (cameraInfo.info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
      rotation = (orientation + deviceOrientation * 90) % 360;
    } else {
      rotation = (orientation - deviceOrientation * 90 + 360) % 360;
    }
    cameraInfo.rotation = rotation;
    Camera.Parameters parameters = camera.getParameters();
    parameters.setRotation(cameraInfo.rotation);

    try {
      camera.setParameters(parameters);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void adjustPreviewLayout(int type) {
    Camera camera = mCameras.get(type);
    if (null == camera) {
      return;
    }

    CameraInfoWrapper cameraInfo = mCameraInfos.get(type);
    int displayRotation;
    int rotation;
    int orientation = cameraInfo.info.orientation;
    if (cameraInfo.info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
      rotation = (orientation + mActualDeviceOrientation * 90) % 360;
      displayRotation = (720 - orientation - mActualDeviceOrientation * 90) % 360;
    } else {
      rotation = (orientation - mActualDeviceOrientation * 90 + 360) % 360;
      displayRotation = rotation;
    }
    cameraInfo.rotation = rotation;

    setAdjustedDeviceOrientation(rotation);
    camera.setDisplayOrientation(displayRotation);

    Camera.Parameters parameters = camera.getParameters();
    parameters.setRotation(cameraInfo.rotation);

    // set preview size
    // defaults to highest resolution available
    Camera.Size optimalPreviewSize = getBestSize(parameters.getSupportedPreviewSizes(), Integer.MAX_VALUE, Integer.MAX_VALUE);
    int width = optimalPreviewSize.width;
    int height = optimalPreviewSize.height;

    parameters.setPreviewSize(width, height);
    try {
      camera.setParameters(parameters);
    } catch (Exception e) {
      e.printStackTrace();
    }

    if (cameraInfo.rotation == 0 || cameraInfo.rotation == 180) {
      cameraInfo.previewWidth = width;
      cameraInfo.previewHeight = height;
    } else {
      cameraInfo.previewWidth = height;
      cameraInfo.previewHeight = width;
    }
  }

  private BarCodeScanner(int deviceOrientation) {
    mCameras = new HashMap<>();
    mCameraInfos = new HashMap<>();
    mCameraTypeToIndex = new HashMap<>();

    mActualDeviceOrientation = deviceOrientation;

    // map camera types to camera indexes and collect cameras properties
    for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
      Camera.CameraInfo info = new Camera.CameraInfo();
      Camera.getCameraInfo(i, info);
      if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT && mCameraInfos.get(BarCodeScannerModule.RCT_CAMERA_TYPE_FRONT) == null) {
        mCameraInfos.put(BarCodeScannerModule.RCT_CAMERA_TYPE_FRONT, new CameraInfoWrapper(info));
        mCameraTypeToIndex.put(BarCodeScannerModule.RCT_CAMERA_TYPE_FRONT, i);
        acquireCameraInstance(BarCodeScannerModule.RCT_CAMERA_TYPE_FRONT);
        releaseCameraInstance(BarCodeScannerModule.RCT_CAMERA_TYPE_FRONT);
      } else if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK && mCameraInfos.get(BarCodeScannerModule.RCT_CAMERA_TYPE_BACK) == null) {
        mCameraInfos.put(BarCodeScannerModule.RCT_CAMERA_TYPE_BACK, new CameraInfoWrapper(info));
        mCameraTypeToIndex.put(BarCodeScannerModule.RCT_CAMERA_TYPE_BACK, i);
        acquireCameraInstance(BarCodeScannerModule.RCT_CAMERA_TYPE_BACK);
        releaseCameraInstance(BarCodeScannerModule.RCT_CAMERA_TYPE_BACK);
      }
    }
  }

  private class CameraInfoWrapper {
    public final Camera.CameraInfo info;
    public int rotation = 0;
    public int previewWidth = -1;
    public int previewHeight = -1;

    public CameraInfoWrapper(Camera.CameraInfo info) {
      this.info = info;
    }
  }
}
