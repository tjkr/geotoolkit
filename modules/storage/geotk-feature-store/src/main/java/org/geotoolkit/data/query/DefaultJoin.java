/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2010, Geomatys
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

package org.geotoolkit.data.query;

import static org.apache.sis.util.ArgumentChecks.*;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.PropertyName;

/**
 * Default join implementation.
 * 
 * @author Johann Sorel (Geomatys)
 * @module pending
 */
public final class DefaultJoin implements Join{

    private final Source left;
    private final Source right;
    private final JoinType type;
    private final PropertyIsEqualTo condition;

    public DefaultJoin(final Source left, final Source right, final JoinType type, final PropertyIsEqualTo condition){
        ensureNonNull("join left source", left);
        ensureNonNull("join right source", right);
        ensureNonNull("join type", type);
        ensureNonNull("join condition", condition);

        //ensure that we have two property names only.
        final Expression expLeft = condition.getExpression1();
        final Expression expRight = condition.getExpression2();
        if(!(expLeft instanceof PropertyName) || !(expRight instanceof PropertyName)){
            throw new IllegalArgumentException("Join condition must be composed of two property names.\n " +
                    "Left expression is : " + expLeft + "   Right expression is : " + expRight);
        }

        this.left = left;
        this.right = right;
        this.type = type;
        this.condition = condition;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public PropertyIsEqualTo getJoinCondition() {
        return condition;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public JoinType getJoinType() {
        return type;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Source getLeft() {
        return left;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Source getRight() {
        return right;
    }

}
