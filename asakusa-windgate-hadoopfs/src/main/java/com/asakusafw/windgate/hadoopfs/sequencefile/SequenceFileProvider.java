/**
 * Copyright 2011 Asakusa Framework Team.
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
package com.asakusafw.windgate.hadoopfs.sequencefile;

import java.io.Closeable;
import java.io.IOException;

import org.apache.hadoop.io.SequenceFile;

/**
 * Provides sequence file list.
 * @since 0.2.2
 */
public interface SequenceFileProvider extends Closeable {

    /**
     * Returns true iff the next sequence file exists,
     * and then the {@link #open()} method returns it.
     * @return {@code true} if the next data model object exists, otherwise {@code false}
     * @throws IOException if failed to prepare the next data
     */
    boolean next() throws IOException;

    /**
     * Opens the current sequence file prepared by the {@link #next()} method.
     * This operation can perform only once for each sequence file.
     * @return the current sequence file contents
     * @throws IOException if failed to open the file
     */
    SequenceFile.Reader open() throws IOException;
}
