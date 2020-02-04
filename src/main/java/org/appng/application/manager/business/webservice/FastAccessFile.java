/*
 * Copyright 2011-2020 the original author or authors.
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
package org.appng.application.manager.business.webservice;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.NoSuchElementException;

class FastAccessFile {

	public static final char LF = '\n';
	private ByteBuffer cb;
	private long size;

	public FastAccessFile(File file) throws IOException {
		try (FileInputStream is = new FileInputStream(file); FileChannel fc = is.getChannel();) {
			size = fc.size();
			cb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
		}
	}

	public Iterator<String> tail(long lines) {
		long cnt = 0;
		long i = 0;
		for (i = size - 1; i >= 0; i--) {
			if (cb.get((int) i) == LF) {
				cnt++;
				if (cnt == lines + 1)
					break;
			}
		}
		return new LineIterator((int) i + 1);
	}

	private class LineIterator implements Iterator<String> {
		private static final char CR = '\r';
		private int offset;

		public LineIterator(int offset) {
			this.offset = offset;
		}

		public boolean hasNext() {
			return offset < cb.limit();
		}

		public String next() {
			if (offset >= cb.limit()) {
				throw new NoSuchElementException();
			}
			ByteArrayOutputStream sb = new ByteArrayOutputStream();
			for (; offset < cb.limit(); offset++) {
				byte c = (cb.get(offset));
				if (c == LF) {
					offset++;
					break;
				}
				if (c != CR) {
					sb.write(c);
				}
			}
			try {
				return sb.toString(StandardCharsets.UTF_8.name());
			} catch (UnsupportedEncodingException e) {
			}
			return sb.toString();
		}

		@Override
		public void remove() {
			// not needed
		}
	}

}
