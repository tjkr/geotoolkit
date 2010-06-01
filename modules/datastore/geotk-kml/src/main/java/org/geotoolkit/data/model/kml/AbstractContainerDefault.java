package org.geotoolkit.data.model.kml;

import java.util.List;
import org.geotoolkit.data.model.atom.AtomPersonConstruct;
import org.geotoolkit.data.model.atom.AtomLink;
import org.geotoolkit.data.model.xal.AddressDetails;
import org.geotoolkit.data.model.xsd.SimpleType;

/**
 *
 * @author Samuel Andrés
 */
public class AbstractContainerDefault extends AbstractFeatureDefault implements AbstractContainer {

    protected List<SimpleType> containerSimpleExtensions;
    protected List<AbstractObject> containerObjectExtensions;

    /**
     *
     * @param objectSimpleExtensions
     * @param idAttributes
     * @param name
     * @param visibility
     * @param open
     * @param author
     * @param atomLink
     * @param address
     * @param addressDetails
     * @param phoneNumber
     * @param snippet
     * @param description
     * @param view
     * @param timePrimitive
     * @param styleUrl
     * @param styleSelector
     * @param region
     * @param extendedData
     * @param abstractFeatureSimpleExtensions
     * @param abstractFeatureObjectExtensions
     * @param abstractContainerSimpleExtensions
     * @param abstractContainerObjectExtensions
     */
    protected AbstractContainerDefault(List<SimpleType> objectSimpleExtensions, IdAttributes idAttributes,
            String name, boolean visibility, boolean open, AtomPersonConstruct author, AtomLink link,
            String address, AddressDetails addressDetails, String phoneNumber, String snippet,
            String description, AbstractView view, AbstractTimePrimitive timePrimitive,
            String styleUrl, List<AbstractStyleSelector> styleSelector,
            Region region, ExtendedData extendedData,
            List<SimpleType> abstractFeatureSimpleExtensions,
            List<AbstractObject> abstractFeatureObjectExtensions,
            List<SimpleType> abstractContainerSimpleExtensions,
            List<AbstractObject> abstractContainerObjectExtensions){

        super(objectSimpleExtensions, idAttributes,
                name, visibility, open, author, link, address, addressDetails,
                phoneNumber, snippet, description, view, timePrimitive, styleUrl, styleSelector, region, extendedData,
                abstractFeatureSimpleExtensions, abstractFeatureObjectExtensions);

        this.containerSimpleExtensions = abstractContainerSimpleExtensions;
        this.containerObjectExtensions = abstractContainerObjectExtensions;
    }

    /**
     * @{@inheritDoc}
     */
    @Override
    public List<SimpleType> getAbstractContainerSimpleExtensions() {return this.containerSimpleExtensions;}

    /**
     * @{@inheritDoc}
     */
    @Override
    public List<AbstractObject> getAbstractContainerObjectExtensions() {return this.featureObjectExtensions;}

    @Override
    public String toString(){
        String resultat = super.toString()+
                "\n\tAbstractContainerDefault : ";
        return resultat;
    }

}
