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
package org.geotoolkit.processing.vector.convexhull;

import org.geotoolkit.process.ProcessException;
import org.geotoolkit.processing.vector.AbstractProcessTest;
import org.opengis.util.NoSuchIdentifierException;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPoint;

import org.geotoolkit.data.FeatureStoreUtilities;
import org.geotoolkit.data.FeatureCollection;
import org.geotoolkit.feature.FeatureTypeBuilder;
import org.geotoolkit.feature.FeatureBuilder;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessFinder;
import org.geotoolkit.referencing.CRS;

import org.geotoolkit.feature.Feature;
import org.geotoolkit.feature.type.FeatureType;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.util.FactoryException;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * JUnit test of ConvexHull process
 *
 * @author Quentin Boileau @module pending
 */
public class ConvexHullTest extends AbstractProcessTest {

    private static FeatureBuilder sfb;
    private static final GeometryFactory geometryFactory = new GeometryFactory();
    private static FeatureType type;

    public ConvexHullTest() {
        super("convexhull");
    }

    @Test
    public void testConvexHull() throws ProcessException, NoSuchIdentifierException, FactoryException {

        // Inputs
        final FeatureCollection featureList = buildFeatureList();
        // Process
        ProcessDescriptor desc = ProcessFinder.getProcessDescriptor("vector", "convexhull");

        ParameterValueGroup in = desc.getInputDescriptor().createValue();
        in.parameter("feature_in").setValue(featureList);
        org.geotoolkit.process.Process proc = desc.createProcess(in);

        //Geometry out
        final Geometry resultGeom = (Geometry) proc.call().parameter("geometry_out").getValue();

        //Expected Features out
        final Geometry geomteryResult1 = buildGeometryResult1();
        assertTrue(geomteryResult1.equals(resultGeom));
        //assertEquals(geomteryResult1, resultGeom);

        //select the second feature geometry
        ProcessDescriptor desc2 = ProcessFinder.getProcessDescriptor("vector", "convexhull");

        ParameterValueGroup in2 = desc2.getInputDescriptor().createValue();
        in2.parameter("feature_in").setValue(featureList);
        in2.parameter("geometry_name").setValue("geom2");
        org.geotoolkit.process.Process proc2 = desc.createProcess(in2);

        //Geometry out
        final Geometry resultGeom2 = (Geometry) proc2.call().parameter("geometry_out").getValue();

        //Expected Features out
        final Geometry geomteryResult2 = buildGeometryResult2();
        assertTrue(geomteryResult2.equals(resultGeom2));
        //assertEquals(geomteryResult2, resultGeom2);

    }

    private static FeatureType createSimpleType() throws NoSuchAuthorityCodeException, FactoryException {
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.setName("IntersectTest");
        ftb.add("name", String.class);
        ftb.add("geom1", Geometry.class, CRS.decode("EPSG:3395"));
        ftb.add("geom2", Geometry.class, CRS.decode("EPSG:3395"));

        ftb.setDefaultGeometry("geom1");
        final FeatureType sft = ftb.buildFeatureType();
        return sft;
    }

    private static FeatureCollection buildFeatureList() throws FactoryException {

        type = createSimpleType();

        final FeatureCollection featureList = FeatureStoreUtilities.collection("", type);


        Feature myFeature1;
        LinearRing ring = geometryFactory.createLinearRing(
                new Coordinate[]{
                    new Coordinate(3.0, 3.0),
                    new Coordinate(3.0, 4.0),
                    new Coordinate(4.0, 4.0),
                    new Coordinate(4.0, 3.0),
                    new Coordinate(3.0, 3.0)
                });
        sfb = new FeatureBuilder(type);
        sfb.setPropertyValue("name", "feature1");
        sfb.setPropertyValue("geom1", geometryFactory.createPolygon(ring, null));
        sfb.setPropertyValue("geom2", geometryFactory.createPoint(new Coordinate(3.5, 3.5)));
        myFeature1 = sfb.buildFeature("id-01");
        featureList.add(myFeature1);

        Feature myFeature2;
        ring = geometryFactory.createLinearRing(
                new Coordinate[]{
                    new Coordinate(5.0, 6.0),
                    new Coordinate(5.0, 7.0),
                    new Coordinate(6.0, 7.0),
                    new Coordinate(6.0, 6.0),
                    new Coordinate(5.0, 6.0)
                });
        MultiPoint multPt = geometryFactory.createMultiPoint(
                new Coordinate[]{
                    new Coordinate(5.0, 4.0),
                    new Coordinate(3.0, 6.0),
                    new Coordinate(4.0, 7.0),
                    new Coordinate(5.5, 6.5)
                });
        sfb = new FeatureBuilder(type);
        sfb.setPropertyValue("name", "feature2");
        sfb.setPropertyValue("geom1", geometryFactory.createPolygon(ring, null));
        sfb.setPropertyValue("geom2", multPt);
        myFeature2 = sfb.buildFeature("id-02");
        featureList.add(myFeature2);

        Feature myFeature3;
        ring = geometryFactory.createLinearRing(
                new Coordinate[]{
                    new Coordinate(9.0, 4.0),
                    new Coordinate(9.0, 5.0),
                    new Coordinate(11.0, 5.0),
                    new Coordinate(11.0, 4.0),
                    new Coordinate(9.0, 4.0)
                });
        LineString line = geometryFactory.createLineString(
                new Coordinate[]{
                    new Coordinate(7.0, 0.0),
                    new Coordinate(9.0, 3.0)
                });
        sfb = new FeatureBuilder(type);
        sfb.setPropertyValue("name", "feature3");
        sfb.setPropertyValue("geom1", geometryFactory.createPolygon(ring, null));
        sfb.setPropertyValue("geom2", line);
        myFeature3 = sfb.buildFeature("id-03");
        featureList.add(myFeature3);

        return featureList;
    }

    private Geometry buildGeometryResult1() {
        LinearRing ring = geometryFactory.createLinearRing(
                new Coordinate[]{
                    new Coordinate(3.0, 3.0),
                    new Coordinate(3.0, 4.0),
                    new Coordinate(5.0, 7.0),
                    new Coordinate(6.0, 7.0),
                    new Coordinate(11.0, 5.0),
                    new Coordinate(11.0, 4.0),
                    new Coordinate(4.0, 3.0),
                    new Coordinate(3.0, 3.0)
                });

        return geometryFactory.createPolygon(ring, null);
    }

    private Geometry buildGeometryResult2() {
        LinearRing ring = geometryFactory.createLinearRing(
                new Coordinate[]{
                    new Coordinate(7.0, 0.0),
                    new Coordinate(3.5, 3.5),
                    new Coordinate(3.0, 6.0),
                    new Coordinate(4.0, 7.0),
                    new Coordinate(5.5, 6.5),
                    new Coordinate(9.0, 3.0),
                    new Coordinate(7.0, 0.0)
                });

        return geometryFactory.createPolygon(ring, null);
    }
}
