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

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * @see Utils
 */
public class UtilsTest extends AbstractReZipDocTest {

	@Test
	public void fileWriteAndReadLines() throws IOException {

		final List<String> linesExpectedUnfiltered = Arrays.asList("line 1", "# line2", "", "4th line");
		final List<String> linesExpectedFiltered = Arrays.asList("line 1", "4th line");

		final Path tmpFile = Files.createTempFile(getClass().getName() + "_filtered_", ".zip");
		tmpFile.toFile().deleteOnExit();

		Utils.writeLines(tmpFile, linesExpectedUnfiltered);

		final List<String> linesActualUnfiltered = Utils.readLines(tmpFile, false);
		Assert.assertArrayEquals(linesExpectedUnfiltered.toArray(), linesActualUnfiltered.toArray());

		final List<String> linesActualFiltered = Utils.readLines(tmpFile, true);
		Assert.assertArrayEquals(linesExpectedFiltered.toArray(), linesActualFiltered.toArray());
	}

	@Test
	public void writeAndDeleteSuffixFiles() throws IOException, URISyntaxException {

		Utils.writeSuffixesFiles();
		Set<String> loadedSuffixesXml = Utils.collectFileOrDefaults(
				Utils.RESOURCE_FILE_SUFFIXES_XML, Utils.DEFAULT_SUFFIXES_XML);
		Set<String> loadedSuffixesText = Utils.collectFileOrDefaults(
				Utils.RESOURCE_FILE_SUFFIXES_TEXT, Utils.DEFAULT_SUFFIXES_TEXT);
		Set<String> loadedSuffixesArchive = Utils.collectFileOrDefaults(
				Utils.RESOURCE_FILE_SUFFIXES_ARCHIVE, Utils.DEFAULT_SUFFIXES_ARCHIVE);
		Assert.assertEquals(Utils.DEFAULT_SUFFIXES_XML, loadedSuffixesXml);
		Assert.assertEquals(Utils.DEFAULT_SUFFIXES_TEXT, loadedSuffixesText);
		Assert.assertEquals(Utils.DEFAULT_SUFFIXES_ARCHIVE, loadedSuffixesArchive);

		Utils.deleteSuffixesFiles();
		loadedSuffixesXml = Utils.collectFileOrDefaults(
				Utils.RESOURCE_FILE_SUFFIXES_XML, Utils.DEFAULT_SUFFIXES_XML);
		loadedSuffixesText = Utils.collectFileOrDefaults(
				Utils.RESOURCE_FILE_SUFFIXES_TEXT, Utils.DEFAULT_SUFFIXES_TEXT);
		loadedSuffixesArchive = Utils.collectFileOrDefaults(
				Utils.RESOURCE_FILE_SUFFIXES_ARCHIVE, Utils.DEFAULT_SUFFIXES_ARCHIVE);
		Assert.assertEquals(Utils.DEFAULT_SUFFIXES_XML, loadedSuffixesXml);
		Assert.assertEquals(Utils.DEFAULT_SUFFIXES_TEXT, loadedSuffixesText);
		Assert.assertEquals(Utils.DEFAULT_SUFFIXES_ARCHIVE, loadedSuffixesArchive);
	}
}
