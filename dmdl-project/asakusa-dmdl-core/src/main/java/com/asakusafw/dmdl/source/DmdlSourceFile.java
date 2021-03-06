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
package com.asakusafw.dmdl.source;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import com.asakusafw.utils.collections.Lists;

/**
 * DMDL source repository includes list of source files.
 */
public class DmdlSourceFile implements DmdlSourceRepository {

    private final List<File> files;

    private final Charset encoding;

    /**
     * Creates and returns a new instance.
     * @param sourceFiles  the target source files
     * @param encoding the charset of each source file
     * @throws IllegalArgumentException if some parameters were {@code null}
     */
    public DmdlSourceFile(List<File> sourceFiles, Charset encoding) {
        if (sourceFiles == null) {
            throw new IllegalArgumentException("sourceFiles must not be null"); //$NON-NLS-1$
        }
        if (encoding == null) {
            throw new IllegalArgumentException("encoding must not be null"); //$NON-NLS-1$
        }
        this.files = Lists.freeze(sourceFiles);
        this.encoding = encoding;
    }

    @Override
    public Cursor createCursor() throws IOException {
        return new FileListCursor(files.iterator(), encoding);
    }

    static class FileListCursor implements Cursor {

        private final Iterator<File> rest;

        private final Charset encoding;

        private File current;

        FileListCursor(Iterator<File> iterator, Charset encoding) {
            assert iterator != null;
            assert encoding != null;
            this.current = null;
            this.rest = iterator;
            this.encoding = encoding;
        }

        @Override
        public boolean next() throws IOException {
            if (rest.hasNext()) {
                current = rest.next();
                return true;
            } else {
                current = null;
                return false;
            }
        }

        @Override
        public URI getIdentifier() {
            if (current == null) {
                throw new NoSuchElementException();
            }
            return current.toURI();
        }

        @Override
        public Reader openResource() throws IOException {
            if (current == null) {
                throw new NoSuchElementException();
            }
            InputStream in = new FileInputStream(current);
            return new InputStreamReader(in, encoding);
        }

        @Override
        public void close() {
            current = null;
            while (rest.hasNext()) {
                rest.next();
                rest.remove();
            }
        }
    }
}
