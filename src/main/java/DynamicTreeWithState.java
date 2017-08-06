import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;

public class DynamicTreeWithState {
    private DefaultTreeModel treeModel;
    private JTree tree;
    private  DefaultMutableTreeNode rootNode;
    private TreePath selectionPath;
    public DynamicTreeWithState(){

        rootNode = new DefaultMutableTreeNode();

        treeModel = new DefaultTreeModel(rootNode);

        tree = new JTree(treeModel);

    }

    //Методы добавления
    public DefaultMutableTreeNode addObject(DefaultMutableTreeNode parent,
                                            Object child, int index) {

        DefaultMutableTreeNode childNode =
                new DefaultMutableTreeNode(child);
        if (parent == null) {
            throw new NullPointerException("No node selected");
        }

        treeModel.insertNodeInto(childNode, parent,
                index);
        return childNode;

    }

    public DefaultMutableTreeNode pushBackObject(DefaultMutableTreeNode parent,
                                                 Object child) {
        return addObject(parent, child, parent.getChildCount());

    }
    public DefaultMutableTreeNode pushBackObject(TreePath parentPath,
                                                 Object child) {
        DefaultMutableTreeNode parent =
                ((DefaultMutableTreeNode)parentPath.getLastPathComponent());
        return pushBackObject(parent, child);

    }
    public void pushBackObjects(DefaultMutableTreeNode parent, Object[] childs) {
        if (parent == null) {
            throw new NullPointerException("No node selected");
        }
        for(Object child: childs) {
            pushBackObject(parent, child);
        }

    }

    public void pushBackObjects(TreePath parentPath,
                                Object[] childs) {
        DefaultMutableTreeNode parent =
                ((DefaultMutableTreeNode)parentPath.getLastPathComponent());
        if (parent == null) {
            throw new NullPointerException("No node selected");
        }

        pushBackObject(parent, childs);

    }




    //Методы удаления
    public void removeNode(DefaultMutableTreeNode node) {
        if(node == null)
            throw new NullPointerException("No node selected");
        MutableTreeNode parent = (MutableTreeNode) (node.getParent());
        if (parent != null) {
            treeModel.removeNodeFromParent(node);
        }
    }
    public void removeNode(TreePath tPath) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) tPath.getLastPathComponent();
        removeNode(node);
    }



    public void clearChilds(TreePath path) {
        if(path==null)
            return;
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        node.removeAllChildren();
        treeModel.nodeStructureChanged(node);

    }

    //Методы для сохранения/извлечения состояний открытых узлов
    public String getExpansionState(){
        StringBuilder sb = new StringBuilder();
        selectionPath = getSelectionPath();
        for ( int i = 0; i < tree.getRowCount(); i++ ){
            if ( tree.isExpanded(i) && getNode(i).getChildCount() > 0){
                sb.append(i).append(",");
            }
        }
        return sb.toString();

    }
    public void setExpansionState(String s){
        String[] indexes = s.split(",");
        for ( String st : indexes ){
            int row = Integer.parseInt(st);
            tree.expandRow(row);
        }
        tree.setSelectionPath(selectionPath);
    }


    //Вспомогательные методы
    public boolean isLeaf(TreePath path) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        return node.isLeaf();
    }

    public DefaultMutableTreeNode getRoot() {
        return rootNode;
    }
    public Object getSelectionObject() {
        TreePath selectionPath = tree.getSelectionPath();
        DefaultMutableTreeNode selectionNode = null;
        if (selectionPath == null) {
            throw new NullPointerException("No node selected");
        } else {
            selectionNode = (DefaultMutableTreeNode)
                    (selectionPath.getLastPathComponent());
        }
        return selectionNode.getUserObject();
    }
    public Object getObject(TreePath path) {
        if(path == null)
            throw new NullPointerException("Не выбран ни один лист");
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        if(node == null)
            throw new NullPointerException("Не выбран ни один лист");

        return node.getUserObject();
    }
    public Object getChild(DefaultMutableTreeNode parentNode, int index) {
        return treeModel.getChild(parentNode,index);
    }
    public TreePath getSelectionPath () {
        return tree.getSelectionPath();
    }
    public void expand(DefaultMutableTreeNode node) {
        TreePath path = new TreePath(node.getPath());
        expand(path);
    }
    public void collapse(TreePath path) {
        tree.collapsePath(path);
    }
    public void expand(TreePath path) {
        tree.expandPath(path);
    }
    public boolean isRoot(TreePath path) {
        if (path == null || path.getPathCount() != 1) {
            return false;
        }
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        return node.isRoot();
    }
    public DefaultMutableTreeNode getNode(int row) {
        DefaultMutableTreeNode node =
                (DefaultMutableTreeNode)tree.getPathForRow(row).getLastPathComponent();
        return node;
    }

    public void nodeStructureChanged(DefaultMutableTreeNode node) {
        treeModel.nodeStructureChanged(node);
    }


    public DefaultTreeModel getTreeModel() {
        return treeModel;
    }

    public JTree getTree() {
        return tree;
    }
}
