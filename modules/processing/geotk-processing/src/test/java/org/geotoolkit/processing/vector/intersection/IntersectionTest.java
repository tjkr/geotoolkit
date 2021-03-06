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
package org.geotoolkit.processing.vector.intersection;

import org.geotoolkit.process.ProcessException;
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
import org.geotoolkit.processing.vector.AbstractProcessTest;
import org.geotoolkit.referencing.CRS;

import org.geotoolkit.feature.Feature;
import org.geotoolkit.feature.type.FeatureType;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.util.FactoryException;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * JUnit test of intersect process
 *
 * @author Quentin Boileau @module pending
 */
public class IntersectionTest extends AbstractProcessTest {

    private static FeatureBuilder sfb;
    private static final GeometryFactory geometryFactory = new GeometryFactory();
    private static FeatureType type;

    public IntersectionTest() {
        super("intersection");
    }

    @Test
    public void testIntersection() throws ProcessException, NoSuchIdentifierException, FactoryException {

        // Inputs
        final FeatureCollection featureList = buildFeatureList();

        final FeatureCollection featureInterList = buildFeatureInterList();
        // Process
        ProcessDescriptor desc = ProcessFinder.getProcessDescriptor("vector", "intersection");

        ParameterValueGroup in = desc.getInputDescriptor().createValue();
        in.parameter("feature_in").setValue(featureList);
        in.parameter("feature_inter").setValue(featureInterList);
        //in.parameter("geometry_name").setValue("geom1");
        org.geotoolkit.process.Process proc = desc.createProcess(in);

        //Features out
        final FeatureCollection featureListOut = (FeatureCollection) proc.call().parameter("feature_out").getValue();


        //Expected Features out
        final FeatureCollection featureListResult = buildResultList();
        assertEquals(featureListOut.getFeatureType(), featureListResult.getFeatureType());
        assertEquals(featureListOut.getID(), featureListResult.getID());
        assertEquals(featureListOut.size(), featureListResult.size());
        assertTrue(featureListOut.containsAll(featureListResult));
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

    private static FeatureType createSimpleResultType() throws NoSuchAuthorityCodeException, FactoryException {
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.setName("IntersectTest");
        ftb.add("name", String.class);
        ftb.add("geom1", Geometry.class, CRS.decode("EPSG:3395"));

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

    private static FeatureCollection buildFeatureInterList() throws FactoryException {

        type = createSimpleType();

        final FeatureCollection featureList = FeatureStoreUtilities.collection("", type);


        Feature myFeature1;
        LinearRing ring = geometryFactory.createLinearRing(
                new Coordinate[]{
                    new Coordinate(1.0, 4.0),
                    new Coordinate(1.0, 5.0),
                    new Coordinate(2.0, 5.0),
                    new Coordinate(2.0, 4.0),
                    new Coordinate(1.0, 4.0)
                });
        MultiPoint multPt = geometryFactory.createMultiPoint(
                new Coordinate[]{
                    new Coordinate(1.0, 6.0), //nothing
                    new Coordinate(3.0, 6.0), //intersection with a point
                    new Coordinate(3.5, 3.5) //intersection with a polygon
                });
        sfb = new FeatureBuilder(type);
        sfb.setPropertyValue("name", "feature11");
        sfb.setPropertyValue("geom1", geometryFactory.createPolygon(ring, null));
        sfb.setPropertyValue("geom2", multPt);
        myFeature1 = sfb.buildFeature("id-11");
        featureList.add(myFeature1);

        Feature myFeature2;
        ring = geometryFactory.createLinearRing(
                new Coordinate[]{
                    new Coordinate(4.0, 2.0),
                    new Coordinate(4.0, 5.0),
                    new Coordinate(7.0, 5.0),
                    new Coordinate(7.0, 2.0),
                    new Coordinate(4.0, 2.0)
                });
        LineString line = geometryFactory.createLineString(
                new Coordinate[]{
                    new Coordinate(8.0, 4.5),
                    new Coordinate(11.0, 4.5)
                });
        sfb = new FeatureBuilder(type);
        sfb.setPropertyValue("name", "feature12");
        sfb.setPropertyValue("geom1", geometryFactory.createPolygon(ring, null));
        sfb.setPropertyValue("geom2", line);
        myFeature2 = sfb.buildFeature("id-12");
        featureList.add(myFeature2);

        Feature myFeature3;
        ring = geometryFactory.createLinearRing(
                new Coordinate[]{
                    new Coordinate(0.0, 0.0),
                    new Coordinate(0.0, 8.0),
                    new Coordinate(10.0, 8.0),
                    new Coordinate(10.0, 0.0),
                    new Coordinate(0.0, 0.0)
                });
        sfb = new FeatureBuilder(type);
        sfb.setPropertyValue("name", "feature13");
        sfb.setPropertyValue("geom1", geometryFactory.createPolygon(ring, null));
        sfb.setPropertyValue("geom2", null);
        myFeature3 = sfb.buildFeature("id-13");
        featureList.add(myFeature3);

        return featureList;
    }

    private static FeatureCollection buildResultList() throws FactoryException {


        type = createSimpleResultType();

        final FeatureCollection featureList = FeatureStoreUtilities.collection("", type);


        Feature myFeature1;
        LineString line = geometryFactory.createLineString(
                new Coordinate[]{
                    new Coordinate(4.0, 4.0),
                    new Coordinate(4.0, 3.0)
                });
        sfb = new FeatureBuilder(type);
        sfb.setPropertyValue("name", "feature1");
        sfb.setPropertyValue("geom1", line);
        myFeature1 = sfb.buildFeature("id-01<->id-12");
        featureList.add(myFeature1);

        Feature myFeature2;
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
        myFeature2 = sfb.buildFeature("id-01<->id-13");
        featureList.add(myFeature2);

        Feature myFeature3;
        sfb = new FeatureBuilder(type);
        sfb.setPropertyValue("name", "feature1");
        sfb.setPropertyValue("geom1", geometryFactory.createPoint(new Coordinate(3.5, 3.5)));
        myFeature3 = sfb.buildFeature("id-01<->id-11");
        featureList.add(myFeature3);


        Feature myFeature4;
        ring = geometryFactory.createLinearRing(
                new Coordinate[]{
                    new Coordinate(9.0, 4.0),
                    new Coordinate(9.0, 5.0),
                    new Coordinate(10.0, 5.0),
                    new Coordinate(10.0, 4.0),
                    new Coordinate(9.0, 4.0)
                });
        sfb = new FeatureBuilder(type);
        sfb.setPropertyValue("name", "feature3");
        sfb.setPropertyValue("geom1", geometryFactory.createPolygon(ring, null));
        myFeature4 = sfb.buildFeature("id-03<->id-13");
        featureList.add(myFeature4);

        Feature myFeature5;
        line = geometryFactory.createLineString(
                new Coordinate[]{
                    new Coordinate(9.0, 4.5),
                    new Coordinate(11.0, 4.5)
                });
        sfb = new FeatureBuilder(type);
        sfb.setPropertyValue("name", "feature3");
        sfb.setPropertyValue("geom1", line);
        myFeature5 = sfb.buildFeature("id-03<->id-12");
        featureList.add(myFeature5);

        Feature myFeature6;
        ring = geometryFactory.createLinearRing(
                new Coordinate[]{
                    new Coordinate(5.0, 6.0),
                    new Coordinate(5.0, 7.0),
                    new Coordinate(6.0, 7.0),
                    new Coordinate(6.0, 6.0),
                    new Coordinate(5.0, 6.0)
                });
        sfb = new FeatureBuilder(type);
        sfb.setPropertyValue("name", "feature2");
        sfb.setPropertyValue("geom1", geometryFactory.createPolygon(ring, null));
        myFeature5 = sfb.buildFeature("id-02<->id-13");
        featureList.add(myFeature5);


        return featureList;
    }
}
