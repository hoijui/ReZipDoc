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

import org.xml.sax.InputSource;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.TransformerException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * The program takes a single argument, the name of the file to convert,
 * and produces a more human readable, textual representation of its content on stdout.
 * @see "https://github.com/costerwi/zipdoc"
 */
public class ZipDoc {

    private static final Set<String> SUFFIXES_XML;
    private static final Set<String> SUFFIXES_TEXT;

    static {
        final Set<String> suffixesXml = new HashSet<>();
        suffixesXml.add("xml");
        suffixesXml.add("svg");
        SUFFIXES_XML = suffixesXml;

        final Set<String> suffixesText = new HashSet<>(suffixesXml);
        suffixesText.add("txt");
        suffixesText.add("md");
        suffixesText.add("markdown");
        SUFFIXES_TEXT = suffixesText;
    }

    public static void main(final String[] argv) throws IOException, TransformerException {

        if (1 != argv.length) {
            System.err.printf("Usage: %s infile > text_representation.txt%n", ZipDoc.class.getSimpleName());
            System.exit(1);
        }

        transform(argv[0]);
    }

    /**
     * Checks whether a file denotes an XML based file format.
     * @param fileName to be checked for known XML file extensions (for example ".xml" and ".svg")
     * @param contentBytes length of the content in bytes
     * @param contentIn to be checked for magic file header
     * @param magicHeader the magic file header to look for
     * @param suffixes the file suffixes to look for
     * @return whether the supplied file type is XML based
     */
    @SuppressWarnings("WeakerAccess")
    public static boolean isType(final String fileName, final long contentBytes, final InputStream contentIn, final String magicHeader, final Set<String> suffixes) {

        if (!contentIn.markSupported()) {
            throw new IllegalStateException();
        }

        boolean matches = false;

        final String fileNameLower = fileName.toLowerCase();
        if (fileNameLower.contains(".")) {
            final String suffix = fileNameLower.substring(fileNameLower.lastIndexOf('.'));
            matches = suffixes.contains(suffix);
        }
        if (!matches && (magicHeader != null) && contentBytes >= magicHeader.length()) {
            final int maxHeaderBytes = magicHeader.length() * 2 + 2;
            contentIn.mark(maxHeaderBytes);
            final byte[] buffer = new byte[maxHeaderBytes];
            try {
                final int readHeaderBytes = contentIn.read(buffer);
                if (readHeaderBytes >= magicHeader.length()) {
                    final String header = new String(buffer);
                    if (header.toLowerCase().startsWith(magicHeader)) {
                        matches = true;
                    }
                }
            } catch (final IOException ex) {
                ex.printStackTrace();
            }
        }

        return matches;
    }

    /**
     * Checks whether a file denotes an XML based file format.
     * @param fileName to be checked for known XML file extensions (for example ".xml" and ".svg")
     * @param contentBytes length of the content in bytes {@literal "<?xml "} TODO
     * @param contentIn to be checked for magic file header for XML: {@code "<?xml "} TODO
     * @return whether the supplied file type is XML based
     */
    @SuppressWarnings("WeakerAccess")
    public static boolean isXml(final String fileName, final long contentBytes, final InputStream contentIn) {
        return isType(fileName, contentBytes, contentIn, "<?xml ", SUFFIXES_XML);
    }

    /**
     * Checks whether a file denotes an plain-text file format.
     * @param fileName to be checked
     * @param contentBytes length of the content in bytes
     * @param contentIn to see if only non-binary data is present
     * @return whether the supplied file name is text based
     */
    @SuppressWarnings("WeakerAccess")
    public static boolean isPlainText(final String fileName, final long contentBytes, final InputStream contentIn) {
        return isType(fileName, contentBytes, contentIn, null, SUFFIXES_TEXT);
    }

    /**
     * Reads the specified ZIP file and outputs a textual representation of its to stdout.
     * @param zipFilePath the ZIP file to convert to a text
     * @throws IOException if any input or output fails
     * @throws TransformerException if XML pretty-printing fails
     */
    @SuppressWarnings("WeakerAccess")
    public static void transform(final String zipFilePath) throws IOException, TransformerException {

        try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath))) {
            transform(zipIn, System.out);
        }
    }

    /**
     * Reads the specified ZIP document and outputs a textual representation of its to the specified output stream.
     * @param zipIn the ZIP document to convert to a text
     * @param output where the text gets written to
     * @throws IOException if any input or output fails
     * @throws TransformerException if XML pretty-printing fails
     */
    @SuppressWarnings("WeakerAccess")
    public static void transform(final ZipInputStream zipIn, final PrintStream output)
            throws IOException, TransformerException
    {
        final Transformer serializer = SAXTransformerFactory.newInstance().newTransformer();
        serializer.setOutputProperty(OutputKeys.INDENT, "yes");
        serializer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        serializer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        final byte[] buffer = new byte[8192];
        ZipEntry entry;
        final ByteArrayOutputStream uncompressedOutRaw = new ByteArrayOutputStream();
        final CRC32 checkSum = new CRC32();
        final CheckedOutputStream uncompressedOutChecked = new CheckedOutputStream(uncompressedOutRaw, checkSum);
        while ((entry = zipIn.getNextEntry()) != null) {
            uncompressedOutRaw.reset();
            checkSum.reset();

            output.println("Sub-file:\t" + entry);

            // Copy the file from zipIn into the uncompressed, check-summed output stream
            int len;
            while ((len = zipIn.read(buffer)) > 0) {
                uncompressedOutChecked.write(buffer, 0, len);
            }
            zipIn.closeEntry();

            if (isXml(entry.getName(), entry.getSize(), zipIn)) {
                // XML file: pretty-print the data to stdout
                final InputSource inBuffer = new InputSource(new ByteArrayInputStream(uncompressedOutRaw.toByteArray()));
                serializer.transform(new SAXSource(inBuffer), new StreamResult(output));
            } else if (isPlainText(entry.getName(), entry.getSize(), zipIn)) {
                // Text file: dump directly to output
                uncompressedOutRaw.writeTo(output);
            } else {
                // Unknown file type: report uncompressed size and CRC32
                output.println("File size:\t" + uncompressedOutRaw.size());
                output.println("Checksum:\t" + Long.toHexString(checkSum.getValue()));
            }
            output.println();
        }
    }
}
