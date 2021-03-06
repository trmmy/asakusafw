/**
 * Copyright 2011-2012 Asakusa Framework Team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.asakusafw.runtime.value;

import java.io.IOException;

import org.apache.hadoop.io.WritableComparator;

final class ByteArrayUtil {

    static int compare(int a, int b) {
        if (a == b) {
            return 0;
        }
        if (a < b) {
            return -1;
        }
        return +1;
    }

    static int compare(long a, long b) {
        if (a == b) {
            return 0;
        }
        if (a < b) {
            return -1;
        }
        return +1;
    }

    static short readShort(byte[] bytes, int offset) {
        return (short) WritableComparator.readUnsignedShort(bytes, offset);
    }

    static int readInt(byte[] bytes, int offset) {
        return WritableComparator.readInt(bytes, offset);
    }

    static long readLong(byte[] bytes, int offset) {
        return WritableComparator.readLong(bytes, offset);
    }

    static long readVLong(byte[] bytes, int offset) {
        try {
            return WritableComparator.readVLong(bytes, offset);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private ByteArrayUtil() {
        return;
    }
}
