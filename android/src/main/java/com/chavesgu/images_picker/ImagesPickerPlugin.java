package com.chavesgu.images_picker;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.luck.picture.lib.basic.PictureSelectionCameraModel;
import com.luck.picture.lib.basic.PictureSelectionModel;
import com.luck.picture.lib.basic.PictureSelector;
import com.luck.picture.lib.config.SelectMimeType;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.interfaces.OnResultCallbackListener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/** ImagesPickerPlugin */
public class ImagesPickerPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware, PluginRegistry.RequestPermissionsResultListener {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private MethodChannel channel;
  private Result _result;
  private Activity activity;
  private Context context;
  private int WRITE_IMAGE_CODE = 33;
  private int WRITE_VIDEO_CODE = 44;
  private String WRITE_IMAGE_PATH;
  private String WRITE_VIDEO_PATH;
  private String ALBUM_NAME;
  public static String channelName = "chavesgu/images_picker";

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), channelName);
    channel.setMethodCallHandler(this);
    context = flutterPluginBinding.getApplicationContext();
  }

  public static void registerWith(Registrar registrar) {
    ImagesPickerPlugin instance = new ImagesPickerPlugin();
    final MethodChannel channel = new MethodChannel(registrar.messenger(), channelName);
    channel.setMethodCallHandler(instance);
    instance.context = registrar.context();
    registrar.addRequestPermissionsResultListener(instance);
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    channel.setMethodCallHandler(null);
  }

  @Override
  public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
    activity = binding.getActivity();
    binding.addRequestPermissionsResultListener(this);
  }

  @Override
  public void onDetachedFromActivityForConfigChanges() {

  }

  @Override
  public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
    activity = binding.getActivity();
  }

  @Override
  public void onDetachedFromActivity() {

  }


  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    _result = result;
    switch (call.method) {
      case "getPlatformVersion":
        result.success("Android " + android.os.Build.VERSION.RELEASE);
        break;
      case "pick": {
        int count = (int) call.argument("count");
        String pickType = call.argument("pickType");
        double quality = call.argument("quality");
        boolean supportGif = call.argument("gif");
        int maxTime = call.argument("maxTime");
        HashMap<String, Object> cropOption = call.argument("cropOption");
        String language = call.argument("language");

        int chooseType;
        switch (pickType) {
          case "PickType.video":
            chooseType = SelectMimeType.ofVideo();
            break;
          case "PickType.all":
            chooseType = SelectMimeType.ofAll();
            break;
          default:
            chooseType = SelectMimeType.ofImage();
            break;
        }
        PictureSelectionModel model = PictureSelector.create(activity)
                .openGallery(chooseType);
        if (language != null) {
          model.setLanguage(ImagesPickerUtil.getLanguage( language));
        }
        ImagesPickerUtil.setPhotoSelectOpt(model, count, quality);
        if (cropOption!=null) ImagesPickerUtil.setCropOpt(model, cropOption);
        model.isGif(supportGif);
        model.setFilterVideoMaxSecond(maxTime);
        model.forResult(res);
        break;
      }
      case "openCamera": {
        String pickType = call.argument("pickType");
        int maxTime = call.argument("maxTime");
        double quality = call.argument("quality");
        HashMap<String, Object> cropOption = call.argument("cropOption");
        String language = call.argument("language");

        int chooseType = SelectMimeType.ofVideo();
        switch (pickType) {
          case "PickType.image":
            chooseType = SelectMimeType.ofImage();
            break;
          default:
            chooseType = SelectMimeType.ofVideo();
            break;
        }

        PictureSelectionCameraModel model = PictureSelector.create(activity)
                .openCamera(chooseType);
        model.setOutputCameraDir(context.getExternalCacheDir().getAbsolutePath());
        if (pickType.equals("PickType.image")) {
          model.setOutputCameraImageFileName("image_picker_camera_"+UUID.randomUUID().toString()+".jpg");
        } else {
          model.setOutputCameraVideoFileName("image_picker_camera_"+UUID.randomUUID().toString()+".mp4");
        }
        model.setRecordVideoMaxSecond(maxTime);
        if (language != null) {
          model.setLanguage(ImagesPickerUtil.getLanguage( language));
        }
        ImagesPickerUtil.setCameraOpt(model, 1, quality);
//        if (cropOption!=null) Utils.setCropOpt(model, cropOption);
        model.forResult(res);
        break;
      }
      case "saveVideoToAlbum": {
        String path = (String) call.argument("path");
        String albumName = call.argument("albumName");
        WRITE_VIDEO_PATH = path;
        ALBUM_NAME = albumName;
        if (hasPermission()) {
          saveVideoToGallery(path, albumName);
        } else {
          String[] permissions = new String[2];
          permissions[0] = Manifest.permission.WRITE_EXTERNAL_STORAGE;
          permissions[1] = Manifest.permission.READ_EXTERNAL_STORAGE;
          ActivityCompat.requestPermissions(activity, permissions, WRITE_VIDEO_CODE);
        }
        break;
      }
      case "saveImageToAlbum": {
        String path = (String) call.argument("path");
        String albumName = call.argument("albumName");
        WRITE_IMAGE_PATH = path;
        ALBUM_NAME = albumName;
        if (hasPermission()) {
          saveImageToGallery(path, albumName);
        } else {
          String[] permissions = new String[2];
          permissions[0] = Manifest.permission.WRITE_EXTERNAL_STORAGE;
          permissions[1] = Manifest.permission.READ_EXTERNAL_STORAGE;
          ActivityCompat.requestPermissions(activity, permissions, WRITE_IMAGE_CODE);
        }
        break;
      }
//      case "saveNetworkImageToAlbum": {
//        String url = (String) call.arguments;
//        saveNetworkImageToGallery(url);
//        break;
//      }
      default:
        result.notImplemented();
        break;
    }
  }

  OnResultCallbackListener res = new OnResultCallbackListener<LocalMedia>() {
    @Override
    public void onResult(final ArrayList<LocalMedia> medias) {
      // 结果回调
      new Thread() {
        @Override
        public void run() {
          final List<Object> resArr = new ArrayList<Object>();
          for (LocalMedia media:medias) {
            HashMap<String, Object> map = new HashMap<String, Object>();
            String path = media.getSandboxPath();
            if (media.getMimeType().contains("image")) {
              if (media.isCut()) path = media.getCutPath();
              if (media.isCompressed()) path = media.getCompressPath();
            }
//              path = copyToTmp(path);
            map.put("path", path);

            String thumbPath;
            if (media.getMimeType().contains("image")) {
              thumbPath = path;
            } else {
              thumbPath = createVideoThumb(path);
            }
            map.put("thumbPath", thumbPath);

            int size = getFileSize(path);
            map.put("size", size);

            Log.i("pick test", map.toString());
            resArr.add(map);
          }

//            PictureFileUtils.deleteCacheDirFile(context, type);
//            PictureFileUtils.deleteAllCacheDirFile(context);

          new Handler(context.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
              _result.success(resArr);
            }
          });
        }
      }.start();
    }

    @Override
    public void onCancel() {
      // 取消
      _result.success(null);
    }
  };

  private String createVideoThumb(String path) {
    Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(path, MediaStore.Video.Thumbnails.FULL_SCREEN_KIND);
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
    try {
      File outputDir = context.getCacheDir();
      File outputFile = File.createTempFile("image_picker_thumb_"+ UUID.randomUUID().toString(), ".jpg", outputDir);
      FileOutputStream fo = new FileOutputStream(outputFile);
      fo.write(bytes.toByteArray());
      fo.close();
      return outputFile.getAbsolutePath();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  private int getFileSize(String path) {
    File file = new File(path);
    int size = Integer.parseInt(String.valueOf(file.length()));
    return size;
  }

  private String copyToTmp(String originPath) {
    String resPath = originPath;
    String suffix = originPath.substring(originPath.lastIndexOf('.'));
    File from = new File(originPath);
    File to;
    try {
      File outputDir = context.getCacheDir();
      to = File.createTempFile("image_picker_"+ UUID.randomUUID().toString(), suffix, outputDir);

      try {
        InputStream in = new FileInputStream(from);
        OutputStream out = new FileOutputStream(to);
        byte[] buf = new byte[1024];
        try {
          int len;
          while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
          }
          resPath = to.getAbsolutePath();
        } catch (IOException e) {
          Log.w("image_picker", e.getLocalizedMessage());
        }
      } catch (FileNotFoundException e) {
        Log.w("image_picker", e.getLocalizedMessage());
      }
    } catch (IOException e) {
      Log.w("image_picker", e.getLocalizedMessage());
    }
    return resPath;
  }

  private void saveImageToGallery(final String path, String albumName) {
    boolean status = false;
    String suffix = path.substring(path.lastIndexOf('.')+1);
    Bitmap bitmap = BitmapFactory.decodeFile(path);
    status = FileSaver.saveImage(context, bitmap, suffix, albumName);
    _result.success(status);
  }

  private void saveVideoToGallery(String path, String albumName) {
    _result.success(FileSaver.saveVideo(context, path, albumName));
  }

  private boolean hasPermission() {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
            (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PERMISSION_GRANTED && ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PERMISSION_GRANTED);
  }

    @Override
    public boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
      if (requestCode == WRITE_IMAGE_CODE && grantResults[0] == PERMISSION_GRANTED && grantResults[1] == PERMISSION_GRANTED) {
          saveImageToGallery(WRITE_IMAGE_PATH, ALBUM_NAME);
          return true;
      }
      if (requestCode == WRITE_VIDEO_CODE && grantResults[0] == PERMISSION_GRANTED && grantResults[1] == PERMISSION_GRANTED) {
          saveVideoToGallery(WRITE_VIDEO_PATH, ALBUM_NAME);
          return true;
      }
      return false;
    }
}
