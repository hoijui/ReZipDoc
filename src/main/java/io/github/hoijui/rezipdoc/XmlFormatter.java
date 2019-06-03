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
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
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

	private static final int DEFAULT_BUFFER_SIZE = 2048;
	private static final int DEFAULT_ARG_INDENT_SPACES = 2;
	private static final String DEFAULT_ARG_INDENT = "  ";
	private static final boolean DEFAULT_ARG_CORRECT = true;

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
		this(DEFAULT_ARG_INDENT_SPACES, DEFAULT_ARG_INDENT, DEFAULT_ARG_CORRECT);
	}

	private static void printUsage(final Level logLevel) {

		if (LOGGER.isLoggable(logLevel)) {
			LOGGER.log(logLevel, "Usage examples:");
			LOGGER.log(logLevel, String.format("\t%s in-file.xml out-file.xml",
					XmlFormatter.class.getSimpleName()));
			LOGGER.log(logLevel, String.format("\t%s in-file.xml > out-file.xml",
					XmlFormatter.class.getSimpleName()));
			LOGGER.log(logLevel, String.format("\t%s < in-file.xml > out-file.xml",
					XmlFormatter.class.getSimpleName()));
		}
	}

	private static InputStream createInput(final Path inFile) throws IOException {

		InputStream in;
		if (inFile == null) {
			in = new BufferedInputStream(System.in, 64) {
				@Override
				public void close() {
					// NOTE We do explicitly NOT close the underlying stream
				}
			};
		} else {
			in = Files.newInputStream(inFile);
		}
		return in;
	}

	private static OutputStream createOutput(final Path outFile) throws IOException {

		OutputStream out;
		if (outFile == null) {
			out = new BufferedOutputStream(System.out, 64) {
				@Override
				public void close() {
					// NOTE We do explicitly NOT close the underlying stream
				}
			};
		} else {
			out = Files.newOutputStream(outFile);
		}
		return out;
	}

	public static void main(final String[] args) {

		final List<String> argsL = Arrays.asList(args);
		if (argsL.contains("-h") || argsL.contains("--help")) {
			printUsage(Level.INFO);
		} else {
			// normal usage: prettify input to output
			int indentSpaces = DEFAULT_ARG_INDENT_SPACES;
			String indent = DEFAULT_ARG_INDENT;
			boolean correct = DEFAULT_ARG_CORRECT;
			Path inFile = null;
			Path outFile = null;
			int bufferSize = DEFAULT_BUFFER_SIZE;
			final Iterator<String> argsIt = argsL.iterator();
			while (argsIt.hasNext()) {
				final String arg = argsIt.next();
				if ("-r".equals(arg) || "--rough".equals(arg)) {
					correct = false;
				} else if ("--indent-spaces".equals(arg)) {
					indentSpaces = Integer.valueOf(argsIt.next());
				} else if ("--indent".equals(arg)) {
					indent = argsIt.next();
				} else if ("-i".equals(arg) || "--input".equals(arg)) {
					inFile = Paths.get(argsIt.next());
				} else if ("-o".equals(arg) || "--output".equals(arg)) {
					outFile = Paths.get(argsIt.next());
				} else if ("-b".equals(arg) || "--buffer-size".equals(arg)) {
					bufferSize = Integer.valueOf(argsIt.next());
				} else {
					if (LOGGER.isLoggable(Level.SEVERE)) {
						LOGGER.log(Level.SEVERE, "Unknown argument '" + arg + "'");
						printUsage(Level.SEVERE);
					}
					System.exit(1);
				}
			}

			final XmlFormatter xmlFormatter = new XmlFormatter(indentSpaces, indent, correct);

			try (InputStream source = createInput(inFile);
					OutputStream target = createOutput(outFile))
			{
				xmlFormatter.prettify(source, target, createBuffer(bufferSize));
			} catch (final Exception exc) {
				if (LOGGER.isLoggable(Level.SEVERE)) {
					LOGGER.log(Level.SEVERE, "Failed to XML pretty-print", exc);
					printUsage(Level.SEVERE);
				}
				System.exit(1);
			}
		}
	}

	private static byte[] createBuffer(final int size) {
		return new byte[size];
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
			throws IOException
	{
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

	/**
	 * Reformats XML content to be easy on the human eye.
	 * NOTE Rather use the
	 *      {@link #prettify(InputStream, OutputStream, byte[]) streamed version},
	 *      as it uses less memory.
	 *
	 * @param xml  the XML content to be pretty-printed
	 * @return pretty XML content
	 * @throws IOException if any input or output fails
	 */
	public String prettify(final String xml) throws IOException {

		final byte[] xmlBytes = xml.getBytes(StandardCharsets.UTF_8);
		try (InputStream xmlIn = new ByteArrayInputStream(xmlBytes);
				ByteArrayOutputStream xmlOut = new ByteArrayOutputStream())
		{
			// NOTE It is a bit hacky to use the input buffer as working buffer,
			//      but should work without problems in this case
			prettify(xmlIn, xmlOut, xmlBytes);
			return new String(xmlOut.toByteArray(), StandardCharsets.UTF_8);
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

		final XPath xPath = XPathFactory.newInstance().newXPath();
		final NodeList nodeList = (NodeList) xPath.evaluate("//text()[normalize-space()='']",
				document,
				XPathConstants.NODESET);

		for (int i = 0; i < nodeList.getLength(); ++i) {
			Node node = nodeList.item(i);
			node.getParentNode().removeChild(node);
		}

		final TransformerFactory transformerFactory = TransformerFactory.newInstance();
		transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		final Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", String.valueOf(indentSpaces));

		final StreamResult streamResult = new StreamResult(xmlOut);

		transformer.transform(new DOMSource(document), streamResult);
	}

	public void prettifyRoughAndFast(final InputStream xmlIn, final OutputStream xmlOut, final byte[] buffer)
			throws IOException
	{
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

	private static void appendIndents(final PrintStream output, final int numIndents, final String indent) {

		for (int ii = 0; ii < numIndents; ii++) {
			output.append(indent);
		}
	}

	private int handleRow(final PrintStream xmlOut, final String row, final int numIndents) {

		int curIndents = numIndents;
		if (!row.isEmpty()) {
			if (row.startsWith("<?")) {
				xmlOut.append(row).append("\n");
			} else if (row.startsWith("</")) {
				--curIndents;
				appendIndents(xmlOut, curIndents, indent);
				xmlOut.append(row).append("\n");
			} else if (row.startsWith("<") && !row.endsWith("/>")) {
				appendIndents(xmlOut, curIndents, indent);
				xmlOut.append(row).append("\n");
				curIndents++;
				if (row.endsWith("]]>")) {
					curIndents--;
				}
			} else {
				appendIndents(xmlOut, curIndents, indent);
				xmlOut.append(row).append("\n");
			}
		}

		return curIndents;
	}
}
