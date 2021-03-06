/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2002-2008, Open Source Geospatial Foundation (OSGeo)
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
package org.geotoolkit.feature.type;

import org.opengis.util.GenericName;
import com.vividsolutions.jts.geom.Geometry;
import java.util.Collection;
import java.util.List;
import org.geotoolkit.feature.simple.DefaultSimpleFeatureType;
import org.opengis.coverage.Coverage;
import org.geotoolkit.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.InternationalString;


/**
 * This implementation is capable of creating a good default implementation of
 * the Types used in the feature model.
 * <p>
 * The implementation focus here is on corretness rather then efficiency or even
 * strict error messages. The code serves as a good example, but is not
 * optimized for any particular use.
 * </p>
 *
 * @author Jody Garnett
 * @module pending
 */
public class DefaultFeatureTypeFactory implements FeatureTypeFactory {

    public DefaultFeatureTypeFactory() {
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public AssociationDescriptor createAssociationDescriptor(final AssociationType type,
            final GenericName name, final int minOccurs, final int maxOccurs, final boolean isNillable){
        return new DefaultAssociationDescriptor(type, name, minOccurs, maxOccurs, isNillable);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public AttributeDescriptor createAttributeDescriptor(final AttributeType type, final GenericName name,
            final int minOccurs, final int maxOccurs, final boolean isNillable, final Object defaultValue){
        if(type instanceof GeometryType){
            return createGeometryDescriptor((GeometryType)type, name, minOccurs, maxOccurs, isNillable, defaultValue);
        }else{
            return new DefaultAttributeDescriptor(type, name, minOccurs, maxOccurs, isNillable, defaultValue);
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public GeometryDescriptor createGeometryDescriptor(final GeometryType type, final GenericName name,
            final int minOccurs, final int maxOccurs, final boolean isNillable, final Object defaultValue){
        return new DefaultGeometryDescriptor(type, name, minOccurs, maxOccurs, isNillable, defaultValue);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public AssociationType createAssociationType(final GenericName name, final AttributeType relatedType,
            final boolean isAbstract, final List restrictions, final AssociationType superType,
            final InternationalString description){
        return new DefaultAssociationType(name, relatedType,
                isAbstract, restrictions, superType, description);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public AttributeType createAttributeType(final GenericName name, final Class binding,
            final boolean isIdentifiable, final boolean isAbstract, final List restrictions,
            final AttributeType superType, final InternationalString description){
        if(Geometry.class.isAssignableFrom(binding)
                || org.opengis.geometry.Geometry.class.isAssignableFrom(binding)
                || Coverage.class.isAssignableFrom(binding)){
            return createGeometryType(name, binding, null, isIdentifiable, isAbstract,
                restrictions, superType, description);
        }else{
            return new DefaultAttributeType(name, binding, isIdentifiable, isAbstract,
                restrictions, superType, description);
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public ComplexType createComplexType(final GenericName name, final Collection schema,
            final boolean isIdentifiable, final boolean isAbstract, final List restrictions,
            final AttributeType superType, final InternationalString description) throws IllegalArgumentException{
        AttributeDescriptor[] testNames = new AttributeDescriptor[schema.size()];
        schema.toArray(testNames);
        for(int i = 0 ; i < testNames.length-1 ; i++){
            final GenericName toTest = testNames[i].getName();
            for(int j = i+1 ; j < testNames.length ; j++){
                if(toTest.equals(testNames[j].getName()))
                    throw new IllegalArgumentException("We can't build a complexType with multiple descriptor owning the same name");
            }
        }
        return new DefaultComplexType(name, schema, isIdentifiable, isAbstract,
                restrictions, superType, description);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public GeometryType createGeometryType(final GenericName name, final Class binding,
            final CoordinateReferenceSystem crs, final boolean isIdentifiable,
            final boolean isAbstract, final List restrictions, final AttributeType superType,
            final InternationalString description){
        return new DefaultGeometryType(name, binding, crs, isIdentifiable,
                isAbstract, restrictions, superType, description);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public FeatureType createFeatureType(final GenericName name, final Collection<PropertyDescriptor> schema,
            final GeometryDescriptor defaultGeometry, final boolean isAbstract,
            final List<Filter> restrictions, final AttributeType superType, final InternationalString description){
        return new DefaultFeatureType(name, schema, defaultGeometry,
                isAbstract, restrictions, superType, description);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public SimpleFeatureType createSimpleFeatureType(final GenericName name, final List<AttributeDescriptor> schema,
            final GeometryDescriptor defaultGeometry, final boolean isAbstract,
            final List<Filter> restrictions, final AttributeType superType,
            final InternationalString description){
        return new DefaultSimpleFeatureType(name, schema, defaultGeometry, isAbstract,
                restrictions, superType, description);
    }
}
