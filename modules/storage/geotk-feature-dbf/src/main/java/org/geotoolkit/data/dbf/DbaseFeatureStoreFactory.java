/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2010, Johann Sorel
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

package org.geotoolkit.data.dbf;

import java.util.Collections;
import org.geotoolkit.data.AbstractFileFeatureStoreFactory;
import org.apache.sis.metadata.iso.DefaultIdentifier;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.identification.DefaultServiceIdentification;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.storage.DataType;
import org.geotoolkit.storage.DefaultFactoryMetadata;
import org.geotoolkit.storage.FactoryMetadata;

import org.opengis.metadata.Identifier;
import org.opengis.metadata.identification.Identification;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;

/**
 * DBF featurestore factory. handle only reading actually.
 * Todo : handle feature writer.
 *
 * @author Johann Sorel
 * @module pending
 */
public class DbaseFeatureStoreFactory extends AbstractFileFeatureStoreFactory {

    /** factory identification **/
    public static final String NAME = "dbf";
    public static final DefaultServiceIdentification IDENTIFICATION;
    static {
        IDENTIFICATION = new DefaultServiceIdentification();
        final Identifier id = new DefaultIdentifier(NAME);
        final DefaultCitation citation = new DefaultCitation(NAME);
        citation.setIdentifiers(Collections.singleton(id));
        IDENTIFICATION.setCitation(citation);
    }

    public static final ParameterDescriptor<String> IDENTIFIER = createFixedIdentifier(NAME);

    public static final ParameterDescriptorGroup PARAMETERS_DESCRIPTOR =
            new ParameterBuilder().addName("DBFParameters").createGroup(
                IDENTIFIER, PATH,NAMESPACE);

    /**
     * {@inheritDoc}
     */
    @Override
    public Identification getIdentification() {
        return IDENTIFICATION;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CharSequence getDescription() {
        return Bundle.formatInternational(Bundle.Keys.databaseDescription);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CharSequence getDisplayName() {
        return Bundle.formatInternational(Bundle.Keys.databaseTitle);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParameterDescriptorGroup getParametersDescriptor() {
        return PARAMETERS_DESCRIPTOR;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DbaseFileFeatureStore open(final ParameterValueGroup params) throws DataStoreException {
        ensureCanProcess(params);
        return new DbaseFileFeatureStore(params);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DbaseFileFeatureStore create(final ParameterValueGroup params) throws DataStoreException {
        return open(params);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getFileExtensions() {
        return new String[] {".dbf"};
    }

    @Override
    public FactoryMetadata getMetadata() {
        return new DefaultFactoryMetadata(DataType.VECTOR, true, true, true, false, DefaultFactoryMetadata.GEOMS_NONE);
    }
    
}
