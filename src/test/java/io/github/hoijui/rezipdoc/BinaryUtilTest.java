/*
 * Copyright (C) 2015-2020, The authors of the ReZipDoc project.
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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @see BinaryUtil
 */
public class BinaryUtilTest {

	private static final String TEST_MANIFEST
		= "-By: hoijui\n"
		+ "Bnd-LastModified: 1584876314307\n"
		+ "Build-Jdk: 1.8.0_151\n"
		+ "Bundle-Description: A Git filter and textconv for converting ZIP based b\n"
		+ " inary files to an uncompressed version of themselves, which works bet\n"
		+ " ter with gits delta-compression and diffs\n"
		+ "Bundle-License: http://www.gnu.org/licenses/gpl-3.0.html\n"
		+ "Bundle-ManifestVersion: 2\n"
		+ "Bundle-Name: ReZipDoc\n"
		+ "Bundle-SymbolicName: io.github.hoijui.rezipdoc\n"
		+ "Bundle-Version: 0.5.0.SNAPSHOT\n"
		+ "Created-By: Apache Maven Bundle Plugin\n"
		+ "Export-Package: io.github.hoijui.rezipdoc;uses:=\"javax.xml.parsers,javax\n"
		+ " .xml.transform,javax.xml.xpath,org.xml.sax\";version=\"0.5.0\"\n"
		+ "Import-Package: javax.xml.namespace,javax.xml.parsers,javax.xml.transfor\n"
		+ " m,javax.xml.transform.dom,javax.xml.transform.stream,javax.xml.xpath,or\n"
		+ " g.w3c.dom,org.xml.sax\n"
		+ "Require-Capability: osgi.ee;filter:=\"(&(osgi.ee=JavaSE)(version=1.8))\"\n"
		+ "Manifest-Version: 1.0\n"
		+ "Tool: Bnd-2.4.1.201501161923\n";

	private Logger nullLogger() {

		final Logger logger = Utils.getLogger(BinaryUtilTest.class.getName());
		logger.setUseParentHandlers(false);
		for (final Handler handler : logger.getHandlers()) {
			logger.removeHandler(handler);
		}

		return logger;
	}

	private void testDefaultNoError(final BinaryUtil binaryUtil) throws IOException {

		try {
			final Logger logger = nullLogger();
			logger.setLevel(Level.FINE);
			if (logger.isLoggable(Level.INFO)) {
				logger.info(binaryUtil.createLibrarySummary());
				if (logger.isLoggable(Level.FINE)) {
					logger.fine(binaryUtil.createManifestPropertiesString(true));
				}
			}
		} catch (final Throwable thr) {
			assertTrue("Default manifest file parsing and logging failed: " + thr.getMessage(), false);
		}
	}

	@Test
	public void testDefaultNoError() throws IOException {

		final BinaryUtil binaryUtil = new BinaryUtil(new ByteArrayInputStream(TEST_MANIFEST.getBytes()));

		testDefaultNoError(binaryUtil);
	}

	@Test
	public void testDefaultNoErrorJar() throws IOException {

		final BinaryUtil binaryUtil = new BinaryUtil();

		testDefaultNoError(binaryUtil);
	}

	@Test
	public void testLibrarySummary() throws IOException {

		final BinaryUtil binaryUtil = new BinaryUtil(new ByteArrayInputStream(TEST_MANIFEST.getBytes()));

		final String expectedLibrarySummary = "\nName:        ReZipDoc\nDescription: A Git filter and textconv for converting ZIP based binary files to an uncompressed version of themselves, which works better with gits delta-compression and diffs\nVersion:     0.5.0.SNAPSHOT\nLicense:     http://www.gnu.org/licenses/gpl-3.0.html";
		final String actualLibrarySummary = binaryUtil.createLibrarySummary();
		assertEquals(expectedLibrarySummary, actualLibrarySummary);
	}

	@Test
	public void testLibrarySummaryEmpty() throws IOException {

		final BinaryUtil binaryUtil = new BinaryUtil(new ByteArrayInputStream("".getBytes()));

		final String expectedLibrarySummary = "\nName:        <unknown>\nDescription: <unknown>\nVersion:     <unknown>\nLicense:     <unknown>";
		final String actualLibrarySummary = binaryUtil.createLibrarySummary();
		assertEquals(expectedLibrarySummary, actualLibrarySummary);
	}

	@Test
	public void testManifestProperties() throws IOException {

		final BinaryUtil binaryUtil = new BinaryUtil(new ByteArrayInputStream(TEST_MANIFEST.getBytes()));

		final String expectedManifestProperties
			= "                             -By -> \"hoijui\"\n"
			+ "                Bnd-LastModified -> \"1584876314307\"\n"
			+ "                       Build-Jdk -> \"1.8.0_151\"\n"
			+ "              Bundle-Description -> \"A Git filter and textconv for converting ZIP based binary files to an uncompressed version of themselves, which works better with gits delta-compression and diffs\"\n"
			+ "                  Bundle-License -> \"http://www.gnu.org/licenses/gpl-3.0.html\"\n"
			+ "          Bundle-ManifestVersion -> \"2\"\n"
			+ "                     Bundle-Name -> \"ReZipDoc\"\n"
			+ "             Bundle-SymbolicName -> \"io.github.hoijui.rezipdoc\"\n"
			+ "                  Bundle-Version -> \"0.5.0.SNAPSHOT\"\n"
			+ "                      Created-By -> \"Apache Maven Bundle Plugin\"\n"
			+ "                  Export-Package -> \"io.github.hoijui.rezipdoc;uses:=\"javax.xml.parsers,javax.xml.transform,javax.xml.xpath,org.xml.sax\";version=\"0.5.0\"\"\n"
			+ "                  Import-Package -> \"javax.xml.namespace,javax.xml.parsers,javax.xml.transform,javax.xml.transform.dom,javax.xml.transform.stream,javax.xml.xpath,org.w3c.dom,org.xml.sax\"\n"
			+ "                Manifest-Version -> \"1.0\"\n"
			+ "              Require-Capability -> \"osgi.ee;filter:=\"(&(osgi.ee=JavaSE)(version=1.8))\"\"\n"
			+ "                            Tool -> \"Bnd-2.4.1.201501161923\"\n";
		final String actualManifestProperties = binaryUtil.createManifestPropertiesString(true);
		assertEquals(expectedManifestProperties, actualManifestProperties);
	}

	@Test
	public void testManifestPropertiesEmpty() throws IOException {

		final BinaryUtil binaryUtil = new BinaryUtil(new ByteArrayInputStream("".getBytes()));

		final String expectedManifestProperties = "";
		final String actualManifestProperties = binaryUtil.createManifestPropertiesString(false);
		assertEquals(expectedManifestProperties, actualManifestProperties);
	}
}
