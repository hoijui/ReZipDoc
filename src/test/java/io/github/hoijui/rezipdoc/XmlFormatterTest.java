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

package io.github.hoijui.rezipdoc;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * @see XmlFormatter
 */
public class XmlFormatterTest {

	@Rule
	public final ExpectedSystemExit exit = ExpectedSystemExit.none();

//	TODO Use System.lineSeparator();

	private void testStringPrettyPrint(final String input, final String expected) throws IOException {

		final String actual = new XmlFormatter().prettify(input);
		Assert.assertEquals(expected, actual);
	}

	private File createTempFile(final String nameBase, final String content) throws IOException {

		final File file = File.createTempFile(nameBase, ".xml");
		file.deleteOnExit();
		try (PrintStream out = new PrintStream(Files.newOutputStream(file.toPath()))) {
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

		try (InputStream resultIn = Files.newInputStream(xmlOutFile.toPath())) {
			final String actual = Utils.readStreamToString(resultIn);

			Assert.assertEquals(expected, actual);
		}
	}

	private static String toString(final ByteArrayOutputStream buffer) {
		return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
	}

	@Test
	public void testHelp() throws IOException {

		try (ByteArrayOutputStream outBuffer = new ByteArrayOutputStream()) {
			Utils.getLogHandler().setOutputStream(outBuffer);
			XmlFormatter.main(new String[] { "-h" });
			MatcherAssert.assertThat(toString(outBuffer),
					CoreMatchers.startsWith("Usage examples:\n"));

			outBuffer.reset();
			XmlFormatter.main(new String[] { "--help" });
			MatcherAssert.assertThat(toString(outBuffer),
					CoreMatchers.startsWith("Usage examples:\n"));
		} finally {
			Utils.getLogHandler().setOutputStream(System.err);
		}
	}

	@Test
	public void testTooManyArguments() throws IOException {

		exit.expectSystemExitWithStatus(1);
		try (BufferedOutputStream outBuffer = new BufferedOutputStream()) {
			Utils.getLogHandler().setOutputStream(outBuffer);
			XmlFormatter.main(new String[] { "file1", "file2", "file3" });
			MatcherAssert.assertThat(new String(outBuffer.toByteArray()),
					CoreMatchers.containsString("Usage:"));
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
