/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 * 
 *    (C) 2002-2008, Open Source Geospatial Foundation (OSGeo)
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
package org.geotoolkit.feature.simple;

import org.geotoolkit.feature.FeatureBuilder;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.geotoolkit.factory.FactoryFinder;
import org.geotoolkit.feature.AttributeDescriptorBuilder;
import org.geotoolkit.feature.AttributeTypeBuilder;
import org.geotoolkit.feature.DataTestCase;
import org.geotoolkit.util.NamesExt;
import org.geotoolkit.feature.FeatureTypeBuilder;
import org.geotoolkit.feature.FeatureUtilities;
import org.geotoolkit.feature.FeatureValidationUtilities;
import org.apache.sis.referencing.CommonCRS;
import org.geotoolkit.feature.Feature;
import org.geotoolkit.feature.Property;

import org.geotoolkit.feature.type.AttributeDescriptor;
import org.geotoolkit.feature.type.FeatureType;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.PropertyIsEqualTo;

public class SimpleFeatureBuilderTest extends DataTestCase {

    static final Set immutable;

    static {
        immutable = new HashSet();
        immutable.add(String.class);
        immutable.add(Integer.class);
        immutable.add(Double.class);
        immutable.add(Float.class);
    }

    FeatureBuilder builder;

    public SimpleFeatureBuilderTest(final String testName) throws Exception {
        super(testName);
    }

    public static void main(final String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(SimpleFeatureBuilderTest.class);
        return suite;
    }

    protected void setUp() throws Exception {
        super.setUp();
        FeatureTypeBuilder typeBuilder = new FeatureTypeBuilder();
        typeBuilder.setName("test");
        typeBuilder.add("point", Point.class, CommonCRS.WGS84.normalizedGeographic());
        typeBuilder.add("integer", Integer.class);
        typeBuilder.add("float", Float.class);

        FeatureType featureType = typeBuilder.buildFeatureType();

        builder = new FeatureBuilder(featureType);
        builder.setValidating(true);
    }

    public void testSanity() throws Exception {
        GeometryFactory gf = new GeometryFactory();
        builder.add(gf.createPoint(new Coordinate(0, 0)));
        builder.add(new Integer(1));
        builder.add(new Float(2.0));

        Feature feature = builder.buildFeature("fid");
        assertNotNull(feature);

        assertEquals(3, feature.getProperties().size());

        assertTrue(gf.createPoint(new Coordinate(0, 0)).equals((Geometry) feature.getPropertyValue("point")));
        assertEquals(new Integer(1), feature.getPropertyValue("integer"));
        assertEquals(new Float(2.0), feature.getPropertyValue("float"));
    }

    public void testTooFewAttributes() throws Exception {
        GeometryFactory gf = new GeometryFactory();
        builder.add(gf.createPoint(new Coordinate(0, 0)));
        builder.add(new Integer(1));

        Feature feature = builder.buildFeature("fid");
        assertNotNull(feature);

        assertEquals(3, feature.getProperties().size());

        assertTrue(gf.createPoint(new Coordinate(0, 0)).equals((Geometry) feature.getPropertyValue("point")));
        assertEquals(new Integer(1), feature.getPropertyValue("integer"));
        assertNull(feature.getPropertyValue("float"));
    }

    public void testSetTooFew() throws Exception {
        builder.setPropertyValue("integer", new Integer(1));
        Feature feature = builder.buildFeature("fid");
        assertNotNull(feature);

        final Property[] props = feature.getProperties().toArray(new Property[0]);
        assertEquals(3, props.length);

        assertNull(props[0].getValue());
        assertEquals(new Integer(1), props[1].getValue());
        assertNull(props[2].getValue());
    }

    public void testConverting() throws Exception {
        builder.setPropertyValue("integer", "1");
        Feature feature = builder.buildFeature("fid");

        try {
            builder.setPropertyValue("integer", "foo");
            fail("should have failed");
        } catch (Exception e) {
        }

    }

    public void testCreateFeatureWithLength() throws Exception {

        final FeatureTypeBuilder builder = new FeatureTypeBuilder();

        final AttributeTypeBuilder atb = new AttributeTypeBuilder();
        atb.setName("name");
        atb.setBinding(String.class);
        atb.setLength(5);
        final AttributeDescriptorBuilder adb = new AttributeDescriptorBuilder();
        adb.setName("name");
        adb.setType(atb.buildType());

        builder.setName("test");
        builder.add(adb.buildDescriptor());
        
        FeatureType featureType = builder.buildSimpleFeatureType();
        Feature feature = FeatureBuilder.build(featureType, new Object[]{"Val"}, "ID");

        assertNotNull(feature);

        try {
            feature = FeatureBuilder.build(featureType, new Object[]{"Longer Than 5"}, "ID");
            feature.validate();
            fail("this should fail because the value is longer than 5 characters");
        } catch (Exception e) {
            // good
        }
    }

    public void testCreateFeatureWithRestriction() throws Exception {
        FilterFactory fac = FactoryFinder.getFilterFactory(null);

        String attributeName = "string";
        PropertyIsEqualTo filter = fac.equals(fac.property("string"), fac.literal("Value"));

        FeatureTypeBuilder builder = new FeatureTypeBuilder();

        AttributeTypeBuilder atb = new AttributeTypeBuilder();
        atb.setBinding(String.class);
        atb.addRestriction(filter);
        AttributeDescriptorBuilder adb = new AttributeDescriptorBuilder();
        adb.setName(attributeName);
        adb.setType(atb.buildType());

        builder.setName("test");
        builder.add(adb.buildDescriptor());
        

        FeatureType featureType = builder.buildSimpleFeatureType();
        Feature feature = FeatureBuilder.build(featureType, new Object[]{"Value"}, "ID");

        assertNotNull(feature);

        try {
            Feature sf = FeatureBuilder.build(featureType, new Object[]{"NotValue"}, "ID");
            sf.validate();
            fail("PropertyIsEqualTo filter should have failed");
        } catch (Exception e) {
            //good
        }

    }

    public void testAbstractType() throws Exception {

        FeatureTypeBuilder tb = new FeatureTypeBuilder();
        tb.setName("http://www.nowhereinparticular.net", "AbstractThing");
        tb.setAbstract(true);

        FeatureType abstractType = tb.buildFeatureType();
        tb.setName("http://www.nowhereinparticular.net", "AbstractType2");
        tb.setSuperType(abstractType);
        tb.add(NamesExt.create("X"), String.class);
        FeatureType abstractType2 = tb.buildFeatureType();

        try {
            FeatureBuilder.build(abstractType, new Object[0], null);
            fail("abstract type allowed create");
        } catch (IllegalArgumentException iae) {
        } catch (UnsupportedOperationException uoe) {
        }

        try {
            FeatureBuilder.build(abstractType2, new Object[0], null);
            fail("abstract type allowed create");
        } catch (IllegalArgumentException iae) {
        } catch (UnsupportedOperationException uoe) {
        }

    }

    public void testCopyFeature() throws Exception {
        Feature feature = lakeFeatures[0];
        assertDuplicate("feature", feature, FeatureUtilities.copy(feature));
    }

    public void testDeepCopy() throws Exception {
        // primative
        String str = "FooBar";
        Integer i = new Integer(3);
        Float f = new Float(3.14);
        Double d = new Double(3.14159);

        AttributeTypeBuilder ab = new AttributeTypeBuilder();
        AttributeDescriptorBuilder adb = new AttributeDescriptorBuilder();
        ab.setBinding(Object.class);
        adb.setName(NamesExt.create("test"));
        adb.setType(ab.buildType());
        AttributeDescriptor testType = adb.buildDescriptor();

        assertSame("String", str, FeatureUtilities.duplicate(str));
        assertSame("Integer", i, FeatureUtilities.duplicate(i));
        assertSame("Float", f, FeatureUtilities.duplicate(f));
        assertSame("Double", d, FeatureUtilities.duplicate(d));

        // collections
        Object objs[] = new Object[]{str, i, f, d,};
        int ints[] = new int[]{1, 2, 3, 4,};
        List list = new ArrayList();
        list.add(str);
        list.add(i);
        list.add(f);
        list.add(d);
        Map map = new HashMap();
        map.put("a", str);
        map.put("b", i);
        map.put("c", f);
        map.put("d", d);
        assertDuplicate("objs", objs, FeatureUtilities.duplicate(objs));
        assertDuplicate("ints", ints, FeatureUtilities.duplicate(ints));
        assertDuplicate("list", list, FeatureUtilities.duplicate(list));
        assertDuplicate("map", map, FeatureUtilities.duplicate(map));

        // complex type
        Feature feature = lakeFeatures[0];

        Coordinate coords = new Coordinate(1, 3);
        Coordinate coords2 = new Coordinate(1, 3);
        GeometryFactory gf = new GeometryFactory();
        Geometry point = gf.createPoint(coords);
        Geometry point2 = gf.createPoint(coords2);

        assertTrue("jts identity", point != point2);
        assertTrue("jts equals1", point.equals(point2));
        assertTrue("jts equals", point.equals((Object) point2));

        assertDuplicate("jts duplicate", point, point2);
        assertDuplicate("feature", feature, FeatureUtilities.duplicate(feature));
        assertDuplicate("point", point, FeatureUtilities.duplicate(point));
    }

    protected void assertDuplicate(final String message, final Object expected, final Object value) {
        // Ensure value is equal to expected
        if (expected.getClass().isArray()) {
            int length1 = Array.getLength(expected);
            int length2 = Array.getLength(value);
            assertEquals(message, length1, length2);
            for (int i = 0; i < length1; i++) {
                assertDuplicate(
                        message + "[" + i + "]",
                        Array.get(expected, i),
                        Array.get(value, i));
            }
            //assertNotSame( message, expected, value );
        } else if (expected instanceof Geometry) {
            // JTS Geometry does not meet the Obejct equals contract!
            // So we need to do our assertEquals statement
            //
            assertTrue(message, value instanceof Geometry);
            assertTrue(message, expected instanceof Geometry);
            Geometry expectedGeom = (Geometry) expected;
            Geometry actualGeom = (Geometry) value;
            assertTrue(message, expectedGeom.equals(actualGeom));
        } else if (expected instanceof SimpleFeature) {
            assertDuplicate(message, ((SimpleFeature) expected).getAttributes(),
                    ((SimpleFeature) value).getAttributes());
        } else {
            assertEquals(message, expected, value);
        }
        // Ensure Non Immutables are actually copied
        if (!immutable.contains(expected.getClass())) {
            //assertNotSame( message, expected, value );
        }
    }


    public void testWithoutRestriction(){
        // used to prevent warning
        FilterFactory fac = FactoryFinder.getFilterFactory(null);

        String attributeName = "string";
        FeatureTypeBuilder builder = new FeatureTypeBuilder();
        builder.setName("test");
        builder.add(attributeName, String.class);
        FeatureType featureType = builder.buildFeatureType();

        Feature feature = FeatureBuilder.build(featureType, new Object[]{"Value"},
                null);

        assertNotNull( feature );
    }
    /**
     * This utility class is used by Types to prevent attribute modification.
     */
    public void testRestrictionCheck() {
        FilterFactory fac = FactoryFinder.getFilterFactory(null);

        String attributeName = "string";
        PropertyIsEqualTo filter = fac.equals(fac.property("string"), fac
                .literal("Value"));

        final FeatureTypeBuilder builder = new FeatureTypeBuilder();

        final AttributeTypeBuilder atb = new AttributeTypeBuilder();
        atb.setBinding(String.class);
        atb.addRestriction(filter);
        final AttributeDescriptorBuilder adb = new AttributeDescriptorBuilder();
        adb.setName(attributeName);
        adb.setType(atb.buildType());

        builder.setName("test");
        builder.add(adb.buildDescriptor());

        FeatureType featureType = builder.buildSimpleFeatureType();

        Feature feature = FeatureBuilder.build(featureType, new Object[]{"Value"},
                null);

        assertNotNull( feature );

    }

    public void testAssertNamedAssignable(){
        FeatureTypeBuilder builder = new FeatureTypeBuilder();

        builder.reset();
        builder.setName("Test");
        builder.add("name", String.class );
        builder.add("age", Double.class );
        FeatureType test = builder.buildFeatureType();

        builder.reset();
        builder.setName("Test");
        builder.add("age", Double.class );
        builder.add("name",String.class);
        FeatureType test2 = builder.buildFeatureType();

        builder.reset();
        builder.setName("Test");
        builder.add("name",String.class);
        FeatureType test3 = builder.buildFeatureType();

        builder.reset();
        builder.setName("Test");
        builder.add("name",String.class);
        builder.add("distance", Double.class );
        FeatureType test4 = builder.buildFeatureType();

        FeatureValidationUtilities.assertNameAssignable( test, test );
        FeatureValidationUtilities.assertNameAssignable( test, test2 );
        FeatureValidationUtilities.assertNameAssignable( test2, test );
        try {
            FeatureValidationUtilities.assertNameAssignable( test, test3 );
            fail("Expected assertNameAssignable to fail as age is not covered");
        }
        catch ( IllegalArgumentException expected ){
        }

    }

}
