/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2009-2015, Geomatys
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotoolkit.coverage.postgresql;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.MissingResourceException;
import org.opengis.util.InternationalString;
import org.apache.sis.util.iso.ResourceInternationalString;
import org.apache.sis.util.resources.IndexedResourceBundle;


/**
 * Locale-dependent resources for words or simple sentences.
 *
 * @author Martin Desruisseaux (Geomatys)
 */
public final class Bundle extends IndexedResourceBundle {
    /**
     * Resource keys. This class is used when compiling sources, but no dependencies to
     * {@code Keys} should appear in any resulting class files. Since the Java compiler
     * inlines final integer values, using long identifiers will not bloat the constant
     * pools of compiled classes.
     *
     * @author Martin Desruisseaux (IRD)
     */
    public static final class Keys {
        private Keys() {
        }

        /**
         * Database
         */
        public static final short database = 0;

        /**
         * Database name
         */
        public static final short database_remarks = 1;

        /**
         * DataSource
         */
        public static final short datasource = 2;

        /**
         * DataSource java object
         */
        public static final short datasource_remarks = 3;

        /**
         * Geotoolkit pyramid model on postgresql database
         */
        public static final short description = 4;

        /**
         * Fetch size
         */
        public static final short fetch_size = 5;

        /**
         * Number of records read with each iteraction with the dbms
         */
        public static final short fetch_size_remarks = 6;

        /**
         * Host
         */
        public static final short host = 7;

        /**
         * Host database server
         */
        public static final short host_remarks = 8;

        /**
         * Max connections
         */
        public static final short max_connections = 9;

        /**
         * maximum number of open connections
         */
        public static final short max_connections_remarks = 10;

        /**
         * Min connections
         */
        public static final short min_connections = 11;

        /**
         * minimum number of open connections o keep alive
         */
        public static final short min_connections_remarks = 12;

        /**
         * Password
         */
        public static final short password = 13;

        /**
         * Password used to login
         */
        public static final short password_remarks = 14;

        /**
         * Port
         */
        public static final short port = 15;

        /**
         * Database connection port
         */
        public static final short port_remarks = 16;

        /**
         * Schema
         */
        public static final short schema = 17;

        /**
         * Database schema
         */
        public static final short schema_remarks = 18;

        /**
         * Timeout(s)
         */
        public static final short timeout = 19;

        /**
         * number of seconds the connection pool wait for login
         */
        public static final short timeout_remarks = 20;

        /**
         * PosgreSQL
         */
        public static final short title = 21;

        /**
         * User
         */
        public static final short user = 22;

        /**
         * user name to login as
         */
        public static final short user_remarks = 23;

        /**
         * Validate connection
         */
        public static final short validate_connections = 24;

        /**
         * Check connection is alive before using it
         */
        public static final short validate_connections_remarks = 25;
    }

    /**
     * Constructs a new resource bundle loading data from the given UTF file.
     *
     * @param filename The file or the JAR entry containing resources.
     */
    public Bundle(final java.net.URL filename) {
        super(filename);
    }

    /**
     * Returns resources in the given locale.
     *
     * @param  locale The locale, or {@code null} for the default locale.
     * @return Resources in the given locale.
     * @throws MissingResourceException if resources can't be found.
     */
    public static Bundle getResources(Locale locale) throws MissingResourceException {
        return getBundle(Bundle.class, locale);
    }

    /**
     * The international string to be returned by {@link formatInternational}.
     */
    private static final class International extends ResourceInternationalString {
        private static final long serialVersionUID = -9199238559657784488L;

        International(final int key) {
            super(Bundle.class.getName(), String.valueOf(key));
        }

        @Override
        protected ResourceBundle getBundle(final Locale locale) {
            return getResources(locale);
        }
    }

    /**
     * Gets an international string for the given key. This method does not check for the key
     * validity. If the key is invalid, then a {@link MissingResourceException} may be thrown
     * when a {@link InternationalString#toString} method is invoked.
     *
     * @param  key The key for the desired string.
     * @return An international string for the given key.
     */
    public static InternationalString formatInternational(final short key) {
        return new International(key);
    }

    /**
     * Gets an international string for the given key. This method does not check for the key
     * validity. If the key is invalid, then a {@link MissingResourceException} may be thrown
     * when a {@link InternationalString#toString} method is invoked.
     *
     * {@note This method is redundant with the one expecting <code>Object...</code>, but is
     *        provided for binary compatibility with previous Geotk versions. It also avoid the
     *        creation of a temporary array. There is no risk of confusion since the two methods
     *        delegate their work to the same <code>format</code> method anyway.}
     *
     * @param  key The key for the desired string.
     * @param  arg Values to substitute to "{0}".
     * @return An international string for the given key.
     *
     * @todo Current implementation just invokes {@link #format}. Need to format only when
     *       {@code toString(Locale)} is invoked.
     */
    public static InternationalString formatInternational(final short key, final Object arg) {
        return new org.apache.sis.util.iso.SimpleInternationalString(format(key, arg));
    }

    /**
     * Gets an international string for the given key. This method does not check for the key
     * validity. If the key is invalid, then a {@link MissingResourceException} may be thrown
     * when a {@link InternationalString#toString} method is invoked.
     *
     * @param  key The key for the desired string.
     * @param  args Values to substitute to "{0}", "{1}", <i>etc</i>.
     * @return An international string for the given key.
     *
     * @todo Current implementation just invokes {@link #format}. Need to format only when
     *       {@code toString(Locale)} is invoked.
     */
    public static InternationalString formatInternational(final short key, final Object... args) {
        return new org.apache.sis.util.iso.SimpleInternationalString(format(key, args));
    }

    /**
     * Gets a string for the given key from this resource bundle or one of its parents.
     *
     * @param  key The key for the desired string.
     * @return The string for the given key.
     * @throws MissingResourceException If no object for the given key can be found.
     */
    public static String format(final short key) throws MissingResourceException {
        return getResources(null).getString(key);
    }

    /**
     * Gets a string for the given key are replace all occurrence of "{0}"
     * with values of {@code arg0}.
     *
     * @param  key The key for the desired string.
     * @param  arg0 Value to substitute to "{0}".
     * @return The formatted string for the given key.
     * @throws MissingResourceException If no object for the given key can be found.
     */
    public static String format(final short  key,
                                final Object arg0) throws MissingResourceException
    {
        return getResources(null).getString(key, arg0);
    }

    /**
     * Gets a string for the given key are replace all occurrence of "{0}",
     * "{1}", with values of {@code arg0}, {@code arg1}.
     *
     * @param  key The key for the desired string.
     * @param  arg0 Value to substitute to "{0}".
     * @param  arg1 Value to substitute to "{1}".
     * @return The formatted string for the given key.
     * @throws MissingResourceException If no object for the given key can be found.
     */
    public static String format(final short  key,
                                final Object arg0,
                                final Object arg1) throws MissingResourceException
    {
        return getResources(null).getString(key, arg0, arg1);
    }

    /**
     * Gets a string for the given key are replace all occurrence of "{0}",
     * "{1}", with values of {@code arg0}, {@code arg1}, etc.
     *
     * @param  key The key for the desired string.
     * @param  arg0 Value to substitute to "{0}".
     * @param  arg1 Value to substitute to "{1}".
     * @param  arg2 Value to substitute to "{2}".
     * @return The formatted string for the given key.
     * @throws MissingResourceException If no object for the given key can be found.
     */
    public static String format(final short  key,
                                final Object arg0,
                                final Object arg1,
                                final Object arg2) throws MissingResourceException
    {
        return getResources(null).getString(key, arg0, arg1, arg2);
    }
}
