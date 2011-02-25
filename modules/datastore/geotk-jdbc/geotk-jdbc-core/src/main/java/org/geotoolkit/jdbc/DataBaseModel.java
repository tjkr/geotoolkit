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

package org.geotoolkit.jdbc;

import java.util.HashSet;
import java.util.Collections;
import com.vividsolutions.jts.geom.Geometry;

import java.util.logging.Level;
import java.sql.Statement;
import java.util.ArrayList;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.geotoolkit.feature.AttributeTypeBuilder;
import org.geotoolkit.feature.AttributeDescriptorBuilder;
import org.geotoolkit.feature.DefaultName;
import org.geotoolkit.feature.FeatureTypeBuilder;
import org.geotoolkit.storage.DataStoreException;
import org.geotoolkit.factory.HintsPending;
import org.geotoolkit.jdbc.dialect.SQLDialect;
import org.geotoolkit.jdbc.fid.PrimaryKeyColumn;
import org.geotoolkit.jdbc.fid.PrimaryKey;
import org.geotoolkit.jdbc.fid.AutoGeneratedPrimaryKeyColumn;
import org.geotoolkit.jdbc.fid.NullPrimaryKey;
import org.geotoolkit.jdbc.fid.NonIncrementingPrimaryKeyColumn;
import org.geotoolkit.jdbc.fid.SequencedPrimaryKeyColumn;

import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import static org.geotoolkit.jdbc.MetaDataConstants.*;
import static org.geotoolkit.jdbc.AbstractJDBCDataStore.*;

/**
 * Represent the structure of the database. The work done here is similar to
 * reverse engineering.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class DataBaseModel {

    private final DefaultJDBCDataStore store;

    private final Map<Name,PrimaryKey> primaryKeys = new HashMap<Name, PrimaryKey>();
    private final Map<Name,FeatureType> names = new HashMap<Name, FeatureType>();
    private Set<Name> nameCache = null;

    public DataBaseModel(final DefaultJDBCDataStore store){
        this.store = store;
    }

    /**
     * Clear the model cache. A new database analyze will be made the next time
     * it is needed.
     */
    public void clearCache(){
        primaryKeys.clear();
        names.clear();
        nameCache = null;
    }

    public PrimaryKey getPrimaryKey(final Name featureTypeName) throws DataStoreException{
        if(nameCache == null){
            visitTables();
        }
        return primaryKeys.get(featureTypeName);
    }

    public synchronized Set<Name> getNames() throws DataStoreException {
        Set<Name> ref = nameCache;
        if(ref == null){
            visitTables();
            ref = Collections.unmodifiableSet(new HashSet<Name>(names.keySet()));
            nameCache = ref;
        }
        return ref;
    }

    public FeatureType getFeatureType(final Name typeName) throws DataStoreException {
        return names.get(typeName);
    }


    /**
     * Explore the available tables and generate schemas and primary keys.
     * @throws DataStoreException
     */
    private synchronized void visitTables() throws DataStoreException{
        final SQLDialect dialect = store.getDialect();

        //clear previous schemas, this might be called after an update schema
        nameCache = null;
        names.clear();
        primaryKeys.clear();

        /*
         *        <LI><B>TABLE_CAT</B> String => table catalog (may be <code>null</code>)
         *        <LI><B>TABLE_SCHEM</B> String => table schema (may be <code>null</code>)
         *        <LI><B>TABLE_NAME</B> String => table name
         *        <LI><B>TABLE_TYPE</B> String => table type.  Typical types are "TABLE",
         *                        "VIEW",        "SYSTEM TABLE", "GLOBAL TEMPORARY",
         *                        "LOCAL TEMPORARY", "ALIAS", "SYNONYM".
         *        <LI><B>REMARKS</B> String => explanatory comment on the table
         *  <LI><B>TYPE_CAT</B> String => the types catalog (may be <code>null</code>)
         *  <LI><B>TYPE_SCHEM</B> String => the types schema (may be <code>null</code>)
         *  <LI><B>TYPE_NAME</B> String => type name (may be <code>null</code>)
         *  <LI><B>SELF_REFERENCING_COL_NAME</B> String => name of the designated
         *                  "identifier" column of a typed table (may be <code>null</code>)
         *        <LI><B>REF_GENERATION</B> String => specifies how values in
         *                  SELF_REFERENCING_COL_NAME are created. Values are
         *                  "SYSTEM", "USER", "DERIVED". (may be <code>null</code>)
         */
        Connection cx = null;
        try {
            cx = store.createConnection();
            final DatabaseMetaData metaData = cx.getMetaData();
            final ResultSet tables = metaData.getTables(null, store.getDatabaseSchema(), "%", new String[]{Table.VALUE_TYPE_TABLE, Table.VALUE_TYPE_VIEW});

            try {
                while (tables.next()) {

                    final String schemaName = tables.getString(Table.TABLE_SCHEM);
                    final String tableName = tables.getString(Table.TABLE_NAME);

                    //use the dialect to filter
                    if (!dialect.includeTable(schemaName, tableName, cx)) {
                        continue;
                    }

                    final Name name = new DefaultName(store.getNamespaceURI(), tableName);
                    final SimpleFeatureType sft = createFeatureType(name);
                    final PrimaryKey pkey = createPrimaryKey(name);
                    names.put(name, sft);
                    primaryKeys.put(name, pkey);
                }
            } finally {
                store.closeSafe(tables);
            }
        } catch (SQLException e) {
            throw new DataStoreException("Error occurred getting table name list.", e);
        } finally {
            store.closeSafe(cx);
        }

    }

    /**
     * Builds the feature type from database metadata.
     */
    private SimpleFeatureType createFeatureType(final Name typeName) throws DataStoreException {
        final SQLDialect dialect = store.getDialect();
        final FeatureTypeBuilder tb = new FeatureTypeBuilder();
        final AttributeDescriptorBuilder adb = new AttributeDescriptorBuilder();
        final AttributeTypeBuilder atb = new AttributeTypeBuilder();

        //set up the name
        final String tableName = typeName.getLocalPart();
        final String namespace = typeName.getNamespaceURI();
        tb.setName(typeName);

        //ensure we have a connection
        Connection cx = null;
        ResultSet columns = null;
        try {
            /* Get metadata about columns from database.
             *
             *        <LI><B>COLUMN_NAME</B> String => column name
             *        <LI><B>DATA_TYPE</B> int => SQL type from java.sql.Types
             *        <LI><B>TYPE_NAME</B> String => Data source dependent type name,
             *  for a UDT the type name is fully qualified
             *        <LI><B>COLUMN_SIZE</B> int => column size.  For char or date
             *            types this is the maximum number of characters, for numeric or
             *            decimal types this is precision.
             *        <LI><B>BUFFER_LENGTH</B> is not used.
             *        <LI><B>DECIMAL_DIGITS</B> int => the number of fractional digits
             *        <LI><B>NUM_PREC_RADIX</B> int => Radix (typically either 10 or 2)
             *        <LI><B>NULLABLE</B> int => is NULL allowed.
             *      <UL>
             *      <LI> columnNoNulls - might not allow <code>NULL</code> values
             *      <LI> columnNullable - definitely allows <code>NULL</code> values
             *      <LI> columnNullableUnknown - nullability unknown
             *      </UL>
             *         <LI><B>COLUMN_DEF</B> String => default value (may be <code>null</code>)
             *        <LI><B>IS_NULLABLE</B> String => "NO" means column definitely
             *      does not allow NULL values; "YES" means the column might
             *      allow NULL values.  An empty string means nobody knows.
             */

            cx = store.createConnection();
            final DatabaseMetaData metaData = cx.getMetaData();
            columns = metaData.getColumns(null, store.getDatabaseSchema(), tableName, "%");

            columnIte :
            while (columns.next()) {
                adb.reset();
                atb.reset();

                final String name = columns.getString(Column.COLUMN_NAME);

                //we need the primary keys as fields since join query rely on them.
//                    //encomment to not include primary key in the type
//                    /*
//                     *        <LI><B>TABLE_CAT</B> String => table catalog (may be <code>null</code>)
//                     *        <LI><B>TABLE_SCHEM</B> String => table schema (may be <code>null</code>)
//                     *        <LI><B>TABLE_NAME</B> String => table name
//                     *        <LI><B>COLUMN_NAME</B> String => column name
//                     *        <LI><B>KEY_SEQ</B> short => sequence number within primary key
//                     *        <LI><B>PK_NAME</B> String => primary key name (may be <code>null</code>)
//                     */
//                    final ResultSet primaryKeys = metaData.getPrimaryKeys(null, databaseSchema, tableName);
//                    try {
//                        while (primaryKeys.next()) {
//                            if (name.equals(primaryKeys.getString(MD_COLUMN_NAME))) {
//                                continue columnIte;
//                            }
//                        }
//                    } finally {
//                        closeSafe(primaryKeys);
//                    }

                //figure out the type mapping

                //first ask the dialect
                Class binding = dialect.getMapping(columns, cx);

                if (binding == null) {
                    //determine from type mappings
                    final int dataType = columns.getInt(Column.DATA_TYPE);
                    binding = dialect.getMapping(dataType);
                }

                if (binding == null) {
                    //determine from type name mappings
                    final String tn = columns.getString(Column.TYPE_NAME);
                    binding = dialect.getMapping(tn);
                }

                //if still not found, resort to Object
                if (binding == null) {
                    store.getLogger().log(Level.WARNING, "Could not find mapping for:{0}", name);
                    binding = Object.class;
                }

                adb.setMinOccurs(1);
                //nullability
                if ( Column.VALUE_NO.equalsIgnoreCase( columns.getString(Column.IS_NULLABLE) ) ) {
                    adb.setNillable(false);
                }else{
                    adb.setNillable(true);
                }

                //primary key never null, min one
                final ResultSet primaryKeys = metaData.getPrimaryKeys(null, store.getDatabaseSchema(), tableName);
                try {
                    while (primaryKeys.next()) {
                        if (name.equals(primaryKeys.getString(Column.COLUMN_NAME))) {
                            adb.setNillable(false);
                            adb.setMinOccurs(1);
                            adb.addUserData(HintsPending.PROPERTY_IS_IDENTIFIER, Boolean.TRUE);
                            break;
                        }
                    }
                } finally {
                    store.closeSafe(primaryKeys);
                }


                //determine if this attribute is a geometry or not
                if (Geometry.class.isAssignableFrom(binding)) {
                    //add the attribute as a geometry, try to figure out
                    // its srid first
                    Integer srid = null;
                    CoordinateReferenceSystem crs = null;
                    try {
                        srid = dialect.getGeometrySRID(store.getDatabaseSchema(), tableName, name, cx);
                        if(srid != null)
                            crs = dialect.createCRS(srid, cx);
                    } catch (SQLException e) {
                        String msg = "Error occured determing srid for " + tableName + "."+ name;
                        store.getLogger().log(Level.WARNING, msg, e);
                    }

                    atb.setBinding(binding);
                    atb.setName(ensureGMLNS(namespace,name));
                    atb.setCRS(crs);
                    if(srid != null) adb.addUserData(JDBCDataStore.JDBC_NATIVE_SRID, srid);
                    adb.setName(ensureGMLNS(namespace,name));
                    adb.setType(atb.buildGeometryType());
                    adb.findBestDefaultValue();
                    tb.add(adb.buildDescriptor());
                } else {
                    //add the attribute
                    final Name attName = ensureGMLNS(namespace, name);
                    atb.setName(attName);
                    atb.setBinding(binding);
                    adb.setName(attName);
                    adb.setType(atb.buildType());
                    adb.findBestDefaultValue();
                    tb.add(adb.buildDescriptor());
                }
            }

            return tb.buildSimpleFeatureType();

        } catch (SQLException e) {
            throw new DataStoreException("Error occurred building feature type", e);
        }finally {
            store.closeSafe(columns);
            store.closeSafe(cx);
        }
    }

    /**
     * Returns the primary key object for a particular entry, deriving it from
     * the underlying database metadata.
     */
    private PrimaryKey createPrimaryKey(final Name entry) throws DataStoreException {
        final SQLDialect dialect = store.getDialect();

        PrimaryKey pkey;

        Connection cx = null;
        try {
            //get metadata from database
            cx = store.createConnection();
            final String tableName = entry.getLocalPart();
            final DatabaseMetaData metaData = cx.getMetaData();
            final ResultSet primaryKey = metaData.getPrimaryKeys(null, store.getDatabaseSchema(), tableName);

            try {
                /*
                 *        <LI><B>TABLE_CAT</B> String => table catalog (may be <code>null</code>)
                 *        <LI><B>TABLE_SCHEM</B> String => table schema (may be <code>null</code>)
                 *        <LI><B>TABLE_NAME</B> String => table name
                 *        <LI><B>COLUMN_NAME</B> String => column name
                 *        <LI><B>KEY_SEQ</B> short => sequence number within primary key
                 *        <LI><B>PK_NAME</B> String => primary key name (may be <code>null</code>)
                 */
                final ArrayList<PrimaryKeyColumn> cols = new ArrayList();

                while (primaryKey.next()) {
                    final String columnName = primaryKey.getString(Column.COLUMN_NAME);

                    //look up the type ( should only be one row )
                    final ResultSet columns = metaData.getColumns(null, store.getDatabaseSchema(), tableName, columnName);
                    columns.next();

                    final int binding = columns.getInt(Column.DATA_TYPE);
                    Class columnType = dialect.getMapping(binding);

                    if (columnType == null) {
                        store.getLogger().log(Level.WARNING, "No class for sql type {0}", binding);
                        columnType = Object.class;
                    }

                    //determine which type of primary key we have
                    PrimaryKeyColumn col = null;

                    //1. Auto Incrementing?
                    final Statement st = cx.createStatement();

                    try {
                        //not actually going to get data
                        st.setFetchSize(1);

                        final StringBuilder sql = new StringBuilder();
                        sql.append("SELECT ");
                        dialect.encodeColumnName(columnName, sql);
                        sql.append(" FROM ");
                        store.queryBuilder.encodeTableName(tableName, sql);

                        sql.append(" WHERE 0=1");

                        store.getLogger().log(Level.FINE, "Grabbing table pk metadata: {0}", sql);

                        ResultSet rs = null;
                        try {
                            rs = st.executeQuery(sql.toString());

                            if (rs.getMetaData().isAutoIncrement(1)) {
                                col = new AutoGeneratedPrimaryKeyColumn(columnName, columnType);
                            }
                        } finally {
                            store.closeSafe(rs);
                        }
                    } finally {
                        store.closeSafe(st);
                    }

                    //2. Has a sequence?
                    if (col == null) {
                        //TODO: look for a sequence
                        final String sequenceName = dialect.getSequenceForColumn(store.getDatabaseSchema(),
                                tableName, columnName, cx);
                        if (sequenceName != null) {
                            col = new SequencedPrimaryKeyColumn(columnName, columnType, sequenceName);
                        }
                    }

                    if (col == null) {
                        col = new NonIncrementingPrimaryKeyColumn(columnName, columnType);
                    }

                    cols.add(col);
                }

                if (cols.isEmpty()) {
                    store.getLogger().log(Level.INFO, "No primary key found for {0}.", tableName);
                    pkey = new NullPrimaryKey(tableName);
                } else {
                    pkey = new PrimaryKey(tableName, cols);
                }

            } finally {
                store.closeSafe(primaryKey);
            }
        } catch (SQLException e) {
            throw new DataStoreException("Error looking up primary key", e);
        } finally {
            store.closeSafe(cx);
        }

        return pkey;
    }


}
