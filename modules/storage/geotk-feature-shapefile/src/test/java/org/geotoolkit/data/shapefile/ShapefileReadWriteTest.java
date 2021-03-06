/*
 *    GeotoolKit - An Open source Java GIS Toolkit
 *    http://geotoolkit.org
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
package org.geotoolkit.data.shapefile;

import org.junit.Test;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.Charset;

import org.geotoolkit.data.FeatureCollection;

import com.vividsolutions.jts.geom.Geometry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.geotoolkit.data.FeatureStoreUtilities;
import org.geotoolkit.data.query.QueryBuilder;
import org.geotoolkit.data.session.Session;
import org.geotoolkit.feature.Feature;
import org.geotoolkit.feature.type.FeatureType;
import org.geotoolkit.test.TestData;
import org.opengis.util.GenericName;
import org.geotoolkit.feature.type.PropertyDescriptor;

import static org.junit.Assert.*;

/**
 * 
 * @version $Id$
 * @author Ian Schneider
 * @module pending
 */
public class ShapefileReadWriteTest extends AbstractTestCaseSupport {
    final String[] files = { "shapes/statepop.shp", "shapes/polygontest.shp",
            "shapes/pointtest.shp", "shapes/holeTouchEdge.shp",
            "shapes/stream.shp", "shapes/chinese_poly.shp" };
    
    @Test
    public void testReadWriteStatePop() throws Exception {
        test("shapes/statepop.shp");
    }
    
    @Test
    public void testReadWritePolygonTest() throws Exception {
        test("shapes/polygontest.shp");
    }
    
    @Test
    public void testReadWritePointTest() throws Exception {
        test("shapes/pointtest.shp");
    }
    
    @Test
    public void testReadWriteHoleTouchEdge() throws Exception {
        test("shapes/holeTouchEdge.shp");
    }
    
    @Test
    public void testReadWriteChinese() throws Exception {
        test("shapes/chinese_poly.shp", Charset.forName("GB18030"));
    }
    
    @Test
    public void testReadDanishPoint() throws Exception {
        test("shapes/danish_point.shp");
    }
    
    

//    public void testAll() {
//        StringBuffer errors = new StringBuffer();
//        Exception bad = null;
//        for (int i = 0, ii = files.length; i < ii; i++) {
//            try {
//                
//            } catch (Exception e) {
//                System.out.println("File failed:" + files[i] + " " + e);
//                e.printStackTrace();
//                errors.append("\nFile " + files[i] + " : " + e.getMessage());
//                bad = e;
//            }
//        }
//        if (errors.length() > 0) {
//            fail(errors.toString(), bad);
//        }
//    }

    boolean readStarted = false;

    Exception exception = null;

    @Test
    public void testConcurrentReadWrite() throws Exception {
        System.gc();
        System.runFinalization(); // If some streams are still open, it may
        // help to close them.
        final File file = getTempFile();
        Runnable reader = new Runnable() {
            public void run() {
                int cutoff = 0;
                FileInputStream fr = null;
                try {
                    fr = new FileInputStream(file);
                    try {
                        fr.read();
                    } catch (IOException e1) {
                        exception = e1;
                        return;
                    }
                    // if (verbose) {
                    // System.out.println("locked");
                    // }
                    readStarted = true;
                    while (cutoff < 10) {
                        synchronized (this) {
                            try {
                                try {
                                    fr.read();
                                } catch (IOException e) {
                                    exception = e;
                                    return;
                                }
                                wait(500);
                                cutoff++;
                            } catch (InterruptedException e) {
                                cutoff = 10;
                            }
                        }
                    }
                } catch (FileNotFoundException e) {
                    assertTrue(false);
                } finally {
                    if (fr != null) {
                        try {
                            fr.close();
                        } catch (IOException e) {
                            exception = e;
                            return;
                        }
                    }
                }
            }
        };
        Thread readThread = new Thread(reader);
        readThread.start();
        while (!readStarted) {
            if (exception != null) {
                throw exception;
            }
            Thread.sleep(100);
        }
        test(files[0]);
    }

    private static void fail(final String message, final Throwable cause) throws Throwable {
        Throwable fail = new Exception(message);
        fail.initCause(cause);
        throw fail;
    }
    
    private void test(final String f) throws Exception {
        test(f, null);
    }

    private void test(final String f, final Charset charset) throws Exception {
        copyShapefiles(f); // Work on File rather than URL from JAR.
        ShapefileFeatureStore s = new ShapefileFeatureStore(
                TestData.url(AbstractTestCaseSupport.class, f).toURI(), null, false, charset);
        GenericName typeName = s.getNames().iterator().next();
        Session session = s.createSession(true);
        FeatureType type = s.getFeatureType(typeName);
        FeatureCollection one = session.getFeatureCollection(QueryBuilder.all(typeName));
        File tmp = getTempFile();

        ShapefileFeatureStoreFactory maker = new ShapefileFeatureStoreFactory();
        test(type, one, tmp, maker, true, charset);

        File tmp2 = getTempFile(); // TODO consider reuse tmp results in
        // failure
        test(type, one, tmp2, maker, false, charset);
    }

    private void test(final FeatureType type, final FeatureCollection original,
            final File tmp, final ShapefileFeatureStoreFactory maker, final boolean memorymapped, final Charset charset)
            throws IOException, MalformedURLException, Exception {

        ShapefileFeatureStore shapefile;
        GenericName typeName = type.getName();
        Map params = new HashMap();
        params.put(ShapefileFeatureStoreFactory.PATH.getName().toString(), tmp.toURI().toURL());
        params.put(ShapefileFeatureStoreFactory.MEMORY_MAPPED.getName().toString(), memorymapped);
        params.put(ShapefileFeatureStoreFactory.DBFCHARSET.getName().toString(), charset);

        shapefile = (ShapefileFeatureStore) maker.open(params);

        shapefile.createFeatureType(typeName,type);

        Session session = shapefile.createSession(true);
        session.addFeatures(typeName, original);
        session.commit();

        assertFalse(session.hasPendingChanges());
        
        FeatureCollection copy = session.getFeatureCollection(QueryBuilder.all(typeName));
        compare(original, copy);

        if (true) {
            // review open
            ShapefileFeatureStore review = new ShapefileFeatureStore(tmp.toURI(), tmp.toString(), memorymapped, charset);
            typeName = review.getNames().iterator().next();
            FeatureCollection again = review.createSession(true).getFeatureCollection(QueryBuilder.all(typeName));

            compare(copy, again);
            compare(original, again);
        }
    }

    static void compare(Collection<Feature> one, Collection<Feature> two)
            throws Exception {

        if (one.size() != two.size()) {
            throw new Exception("Number of Features unequal : " + one.size()
                    + " != " + two.size());
        }

        //copy values, order is not tested here.
        one = FeatureStoreUtilities.fill(one, new ArrayList<>());
        two = FeatureStoreUtilities.fill(two, new ArrayList<>());

        one.containsAll(two);
        two.containsAll(one);


//        Iterator<SimpleFeature> iterator1 = one.iterator();
//        Iterator<SimpleFeature> iterator2 = two.iterator();
//
//        while (iterator1.hasNext()) {
//            SimpleFeature f1 = iterator1.next();
//            SimpleFeature f2 = iterator2.next();
//            compare(f1, f2);
//        }
//
//        if(iterator1 instanceof Closeable){
//            ((Closeable)iterator1).close();
//        }
//        if(iterator2 instanceof Closeable){
//            ((Closeable)iterator2).close();
//        }
    }

    static void compare(final Feature f1, final Feature f2) throws Exception {
        Collection<PropertyDescriptor> descs = f1.getType().getDescriptors();
        if (descs.size() != f2.getType().getDescriptors().size()) {
            throw new Exception("Unequal number of attributes");
        }

        for(PropertyDescriptor desc : descs){
            final String name = desc.getName().tip().toString();
            Object att1 = f1.getPropertyValue(name);
            Object att2 = f2.getPropertyValue(name);
            if (att1 instanceof Geometry && att2 instanceof Geometry) {
                Geometry g1 = ((Geometry) att1);
                Geometry g2 = ((Geometry) att2);
                g1.normalize();
                g2.normalize();
                if (!g1.equalsExact(g2)) {
                    throw new Exception("Different geometries (" + name + "):\n"
                            + g1 + "\n" + g2);
                }
            } else {
                if (!att1.equals(att2)) {
                    throw new Exception("Different attribute (" + name + "): ["
                            + att1 + "] - [" + att2 + "]");
                }
            }
        }

    }

}
