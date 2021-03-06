/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2010, Geomatys
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
package org.geotoolkit.data.kml;

import java.net.URISyntaxException;
import org.geotoolkit.data.kml.xml.KmlReader;
import java.io.File;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;

import org.geotoolkit.data.kml.model.AbstractView;
import org.geotoolkit.data.kml.model.Kml;
import org.geotoolkit.data.kml.model.KmlException;
import org.geotoolkit.data.kml.model.KmlModelConstants;
import org.geotoolkit.data.kml.model.LookAt;
import org.geotoolkit.data.kml.xml.KmlWriter;
import org.geotoolkit.xml.DomCompare;

import org.junit.Test;

import org.geotoolkit.feature.Feature;
import org.geotoolkit.feature.FeatureFactory;
import static org.junit.Assert.*;
import org.xml.sax.SAXException;

/**
 *
 * @author Samuel Andrés
 * @module pending
 */
public class LookAtTest extends org.geotoolkit.test.TestBase {

    private static final double DELTA = 0.000000000001;
    private static final String pathToTestFile = "src/test/resources/org/geotoolkit/data/kml/lookAt.kml";
    private static final FeatureFactory FF = FeatureFactory.LENIENT;

    public LookAtTest() {
    }

    @Test
    public void lookAtReadTest() throws IOException, XMLStreamException, KmlException, URISyntaxException {

        final KmlReader reader = new KmlReader();
        reader.setInput(new File(pathToTestFile));
        final Kml kmlObjects = reader.read();
        reader.dispose();

        final Feature folder = kmlObjects.getAbstractFeature();
        assertTrue(folder.getType().equals(KmlModelConstants.TYPE_FOLDER));
        final AbstractView view = (AbstractView) folder.getProperty(KmlModelConstants.ATT_VIEW.getName()).getValue();
        assertTrue(view instanceof LookAt);

        final LookAt lookAt = (LookAt) view;
        assertEquals(-122.0839597145766, lookAt.getLongitude(), DELTA);
        assertEquals(37.42222904525232, lookAt.getLatitude(), DELTA);
        assertEquals(1000.34, lookAt.getAltitude(), DELTA);
        assertEquals(-148.4122922628044, lookAt.getHeading(), DELTA);
        assertEquals(40.5575073395506, lookAt.getTilt(), DELTA);
        assertEquals(500.6566641072245, lookAt.getRange(), DELTA);

    }

    @Test
    public void lookAtWriteTest() throws KmlException, IOException, XMLStreamException, ParserConfigurationException, SAXException {
        final KmlFactory kmlFactory = DefaultKmlFactory.getInstance();

        final double longitude = -122.0839597145766;
        final double latitude = 37.42222904525232;
        final double altitude = 1000.34;
        final double heading = -148.4122922628044;
        final double tilt = 40.5575073395506;
        final double range = 500.6566641072245;

        final LookAt lookAt = kmlFactory.createLookAt();
        lookAt.setLongitude(longitude);
        lookAt.setLatitude(latitude);
        lookAt.setAltitude(altitude);
        lookAt.setHeading(heading);
        lookAt.setTilt(tilt);
        lookAt.setRange(range);
        final Feature folder = kmlFactory.createFolder();
        folder.getProperties().add(FF.createAttribute(lookAt, KmlModelConstants.ATT_VIEW, null));
        final Kml kml = kmlFactory.createKml(null, folder, null, null);

        final File temp = File.createTempFile("testLookAt", ".kml");
        temp.deleteOnExit();

        final KmlWriter writer = new KmlWriter();
        writer.setOutput(temp);
        writer.write(kml);
        writer.dispose();

        DomCompare.compare(
                new File(pathToTestFile), temp);

    }
}
