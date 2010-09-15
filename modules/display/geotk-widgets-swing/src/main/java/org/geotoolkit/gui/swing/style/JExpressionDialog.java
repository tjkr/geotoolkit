/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2007 - 2008, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2008 - 2009, Johann Sorel
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
package org.geotoolkit.gui.swing.style;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.geotoolkit.filter.text.cql2.CQL;
import org.geotoolkit.filter.text.cql2.CQLException;
import org.geotoolkit.gui.swing.resource.MessageBundle;
import org.geotoolkit.map.FeatureMapLayer;
import org.geotoolkit.map.MapLayer;

import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.filter.expression.Expression;

/**
 * Expression dialog
 * 
 * @author Johann Sorel
 * @module pending
 */
public class JExpressionDialog extends javax.swing.JDialog {

    private Expression old = null;

    /** 
     * Creates new form JExpressionDialog 
     */
    public JExpressionDialog() {
        this(null, null);
    }

    /**
     * 
     * @param layer the layer to edit
     */
    public JExpressionDialog(MapLayer layer) {
        this(layer, null);
    }

    /**
     * 
     * @param exp the default expression
     */
    public JExpressionDialog(Expression exp) {
        this(null, exp);
    }

    /**
     * 
     * @param layer the layer to edit
     * @param exp the default expression
     */
    public JExpressionDialog(MapLayer layer, Expression exp) {
        initComponents();

        setLayer(layer);
        setExpression(exp);

        guiFields.addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {

                if (guiFields.getSelectedValue() != null) {
                    append(guiFields.getSelectedValue().toString());
                }
            }
        });
    }

    private void append(String val) {
        if (!jta.getText().endsWith(val)) {

            if (!jta.getText().endsWith(" ") && jta.getText().length() > 0) {
                jta.append(" ");
            }
            jta.append(val);
        }
    }

    /**
     * 
     * @param layer the layer to edit
     */
    public void setLayer(MapLayer layer) {
        guiFields.removeAll();

        if (layer instanceof FeatureMapLayer) {

            guiFields.removeAll();

            final Collection<PropertyDescriptor> col = ((FeatureMapLayer)layer).getCollection().getFeatureType().getDescriptors();
            final List<String> vec = new ArrayList<String>();
            
            for(final PropertyDescriptor desc : col){
                vec.add(desc.getName().getLocalPart());
            }

            guiFields.setListData(vec.toArray());
        }

    }

    /**
     * 
     * @param exp the default expression
     */
    public void setExpression(Expression exp) {
        this.old = exp;
        
        if (exp != null) {
            if (exp != Expression.NIL) {
                jta.setText(exp.toString());
            }
        }

    }

    /**
     * 
     * @return Expression : New Expression
     */
    public Expression getExpression() {
//        final FilterFactory ff = FactoryFinder.getFilterFactory(null);
//        return ff.property(jta.getText());
        
        try {
            Expression expr = CQL.toExpression(jta.getText());           
            return expr;
        } catch (CQLException ex) {
            ex.printStackTrace();
            return old;
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        jToolBar1.setFloatable(false);
        jToolBar1.setLayout(new FlowLayout(FlowLayout.RIGHT));
        jToolBar1.setRollover(true);

        jButton6.setText(MessageBundle.getString("apply")); // NOI18N
        jButton6.setFocusable(false);
        jButton6.setHorizontalTextPosition(SwingConstants.CENTER);
        jButton6.setVerticalTextPosition(SwingConstants.BOTTOM);
        jButton6.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                actionClose(evt);
            }
        });
        jToolBar1.add(jButton6);

        getContentPane().add(jToolBar1, BorderLayout.SOUTH);

        jSplitPane1.setDividerLocation(200);

        jScrollPane2.setViewportView(guiFields);

        jSplitPane1.setLeftComponent(jScrollPane2);

        jta.setColumns(20);
        jta.setRows(5);
        jScrollPane1.setViewportView(jta);

        jButton5.setFont(jButton5.getFont().deriveFont(jButton5.getFont().getStyle() | Font.BOLD));
        jButton5.setText("*");
        jButton5.setMargin(new Insets(2, 4, 2, 4));
        jButton5.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                jButton5actionMultiply(evt);
            }
        });

        jButton4.setFont(jButton4.getFont().deriveFont(jButton4.getFont().getStyle() | Font.BOLD));
        jButton4.setText("/");
        jButton4.setMargin(new Insets(2, 4, 2, 4));
        jButton4.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                jButton4actionDivide(evt);
            }
        });

        jButton3.setFont(jButton3.getFont().deriveFont(jButton3.getFont().getStyle() | Font.BOLD));
        jButton3.setText("-");
        jButton3.setMargin(new Insets(2, 4, 2, 4));
        jButton3.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                jButton3actionMinus(evt);
            }
        });

        jButton2.setFont(jButton2.getFont().deriveFont(jButton2.getFont().getStyle() | Font.BOLD));
        jButton2.setText("+");
        jButton2.setMargin(new Insets(2, 4, 2, 4));
        jButton2.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                jButton2actionPlus(evt);
            }
        });

        GroupLayout jPanel2Layout = new GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(jButton2)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(jButton3)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(jButton4)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(jButton5)
                .addContainerGap())
            .addComponent(jScrollPane1, GroupLayout.DEFAULT_SIZE, 261, Short.MAX_VALUE)
        );

        jPanel2Layout.linkSize(SwingConstants.HORIZONTAL, new Component[] {jButton2, jButton3, jButton4, jButton5});

        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(jButton2)
                    .addComponent(jButton3)
                    .addComponent(jButton4)
                    .addComponent(jButton5))
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, GroupLayout.DEFAULT_SIZE, 183, Short.MAX_VALUE))
        );

        jPanel2Layout.linkSize(SwingConstants.VERTICAL, new Component[] {jButton2, jButton3, jButton4, jButton5});

        jSplitPane1.setRightComponent(jPanel2);

        jTabbedPane1.addTab("Expression", jSplitPane1);

        GroupLayout jPanel1Layout = new GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(Alignment.LEADING)
            .addGap(0, 467, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(Alignment.LEADING)
            .addGap(0, 226, Short.MAX_VALUE)
        );

        jTabbedPane1.addTab("Categorize", jPanel1);

        GroupLayout jPanel3Layout = new GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(Alignment.LEADING)
            .addGap(0, 467, Short.MAX_VALUE)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(Alignment.LEADING)
            .addGap(0, 226, Short.MAX_VALUE)
        );

        jTabbedPane1.addTab("Interpolate", jPanel3);

        getContentPane().add(jTabbedPane1, BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents
    private void jButton2actionPlus(ActionEvent evt) {//GEN-FIRST:event_jButton2actionPlus
        append("+");
    }//GEN-LAST:event_jButton2actionPlus

    private void jButton3actionMinus(ActionEvent evt) {//GEN-FIRST:event_jButton3actionMinus
        append("-");
    }//GEN-LAST:event_jButton3actionMinus

    private void jButton4actionDivide(ActionEvent evt) {//GEN-FIRST:event_jButton4actionDivide
        append("/");
    }//GEN-LAST:event_jButton4actionDivide

    private void jButton5actionMultiply(ActionEvent evt) {//GEN-FIRST:event_jButton5actionMultiply
        append("*");
    }//GEN-LAST:event_jButton5actionMultiply

    private void actionClose(ActionEvent evt) {//GEN-FIRST:event_actionClose
        dispose();
    }//GEN-LAST:event_actionClose
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private final JList guiFields = new JList();
    private final JButton jButton2 = new JButton();
    private final JButton jButton3 = new JButton();
    private final JButton jButton4 = new JButton();
    private final JButton jButton5 = new JButton();
    private final JButton jButton6 = new JButton();
    private final JPanel jPanel1 = new JPanel();
    private final JPanel jPanel2 = new JPanel();
    private final JPanel jPanel3 = new JPanel();
    private final JScrollPane jScrollPane1 = new JScrollPane();
    private final JScrollPane jScrollPane2 = new JScrollPane();
    private final JSplitPane jSplitPane1 = new JSplitPane();
    private final JTabbedPane jTabbedPane1 = new JTabbedPane();
    private final JToolBar jToolBar1 = new JToolBar();
    private final JTextArea jta = new JTextArea();
    // End of variables declaration//GEN-END:variables
}
