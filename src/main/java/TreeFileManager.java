import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.filechooser.FileSystemView;
import javax.swing.tree.*;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Comparator;
/*Используется для манипуляций над деревом и его моделью
* */
public class TreeFileManager extends JPanel
        implements TreeWillExpandListener {
    private FileSystemView fileSystemView;
    //Переменная используется для отключения обработки расширения узла
    //перед тем как расширить уже заполненный узел
    private boolean deactivateTreeWillExpandListener;
    private PropertyChangeListener progressListener;
    private DynamicTreeWithState ftree;

    public TreeFileManager() {
        super(new GridLayout(1,0));
        InitTreeUI();
        InitTreeData();
        this.progressListener = null;

    }
    public TreeFileManager(PropertyChangeListener progressListener) {
        super(new GridLayout(1,0));
        InitTreeUI();
        InitTreeData();
        this.progressListener = progressListener;

    }
    //Инициализация дерева
    private void InitTreeUI() {
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode();
        ftree = new DynamicTreeWithState();

        ftree.getTree().setRootVisible(false);
        ftree.getTree().getSelectionModel().setSelectionMode
                (TreeSelectionModel.SINGLE_TREE_SELECTION);
        ftree.getTree().setShowsRootHandles(true);
        ftree.getTree().addTreeWillExpandListener(this);
        ftree.getTree().setCellRenderer(new FileTreeCellRenderer());

        deactivateTreeWillExpandListener = false;

        JScrollPane scrollPane = new JScrollPane(ftree.getTree());
        add(scrollPane);

        setPreferredSize(new Dimension(250, 500));

    }
    //Инициализация файлового менеджера и заполнения дерева
    private void InitTreeData() {


        fileSystemView = FileSystemView.getFileSystemView();
        File[] systemRoots = fileSystemView.getRoots();
        addSystemRoots(systemRoots);

    }
    //Обработка открытия ветки
    public class OpenTreeDirectoryWorker extends SwingWorker<File[],File> {
        private TreePath expandedPath;
        public OpenTreeDirectoryWorker(TreePath expandedPath) {
            this.expandedPath = expandedPath;
        }
        public File[] doInBackground() {

            try {
                for(int i = 0; i <= 2000; i+=400) {
                    setProgress(100*i/2000);
                    Thread.sleep(400);
                }
                setProgress(0);
            }
            catch (Throwable e) {

            }

            File expandedFile = getFile(expandedPath);
            try {
                File[] childs = expandedFile.listFiles();
                return childs;

            } catch (Throwable e) {
                System.err.println("This location could not be added. " + e);
            }

            return null;
        }
        protected void done() {
            try {
                File[] childs = get();
                if(childs!=null && childs.length > 0) {
                    addFiles(expandedPath, childs);
                    DefaultMutableTreeNode expandedNode =
                            getNodeFromPath(expandedPath.getParentPath());

                    //Мы уже выгрузили нужные файлы, поэтому нам не нужно обрабатывать
                    //расширение узла
                    //deactivateTreeWillExpandListener = true - на время расширение узла
                    String s = ftree.getExpansionState();
                    ftree.nodeStructureChanged(expandedNode);
                    deactivateTreeWillExpandListener = true;
                    ftree.setExpansionState(s);
                    ftree.expand(expandedPath);
                    deactivateTreeWillExpandListener = false;
                } else {
                    ftree.collapse(expandedPath);

                    ftree.nodeStructureChanged(getNodeFromPath(expandedPath));
                }
            }
            catch (Throwable e) {

            }


        }
    }
    public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {

        TreePath  expandPath = event.getPath();
        if (deactivateTreeWillExpandListener || ftree.isRoot(expandPath))
            return;
        ftree.clearChilds(expandPath);

        SwingWorker openWorker = new OpenTreeDirectoryWorker(expandPath);
        openWorker.execute();
        if (progressListener != null) {
            openWorker.getPropertyChangeSupport().addPropertyChangeListener("progress", progressListener);
        }
    }
    public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
    }
    public void expandWithoutExpendListener(TreePath expandedPath){
        deactivateTreeWillExpandListener = true;
        ftree.expand(expandedPath);
        deactivateTreeWillExpandListener = false;
    }

    //Операции добавление, удаления, переименования файла, а так же обновления всей структуры
    public void newFolder(String fileName) throws FileManagerException {
        TreePath selectionPath = getSelectionPath();
        File selectionFile = getFile(selectionPath);
        if (selectionFile == null) {
            throw new FileManagerException("No files selected");
        }
        if(!selectionFile.isDirectory()) {
            selectionPath = selectionPath.getParentPath();
            selectionFile = getFile(selectionPath);
        }
        boolean created;
        File newFolder = new File(selectionFile, fileName);
        try {
            created = newFolder.mkdir();

            if (created) {
                addFileWithSort(selectionPath, newFolder);
                if(ftree.isLeaf(selectionPath)) {
                    expandWithoutExpendListener(selectionPath);
                }
            } else {
                String msg = "The file '" +
                        newFolder.getName() +
                        "' could not be created.";
                throw new FileManagerException(msg);
            }

        }
        catch (Throwable t) {
            String msg = "The file '" +
                    newFolder.getName() +
                    "' could not be created.";
            throw new FileManagerException(msg);
        }

    }
    public void refresh() {
        ftree.getRoot().removeAllChildren();
        ftree.getTreeModel().reload();

        File[] systemRoots = fileSystemView.getRoots();
        addSystemRoots(systemRoots);
    }
    public void renameCurrentFile(String newName) throws FileManagerException {
        TreePath fPath = getSelectionPath();
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) fPath.getLastPathComponent();
        File selectionFile = getFile(fPath);
        File newFile = new File(selectionFile.getParentFile(), newName);

        if (selectionFile.renameTo(newFile)) {
            selectionFile = newFile;
            node.setUserObject(selectionFile);
            ftree.getTreeModel().nodeChanged(node);
        }
        else {
            String msg = "The file '" +
                    selectionFile.getName() +
                    "' could not be renamed.";
            throw new FileManagerException(msg);
        }
    }
    public void removeCurrentFile() throws FileManagerException {
        TreePath fPath = getSelectionPath();
        File currentFile = getSelectionFile();
        try {
            deleteFile(currentFile);
            ftree.removeNode(fPath);
        } catch (Throwable e) {
            throw new FileManagerException("Can't remove file " + currentFile.getName() + ".");
        }
    }
    private void deleteFile(File file) throws FileNotFoundException {
        if (file.isDirectory()) {
            for (File child : file.listFiles())
                deleteFile(child);
        }
        if (!file.delete()) {
            throw new FileNotFoundException(file.getAbsolutePath());
        }
    }
    public boolean haveSelectionFile() {
        return ftree.getSelectionPath()!=null;
    }

    //Вспомогательные опперации
    public void addSystemRoots(File[] systemRoots) {
        for (File systemRoot : systemRoots) {

            DefaultMutableTreeNode systemRootNode =
                    ftree.pushBackObject(ftree.getRoot(),systemRoot);

            addEmptyFile(systemRootNode);
        }
        ftree.expand(ftree.getRoot());
    }
    public DefaultMutableTreeNode pushBackFile(TreePath path, File child) {
        DefaultMutableTreeNode nodeChild = ftree.pushBackObject(path, child);
        if (child.isDirectory()) {
            addEmptyFile(nodeChild);
        }
        return nodeChild;
    }
    //Т.к. мы подгружаем файлы при открытии узла
    //Чтобы в представлении дерева, папки были не листьями
    //в них добавляется пустой файл до их расширения
    //при расширении узла пустой файл будет удалён
    public void addEmptyFile(DefaultMutableTreeNode node) {
        File emptyFile = new File("empty");
        ftree.pushBackObject(node, emptyFile);
    }
    //Исользуется для сортировки файлов
    // либо для добавления файлов в уже отсортированный список
    class FileComparator implements Comparator<File> {
        public int compare(File lhs, File rhs) {
            int sumLhs = getSum(lhs);
            int sumRhs = getSum(rhs);
            if(sumLhs == sumRhs) {
                return lhs.compareTo(rhs);
            }
            else if(sumLhs < sumRhs)
                return 1;
            else
                return -1;
        }
        public int getSum(File file) {
            int sum = 0;
            if(file.isDirectory())
                sum+=10;
            if(!file.isHidden())
                sum+=1;
            return sum;
        }
    }
    public void addFiles(TreePath path, File[] childs) {
        Arrays.sort(childs,new FileComparator());
        for (File child : childs) {
            pushBackFile(path, child);
        }

    }
    public void addFileWithSort (TreePath parentPath, File addedFile) {
        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) parentPath.getLastPathComponent();
        Comparator<File> t = new FileComparator();
        for(int i = 0; i < parentNode.getChildCount(); i++) {
            File childFile =
                    (File) ((DefaultMutableTreeNode) ftree.getChild(parentNode,i)).getUserObject();
            if(t.compare(addedFile,childFile) < 0) {
                DefaultMutableTreeNode nodeAddedFile =  ftree.addObject(parentNode, addedFile, i);
                if (childFile.isDirectory()) {
                    addEmptyFile(nodeAddedFile);
                }
                return;
            }
        }
        pushBackFile(parentPath, addedFile);
        return;
    }

    private TreePath getSelectionPath() {
        return ftree.getSelectionPath();
    }
    private File getFile(TreePath path) {
        return (File) ftree.getObject(path);
    }
    public File getSelectionFile() {
        return (File) ftree.getSelectionObject();
    }
    private TreePath getPathFromNode(DefaultMutableTreeNode node) {
        return new TreePath(node.getPath());

    }
    private DefaultMutableTreeNode getNodeFromPath(TreePath path) {
        return (DefaultMutableTreeNode) path.getLastPathComponent();

    }

}
class   FileTreeCellRenderer extends DefaultTreeCellRenderer {

    private FileSystemView fileSystemView;

    private JLabel label;
    private ImageIcon openFolderIcon;
    private ImageIcon closeFolderIcon;
    private ImageIcon openingFolderIcon;
    FileTreeCellRenderer() {
        openFolderIcon = createImageIcon("Opened Folder.png");
        closeFolderIcon = createImageIcon("Close Folder.png");
        openingFolderIcon =  createImageIcon("Opening Folder.png");
        label = new JLabel();
        label.setOpaque(true);
        fileSystemView = FileSystemView.getFileSystemView();
    }
    protected ImageIcon createImageIcon(String path) {
        java.net.URL imgURL = getClass().getResource(path);
        if (imgURL != null) {
            try {
                Image image = ImageIO.read(imgURL);
                image = image.getScaledInstance(20, 20, java.awt.Image.SCALE_SMOOTH);
                return new ImageIcon(image);
            }catch (Throwable e) {
                throw new NullPointerException();
            }
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }
    @Override
    public Component getTreeCellRendererComponent(
            JTree tree,
            Object value,
            boolean selected,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus) {

        DefaultMutableTreeNode node = (DefaultMutableTreeNode)value;
        TreePath path = new TreePath(node.getPath());
        File file = (File)node.getUserObject();
        Icon icon;
        if(file==null)
            return label;
        if(file.isDirectory()) {
            if(tree.isExpanded(path)) {
                if(node.getChildCount()<=0) {
                    icon = openingFolderIcon;
                }
                else{
                    icon = openFolderIcon;
                }
            }else {
                icon = closeFolderIcon;
            }
        }else {
            icon = fileSystemView.getSystemIcon(file);
        }
        label.setIcon(icon);
        label.setText(fileSystemView.getSystemDisplayName(file));
        if (selected) {
            label.setBackground(backgroundSelectionColor);
            label.setForeground(textSelectionColor);
        } else {
            if(file!=null && fileSystemView.isHiddenFile(file)) {
                label.setBackground(backgroundNonSelectionColor);
                label.setForeground(Color.GRAY);
            }
            else {
                label.setBackground(backgroundNonSelectionColor);
                label.setForeground(textNonSelectionColor);
            }
        }




        return label;
    }

}

