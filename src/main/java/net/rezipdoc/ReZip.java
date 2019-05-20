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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Read ZIP content and write <i>uncompressed</i> ZIP content out.
 * Uncompressed files are stored more efficiently in Git.
 *
 * @see "https://github.com/costerwi/rezip"
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class ReZip {

	private static final Logger LOGGER = Utils.getLogger(ReZip.class.getName());

	/**
	 * Whether to re-pack the output ZIP with compression
	 * (default: {@code false}).
	 */
	private final boolean compression;
	/**
	 * Whether to re-pack the output ZIP with all entries time-stamps
	 * (creation-, last-access- and last-modified-times) set to zero
	 * (default: {@code false}).
	 */
	private final boolean nullifyTimes;
	/**
	 * Whether to also re-pack ZIP files contained within the supplied ZIP
	 * (and therein, and therein, ...)
	 * (default: {@code true}).
	 */
	private final boolean recursive;
	/**
	 * Whether to pretty-print XML content
	 * (default: {@code false}).
	 */
	private final boolean formatXml;

	/**
	 * Stores settings about how to re-zip.
	 *
	 * @param compression whether the output ZIP is to use compression
	 * @param nullifyTimes whether the creation-, last-access- and last-modified-times
	 *   of the re-packed archive entries should be set to {@code 0}
	 * @param recursive whether to re-pack the ZIP recursively
	 *   (repacking the ZIPs within ZIPs ... within the supplied ZIP)
	 * @param formatXml whether to pretty-print XML content
	 *   (default: {@code true})
	 */
	public ReZip(final boolean compression, final boolean nullifyTimes, final boolean recursive, final boolean formatXml) {

		this.compression = compression;
		this.nullifyTimes = nullifyTimes;
		this.recursive = recursive;
		this.formatXml = formatXml;
	}

	public ReZip() {
		this(false, false, true, false);
	}

	/**
	 * Whether to re-pack the output ZIP with compression.
	 * @return default: {@code false}
	 */
	public boolean isCompression() {
		return compression;
	}

	/**
	 * Whether to re-pack the output ZIP with all entries time-stamps
	 * (creation-, last-access- and last-modified-times) set to zero.
	 * @return default: {@code false}
	 */
	public boolean isNullifyTimes() {
		return nullifyTimes;
	}

	/**
	 * Whether to also re-pack ZIP files contained within the supplied ZIP
	 * (and therein, and therein, ...).
	 * @return default: {@code true}
	 */
	public boolean isRecursive() {
		return recursive;
	}

	/**
	 * Whether to pretty-print XML content
	 * @return default: {@code false}
	 */
	public boolean isFormatXml() {
		return formatXml;
	}

	/**
	 * Reads a ZIP file from stdin and writes new ZIP content to stdout.
	 * With the <code>--compressed</code> command line argument,
	 * the output will be a compressed ZIP as well.
	 *
	 * @param argv the command line arguments
	 * @throws IOException if any input or output fails
	 */
	public static void main(final String[] argv) throws IOException {

		boolean compressed = false;
		boolean nullifyTimes = false;
		boolean recursive = true;
		boolean formatXml = false;
		for (final String arg : argv) {
			if ("--compressed".equals(arg)) {
				compressed = true;
			} else if ("--nullify-times".equals(arg)) {
				nullifyTimes = true;
			} else if ("--non-recursive".equals(arg)) {
				recursive = false;
			} else if ("--format-xml".equals(arg)) {
				formatXml = true;
			} else if ("--write-suffixes".equals(arg)) {
				try {
					Utils.writeSuffixesFiles();
					return;
				} catch (URISyntaxException exc) {
					LOGGER.log(Level.SEVERE, "Failed writing suffixes files", exc);
					System.exit(1);
				}
			} else {
				if (LOGGER.isLoggable(Level.WARNING)) {
					LOGGER.warning("Usage:");
					LOGGER.warning(String.format("\t%s [--compressed] [--nullify-times] [--non-recursive] <in.zip >out.zip",
							ReZip.class.getSimpleName()));
					LOGGER.warning(String.format("\t%s --write-suffixes", ReZip.class.getSimpleName()));
					LOGGER.warning("Options:");
					LOGGER.warning("\t--compressed       re-zip compressed");
					LOGGER.warning("\t--nullify-times    set creation-, last-access- and last-modified-times of the re-zipped archives entries to 0");
					LOGGER.warning("\t--non-recursive    do not re-zip archives within archives");
					LOGGER.warning("\t--format-xml       pretty-print (reformat) XML content");
					LOGGER.warning("\t--write-suffixes   writes suffix files next to the JAR, populated with defaults, and exits");
				}
				System.exit(1);
			}
		}

		new ReZip(compressed, nullifyTimes, recursive, formatXml).reZip();
	}

	/**
	 * Reads a ZIP file from stdin and writes new ZIP content to stdout.
	 *
	 * @throws IOException if any input or output fails
	 */
	public void reZip() throws IOException {

		try (ZipInputStream zipIn = new ZipInputStream(System.in);
				ZipOutputStream zipOut = new ZipOutputStream(System.out))
		{
			reZip(zipIn, zipOut);
		}
	}

	public void reZip(final Path zipInFile, final Path zipOutFile) throws IOException {

		try (ZipInputStream zipIn = new ZipInputStream(new BufferedInputStream(Files.newInputStream(zipInFile)));
				ZipOutputStream zipOut = new ZipOutputStream(Files.newOutputStream(zipOutFile)))
		{
			reZip(zipIn, zipOut);
		}
	}

	/**
	 * Reads a ZIP and writes to an other ZIP.
	 *
	 * @param zipIn    the source ZIP
	 * @param zipOut   the destination ZIP
	 * @throws IOException if any input or output fails
	 */
	public void reZip(final ZipInputStream zipIn, final ZipOutputStream zipOut)
			throws IOException
	{
		final int compressionMethod = isCompression() ? ZipEntry.DEFLATED : ZipEntry.STORED;
		final byte[] buffer = new byte[8192];
		final BufferedOutputStream uncompressedOutRaw = new BufferedOutputStream();
		final CRC32 checksum = new CRC32();
		final CheckedOutputStream uncompressedOutChecked = new CheckedOutputStream(uncompressedOutRaw, checksum);
		reZip(zipIn, zipOut, compressionMethod, buffer, uncompressedOutRaw, checksum, uncompressedOutChecked);
	}

	private void reZip(
			final ZipInputStream zipIn,
			final ZipOutputStream zipOut,
			final int compressionMethod,
			final byte[] buffer,
			final BufferedOutputStream uncompressedOutRaw,
			final CRC32 checksum,
			final CheckedOutputStream uncompressedOutChecked)
			throws IOException
	{
		final XmlFormatter xmlFormatter = new XmlFormatter();
		for (ZipEntry entry = zipIn.getNextEntry(); entry != null; entry = zipIn.getNextEntry()) {
			uncompressedOutRaw.reset();
			checksum.reset();

			// Copy file from zipIn into uncompressed, check-summed output stream
			Utils.transferTo(zipIn, uncompressedOutChecked, buffer);
			zipIn.closeEntry();

			// If we found a ZIP in this ZIP, and we want to recursively filter, then do so
			if (formatXml && Utils.isXml(entry.getName(), entry.getSize(), uncompressedOutRaw)) {
				// XML file: pretty-print the data to stdout
				final InputStream source = uncompressedOutRaw.createInputStream(true);
				uncompressedOutRaw.reset();
				xmlFormatter.prettify(source, uncompressedOutRaw, buffer);
			} else if (isRecursive() && Utils.isZip(entry.getName(), entry.getSize(), uncompressedOutRaw)) {
				final BufferedOutputStream subUncompressedOutRaw = new BufferedOutputStream();
				final CRC32 subChecksum = new CRC32();
				final CheckedOutputStream subUncompressedOutChecked = new CheckedOutputStream(subUncompressedOutRaw, subChecksum);
				try (ZipInputStream zipInRec = new ZipInputStream(uncompressedOutRaw.createInputStream(true));
						ZipOutputStream zipOutRec = new ZipOutputStream(uncompressedOutChecked))
				{
					uncompressedOutRaw.reset();
					checksum.reset();
					reZip(zipInRec, zipOutRec, compressionMethod, buffer, subUncompressedOutRaw, subChecksum, subUncompressedOutChecked);
				}
			}

			// Create the ZIP entry for destination ZIP
			entry.setSize(uncompressedOutRaw.size());
			entry.setCrc(checksum.getValue());
			entry.setMethod(compressionMethod);
			// Unknown compressed size
			entry.setCompressedSize(-1);
			if (isNullifyTimes()) {
				entry.setTime(0);
				entry.setCreationTime(FileTime.fromMillis(0));
				entry.setLastAccessTime(FileTime.fromMillis(0));
				entry.setLastModifiedTime(FileTime.fromMillis(0));
			}

			zipOut.putNextEntry(entry);
			uncompressedOutRaw.writeTo(zipOut);
			zipOut.closeEntry();
		}
	}
}
