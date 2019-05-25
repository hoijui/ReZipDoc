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

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * @see XmlFormatter
 */
public class XmlFormatterTest {

//	TODO Use System.lineSeparator();

	private void testStringPrettyPrint(final String input, final String expected) throws IOException {

		final String actual = new XmlFormatter().prettify(input);
		Assert.assertEquals(expected, actual);
	}

	private File createTempFile(final String nameBase, final String content) throws IOException {

		final File file = File.createTempFile(nameBase, ".xml");
		file.deleteOnExit();
		try (final PrintStream out = new PrintStream(new FileOutputStream(file))) {
			out.print(content);
		}
		return file;
	}

	private void testFilePrettyPrint(final String input, final String expected) throws IOException {

		final File xmlInFile = createTempFile("rezipdoc-unformatted-in", input);
		final File xmlOutFile = createTempFile("rezipdoc-unformatted-out", "");

		XmlFormatter.main(new String[] {
				xmlInFile.getAbsolutePath(),
				xmlOutFile.getAbsolutePath() });

		try (final FileInputStream resultIn = new FileInputStream(xmlOutFile)) {
			final String actual = Utils.readStreamToString(resultIn);

			Assert.assertEquals(expected, actual);
		}
	}

	private static String toString(final ByteArrayOutputStream buffer) {
		return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
	}

	@Test
	public void testHelp() throws IOException {

		try (final ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
				final PrintStream out = new PrintStream(outBuffer))
		{
			Utils.getLogHandler().setOutputStream(out);

			XmlFormatter.main(new String[] { "-h" });
			out.flush();
			Assert.assertThat(toString(outBuffer),
					CoreMatchers.startsWith("Usage examples:\n"));

			outBuffer.reset();
			XmlFormatter.main(new String[] { "--help" });
			out.flush();
			Assert.assertThat(toString(outBuffer),
					CoreMatchers.startsWith("Usage examples:\n"));
		} finally {
			Utils.getLogHandler().setOutputStream(System.err);
		}
	}

	@Test
	public void testSingleTag() throws IOException {

		testStringPrettyPrint("<my-tag/>",
				"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n<my-tag/>\n");
	}

	@Test
	public void testBeginAndEndTag() throws IOException {

		testStringPrettyPrint("<my-tag></my-tag>",
				"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n<my-tag/>\n");
	}

	@Test
	public void testBeginMiddleAndEndTag() throws IOException {

		testStringPrettyPrint("<my-tag><middle/></my-tag>",
				"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n<my-tag>\n  <middle/>\n</my-tag>\n");
	}

	@Test
	public void testFileSingleTag() throws IOException {

		testFilePrettyPrint("<my-tag/>",
				"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n<my-tag/>\n");
	}

	@Test
	public void testFileBeginAndEndTag() throws IOException {

		testFilePrettyPrint("<my-tag></my-tag>",
				"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n<my-tag/>\n");
	}

	@Test
	public void testFileBeginMiddleAndEndTag() throws IOException {

		testFilePrettyPrint("<my-tag><middle/></my-tag>",
				"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n<my-tag>\n  <middle/>\n</my-tag>\n");
	}
}
