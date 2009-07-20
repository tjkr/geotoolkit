/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 * 
 *    (C) 2003-2008, Open Source Geospatial Foundation (OSGeo)
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
package org.geotoolkit.data;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Set;

import org.geotoolkit.data.concurrent.FeatureLock;
import org.geotoolkit.data.concurrent.FeatureLocking;
import org.geotoolkit.data.concurrent.LockingManager;
import org.geotoolkit.data.query.Query;
import org.geotoolkit.feature.collection.FeatureIterator;

import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;

/**
 * A Starting point for your own FeatureLocking implementations.
 * 
 * <p>
 * This class extends AbstractFeatureSource and depends on getDataStore().
 * </p>
 * The implementation of the following functions depends on
 * getDataStore().getLockingManger() not being <code>null</code>:
 * 
 * <ul>
 * <li>
 * lockFeatures( Query )
 * </li>
 * <li>
 * unLockFeatures( Query )
 * </li>
 * <li>
 * releaseLock( AuthorizationID )
 * </li>
 * <li>
 * refreshLock( AuthorizationID )
 * </li>
 * </ul>
 * 
 * <p>
 * FeatureStores that have provided their own locking to will need to override
 * the above methods, or provide a custom LockingManger.
 * </p>
 *
 * @author Jody Garnett, Refractions Research
 * @source $URL$
 */
public abstract class AbstractFeatureLocking extends AbstractFeatureStore
        implements FeatureLocking<SimpleFeatureType, SimpleFeature> {

    private FeatureLock featureLock = FeatureLock.TRANSACTION;

    public AbstractFeatureLocking() {
        // just to keep the default constructor around
    }

    /**
     * This constructors allows to set the supported hints 
     * @param hints
     */
    public AbstractFeatureLocking(final Set hints) {
        super(hints);
    }

    /**
     * Provide a FeatureLock for locking opperations to opperate against.
     * 
     * <p>
     * Initial Transactional duration locks can be restored with
     * setFeatureLock( FetaureLock.TRANSACTION )
     * </p>
     *
     * @param lock FeatureLock (or FeatureLock.TRANSACTION );
     *
     * @throws NullPointerException If lock was <code>null</code>
     *
     * @see org.geotoolkit.data.FeatureLocking#setFeatureLock(org.geotoolkit.data.FeatureLock)
     */
    @Override
    public void setFeatureLock(final FeatureLock lock) {
        if (lock == null) {
            throw new NullPointerException(
                    "A FeatureLock is required - did you mean FeatureLock.TRANSACTION?");
        }

        featureLock = lock;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public int lockFeatures() throws IOException {
        return lockFeatures(Filter.INCLUDE);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public int lockFeatures(final Filter filter) throws IOException {
        return lockFeatures(new DefaultQuery(getSchema().getTypeName(), filter));
    }

    /**
     * Lock features matching Query.
     * 
     * <p>
     * FeatureStores that have provided their own locking to will need to
     * override this method.
     * </p>
     *
     * @param query
     *
     * @return Number of locked Features
     *
     * @throws IOException If we could not determine which feature to lock
     *         based on Query
     * @throws UnsupportedOperationException When DataStore does not provide a
     *         LockingManager
     * @throws DataSourceException If feature to be locked does not exist
     *
     * @see org.geotoolkit.data.FeatureLocking#lockFeatures(org.geotoolkit.data.Query)
     */
    @Override
    public int lockFeatures(final Query query) throws IOException {
        LockingManager lockingManager = getDataStore().getLockingManager();

        if (lockingManager == null) {
            throw new UnsupportedOperationException(
                    "DataStore not using lockingManager, must provide alternate implementation");
        }

        // Could we reduce the Query to only return the FetureID here?
        //
        final FeatureIterator<SimpleFeature> reader = getFeatures(query).features();
        final String typeName = query.getTypeName();

        int count = 0;

        try {
            while (reader.hasNext()) {
                try {
                    final SimpleFeature feature = reader.next();
                    lockingManager.lockFeatureID(typeName, feature.getID(),
                            getTransaction(), featureLock);
                    count++;
                } catch (FeatureLockException locked) {
                    // could not aquire - don't increment count                
                } catch (NoSuchElementException nosuch) {
                    throw new DataSourceException("Problem with " + query.getHandle() + " while locking", nosuch);
                }
            }
        } finally {
            reader.close();
        }

        return count;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void unLockFeatures() throws IOException {
        unLockFeatures(Filter.INCLUDE);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void unLockFeatures(final Filter filter) throws IOException {
        unLockFeatures(new DefaultQuery(getSchema().getTypeName(), filter));
    }

    /**
     * Unlock features specified by the <code>query</code>.
     * 
     * <p>
     * FeatureStores that have provided their own locking to will need to
     * override this method.
     * </p>
     *
     * @param query
     *
     * @throws IOException
     * @throws UnsupportedOperationException If lockingManager is not provided
     *         by DataStore subclass
     * @throws DataSourceException Filter describes an unlocked Feature, or
     *         authorization not held
     *
     * @see org.geotoolkit.data.FeatureLocking#unLockFeatures(org.geotoolkit.data.Query)
     */
    @Override
    public void unLockFeatures(final Query query) throws IOException {
        final LockingManager lockingManager = getDataStore().getLockingManager();

        if (lockingManager == null) {
            throw new UnsupportedOperationException(
                    "DataStore not using lockingManager, must provide alternate implementation");
        }

        // Could we reduce the Query to only return the FetureID here?
        //
        final FeatureIterator<SimpleFeature> reader = getFeatures(query).features();
        final String typeName = query.getTypeName();

        try {
            while (reader.hasNext()) {
                try {
                    final SimpleFeature feature = reader.next();
                    lockingManager.unLockFeatureID(typeName, feature.getID(),
                            getTransaction(), featureLock);
                } catch (NoSuchElementException nosuch) {
                    throw new DataSourceException("Problem with " + query.getHandle() + " while locking", nosuch);
                }
            }
        } finally {
            reader.close();
        }
    }
}
