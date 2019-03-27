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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * The program takes a single argument, the name of the ZIP file to convert,
 * and produces a more human readable, textual representation of its content on stdout.
 * It is meant to be used as a git textconv filter; see the README for details.
 */
@SuppressWarnings("WeakerAccess")
public class ZipDoc {

	private ZipDoc() {
	}

	public static void main(final String[] argv) throws IOException, TransformerException {

		if (1 != argv.length) {
			System.err.printf("Usage: %s in-file.zip > text-representation.txt%n", ZipDoc.class.getSimpleName());
			System.exit(1);
		}

		new ZipDoc().transform(argv[0]);
	}

	/**
	 * Reads the specified ZIP file and outputs a textual representation of its to stdout.
	 *
	 * @param zipFilePath the ZIP file to convert to a text
	 * @throws IOException          if any input or output fails
	 * @throws TransformerException if XML pretty-printing fails
	 */
	public void transform(final String zipFilePath) throws IOException, TransformerException {

		try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath))) {
			transform(zipIn, System.out);
		}
	}

	/**
	 * Reads the specified ZIP document and outputs a textual representation
	 * of its to the specified output stream.
	 *
	 * @param zipIn  the ZIP document to convert to a text
	 * @param output where the text gets written to
	 * @throws IOException          if any input or output fails
	 * @throws TransformerException if XML pretty-printing fails
	 */
	public void transform(final ZipInputStream zipIn, final PrintStream output)
			throws IOException, TransformerException
	{
		final Transformer serializer = SAXTransformerFactory.newInstance().newTransformer();
		serializer.setOutputProperty(OutputKeys.INDENT, "yes");
		serializer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		serializer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
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
			int len;
			while ((len = zipIn.read(buffer)) > 0) {
				uncompressedOutChecked.write(buffer, 0, len);
			}
			zipIn.closeEntry();

			if (Utils.isXml(entry.getName(), entry.getSize(), uncompressedOutRaw)) {
				// XML file: pretty-print the data to stdout
				final InputSource inBuffer = new InputSource(uncompressedOutRaw.createInputStream());
				serializer.transform(new SAXSource(inBuffer), new StreamResult(output));
			} else if (Utils.isPlainText(entry.getName(), entry.getSize(), uncompressedOutRaw)) {
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
