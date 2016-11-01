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

import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import me.stojan.camstream.util.CameraFunction1;
import me.stojan.camstream.util.GLESUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.Locale;

/**
 * Renders an image from the Camera into the current EGL context.
 */
public final class CameraRenderer {

    private static final int FLOAT_SIZE_BYTES = 4;
    private static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES;
    private static final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;
    private static final int TRIANGLE_VERTICES_DATA_UV_OFFSET = 3;

    private final float[] triangleVerticesData = {
            // X, Y, Z, U, V
            -1.0f, -1.0f, 0, 0.f, 0.f,
             1.0f, -1.0f, 0, 1.f, 0.f,
            -1.0f,  1.0f, 0, 0.f, 1.f,
             1.0f,  1.0f, 0, 1.f, 1.f,
    };

    private final FloatBuffer triangleVerticesBuffer;

    private static final String VERTEX_SHADER =
            "uniform mat4 uMVPMatrix;\n" +
            "uniform mat4 uSTMatrix;\n" +
            "attribute vec4 aPosition;\n" +
            "attribute vec4 aTextureCoord;\n" +
            "varying vec2 vTextureCoord;\n" +
            "void main() {\n" +
            "    gl_Position = uMVPMatrix * aPosition;\n" +
            "    vTextureCoord = (uSTMatrix * aTextureCoord).xy;\n" +
            "}\n";

    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;\n" +
            "varying vec2 vTextureCoord;\n" +
            "uniform samplerExternalOES sTexture;\n" +
            "void main() {\n" +
            "    gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
            "}\n";

    private volatile float[] mvpMatrix = new float[16];
    private volatile float[] stMatrix = new float[16];

    private volatile float[] clearColor = new float[] { 0f, 1f, 0f, 1f };

    private int glesProgram;
    private int mvpMatrixHandle;
    private int stMatrixHandle;
    private int positionHandle;
    private int textureHandle;

    private volatile int textureId = Integer.MIN_VALUE;
    private volatile SurfaceTexture surfaceTexture;

    /**
     * Create a new renderer. This constructor uses OpenGL ES 2.0 calls and therefore it must be called with a valid
     * EGL context and surface to work.
     */
    public CameraRenderer() {
        triangleVerticesBuffer = ByteBuffer.allocateDirect(
                triangleVerticesData.length * FLOAT_SIZE_BYTES)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();

        triangleVerticesBuffer.put(triangleVerticesData).position(0);

        Matrix.setIdentityM(stMatrix, 0);
        Matrix.setIdentityM(mvpMatrix, 0);

        setup();
    }

    private void setup() {
        glesProgram = createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        positionHandle = glAttributeLocation(glesProgram, "aPosition");
        textureHandle = glAttributeLocation(glesProgram, "aTextureCoord");

        mvpMatrixHandle = glUniformLocation(glesProgram, "uMVPMatrix");
        stMatrixHandle =  glUniformLocation(glesProgram, "uSTMatrix");

        final int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);

        textureId = textures[0];
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
        GLESUtils.glError("glBindTexture textureId");

        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);
        GLESUtils.glError("glTexParameter");

        surfaceTexture = new SurfaceTexture(textureId);
    }

    /**
     * Return the {@link #surfaceTexture()}'s presentation timestamp.
     * @return the timestamp in nanoseconds
     */
    public long timestamp() {
        return surfaceTexture.getTimestamp();
    }

    /**
     * Update the texture from {@link #surfaceTexture()}.
     */
    public void update() {
        surfaceTexture.updateTexImage();
    }

    /**
     * Set the clear color.
     * @param color an Android {@link Color} integer
     */
    public void clearColor(int color) {
        clearColor = new float[] {
                ((float) Color.red(color))   / 256f,
                ((float) Color.green(color)) / 256f,
                ((float) Color.blue(color))  / 256f,
                ((float) Color.alpha(color)) / 256f
        };
    }

    /**
     * Draw the {@link #surfaceTexture()} onto the current EGL context with surface.
     */
    public void draw() {
        GLESUtils.glError("onDrawFrame start");
        surfaceTexture.getTransformMatrix(stMatrix);

        final float[] clearColor = this.clearColor;

        GLES20.glClearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3]);
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glUseProgram(glesProgram);
        GLESUtils.glError("glUseProgram");

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);

        triangleVerticesBuffer.position(TRIANGLE_VERTICES_DATA_POS_OFFSET);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES, triangleVerticesBuffer);
        GLESUtils.glError("glVertexAttribPointer maPosition");
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLESUtils.glError("glEnableVertexAttribArray positionHandle");

        triangleVerticesBuffer.position(TRIANGLE_VERTICES_DATA_UV_OFFSET);
        GLES20.glVertexAttribPointer(textureHandle, 2, GLES20.GL_FLOAT, false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES, triangleVerticesBuffer);
        GLESUtils.glError("glVertexAttribPointer textureHandle");
        GLES20.glEnableVertexAttribArray(textureHandle);
        GLESUtils.glError("glEnableVertexAttribArray textureHandle");

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);
        GLES20.glUniformMatrix4fv(stMatrixHandle, 1, false, stMatrix, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLESUtils.glError("glDrawArrays");

        // IMPORTANT: on some devices, if you are sharing the external texture between two
        // contexts, one context may not see updates to the texture unless you un-bind and
        // re-bind it.  If you're not using shared EGL contexts, you don't need to bind
        // texture 0 here.
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
    }

    /**
     * Returns the texture ID of the OpenGL ES texture.
     * @return the id, or {@link Integer#MIN_VALUE} if {@link #release()} has been called
     */
    public int textureId() {
        return textureId;
    }

    /**
     * Returns the surface texture.
     * @return the surface texture, or null if {@link #release()} has been called
     */
    public SurfaceTexture surfaceTexture() {
        return surfaceTexture;
    }

    /**
     * Update the model-view-projection matrix for the next {@link #draw()}. It is important for the updater to return
     * a non-null, 16-element (4x4) array. If this is not the case, the call will fail with an exception. It is OK to
     * return and operate on the provided array.
     * <p>
     * The update operation is decoupled from the drawing pass, meaning that if the update finishes after the draw,
     * then this will have no effect.
     * @param updater the updater function, must not be null
     */
    public void updateModelViewProjectionMatrix(CameraFunction1<float[], float[]> updater) {
        if (null == updater) {
            throw new IllegalArgumentException("Argument updater is null");
        }

        final float[] mvp = mvpMatrix;
        final float[] copyOfMVP = Arrays.copyOf(mvp, mvp.length);

        final float[] update = updater.apply(copyOfMVP);

        if (null == update) {
            throw new RuntimeException("Updater must not return null");
        }

        if (16 != update.length) {
            throw new RuntimeException("Updater must return a 4x4 matrix");
        }

        mvpMatrix = update;
    }

    /**
     * Update the texture matrix (UV). It is important for the updater to return a non-null, 16-element (4x4) array. If
     * this is not the case, the call will fail with an exception. It is OK to return and operate on the provided array.
     * <p>
     * The update operation is decoupled from the drawing pass, meaning that if the update finishes after the draw,
     * then this will have no effect.
     * @param updater the updater function, must not be null
     */
    public void updateTextureMatrix(CameraFunction1<float[], float[]> updater) {
        if (null == updater) {
            throw new IllegalArgumentException("Argument updater is null");
        }

        final float[] st = stMatrix;
        final float[] copyST = Arrays.copyOf(st, st.length);

        final float[] update = updater.apply(copyST);

        if (null == update) {
            throw new RuntimeException("Updater must not return null");
        }

        if (16 != update.length) {
            throw new RuntimeException("Updater must return a 4x4 matrix");
        }

        stMatrix = update;
    }

    /**
     * Release the surface texture. It is illegal to call {@link #draw()} or any other function after this call and the
     * behavior of this object is unspecified.
     */
    public void release() {
        textureId = Integer.MIN_VALUE;
        surfaceTexture.release();
        surfaceTexture = null;
    }

    private int loadShader(int shaderType, String source) {
        final int shader = GLES20.glCreateShader(shaderType);
        GLESUtils.glError("glCreateShader(%d)", shaderType);

        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);

        final int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);

        if (0 == compiled[0]) {
            GLES20.glDeleteShader(shader);
            throw new RuntimeException(String.format((Locale) null, "Could not compile shader type %d: %s", shaderType, GLES20.glGetShaderInfoLog(shader)));
        }

        return shader;
    }

    private int createProgram(String vertexSource, String fragmentSource) {
        final int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        final int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);

        final int program = GLES20.glCreateProgram();

        if (0 == program) {
            throw new RuntimeException("Unable to create GLES20 program");
        }

        GLES20.glAttachShader(program, vertexShader);
        GLESUtils.glError("glAttachShader(program, vertexShader)");

        GLES20.glAttachShader(program, fragmentShader);
        GLESUtils.glError("glAttachShader(program, fragmentShader)");

        GLES20.glLinkProgram(program);

        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);

        if (GLES20.GL_TRUE != linkStatus[0]) {
            GLES20.glDeleteProgram(program);
            throw new RuntimeException(String.format((Locale) null, "Unable to link GLES20 program: %s", GLES20.glGetProgramInfoLog(program)));
        }

        return program;
    }

    private static int glAttributeLocation(int program, String attribute) {
        final int label = GLES20.glGetAttribLocation(program, attribute);

        if (label < 0) {
            throw new RuntimeException(String.format((Locale) null, "Unable to find attribute '%s' in program %d", attribute, program));
        }

        return label;
    }

    private static int glUniformLocation(int program, String locationName) {
        final int location = GLES20.glGetUniformLocation(program, locationName);

        if (location < 0) {
            throw new RuntimeException(String.format((Locale) null, "Unable to find uniform location '%s' in program %d", locationName, program));
        }

        return location;
    }

}
