/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2011-2013, Geomatys
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

package org.geotoolkit.db.reverse;


import com.vividsolutions.jts.geom.Geometry;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import org.geotoolkit.db.AbstractJDBCFeatureStoreFactory;
import org.geotoolkit.db.JDBCFeatureStore;
import org.geotoolkit.db.JDBCFeatureStoreUtilities;
import static org.geotoolkit.db.JDBCFeatureStoreUtilities.*;
import org.geotoolkit.db.dialect.SQLDialect;
import org.geotoolkit.db.reverse.ColumnMetaModel.Type;
import org.geotoolkit.db.reverse.MetaDataConstants.Column;
import org.geotoolkit.db.reverse.MetaDataConstants.ExportedKey;
import org.geotoolkit.db.reverse.MetaDataConstants.ImportedKey;
import org.geotoolkit.db.reverse.MetaDataConstants.Index;
import org.geotoolkit.db.reverse.MetaDataConstants.Schema;
import org.geotoolkit.db.reverse.MetaDataConstants.SuperTable;
import org.geotoolkit.db.reverse.MetaDataConstants.Table;
import org.geotoolkit.factory.HintsPending;
import org.geotoolkit.feature.AttributeDescriptorBuilder;
import org.geotoolkit.feature.AttributeTypeBuilder;
import org.geotoolkit.feature.DefaultName;
import org.geotoolkit.feature.FeatureTypeBuilder;
import org.geotoolkit.feature.type.ModifiableFeatureTypeFactory;
import org.geotoolkit.feature.type.ModifiableType;
import org.geotoolkit.parameter.Parameters;
import org.geotoolkit.storage.DataStoreException;
import org.opengis.feature.type.*;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Represent the structure of the database. The work done here is similar to
 * reverse engineering.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class DataBaseModel {

    public static final String ASSOCIATION_SEPARATOR = "→"; 
    
    /**
     * The native SRID associated to a certain descriptor
     */
    public static final String JDBC_NATIVE_SRID = "nativeSRID";
    
    /**
     * Custom factory where types can be modified after they are created.
     */
    private static final FeatureTypeFactory FTF = new ModifiableFeatureTypeFactory();
    /**
     * Dummy type which will be replaced dynamically in the reverse engineering process.
     */
    private static final ComplexType FLAG_TYPE = FTF.createComplexType(
            new DefaultName("flag"), Collections.EMPTY_LIST, false, false, Collections.EMPTY_LIST, null, null);

    private final JDBCFeatureStore store;
    private final Map<Name,PrimaryKey> pkIndex = new HashMap<Name, PrimaryKey>();
    private final Map<Name,FeatureType> typeIndex = new HashMap<Name, FeatureType>();
    private Map<String,SchemaMetaModel> schemas = null;
    private Set<Name> nameCache = null;
    private final boolean simpleTypes;
    
    //metadata getSuperTable query is not implemented on all databases
    private Boolean handleSuperTableMetadata = null;

    public DataBaseModel(final JDBCFeatureStore store, final boolean simpleTypes){
        this.store = store;
        this.simpleTypes = simpleTypes;
    }

    public Collection<SchemaMetaModel> getSchemaMetaModels() throws DataStoreException {
        if(schemas == null){
            analyze();
        }
        return schemas.values();
    }

    public SchemaMetaModel getSchemaMetaModel(String name) throws DataStoreException{
        if(schemas == null){
            analyze();
        }
        return schemas.get(name);
    }

    /**
     * Clear the model cache. A new database analyze will be made the next time
     * it is needed.
     */
    public synchronized void clearCache(){
        pkIndex.clear();
        typeIndex.clear();
        nameCache = null;
        schemas = null;
    }

    public PrimaryKey getPrimaryKey(final Name featureTypeName) throws DataStoreException{
        if(schemas == null){
            analyze();
        }
        return pkIndex.get(featureTypeName);
    }

    public synchronized Set<Name> getNames() throws DataStoreException {
        Set<Name> ref = nameCache;
        if(ref == null){
            analyze();
            final Set<Name> names = new HashSet<Name>();
            for(Entry<Name,FeatureType> entry : typeIndex.entrySet()){
                if(Boolean.TRUE.equals(entry.getValue().getUserData().get("subtype"))) continue;
                if(store.getDialect().ignoreTable(entry.getKey().getLocalPart())) continue;
                names.add(entry.getKey());
            }
            ref = Collections.unmodifiableSet(names);
            nameCache = ref;
        }
        return ref;
    }

    public FeatureType getFeatureType(final Name typeName) throws DataStoreException {
        if(schemas == null){
            analyze();
        }
        return typeIndex.get(typeName);
    }

    /**
     * Explore all tables and views then recreate a complex feature model from
     * relations.
     */
    private synchronized void analyze() throws DataStoreException{
        if(schemas != null){
            //already analyzed
            return;
        }

        clearCache();
        schemas = new HashMap<String, SchemaMetaModel>();

        Connection cx = null;
        ResultSet schemaSet = null;
        try {
            cx = store.getDataSource().getConnection();

            final DatabaseMetaData metadata = cx.getMetaData();
            schemaSet = metadata.getSchemas();
            while (schemaSet.next()) {
                final String SchemaName = schemaSet.getString(Schema.TABLE_SCHEM);
                final SchemaMetaModel schema = analyzeSchema(SchemaName,cx);
                schemas.put(schema.name, schema);
            }
            
            reverseSimpleFeatureTypes(cx);
            reverseComplexFeatureTypes();

        } catch (SQLException e) {
            throw new DataStoreException("Error occurred analyzing database model.", e);
        } finally {
            closeSafe(store.getLogger(),schemaSet);
            closeSafe(store.getLogger(),cx);
        }


        //build indexes---------------------------------------------------------
        final String baseSchemaName = store.getDatabaseSchema();

        final Collection<SchemaMetaModel> candidates;
        if(baseSchemaName == null){
            //take all schemas
            candidates = getSchemaMetaModels();
        }else{
            candidates = Collections.singleton(getSchemaMetaModel(baseSchemaName));
        }

        for(SchemaMetaModel schema : candidates){
           if (schema != null) {
                for(TableMetaModel table : schema.tables.values()){
                    
                    final ComplexType ft;
                    if(simpleTypes || table.isSubType()){
                        ft = table.getSimpleType();
                    }else{
                        ft = table.getComplexType();
                    }
                    final Name name = ft.getName();
                    pkIndex.put(name, table.key);
                    if(table.isSubType()){
                        //we don't show subtype, they are part of other feature types, add a flag to identify then
                        ft.getUserData().put("subtype", Boolean.TRUE);
                    }
                    typeIndex.put(name, (FeatureType)ft);
                 }
            } else {
                throw new DataStoreException("Specifed schema " + baseSchemaName + " does not exist.");
             }
         }
        
    }

    private SchemaMetaModel analyzeSchema(final String schemaName, final Connection cx) throws DataStoreException{

        final SchemaMetaModel schema = new SchemaMetaModel(schemaName);

        ResultSet tableSet = null;
        try {

            final DatabaseMetaData metadata = cx.getMetaData();
            final String tableNamePattern = 
                    (Parameters.getOrCreate(AbstractJDBCFeatureStoreFactory.TABLE,store.getConfiguration()).getValue()!=null)
                    ? Parameters.getOrCreate(AbstractJDBCFeatureStoreFactory.TABLE,store.getConfiguration()).getValue().toString()
                    : "%";
            tableSet = metadata.getTables(null, schemaName, tableNamePattern,
                    new String[]{Table.VALUE_TYPE_TABLE, Table.VALUE_TYPE_VIEW});

            while (tableSet.next()) {
                final TableMetaModel table = analyzeTable(tableSet,cx);
                schema.tables.put(table.name, table);
            }

        } catch (SQLException e) {
            throw new DataStoreException("Error occurred analyzing database model.", e);
        } finally {
            closeSafe(store.getLogger(),tableSet);
        }

        return schema;
    }

    private TableMetaModel analyzeTable(final ResultSet tableSet, final Connection cx) throws DataStoreException, SQLException{
        final SQLDialect dialect = store.getDialect();
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder(FTF);

        final String schemaName = tableSet.getString(Table.TABLE_SCHEM);
        final String tableName = tableSet.getString(Table.TABLE_NAME);
        final String tableType = tableSet.getString(Table.TABLE_TYPE);

        final TableMetaModel table = new TableMetaModel(tableName,tableType);
        
        ResultSet result = null;
        try {
            final DatabaseMetaData metadata = cx.getMetaData();

            //explore all columns ----------------------------------------------
            result = metadata.getColumns(null, schemaName, tableName, "%");
            while (result.next()) {
                ftb.add(analyzeColumn(result,cx));
            }
            closeSafe(store.getLogger(),result);

            //find primary key -------------------------------------------------
            final List<ColumnMetaModel> cols = new ArrayList();
            result = metadata.getPrimaryKeys(null, schemaName, tableName);
            while (result.next()) {
                final String columnName = result.getString(Column.COLUMN_NAME);

                //look up the type ( should only be one row )
                final ResultSet columns = metadata.getColumns(null, schemaName, tableName, columnName);
                columns.next();

                final int sqlType = columns.getInt(Column.DATA_TYPE);
                final String sqlTypeName = columns.getString(Column.TYPE_NAME);
                Class columnType = dialect.getJavaType(sqlType, sqlTypeName);

                if (columnType == null) {
                    store.getLogger().log(Level.WARNING, "No class for sql type {0}", sqlType);
                    columnType = Object.class;
                }

                ColumnMetaModel col = null;

                final String str = columns.getString(Column.IS_AUTOINCREMENT);
                if(Column.VALUE_YES.equalsIgnoreCase(str)){
                    col = new ColumnMetaModel(schemaName, tableName, columnName, sqlType, sqlTypeName, columnType, Type.AUTO);
                }else {
                    final String sequenceName = dialect.getColumnSequence(cx,schemaName, tableName, columnName);
                    if (sequenceName != null) {
                        col = new ColumnMetaModel(schemaName, tableName, columnName, sqlType, 
                                sqlTypeName, columnType, Type.SEQUENCED,sequenceName);
                    }else{
                        col = new ColumnMetaModel(schemaName, tableName, columnName, sqlType, 
                                sqlTypeName, columnType, Type.NON_INCREMENTING);
                    }
                }

                cols.add(col);
            }

            //Search indexes, they provide informations such as :
            // - Unique indexes may indicate 1:1 relations in complexe features
            // - Unique indexes can be used as primary key if no primary key are defined
            final boolean pkEmpty = cols.isEmpty();
            final List<String> names = new ArrayList<String>();
            final Map<String,List<String>> uniqueIndexes = new HashMap<String, List<String>>();
            String indexname = null;
            result = metadata.getIndexInfo(null, schemaName, tableName, true, false);
            while (result.next()) {
                final String columnName = result.getString(Index.COLUMN_NAME);
                final String idxName = result.getString(Index.INDEX_NAME);
                
                List<String> lst = uniqueIndexes.get(idxName);
                if(lst == null){
                    lst = new ArrayList<String>();
                    uniqueIndexes.put(idxName, lst);
                }
                lst.add(columnName);
                
                if(pkEmpty){
                    //we use a single index columns set as primary key
                    //we must not mix with other potential indexes.
                    if(indexname == null){
                        indexname = idxName;
                    }else if(!indexname.equals(idxName)){
                        continue;
                    }
                    names.add(columnName);
                }
            }
            closeSafe(store.getLogger(),result);
            
            //for each unique index composed of one column add a flag on the property descriptor
            for(Entry<String,List<String>> entry : uniqueIndexes.entrySet()){
                final List<String> columns = entry.getValue();
                if(columns.size() == 1){
                    String columnName = columns.get(0);
                    for(PropertyDescriptor desc : ftb.getProperties()){
                        if(desc.getName().getLocalPart().equals(columnName)){
                            desc.getUserData().put(JDBCFeatureStore.JDBC_PROPERTY_UNIQUE, Boolean.TRUE);
                        }
                    }
                }
            }
            
            if(pkEmpty && !names.isEmpty()){
                //build a primary key from unique index
                result = metadata.getColumns(null, schemaName, tableName, "%");
                while (result.next()) {
                    final String columnName = result.getString(Column.COLUMN_NAME);
                    if(!names.contains(columnName)){
                        continue;
                    }

                    final int sqlType = result.getInt(Column.DATA_TYPE);
                    final String sqlTypeName = result.getString(Column.TYPE_NAME);
                    final Class columnType = dialect.getJavaType(sqlType, sqlTypeName);                        
                    final ColumnMetaModel col = new ColumnMetaModel(schemaName, tableName, columnName, 
                            sqlType, sqlTypeName, columnType, Type.NON_INCREMENTING);
                    cols.add(col);
                    
                    //set the information
                    for(PropertyDescriptor desc : ftb.getProperties()){
                        if(desc.getName().getLocalPart().equals(columnName)){
                            desc.getUserData().put(HintsPending.PROPERTY_IS_IDENTIFIER,Boolean.TRUE);
                            break;
                        }
                    }
                }
                closeSafe(store.getLogger(),result);
            }
            
            
            if(cols.isEmpty()){
                store.getLogger().log(Level.INFO, "No primary key found for {0}.", tableName);
            }
            table.key = new PrimaryKey(tableName, cols);
            
            closeSafe(store.getLogger(),result);

            //find imported keys -----------------------------------------------
            result = metadata.getImportedKeys(null, schemaName, tableName);
            while (result.next()) {
                final String localColumn = result.getString(ImportedKey.FKCOLUMN_NAME);
                final String refSchemaName = result.getString(ImportedKey.PKTABLE_SCHEM);
                final String refTableName = result.getString(ImportedKey.PKTABLE_NAME);
                final String refColumnName = result.getString(ImportedKey.PKCOLUMN_NAME);
                final int deleteRule = result.getInt(ImportedKey.DELETE_RULE);
                final boolean deleteCascade = DatabaseMetaData.importedKeyCascade == deleteRule;
                final RelationMetaModel relation = new RelationMetaModel(localColumn,
                        refSchemaName, refTableName, refColumnName, true, deleteCascade);
                table.importedKeys.add(relation);
                
                //set the information
                for(PropertyDescriptor desc : ftb.getProperties()){
                    if(desc.getName().getLocalPart().equals(localColumn)){
                        desc.getUserData().put(JDBCFeatureStore.JDBC_PROPERTY_RELATION,relation);
                        break;
                    }
                }
            }
            closeSafe(store.getLogger(),result);

            //find exported keys -----------------------------------------------
            result = metadata.getExportedKeys(null, schemaName, tableName);
            while (result.next()) {
                final String localColumn = result.getString(ExportedKey.PKCOLUMN_NAME);
                final String refSchemaName = result.getString(ExportedKey.FKTABLE_SCHEM);
                final String refTableName = result.getString(ExportedKey.FKTABLE_NAME);
                final String refColumnName = result.getString(ExportedKey.FKCOLUMN_NAME);
                final int deleteRule = result.getInt(ImportedKey.DELETE_RULE);
                final boolean deleteCascade = DatabaseMetaData.importedKeyCascade == deleteRule;
                table.exportedKeys.add(new RelationMetaModel(localColumn,
                        refSchemaName, refTableName, refColumnName, false, deleteCascade));
            }
            closeSafe(store.getLogger(),result);

            //find parent table if any -----------------------------------------
            if(handleSuperTableMetadata == null || handleSuperTableMetadata){
                try{
                    result = metadata.getSuperTables(null, schemaName, tableName);
                    while (result.next()) {
                        final String parentTable = result.getString(SuperTable.SUPERTABLE_NAME);
                        table.parents.add(parentTable);
                    }
                }catch(final SQLException ex){
                    //not implemented by database
                    handleSuperTableMetadata = Boolean.FALSE;
                    store.getLogger().log(Level.INFO, "Database does not handle getSuperTable, feature type hierarchy will be ignored.");
                }finally{
                    closeSafe(store.getLogger(),result);
                }
            }

        } catch (SQLException e) {
            throw new DataStoreException("Error occurred analyzing table : " + tableName, e);
        } finally {
            closeSafe(store.getLogger(),result);
        }

        ftb.setName(tableName);
        table.baseType = ftb.buildType();
        return table;
    }

    private AttributeDescriptor analyzeColumn(final ResultSet columnSet, final Connection cx) throws SQLException, DataStoreException{
        final SQLDialect dialect = store.getDialect();
        final AttributeDescriptorBuilder adb = new AttributeDescriptorBuilder(FTF);
        final AttributeTypeBuilder atb = new AttributeTypeBuilder(FTF);

        final String schemaName     = columnSet.getString(Column.TABLE_SCHEM);
        final String tableName      = columnSet.getString(Column.TABLE_NAME);
        final String columnName     = columnSet.getString(Column.COLUMN_NAME);
        final int columnDataType    = columnSet.getInt(Column.DATA_TYPE);
        final String columnTypeName = columnSet.getString(Column.TYPE_NAME);
        final String columnNullable = columnSet.getString(Column.IS_NULLABLE);

        atb.setName(columnName);
        adb.setName(columnName);

        try {
            dialect.decodeColumnType(atb, cx, columnTypeName, columnDataType, schemaName, tableName, columnName);
        } catch (SQLException e) {
            throw new DataStoreException("Error occurred analyzing column : " + columnName, e);
        }

        //table values are always min 1, max 1
        adb.setMinOccurs(1);
        adb.setMaxOccurs(1);

        //nullability
        adb.setNillable(!Column.VALUE_NO.equalsIgnoreCase(columnNullable));

        if(Geometry.class.isAssignableFrom(atb.getBinding())){
            adb.setType(atb.buildGeometryType());
        }else{
            adb.setType(atb.buildType());
        }
        adb.findBestDefaultValue();
        return adb.buildDescriptor();
    }

    /**
     * Analyze the metadata of the ResultSet to rebuild a feature type.
     *
     * @param result
     * @param name
     * @return FeatureType
     * @throws SQLException
     */
    public FeatureType analyzeResult(final ResultSet result, final String name) throws SQLException, DataStoreException{
        final SQLDialect dialect = store.getDialect();
        final String namespace = store.getDefaultNamespace();
        
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder(FTF);
        ftb.setName(namespace, name);

        final ResultSetMetaData metadata = result.getMetaData();
        final int nbcol = metadata.getColumnCount();

        for(int i=1; i<=nbcol; i++){
            final String columnName = metadata.getColumnName(i);
            final String typeName = metadata.getColumnTypeName(i);
            final String schemaName = metadata.getSchemaName(i);
            final String tableName = metadata.getTableName(i);
            final int sqlType = metadata.getColumnType(i);
            final String sqlTypeName = metadata.getColumnTypeName(i);

            //search if we already have this minute
            PropertyDescriptor desc = null;
            final SchemaMetaModel schema = getSchemaMetaModel(schemaName);
            if(schema != null){
                TableMetaModel table = schema.getTable(tableName);
                if(table != null){
                    desc = table.getSimpleType().getDescriptor(columnName);
                }
            }

            if(desc == null){
                //could not find the original type
                //this column must be calculated
                final AttributeDescriptorBuilder adb = new AttributeDescriptorBuilder(FTF);
                final AttributeTypeBuilder atb = new AttributeTypeBuilder(FTF);

                adb.setName(namespace, columnName);
                adb.setMinOccurs(1);
                adb.setMaxOccurs(1);

                final int nullable = metadata.isNullable(i);
                adb.setNillable(nullable == metadata.columnNullable);


                atb.setName(namespace, columnName);
                Connection cx = null;
                try {
                    cx = store.getDataSource().getConnection();
                    final Class type = dialect.getJavaType(sqlType, sqlTypeName);
                    atb.setName(columnName);
                    atb.setBinding(type);
                } catch (SQLException e) {
                    throw new DataStoreException("Error occurred analyzing column : " + columnName, e);
                } finally {
                    closeSafe(store.getLogger(),cx);
                }

                if(Geometry.class.isAssignableFrom(atb.getBinding())){
                    adb.setType(atb.buildGeometryType());
                }else{
                    adb.setType(atb.buildType());
                }
                
                adb.findBestDefaultValue();
                desc = adb.buildDescriptor();
            }

            ftb.add(desc);
        }

        return ftb.buildFeatureType();
    }

    /**
     * Rebuild simple feature types for each table.
     */
    private void reverseSimpleFeatureTypes(final Connection cx){
        final SQLDialect dialect = store.getDialect();
        
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder(FTF);
        final AttributeDescriptorBuilder adb = new AttributeDescriptorBuilder(FTF);
        final AttributeTypeBuilder atb = new AttributeTypeBuilder(FTF);

        for(final SchemaMetaModel schema : schemas.values()){
            for(final TableMetaModel table : schema.tables.values()){
                final String tableName = table.name;
                
                //add flag for primary key fields-------------------------------
                final PrimaryKey pk = table.key;
                for(ColumnMetaModel column : pk.getColumns()){
                    final String attName = column.getName();
                    final PropertyDescriptor base = table.baseType.getDescriptor(attName);
                    base.getUserData().put(HintsPending.PROPERTY_IS_IDENTIFIER, Boolean.TRUE);
                }


                //fill the namespace--------------------------------------------
                ftb.reset();
                ftb.copy(table.baseType);
                final String namespace = store.getDefaultNamespace();
                ftb.setName(namespace, ftb.getName().getLocalPart());


                final List<PropertyDescriptor> descs = ftb.getProperties();

                for(int i=0,n=descs.size(); i<n; i++){
                    final PropertyDescriptor desc = descs.get(i);
                    final PropertyType type = desc.getType();
                    final String name = desc.getName().getLocalPart();

                    adb.reset();
                    adb.copy((AttributeDescriptor) desc);
                    adb.setName(namespace,name);
                    atb.reset();
                    atb.copy((AttributeType) type);
                    atb.setName(namespace,name);
                    adb.setType(atb.buildType());

                    //Set the CRS if it's a geometry
                    if (Geometry.class.isAssignableFrom(type.getBinding())) {
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

                        atb.setCRS(crs);
                        if(srid != null){
                            adb.addUserData(JDBCFeatureStore.JDBC_PROPERTY_SRID, srid);
                        }
                        adb.setType(atb.buildGeometryType());
                        adb.findBestDefaultValue();
                    }
                    
                    descs.set(i, adb.buildDescriptor());
                }

                table.simpleType = ftb.buildSimpleFeatureType();
            }
        }
        
    }

    /**
     * Rebuild complex feature types using foreign key relations.
     */
    private void reverseComplexFeatureTypes(){
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder(FTF);
        final AttributeDescriptorBuilder adb = new AttributeDescriptorBuilder(FTF);
        final AttributeTypeBuilder atb = new AttributeTypeBuilder(FTF);
        
        //result map
        final Map<String,ModifiableType> builded = new HashMap<String, ModifiableType>();

        //first pass to create the real types but without relations types-------
        //since we must have all of them before creating relations
        final List<Object[]> secondPass = new ArrayList<Object[]>();
        
        for(final SchemaMetaModel schema : schemas.values()){
            for(final TableMetaModel table : schema.tables.values()){
                final String code = schema.name +"."+table.name;

                //create the complex model by replacing descriptors
                final ComplexType baseType = table.simpleType;
                ftb.reset();
                ftb.copy(baseType);

                // replace 0:1 relations----------------------------------------
                for(final RelationMetaModel relation : table.importedKeys){
                    
                    //find the descriptor to replace
                    final List<PropertyDescriptor> descs = ftb.getProperties();
                    int index = -1;
                    for(int i=0,n=descs.size();i<n;i++){
                        final PropertyDescriptor pd = descs.get(i);
                        if(pd.getName().getLocalPart().equals(relation.getCurrentColumn())){
                            index = i;
                        }
                    }

                    if(table.isSubType() && relation.isDeleteCascade()){
                        //this type is a subtype and relation points toward it's parent
                        //so we actualy don't want to view this property since it will
                        //be visible the other way araound (parent -> child)
                        descs.remove(index);
                        continue;
                    }else{
                        //create the new descriptor derivated
                        final PropertyDescriptor baseDescriptor = descs.get(index);
                        adb.reset();
                        adb.copy((AttributeDescriptor) baseDescriptor);
                        adb.setType(FLAG_TYPE);
                        adb.setDefaultValue(null);
                        adb.addUserData(JDBCFeatureStore.JDBC_PROPERTY_RELATION, relation);
                        final PropertyDescriptor newDescriptor = adb.buildDescriptor();
                        descs.set(index, newDescriptor);
                    }

                    final Object[] futur = new Object[]{schema, table, relation};
                    secondPass.add(futur);
                }

                // create N:1 relations-----------------------------------------
                for(final RelationMetaModel relation : table.exportedKeys){
                    
                    //find an appropriate name
                    Name n = new DefaultName(store.getDefaultNamespace(),relation.getForeignColumn());
                    for(PropertyDescriptor dpd : ftb.getProperties()){
                        if(n.getLocalPart().equals(dpd.getName().getLocalPart())){
                            //name already used, make it unique by including reference table name
                            n = new DefaultName(store.getDefaultNamespace(),
                                relation.getForeignTable()+ASSOCIATION_SEPARATOR+relation.getForeignColumn());
                            break;
                        }
                    }
                    
                    final ComplexType foreignType = schema.getTable(relation.getForeignTable()).getBaseType();
                    final PropertyDescriptor propDesc = foreignType.getDescriptor(relation.getForeignColumn());
                    final boolean unique = Boolean.TRUE.equals(propDesc.getUserData().get(JDBCFeatureStore.JDBC_PROPERTY_UNIQUE));
                    adb.reset();
                    adb.setName(n);
                    adb.setType(FLAG_TYPE);
                    adb.setMinOccurs(0);
                    adb.setMaxOccurs(unique? 1 : Integer.MAX_VALUE);
                    adb.setNillable(false);
                    adb.setDefaultValue(null);
                    adb.addUserData(JDBCFeatureStore.JDBC_PROPERTY_RELATION, relation);
                    final PropertyDescriptor newDescriptor = adb.buildDescriptor();
                    final int index = ftb.add(newDescriptor);
                    
                    final Object[] futur = new Object[]{schema, table, relation, index};
                    secondPass.add(futur);
                }

                final ComplexType ft;
                if(table.isSubType()){
                    //we wont a complex type for sub types, we don't want them to have ids
                    ft = ftb.buildType();
                    //store it as a real feature type in the table
                    table.complexType = ftb.buildFeatureType();
                }else{
                    ft = ftb.buildFeatureType();
                    table.complexType = ft;
                }
                builded.put(code, (ModifiableType) ft);
                
            }
        }

        //second pass to fill relations-----------------------------------------
        for(Object[] futur : secondPass){
            final SchemaMetaModel schema = (SchemaMetaModel) futur[0];
            final TableMetaModel table = (TableMetaModel) futur[1];
            final RelationMetaModel relation = (RelationMetaModel) futur[2];
            //final int index = (Integer) futur[3];
            
            final String code = schema.name +"."+table.name;            
            final ModifiableType candidate = builded.get(code);
            final List<PropertyDescriptor> descs = candidate.getDescriptors();
            final String relCode = relation.getForeignSchema() +"."+relation.getForeignTable();
            final ComplexType relType = builded.get(relCode);
                        
            //create the new association descriptor derivated
            atb.reset();
            atb.copy(relType);
            atb.setParentType(null);
            
            //find the descriptor to replace
            int index = -1;
            final String searchedName = relation.isImported() ? relation.getCurrentColumn() : relation.getForeignColumn();
            for(int i=0,n=descs.size();i<n;i++){
                final PropertyDescriptor pd = descs.get(i);
                if(pd.getName().getLocalPart().equals(searchedName)){
                    index = i;
                }
            }
            
            final PropertyDescriptor baseDescriptor = descs.get(index);
            adb.reset();
            adb.copy(baseDescriptor);
            adb.setDefaultValue(null);
            
            final PropertyDescriptor newDescriptor;
            if(relation.isDeleteCascade()){
                adb.setType(relType);
                newDescriptor = adb.buildDescriptor();
            }else{
                adb.setType(atb.buildAssociationType(relType));
                newDescriptor = adb.buildAssociationDescriptor();
            }

            newDescriptor.getUserData().put(JDBCFeatureStore.JDBC_PROPERTY_RELATION,relation);
            candidate.changeProperty(index, newDescriptor);
        }
        
    }

}
