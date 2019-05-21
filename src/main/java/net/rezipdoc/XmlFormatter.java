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

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Takes XML content as input,
 * and reproduces the same content as output,
 * but more pleasing on the human eye,
 * by adding proper line-endings and indents.
 */
@SuppressWarnings("WeakerAccess")
public class XmlFormatter {

	private static final Logger LOGGER = Utils.getLogger(XmlFormatter.class.getName());

	private final int indentSpaces;
	private final String indent;
	private final boolean correct;

	/**
	 * Creates an instance with specific values.
	 * Usually {@code indentSpaces} is used for <em>correct</em>,
	 * while {@code indent} is used for <em>rough and fast</em> pretty'fication.
	 *
	 * @param indentSpaces how many spaces to use per indent
	 * @param indent what string to use for oen indent
	 *   (this might be two spaces or one TAB, for example)
	 * @param correct whether to use <em>correct</em> or <em>rough and fast</em> pretty'fication.
	 *   <em>correct</em> is slower, and works for valid XML only.
	 *   <em>rough and fast</em> might produce weird results if there are
	 *   '{@literal <}' or '{@literal >}' characters which are not part of tags.
	 */
	public XmlFormatter(final int indentSpaces, final String indent, final boolean correct) {

		this.indentSpaces = indentSpaces;
		this.indent = indent;
		this.correct = correct;
	}

	/**
	 * Creates an instance with default values.
	 */
	public XmlFormatter() {
		this(2, "  ", true);
	}

	public static void main(final String[] args) throws IOException {

		if (0 == args.length) {
			new XmlFormatter().prettify(System.in, System.out, createBuffer());
		} else if (1 == args.length) {
			try (final InputStream source = new FileInputStream(args[0])) {
				new XmlFormatter().prettify(source, System.out, createBuffer());
			}
		} else if (2 == args.length) {
			try (final InputStream source = new FileInputStream(args[0]);
					final OutputStream target = new FileOutputStream(args[1]))
			{
				new XmlFormatter().prettify(source, target, createBuffer());
			}
		} else {
			if (LOGGER.isLoggable(Level.WARNING)) {
				LOGGER.warning("Usage:");
				LOGGER.warning(String.format("\t%s in-file.xml out-file.xml",
						XmlFormatter.class.getSimpleName()));
				LOGGER.warning(String.format("\t%s in-file.xml > out-file.xml",
						XmlFormatter.class.getSimpleName()));
				LOGGER.warning(String.format("\t%s < in-file.xml > out-file.xml",
						XmlFormatter.class.getSimpleName()));
			}
			System.exit(1);
		}
	}

	private static byte[] createBuffer() {
		return new byte[2048];
	}

	/**
	 * Reformats XML content to be easy on the human eye.
	 *
	 * @param xmlIn  the supplier of XML content to pretty-print
	 * @param xmlOut where the pretty XML content shall be written to
	 * @param buffer may be used internally for whatever in- or out-buffering there might be
	 * @throws IOException if any input or output fails
	 */
	public void prettify(final InputStream xmlIn, final OutputStream xmlOut, final byte[] buffer)
			throws IOException {

		try {
			if (correct) {
				prettifyCorrect(xmlIn, xmlOut);
			} else {
				prettifyRoughAndFast(xmlIn, xmlOut, buffer);
			}
		} catch (final Exception exc) {
			LOGGER.log(Level.WARNING, "Failed to pretty print; fallback to carbon-copy", exc);
			// In case of failure of pretty-printing, use the XML as-is
			Utils.transferTo(xmlIn, xmlOut, buffer);
		}
	}

	public void prettifyCorrect(final InputStream xmlIn, final OutputStream xmlOut)
			throws IOException,
			ParserConfigurationException,
			SAXException,
			XPathExpressionException,
			TransformerException
	{
		final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		final Document document = documentBuilderFactory
				.newDocumentBuilder()
				.parse(new InputSource(xmlIn));

		XPath xPath = XPathFactory.newInstance().newXPath();
		NodeList nodeList = (NodeList) xPath.evaluate("//text()[normalize-space()='']",
				document,
				XPathConstants.NODESET);

		for (int i = 0; i < nodeList.getLength(); ++i) {
			Node node = nodeList.item(i);
			node.getParentNode().removeChild(node);
		}

		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
//		transformerFactory.setAttribute("indent-number", indentSpaces);
		Transformer transformer = transformerFactory.newTransformer();
		//transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", String.valueOf(indentSpaces));

		StreamResult streamResult = new StreamResult(xmlOut);

		transformer.transform(new DOMSource(document), streamResult);
	}

	public void prettifyRoughAndFast(final InputStream xmlIn, final OutputStream xmlOut, final byte[] buffer)
			throws IOException {

		// this is a kind of stack, denoting the number of indents
		int numIndents = 0;

		// prepare the in-buffer
		final StringBuilder inBuffer = new StringBuilder();
		// prepare the out stream wrapper,
		// which allows to write string data more comfortably
		final PrintStream xmlOutPrinter = new PrintStream(xmlOut);

		for (int readBytes = xmlIn.read(buffer); readBytes > 0; readBytes = xmlIn.read(buffer)) {
			// convert the newly read part to a string
			// and append it to the leftover, which was already read
			inBuffer.append(new String(buffer, 0, readBytes));

			// split all the content we have at the moment into rows (think: lines)
			final String[] rows = inBuffer.toString()
					.replaceAll(">", ">\n")
					.replaceAll("<", "\n<")
					.split("\n");

			// handle all except the last row,
			// because it is potentially incomplete
			for (int ir = 0; ir < rows.length - 1; ir++) {
				numIndents = handleRow(xmlOutPrinter, rows[ir].trim(), numIndents);
			}

			// fill the buffer with only the last row,
			// which is potentially incomplete
			inBuffer.setLength(0);
			inBuffer.append(rows[rows.length - 1]);
		}
		// handle the last row
		handleRow(xmlOutPrinter, inBuffer.toString().trim(), numIndents);
	}

	private static void appendIndents(final PrintStream output, final int numIndents, String indent) {

		for (int ii = 0; ii < numIndents; ii++) {
			output.append(indent);
		}
	}

	public int handleRow(final PrintStream xmlOut, final String row, int numIndents) {
		if (!row.isEmpty()) {
			if (row.startsWith("<?")) {
				xmlOut.append(row).append("\n");
			} else if (row.startsWith("</")) {
				--numIndents;
				appendIndents(xmlOut, numIndents, indent);
				xmlOut.append(row).append("\n");
			} else if (row.startsWith("<") && !row.endsWith("/>")) {
				numIndents++;
				appendIndents(xmlOut, numIndents, indent);
				xmlOut.append(row).append("\n");
				if (row.endsWith("]]>")) {
					numIndents--;
				}
			} else {
				appendIndents(xmlOut, numIndents, indent);
				xmlOut.append(row).append("\n");
			}
		}

		return numIndents;
	}
}
