/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2002-2008, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2009, Geomatys
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
package org.geotoolkit.filter.binaryspatial;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;

import java.util.logging.Level;

import org.geotoolkit.filter.DefaultLiteral;
import org.geotoolkit.filter.DefaultPropertyName;
import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.geometry.jts.SRIDGenerator;
import org.geotoolkit.geometry.jts.SRIDGenerator.Version;
import org.geotoolkit.referencing.CRS;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.CommonCRS;
import org.geotoolkit.util.StringUtilities;

import org.geotoolkit.feature.Feature;
import org.geotoolkit.feature.type.GeometryDescriptor;
import org.geotoolkit.feature.type.PropertyDescriptor;
import org.opengis.filter.FilterVisitor;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.spatial.BBOX;
import org.opengis.geometry.BoundingBox;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.util.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.util.logging.Logging;

/**
 * Immutable "BBOX" filter.
 *
 * @author Johann Sorel (Geomatys).
 * @module pending
 */
public class DefaultBBox extends AbstractBinarySpatialOperator<PropertyName,DefaultLiteral<BoundingBox>> implements BBOX {

    private static final LinearRing[] EMPTY_RINGS = new LinearRing[0];
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();
    private static final PreparedGeometryFactory PREPARED_FACTORY = new PreparedGeometryFactory();

    //cache the bbox geometry
    protected transient PreparedGeometry boundingGeometry;
    protected final com.vividsolutions.jts.geom.Envelope boundingEnv;
    protected final CoordinateReferenceSystem crs;
    protected final int srid;

    public DefaultBBox(final PropertyName property, final DefaultLiteral<BoundingBox> bbox) {
        super(nonNullPropertyName(property),bbox);
        boundingGeometry = toGeometry(bbox.getValue());
        boundingEnv = boundingGeometry.getGeometry().getEnvelopeInternal();
        final CoordinateReferenceSystem crsFilter = bbox.getValue().getCoordinateReferenceSystem();
        if(crsFilter != null){
            this.crs = crsFilter;
            this.srid = SRIDGenerator.toSRID(crs, Version.V1);
        }else{
            // In CQL if crs is not specified, it is EPSG:4326
            this.crs = CommonCRS.WGS84.normalizedGeographic();
            this.srid = 4326;
        }
    }

    private PreparedGeometry getPreparedGeometry(){
        if(boundingGeometry == null){
            boundingGeometry = toGeometry(right.getValue());
        }
        return boundingGeometry;
    }

    private static PropertyName nonNullPropertyName(final PropertyName proper) {
        if (proper == null)
            return new DefaultPropertyName("");
        return proper;
    }


    protected CoordinateReferenceSystem findCRS(final Object base, final Geometry candidate){
        //we don't know in which crs it is, try to find it
        CoordinateReferenceSystem crs = null;
        try{
            crs = JTS.findCoordinateReferenceSystem(candidate);
        }catch(IllegalArgumentException ex){
            LOGGER.log(Level.WARNING, null, ex);
        }catch(NoSuchAuthorityCodeException ex){
            LOGGER.log(Level.WARNING, null, ex);
        }catch(FactoryException ex){
            LOGGER.log(Level.WARNING, null, ex);
        }

        if(crs == null && base instanceof Feature){
            //try to find it on the base
            final Feature att = (Feature) base;
            final String propertyName = left.getPropertyName();
            if (propertyName.isEmpty()) {
                crs = att.getType().getCoordinateReferenceSystem();
            } else {
                final PropertyDescriptor desc = att.getType().getDescriptor(propertyName);
                if(desc instanceof GeometryDescriptor){
                    crs = ((GeometryDescriptor)desc).getCoordinateReferenceSystem();
                }
            }
        }

        return crs;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public String getPropertyName() {
        return left.getPropertyName();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public String getSRS() {
        return IdentifiedObjects.getIdentifierOrName(right.getValue().getCoordinateReferenceSystem());
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public double getMinX() {
        return right.getValue().getMinimum(0);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public double getMinY() {
        return right.getValue().getMinimum(1);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public double getMaxX() {
        return right.getValue().getMaximum(0);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public double getMaxY() {
        return right.getValue().getMaximum(1);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean evaluate(final Object object) {
        Geometry candidate = toGeometry(object, left);

        if(candidate == null){
            return false;
        }


        //we don't know in which crs it is, try to find it
        final CoordinateReferenceSystem candidateCrs = findCRS(object, candidate);

        //if we don't know the crs, we will assume it's the objective crs already
        if(candidateCrs != null){
            //reproject in objective crs if needed
            if(!CRS.equalsIgnoreMetadata(this.crs,candidateCrs)){
                try {
                    candidate = JTS.transform(candidate, CRS.findMathTransform(candidateCrs, this.crs));
                } catch (MismatchedDimensionException | TransformException | FactoryException ex) {
                    Logging.getLogger("org.geotoolkit.filter.binaryspatial").log(Level.WARNING, null, ex);
                    return false;
                }
            }
        }


        final com.vividsolutions.jts.geom.Envelope candidateEnv = candidate.getEnvelopeInternal();

        if(boundingEnv.contains(candidateEnv) || candidateEnv.contains(boundingEnv)) {
            return true;
        } else if(boundingEnv.intersects(candidateEnv)) {
            return getPreparedGeometry().intersects(candidate);
        } else {
            return false;
        }

    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Object accept(final FilterVisitor visitor, final Object extraData) {
        return visitor.visit(this, extraData);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("BBOX \n");
        sb.append(StringUtilities.toStringTree(left,right));
        return sb.toString();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AbstractBinarySpatialOperator other = (AbstractBinarySpatialOperator) obj;
        if (this.left != other.left && !this.left.equals(other.left)) {
            return false;
        }
        if (this.right != other.right && !this.right.equals(other.right)) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public int hashCode() {
        int hash = 15;
        hash = 71 * hash + this.left.hashCode();
        hash = 71 * hash + this.right.hashCode();
        return hash;
    }

    /**
     * Utility method to transform an envelope in geometry.
     * @param env
     * @return Geometry
     */
    private static PreparedGeometry toGeometry(final Envelope env){

        double minX = env.getMinimum(0);
        double minY = env.getMinimum(1);
        double maxX = env.getMaximum(0);
        double maxY = env.getMaximum(1);
        if(Double.isNaN(minX) || Double.isInfinite(minX)) minX = Double.MIN_VALUE;
        if(Double.isNaN(minY) || Double.isInfinite(minY)) minY = Double.MIN_VALUE;
        if(Double.isNaN(maxX) || Double.isInfinite(maxX)) maxX = Double.MAX_VALUE;
        if(Double.isNaN(maxY) || Double.isInfinite(maxY)) maxY = Double.MAX_VALUE;

        final Coordinate[] coords = new Coordinate[5];
        coords[0] = new Coordinate(minX, minY);
        coords[1] = new Coordinate(minX, maxY);
        coords[2] = new Coordinate(maxX, maxY);
        coords[3] = new Coordinate(maxX, minY);
        coords[4] = new Coordinate(minX, minY);
        final LinearRing ring = GEOMETRY_FACTORY.createLinearRing(coords);
        Geometry geom = GEOMETRY_FACTORY.createPolygon(ring, EMPTY_RINGS);
        return PREPARED_FACTORY.create(geom);
    }
}
