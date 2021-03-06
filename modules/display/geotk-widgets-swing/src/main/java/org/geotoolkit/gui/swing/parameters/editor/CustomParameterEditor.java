/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2013 Geomatys
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
package org.geotoolkit.gui.swing.parameters.editor;

import org.geotoolkit.gui.swing.propertyedit.featureeditor.PropertyValueEditor;
import org.opengis.parameter.ParameterValue;

/**
 * 
 * @author Quentin Boileau (Geomatys)
 */
public interface CustomParameterEditor {
    
    /**
     * Return the custom {@link PropertyValueEditor} for the specified
     * {@link ParameterValue}.
     * 
     * @param parameter {@link ParameterValue}
     * @return editor or null.
     */
    public PropertyValueEditor getCustomEditor(final ParameterValue parameter);
}
