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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class BasicLogFormatter extends Formatter {

	@Override
	public synchronized String format(final LogRecord record) {

		final StringWriter stringW = new StringWriter();
		final PrintWriter printW = new PrintWriter(stringW);
		printW.println(formatMessage(record));
		if (record.getThrown() != null) {
			record.getThrown().printStackTrace(printW);
			printW.close();
		}
		return stringW.toString();
	}
}
