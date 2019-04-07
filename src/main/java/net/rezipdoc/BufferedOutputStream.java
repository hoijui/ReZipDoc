/*
 * Copyright (C) 2015-2019, The authors of the ReZipDoc project.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.rezipdoc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * A simple wrapper around {@link ByteArrayOutputStream}
 * that adds some (partly unsafe) methods,
 * mainly to avoid excessive copying of memory.
 */
@SuppressWarnings("WeakerAccess")
public class BufferedOutputStream extends ByteArrayOutputStream {

	/**
	 * Creates a new byte array output stream. The buffer capacity is
	 * initially 256 bytes, though its size increases if necessary.
	 */
	public BufferedOutputStream() {
		this(256);
	}

	/**
	 * Creates a new byte array output stream, with a buffer capacity of
	 * the specified size, in bytes.
	 *
	 * @param   size   the initial size.
	 * @exception  IllegalArgumentException if size is negative.
	 */
	public BufferedOutputStream(final int size) {
		super(size);
	}

	/**
	 * Tests if this buffer starts with the specified prefix.
	 *
	 * @param   prefix   the prefix.
	 * @return  {@code true} if the byte sequence represented by the
	 *          argument is a prefix of the byte sequence stored in
	 *          this buffer; {@code false} otherwise.
	 *          Note also that {@code true} will be returned if the
	 *          argument is an empty sequence.
	 */
	public boolean startsWith(final byte[] prefix) {

		boolean startsWith = true;
		if (prefix.length > count) {
			startsWith = false;
		} else {
			int idx = prefix.length - 1;
			while (idx >= 0) {
				if (buf[idx] != prefix[idx]) {
					startsWith = false;
					break;
				}
				idx--;
			}
		}
		return startsWith;
	}

	/**
	 * Creates an {@code InputStream} streaming the data of this buffer.
	 * CAUTION If {@code copyBytes} is false, do not add data to this buffer
	 *   while the stream is still in use!
	 *
	 * @param copyBytes whether to copy the internal data into the stream,
	 *   or just reference it.
	 *   If this is {@code false}, then the behavior of the created stream
	 *   in case of data being added to the buffer after the streams creations
	 *   is undefined.
	 * @return an {@code InputStream} using the same data as this buffer.
	 */
	public ByteArrayInputStream createInputStream(final boolean copyBytes) {

		final ByteArrayInputStream inStream;
		if (copyBytes) {
			inStream = new ByteArrayInputStream(toByteArray());
		} else {
			inStream = new ByteArrayInputStream(buf, 0, count);
		}

		return inStream;
	}
}
