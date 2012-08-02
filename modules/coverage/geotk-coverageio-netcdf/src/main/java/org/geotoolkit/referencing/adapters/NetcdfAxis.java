/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2010-2012, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2010-2012, Geomatys
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
package org.geotoolkit.referencing.adapters;

import java.util.List;
import javax.measure.unit.Unit;

import ucar.nc2.Dimension;
import ucar.nc2.constants.CF;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateAxis2D;

import org.opengis.util.InternationalString;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.cs.RangeMeaning;
import org.opengis.referencing.operation.TransformException;

import org.geotoolkit.util.Strings;
import org.geotoolkit.util.SimpleInternationalString;
import org.geotoolkit.measure.Units;

import org.geotoolkit.resources.Errors;
import static org.geotoolkit.util.ArgumentChecks.ensureNonNull;


/**
 * Wraps a NetCDF {@link CoordinateAxis} as an implementation of GeoAPI interfaces.
 * <p>
 * {@code NetcdfAxis} is a <cite>view</cite>: every methods in this class delegate their work to the
 * wrapped NetCDF axis. Consequently any change in the wrapped axis is immediately reflected in this
 * {@code NetcdfAxis} instance. However users are encouraged to not change the wrapped axis after
 * construction, since GeoAPI referencing objects are expected to be immutable.
 *
 * @author Martin Desruisseaux (Geomatys)
 * @version 3.20
 *
 * @since 3.08
 * @module
 */
public class NetcdfAxis extends NetcdfIdentifiedObject implements CoordinateSystemAxis {
    /**
     * The NetCDF coordinate axis wrapped by this {@code NetcdfAxis} instance.
     */
    final CoordinateAxis axis;

    /**
     * The unit, computed when first needed.
     */
    volatile Unit<?> unit;

    /**
     * Creates a new {@code NetcdfAxis} object wrapping the given NetCDF coordinate axis.
     *
     * @param axis The NetCDF coordinate axis to wrap.
     */
    public NetcdfAxis(final CoordinateAxis axis) {
        ensureNonNull("axis", axis);
        this.axis = axis;
    }

    /**
     * Creates a new {@code NetcdfAxis} object wrapping the given NetCDF coordinate axis.
     *
     * @param axis The NetCDF coordinate axis to wrap.
     * @param domain Dimensions of the coordinate system for which we are wrapping an axis, in NetCDF order.
     *        This is typically {@link ucar.nc2.dataset.CoordinateSystem#getDomain()}.
     * @return The {@code NetcdfAxis} object wrapping the given axis.
     */
    static NetcdfAxis wrap(final CoordinateAxis axis, final List<Dimension> domain) {
        if (axis instanceof CoordinateAxis1D) {
            return new NetcdfAxis1D((CoordinateAxis1D) axis, domain);
        }
        if (axis instanceof CoordinateAxis2D) {
            return new NetcdfAxis2D((CoordinateAxis2D) axis, domain);
        }
        return new NetcdfAxis(axis);
    }

    /**
     * Returns the wrapped NetCDF axis.
     */
    @Override
    public CoordinateAxis delegate() {
        return axis;
    }

    /**
     * Returns the axis name. The default implementation delegates to
     * {@link CoordinateAxis1D#getShortName()}.
     *
     * @see CoordinateAxis1D#getShortName()
     */
    @Override
    public String getCode() {
        return axis.getShortName();
    }

    /**
     * Returns the axis abbreviation. The default implementation returns
     * an acronym of the value returned by {@link CoordinateAxis1D#getShortName()}.
     *
     * @see CoordinateAxis1D#getShortName()
     */
    @Override
    public String getAbbreviation() {
        final String name = axis.getShortName().trim();
        if (name.equalsIgnoreCase("longitude")) return "\u03BB";
        if (name.equalsIgnoreCase("latitude"))  return "\u03C6";
        return Strings.camelCaseToAcronym(name).toLowerCase();
    }

    /**
     * Returns the axis direction. The default implementation delegates to
     * {@link #getDirection(CoordinateAxis)}.
     *
     * @see CoordinateAxis1D#getAxisType()
     * @see CoordinateAxis1D#getPositive()
     */
    @Override
    public AxisDirection getDirection() {
        return getDirection(axis);
    }

    /**
     * Returns the direction of the given axis. This method infers the direction from
     * {@link CoordinateAxis#getAxisType()} and {@link CoordinateAxis#getPositive()}.
     * If the direction can not be determined, then this method returns
     * {@link AxisDirection#OTHER}.
     *
     * @param  axis The axis for which to get the direction.
     * @return The direction of the given axis.
     */
    public static AxisDirection getDirection(final CoordinateAxis axis) {
        final AxisType type = axis.getAxisType();
        final boolean down = CF.POSITIVE_DOWN.equals(axis.getPositive());
        if (type != null) {
            switch (type) {
                case Time: return down ? AxisDirection.PAST : AxisDirection.FUTURE;
                case Lon:
                case GeoX: return down ? AxisDirection.WEST : AxisDirection.EAST;
                case Lat:
                case GeoY: return down ? AxisDirection.SOUTH : AxisDirection.NORTH;
                case Pressure:
                case Height:
                case GeoZ: return down ? AxisDirection.DOWN : AxisDirection.UP;
            }
        }
        return AxisDirection.OTHER;
    }

    /**
     * Returns the axis minimal value. The default implementation delegates
     * to {@link CoordinateAxis#getMinValue()}.
     *
     * @see CoordinateAxis#getMinValue()
     */
    @Override
    public double getMinimumValue() {
        return axis.getMinValue();
    }

    /**
     * Returns the axis maximal value. The default implementation delegates
     * to {@link CoordinateAxis#getMaxValue()}.
     *
     * @see CoordinateAxis#getMaxValue()
     */
    @Override
    public double getMaximumValue() {
        return axis.getMaxValue();
    }

    /**
     * Returns {@code null} since the range meaning is unspecified.
     */
    @Override
    public RangeMeaning getRangeMeaning() {
        return null;
    }

    /**
     * Returns {@code true} if the NetCDF axis is an instance of {@link CoordinateAxis1D} and
     * {@linkplain CoordinateAxis1D#isRegular() is regular}.
     *
     * @return {@code true} if the NetCDF axis is regular.
     *
     * @see CoordinateAxis1D#isRegular()
     *
     * @since 3.20
     */
    final boolean isRegular() {
        return (axis instanceof CoordinateAxis1D) && ((CoordinateAxis1D) axis).isRegular();
    }

    /**
     * Interpolates the ordinate value for the given grid coordinate. The {@code gridPts} array
     * shall contains a complete grid coordinate - not only the grid index value for this axis -
     * starting at index {@code srcOff}.
     * <p>
     * The interpolated ordinate value shall maps the
     * {@linkplain org.opengis.referencing.datum.PixelInCell#CELL_CENTER cell center}.
     * <p>
     * The default implementation throws an exception in all cases.
     * The actual implementation needs to be provided by subclasses.
     *
     * @param  gridPts An array containing grid coordinates.
     * @param  srcOff  Index of the first ordinate value in the {@code gridPts}.
     * @return The ordinate value of cell center interpolated from the given grid coordinate.
     * @throws TransformException If the ordinate value can not be computed.
     *
     * @since 3.20
     */
    public double getOrdinateValue(final double[] gridPts, final int srcOff) throws TransformException {
        throw new TransformException(Errors.format(Errors.Keys.UNSPECIFIED_TRANSFORM));
    }

    /**
     * Returns the units as a string. If the axis direction or the time epoch
     * was appended to the units, then this part of the string is removed.
     */
    private String getUnitsString() {
        String symbol = axis.getUnitsString();
        if (symbol != null) {
            int i = symbol.lastIndexOf('_');
            if (i > 0) {
                final String direction = getDirection().name();
                if (symbol.regionMatches(true, i+1, direction, 0, direction.length())) {
                    symbol = symbol.substring(0, i).trim();
                }
            }
            i = symbol.indexOf(" since ");
            if (i > 0) {
                symbol = symbol.substring(0, i);
            }
            symbol = symbol.trim();
        }
        return symbol;
    }

    /**
     * Returns the units, or {@code null} if unknown.
     *
     * @see CoordinateAxis1D#getUnitsString()
     * @see Units#valueOf(String)
     */
    @Override
    public Unit<?> getUnit() {
        Unit<?> unit = this.unit;
        if (unit == null) {
            final String symbol = getUnitsString();
            if (symbol != null) try {
                this.unit = unit = Units.valueOf(symbol);
            } catch (IllegalArgumentException e) {
                // TODO: use Unit library in order to parse this kind of units.
                // For now just report that the unit is unknown, which is compatible
                // with the method contract.
            }
        }
        return unit;
    }

    /**
     * Returns the NetCDF description, or {@code null} if none.
     * The default implementation delegates to {@link CoordinateAxis1D#getDescription()}.
     *
     * @see CoordinateAxis1D#getDescription()
     */
    @Override
    public InternationalString getRemarks() {
        final String description = axis.getDescription();
        if (description != null) {
            return new SimpleInternationalString(description);
        }
        return super.getRemarks();
    }
}
