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

import android.opengl.GLUtils;

import java.util.Locale;

/**
 * An EGL error exception.
 */
public class EGLErrorException extends RuntimeException {

    private final int error;

    /**
     * Construct a new EGL error exception with an EGL error. An automatic message describing the error code will be
     * generated.
     * @param error the EGL error
     */
    public EGLErrorException(int error) {
        super(format(error, null));
        this.error = error;
    }

    /**
     * Construct a new EGL error exception with an EGL error and an optional message.
     * @param error the EGL error
     * @param message the message, may be null (an automatic message will be generated)
     */
    public EGLErrorException(int error, String message) {
        super(format(error, message));
        this.error = error;
    }

    private static String format(int error, String message) {
        if (null == message) {
            return String.format((Locale) null, "EGL error code 0x%x (%s)", error, GLUtils.getEGLErrorString(error));
        }

        return String.format((Locale) null, "EGL error code 0x%x (%s): %s", error, GLUtils.getEGLErrorString(error), message);
    }

    /**
     * Returns the EGL error associated with this exception.
     * @return the EGL error
     */
    public int error() {
        return error;
    }
}
