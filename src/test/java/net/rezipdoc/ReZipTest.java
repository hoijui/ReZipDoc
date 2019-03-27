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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @see ReZip
 */
@SuppressWarnings("WeakerAccess")
public class ReZipTest {

	private Path projectRoot;
	private List<File> archiveContents;
	private Path zipFile;
	private Path reZipFile;

	public List<File> createArchiveContentsList(final Path scanRoot) throws IOException {

		final List<File> collect = Files.find(scanRoot, 15,
				(p, a) -> p.getFileName().toString().matches(".*[.](java|txt|xml|properties)"))
				.map(Path::toFile)
				.collect(Collectors.toList());
		if (collect.isEmpty()) {
			throw new RuntimeException("No sample files found to use in ZIP testing");
		}

		return collect;
	}

	public void createZip(final Path zipFile, final Map<Path, List<File>> rootContents, final int compressionMethod) throws IOException {

		try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFile.toFile()))) {
			zipOut.setMethod(compressionMethod);
			for (final Path rootDir : rootContents.keySet()) {
				for (final File file : rootContents.get(rootDir)) {
					final Path relPath = rootDir.relativize(file.toPath());
					final ZipEntry entry = new ZipEntry(relPath.toString());
					entry.setMethod(compressionMethod);
					zipOut.putNextEntry(entry);
					Files.copy(file.toPath(), zipOut);
					zipOut.closeEntry();
				}
			}
		}
	}

	public void createZip(final Path zipFile, final Path rootDir, final List<File> contents, final int compressionMethod) throws IOException {

		final Map<Path, List<File>> rootContents = new HashMap<>();
		rootContents.put(rootDir, contents);
		createZip(zipFile, rootContents, compressionMethod);
	}

	public void createRecursiveZip(final Path zipFile, final Path rootDir, final List<File> contents, final int compressionMethod) throws IOException {

		if (contents.size() < 4) {
			throw new IllegalStateException("We need at least 4 content elements for a recursive ZIP file");
		}

		final Map<Path, List<File>> subRootContents = new HashMap<>();
		subRootContents.put(rootDir, contents.subList(0, 2));
		final Path subZipFile = Files.createTempFile(getClass().getName() + "_original_sub_", ".zip");
		createZip(subZipFile, subRootContents, compressionMethod);

		final Map<Path, List<File>> mainRootContents = new HashMap<>();
		mainRootContents.put(subZipFile.getParent(), Collections.singletonList(subZipFile.toFile()));
		mainRootContents.put(rootDir, contents.subList(2, contents.size()));
		createZip(zipFile, mainRootContents, compressionMethod);
	}

	@Before
	public void setUp() throws IOException {

		projectRoot = new File("").toPath();
		archiveContents = createArchiveContentsList(projectRoot);
		// We do not want too much content
		final int maxContentFiles = 10;
		if (archiveContents.size() > maxContentFiles) {
			archiveContents = archiveContents.subList(0, maxContentFiles);
		}

		zipFile   = Files.createTempFile(getClass().getName() + "_original_", ".zip");
		reZipFile = Files.createTempFile(getClass().getName() + "_filtered_", ".zip");
	}

	@After
	public void tearDown() {
		zipFile.toFile().delete();
		reZipFile.toFile().delete();
	}

	private static void checkContains(final boolean contains, final Path zipFile, final List<File> contents) throws IOException {

		final String wholeZipContent = new String(Files.readAllBytes(zipFile));
		for (final File file : contents) {
			final String fileContent = new String(Files.readAllBytes(file.toPath()));
			Assert.assertEquals(
					"Content of file '" + file.toString() + "' does " + (contains ? "not " : "")
							+ "appear in Re(Un)Zip filtered ZIP file",
					contains,
					wholeZipContent.contains(fileContent));
		}
	}

	@Test
	public void testNonRecursive() throws IOException {

		// This is the original, compressed file
		createRecursiveZip(zipFile, projectRoot, archiveContents, ZipEntry.DEFLATED);
		// This creates the uncompressed file
		new ReZip(false, false, false).reZip(zipFile, reZipFile);
		// Test whether the filtered ZIP file does not contain the original content placed in a sub-ZIP file in plain text
		checkContains(false, reZipFile, archiveContents.subList(0, 2));
		// Test whether the filtered ZIP file contains the directly embedded original content in plain text
		checkContains(true, reZipFile, archiveContents.subList(2, archiveContents.size()));
	}

	@Test
	public void testRecursive() throws IOException {

		// This is the original, compressed file
		createRecursiveZip(zipFile, projectRoot, archiveContents, ZipEntry.DEFLATED);
		// This creates the uncompressed file
		new ReZip().reZip(zipFile, reZipFile);
		// Test whether the filtered ZIP file contains all original content in plain text
		checkContains(true, reZipFile, archiveContents);
	}

	@Test
	public void testContentsNotVisibleInFullInPlainText() throws IOException {

		// This is the original, compressed file
		createZip(zipFile, projectRoot, archiveContents, ZipEntry.DEFLATED);
		// This creates the *still compressed* file
		new ReZip(true, false, true).reZip(zipFile, reZipFile);
		// Test whether the filtered ZIP file does not contain the original content in plain text
		checkContains(false, reZipFile, archiveContents);
	}

	@Test
	public void testContentsVisibleInFullInPlainText() throws IOException {

		// This is the original, compressed file
		createZip(zipFile, projectRoot, archiveContents, ZipEntry.DEFLATED);
		// This creates the uncompressed file
		new ReZip().reZip(zipFile, reZipFile);
		// Test whether the filtered ZIP file contains all original content in plain text
		checkContains(true, reZipFile, archiveContents);
	}
}
