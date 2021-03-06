/*
 *    GeoAPI - Java interfaces for OGC/ISO standards
 *    http://www.geoapi.org
 *
 *    Copyright (C) 2006-2014 Open Geospatial Consortium, Inc.
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

import org.opengis.util.GenericName;
import java.util.List;
import java.util.Map;

import org.geotoolkit.feature.Property;
import org.opengis.filter.Filter;

/**
 * The type of a Property.
 * <p>
 * A property type defines information about the value of a property. This
 * includes:
 * <ul>
 *   <li>java class of the value of the property ((also known as the property "binding")
 *   <li>any restrictions on the value of the property
 *   <li>a description of the property
 *   <li>if the type is abstract or not
 * </ul>
 * </p>
 * <br/>
 * <p>
 *  <h3>Binding</h3>
 *  The {@link #getBinding()} method returns the java class of which the value
 *  of the property is an instance of.
 *  <pre>
 *   Property property = ...;
 *   property.getType().getBinding().isAssignableFrom(property.getValue().getClass());
 *  </pre>
 * </p>
 * <p>
 * <h3>Restrictions</h3>
 * The {@link #getRestrictions()} method returns a set of {@link Filter} objects
 * which define additional restrictions on the value of the property.
 * <pre>
 *   Property property = ...;
 *   for ( Filter restriction : property.getType().getRestrictions() ) {
 *       restriction.evaluate( property ) == true;
 *   }
 * </pre>
 * </p>
 * <p>
 * <h3>Inheritance</h3>
 * A property type may extend from another property type. When this occurs any
 * restrictions defined by the parent type are inherited by the child type. The
 * binding declared by the super type may or may not be a super class of the
 * binding declared by the child type.
 * </p>
 * <p>
 * <h3>Abstract Types</h3>
 * A property type may be abstract similar to how a java class can be abstract.
 * Such property types are usually not referenced directly by a descriptor, but
 * usually are the parent type of a non-abstract property type.
 * </p>
 * <p>
 *  <h3>Example</h3>
 *  Property, PropertyDescriptor, and PropertyType are very similar to concepts
 *  encountered in xml schema. Consider the following xml schema:
 *  <pre>
 *    &lt; simpleType name="number"/>
 *
 *    &lt; simpleType name="integer"/>
 *
 *    &lt; complexType name="myComplexType"/>
 *      &lt;element name="foo" type="integer"/>
 *    &lt;/complexType>
 *  </pre>
 *  <br>
 *  In the above, "number", "integer", and "myComplexType" all map to PropertyType.
 *  While "foo" maps to a PropertyDescriptor. Consider a complex attribute which is
 *  of type "myComplexType:
 *  <pre>
 *  ComplexAttribute complexAttribute = ...;
 *  ComplexType complexType = complexAttribute.getType();
 *
 *  complexType.getName().getLocalPart() == "myComplexType";
 *
 *  //the property descriptor
 *  PropertyDescriptor propertyDescriptor = complexType.getProperty( "foo" );
 *  propertyDescriptor.getName().getLocalPart() == "foo";
 *
 *  //the property type
 *  PropertyType propertyType = propertyDescriptor.getType();
 *  propertyType.getName().getLocalPart() == "integer";
 *  propertyType.getBinding() == Integer.class;
 *  propertyType.getSuper().getName().getLocalPart() == "number";
 *  propertyType.getSuper().getBinding() == Number.class;
 *
 *  //the property
 *  Property property = complexAttribute.getProperty( "foo" );
 *  property.getDescriptor() == propertyDescriptor;
 *  property.getType() == propertyType;
 *  property.getName().getLocalPart() == "foo";
 *  property.getValue() instanceof Integer;
 *  </pre>
 * </p>

 * @author Jody Garnett, Refractions Research, Inc.
 * @author Justin Deoliveira, The Open Planning Project
 *
 * @deprecated Replaced by {@link org.opengis.feature.PropertyType} in the {@code org.opengis.feature} package.
 */
@Deprecated
public interface PropertyType extends org.opengis.feature.PropertyType {
    /**
     * The name of the property type.
     * <p>
     * Note that this is not the same name as {@link Property#getName()}, which
     * is the name of the instance of the type, not the type itself.
     * </p>
     * <p>
     * The returned name is a qualified name made up of two parts. The first
     * a namespace uri ({@link Name#getNamespaceURI()}, and the second a local
     * part ({@link Name#getLocalPart()}.
     * </p>
     * <p>
     * This value is never <code>null</code>.
     * </p>
     * @return The name of the property type.
     *
     * @deprecated Inherited from {@link org.opengis.feature.PropertyType#getName()}.
     */
    @Override
    GenericName getName();

    /**
     * The java class that values of properties of the property type are bound
     * to.
     * <p>
     * This value is never <code>null</code>.
     * </p>
     * @return The binding of the property type.
     *
     * @deprecated Moved to {@link org.opengis.feature.AttributeType#getValueClass()}.
     */
    Class<?> getBinding();

    /**
     * The parent type of the property type.
     * <p>
     * This method returns <code>null</code> if no super type is defined.
     * </p>
     * <p>
     * The super type may contain additional restrictions to be considered against
     * properties of the the property type.
     * </p>
     *
     * @return The parent or super type, or <code>null</code>.
     *
     * @deprecated Moved to {@link org.opengis.feature.FeatureType#getSuperTypes()}.
     */
    PropertyType getSuper();

    /**
     * Flag indicating if the type is abstract or not.
     *
     * @return <code>true</code> if the type is abstract, otherwise <code>false</code>.
     *
     * @deprecated Moved to {@link org.opengis.feature.FeatureType#isAbstract()}.
     */
    boolean isAbstract();

    /**
     * List of restrictions used define valid values for properties of this
     * property type.
     * <p>
     * Each restriction is a {@link Filter} object in which the property is
     * passed through. If {@link Filter#evaluate(Object)} returns <code>true</code>
     * the restriction is met. If <code>false</code> is returned then the
     * restriction has not been met and the property should be considered invalid.
     * Remember to check getSuper().getRestrictions() as well.
     * <p>
     * This method returns an empty set in the case of no restrictions and should
     * not return <code>null</code>.
     * </p>
     * @return {@code List<Restriction>} used to validate allowable values.
     *
     * @deprecated Defined by {@code valueDomain} in ISO 19109, but not yet implemented.
     */
    List<Filter> getRestrictions();

    /**
     * A map of "user data" which enables applications to store "application-specific"
     * information against a property type.
     * <p>
     * As an example, consider an application that builds a PropertyType from an
     * xml schema. A useful bit of information to attach to the PropertyType is
     * the original schema itself, in whatever construct it might be stored in:
     * <pre>
     * <code>
     * XSDComplexTypeDefinition complexTypeDef = ...;
     * PropertyType type = buildPropertyType( complexTypeDef );
     *
     * type.getUserData().put( XSDComplexTypeDefintion.class, complexTypeDef );
     * </code>
     * </pre>
     * </p>
     *
     * @return A map of user data.
     */
    Map<Object,Object> getUserData();

    /**
     * Equality based on property {@link #getName()}.
     * </p>
     *
     * @return <code>true</code> if other is a PropertyType with the same name
     */
    @Override
    boolean equals(Object other);

    /**
     * Hash code override based on {@link #getName()}.
     *
     * @return getName().hashCode()
     */
    @Override
    int hashCode();
}
