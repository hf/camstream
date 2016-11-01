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

import android.opengl.EGL14;

/**
 * Contains EGL utilities.
 */
public final class EGLUtils {

    private EGLUtils() {
        // No-op.
    }

    /**
     * Check if an EGL error has occured. If it has, it throws an {@link EGLErrorException}.
     * @param format the format/message, may be null (but values will be ignored then)
     * @param values the values
     *
     * @throws EGLErrorException if there is an EGL error
     */
    public static void eglError(String format, Object... values) throws EGLErrorException {
        final int error = EGL14.eglGetError();

        if (EGL14.EGL_SUCCESS != error) {
            final String message;

            if (null == format || null == values || 0 == values.length) {
                message = format;
            } else {
                message = String.format(null, format, values);
            }

            throw new EGLErrorException(error, message);
        }
    }
}
