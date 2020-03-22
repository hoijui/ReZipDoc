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

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @see BinaryUtil
 */
public class BinaryUtilTest {

	@Test
	public void testLogging() throws IOException {

		// TODO Actually capture and check the generated output.
		final Logger logger = Utils.getLogger(BinaryUtilTest.class.getName());
		logger.setLevel(Level.FINE);
		if (logger.isLoggable(Level.INFO)) {
			final BinaryUtil binaryUtil = new BinaryUtil();
			logger.info(binaryUtil.createLibrarySummary());
			if (logger.isLoggable(Level.FINE)) {
				logger.fine(binaryUtil.createManifestPropertiesString());
			}
		}
	}
}
