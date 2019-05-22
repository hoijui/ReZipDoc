/*
 * Copyright (C) 2019, The authors of the ReZipDoc project.
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

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

/**
 * @see BufferedOutputStream
 */
public class BufferedOutputStreamTest {

	private static String readStreamToString(final InputStream inputStream) {

		final Scanner inScr = new Scanner(inputStream).useDelimiter("\\A");
		return inScr.hasNext() ? inScr.next() : "";
	}

	@Test
	public void testStartsWith() throws IOException {

		final BufferedOutputStream outStream = new BufferedOutputStream();
		outStream.write("hello".getBytes());

		Assert.assertTrue(outStream.startsWith("hello".getBytes()));
		Assert.assertTrue(outStream.startsWith("hel".getBytes()));
		Assert.assertTrue(outStream.startsWith("".getBytes()));
		Assert.assertFalse(outStream.startsWith("hello world".getBytes()));
	}

	@Test
	public void testCreateInputStreamWithReference() throws IOException {

		try (final BufferedOutputStream outStream = new BufferedOutputStream()) {
			outStream.write("hello".getBytes());
			try (final ByteArrayInputStream inStream = outStream.createInputStream(false)) {
				Assert.assertEquals("hello", readStreamToString(inStream));
			}
		}

		// The length of the buffer is always separate for the created InputStream,
		// thus we see the same output in this scenario
		try (final BufferedOutputStream outStream = new BufferedOutputStream()) {
			outStream.write("hello".getBytes());
			try (final ByteArrayInputStream inStream = outStream.createInputStream(false)) {
				outStream.write(" world".getBytes());
				Assert.assertEquals("hello", readStreamToString(inStream));
			}
		}

		try (final BufferedOutputStream outStream = new BufferedOutputStream()) {
			outStream.write("hello".getBytes());
			try (final ByteArrayInputStream inStream = outStream.createInputStream(false)) {
				outStream.reset();
				Assert.assertEquals("hello", readStreamToString(inStream));
			}
		}

		try (final BufferedOutputStream outStream = new BufferedOutputStream()) {
			outStream.write("hello".getBytes());
			try (final ByteArrayInputStream inStream = outStream.createInputStream(false)) {
				outStream.reset();
				outStream.write("world".getBytes());
				Assert.assertEquals("world", readStreamToString(inStream));
			}
		}
	}

	@Test
	public void testCreateInputStreamWithCopy() throws IOException {

		try (final BufferedOutputStream outStream = new BufferedOutputStream()) {
			outStream.write("hello".getBytes());
			try (final ByteArrayInputStream inStream = outStream.createInputStream(true)) {
				Assert.assertEquals("hello", readStreamToString(inStream));
			}
		}

		try (final BufferedOutputStream outStream = new BufferedOutputStream()) {
			outStream.write("hello".getBytes());
			try (final ByteArrayInputStream inStream = outStream.createInputStream(true)) {
				outStream.write(" world".getBytes());
				Assert.assertEquals("hello", readStreamToString(inStream));
			}
		}

		try (final BufferedOutputStream outStream = new BufferedOutputStream()) {
			outStream.write("hello".getBytes());
			try (final ByteArrayInputStream inStream = outStream.createInputStream(true)) {
				outStream.reset();
				Assert.assertEquals("hello", readStreamToString(inStream));
			}
		}

		try (final BufferedOutputStream outStream = new BufferedOutputStream()) {
			outStream.write("hello".getBytes());
			try (final ByteArrayInputStream inStream = outStream.createInputStream(true)) {
				outStream.reset();
				outStream.write("world".getBytes());
				Assert.assertEquals("hello", readStreamToString(inStream));
			}
		}
	}
}
