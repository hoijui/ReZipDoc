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

package io.github.hoijui.rezipdoc;

import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;

/**
 * @see ReZip
 */
@SuppressWarnings("WeakerAccess")
public class ReZipTest extends AbstractReZipDocTest {

	protected Path reZipFile;

	@Before
	public void setUp() throws IOException {

		super.setUp();
		reZipFile = Files.createTempFile(getClass().getName() + "_filtered_", ".zip");
	}

	@After
	public void tearDown() {

		super.tearDown();
		reZipFile.toFile().delete();
	}

	private void runReZip(final boolean compression, final boolean nullifyTimes,
			final boolean recursive, final boolean formatXml,
			final Path zipFile, final Path reZipFile)
			throws IOException
	{
		// call the internal function directly
//		new ReZip(compression, nullifyTimes, recursive, formatXml)
//				.reZip(zipFile, reZipFile);

		// call main method (this tests more code, and uses the class
		// nearly as it wil be used as a git filter
		final List<String> mainArgs = new LinkedList<>();
		if (compression) {
			mainArgs.add("--compressed");
		}
		if (nullifyTimes) {
			mainArgs.add("--nullify-times");
		}
		if (!recursive) {
			mainArgs.add("--non-recursive");
		}
		if (formatXml) {
			mainArgs.add("--format-xml");
		}
		final InputStream inBefore = System.in;
		final PrintStream outBefore = System.out;
		try (final InputStream tempIn = new FileInputStream(zipFile.toFile());
				final PrintStream tempOut = new PrintStream(new FileOutputStream(reZipFile.toFile())))
		{
			System.setIn(tempIn);
			System.setOut(tempOut);
			ReZip.main(mainArgs.toArray(new String[0]));
		} finally {
			System.setIn(inBefore);
			System.setOut(outBefore);
		}
	}

	private void testRecursive(final boolean recursive) throws IOException {

		// This is the original, compressed file
		createRecursiveZip(zipFile, projectRoot, archiveContents, ZipEntry.DEFLATED);

		// This creates the uncompressed file
		runReZip(false, false, recursive, false, zipFile, reZipFile);

		// Test whether the filtered ZIP file does (not) contain the original content
		// placed in a sub-ZIP file in plain text
		checkContains(recursive, reZipFile, archiveContents.subList(0, 2));
		// Test whether the filtered ZIP file contains the directly embedded original content
		// in plain text
		checkContains(true, reZipFile, archiveContents.subList(2, archiveContents.size()));
	}

	private void testPlainText(final boolean plainText) throws IOException {

		// This is the original, compressed file
		createZip(zipFile, projectRoot, archiveContents, ZipEntry.DEFLATED);

		// This creates the *still compressed* file
		runReZip(!plainText, false, true, false, zipFile, reZipFile);

		// Test whether the filtered ZIP file does (not) contain the original content in plain text
		checkContains(plainText, reZipFile, archiveContents);
	}

	@Test
	public void testNonRecursive() throws IOException {
		testRecursive(false);
	}

	@Test
	public void testRecursive() throws IOException {
		testRecursive(true);
	}

	@Test
	public void testContentsNotVisibleInFullInPlainText() throws IOException {
		testPlainText(false);
	}

	@Test
	public void testContentsVisibleInFullInPlainText() throws IOException {
		testPlainText(true);
	}

	@Test
	public void testHelp() throws IOException {

		try (final BufferedOutputStream outBuffer = new BufferedOutputStream()) {
			Utils.getLogHandler().setOutputStream(outBuffer);
			ReZip.main(new String[] { "-h" });
			Assert.assertThat(new String(outBuffer.toByteArray()),
					CoreMatchers.startsWith("Usage:"));

			outBuffer.reset();
			ReZip.main(new String[] { "--help" });
			Assert.assertThat(new String(outBuffer.toByteArray()),
					CoreMatchers.startsWith("Usage:"));
		} finally {
			Utils.getLogHandler().setOutputStream(System.err);
		}
	}

	@Test
	public void testInvalidArgument() throws IOException {

		exit.expectSystemExitWithStatus(1);
		ReZip.main(new String[] { "-invalid-argument" });
	}
}
