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
package org.geotoolkit.processing.vector.extendfeature;

import org.apache.sis.parameter.ParameterBuilder;
import org.geotoolkit.data.memory.GenericExtendFeatureIterator.FeatureExtend;
import org.geotoolkit.factory.Hints;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.Process;
import org.geotoolkit.processing.vector.VectorDescriptor;

import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;

/**
 * Parameter description of ExtendFeature process.
 * name of the process : "extendfeature"
 * inputs :
 * <ul>
 *     <li>FEATURE_IN "feature_in" FeatureCollection </li>
 *     <li>EXTEND_IN "extend_in" FeatureExtend</li>
 *     <li>HINTS_IN "hint_in" Hints</li>
 * </ul>
 * outputs :
 * <ul>
 *     <li>FEATURE_OUT "feature_out" FeatureCollection</li>
 * </ul>
 * @author Quentin Boileau
 * @module pending
 */
public final class ExtendFeatureDescriptor extends VectorDescriptor {

    /**Process name : extendfeature */
    public static final String NAME = "extendfeature";

    /**
     * Mandatory - Feature Extend
     */
    public static final ParameterDescriptor<FeatureExtend> EXTEND_IN = new ParameterBuilder()
            .addName("extend_in")
            .setRemarks("Feature extension")
            .setRequired(true)
            .create(FeatureExtend.class, null);

    /**
     * Mandatory - Hints
     */
    public static final ParameterDescriptor<Hints> HINTS_IN = new ParameterBuilder()
            .addName("hint_in")
            .setRemarks("Hints")
            .setRequired(true)
            .create(Hints.class, null);

    /** Input Parameters */
    public static final ParameterDescriptorGroup INPUT_DESC =
            new ParameterBuilder().addName("InputParameters").createGroup(FEATURE_IN, EXTEND_IN);

    /** Ouput Parameters */
    public static final ParameterDescriptorGroup OUTPUT_DESC =
            new ParameterBuilder().addName("OutputParameters").createGroup(FEATURE_OUT);
    
    /** Instance */
    public static final ProcessDescriptor INSTANCE = new ExtendFeatureDescriptor();

    /**
     * Default constructor
     */
    private ExtendFeatureDescriptor() {
        super(NAME, "Adding on the fly attributes of Feature contents", INPUT_DESC, OUTPUT_DESC);
    }

    /**
     *  {@inheritDoc }
     */
    @Override
    public Process createProcess(final ParameterValueGroup input) {
        return new ExtendFeatureProcess(input);
    }
}
