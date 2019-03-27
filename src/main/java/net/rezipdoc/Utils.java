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
	private static final Set<String> SUFFIXES_ZIP;

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

		final Set<String> suffixesZip = new HashSet<>();
		suffixesZip.add("zip");
		suffixesZip.add("jar");
		SUFFIXES_ZIP = suffixesZip;
	}

	private Utils() {
	}

	/**
	 * Checks whether a file denotes an XML based file format.
	 *
	 * @param fileName     to be checked for known XML file extensions (for example ".xml" and ".svg")
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
		if (!matches && (magicHeader != null) && contentBytes >= magicHeader.length()) {
			try {
				if (contentIn.startsWith(magicHeader.getBytes())) {
					matches = true;
				}
			} finally {
				contentIn.reset();
			}
		}

		return matches;
	}

	/**
	 * Checks whether a file denotes an XML based file format.
	 *
	 * @param fileName     to be checked for known XML file extensions (for example ".xml" and ".svg")
	 * @param contentBytes length of the content in bytes
	 * @param contentIn    to be checked for magic file header for XML: {@literal "<?xml "}
	 * @return whether the supplied file type is XML based
	 */
	public static boolean isXml(final String fileName, final long contentBytes, final BufferedOutputStream contentIn) {
		return isType(fileName, contentBytes, contentIn, "<?xml ", SUFFIXES_XML);
	}

	/**
	 * Checks whether a file denotes an plain-text file format.
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
		return isType(fileName, contentBytes, contentIn, null, SUFFIXES_ZIP);
	}
}
