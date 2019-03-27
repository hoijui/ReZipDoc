/*
 * Copyright (C) 2015-2019,
 * Carl Osterwisch <costerwi@gmail.com>,
 * Robin Vobruba <hoijui.quaero@gmail.com>.
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
	public BufferedOutputStream(int size) {
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

		if (prefix.length > count) {
			return false;
		}
		int idx = prefix.length;
		while (--idx >= 0) {
			if (buf[idx] != prefix[idx]) {
				return false;
			}
		}
		return true;
	}

	public ByteArrayInputStream createInputStream(final boolean copyBytes) {

		final ByteArrayInputStream inStream;
		if (copyBytes) {
			inStream = new ByteArrayInputStream(toByteArray());
		} else {
			inStream = new ByteArrayInputStream(buf, 0, count);
		}

		return inStream;
	}

	public ByteArrayInputStream createInputStream() {
		return createInputStream(false);
	}
}