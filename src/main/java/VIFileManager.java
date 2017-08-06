

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;


public class VIFileManager extends JPanel
        implements ActionListener {
    private final String NEW_COMMAND = "create";
    private final String REMOVE_COMMAND = "remove";
    private final String REFRESH_COMMAND = "refresh";
    private final String RENAME_COMMAND = "rename";
    private TreeFileManager treePanel;
    private JPanel buttonPanel;
    public VIFileManager() {
        super(new BorderLayout());

        final JProgressBar bar = new JProgressBar();
        PropertyChangeListener progressListener = new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                bar.setValue((Integer) evt.getNewValue());
            }
        };

        treePanel = new TreeFileManager(progressListener);

        InitButtonPanel();

        add(treePanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.NORTH);
        add(bar, BorderLayout.SOUTH);
    }

    private void InitButtonPanel() {
        JButton newfolderButton = getNewButton("New Folder.png");
        newfolderButton.setActionCommand(NEW_COMMAND);
        newfolderButton.setToolTipText("New folder");

        JButton removeButton = getNewButton("Remove Folder.png");
        removeButton.setActionCommand(REMOVE_COMMAND);
        removeButton.setToolTipText("Remove");

        JButton renameButton = getNewButton("Rename Folder.png");
        renameButton.setActionCommand(RENAME_COMMAND);
        renameButton.setToolTipText("Rename");

        JButton refreshButton = getNewButton("Refresh Folder.png");
        refreshButton.setActionCommand(REFRESH_COMMAND);
        refreshButton.setToolTipText("Refresh");



        buttonPanel = new JPanel(new GridLayout(0,4));
        buttonPanel.add(newfolderButton);
        buttonPanel.add(removeButton);
        buttonPanel.add(renameButton);
        buttonPanel.add(refreshButton);
        add(buttonPanel, BorderLayout.NORTH);
    }

    private ImageIcon createImageIcon(String path) {
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
    private void showErrorPanel(String windowName, String message) {
        JOptionPane.showMessageDialog(this,
                message,
                windowName,
                JOptionPane.ERROR_MESSAGE);
    }
    private JButton getNewButton(String path) {
        JButton button = new JButton(createImageIcon(path));
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.addActionListener(this);
        return button;
    }

    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();
        if(command.equals(NEW_COMMAND)) {
            newFolder();
        } else if(command.equals(REMOVE_COMMAND)) {
            removeCurrentFile();
        } else if(command.equals(REFRESH_COMMAND)) {
            refresh();
        } else if(command.equals(RENAME_COMMAND)) {
            rename();
        }


    }

    private void removeCurrentFile(){
        if(!treePanel.haveSelectionFile()) {
            showErrorPanel("Error remove","No files selected");
            return;
        }
        try {
            treePanel.removeCurrentFile();
        }catch (FileManagerException e) {
            showErrorPanel("Error remove",e.getDetail());
        }
    }

    private void newFolder() {
        if(!treePanel.haveSelectionFile()) {
            showErrorPanel("Error new folder","No files selected");
            return;
        }
        JPanel createDirectoryPanel = new JPanel(new BorderLayout(3, 3));


        JTextField name = new JTextField(15);

        createDirectoryPanel.add(new JLabel("Enter a new folder name"), BorderLayout.WEST);
        createDirectoryPanel.add(name);
        int result = JOptionPane.showConfirmDialog(
                this,
                createDirectoryPanel,
                "New folder",
                JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {

            try {
                treePanel.newFolder(name.getText());
            } catch (FileManagerException e) {
                showErrorPanel("Error new folder",e.getDetail());
            }

        }
    }

    public void refresh() {

        treePanel.refresh();
    }
    public void rename() {
        if(!treePanel.haveSelectionFile()) {
            showErrorPanel("Error rename","No files selected");
            return;
        }
        JPanel createDirectoryPanel = new JPanel(new BorderLayout(3, 3));


        JTextField name = new JTextField(15);

        createDirectoryPanel.add(new JLabel("Enter a new name"), BorderLayout.WEST);
        createDirectoryPanel.add(name);
        int result = JOptionPane.showConfirmDialog(
                this,
                createDirectoryPanel,
                "New folder",
                JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                treePanel.renameCurrentFile(name.getText());
            } catch (FileManagerException e) {
                showErrorPanel("Error rename",e.getDetail());
            }
        }
    }



    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JFrame frame = new JFrame("File manager");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                VIFileManager newFileManagerPlane = new VIFileManager();
                newFileManagerPlane.setOpaque(true);
                frame.setContentPane(newFileManagerPlane);

                frame.pack();
                frame.setVisible(true);
            }
        });

    }
}
