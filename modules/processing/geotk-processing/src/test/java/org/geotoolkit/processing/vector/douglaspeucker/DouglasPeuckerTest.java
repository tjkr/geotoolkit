/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2011, Geomatys
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
package org.geotoolkit.processing.vector.douglaspeucker;

import org.geotoolkit.process.ProcessException;
import org.geotoolkit.processing.vector.AbstractProcessTest;
import org.opengis.util.NoSuchIdentifierException;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;

import javax.measure.quantity.Length;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import org.geotoolkit.data.FeatureStoreUtilities;
import org.geotoolkit.data.FeatureCollection;
import org.geotoolkit.data.FeatureIterator;
import org.geotoolkit.feature.FeatureTypeBuilder;
import org.geotoolkit.feature.FeatureBuilder;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessFinder;
import org.geotoolkit.referencing.CRS;

import org.geotoolkit.feature.Feature;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.util.FactoryException;


import org.junit.Test;
import org.geotoolkit.feature.Property;
import org.geotoolkit.feature.type.FeatureType;
import org.geotoolkit.feature.type.GeometryDescriptor;

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * JUnit test douglas peucker simplification on FeatureCollection
 *
 * @author Quentin Boileau @module pending
 */
public class DouglasPeuckerTest extends AbstractProcessTest {

    private static FeatureBuilder sfb;
    private static GeometryFactory geometryFactory;
    private static FeatureType type;

    public DouglasPeuckerTest() {
        super("douglasPeucker");
    }

    /**
     * Test DouglasPeucker process with in input two Feature into a FeatureCollection Feature projection should be conic
     * for the first and mercator for second one. The accuracy of the simplification is set to 10 and the "delete small
     * geometry" disable.
     */
    @Test
    public void testDouglasPeucker() throws ProcessException, NoSuchIdentifierException, FactoryException {

        // Inputs
        final FeatureCollection featureList = buildFeatureCollectionInput1();
        Unit<Length> unit = SI.METRE;

        // Process
        ProcessDescriptor desc = ProcessFinder.getProcessDescriptor("vector", "douglasPeucker");

        ParameterValueGroup in = desc.getInputDescriptor().createValue();
        in.parameter("feature_in").setValue(featureList);
        in.parameter("accuracy_in").setValue(new Double(10));
        in.parameter("del_small_geo_in").setValue(false);
        in.parameter("lenient_transform_in").setValue(false);
        org.geotoolkit.process.Process proc = desc.createProcess(in);

        //Features out
        final FeatureCollection featureListOut = (FeatureCollection) proc.call().parameter("feature_out").getValue();
        //Expected Features out
        final FeatureCollection featureListResult = buildFeatureCollectionResult();

        assertEquals(featureListOut.getFeatureType(), featureListResult.getFeatureType());
        assertEquals(featureListOut.getID(), featureListResult.getID());
        assertEquals(featureListOut.size(), featureListResult.size());

        FeatureIterator iteratorOut = featureListOut.iterator();
        FeatureIterator iteratorResult = featureListResult.iterator();

        double precision = 0.0001;
        while (iteratorOut.hasNext() && iteratorResult.hasNext()) {
            Feature featureOut = iteratorOut.next();
            Feature featureResult = iteratorResult.next();

            for (Property propertyOut : featureOut.getProperties()) {
                if (propertyOut.getDescriptor() instanceof GeometryDescriptor) {

                    final Geometry geomOut = (Geometry) propertyOut.getValue();
                    for (Property propertyResult : featureResult.getProperties()) {
                        if (propertyResult.getDescriptor() instanceof GeometryDescriptor) {
                            final Geometry geomResult = (Geometry) propertyResult.getValue();

                            Coordinate[] coordOut = geomOut.getCoordinates();
                            Coordinate[] coordResult = geomResult.getCoordinates();

                            assertEquals(coordOut.length, coordResult.length);
                            for (int i = 0; i < coordOut.length; i++) {
                                assertEquals(coordOut[i].x, coordResult[i].x, precision);
                                assertEquals(coordOut[i].y, coordResult[i].y, precision);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Test DouglasPeucker process when a geometry is smaller than the precision of the simplification, and the user
     * want to delete small features. In input there is a small feature geometry, and in output an empty
     * FeatureCollection.
     */
    @Test
    public void testDouglasPeuckerWithDelete() throws ProcessException, NoSuchIdentifierException, FactoryException {

        // Inputs
        final FeatureCollection featureList = buildFeatureCollectionInput2();
        Unit<Length> unit = SI.METRE;

        // Process
        ProcessDescriptor desc = ProcessFinder.getProcessDescriptor("vector", "douglasPeucker");

        ParameterValueGroup in = desc.getInputDescriptor().createValue();
        in.parameter("feature_in").setValue(featureList);
        in.parameter("accuracy_in").setValue(new Double(61));
        in.parameter("del_small_geo_in").setValue(true);
        in.parameter("lenient_transform_in").setValue(true);
        org.geotoolkit.process.Process proc = desc.createProcess(in);

        //Features out
        final FeatureCollection featureListOut = (FeatureCollection) proc.call().parameter("feature_out").getValue();

        assertTrue(featureListOut.isEmpty());

    }

    /**
     * Test DouglasPeucker process with no del_small_geo_in and lenient_transform_in parameters.
     */
    @Test
    public void testDouglasPeuckerOptionalParam() throws ProcessException, NoSuchIdentifierException, FactoryException {

        // Inputs
        final FeatureCollection featureList = buildFeatureCollectionInput2();
        Unit<Length> unit = SI.METRE;

        // Process
        ProcessDescriptor desc = ProcessFinder.getProcessDescriptor("vector", "douglasPeucker");

        ParameterValueGroup in = desc.getInputDescriptor().createValue();
        in.parameter("feature_in").setValue(featureList);
        in.parameter("accuracy_in").setValue(new Double(61));
        org.geotoolkit.process.Process proc = desc.createProcess(in);

        //Features out
        final FeatureCollection featureListOut = (FeatureCollection) proc.call().parameter("feature_out").getValue();

        assertTrue(!featureListOut.isEmpty());

    }

    /**
     * Test DouglasPeucker process when a geometry is smaller than the precision of the simplification, and the user
     * don't want to delete small features. In input there is a small feature geometry, and in output a feature with
     * null geometry
     */
    @Test
    public void testDouglasPeuckerWithoutDelete() throws ProcessException, NoSuchIdentifierException, FactoryException {

        // Inputs
        final FeatureCollection featureList = buildFeatureCollectionInput2();
        Unit<Length> unit = SI.METRE;

        // Process
        ProcessDescriptor desc = ProcessFinder.getProcessDescriptor("vector", "douglasPeucker");

        ParameterValueGroup in = desc.getInputDescriptor().createValue();
        in.parameter("feature_in").setValue(featureList);
        in.parameter("accuracy_in").setValue(new Double(61));
        in.parameter("del_small_geo_in").setValue(false);
        in.parameter("lenient_transform_in").setValue(true);
        org.geotoolkit.process.Process proc = desc.createProcess(in);

        //Features out
        final FeatureCollection featureListOut = (FeatureCollection) proc.call().parameter("feature_out").getValue();
        //Expected Features out
        final FeatureCollection featureListResult = buildFeatureCollectionResult2();

        assertEquals(featureListResult.getFeatureType(), featureListOut.getFeatureType());
        assertEquals(featureListResult.getID(), featureListOut.getID());
        assertEquals(featureListResult.size(), featureListOut.size());

        FeatureIterator iteratorOut = featureListOut.iterator();
        FeatureIterator iteratorResult = featureListResult.iterator();

        while (iteratorOut.hasNext() && iteratorResult.hasNext()) {
            Feature featureOut = iteratorOut.next();

            for (Property propertyOut : featureOut.getProperties()) {
                if (propertyOut.getDescriptor() instanceof GeometryDescriptor) {

                    final Geometry geomOut = (Geometry) propertyOut.getValue();
                    assertTrue(geomOut == null || geomOut.isEmpty());
                }
            }
        }
    }

    private static FeatureType createSimpleType() throws NoSuchAuthorityCodeException, FactoryException {
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.setName("Building");
        ftb.add("name", String.class);
        ftb.add("position", Polygon.class, CRS.decode("EPSG:3395"));

        ftb.setDefaultGeometry("position");
        final FeatureType sft = ftb.buildFeatureType();
        return sft;
    }

    private static FeatureCollection buildFeatureCollectionInput1() throws FactoryException {
        type = createSimpleType();

        final FeatureCollection featureList = FeatureStoreUtilities.collection("", type);

        geometryFactory = new GeometryFactory();

        Feature myFeature1;
        LinearRing ring = geometryFactory.createLinearRing(
                new Coordinate[]{
                    new Coordinate(0.0, 0.0),
                    new Coordinate(10.0, 20.0),
                    new Coordinate(10.0, 40.0),
                    new Coordinate(20.0, 50.0),
                    new Coordinate(30.0, 40.0),
                    new Coordinate(50.0, 60.0),
                    new Coordinate(60.0, 50.0),
                    new Coordinate(70.0, 40.0),
                    new Coordinate(60.0, 20.0),
                    new Coordinate(40.0, 10.0),
                    new Coordinate(40.0, 20.0),
                    new Coordinate(0.0, 0.0)
                });
        sfb = new FeatureBuilder(type);
        sfb.setPropertyValue("name", "Feature1");
        sfb.setPropertyValue("position", geometryFactory.createPolygon(ring, null));
        myFeature1 = sfb.buildFeature("id-01");
        featureList.add(myFeature1);



        Feature myFeature2;
        LinearRing ring2 = geometryFactory.createLinearRing(
                new Coordinate[]{
                    new Coordinate(-10.0, -10.0),
                    new Coordinate(0.0, -30.0),
                    new Coordinate(-20.0, -20.0),
                    new Coordinate(-30.0, 10.0),
                    new Coordinate(-20.0, 30.0),
                    new Coordinate(0.0, 20.0),
                    new Coordinate(10.0, 10.0),
                    new Coordinate(20.0, -20.0),
                    new Coordinate(10.0, -20.0),
                    new Coordinate(-10.0, -10.0)
                });
        sfb = new FeatureBuilder(type);
        sfb.setPropertyValue("name", "Feature2");
        sfb.setPropertyValue("position", geometryFactory.createPolygon(ring2, null));
        myFeature2 = sfb.buildFeature("id-02");
        featureList.add(myFeature2);

        return featureList;
    }

    private static FeatureCollection buildFeatureCollectionInput2() throws FactoryException {
        type = createSimpleType();

        final FeatureCollection featureList = FeatureStoreUtilities.collection("", type);

        geometryFactory = new GeometryFactory();

        Feature myFeature2;
        LinearRing ring2 = geometryFactory.createLinearRing(
                new Coordinate[]{
                    new Coordinate(-10.0, -10.0),
                    new Coordinate(0.0, -30.0),
                    new Coordinate(-20.0, -20.0),
                    new Coordinate(-30.0, 10.0),
                    new Coordinate(-20.0, 30.0),
                    new Coordinate(0.0, 20.0),
                    new Coordinate(10.0, 10.0),
                    new Coordinate(20.0, -20.0),
                    new Coordinate(10.0, -20.0),
                    new Coordinate(-10.0, -10.0)
                });
        sfb = new FeatureBuilder(type);
        sfb.setPropertyValue("name", "Feature2");
        sfb.setPropertyValue("position", geometryFactory.createPolygon(ring2, null));
        myFeature2 = sfb.buildFeature("id-02");
        featureList.add(myFeature2);

        return featureList;
    }

    private static FeatureCollection buildFeatureCollectionResult() throws FactoryException {
        type = createSimpleType();

        final FeatureCollection featureList = FeatureStoreUtilities.collection("", type);

        geometryFactory = new GeometryFactory();

        Feature myFeature1;
        LinearRing ring = geometryFactory.createLinearRing(
                new Coordinate[]{
                    new Coordinate(0.0, 0.0),
                    new Coordinate(20.0, 50.0),
                    new Coordinate(30.0, 40.0),
                    new Coordinate(50.0, 60.0),
                    new Coordinate(70.0, 40.0),
                    new Coordinate(60.0, 20.0),
                    new Coordinate(0.0, 0.0)
                });
        sfb = new FeatureBuilder(type);
        sfb.setPropertyValue("name", "Feature1");
        sfb.setPropertyValue("position", geometryFactory.createPolygon(ring, null));
        myFeature1 = sfb.buildFeature("id-01");
        featureList.add(myFeature1);



        Feature myFeature2;
        LinearRing ring2 = geometryFactory.createLinearRing(
                new Coordinate[]{
                    new Coordinate(-10.0, -10.0),
                    new Coordinate(0.0, -30.0),
                    new Coordinate(-30.0, 10.0),
                    new Coordinate(-20.0, 30.0),
                    new Coordinate(10.0, 10.0),
                    new Coordinate(20.0, -20.0),
                    new Coordinate(-10.0, -10.0)
                });
        sfb = new FeatureBuilder(type);
        sfb.setPropertyValue("name", "Feature2");
        sfb.setPropertyValue("position", geometryFactory.createPolygon(ring2, null));
        myFeature2 = sfb.buildFeature("id-02");
        featureList.add(myFeature2);

        return featureList;

    }

    private static FeatureCollection buildFeatureCollectionResult2() throws FactoryException {
        type = createSimpleType();

        final FeatureCollection featureList = FeatureStoreUtilities.collection("", type);

        geometryFactory = new GeometryFactory();

        Feature myFeature2;

        sfb = new FeatureBuilder(type);
        sfb.setPropertyValue("name", "Feature2");
        sfb.setPropertyValue("position", null);
        myFeature2 = sfb.buildFeature("id-02");
        featureList.add(myFeature2);

        return featureList;

    }
}
