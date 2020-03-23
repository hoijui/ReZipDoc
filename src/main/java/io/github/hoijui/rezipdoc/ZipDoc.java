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

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * The program takes a single argument,
 * the name of the ZIP file to convert,
 * and produces a more human readable,
 * textual representation of its content on stdout.
 * It is meant to be used as a {@code git textconv} filter;
 * see the README for details.
 */
@SuppressWarnings("WeakerAccess")
public class ZipDoc {

	private static final Logger LOGGER = Utils.getLogger(ZipDoc.class.getName());

	private final boolean recursive;
	private final boolean formatXml;

	/**
	 * Creates an instance with specific values.
	 *
	 * @param recursive whether to also text-ify ZIPs within the main ZIP
	 *   (and therein, and therein, ...) (default: {@code true})
	 * @param formatXml whether to pretty-print XML content
	 *   (default: {@code true})
	 */
	public ZipDoc(final boolean recursive, final boolean formatXml) {

		this.recursive = recursive;
		this.formatXml = formatXml;
	}

	/**
	 * Creates an instance with default values.
	 */
	public ZipDoc() {
		this(true, true);
	}

	private static void printUsage(final Level logLevel) {

		final String name = ZipDoc.class.getSimpleName();
		if (LOGGER.isLoggable(logLevel)) {
			Utils.printUsageHeader(LOGGER, logLevel, name);
			LOGGER.log(logLevel, "Usage:");
			LOGGER.log(logLevel, String.format(
					"\t%s [Archive-input-file]   # writes textual version to stdout",
					name));
			LOGGER.log(logLevel, String.format(
					"Examples:%n\t%s in-file.zip > text-representation.txt",
					name));
		}
	}

	public static void main(final String[] argv) throws IOException {

		if (argv.length == 0) {
			printUsage(Level.WARNING);
			System.exit(1);
		}
		if ("--help".equals(argv[0]) || "-h".equals(argv[0])) {
			printUsage(Level.INFO);
			return;
		}

		boolean recursive = true;
		boolean formatXml = false;
		for (int i = 0; i < argv.length - 1; i++) {
			final String arg = argv[i];
			if ("--non-recursive".equals(arg)) {
				recursive = false;
			} else if ("--format-xml".equals(arg)) {
				formatXml = true;
			} else {
				if (LOGGER.isLoggable(Level.WARNING)) {
					LOGGER.log(Level.WARNING, String.format("Invalid argument '%s'%n", arg));
				}
				printUsage(Level.WARNING);
				System.exit(1);
			}
		}

		new ZipDoc(recursive, formatXml).transform(Paths.get(argv[argv.length - 1]));
	}

	/**
	 * Reads the specified ZIP file and outputs
	 * a textual representation of it to stdout.
	 *
	 * @param zipFile the ZIP file to convert to a text
	 * @throws IOException if any input or output fails
	 */
	public void transform(final Path zipFile) throws IOException {

		try (ZipInputStream zipIn = new ZipInputStream(Files.newInputStream(zipFile))) {
			transform(zipIn, System.out);
		}
	}

	/**
	 * Reads the specified ZIP document and outputs a textual representation
	 * of its to the specified output stream.
	 *
	 * @param zipIn  the ZIP document to convert to a text
	 * @param output where the text gets written to
	 * @throws IOException if any input or output fails
	 */
	public void transform(final ZipInputStream zipIn, final PrintStream output)
			throws IOException
	{
		final XmlFormatter xmlFormatter = new XmlFormatter(2, "  ", true);
		final byte[] buffer = new byte[8192];
		ZipEntry entry;
		final BufferedOutputStream uncompressedOutRaw = new BufferedOutputStream();
		final CRC32 checkSum = new CRC32();
		final CheckedOutputStream uncompressedOutChecked = new CheckedOutputStream(uncompressedOutRaw, checkSum);
		while ((entry = zipIn.getNextEntry()) != null) {
			uncompressedOutRaw.reset();
			checkSum.reset();

			output.println("Sub-file:\t" + entry);

			// Copy the file from zipIn into the uncompressed, check-summed output stream
			Utils.transferTo(zipIn, uncompressedOutChecked, buffer);
			zipIn.closeEntry();

			final boolean isXml = Utils.isXml(entry.getName(), entry.getSize(), uncompressedOutRaw);
			if (formatXml && isXml) {
				// XML file: pretty-print the data to stdout
				xmlFormatter.prettify(uncompressedOutRaw.createInputStream(false), output, buffer);
			} else if (Utils.isPlainText(entry.getName(), entry.getSize(), uncompressedOutRaw) || isXml) {
				// Text file: dump directly to output
				uncompressedOutRaw.writeTo(output);
			} else if (Utils.isZip(entry.getName(), entry.getSize(), uncompressedOutRaw)
					&& recursive)
			{
				// Zip: recursively uncompress to output
				output.println("Sub-ZIP start:\t" + entry.getName());
				try (ZipInputStream zipInRec = new ZipInputStream(
						uncompressedOutRaw.createInputStream(false)))
				{
					transform(zipInRec, output);
				}
				output.println("Sub-ZIP end:  \t" + entry.getName());
			} else {
				// Unknown file type: report uncompressed size and CRC32
				output.println("File size:\t" + uncompressedOutRaw.size());
				output.println("Checksum:\t" + Long.toHexString(checkSum.getValue()));
			}
			output.println();
		}
	}
}
