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

package me.stojan.camstream.util;

import android.opengl.GLU;
import android.opengl.GLUtils;

import java.util.Locale;

/**
 * An OpenGL ES error.
 */
public class GLESErrorException extends RuntimeException {

    private final int error;

    /**
     * Construct a new OpenGL ES error exception. A default message will be generated.
     * @param error the OpenGL ES error code
     */
    public GLESErrorException(int error) {
        super(format(error, null));

        this.error = error;
    }

    /**
     * Construct a new OpenGL ES error exception with an optional message.
     * @param error the OpenGL ES error code
     * @param message an optional message
     */
    public GLESErrorException(int error, String message) {
        super(format(error, message));

        this.error = error;
    }

    private static String format(int error, String message) {
        final String gluErrorString = GLU.gluErrorString(error);

        if (null == message) {
            return String.format((Locale) null, "GLES error 0x%x (%s)", error, gluErrorString);
        } else {
            return String.format((Locale) null, "GLES error 0x%x (%s): %s", error, gluErrorString, message);
        }
    }

    /**
     * Returns the OpenGL ES error code.
     * @return the error code
     */
    public int error() {
        return error;
    }
}
