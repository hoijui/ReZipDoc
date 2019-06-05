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
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.junit.Test;

import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;

/**
 * @see ZipDoc
 */
public class ZipDocTest extends AbstractReZipDocTest {

	private void testRecursive(final boolean recursive) throws IOException {

		// This is the original, compressed file
		createRecursiveZip(zipFile, projectRoot, archiveContents, ZipEntry.DEFLATED);

		// This creates the uncompressed file
		final BufferedOutputStream bufferedOutputStream = new BufferedOutputStream();
//		new ZipDoc(recursive, false).transform(
//				new ZipInputStream(Files.newInputStream(zipFile)),
//				new PrintStream(bufferedOutputStream));
		final List<String> mainArgs = new LinkedList<>();
		if (!recursive) {
			mainArgs.add("--non-recursive");
		}
		mainArgs.add(zipFile.toFile().getAbsolutePath());
		final PrintStream outBefore = System.out;
		try (PrintStream tempOut = new PrintStream(bufferedOutputStream)) {
			System.setOut(tempOut);
			ZipDoc.main(mainArgs.toArray(new String[0]));
			System.setOut(outBefore);
		}

		// Test whether the filtered ZIP file does (not) contain the original content
		// placed in a sub-ZIP file in plain text
		checkContains(recursive, bufferedOutputStream, archiveContents.subList(0, 2));
		// Test whether the filtered ZIP file contains the directly embedded original content
		// in plain text
		checkContains(true, bufferedOutputStream, archiveContents.subList(2, archiveContents.size()));
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
	public void testHelp() throws IOException {

		final Matcher<String> helpMatchers = CoreMatchers.allOf(
				CoreMatchers.startsWith(ZipDoc.class.getSimpleName()),
				CoreMatchers.containsString("License:"),
				CoreMatchers.containsString("Usage:"),
				CoreMatchers.containsString("Examples:"));

		try (BufferedOutputStream outBuffer = new BufferedOutputStream()) {
			Utils.getLogHandler().setOutputStream(outBuffer);
			ZipDoc.main(new String[] { "-h" });
			MatcherAssert.assertThat(new String(outBuffer.toByteArray()), helpMatchers);

			outBuffer.reset();
			ZipDoc.main(new String[] { "--help" });
			MatcherAssert.assertThat(new String(outBuffer.toByteArray()), helpMatchers);
		} finally {
			Utils.getLogHandler().setOutputStream(System.err);
		}
	}

	@Test
	public void testNoArgs() throws IOException {

		exit.expectSystemExitWithStatus(1);
		try (BufferedOutputStream outBuffer = new BufferedOutputStream()) {
			Utils.getLogHandler().setOutputStream(outBuffer);
			ZipDoc.main(new String[] {});
		} finally {
			Utils.getLogHandler().setOutputStream(System.err);
		}
	}

	@Test
	public void testInvalidArgument() throws IOException {

		exit.expectSystemExitWithStatus(1);
		try (BufferedOutputStream outBuffer = new BufferedOutputStream()) {
			Utils.getLogHandler().setOutputStream(outBuffer);
			ReZip.main(new String[] { "-invalid-argument", "theZipFile" });
		} finally {
			Utils.getLogHandler().setOutputStream(System.err);
		}
	}
}
