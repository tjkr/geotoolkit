/*
 *    GeoAPI - Java interfaces for OGC/ISO standards
 *    http://www.geoapi.org
 *
 *    Copyright (C) 2007-2014 Open Geospatial Consortium, Inc.
 *    All Rights Reserved. http://www.opengeospatial.org/ogc/legal
 *
 *    Permission to use, copy, and modify this software and its documentation, with
 *    or without modification, for any purpose and without fee or royalty is hereby
 *    granted, provided that you include the following on ALL copies of the software
 *    and documentation or portions thereof, including modifications, that you make:
 *
 *    1. The full text of this NOTICE in a location viewable to users of the
 *       redistributed or derivative work.
 *    2. Notice of any changes or modifications to the OGC files, including the
 *       date changes were made.
 *
 *    THIS SOFTWARE AND DOCUMENTATION IS PROVIDED "AS IS," AND COPYRIGHT HOLDERS MAKE
 *    NO REPRESENTATIONS OR WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
 *    TO, WARRANTIES OF MERCHANTABILITY OR FITNESS FOR ANY PARTICULAR PURPOSE OR THAT
 *    THE USE OF THE SOFTWARE OR DOCUMENTATION WILL NOT INFRINGE ANY THIRD PARTY
 *    PATENTS, COPYRIGHTS, TRADEMARKS OR OTHER RIGHTS.
 *
 *    COPYRIGHT HOLDERS WILL NOT BE LIABLE FOR ANY DIRECT, INDIRECT, SPECIAL OR
 *    CONSEQUENTIAL DAMAGES ARISING OUT OF ANY USE OF THE SOFTWARE OR DOCUMENTATION.
 *
 *    The name and trademarks of copyright holders may NOT be used in advertising or
 *    publicity pertaining to the software without specific, written prior permission.
 *    Title to copyright in this software and any associated documentation will at all
 *    times remain with copyright holders.
 */
package org.geotoolkit.feature.type;

import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Describes an instance of a geometry attribute.
 * <p>
 * This interface adds no additional methods, the point of it is convenience
 * to type narrow {@link #getType()} to {@link GeometryType}.
 * </p>
 * @author Justin Deoliveira, The Open Planning Project
 *
 */
public interface GeometryDescriptor extends AttributeDescriptor {
    /**
     * Override of {@link AttributeDescriptor#getType()} which type narrows
     * to {@link GeometryType}.
     */
    GeometryType getType();

    /**
     * The coordinate reference system in which these geometries are defined.
     * <p>
     * This method may return <code>null</code>, but this should only occur in
     * cases where the actual crs is not known. A common case is when a shapefile
     * does not have an accompanied .prj file.
     * </p>
     */
    CoordinateReferenceSystem getCoordinateReferenceSystem();
}
