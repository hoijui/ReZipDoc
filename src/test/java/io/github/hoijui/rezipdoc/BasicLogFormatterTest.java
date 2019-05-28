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

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Test;

import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * @see BasicLogFormatter
 */
public class BasicLogFormatterTest {

	private void testMessageOnly(final Level logLevel) {

		final BasicLogFormatter formatter = new BasicLogFormatter();
		final LogRecord record = new LogRecord(logLevel, "Hello World!");
		final String actual = formatter.format(record);
		Assert.assertEquals("Hello World!\n", actual);
	}

	private void testWithException(final Level logLevel) {

		final BasicLogFormatter formatter = new BasicLogFormatter();
		final LogRecord record = new LogRecord(logLevel, "Hello World!");
		record.setThrown(new NullPointerException());
		final String actual = formatter.format(record);
		MatcherAssert.assertThat(actual, CoreMatchers.startsWith("Hello World!\n"
						+ "java.lang.NullPointerException\n"
						+ "	at io.github.hoijui.rezipdoc.BasicLogFormatterTest.testWithException(BasicLogFormatterTest.java:"));
	}

	@Test
	public void testFinest() {
		testMessageOnly(Level.FINEST);
	}

	@Test
	public void testInfo() {
		testMessageOnly(Level.INFO);
	}

	@Test
	public void testWarning() {
		testMessageOnly(Level.WARNING);
	}

	@Test
	public void testSevere() {
		testMessageOnly(Level.SEVERE);
	}

	@Test
	public void testFinestWithException() {
		testWithException(Level.FINEST);
	}

	@Test
	public void testInfoWithException() {
		testWithException(Level.INFO);
	}

	@Test
	public void testWarningWithException() {
		testWithException(Level.WARNING);
	}

	@Test
	public void testSevereWithException() {
		testWithException(Level.SEVERE);
	}
}
