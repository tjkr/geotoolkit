/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2015, Geomatys
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation; either
 *    version 2.1 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotoolkit.wps.converters.inputs.reference;

import java.io.IOException;
import org.geotoolkit.data.FeatureCollection;
import org.geotoolkit.wps.converters.ConvertersTestUtils;
import org.geotoolkit.wps.io.WPSEncoding;
import org.geotoolkit.wps.io.WPSMimeType;
import org.geotoolkit.wps.xml.v100.ReferenceType;
import org.junit.Test;

/**
 *
 * @author Théo Zozime
 */
public class ReferenceToFeatureCollectionConverterTest extends org.geotoolkit.test.TestBase {

    @Test
    public void testJSONConversion() throws IOException {

        final FeatureCollection featureCollection = ConvertersTestUtils.initAndRunInputConversion(
                                                      ReferenceType.class,
                                                      FeatureCollection.class,
                                                      "/inputs/featurecollection.json",
                                                      WPSMimeType.APP_GEOJSON.val(),
                                                      WPSEncoding.UTF8.getValue(),
                                                      null);

        // Test the feature collection
        ConvertersTestUtils.assertFeatureCollectionIsValid(featureCollection);
    }
}
