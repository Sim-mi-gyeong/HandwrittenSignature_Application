package com.me.handwrittensignature;

import android.content.Context;
import android.content.res.Configuration;
import android.media.CamcorderProfile;
import android.util.DisplayMetrics;
import android.view.WindowManager;

class RecordingConfig {
    private static final int[] CAMCORDER_PROFILES={
            CamcorderProfile.QUALITY_2160P,
            CamcorderProfile.QUALITY_1080P,
            CamcorderProfile.QUALITY_720P,
            CamcorderProfile.QUALITY_480P,
            CamcorderProfile.QUALITY_CIF,
            CamcorderProfile.QUALITY_QVGA,
            CamcorderProfile.QUALITY_QCIF
    };

    final int width;
    final int height;
    final int frameRate;
    final int bitRate;
    final int density;

    RecordingConfig(Context ctxt) {
        DisplayMetrics metrics=new DisplayMetrics();
        WindowManager wm=(WindowManager)ctxt.getSystemService(Context.WINDOW_SERVICE);

        wm.getDefaultDisplay().getRealMetrics(metrics);

        density=metrics.densityDpi;

        Configuration cfg=ctxt.getResources().getConfiguration();

        boolean isLandscape=
                (cfg.orientation==Configuration.ORIENTATION_LANDSCAPE);

        CamcorderProfile selectedProfile=null;

        for (int profileId : CAMCORDER_PROFILES) {
            CamcorderProfile profile=null;

            try {
                profile=CamcorderProfile.get(profileId);
            }
            catch (Exception e) {
                // not documented to throw anything, but does
            }

            if (profile!=null) {
                if (selectedProfile==null) {
                    selectedProfile=profile;
                }
                else if (profile.videoFrameWidth>=metrics.widthPixels &&
                        profile.videoFrameHeight>=metrics.heightPixels) {
                    selectedProfile=profile;
                }
            }
        }

        if (selectedProfile==null) {
            throw new IllegalStateException("No CamcorderProfile available!");
        }
        else {
            frameRate=selectedProfile.videoFrameRate;
            bitRate=selectedProfile.videoBitRate;

            int targetWidth, targetHeight;

            if (isLandscape) {
                targetWidth=selectedProfile.videoFrameWidth;
                targetHeight=selectedProfile.videoFrameHeight;
            }
            else {
                targetWidth=selectedProfile.videoFrameHeight;
                targetHeight=selectedProfile.videoFrameWidth;
            }

            if (targetWidth>=metrics.widthPixels &&
                    targetHeight>=metrics.heightPixels) {
                width=metrics.widthPixels;
                height=metrics.heightPixels;
            }
            else {
                if (isLandscape) {
                    width=targetHeight*metrics.widthPixels/metrics.heightPixels;
                    height=targetHeight;
                }
                else {
                    width=targetWidth;
                    height=targetWidth*metrics.heightPixels/metrics.widthPixels;
                }
            }
        }
    }
}