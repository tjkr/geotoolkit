/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2013, Geomatys
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
package org.geotoolkit.data.mapinfo.mif;

import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.data.*;
import org.geotoolkit.data.mapinfo.ProjectionUtils;
import org.geotoolkit.data.memory.GenericReprojectFeatureIterator;
import org.geotoolkit.data.query.DefaultQueryCapabilities;
import org.geotoolkit.data.query.Query;
import org.geotoolkit.data.query.QueryCapabilities;
import org.geotoolkit.factory.Hints;
import org.geotoolkit.parameter.Parameters;
import org.geotoolkit.feature.Feature;
import org.geotoolkit.feature.type.FeatureType;
import org.opengis.util.GenericName;
import org.geotoolkit.feature.type.PropertyDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.identity.FeatureId;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.geotoolkit.storage.DataStores;

/**
 * A featureStore for MapInfo exchange format MIF-MID.
 *
 * @author Alexis Manin (Geomatys)
 *         Date : 21/02/13
 */
public class MIFFeatureStore extends AbstractFeatureStore {

    private final MIFManager manager;

    private final QueryCapabilities queryCapabilities = new DefaultQueryCapabilities(false);

    /**
     * Creates a new instance of MIFFeatureStore.
     *
     * @param uri The URL of the MIF file to use for this DataStore.
     *
     * @throws DataStoreException If we got a problem getting needed files.
     */
    public MIFFeatureStore(final URI uri) throws DataStoreException {
        this(uri, null);
    }

    /**
     * This sets the datastore's namespace during construction (so the schema -
     * FeatureType - will have the correct value) You can call this with
     * namespace = null, but I suggest you give it an actual namespace.
     *
     * @param uri
     * @param namespace
     */
    public MIFFeatureStore(final URI uri, final String namespace) throws DataStoreException {
        this(toParameter(uri, namespace));
    }

    public MIFFeatureStore(final ParameterValueGroup params) throws DataStoreException {
        super(params);

        final URI filePath = (URI) params.parameter(MIFFeatureStoreFactory.PATH.getName().toString()).getValue();
        try {
            manager = new MIFManager(filePath);
        } catch (Exception e) {
            throw new DataStoreException("Datastore can't reach target data.", e);
        }
    }

    private static ParameterValueGroup toParameter(final URI uri, final String namespace) {
        final ParameterValueGroup params = MIFFeatureStoreFactory.PARAMETERS_DESCRIPTOR.createValue();
        Parameters.getOrCreate(MIFFeatureStoreFactory.PATH, params).setValue(uri);
        Parameters.getOrCreate(MIFFeatureStoreFactory.NAMESPACE, params).setValue(namespace);
        return params;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FeatureStoreFactory getFactory() {
        return (FeatureStoreFactory) DataStores.getFactoryById(MIFFeatureStoreFactory.NAME);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<GenericName> getNames() throws DataStoreException {
        return manager.getTypeNames();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createFeatureType(GenericName typeName, FeatureType featureType) throws DataStoreException {
        try {
            manager.addSchema(typeName, featureType);
        } catch (URISyntaxException e) {
            throw new DataStoreException("We're unable to add a schema because we can't access source files.", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateFeatureType(GenericName typeName, FeatureType featureType) throws DataStoreException {
        throw new DataStoreException("Can not update MIF schema.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteFeatureType(GenericName typeName) throws DataStoreException {
        manager.deleteSchema(typeName);
        removeFeatures(typeName, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FeatureType getFeatureType(GenericName typeName) throws DataStoreException {
        return manager.getType(typeName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public QueryCapabilities getQueryCapabilities() {
        return queryCapabilities;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<FeatureId> addFeatures(GenericName groupName, Collection<? extends Feature> newFeatures, Hints hints) throws DataStoreException {
        final FeatureWriter writer = getFeatureWriter(groupName, null, null);

        // We remove the features as we get them. We don't need to write them as the default writing behaviour is append mode.
        while (writer.hasNext()) {
            writer.next();
            writer.remove();
        }

        List<FeatureId> addedFeatures = null;
        if(manager.getWrittenCRS() != null) {
            final FeatureCollection toWrite;
            if(newFeatures instanceof FeatureCollection) {
                toWrite = GenericReprojectFeatureIterator.wrap( (FeatureCollection) newFeatures, manager.getWrittenCRS());
            } else {
                toWrite = GenericReprojectFeatureIterator.wrap(
                        FeatureStoreUtilities.collection(newFeatures.toArray(new Feature[newFeatures.size()])),
                        manager.getWrittenCRS());
            }
            addedFeatures = FeatureStoreUtilities.write(writer, toWrite);
        } else {
            addedFeatures = FeatureStoreUtilities.write(writer, newFeatures);
        }
        return addedFeatures;
    }

    /**
     * The update operation is not supported for now, because of the behaviour of the writer, which have to manage
     * multiple feature types.
     */
    @Override
    public void updateFeatures(GenericName groupName, Filter filter, Map<? extends PropertyDescriptor, ? extends Object> values) throws DataStoreException {
        //handleUpdateWithFeatureWriter(groupName, filter, values);
        throw new UnsupportedOperationException("Update operation is not supported now");
    }

    /**
     * The remove operation is not supported for now, because of the behaviour of the writer, which have to manage
     * multiple feature types.
     */
    @Override
    public void removeFeatures(GenericName groupName, Filter filter) throws DataStoreException {
        //handleRemoveWithFeatureWriter(groupName, filter);
        throw new UnsupportedOperationException("Remove operation is not supported now");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FeatureReader getFeatureReader(Query query) throws DataStoreException {
        typeCheck(query.getTypeName());
        return handleRemaining(new MIFFeatureReader(manager, query.getTypeName()), query);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FeatureWriter getFeatureWriter(GenericName typeName, Filter filter, Hints hints) throws DataStoreException {
        typeCheck(typeName);
        final MIFFeatureReader reader = new MIFFeatureReader(manager, typeName);
        final MIFFeatureWriter writer = new MIFFeatureWriter(manager, reader);
        return  writer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void refreshMetaModel() {
        manager.refreshMetaModel();
    }


    /**
     * MIF file defines a delimiter character to separate values into the MID file. This function allows user to redefine it.
     * @param newDelimiter The new delimiter to use for MID value separation.
     */
    public void setDelimiter(char newDelimiter) {
        manager.setDelimiter(newDelimiter);
    }

        public static boolean isCompatibleCRS(CoordinateReferenceSystem source) {
            boolean isCompatible = false;
            try {
                final String mifCRS = ProjectionUtils.crsToMIFSyntax(source);
                if(mifCRS != null && ! mifCRS.isEmpty()) {
                    isCompatible = true;
                }
            } catch(Exception e) {
                // Nothing to do here, if we get an exception, we just get an incompatible CRS.
            }
            return isCompatible;
        }

}
