/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2009-2010, Geomatys
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
package org.geotoolkit.wmts;

import org.geotoolkit.client.Request;


/**
 *
 * @author Guilhem Legal (Geomatys)
 * @module pending
 */
public interface GetCapabilitiesRequest extends Request {
    /**
     * Returns the upate sequence string.
     */
    String getUpdateSequence();

    /**
     * Sets the update sequence for the GetCapabilities response.
     */
    void setUpdateSequence(String sequence);
}
