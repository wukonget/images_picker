package com.chavesgu.images_picker;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.text.TextUtils;

import com.luck.picture.lib.basic.PictureSelectionCameraModel;
import com.luck.picture.lib.basic.PictureSelectionModel;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.config.SelectModeConfig;
import com.luck.picture.lib.engine.CompressFileEngine;
import com.luck.picture.lib.engine.UriToFileTransformEngine;
import com.luck.picture.lib.interfaces.OnKeyValueResultCallbackListener;
import com.luck.picture.lib.language.LanguageConfig;
import com.luck.picture.lib.utils.SandboxTransformUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import top.zibin.luban.CompressionPredicate;
import top.zibin.luban.Luban;
import top.zibin.luban.OnNewCompressListener;

public class ImagesPickerUtil {
    public static PictureSelectionModel setPhotoSelectOpt(PictureSelectionModel model, int count, double quality) {
        model
                .setImageEngine(GlideEngine.createGlideEngine())
                .setMaxSelectNum(count)
                .setMinSelectNum(1)
                .setMaxVideoSelectNum(count)
                .setMinVideoSelectNum(1)
                .setSelectionMode(count > 1 ? SelectModeConfig.MULTIPLE : SelectModeConfig.SINGLE)
                .isDirectReturnSingle(false)
                .isDisplayCamera(false)
                .isPreviewZoomEffect(true)
                .isGif(true)
                .isEmptyResultReturn(false)
                .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                .isOriginalControl(false)
                .isMaxSelectEnabledMask(true)
                .setCameraImageFormat(PictureMimeType.JPEG)
                .setCameraVideoFormat(PictureMimeType.MP4)
                .setCompressEngine(lubanEngine)
                .setSandboxFileEngine(uriTransEngine);
        return model;
    }

    public static PictureSelectionCameraModel setCameraOpt(PictureSelectionCameraModel model, int count, double quality) {
        model
                .setMaxVideoSelectNum(count)
                .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                .isOriginalControl(false)
                .setCameraImageFormat(PictureMimeType.JPEG)
                .setCameraVideoFormat(PictureMimeType.MP4)
                .setCompressEngine(lubanEngine);
        return model;
    }

    public static PictureSelectionModel setCropOpt(PictureSelectionModel model, HashMap<String, Object> opt) {
//        model
//                .isEnableCrop(true)
//                .freeStyleCropMode(OverlayView.FREESTYLE_CROP_MODE_ENABLE)
//                .circleDimmedLayer(opt.get("cropType").equals("CropType.circle"))
//                .showCropFrame(!opt.get("cropType").equals("CropType.circle"))
//                .showCropGrid(false)
//                .rotateEnabled(true)
//                .scaleEnabled(true)
//                .isDragFrame(true)
//                .hideBottomControls(false)
//                .isMultipleSkipCrop(true)
//                .cutOutQuality((int) ((double) opt.get("quality") * 100));
//        if (opt.get("aspectRatioX") != null) {
//            model.isDragFrame(false);
//            model.withAspectRatio((int) opt.get("aspectRatioX"), (int) opt.get("aspectRatioY"));
//        }
        return model;
    }

    public static int getLanguage(String language) {
        switch (language) {
            case "Language.Chinese":
                return LanguageConfig.CHINESE;
            case "Language.ChineseTraditional":
                return LanguageConfig.TRADITIONAL_CHINESE;

            case "Language.English":
                return LanguageConfig.ENGLISH;

            case "Language.Japanese":
                return LanguageConfig.JAPAN;

            case "Language.French":
                return LanguageConfig.FRANCE;

            case "Language.Korean":
                return LanguageConfig.KOREA;

            case "Language.German":
                return LanguageConfig.GERMANY;

            case "Language.Vietnamese":
                return LanguageConfig.VIETNAM;

            default:
                return -1;
        }
    }

    /**
     * 图片压缩
     */
    public static CompressFileEngine lubanEngine = new CompressFileEngine() {
        @Override
        public void onStartCompress(Context context, ArrayList<Uri> source, OnKeyValueResultCallbackListener call) {
            Luban.with(context)
                    .load(source)
                    .ignoreBy(1024)
                    .filter(new CompressionPredicate() {
                        @Override
                        public boolean apply(String path) {
                            return !(TextUtils.isEmpty(path) || path.toLowerCase().endsWith(".gif"));
                        }
                    })
                    .setCompressListener(new OnNewCompressListener() {

                        @Override
                        public void onStart() {

                        }

                        @Override
                        public void onSuccess(String source, File compressFile) {
                            if(call != null){
                                call.onCallback(source,compressFile.getAbsolutePath());
                            }
                        }

                        @Override
                        public void onError(String source, Throwable e) {
                            if(call != null){
                                call.onCallback(source,null);
                            }
                        }
                    }).launch();
        }
    };

    /**
     * 路径转为沙盒路径
     */
    public static UriToFileTransformEngine uriTransEngine = new UriToFileTransformEngine() {
        @Override
        public void onUriToFileAsyncTransform(Context context, String srcPath, String mineType, OnKeyValueResultCallbackListener call) {
            if(call != null){
                String sandboxPath = SandboxTransformUtils.copyPathToSandbox(context, srcPath, mineType);
                call.onCallback(srcPath,sandboxPath);
            }
        }
    };
}
