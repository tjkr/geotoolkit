/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2009-2012, Geomatys
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
package org.geotoolkit.index.tree.basic;

import java.util.ArrayList;
import java.util.List;
import org.geotoolkit.geometry.GeneralDirectPosition;
import org.geotoolkit.geometry.GeneralEnvelope;
import org.geotoolkit.index.tree.AbstractTree;
import org.geotoolkit.index.tree.DefaultNodeFactory;
import org.geotoolkit.index.tree.DefaultTreeUtils;
import org.geotoolkit.index.tree.Node;
import org.geotoolkit.index.tree.Tree;
import org.geotoolkit.index.tree.calculator.Calculator;
import static org.geotoolkit.index.tree.DefaultTreeUtils.*;
import static org.geotoolkit.index.tree.io.TVR.*;
import org.geotoolkit.index.tree.io.TreeVisitor;
import org.geotoolkit.index.tree.io.TreeVisitorResult;
import org.geotoolkit.index.tree.NodeFactory;
import org.geotoolkit.referencing.CRS;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.internal.util.UnmodifiableArrayList;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.MismatchedReferenceSystemException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Create R-Tree (Basic)
 *
 * @author Rémi Marechal (Geomatys)
 * @author Johann Sorel (Geomatys)
 */
public class BasicRTree extends AbstractTree {

    private SplitCase choice;

    /**
     * Create a Basic R-Tree using default node factory.
     *
     * @param maxElements           : max elements number within each tree Node.
     * @param crs                   : associate coordinate system.
     * @param choice                : made to split "linear" or "quadratic".
     * @return Basic RTree.
     */
    public BasicRTree(final int maxElements, CoordinateReferenceSystem crs, final SplitCase choice) {
        this(maxElements, crs, choice, DefaultNodeFactory.INSTANCE);
    }

    /**
     * Create a Basic R-Tree.
     *
     * @param maxElements           : max elements number within each tree Node.
     * @param crs                   : associate coordinate system.
     * @param choice                : made to split "linear" or "quadratic".
     * @param nodefactory           : made to create tree {@code Node}.
     * @return Basic RTree.
     */
    public BasicRTree(final int maxElements, CoordinateReferenceSystem crs, final SplitCase choice, NodeFactory nodefactory) {
        super(maxElements, crs, nodefactory);
        this.choice = choice;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void search(final Envelope regionSearch, final TreeVisitor visitor) throws IllegalArgumentException {
        ArgumentChecks.ensureNonNull("search : region search", regionSearch);
        ArgumentChecks.ensureNonNull("search : result", visitor);
        if (!CRS.equalsIgnoreMetadata(crs, regionSearch.getCoordinateReferenceSystem())) throw new MismatchedReferenceSystemException();
        final Node root = getRoot();
        if (root != null) nodeSearch(root, regionSearch, visitor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void insert(final Envelope entry) throws IllegalArgumentException {
        super.insert(entry);
        if (!CRS.equalsIgnoreMetadata(crs, entry.getCoordinateReferenceSystem())) throw new MismatchedReferenceSystemException();
        super.eltCompteur++;
        final Node root       = getRoot();
        final int dim         = entry.getDimension();
        final double[] coords = new double[2 * dim];
        System.arraycopy(entry.getLowerCorner().getCoordinate(), 0, coords, 0, dim);
        System.arraycopy(entry.getUpperCorner().getCoordinate(), 0, coords, dim, dim);
        if (root == null || root.isEmpty()) {
            setRoot(createNode(this, null, null, UnmodifiableArrayList.wrap(entry), coords));
        } else {
            nodeInsert(root, entry);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean delete(final Envelope entry) throws IllegalArgumentException {
        ArgumentChecks.ensureNonNull("delete : entry", entry);
        if (!CRS.equalsIgnoreMetadata(crs, entry.getCoordinateReferenceSystem())) throw new MismatchedReferenceSystemException();
        final Node root = getRoot();
        if (root != null) return deleteNode(root, entry);
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean remove(final Envelope entry) throws IllegalArgumentException {
        ArgumentChecks.ensureNonNull("remove : entry", entry);
        if (!CRS.equalsIgnoreMetadata(crs, entry.getCoordinateReferenceSystem())) throw new MismatchedReferenceSystemException();
        final Node root = getRoot();
        if (root != null) return removeNode(root, entry);
        return false;
    }

    /**
     * @return split case chosen to split.
     */
    public SplitCase getSplitCase() {
        return this.choice;
    }

    /**Find all {@code Envelope} which intersect regionSearch parameter in {@code Node}.
     *
     * @param candidate current Node
     * @param regionSearch area of search.
     * @param result {@code List} where is add search resulting.
     */
    private static TreeVisitorResult nodeSearch(final Node candidate, final Envelope regionSearch, final TreeVisitor visitor){
        final TreeVisitorResult tvr = visitor.filter(candidate);
        if (isTerminate(tvr)) return tvr;
        final Envelope bound = candidate.getBoundary();
        if (bound != null) {
            if (regionSearch == null) {
                if (candidate.isLeaf()) {
                    for(Envelope env : candidate.getEntries()){
                        final TreeVisitorResult tvrTemp = visitor.visit(env);
                        if (isTerminate(tvrTemp))   return tvrTemp;
                        if (isSkipSibling(tvrTemp)) break;
                    }
                }else{
                    if (!isSkipSubTree(tvr)) {
                        for (Node nod : candidate.getChildren()) {
                            final TreeVisitorResult tvrTemp = nodeSearch(nod, null, visitor);//filter
                            if (isTerminate(tvrTemp))   return tvrTemp;
                            if (isSkipSibling(tvrTemp)) break;
                        }
                    }
                }
            }else{
                final GeneralEnvelope rS = new GeneralEnvelope(regionSearch);
                if (rS.contains(bound, true)) {
                    nodeSearch(candidate, null, visitor);
                }else if (rS.intersects(bound, true)) {
                    if (candidate.isLeaf()) {
                        for (Envelope gn : candidate.getEntries()) {
                            TreeVisitorResult tvrTemp = null;
                            if (rS.intersects(gn, true)) {
                                tvrTemp = visitor.visit(gn);
                            }
                            if (tvrTemp != null) {
                                if (isTerminate(tvrTemp))   return tvrTemp;
                                if (isSkipSibling(tvrTemp)) break;
                            }
                        }
                    } else {
                        if (!isSkipSubTree(tvr)) {
                            for (Node child : candidate.getChildren()) {
                                final TreeVisitorResult tvrTemp = nodeSearch(child, regionSearch, visitor);
                                if (isTerminate(tvrTemp))   return tvrTemp;
                                if (isSkipSibling(tvrTemp)) break;
                            }
                        }
                    }
                }
            }
            return tvr;
        }
        return TreeVisitorResult.TERMINATE;
    }

    /**Insert new {@code Envelope} in branch and re-organize {@code Node} if it's necessary.
     *
     * <blockquote><font size=-1>
     * <strong>NOTE: insertion is in accordance with R*Tree properties.</strong>
     * </font></blockquote>
     *
     * @param candidate where to insert entry.
     * @param entry to add.
     * @throws IllegalArgumentException if {@code Node} candidate is null.
     * @throws IllegalArgumentException if {@code Envelope} entry is null.
     */
    private static void nodeInsert(final Node candidate, final Envelope entry) throws IllegalArgumentException{
        if (candidate.isLeaf()) {
            candidate.getEntries().add(entry);
        }else{
            nodeInsert(chooseSubtree(candidate, entry), entry);
        }

        if (!candidate.isLeaf()) {
            final List<Node> children = candidate.getChildren();
            int size = children.size();
            if (children.get(0).isLeaf()) {
                for (int i = 0; i<size-1; i++) {
                    for (int j = i+1; j<size; j++) {
                        final Node nodeA = children.get(i);
                        final Node nodeB = children.get(j);
                        if (new GeneralEnvelope(nodeA.getBoundary()).intersects(nodeB.getBoundary(), false)
                                && nodeA.isLeaf() && nodeB.isLeaf()
                                && nodeA.getEntries().size() > 1 && nodeB.getEntries().size() > 1){
                            branchGrafting(nodeA, nodeB);
                        }
                    }
                }
            }

            for (int i =0; i<size; i++) {
                if (DefaultTreeUtils.countElements(children.get(i)) > candidate.getTree().getMaxElements()) {
                    final Node child = children.remove(i);
                    final List<Node> l = splitNode(child);
                    final Node l0 = l.get(0);
                    final Node l1 = l.get(1);
                    l0.setParent(candidate);
                    l1.setParent(candidate);
                    children.addAll(l);
                }
            }
        }

        if (candidate.getParent() == null) {
            if (DefaultTreeUtils.countElements(candidate) > candidate.getTree().getMaxElements()) {
                List<Node> l = splitNode(candidate);
                final Node l0 = l.get(0);
                final Node l1 = l.get(1);
                l0.setParent(candidate);
                l1.setParent(candidate);
                candidate.getEntries().clear();
                candidate.getChildren().clear();
                candidate.getChildren().addAll(l);
            }
        }
    }

    /**
     * Exchange some entry(ies) between two nodes in aim to find best form with lesser overlaps.
     * Also branchGrafting will be able to avoid splitting node.
     *
     * @param nodeA Node
     * @param nodeB Node
     * @throws IllegalArgumentException if nodeA or nodeB are null.
     * @throws IllegalArgumentException if nodeA and nodeB have different "parent".
     * @throws IllegalArgumentException if nodeA or nodeB are not tree leaf.
     * @throws IllegalArgumentException if nodeA or nodeB, and their subnodes, don't contains some {@code Entry}.
     */
    private static void branchGrafting(final Node nodeA, final Node nodeB ) throws IllegalArgumentException {
        if(!nodeA.isLeaf() || !nodeB.isLeaf()) throw new IllegalArgumentException("branchGrafting : not leaf");
        List<Envelope> listGlobale = new ArrayList<Envelope>(nodeA.getEntries());
        listGlobale.addAll(new ArrayList<Envelope>(nodeB.getEntries()));
        nodeA.getEntries().clear();
        nodeB.getEntries().clear();
        if(listGlobale.isEmpty()) throw new IllegalArgumentException("branchGrafting : empty list");
        final AbstractTree tree = (AbstractTree)nodeA.getTree();
        final int size = listGlobale.size();
        final GeneralEnvelope globalE = getEnveloppeMin(listGlobale);
        final int dim = globalE.getDimension();
        double lengthDimRef = -1;
        int indexSplit = -1;
        for(int i = 0; i<dim; i++) {
            double lengthDimTemp = globalE.getSpan(i);
            if(lengthDimTemp>lengthDimRef) {
                lengthDimRef = lengthDimTemp;
                indexSplit = i;
            }
        }
        assert indexSplit != -1 : "BranchGrafting : indexSplit not find"+indexSplit;
        final Calculator calc = tree.getCalculator();
        listGlobale = calc.sortList(indexSplit, true, listGlobale);
        GeneralEnvelope envB;
        final GeneralEnvelope envA = new GeneralEnvelope(listGlobale.get(0));
        double overLapsRef = Double.POSITIVE_INFINITY;
        int index =-1;
        final int size04 = (int)((size * 0.4 >= 1) ? size*0.4 : 1);
        for (int cut = size04; cut < size-size04; cut++) {
            for (int i = 1; i<cut; i++) {
                envA.add(listGlobale.get(i));
            }
            envB = new GeneralEnvelope(listGlobale.get(cut));
            for (int i = cut+1; i<size; i++) {
                envB.add(listGlobale.get(i));
            }
            double overLapsTemp = calc.getOverlaps(envA, envB);
            if (overLapsTemp < overLapsRef) {
                overLapsRef = overLapsTemp;
                index = cut;
            }
        }
        for (int i = 0; i<index; i++) {
            nodeA.getEntries().add(listGlobale.get(i));
        }
        for (int i = index; i<size; i++) {
            nodeB.getEntries().add(listGlobale.get(i));
        }
    }

    /**
     * Split a overflow {@code Node} in accordance with R-Tree properties.
     *
     * @param candidate {@code Node} to Split.
     * @throws IllegalArgumentException if candidate is null.
     * @throws IllegalArgumentException if candidate elements number is lesser 2.
     * @return {@code Node} List which contains two {@code Node} (split result of candidate).
     */
    private static List<Node> splitNode(final Node candidate) throws IllegalArgumentException {
        ArgumentChecks.ensureNonNull("splitNode : candidate", candidate);
        if (DefaultTreeUtils.countElements(candidate) < 2) throw new IllegalArgumentException("not enought elements within " + candidate + " to split.");
        final int dim = candidate.getBoundary().getDimension();
        final Tree tree = candidate.getTree();
        final Calculator calc = tree.getCalculator();
        final int maxElmnts = tree.getMaxElements();
        boolean leaf = candidate.isLeaf();
        List<?> ls;
        Object s1, s2;
        ls = (leaf) ? candidate.getEntries() : candidate.getChildren();
        s1 = ls.get(0);
        s2 = ls.get(1);
        double refValue = 0;
        double tempValue;
        int index1 = 0;
        int index2 = 0;

        switch (((BasicRTree) tree).getSplitCase()) {
            case LINEAR: {
                for (int i = 0; i < ls.size() - 1; i++) {
                    for (int j = i + 1; j < ls.size(); j++) {
                        tempValue = (leaf) ? calc.getDistance(((Envelope) ls.get(i)), ((Envelope) ls.get(j)))
                                           : calc.getDistance(((Node) ls.get(i)).getBoundary(), ((Node) ls.get(j)).getBoundary());
                        if (tempValue > refValue) {
                            s1     = ls.get(i);
                            s2     = ls.get(j);
                            index1 = i;
                            index2 = j;
                            refValue = tempValue;
                        }
                    }
                }
            }
            break;

            case QUADRATIC: {
                Envelope rectGlobal, bound1, bound2;
                for (int i = 0; i < ls.size() - 1; i++) {
                    for (int j = i + 1; j < ls.size(); j++) {
                        if (leaf) {
                            bound1 = ((Envelope) ls.get(i));
                            bound2 = ((Envelope) ls.get(j));
                        } else {
                            bound1 = ((Node) ls.get(i)).getBoundary();
                            bound2 = ((Node) ls.get(j)).getBoundary();
                        }
                        rectGlobal = getEnveloppeMin(UnmodifiableArrayList.wrap(bound1, bound2));
                        tempValue  = calc.getSpace(rectGlobal) - calc.getSpace(bound1) - calc.getSpace(bound2);
                        if (tempValue > refValue) {
                            s1     = ls.get(i);
                            s2     = ls.get(j);
                            index1 = i;
                            index2 = j;
                            refValue = tempValue;
                        }
                    }
                }
            }
            break;
        }

        ls.remove(Math.max(index1, index2));
        ls.remove(Math.min(index1, index2));
        Envelope r1Temp, r2Temp;
        Envelope boundS1, boundS2;
        Node result1, result2;
        boundS1 = (leaf) ? ((Envelope)s1) : ((Node)s1).getBoundary();
        boundS2 = (leaf) ? ((Envelope)s2) : ((Node)s2).getBoundary();
        double[] tabS1 = new double[2*dim];
        double[] tabS2 = new double[2*dim];
        for(int i = 0; i<dim;i++) {
            tabS1[i] = boundS1.getLowerCorner().getOrdinate(i);
            tabS2[i] = boundS2.getLowerCorner().getOrdinate(i);
        }
        for(int i = 0; i<dim;i++) {
            tabS1[i+dim-1] = boundS1.getUpperCorner().getOrdinate(i);
            tabS2[i+dim-1] = boundS2.getUpperCorner().getOrdinate(i);
        }
        result1 = (leaf) ? tree.createNode(tree, null, null, UnmodifiableArrayList.wrap((Envelope) s1), tabS1)
                         : tree.createNode(tree, null, UnmodifiableArrayList.wrap((Node) s1), null, tabS1);
        result2 = (leaf) ? tree.createNode(tree, null, null, UnmodifiableArrayList.wrap((Envelope) s2), tabS2)
                         : tree.createNode(tree, null, UnmodifiableArrayList.wrap((Node) s2), null, tabS2);
        double demimaxE = maxElmnts / 3;
        demimaxE = Math.max(demimaxE, 1);

        for (Object ent : ls) {
            r1Temp = getEnveloppeMin(UnmodifiableArrayList.wrap(s1, ent));
            r2Temp = getEnveloppeMin(UnmodifiableArrayList.wrap(s2, ent));

            double area1 = calc.getSpace(r1Temp);
            double area2 = calc.getSpace(r2Temp);
            int r1nbE = DefaultTreeUtils.countElements(result1);
            int r2nbE = DefaultTreeUtils.countElements(result2);
            if (area1 < area2) {
                if (r1nbE <= demimaxE && r2nbE > demimaxE) {
                    if(leaf){
                        result1.getEntries().add((Envelope)ent);
                    }else{
                        result1.getChildren().add((Node)ent);
                    }

                } else if (r2nbE <= demimaxE && r1nbE > demimaxE) {
                    if(leaf){
                        result2.getEntries().add((Envelope)ent);
                    }else{
                        result2.getChildren().add((Node)ent);
                    }
                } else {
                    if(leaf){
                        result1.getEntries().add((Envelope)ent);
                    }else{
                        result1.getChildren().add((Node)ent);
                    }
                }
            } else if (area1 == area2) {
                if (r1nbE < r2nbE) {
                    if(leaf){
                        result1.getEntries().add((Envelope)ent);
                    }else{
                        result1.getChildren().add((Node)ent);
                    }
                } else {
                    if(leaf){
                        result2.getEntries().add((Envelope)ent);
                    }else{
                        result2.getChildren().add((Node)ent);
                    }
                }
            } else {
                if (r1nbE <= demimaxE && r2nbE > demimaxE) {
                    if(leaf){
                        result1.getEntries().add((Envelope)ent);
                    }else{
                        result1.getChildren().add((Node)ent);
                    }
                } else if (r2nbE <= demimaxE && r1nbE > demimaxE) {
                    if(leaf){
                        result2.getEntries().add((Envelope)ent);
                    }else{
                        result2.getChildren().add((Node)ent);
                    }
                } else {
                    if(leaf){
                        result2.getEntries().add((Envelope)ent);
                    }else{
                        result2.getChildren().add((Node)ent);
                    }
                }
            }
        }

        if(!leaf){
            final List<Node> lR1 = result1.getChildren();
            if(lR1.size() == 1) {
                result1 = lR1.get(0);
                result1.setParent(null);
            }
            final List<Node> lR2 = result2.getChildren();
            if(lR2.size() == 1) {
                result2 = lR2.get(0);
                result2.setParent(null);
            }
            for (Node nod2d : lR1) {
                nod2d.setParent(result1);
            }
            for (Node nod2d : lR2) {
                nod2d.setParent(result2);
            }
        }
        return UnmodifiableArrayList.wrap(result1, result2);
    }

    /**
     * Travel {@code Tree}, find {@code Entry} if it exist and delete it.
     *
     * <blockquote><font size=-1>
     * <strong>NOTE: Moreover {@code Tree} is condensate after a deletion to stay conform about R-Tree properties.</strong>
     * </font></blockquote>
     *
     * @param candidate {@code Node}  where to delete.
     * @param entry {@code Envelope} to delete.
     * @throws IllegalArgumentException if candidate or entry is null.
     * @return true if entry is find and deleted else false.
     */
    private static boolean deleteNode(final Node candidate, final Envelope entry) throws IllegalArgumentException{
        ArgumentChecks.ensureNonNull("DeleteNode : Node candidate", candidate);
        ArgumentChecks.ensureNonNull("DeleteNode : Node candidate", candidate);
        if (new GeneralEnvelope(candidate.getBoundary()).intersects(entry, true)) {
            if (candidate.isLeaf()) {
                final boolean removed = candidate.getEntries().remove(entry);
                if (removed) {
                    final AbstractTree tree = ((AbstractTree)candidate.getTree());
                    tree.setElementsNumber(tree.getElementsNumber()-1);
                    trim(candidate);
                    return true;
                }
            }else{
                for (Node no : candidate.getChildren()) {
                    final boolean removed = deleteNode(no, entry);
                    if (removed) return true;
                }
            }
        }
        return false;
    }

    /**
     * Travel {@code Tree}, find {@code Entry} if it exist and delete it from reference.
     *
     * <blockquote><font size=-1>
     * <strong>NOTE: Moreover {@code Tree} is condensate after a deletion to stay conform about R-Tree properties.</strong>
     * </font></blockquote>
     *
     * @param candidate {@code Node}  where to delete.
     * @param entry {@code Envelope} to delete.
     * @throws IllegalArgumentException if candidate or entry is null.
     * @return true if entry is find and deleted else false.
     */
    private static boolean removeNode(final Node candidate, final Envelope entry) throws IllegalArgumentException{
        ArgumentChecks.ensureNonNull("removeNode : Node candidate", candidate);
        ArgumentChecks.ensureNonNull("removeNode : Node candidate", candidate);
        if(new GeneralEnvelope(candidate.getBoundary()).intersects(entry, true)){
            if (candidate.isLeaf()) {
                final List<Envelope> l = candidate.getEntries();
                boolean removed = false;
                for (int i = l.size()-1; i>=0; i--) {
                    if (l.get(i).equals(entry)) {
                        removed = true;
                        l.remove(i);
                        break;
                    }
                }
                if (removed) {
                    final AbstractTree tree = ((AbstractTree)candidate.getTree());
                    tree.setElementsNumber(tree.getElementsNumber()-1);
                    trim(candidate);
                    return true;
                }
            } else {
                for (Node no : candidate.getChildren()) {
                    final boolean removed = removeNode(no, entry);
                    if (removed) return true;
                }
            }
        }
        return false;
    }

    /**Condense R-Tree.
     *
     * Condense made, travel up from leaf to tree trunk.
     *
     * @param candidate {@code Node} to begin condense.
     * @throws IllegalArgumentException if candidate is null.
     */
    private static void trim(final Node candidate) throws IllegalArgumentException {
        ArgumentChecks.ensureNonNull("trim : Node3D candidate", candidate);
        final List<Node> children = candidate.getChildren();
        final AbstractTree tree = ((AbstractTree)candidate.getTree());
        final List<Envelope> reinsertList = new ArrayList<Envelope>();
        for(int i = children.size()-1; i>=0; i--) {
            final Node child = children.get(i);
            final List<Envelope> childList = child.getEntries();
            final int siz = childList.size();
            if (child.isEmpty()) {
                children.remove(i);
            } else if (child.getChildren().size() == 1) {
                final Node n = children.remove(i);
                for(Node n2d : n.getChildren()) {
                    n2d.setParent(candidate);
                }
                children.addAll(n.getChildren());
            } else if(child.isLeaf() && siz<=tree.getMaxElements()/3) {
                reinsertList.addAll(childList);
                tree.setElementsNumber(tree.getElementsNumber()-siz);
                children.remove(i);
            }
        }
        if (candidate.getParent() != null) trim((Node)candidate.getParent());
        for (Envelope ent : reinsertList) {
            tree.insert(ent);
        }
    }

    /**
     * Find appropriate {@code Node} to insert {@code Envelope} entry.
     * To define appropriate Node, criterion are :
     *      - require minimum area enlargement to cover shape.
     *      - or put into {@code Node} with lesser elements number in case of area equals.
     *
     * @param children List of {@code Node}.
     * @param entry {@code Envelope} to add.
     * @throws IllegalArgumentException if children or entry are null.
     * @throws IllegalArgumentException if children is empty.
     * @return {@code Node} which is appropriate to contain shape.
     */
    private static Node chooseSubtree(final Node candidate, final Envelope entry) {
        ArgumentChecks.ensureNonNull("chooseSubtree : candidate", candidate);
        ArgumentChecks.ensureNonNull("chooseSubtree : Envelope entry", entry);
        final Calculator calc = candidate.getTree().getCalculator();
        final List<Node> children = candidate.getChildren();
        if (children.isEmpty()) throw new IllegalArgumentException("chooseSubtree : ln is empty");
        if (children.size() == 1) return children.get(0);
        Node n = children.get(0);
        for (Node nod : children) {
            if (new GeneralEnvelope(nod.getBoundary()).contains(entry, true)) return nod;
        }

        final List<Envelope> lGE = new ArrayList<Envelope>();
        for(Node dn : children) {
            lGE.add(dn.getBoundary());
        }
        double area = calc.getSpace(DefaultTreeUtils.getEnveloppeMin(lGE));
        double nbElmt = DefaultTreeUtils.countElements(n);
        double areaTemp;
        for (Node dn : children) {
            final GeneralEnvelope rnod = new GeneralEnvelope(dn.getBoundary());
            rnod.add(entry);
            final int nbe = DefaultTreeUtils.countElements(dn);
            areaTemp = calc.getEnlargement(dn.getBoundary(), rnod);
            if (areaTemp < area) {
                n = dn;
                area = areaTemp;
                nbElmt = nbe;
            } else if (areaTemp== area) {
                if (nbe < nbElmt) {
                    n = dn;
                    area = areaTemp;
                    nbElmt = nbe;
                }
            }
        }
        return n;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Node createNode(Tree tree, Node parent, List<Node> listChildren, List<Envelope> listEntries, double... coordinates) {
        final int ddim = coordinates.length;
        if((ddim % 2) != 0) throw new IllegalArgumentException("coordinate dimension is not correct");
        if(ddim == 0) return nodefactory.createNode(tree, parent, null, null,listChildren, listEntries);
        final int dim = coordinates.length/2;
        final double[] dp1Coords = new double[dim];
        final double[] dp2Coords = new double[dim];
        System.arraycopy(coordinates, 0, dp1Coords, 0, dim);
        System.arraycopy(coordinates, dim, dp2Coords, 0, dim);

        final DirectPosition dp1 = new GeneralDirectPosition(crs);
        final DirectPosition dp2 = new GeneralDirectPosition(crs);
        for(int i =0; i<dim; i++){
            dp1.setOrdinate(i, dp1Coords[i]);
            dp2.setOrdinate(i, dp2Coords[i]);
        }
        return nodefactory.createNode(tree, parent, dp1, dp2,listChildren, listEntries);
    }
}
