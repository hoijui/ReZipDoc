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
            if (arg.equals("--store")) {
                compression = ZipEntry.STORED;
            } else {
                System.err.printf("Usage: %s {--store} <in.zip >out.zip\n", ReZip.class.getSimpleName());
                System.exit(1);
            }
        }

        final byte[] buffer = new byte[8192];
        ZipEntry entry;
        final ByteArrayOutputStream uncompressedOutRaw = new ByteArrayOutputStream();
        final CRC32 checksum = new CRC32();
        final CheckedOutputStream uncompressedOutChecked = new CheckedOutputStream(uncompressedOutRaw, checksum);
        try (final ZipInputStream zipIn = new ZipInputStream(System.in); final ZipOutputStream zipOut = new ZipOutputStream(System.out)) {
            while ((entry = zipIn.getNextEntry()) != null) {
                uncompressedOutRaw.reset();
                checksum.reset();

                // Copy file from zipIn into uncompressed, check-summed output stream
                int len;
                while ((len = zipIn.read(buffer)) > 0) {
                    uncompressedOutChecked.write(buffer, 0, len);
                }
                zipIn.closeEntry();

                // Modify ZIP entry for destination ZIP
                entry.setSize(uncompressedOutRaw.size());
                entry.setCrc(checksum.getValue());
                entry.setMethod(compression);
                entry.setCompressedSize(-1); // Unknown compressed size

                // Copy uncompressed file into destination ZIP
                zipOut.putNextEntry(entry);
                uncompressedOutRaw.writeTo(zipOut);
                zipOut.closeEntry();
            }
        }
    }
}
