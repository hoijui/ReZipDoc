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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

	private void testRecursive(final boolean recursive) throws IOException {

		// This is the original, compressed file
		createRecursiveZip(zipFile, projectRoot, archiveContents, ZipEntry.DEFLATED);
		// This creates the uncompressed file
		new ReZip(false, false, recursive).reZip(zipFile, reZipFile);
		// Test whether the filtered ZIP file does (not) contain the original content
		// placed in a sub-ZIP file in plain text
		checkContains(recursive, reZipFile, archiveContents.subList(0, 2));
		// Test whether the filtered ZIP file contains the directly embedded original content
		// in plain text
		checkContains(true, reZipFile, archiveContents.subList(2, archiveContents.size()));
	}

	@Test
	public void testNonRecursive() throws IOException {
		testRecursive(false);
	}

	@Test
	public void testRecursive() throws IOException {
		testRecursive(true);
	}

	private void testPlainText(final boolean plainText) throws IOException {

		// This is the original, compressed file
		createZip(zipFile, projectRoot, archiveContents, ZipEntry.DEFLATED);
		// This creates the *still compressed* file
		new ReZip(!plainText, false, true).reZip(zipFile, reZipFile);
		// Test whether the filtered ZIP file does (not) contain the original content in plain text
		checkContains(plainText, reZipFile, archiveContents);
	}

	@Test
	public void testContentsNotVisibleInFullInPlainText() throws IOException {
		testPlainText(false);
	}

	@Test
	public void testContentsVisibleInFullInPlainText() throws IOException {
		testPlainText(true);
	}
}
