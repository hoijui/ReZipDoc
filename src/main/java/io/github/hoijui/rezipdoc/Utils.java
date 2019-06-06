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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Various helper functions.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public final class Utils {

	private static final Logger LOGGER = Utils.getLogger(Utils.class.getName());

	public static final String RESOURCE_FILE_SUFFIXES_PREFIX = "reZipDoc-";
	public static final String RESOURCE_FILE_SUFFIXES_TEXT
			= RESOURCE_FILE_SUFFIXES_PREFIX + "suffixes-text.csv";
	public static final String RESOURCE_FILE_SUFFIXES_XML
			= RESOURCE_FILE_SUFFIXES_PREFIX + "suffixes-xml.csv";
	public static final String RESOURCE_FILE_SUFFIXES_ARCHIVE
			= RESOURCE_FILE_SUFFIXES_PREFIX + "suffixes-archive.csv";
	public static final Set<String> DEFAULT_SUFFIXES_XML = setOf(
			"xml",
			"svg");
	public static final Set<String> DEFAULT_SUFFIXES_TEXT = setOf(
			"txt",
			"md",
			"markdown",
			"properties",
			"java",
			"kt",
			"c",
			"cxx",
			"cpp",
			"h",
			"hxx",
			"hpp",
			"js",
			"html");
	public static final Set<String> DEFAULT_SUFFIXES_ARCHIVE = setOf(
			"zip",
			"jar",
			"docx",
			"xlsx",
			"pptx",
			"odt",
			"ods",
			"odp",
			"fcstd");
	private static final Set<String> SUFFIXES_XML;
	private static final Set<String> SUFFIXES_TEXT;
	private static final Set<String> SUFFIXES_ARCHIVE;

	static {
		SUFFIXES_XML = collectFileOrDefaults(RESOURCE_FILE_SUFFIXES_XML, DEFAULT_SUFFIXES_XML);
		SUFFIXES_TEXT = collectFileOrDefaults(RESOURCE_FILE_SUFFIXES_TEXT, DEFAULT_SUFFIXES_TEXT);
		SUFFIXES_ARCHIVE = collectFileOrDefaults(RESOURCE_FILE_SUFFIXES_ARCHIVE, DEFAULT_SUFFIXES_ARCHIVE);
	}

	private Utils() {
	}

	private static ReroutableConsoleHandler stdErr;

	public static Logger getLogger(final String name) {

		final Logger logger = Logger.getLogger(name);
		logger.setUseParentHandlers(false);

		final Formatter basicFmt = new BasicLogFormatter();

		if (stdErr == null) {
			stdErr = new ReroutableConsoleHandler();
			stdErr.setLevel(Level.FINEST);
			stdErr.setFormatter(basicFmt);
		}
		logger.addHandler(stdErr);

		logger.setLevel(Level.FINEST);

		return logger;
	}

	public static ReroutableConsoleHandler getLogHandler() {
		return stdErr;
	}

	/**
	 * Returns the directory in which our sources are located.
	 *
	 * @return the dir where this ".class" or ".jar" file is located
	 * @throws URISyntaxException should never happen
	 * @see "https://stackoverflow.com/a/320595/586229"
	 */
	public static Path sourceDir() throws URISyntaxException {

		File sourceLocation = new File(Utils.class.getProtectionDomain()
				.getCodeSource().getLocation().toURI());
		// If our source location is a JAR file, get its parent dir
		if (sourceLocation.isFile()) {
			sourceLocation = sourceLocation.getParentFile();
		}
		return sourceLocation.toPath();
	}

	/**
	 * Reads all lines of a file, and streams them.
	 * @param textFile the file to be used as a data source
	 * @param filter whether to filter out empty lines starting with '#'
	 * @return the file as a stream of lines
	 * @throws IOException if there is a problem while reading the file
	 */
	public static List<String> readLines(final Path textFile, final boolean filter) throws IOException {

		final Charset encoding = StandardCharsets.UTF_8;
		try (Stream<String> fileIn = Files.lines(textFile, encoding)) {
			return fileIn
					.map(String::trim)
					// filter-out empty lines and comments
					.filter(s -> !filter || (!s.isEmpty() && (s.charAt(0) != '#')))
					.collect(Collectors.toList());
		}
	}

	public static String readStreamToString(final InputStream inputStream) {

		final Scanner inScr = new Scanner(inputStream).useDelimiter("\\A");
		return inScr.hasNext() ? inScr.next() : "";
	}

	public static void writeLines(final Path textFile, final Collection<String> lines) throws IOException {

		try (PrintStream out = new PrintStream(Files.newOutputStream(textFile))) {
			lines.forEach(out::println);
		}
	}

	/**
	 * Writes all suffix files next to our binary (JAR).
	 * @throws IOException if there is a problem while writing the file
	 * @throws URISyntaxException if there is a problem while looking up the file path
	 */
	public static void writeSuffixesFiles() throws IOException, URISyntaxException {

		Path suffixesFile = sourceDir().resolve(RESOURCE_FILE_SUFFIXES_TEXT);
		writeLines(suffixesFile, SUFFIXES_TEXT);

		suffixesFile = sourceDir().resolve(RESOURCE_FILE_SUFFIXES_XML);
		writeLines(suffixesFile, SUFFIXES_XML);

		suffixesFile = sourceDir().resolve(RESOURCE_FILE_SUFFIXES_ARCHIVE);
		writeLines(suffixesFile, SUFFIXES_ARCHIVE);
	}

	/**
	 * Deletes all suffix files next to our binary (JAR).
	 * @throws URISyntaxException if there is a problem while looking up the file path
	 * @throws IOException if deleting the any of the files failed
	 */
	public static void deleteSuffixesFiles() throws URISyntaxException, IOException {

		Path suffixesFile = sourceDir().resolve(RESOURCE_FILE_SUFFIXES_TEXT);
		Files.deleteIfExists(suffixesFile);

		suffixesFile = sourceDir().resolve(RESOURCE_FILE_SUFFIXES_XML);
		Files.deleteIfExists(suffixesFile);

		suffixesFile = sourceDir().resolve(RESOURCE_FILE_SUFFIXES_ARCHIVE);
		Files.deleteIfExists(suffixesFile);
	}

	@SafeVarargs
	public static <T> Set<T> setOf(final T... defaults) {

		final Set<T> strings = new HashSet<>();
		Collections.addAll(strings, defaults);
		return Collections.unmodifiableSet(strings);
	}

	public static Set<String> collectFileOrDefaults(final String localResourceFilePath, final Set<String> defaults) {

		Set<String> suffixes;
		Path suffixesFile = null;
		try {
			suffixesFile = sourceDir().resolve(localResourceFilePath);
			suffixes = collectFileNameMatchers(suffixesFile);
			if (LOGGER.isLoggable(Level.INFO)) {
				LOGGER.info(String.format("Read suffixes from file \"%s\".", suffixesFile));
			}
		} catch (IOException exc) {
			if (LOGGER.isLoggable(Level.INFO)) {
				LOGGER.info(String.format("Did not read suffixes from file \"%s\".", suffixesFile));
			}
			suffixes = defaults;
		} catch (URISyntaxException exc) {
			if (LOGGER.isLoggable(Level.SEVERE)) {
				LOGGER.log(Level.SEVERE, String.format("Failed collecting suffixes for \"%s\"",
						localResourceFilePath), exc);
			}
			suffixes = null;
			System.exit(1);
		}

		return suffixes;
	}

	public static Set<String> collectFileNameMatchers(final Path resourceFile) throws IOException {
		return new HashSet<>(readLines(resourceFile, true));
	}

	/**
	 * Checks whether a file denotes an XML based file format.
	 *
	 * @param fileName     to be checked for known XML file extensions
	 *                     (for example ".xml" and ".svg")
	 * @param contentBytes length of the content in bytes
	 * @param contentIn    to be checked for magic file header
	 * @param magicHeader  the magic file header to look for
	 * @param suffixes     the file suffixes to look for
	 * @param mimeType     which MIME-type to accept as matching
	 * @return whether the supplied file type is XML based
	 * @throws IOException If something went wrong while trying to read the magic file header
	 */
	public static boolean isType(final String fileName, final long contentBytes, final BufferedOutputStream contentIn,
			final String magicHeader, final Set<String> suffixes, final String mimeType) throws IOException
	{
		boolean matches = false;

		final String fileNameLower = fileName.toLowerCase();
		if (fileNameLower.contains(".")) {
			final String suffix = fileNameLower.substring(fileNameLower.lastIndexOf('.') + 1);
			matches = suffixes.contains(suffix);
		}
		if (!matches && (magicHeader != null)
				&& (contentBytes >= magicHeader.length())
				&& contentIn.startsWith(magicHeader.getBytes()))
		{
			matches = true;
		}
		if (!matches && mimeType != null && !mimeType.isEmpty()) {
			final String foundMimeType = guessContentTypeFromStream(contentIn.createInputStream(false));
			if (mimeType.equals(foundMimeType)) {
				matches = true;
			}
		}

		return matches;
	}

	/**
	 * Checks whether a file denotes an XML based file format.
	 *
	 * @param fileName to be checked for known XML file extensions
	 *   (for example ".xml" and ".svg")
	 * @param contentBytes length of the content in bytes
	 * @param contentIn to be checked for magic file header for XML:
	 *   {@literal "<?xml "}
	 * @return whether the supplied file type is XML based
	 * @throws IOException If something went wrong while trying to read the magic file header
	 */
	public static boolean isXml(final String fileName, final long contentBytes, final BufferedOutputStream contentIn) throws IOException {
		return isType(fileName, contentBytes, contentIn, "<?xml ", SUFFIXES_XML, "application/xml");
	}

	/**
	 * Checks whether a file denotes a plain-text file format.
	 *
	 * @param fileName     to be checked
	 * @param contentBytes length of the content in bytes
	 * @param contentIn    to see if only non-binary data is present
	 * @return whether the supplied file name is text based
	 * @throws IOException If something went wrong while trying to read the magic file header
	 */
	public static boolean isPlainText(final String fileName, final long contentBytes, final BufferedOutputStream contentIn) throws IOException {
		return isType(fileName, contentBytes, contentIn, null, SUFFIXES_TEXT, null);
	}

	public static boolean isZip(final String fileName, final long contentBytes, final BufferedOutputStream contentIn) throws IOException {
		return isType(fileName, contentBytes, contentIn, null, SUFFIXES_ARCHIVE, "application/zip");
	}

	/**
	 * Copies input content to output.
	 * The same like Java 9's {@code InputStream#transferTo(OutputStream)}.
	 *
	 * @param source the source of the data
	 * @param target where the source data should be copied to
	 * @param buffer the buffer to use for transfering;
	 *   no more then {@code buffer.length} bytes are read at a time
	 * @throws IOException if any input or output fails
	 */
	public static void transferTo(final InputStream source, final OutputStream target, final byte[] buffer)
			throws IOException
	{
		for (int n = source.read(buffer); n >= 0; n = source.read(buffer)) {
			target.write(buffer, 0, n);
		}
	}

	/**
	 * Tries to determine the type of an input stream based on the
	 * characters at the beginning of the stream.
	 *
	 * Additional values herein over what is already supported by the JDK
	 * are taken from @{link https://en.wikipedia.org/wiki/List_of_file_signatures}.
	 *
	 * @param is an input stream that supports marks.
	 * @return a guess at the content type, or {@code null} if none
	 * can be determined.
	 *
	 * @throws IOException if an I/O error occurs while reading the
	 *                     input stream.
	 * @see InputStream#mark(int)
	 * @see InputStream#markSupported()
	 * @see URLConnection#guessContentTypeFromStream(InputStream)
	 */
	public static String guessContentTypeFromStream(final InputStream is) throws IOException {

		String contentMimeType = URLConnection.guessContentTypeFromStream(is);
		if (contentMimeType == null && is.markSupported()) {
			// JDK does not know the mime type, but we might be able to figure it out ...

			// read the first 16 byte of the file (it might be a magic header)
			is.mark(16);
			final int c1 = is.read();
			final int c2 = is.read();
			final int c3 = is.read();
			final int c4 = is.read();
			is.reset();

			if ((c1 == 0x50 && c2 == 0x4B)
					&&((c3 == 0x03 && c4 == 0x04)
						|| (c3 == 0x05 && c4 == 0x06)
						|| (c3 == 0x07 && c4 == 0x08)))
			{
				contentMimeType = "application/zip";
			}
		}

		return contentMimeType;
	}
}
