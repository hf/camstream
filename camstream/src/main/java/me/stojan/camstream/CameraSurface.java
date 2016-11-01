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

import android.opengl.*;
import android.view.Surface;
import me.stojan.camstream.util.EGLUtils;

/**
 * An output surface for the camera. This is basically an EGL context with a surface, and {@link CameraRenderer} renders
 * into this EGL context and surface.
 */
public final class CameraSurface {
    /** EGL constant that makes the context / surface be "recordable." */
    public static final int EGL_RECORDABLE_ANDROID = 0x3142;

    private Surface surface;
    private EGLContainer eglContainer;

    private final static class EGLContainer {
        private final EGLDisplay eglDisplay;
        private final EGLContext eglContext;
        private final EGLSurface eglSurface;

        private EGLContainer(EGLDisplay eglDisplay, EGLContext eglContext, EGLSurface eglSurface) {
            this.eglDisplay = eglDisplay;
            this.eglContext = eglContext;
            this.eglSurface = eglSurface;
        }

        private void release() {
            EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
            EGL14.eglDestroySurface(eglDisplay, eglSurface);
            EGL14.eglDestroyContext(eglDisplay, eglContext);
            EGL14.eglReleaseThread();
            EGL14.eglTerminate(eglDisplay);
        }
    }

    private static EGLContainer setup(Surface surface, EGLContext sharedEGLContext) {
        if (null == surface) {
            throw new IllegalArgumentException("Argument surface must not be null");
        }

        if (null == sharedEGLContext) {
            sharedEGLContext = EGL14.EGL_NO_CONTEXT;
        }

        final EGLDisplay eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);

        if (eglDisplay == EGL14.EGL_NO_DISPLAY) {
            throw new RuntimeException("unable to get EGL14 display");
        }

        int[] version = new int[2];

        if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
            throw new RuntimeException("unable to initialize EGL14");
        }

        // Configure EGL for recording and OpenGL ES 2.0.
        int[] attributes = new int[] {
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL_RECORDABLE_ANDROID, 1,
                EGL14.EGL_NONE
        };

        final EGLConfig[] configs = new EGLConfig[1];
        int[] numConfigs = new int[1];
        EGL14.eglChooseConfig(eglDisplay, attributes, 0, configs, 0, configs.length, numConfigs, 0);

        EGLUtils.eglError("eglCreateContext RGB888+recordable ES2");

        // Configure context for OpenGL ES 2.0.
        attributes = new int[] {
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
        };

        final EGLContext eglContext = EGL14.eglCreateContext(eglDisplay, configs[0], sharedEGLContext, attributes, 0);

        EGLUtils.eglError("eglCreateContext");

        attributes = new int[] {
                EGL14.EGL_NONE
        };

        final EGLSurface eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, configs[0], surface, attributes, 0);

        EGLUtils.eglError("eglCreateWindowSurface");

        return new EGLContainer(eglDisplay, eglContext, eglSurface);
    }

    /**
     * Create an output surface for a camera by using the resources from an EGL context of a non-released camera
     * surface.
     * @param surface the output surface, must not be null
     * @param cameraSurface the camera surface, must not be null or be released
     * @return the new camera surface
     */
    public static CameraSurface create(Surface surface, CameraSurface cameraSurface) {
        final EGLContainer eglContainer = setup(surface, cameraSurface.eglContainer.eglContext);

        return new CameraSurface(surface, eglContainer);
    }

    /**
     * Create an output surface for a camera by using the resources from the provided shared EGL context.
     * @param surface the output surface, must not be null
     * @param sharedEGLContext the shared EGL context, must not be null (may be {@link EGL14#EGL_NO_CONTEXT}
     * @return the new camera surface
     */
    public static CameraSurface create(Surface surface, EGLContext sharedEGLContext) {
        final EGLContainer eglContainer = setup(surface, sharedEGLContext);

        return new CameraSurface(surface, eglContainer);
    }

    /**
     * Create an output surface for a camera without using any shared EGL context.
     * @param surface the output surface, must not be null
     * @return the new camera surface
     */
    public static CameraSurface create(Surface surface) {
        return create(surface, EGL14.EGL_NO_CONTEXT);
    }

    private CameraSurface(Surface surface, EGLContainer eglContainer) {
        if (null == surface) {
            throw new IllegalArgumentException("Argument surface must not be null");
        }

        this.surface = surface;
        this.eglContainer = eglContainer;
    }

    /**
     * Release the resources for this surface. It is illegal to call other methods after this and the behavior is
     * undefined.
     */
    public void release() {
        if (null != eglContainer) {
            eglContainer.release();
            eglContainer = null;
        }

        if (null != surface) {
            surface.release();
            surface = null;
        }
    }

    /**
     * Make this surface the current EGL context and surface.
     */
    public void current() {
        EGL14.eglMakeCurrent(eglContainer.eglDisplay, eglContainer.eglSurface, eglContainer.eglSurface, eglContainer.eglContext);
        EGLUtils.eglError("eglMakeCurrent");
    }

    /**
     * Publish the surface. Requires that {@link #current()} was called before.
     * @param timestamp the timestamp in nanoseconds
     */
    public void publish(long timestamp) {
        EGLExt.eglPresentationTimeANDROID(eglContainer.eglDisplay, eglContainer.eglSurface, timestamp);
        EGLUtils.eglError("eglPresentationTimeANDROID");

        EGL14.eglSwapBuffers(eglContainer.eglDisplay, eglContainer.eglSurface);
        EGLUtils.eglError("eglSwapBuffers");
    }

    /**
     * Return the surface.
     * @return the surface
     */
    public Surface surface() {
        return surface;
    }

    /**
     * Renders the camera into this surface.
     * @param renderer the camera renderer which will render into this surface, must not be null
     *
     * @throws IllegalArgumentException if renderer is null
     */
    public void render(CameraRenderer renderer) {
        if (null == renderer) {
            throw new IllegalArgumentException("Argument renderer must not be null");
        }

        current();
        renderer.update();
        renderer.draw();
        publish(renderer.timestamp());
    }
}
