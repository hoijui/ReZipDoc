/*
 * Copyright (C) 2019, The authors of the ReZipDoc project.
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A global hub, providing general information about this software.
 * The information available here comes either from the libraries manifest file at
 * "{JAR_ROOT}/META-INF/MANIFEST.MF", or is fetched directly from code inside this software.
 */
public final class BinaryUtil {

	private static final String MANIFEST_FILE = "/META-INF/MANIFEST.MF";
	@SuppressWarnings("WeakerAccess")
	public static final String UNKNOWN_VALUE = "<unknown>";
	private static final char MANIFEST_CONTINUATION_LINE_INDICATOR = ' ';
	/** 1 key + 1 value = 2 parts of a key-value pair */
	private static final int KEY_PLUS_VALUE_COUNT = 2;
	private final Properties manifestProperties;

	@SuppressWarnings("WeakerAccess")
	public BinaryUtil() throws IOException {

		this.manifestProperties = readJarManifest();
	}

	private static Properties parseManifestFile(final InputStream manifestIn) throws IOException {

		final Properties manifestProps = new Properties();

		try (final BufferedReader manifestBufferedIn = new BufferedReader(new InputStreamReader(manifestIn,
				StandardCharsets.UTF_8)))
		{
			String manifestLine = manifestBufferedIn.readLine();
			String currentKey = null;
			final StringBuilder currentValue = new StringBuilder(80);
			// NOTE one property can be specified on multiple lines.
			//   This is done by prepending all but the first line with white-space, for example:
			//   "My-Key: hello, this is my very long property value, which is sp"
			//   " lit over multiple lines, and because we also want to show the "
			//   " third line, we write a little more."
			while (manifestLine != null) {
				// filter out empty lines and comments
				// NOTE Is there really a comment syntax defined for manifest files?
				if (!manifestLine.trim().isEmpty() && !manifestLine.startsWith("[#%]")) {
					if (manifestLine.charAt(0) == MANIFEST_CONTINUATION_LINE_INDICATOR) {
						// remove the initial space and add it to the already read value
						currentValue.append(manifestLine.substring(1));
					} else {
						if (currentKey != null) {
							manifestProps.setProperty(currentKey, currentValue.toString());
						}
						final String[] keyAndValue = manifestLine.split(": ", KEY_PLUS_VALUE_COUNT);
						if (keyAndValue.length < KEY_PLUS_VALUE_COUNT) {
							throw new IOException("Invalid manifest line: \"" + manifestLine + '"');
						}
						currentKey = keyAndValue[0];
						currentValue.setLength(0);
						currentValue.append(keyAndValue[1]);
					}
				}
				manifestLine = manifestBufferedIn.readLine();
			}
			if (currentKey != null) {
				manifestProps.setProperty(currentKey, currentValue.toString());
			}
		}

		return manifestProps;
	}

	private static Properties readJarManifest() throws IOException {

		Properties mavenProps;

		try (final InputStream manifestFileIn = BinaryUtil.class.getResourceAsStream(MANIFEST_FILE)) {
			if (manifestFileIn == null) {
				throw new IOException("Failed locating resource in the classpath: " + MANIFEST_FILE);
			}
			mavenProps = parseManifestFile(manifestFileIn);
		}

		return mavenProps;
	}

	/**
	 * Returns this application JARs {@link #MANIFEST_FILE} properties.
	 * @return the contents of the manifest file as {@code String} to {@code String} mapping
	 */
	@SuppressWarnings("WeakerAccess")
	public Properties getManifestProperties() {
		return manifestProperties;
	}

	public String getVersion() {
		return getManifestProperties().getProperty("Bundle-Version", UNKNOWN_VALUE);
	}

	public String getLicense() {
		return getManifestProperties().getProperty("Bundle-License", UNKNOWN_VALUE);
	}

	@SuppressWarnings("unused")
	public String createManifestPropertiesString() {

		final StringBuilder info = new StringBuilder(1024);

		for (final Map.Entry<Object, Object> manifestEntry : getManifestProperties().entrySet()) {
			final String key = (String) manifestEntry.getKey();
			final String value = (String) manifestEntry.getValue();
			info
					.append(String.format("%32s", key))
					.append(" -> ")
					.append(value)
					.append('\n');
		}

		return info.toString();
	}

	@SuppressWarnings("WeakerAccess")
	public String createLibrarySummary() {

		final StringBuilder summary = new StringBuilder(1024);

		summary
				.append("\nName:        ").append(getManifestProperties().getProperty("Bundle-Name", UNKNOWN_VALUE))
				.append("\nDescription: ").append(getManifestProperties().getProperty("Bundle-Description", UNKNOWN_VALUE))
				.append("\nVersion:     ").append(getVersion())
				.append("\nLicense:     ").append(getLicense());

		return summary.toString();
	}

	public static void main(final String[] args) throws IOException {

		final Logger logger = Utils.getLogger(BinaryUtil.class.getName());
		if (logger.isLoggable(Level.INFO)) {
			final BinaryUtil binaryUtil = new BinaryUtil();
			logger.info(binaryUtil.createLibrarySummary());
			if (logger.isLoggable(Level.FINE)) {
				logger.fine(binaryUtil.createManifestPropertiesString());
			}
		}
	}
}
