/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2008 - 2009, Johann Sorel
 *    (C) 2011 Geomatys
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
package org.geotoolkit.gui.swing.propertyedit.styleproperty.simple;

import java.awt.Color;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JLabel;
import org.geotoolkit.gui.swing.style.JNumberExpressionPane;
import org.geotoolkit.gui.swing.style.JUOMPane;
import org.geotoolkit.gui.swing.style.StyleElementEditor;
import org.geotoolkit.map.MapLayer;
import org.geotoolkit.style.StyleConstants;
import org.opengis.style.LineSymbolizer;

/**
 * This class able to display LineSymbolizer tool pane
 * @author Fabien Rétif
 */
public class JLineSymbolizerPane extends StyleElementEditor<LineSymbolizer>  {

    private MapLayer layer = null;
    
    /** 
     * Creates new form JLineSymbolizerPanel
     */
    public JLineSymbolizerPane() {
        super(LineSymbolizer.class);
        initComponents();
        guiDisplacementX.setModel(0d, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 1d);
        guiDisplacementX.setExpressionUnvisible();
    }
    
    /**
     * {@inheritDoc }
     */
    @Override
    public void setLayer(final MapLayer layer){
        this.layer = layer;
        guiStrokeControlPane.setLayer(layer);
        guiDisplacementX.setLayer(layer);
        guiUOM.setLayer(layer);

    }
    
    /**
     * {@inheritDoc }
     */
    @Override
    public MapLayer getLayer(){
        return layer;
    }
 
    /**
     * {@inheritDoc }
     */
    @Override
    public void parse(final LineSymbolizer symbol) {
        
        if (symbol != null) {  
            
            guiStrokeControlPane.parse(symbol.getStroke());
            guiDisplacementX.parse(symbol.getPerpendicularOffset());            
//            guiGeom.setGeom(symbol.getGeometryPropertyName());          
            guiUOM.parse(symbol.getUnitOfMeasure());
    }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public LineSymbolizer create(){
        return getStyleFactory().lineSymbolizer(
                    "lineSymbolizer",
                    (String)null,
                    StyleConstants.DEFAULT_DESCRIPTION,
                    guiUOM.create(),
                    guiStrokeControlPane.create(), 
                    guiDisplacementX.create());
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new JLabel();
        jLabel5 = new JLabel();
        guiDisplacementX = new JNumberExpressionPane();
        jLabel6 = new JLabel();
        guiUOM = new JUOMPane();

        setBackground(new Color(204, 204, 204));

        jLabel1.setText("Forme et couleur de la ligne :");
        add(jLabel1);

        jLabel5.setText("Décallage X :");
        add(jLabel5);

        guiDisplacementX.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                JLineSymbolizerPane.this.propertyChange(evt);
            }
        });
        add(guiDisplacementX);

        jLabel6.setText("Unité :");
        add(jLabel6);
        add(guiUOM);
    }// </editor-fold>//GEN-END:initComponents

    private void propertyChange(PropertyChangeEvent evt) {//GEN-FIRST:event_propertyChange
        // TODO add your handling code here:
        if (PROPERTY_TARGET.equalsIgnoreCase(evt.getPropertyName())) {
            firePropertyChange(PROPERTY_TARGET, null, create());
        }
    }//GEN-LAST:event_propertyChange

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JNumberExpressionPane guiDisplacementX;
    private JUOMPane guiUOM;
    private JLabel jLabel1;
    private JLabel jLabel5;
    private JLabel jLabel6;
    // End of variables declaration//GEN-END:variables

    public void componentHidden(ComponentEvent e) {
        System.out.println(e.getComponent().getClass().getName() + " --- Hidden");
    }

}
