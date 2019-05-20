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

package net.rezipdoc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
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
	private static final Set<String> SUFFIXES_XML;
	private static final Set<String> SUFFIXES_TEXT;
	private static final Set<String> SUFFIXES_ARCHIVE;

	static {
		{
			final Set<String> defaultSuffixesXml = new HashSet<>();
			defaultSuffixesXml.add("xml");
			defaultSuffixesXml.add("svg");
			SUFFIXES_XML = collectFileOrDefaults(RESOURCE_FILE_SUFFIXES_XML, defaultSuffixesXml);
		}

		{
			final Set<String> defaultSuffixesText = new HashSet<>();
			defaultSuffixesText.add("txt");
			defaultSuffixesText.add("md");
			defaultSuffixesText.add("markdown");
			defaultSuffixesText.add("properties");
			defaultSuffixesText.add("java");
			defaultSuffixesText.add("kt");
			defaultSuffixesText.add("c");
			defaultSuffixesText.add("cxx");
			defaultSuffixesText.add("cpp");
			defaultSuffixesText.add("h");
			defaultSuffixesText.add("hxx");
			defaultSuffixesText.add("hpp");
			defaultSuffixesText.add("js");
			defaultSuffixesText.add("html");
			SUFFIXES_TEXT = collectFileOrDefaults(RESOURCE_FILE_SUFFIXES_TEXT, defaultSuffixesText);
		}

		{
			Set<String> defaultSuffixesZip = new HashSet<>();
			defaultSuffixesZip.add("zip");
			defaultSuffixesZip.add("jar");
			defaultSuffixesZip.add("docx");
			defaultSuffixesZip.add("xlsx");
			defaultSuffixesZip.add("pptx");
			defaultSuffixesZip.add("odt");
			defaultSuffixesZip.add("ods");
			defaultSuffixesZip.add("odp");
			defaultSuffixesZip.add("fcstd");
			SUFFIXES_ARCHIVE = collectFileOrDefaults(RESOURCE_FILE_SUFFIXES_ARCHIVE, defaultSuffixesZip);
		}
	}

	private Utils() {
	}

	public static Logger getLogger(final String name) {

		final Logger logger = Logger.getLogger(name);
		for (final Handler handler : logger.getHandlers()) {
			logger.removeHandler(handler);
		}
		logger.setUseParentHandlers(false);

		final Formatter basicFmt = new BasicLogFormatter();

		final ConsoleHandler stdErr = new ConsoleHandler();
		stdErr.setLevel(Level.FINEST);
		stdErr.setFormatter(basicFmt);
		logger.addHandler(stdErr);

		logger.setLevel(Level.FINEST);

		return logger;
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
		if (Files.isRegularFile(sourceLocation.toPath())) {
			sourceLocation = sourceLocation.getParentFile();
		}
		return sourceLocation.toPath();
	}

	/**
	 * Reads all lines of a file, and streams them.
	 * @param textFile the file to be used as a data source
	 * @return the file as a stream of lines
	 * @throws IOException if there is a problem while reading the file
	 */
	public static List<String> readLines(final Path textFile) throws IOException {

		final Charset encoding = StandardCharsets.UTF_8;
		try (Stream<String> fileIn = Files.lines(textFile, encoding)) {
			return fileIn
					.map(String::trim)
					// filter-out empty lines and comments
					.filter(s -> !s.isEmpty() && (s.charAt(0) != '#'))
					.collect(Collectors.toList());
		}
	}

	public static void writeLines(final Path textFile, final Collection<String> lines) throws IOException {

		try (final PrintStream out = new PrintStream(new FileOutputStream(textFile.toFile()))) {
			lines.forEach(out::println);
		}
	}

	/**
	 * Writes all suffix files next ot our binary (JAR).
	 * @throws IOException if there is a problem while writing the file
	 * @throws URISyntaxException if there is a problem while writing the file
	 */
	public static void writeSuffixesFiles() throws IOException, URISyntaxException {

		Path suffixesFile = sourceDir().resolve(RESOURCE_FILE_SUFFIXES_TEXT);
		writeLines(suffixesFile, SUFFIXES_TEXT);

		suffixesFile = sourceDir().resolve(RESOURCE_FILE_SUFFIXES_XML);
		writeLines(suffixesFile, SUFFIXES_XML);

		suffixesFile = sourceDir().resolve(RESOURCE_FILE_SUFFIXES_ARCHIVE);
		writeLines(suffixesFile, SUFFIXES_ARCHIVE);
	}

	public static Set<String> collectFileOrDefaults(final String localResourceFilePath, final Set<String> defaults) {

		Set<String> suffixes;
		try {
			final Path suffixesFile = sourceDir().resolve(localResourceFilePath);
			try {
				suffixes = collectFileNameMatchers(suffixesFile);
				if (LOGGER.isLoggable(Level.INFO)) {
					LOGGER.info(String.format("Read suffixes from file \"%s\".", suffixesFile));
				}
			} catch (IOException exc) {
				if (LOGGER.isLoggable(Level.INFO)) {
					LOGGER.info(String.format("Did not read suffixes from file \"%s\".", suffixesFile));
				}
				suffixes = defaults;
			}
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
		return new HashSet<>(readLines(resourceFile));
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
	 * @return whether the supplied file type is XML based
	 */
	public static boolean isType(final String fileName, final long contentBytes, final BufferedOutputStream contentIn,
			final String magicHeader, final Set<String> suffixes)
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
	 */
	public static boolean isXml(final String fileName, final long contentBytes, final BufferedOutputStream contentIn) {
		return isType(fileName, contentBytes, contentIn, "<?xml ", SUFFIXES_XML);
	}

	/**
	 * Checks whether a file denotes a plain-text file format.
	 *
	 * @param fileName     to be checked
	 * @param contentBytes length of the content in bytes
	 * @param contentIn    to see if only non-binary data is present
	 * @return whether the supplied file name is text based
	 */
	public static boolean isPlainText(final String fileName, final long contentBytes, final BufferedOutputStream contentIn) {
		return isType(fileName, contentBytes, contentIn, null, SUFFIXES_TEXT);
	}

	public static boolean isZip(final String fileName, final long contentBytes, final BufferedOutputStream contentIn) {
		return isType(fileName, contentBytes, contentIn, null, SUFFIXES_ARCHIVE);
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
}
