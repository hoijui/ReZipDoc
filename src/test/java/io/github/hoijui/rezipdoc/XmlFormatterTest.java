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
import org.junit.contrib.java.lang.system.SystemErrRule;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.contrib.java.lang.system.TextFromStandardInputStream;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;

/**
 * @see XmlFormatter
 */
public class XmlFormatterTest {

	@Rule
	public final ExpectedSystemExit exit = ExpectedSystemExit.none();
	@Rule
	public final TextFromStandardInputStream systemInMock
			= TextFromStandardInputStream.emptyStandardInputStream();
	@Rule
	public final SystemOutRule systemOutRule
			= new SystemOutRule().mute().enableLog();
	@Rule
	public final SystemErrRule systemErrRule
			= new SystemErrRule().mute().enableLog();

//	TODO Use System.lineSeparator();

	private void testStringPrettyPrint(final String input, final String expected) throws IOException {

		final String actual = new XmlFormatter().prettify(input);
		Assert.assertEquals(expected, actual);
	}

	private void testRoughStringPrettyPrint(final String input, final String expected) throws IOException {

		final String actual = new XmlFormatter(2, "  ", false).prettify(input);
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

	private void testPrettyPrint(final String input, final String expected) throws IOException {

		testPrettyPrintFiles(input, expected);
		testPrettyPrintFileAndStream(input, expected);
		testPrettyPrintStreams(input, expected);
	}

	private void testPrettyPrintFiles(final String input, final String expected) throws IOException {

		final File xmlInFile = createTempFile("rezipdoc-unformatted-in", input);
		final File xmlOutFile = createTempFile("rezipdoc-unformatted-out", "");

		XmlFormatter.main(new String[] {
				"--input", xmlInFile.getAbsolutePath(),
				"--output", xmlOutFile.getAbsolutePath() });

		try (InputStream resultIn = Files.newInputStream(xmlOutFile.toPath())) {
			final String actual = Utils.readStreamToString(resultIn);

			Assert.assertEquals(expected, actual);
		}
	}

	private void testPrettyPrintFileAndStream(final String input, final String expected) throws IOException {

		final File xmlInFile = createTempFile("rezipdoc-unformatted-in", input);

		systemOutRule.clearLog();
		XmlFormatter.main(new String[] { "--input", xmlInFile.getAbsolutePath() });
		final String actual = systemOutRule.getLog();

		Assert.assertEquals(expected, actual);
	}

	private void testPrettyPrintStreams(final String input, final String expected) {

		systemInMock.provideText(input);
		systemOutRule.clearLog();
		XmlFormatter.main(new String[] {});
		final String actual = systemOutRule.getLog();

		Assert.assertEquals(expected, actual);
	}

	@Test
	public void testHelp() {

		systemErrRule.clearLog();
		XmlFormatter.main(new String[] { "-h" });
		MatcherAssert.assertThat(systemErrRule.getLog(),
				CoreMatchers.startsWith("Usage examples:\n"));

		systemErrRule.clearLog();
		XmlFormatter.main(new String[] { "--help" });
		MatcherAssert.assertThat(systemErrRule.getLog(),
				CoreMatchers.startsWith("Usage examples:\n"));
	}

	@Test
	public void testBadArguments() {

		exit.expectSystemExitWithStatus(1);
		systemErrRule.clearLog();
		XmlFormatter.main(new String[] { "file1", "file2", "file3" });
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

		testPrettyPrint("<my-tag/>",
				"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n<my-tag/>\n");
	}

	@Test
	public void testFileBeginAndEndTag() throws IOException {

		testPrettyPrint("<my-tag></my-tag>",
				"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n<my-tag/>\n");
	}

	@Test
	public void testFileBeginMiddleAndEndTag() throws IOException {

		testPrettyPrint("<my-tag><middle/></my-tag>",
				"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n<my-tag>\n  <middle/>\n</my-tag>\n");
	}

	@Test
	public void testRoughSingleTag() throws IOException {

		testRoughStringPrettyPrint("<my-tag/>",
				"<my-tag/>\n");
	}

	@Test
	public void testRoughBeginAndEndTag() throws IOException {

		testRoughStringPrettyPrint("<my-tag></my-tag>",
				"<my-tag>\n</my-tag>\n");
	}
	@Test
	public void testRoughBeginMiddleAndEndTag() throws IOException {

		testRoughStringPrettyPrint("<my-tag><middle/></my-tag>",
				"<my-tag>\n  <middle/>\n</my-tag>\n");
	}
}
