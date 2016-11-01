package me.stojan.camstream.example.activity;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.Matrix;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import me.stojan.camstream.CameraRenderer;
import me.stojan.camstream.CameraSurface;
import me.stojan.camstream.CameraSurfaceView;
import me.stojan.camstream.util.CameraFunction1;

import java.io.IOException;

/**
 * A simple CameraFragment that shows a {@link CameraSurfaceView}.
 */
public class CameraFragment extends Fragment {

    Camera camera;
    CameraRenderer cameraRenderer;
    CameraSurfaceView cameraSurfaceView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        cameraSurfaceView = new CameraSurfaceView(getActivity());
        cameraSurfaceView.setCallback(new CameraSurfaceView.Callback() {
            @Override
            public void onCameraSurfaceCreated(final CameraSurface cameraSurface, final SurfaceHolder surfaceHolder) {
                cameraSurface.current();

                if (null != camera) {
                    camera.release();
                }

                if (null != cameraRenderer) {
                    cameraRenderer.release();
                }

                cameraRenderer = new CameraRenderer();
                cameraRenderer.updateModelViewProjectionMatrix(new CameraFunction1<float[], float[]>() {
                    @Override
                    public float[] apply(float[] parameter) {
                        Matrix.rotateM(parameter, 0, 45, 0, 0, 1);
                        return parameter;
                    }
                });


                camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
                camera.getParameters().setRecordingHint(true);
                try {
                    camera.setPreviewTexture(cameraRenderer.surfaceTexture());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                camera.startPreview();

                cameraRenderer.surfaceTexture().setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                    @Override
                    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                        cameraSurface.render(cameraRenderer);
                    }
                });
            }

            @Override
            public void onCameraSurfaceChanged(CameraSurface cameraSurface, SurfaceHolder surfaceHolder, int format, int width, int height) {

            }

            @Override
            public void onCameraSurfaceDestroyed(CameraSurface cameraSurface, SurfaceHolder surfaceHolder) {
                if (null != camera) {
                    camera.release();
                    camera = null;
                }

                cameraRenderer.surfaceTexture().setOnFrameAvailableListener(null);
            }
        });

        return cameraSurfaceView;
    }

    @Override
    public void onPause() {
        super.onPause();

        if (null != camera) {
            camera.release();
            camera = null;
        }
    }
}
