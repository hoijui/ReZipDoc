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

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
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

	private final boolean compression;
	private final boolean nullifyTimes;
	private final boolean recursive;

	/**
	 * Stores settings about how to re-zip.
	 *
	 * @param compression  whether the output ZIP is compressed or not
	 * @param nullifyTimes whether the creation-, last-access- and last-modified-times of the archive entries
	 *                     should be set to <code>0</code>
	 * @param recursive    whether to re-zip recursively (the ZIPs within ZIPs within ZIPs ...)
	 */
	public ReZip(final boolean compression, final boolean nullifyTimes, final boolean recursive) {

		this.compression = compression;
		this.nullifyTimes = nullifyTimes;
		this.recursive = recursive;
	}

	public ReZip() {
		this(false, false, true);
	}

	public boolean isCompression() {
		return compression;
	}

	public boolean isNullifyTimes() {
		return nullifyTimes;
	}

	public boolean isRecursive() {
			return recursive;
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
		for (final String arg : argv) {
			if ("--compressed".equals(arg)) {
				compressed = true;
			} else if ("--nullify-times".equals(arg)) {
				nullifyTimes = true;
			} else if ("--non-recursive".equals(arg)) {
				recursive = false;
			} else {
				System.err.printf("Usage: %s [--compressed] [--nullify-times] [--non-recursive] <in.zip >out.zip%n",
						ReZip.class.getSimpleName());
				System.err.println("\t--compressed       re-zip compressed");
				System.err.println("\t--nullify-times    set creation-, last-access- and last-modified-times of the re-zipped archives entries to 0");
				System.err.println("\t--non-recursive    do not re-zip archives within archives");
				System.exit(1);
			}
		}

		new ReZip(compressed, nullifyTimes, recursive).reZip();
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

		try (ZipInputStream zipIn = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipInFile.toFile())));
		     ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipOutFile.toFile())))
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
		final int compression = isCompression() ? ZipEntry.DEFLATED : ZipEntry.STORED;
		final byte[] buffer = new byte[8192];
		final BufferedOutputStream uncompressedOutRaw = new BufferedOutputStream();
		final CRC32 checksum = new CRC32();
		final CheckedOutputStream uncompressedOutChecked = new CheckedOutputStream(uncompressedOutRaw, checksum);
		reZip(zipIn, zipOut, compression, buffer, uncompressedOutRaw, checksum, uncompressedOutChecked);
	}

	private void reZip(
			final ZipInputStream zipIn,
			final ZipOutputStream zipOut,
			final int compression,
			final byte[] buffer,
			final BufferedOutputStream uncompressedOutRaw,
			final CRC32 checksum,
			final CheckedOutputStream uncompressedOutChecked)
			throws IOException
	{
		for (ZipEntry entry = zipIn.getNextEntry(); entry != null; entry = zipIn.getNextEntry()) {
			uncompressedOutRaw.reset();
			checksum.reset();

			// Copy file from zipIn into uncompressed, check-summed output stream
			for (int len = zipIn.read(buffer); len > 0; len = zipIn.read(buffer)) {
				uncompressedOutChecked.write(buffer, 0, len);
			}
			zipIn.closeEntry();

			// If we found a ZIP in this ZIP, an dwe want to recursively filter, then do so
			if (isRecursive() && Utils.isZip(entry.getName(), entry.getSize(), uncompressedOutRaw)) {
				final BufferedOutputStream subUncompressedOutRaw = new BufferedOutputStream();
				final CRC32 subChecksum = new CRC32();
				final CheckedOutputStream subUncompressedOutChecked = new CheckedOutputStream(subUncompressedOutRaw, subChecksum);
				try (ZipInputStream zipInRec = new ZipInputStream(uncompressedOutRaw.createInputStream(true));
				     ZipOutputStream zipOutRec = new ZipOutputStream(uncompressedOutChecked))
				{
					uncompressedOutRaw.reset();
					checksum.reset();
					reZip(zipInRec, zipOutRec, compression, buffer, subUncompressedOutRaw, subChecksum, subUncompressedOutChecked);
				}
			}

			// Create the ZIP entry for destination ZIP
			entry.setSize(uncompressedOutRaw.size());
			entry.setCrc(checksum.getValue());
			entry.setMethod(compression);
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
