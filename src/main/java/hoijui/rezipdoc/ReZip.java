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

package hoijui.rezipdoc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Read ZIP content and write (optionally <i>uncompressed</i>) ZIP content out.
 * Uncompressed files are stored more efficiently in Git.
 * @see "https://github.com/costerwi/rezip"
 */
public class ReZip {

    /**
     * Reads a ZIP file from stdin and writes new ZIP content to stdout.
     * With the --store command line argument the output will be an
     * uncompressed zip.
     * @param argv the command line arguments
     * @throws IOException if any input or output fails
     */
    public static void main(final String[] argv) throws IOException {
        int compression = ZipEntry.DEFLATED;
        for (final String arg : argv) {
            if ("--store".equals(arg)) {
                compression = ZipEntry.STORED;
            } else {
                System.err.printf("Usage: %s {--store} <in.zip >out.zip%n", ReZip.class.getSimpleName());
                System.exit(1);
            }
        }

        reZip(compression);
    }

    /**
     * Reads a ZIP file from stdin and writes new ZIP content to stdout.
     * @param compression the compression method to use,
     *                    either {@link ZipEntry#DEFLATED} (=> compressed)
     *                    or {@link ZipEntry#STORED} (-> uncompressed)
     * @throws IOException if any input or output fails
     */
    @SuppressWarnings("WeakerAccess")
    public static void reZip(final int compression) throws IOException {

        try (ZipInputStream zipIn = new ZipInputStream(System.in); ZipOutputStream zipOut = new ZipOutputStream(System.out)) {
            reZip(zipIn, zipOut, compression);
        }
    }

    /**
     * Reads a ZIP and writes to an other ZIP.
     * @param zipIn the source ZIP
     * @param zipOut the destination ZIP
     * @param compression the compression method to use,
     *                    either {@link ZipEntry#DEFLATED} (=> compressed)
     *                    or {@link ZipEntry#STORED} (-> uncompressed)
     * @throws IOException if any input or output fails
     */
    @SuppressWarnings("WeakerAccess")
    public static void reZip(final ZipInputStream zipIn, final ZipOutputStream zipOut, final int compression) throws IOException {

        final byte[] buffer = new byte[8192];
        final ByteArrayOutputStream uncompressedOutRaw = new ByteArrayOutputStream();
        final CRC32 checksum = new CRC32();
        final CheckedOutputStream uncompressedOutChecked = new CheckedOutputStream(uncompressedOutRaw, checksum);
        for (ZipEntry entry = zipIn.getNextEntry(); entry != null; entry = zipIn.getNextEntry()) {
            uncompressedOutRaw.reset();
            checksum.reset();

            // Copy file from zipIn into uncompressed, check-summed output stream
            for  (int len = zipIn.read(buffer); len > 0; len = zipIn.read(buffer)) {
                uncompressedOutChecked.write(buffer, 0, len);
            }
            zipIn.closeEntry();

            // Modify ZIP entry for destination ZIP
            entry.setSize(uncompressedOutRaw.size());
            entry.setCrc(checksum.getValue());
            entry.setMethod(compression);
            // Unknown compressed size
            entry.setCompressedSize(-1);

            // Copy uncompressed entry content into destination ZIP
            zipOut.putNextEntry(entry);
            uncompressedOutRaw.writeTo(zipOut);
            zipOut.closeEntry();
        }
    }
}
