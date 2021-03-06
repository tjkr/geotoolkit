/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2008-2012, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2009-2012, Geomatys
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
package org.geotoolkit.referencing.operation.projection;

import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.test.DependsOn;
import org.apache.sis.referencing.CommonCRS;
import org.junit.*;

import static java.lang.StrictMath.*;
import static org.geotoolkit.referencing.operation.provider.ObliqueMercator.PARAMETERS;


/**
 * Tests the {@link ObliqueMercator} class.
 *
 * @author Martin Desruisseaux (Geomatys)
 * @author Rémi Maréchal (Geomatys)
 *
 * @since 3.00
 */
@DependsOn(UnitaryProjectionTest.class)
public final strictfp class ObliqueMercatorTest extends ProjectionTestBase {
    /**
     * Creates a default test suite.
     */
    public ObliqueMercatorTest() {
        super(ObliqueMercator.class, null);
    }

    /**
     * Returns a new instance of {@link ObliqueMercator}.
     *
     * @param  cx The longitude of projection center.
     * @param  cy The latitude of projection center.
     * @param  azimuth The azimuth.
     * @return Newly created projection.
     */
    private static ObliqueMercator create(final double cx, final double cy, final double azimuth) {
        final ParameterValueGroup values = PARAMETERS.createValue();
        final Ellipsoid ellipsoid = CommonCRS.WGS84.ellipsoid();
        values.parameter("semi_major").setValue(ellipsoid.getSemiMajorAxis());
        values.parameter("semi_minor").setValue(ellipsoid.getSemiMinorAxis());
        values.parameter("azimuth").setValue(azimuth); // Given here because mandatory parameter.
        final Parameters parameters = Parameters.castOrWrap(values);
        parameters.parameter("longitude_of_center").setValue(cx);
        parameters.parameter("latitude_of_center").setValue(cy);
        return new ObliqueMercator(new org.geotoolkit.referencing.operation.provider.ObliqueMercator(), parameters);
    }

    /**
     * Creates a projection and derivates a few points.
     *
     * @throws TransformException Should never happen.
     *
     * @since 3.19
     */
    @Test
    @Ignore
    public void testEllipsoidalDerivative() throws TransformException {
        tolerance = 1E-9;
        transform = create(0, 0, 0);
        validate();

        final double delta = toRadians(100.0 / 60) / 1852; // Approximatively 100 metres.
        derivativeDeltas = new double[] {delta, delta};
        verifyDerivative(toRadians( 0), toRadians( 0));
        verifyDerivative(toRadians(15), toRadians(30));
        verifyDerivative(toRadians(15), toRadians(40));
        verifyDerivative(toRadians(10), toRadians(60));
    }

    /**
     * Runs the test defined in the GeoAPI-conformance module.
     *
     * @throws FactoryException   Should never happen.
     * @throws TransformException Should never happen.
     */
    @Test
    public void runGeoapiTest() throws FactoryException, TransformException {
        new GeoapiTest(mtFactory).testHotineObliqueMercator();
    }
}
