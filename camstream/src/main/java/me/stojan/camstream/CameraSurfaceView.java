// Copyright (c) 2016 Stojan Dimitrovski
//
// Permission is hereby granted, free of charge, to any person obtaining a copy of
// this software and associated documentation files (the "Software"), to deal in
// the Software without restriction, including without limitation the rights to
// use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
// of the Software, and to permit persons to whom the Software is furnished to do
// so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.

package me.stojan.camstream;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;


/**
 * A {@link SurfaceView} that creates the proper EGL context and setup for a {@link CameraRenderer} to be able to render
 * into this view's surface.
 *
 * @see Callback
 */
public class CameraSurfaceView extends SurfaceView {

    /**
     * Callbacks for the {@link CameraSurface} that this view creates.
     */
    public interface Callback {
        /**
         * Called when the camera surface has been created with the proper EGL context, surface.
         *
         * @see android.view.SurfaceHolder.Callback#surfaceCreated(SurfaceHolder)
         * @param cameraSurface the camera surface, will not be null
         * @param surfaceHolder the surface holder, will not be null
         */
        void onCameraSurfaceCreated(CameraSurface cameraSurface, SurfaceHolder surfaceHolder);

        /**
         * Called when the camera surface has changed.
         *
         * @see android.view.SurfaceHolder.Callback#surfaceChanged(SurfaceHolder, int, int, int)
         * @param cameraSurface the camera surface, will not be null
         * @param surfaceHolder the surface holder, will not be null
         * @param format the surface format
         * @param width the surface width
         * @param height the surface height
         */
        void onCameraSurfaceChanged(CameraSurface cameraSurface, SurfaceHolder surfaceHolder, int format, int width, int height);

        /**
         * Called when the camera surface has been destroyed. After this function completes, {@link #cameraSurface()}
         * will return null.
         *
         * @see android.view.SurfaceHolder.Callback#surfaceDestroyed(SurfaceHolder)
         * @param cameraSurface the camera surface, will not be null
         * @param surfaceHolder the surface holder, will not be null
         */
        void onCameraSurfaceDestroyed(CameraSurface cameraSurface, SurfaceHolder surfaceHolder);
    }

    private Callback callback;
    private CameraSurface surface;

    /**
     * @see SurfaceView#SurfaceView(Context)
     */
    public CameraSurfaceView(Context context) {
        super(context);
        setup();
    }

    /**
     * @see SurfaceView#SurfaceView(Context, AttributeSet)
     */
    public CameraSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup();
    }

    /**
     * @see SurfaceView#SurfaceView(Context, AttributeSet, int)
     */
    public CameraSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup();
    }

    /**
     * @see SurfaceView#SurfaceView(Context, AttributeSet, int, int)
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CameraSurfaceView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setup();
    }

    private void setup() {
        getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                surface = CameraSurface.create(surfaceHolder.getSurface());

                final Callback callback = getCallback();

                if (null != callback) {
                    callback.onCameraSurfaceCreated(surface, surfaceHolder);
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
                final Callback callback = getCallback();

                if (null != callback) {
                    callback.onCameraSurfaceChanged(surface, surfaceHolder, i, i1, i2);
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                final Callback callback = getCallback();

                if (null != callback) {
                    callback.onCameraSurfaceDestroyed(surface, surfaceHolder);
                }

                surface.release();
                surface = null;
            }
        });
    }

    /**
     * Return the current camera surface. This will be null if the surface has not already been created.
     * @return the surface, or null
     */
    public CameraSurface cameraSurface() {
        return surface;
    }

    /**
     * Return the currently set callback.
     * @return the callback or null
     */
    public Callback getCallback() {
        return callback;
    }

    /**
     * Set the callback.
     * @param callback a callback or null
     */
    public void setCallback(Callback callback) {
        this.callback = callback;
    }
}
