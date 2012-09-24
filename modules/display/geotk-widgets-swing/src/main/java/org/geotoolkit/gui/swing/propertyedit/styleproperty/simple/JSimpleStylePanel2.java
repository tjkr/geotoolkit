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

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import java.awt.BorderLayout;
import java.awt.Component;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.xml.bind.JAXBException;
import org.geotoolkit.gui.swing.misc.EmptyCellRenderer;
import org.geotoolkit.gui.swing.misc.JOptionDialog;
import org.geotoolkit.gui.swing.propertyedit.PropertyPane;
import org.geotoolkit.gui.swing.propertyedit.styleproperty.JSLDImportExportPanel;
import org.geotoolkit.gui.swing.resource.IconBundle;
import org.geotoolkit.gui.swing.style.JBankPanel;
import org.geotoolkit.gui.swing.style.JPreview;
import org.geotoolkit.gui.swing.style.StyleElementEditor;
import org.geotoolkit.map.CoverageMapLayer;
import org.geotoolkit.map.FeatureMapLayer;
import org.geotoolkit.map.MapLayer;
import org.geotoolkit.sld.xml.Specification;
import org.geotoolkit.sld.xml.XMLUtilities;
import org.geotoolkit.style.DefaultStyleFactory;
import org.geotoolkit.style.MutableStyle;
import org.geotoolkit.style.MutableStyleFactory;
import org.geotoolkit.util.logging.Logging;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.style.FeatureTypeStyle;
import org.opengis.style.Fill;
import org.opengis.style.LineSymbolizer;
import org.opengis.style.PointSymbolizer;
import org.opengis.style.PolygonSymbolizer;
import org.opengis.style.RasterSymbolizer;
import org.opengis.style.Rule;
import org.opengis.style.Stroke;
import org.opengis.style.Symbolizer;
import org.opengis.style.TextSymbolizer;

/**
 * Frame of simple style editor
 * @author Fabien Rétif
 */
public class JSimpleStylePanel2 extends StyleElementEditor implements PropertyPane{
    
    public DefaultListModel symbolizerModel = new DefaultListModel();
    public DefaultComboBoxModel symbolizerTypeModel = new DefaultComboBoxModel();
    private MapLayer layer = null;
    private MutableStyle style = null;
    private JBankPanel bankController = new JBankPanel();    
    private PropertyChangeListener listenerPane;
    

    /**
     * Creates new form jStylePane
     */
    public JSimpleStylePanel2() {
        super(Object.class);
        initComponents();

        this.listenerPane = new PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {

                if (evt.getNewValue() instanceof Symbolizer) {
                    
                    guiOverviewLabel.parse(evt.getNewValue());
                    if (guiList.getSelectedValue() != null && guiList.getSelectedValue().getClass().isInstance(evt.getNewValue())) {
                        symbolizerModel.set(symbolizerModel.indexOf(guiList.getSelectedValue()), evt.getNewValue());
                        guiList.updateUI();
                    }
                }

            }
        };

       
        // Set models       
        guiList.setModel(symbolizerModel);

        guiList.setCellRenderer(new JSimpleStylePanel2.SymbolizerRenderer());

        guiOverviewLabel.setMir(true);
    }   
     

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jPanel3 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        jAddButton = new javax.swing.JButton();
        jRemoveButton = new javax.swing.JButton();
        jUpButton = new javax.swing.JButton();
        jDownButton = new javax.swing.JButton();
        jSaveButton = new javax.swing.JButton();
        jBankButton = new javax.swing.JButton();
        jLabel7 = new javax.swing.JLabel();
        jScrollPropPane = new javax.swing.JScrollPane();
        pan_info = new javax.swing.JPanel();
        jPanel9 = new javax.swing.JPanel();
        guiOverviewLabel = new org.geotoolkit.gui.swing.style.JPreview();
        jScrollPane1 = new javax.swing.JScrollPane();
        guiList = new javax.swing.JList();

        setMinimumSize(new java.awt.Dimension(820, 400));
        setSize(new java.awt.Dimension(820, 400));
        setLayout(new java.awt.BorderLayout());

        jLabel6.setFont(new java.awt.Font("Lucida Grande", 1, 13)); // NOI18N
        jLabel6.setText("Edition du symbole");

        jLabel5.setFont(new java.awt.Font("Lucida Grande", 1, 13)); // NOI18N
        jLabel5.setText("Couche de symboles");

        jPanel4.setLayout(new java.awt.GridBagLayout());

        jAddButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/geotoolkit/gui/swing/resource/icon/crystalproject/16x16/actions/edit_add.png"))); // NOI18N
        jAddButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jAddButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.ipadx = 40;
        jPanel4.add(jAddButton, gridBagConstraints);

        jRemoveButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/geotoolkit/gui/swing/resource/icon/crystalproject/16x16/actions/edit_remove.png"))); // NOI18N
        jRemoveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRemoveButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        jPanel4.add(jRemoveButton, gridBagConstraints);

        jUpButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/geotoolkit/gui/swing/resource/icon/crystalproject/16x16/actions/1downarrow.png"))); // NOI18N
        jUpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jUpButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.ipadx = 40;
        jPanel4.add(jUpButton, gridBagConstraints);

        jDownButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/geotoolkit/gui/swing/resource/icon/crystalproject/16x16/actions/1uparrow.png"))); // NOI18N
        jDownButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jDownButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        jPanel4.add(jDownButton, gridBagConstraints);

        jSaveButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/geotoolkit/gui/swing/resource/icon/crystalproject/16x16/actions/filesave.png"))); // NOI18N
        jSaveButton.setText("Sauvegarder le style");
        jSaveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jSaveButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        jPanel4.add(jSaveButton, gridBagConstraints);

        jBankButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/geotoolkit/gui/swing/resource/icon/crystalproject/16x16/actions/tools.png"))); // NOI18N
        jBankButton.setText("Gestionnaire de style");
        jBankButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBankButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        jPanel4.add(jBankButton, gridBagConstraints);

        jLabel7.setFont(new java.awt.Font("Lucida Grande", 1, 13)); // NOI18N
        jLabel7.setText("Aperçu du symbole");

        pan_info.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        pan_info.setLayout(new java.awt.BorderLayout());
        jScrollPropPane.setViewportView(pan_info);

        jPanel9.setBackground(new java.awt.Color(255, 255, 255));
        jPanel9.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 1, true));
        jPanel9.setLayout(new java.awt.BorderLayout(2, 2));

        javax.swing.GroupLayout guiOverviewLabelLayout = new javax.swing.GroupLayout(guiOverviewLabel);
        guiOverviewLabel.setLayout(guiOverviewLabelLayout);
        guiOverviewLabelLayout.setHorizontalGroup(
            guiOverviewLabelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 378, Short.MAX_VALUE)
        );
        guiOverviewLabelLayout.setVerticalGroup(
            guiOverviewLabelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 86, Short.MAX_VALUE)
        );

        jPanel9.add(guiOverviewLabel, java.awt.BorderLayout.CENTER);

        guiList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        guiList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                guiListValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(guiList);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel5)
                    .addComponent(jLabel7)
                    .addComponent(jScrollPane1)
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel9, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPropPane)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jLabel6)
                        .addGap(0, 292, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(17, 17, 17)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(jLabel6))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 153, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel7)
                        .addGap(18, 18, 18)
                        .addComponent(jPanel9, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jScrollPropPane))
                .addContainerGap())
        );

        add(jPanel3, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    

    @Override
    public boolean canHandle(Object target) {
        return target instanceof MapLayer;
    }

    @Override
    public void setTarget(Object target) {
        
         if(target instanceof MapLayer){
            this.layer = (MapLayer) target;
            parse(layer);
        }
        
    }

    @Override
    public void apply() {
         if(layer != null){
            MutableStyleFactory SF = new DefaultStyleFactory();
            
            List<Symbolizer> symbols = new ArrayList<Symbolizer>();
            for (Object o : symbolizerModel.toArray()) {
                symbols.add((Symbolizer)o);
            }
            
            Symbolizer[] array = symbols.toArray(new Symbolizer[symbols.size()]);

            layer.setStyle(SF.style(array));
            

        }
       
    }

    @Override
    public void reset() {
       
    }

    @Override
    public String getTitle() {
      return "Simple2";
    }

    @Override
    public ImageIcon getIcon() {
        return IconBundle.getIcon("16_simple_style");
    }

    @Override
    public String getToolTip() {
        return "";
    }

    @Override
    public Component getComponent() {
        return this;
    }
    
     @Override
    public void setLayer(final MapLayer layer) {
        this.layer = layer;       
        
    }

    @Override
    public MapLayer getLayer() {
        return layer;
    }

    @Override
    public void parse(final Object obj) {
       
        if(layer instanceof FeatureMapLayer){
            final FeatureMapLayer featureLayer = (FeatureMapLayer) layer;
            final GeometryDescriptor geomDesc = featureLayer.getCollection().getFeatureType().getGeometryDescriptor();
            final Class val = (geomDesc != null) ? geomDesc.getType().getBinding() : null;

            if (val!= null && (Polygon.class.isAssignableFrom(val) || MultiPolygon.class.isAssignableFrom(val)) ) {

                loop:
                for(final FeatureTypeStyle fts : layer.getStyle().featureTypeStyles()){
                    for(final Rule rule : fts.rules()){
                        for(final Symbolizer symbol : rule.symbolizers()){
                            if(symbol instanceof PolygonSymbolizer){                                
                                symbolizerModel.addElement((PolygonSymbolizer) symbol);
                                break loop;
                            }
                        }
                    }
                }
               
            } else if (val != null && (MultiLineString.class.isAssignableFrom(val) || LineString.class.isAssignableFrom(val))) {                

                loop:
                for(final FeatureTypeStyle fts : layer.getStyle().featureTypeStyles()){
                    for(final Rule rule : fts.rules()){
                        for(final Symbolizer symbol : rule.symbolizers()){
                            if(symbol instanceof LineSymbolizer){
                                symbolizerModel.addElement((LineSymbolizer) symbol);
                                break loop;
                            }
                        }
                    }
                }
              
            } else if (val != null && (Point.class.isAssignableFrom(val) || MultiPoint.class.isAssignableFrom(val))) {               

                loop:
                for(final FeatureTypeStyle fts : layer.getStyle().featureTypeStyles()){
                    for(final Rule rule : fts.rules()){
                        for(final Symbolizer symbol : rule.symbolizers()){
                            if(symbol instanceof PointSymbolizer){
                                symbolizerModel.addElement((PointSymbolizer) symbol);
                                break loop;
                            }
                        }
                    }
                }

               
            } 

        }else if(layer instanceof CoverageMapLayer){         

            loop:
            for(final FeatureTypeStyle fts : layer.getStyle().featureTypeStyles()){
                for(final Rule rule : fts.rules()){
                    for(final Symbolizer symbol : rule.symbolizers()){
                        if(symbol instanceof RasterSymbolizer){
                            symbolizerModel.addElement((RasterSymbolizer) symbol);
                            break loop;
                        }
                    }
                }
            }

           
        }

        if(layer != null){
            loop:
            for(final FeatureTypeStyle fts : layer.getStyle().featureTypeStyles()){
                for(final Rule rule : fts.rules()){
                    for(final Symbolizer symbol : rule.symbolizers()){
                        if(symbol instanceof TextSymbolizer){
                            symbolizerModel.addElement((TextSymbolizer) symbol);                            
                            break loop;
                        }
                    }
                }
            }
        }

        guiList.updateUI();
    }

    @Override
    public Object create() {
        return style;
    }

   
    
    private void swapElements(int pos1, int pos2) throws ArrayIndexOutOfBoundsException{
        
        Symbolizer symbol = (Symbolizer) symbolizerModel.get(pos1);
        symbolizerModel.set(pos1, symbolizerModel.get(pos2));
        symbolizerModel.set(pos2, symbol);
        
    }

    private void jRemoveButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRemoveButtonActionPerformed
        // TODO add your handling code here:
        try {
            int indexOfSelected = guiList.getSelectedIndex();
            symbolizerModel.remove(indexOfSelected);
            guiList.setSelectedIndex(guiList.getFirstVisibleIndex());
            guiList.updateUI();
        } catch (ArrayIndexOutOfBoundsException ex) {
            //Do nothing because it's probably due to index of the list 
        }
    }//GEN-LAST:event_jRemoveButtonActionPerformed

    private void jUpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jUpButtonActionPerformed
        // TODO add your handling code here:
        try {
            int indexOfSelected = guiList.getSelectedIndex();
            swapElements(indexOfSelected, indexOfSelected - 1);
            indexOfSelected = indexOfSelected - 1;
            guiList.setSelectedIndex(indexOfSelected);
            guiList.updateUI();
        } catch (ArrayIndexOutOfBoundsException ex) {
            //Do nothing because it's probably due to index of the list 
        }
    }//GEN-LAST:event_jUpButtonActionPerformed

    private void jDownButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jDownButtonActionPerformed
        // TODO add your handling code here:
        try {
            int indexOfSelected = guiList.getSelectedIndex();
            swapElements(indexOfSelected, indexOfSelected + 1);
            indexOfSelected = indexOfSelected + 1;
            guiList.setSelectedIndex(indexOfSelected);
            guiList.updateUI();
        } catch (ArrayIndexOutOfBoundsException ex) {
            //Do nothing because it's probably due to index of the list 
        }
    }//GEN-LAST:event_jDownButtonActionPerformed

    private void guiListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_guiListValueChanged
        // TODO add your handling code here:
        pan_info.removeAll();

        if (!evt.getValueIsAdjusting()) {

            JList list = (JList) evt.getSource();

            // Get all selected items
            Object[] selected = list.getSelectedValues();

            // gets only the first selected
            if (selected.length > 0) {
                Object item = selected[0];


                if (item != null) {
                    if (item instanceof PointSymbolizer) {
                        JPointSymbolizerPane pane = new JPointSymbolizerPane();
                        pane.setLayer(layer);
                        pane.addPropertyChangeListener(listenerPane);

                        pane.parse((PointSymbolizer) item);
                        pan_info.add("Center", pane);
                    } else if (item instanceof LineSymbolizer) {
                        JLineSymbolizerPane pane = new JLineSymbolizerPane();
                        pane.setLayer(layer);
                        pane.addPropertyChangeListener(listenerPane);
                        pane.parse((LineSymbolizer) item);
                        pan_info.add("Center", pane);
                    } else if (item instanceof PolygonSymbolizer) {
                        JPolygonSymbolizerPane pane = new JPolygonSymbolizerPane();
                        pane.setLayer(layer);
                         pane.addPropertyChangeListener(listenerPane);
                        pane.parse((PolygonSymbolizer) item);
                        pan_info.add("Center", pane);
                    } else if (item instanceof TextSymbolizer) {
                        JTextSymbolizerPane pane = new JTextSymbolizerPane();
                        pane.setLayer(layer);
                        pane.addPropertyChangeListener(listenerPane);
                        pane.parse((TextSymbolizer) item);
                        pan_info.add("Center", pane);
                    } else if (item instanceof RasterSymbolizer) {

                        pan_info.add("Center", new JRasterSymbolizerPane());
                    }

                    guiOverviewLabel.parse(item);
                }

            }
        }
    }//GEN-LAST:event_guiListValueChanged

    private void jAddButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jAddButtonActionPerformed
        // TODO add your handling code here: 

        List<Class> clazz = new ArrayList<Class>();
        clazz.add(Symbolizer.class);
        clazz.add(PointSymbolizer.class);
        clazz.add(LineSymbolizer.class);
        clazz.add(PolygonSymbolizer.class);
        clazz.add(TextSymbolizer.class);
        clazz.add(RasterSymbolizer.class);
        
        bankController.setClazzList(clazz);

        int result = JOptionDialog.show(bankController);

        if (result == JOptionPane.OK_OPTION) {

            if (bankController.getSelectedSymbol() != null) {
                symbolizerModel.addElement(bankController.getSelectedSymbol());
                guiList.setSelectedValue(bankController.getSelectedSymbol(), true);
                guiList.updateUI();
            }

        }

    }//GEN-LAST:event_jAddButtonActionPerformed

    private void jBankButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBankButtonActionPerformed
        // TODO add your handling code here:
        
        List<Class> clazz = new ArrayList<Class>();
        clazz.add(Symbolizer.class);
        clazz.add(PointSymbolizer.class);
        clazz.add(LineSymbolizer.class);
        clazz.add(PolygonSymbolizer.class);
        clazz.add(TextSymbolizer.class);
        clazz.add(RasterSymbolizer.class);
        clazz.add(Fill.class);
        clazz.add(Stroke.class);        
        
        bankController.setClazzList(clazz);
        
        int result = JOptionDialog.show(bankController);
       
    }//GEN-LAST:event_jBankButtonActionPerformed

    private void jSaveButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jSaveButtonActionPerformed
        // TODO add your handling code here:
         final Specification.StyledLayerDescriptor version = Specification.StyledLayerDescriptor.V_1_1_0;
        if(layer != null && layer.getStyle() != null){
            final MutableStyle currentStyle = layer.getStyle();
            final JFileChooser chooser = new JFileChooser();
            final int result = chooser.showSaveDialog(this);

            if(result == JFileChooser.APPROVE_OPTION){
                final XMLUtilities tool = new XMLUtilities();
                try {
                    tool.writeStyle(chooser.getSelectedFile(), currentStyle, version);
                } catch (JAXBException ex) {
                    Logging.getLogger(JSLDImportExportPanel.class).log(Level.WARNING,ex.getMessage(),ex);
                }
            }
        }
    }//GEN-LAST:event_jSaveButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JList guiList;
    private org.geotoolkit.gui.swing.style.JPreview guiOverviewLabel;
    private javax.swing.JButton jAddButton;
    private javax.swing.JButton jBankButton;
    private javax.swing.JButton jDownButton;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JButton jRemoveButton;
    private javax.swing.JButton jSaveButton;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPropPane;
    private javax.swing.JButton jUpButton;
    private javax.swing.JPanel pan_info;
    // End of variables declaration//GEN-END:variables

    private static class SymbolizerRenderer extends DefaultListCellRenderer {

        private final JPreview preview = new JPreview();

        @Override
        public Component getListCellRendererComponent(JList jlist, Object o, int i, boolean bln, boolean bln1) {
            final JLabel lbl = (JLabel) super.getListCellRendererComponent(jlist, o, i, bln, bln1);

            final JPanel pane = new JPanel(new BorderLayout());
            pane.add(BorderLayout.CENTER, lbl);
            pane.add(BorderLayout.WEST, preview);
            pane.setOpaque(false);
            EmptyCellRenderer.mimicStyle(lbl, preview);
            
            if (o instanceof Symbolizer) {
                Symbolizer symbol = (Symbolizer) o;
                preview.parse(symbol);
                if (symbol.getName() != null && !symbol.getName().isEmpty()) {
                    lbl.setText(symbol.getName());
                } else if (symbol.getDescription() != null && symbol.getDescription().getTitle() != null && !symbol.getDescription().getTitle().toString().isEmpty()) {
                    lbl.setText(symbol.getDescription().getTitle().toString());
                } else {
                    lbl.setText("Unnamed");
                }

            }

            return pane;
        }
        
    }
}
