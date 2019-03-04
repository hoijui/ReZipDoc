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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * Various helper functions.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public final class Utils {

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

    /**
     * Checks whether a file denotes an XML based file format.
     * @param fileName to be checked for known XML file extensions (for example ".xml" and ".svg")
     * @param contentBytes length of the content in bytes
     * @param contentIn to be checked for magic file header
     * @param magicHeader the magic file header to look for
     * @param suffixes the file suffixes to look for
     * @return whether the supplied file type is XML based
     */
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
                try {
                    final int readHeaderBytes = contentIn.read(buffer);
                    if (readHeaderBytes >= magicHeader.length()) {
                        final String header = new String(buffer);
                        if (header.toLowerCase().startsWith(magicHeader)) {
                            matches = true;
                        }
                    }
                } finally {
                    contentIn.reset();
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
     * @param contentBytes length of the content in bytes
     * @param contentIn to be checked for magic file header for XML: {@literal "<?xml "}
     * @return whether the supplied file type is XML based
     */
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
    public static boolean isPlainText(final String fileName, final long contentBytes, final InputStream contentIn) {
        return isType(fileName, contentBytes, contentIn, null, SUFFIXES_TEXT);
    }
}
