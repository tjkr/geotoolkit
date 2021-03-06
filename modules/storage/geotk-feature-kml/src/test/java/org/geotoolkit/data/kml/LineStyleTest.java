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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;

import org.geotoolkit.data.kml.model.IdAttributes;
import org.geotoolkit.data.kml.model.Kml;
import org.geotoolkit.data.kml.model.KmlException;
import org.geotoolkit.data.kml.model.KmlModelConstants;
import org.geotoolkit.data.kml.model.LineString;
import org.geotoolkit.data.kml.model.LineStyle;
import org.geotoolkit.data.kml.model.Style;
import org.geotoolkit.data.kml.xml.KmlReader;
import org.geotoolkit.data.kml.xml.KmlWriter;
import org.geotoolkit.feature.FeatureUtilities;
import org.geotoolkit.xml.DomCompare;

import org.junit.Test;

import org.geotoolkit.feature.Feature;
import org.geotoolkit.feature.FeatureFactory;
import org.geotoolkit.feature.Property;
import org.xml.sax.SAXException;
import static org.junit.Assert.*;

/**
 *
 * @author Samuel Andrés
 * @module pending
 */
public class LineStyleTest extends org.geotoolkit.test.TestBase {

    private static final double DELTA = 0.000000000001;
    private static final String pathToTestFile = "src/test/resources/org/geotoolkit/data/kml/lineStyle.kml";
    private static final FeatureFactory FF = FeatureFactory.LENIENT;

    public LineStyleTest() {
    }

    @Test
    public void lineStyleReadTest() throws IOException, XMLStreamException, URISyntaxException, KmlException {

        final KmlReader reader = new KmlReader();
        reader.setInput(new File(pathToTestFile));
        final Kml kmlObjects = reader.read();
        reader.dispose();

        final Feature document = kmlObjects.getAbstractFeature();
        assertEquals("LineStyle.kml", document.getProperty(KmlModelConstants.ATT_NAME.getName()).getValue());
        assertTrue((Boolean) document.getProperty(KmlModelConstants.ATT_OPEN.getName()).getValue());

        assertEquals(1, document.getProperties(KmlModelConstants.ATT_STYLE_SELECTOR.getName()).size());

        Iterator<Property> i = document.getProperties(KmlModelConstants.ATT_STYLE_SELECTOR.getName()).iterator();

        if (i.hasNext()) {
            Object object = i.next().getValue();
            assertTrue(object instanceof Style);
            Style style = (Style) object;
            assertEquals("linestyleExample", style.getIdAttributes().getId());
            LineStyle lineStyle = style.getLineStyle();
            assertEquals(new Color(255, 0, 0, 127), lineStyle.getColor());
            assertEquals(4, lineStyle.getWidth(), DELTA);
        }

        assertEquals(1, document.getProperties(KmlModelConstants.ATT_DOCUMENT_FEATURES.getName()).size());

        i = document.getProperties(KmlModelConstants.ATT_DOCUMENT_FEATURES.getName()).iterator();

        if (i.hasNext()) {
            Object object = i.next();
            assertTrue(object instanceof Feature);
            Feature placemark = (Feature) object;
            assertTrue(placemark.getType().equals(KmlModelConstants.TYPE_PLACEMARK));
            assertEquals("LineStyle Example", placemark.getProperty(KmlModelConstants.ATT_NAME.getName()).getValue());
            assertEquals(new URI("#linestyleExample"), placemark.getProperty(KmlModelConstants.ATT_STYLE_URL.getName()).getValue());
            assertTrue(placemark.getProperty(KmlModelConstants.ATT_PLACEMARK_GEOMETRY.getName()).getValue() instanceof LineString);
            LineString lineString = (LineString) placemark.getProperty(KmlModelConstants.ATT_PLACEMARK_GEOMETRY.getName()).getValue();
            assertTrue(lineString.getExtrude());
            assertTrue(lineString.getTessellate());
            CoordinateSequence coordinates = lineString.getCoordinateSequence();
            assertEquals(2, coordinates.size());
            Coordinate coordinate0 = coordinates.getCoordinate(0);
            assertEquals(-122.364383, coordinate0.x, DELTA);
            assertEquals(37.824664, coordinate0.y, DELTA);
            assertEquals(0, coordinate0.z, DELTA);
            Coordinate coordinate1 = coordinates.getCoordinate(1);
            assertEquals(-122.364152, coordinate1.x, DELTA);
            assertEquals(37.824322, coordinate1.y, DELTA);
            assertEquals(0, coordinate1.z, DELTA);
        }

    }

    @Test
    public void lineStyleWriteTest() throws KmlException, IOException, XMLStreamException, ParserConfigurationException, SAXException, URISyntaxException {
        final KmlFactory kmlFactory = DefaultKmlFactory.getInstance();

        final Coordinate coordinate0 = kmlFactory.createCoordinate(-122.364383, 37.824664, 0);
        final Coordinate coordinate1 = kmlFactory.createCoordinate(-122.364152, 37.824322, 0);
        final CoordinateSequence coordinates = kmlFactory.createCoordinates(Arrays.asList(coordinate0, coordinate1));
        final LineString lineString = kmlFactory.createLineString(coordinates);
        lineString.setTessellate(true);
        lineString.setExtrude(true);

        final Feature placemark = kmlFactory.createPlacemark();
        final Collection<Property> placemarkProperties = placemark.getProperties();
        placemarkProperties.add(FF.createAttribute("LineStyle Example", KmlModelConstants.ATT_NAME, null));
        placemarkProperties.add(FF.createAttribute(new URI("#linestyleExample"), KmlModelConstants.ATT_STYLE_URL, null));
        placemarkProperties.add(FF.createAttribute(lineString, KmlModelConstants.ATT_PLACEMARK_GEOMETRY, null));

        final Style style = kmlFactory.createStyle();
        final LineStyle lineStyle = kmlFactory.createLineStyle();
        lineStyle.setWidth(4);
        lineStyle.setColor(new Color(255, 0, 0, 127));
        style.setLineStyle(lineStyle);
        final IdAttributes idAttributes = kmlFactory.createIdAttributes("linestyleExample", null);
        style.setIdAttributes(idAttributes);

        final Feature document = kmlFactory.createDocument();
        final Collection<Property> documentProperties = document.getProperties();
        documentProperties.add(FF.createAttribute(style, KmlModelConstants.ATT_STYLE_SELECTOR,null));
        documentProperties.add(FeatureUtilities.wrapProperty(placemark, KmlModelConstants.ATT_DOCUMENT_FEATURES));
        documentProperties.add(FF.createAttribute("LineStyle.kml", KmlModelConstants.ATT_NAME,null));
        document.getProperty(KmlModelConstants.ATT_OPEN.getName()).setValue(Boolean.TRUE);

        final Kml kml = kmlFactory.createKml(null, document, null, null);

        final File temp = File.createTempFile("testLineStyle", ".kml");
        temp.deleteOnExit();

        final KmlWriter writer = new KmlWriter();
        writer.setOutput(temp);
        writer.write(kml);
        writer.dispose();

        DomCompare.compare(
                new File(pathToTestFile), temp);

    }
}
