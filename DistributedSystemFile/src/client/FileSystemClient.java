package client;

import coordinator.ICoordinator;
import utils.User; // Assuming this file exists in the correct path

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class FileSystemClient {
    private static ICoordinator coordinator;
    private static JFrame currentFrame; // To track the current active frame
    private static String username;
    private static String token;

    // --- Theme Constants ---
    private static final Color COLOR_PRIMARY = new Color(0, 123, 255); // Bootstrap Primary Blue
    private static final Color COLOR_SUCCESS = new Color(40, 167, 69);  // Bootstrap Success Green
    private static final Color COLOR_DANGER = new Color(220, 53, 69);   // Bootstrap Danger Red
    private static final Color COLOR_WARNING = new Color(255, 193, 7);  // Bootstrap Warning Yellow
    private static final Color COLOR_INFO = new Color(23, 162, 184);    // Bootstrap Info Teal
    private static final Color COLOR_SECONDARY = new Color(108, 117, 125); // Bootstrap Secondary Gray

    private static final Color COLOR_BACKGROUND_LIGHT = new Color(248, 249, 250); // Light gray
    private static final Color COLOR_TEXT_FIELD_BORDER = new Color(206, 212, 218);
    private static final Color COLOR_BUTTON_TEXT = Color.WHITE;
    private static final Color COLOR_DARK_TEXT = new Color(33, 37, 41); // Dark gray for text

    private static final Font FONT_TITLE_LARGE = new Font("Segoe UI", Font.BOLD, 24);
    private static final Font FONT_TITLE_MEDIUM = new Font("Segoe UI", Font.BOLD, 20);
    private static final Font FONT_LABEL = new Font("Segoe UI", Font.PLAIN, 15);
    private static final Font FONT_INPUT = new Font("Segoe UI", Font.PLAIN, 15);
    private static final Font FONT_BUTTON = new Font("Segoe UI", Font.BOLD, 15);
    private static final Font FONT_FOOTER = new Font("Segoe UI", Font.ITALIC, 13);

    private static final Insets PADDING_PANEL = new Insets(25, 30, 25, 30);
    private static final Insets PADDING_FORM_ELEMENT = new Insets(10, 5, 10, 5);
    private static final Insets PADDING_TEXT_FIELD_INTERNAL = new Insets(7, 10, 7, 10);

    // --- String Constants (English) ---
    private static final String APP_NAME = "File Management System";

    // Login Window
    private static final String LOGIN_WINDOW_TITLE = APP_NAME + " - Login";
    private static final String LOGIN_HEADER_TEXT = "System Login";
    private static final String LOGIN_USERNAME_LABEL = "Username:";
    private static final String LOGIN_PASSWORD_LABEL = "Password:";
    private static final String LOGIN_BUTTON_TEXT = "Login";
    private static final String LOGIN_ERROR_TITLE = "Login Failed";
    private static final String LOGIN_ERROR_INVALID_CREDENTIALS = "The username or password you entered is incorrect.";
    private static final String LOGIN_ERROR_EMPTY_FIELDS_TITLE = "Empty Fields";
    private static final String LOGIN_ERROR_EMPTY_FIELDS_MSG = "Please enter both username and password.";

    // Home Window
    private static final String HOME_WINDOW_TITLE_PREFIX = "Welcome, ";
    private static final String HOME_HEADER_TEXT = "Main Dashboard";
    private static final String HOME_BUTTON_UPLOAD = "Upload File";
    private static final String HOME_BUTTON_DOWNLOAD = "Download File";
    private static final String HOME_BUTTON_DELETE = "Delete File";
    private static final String HOME_BUTTON_EDIT = "Edit File";
    private static final String HOME_BUTTON_REGISTER_USER = "Register New User";
    private static final String HOME_BUTTON_EXIT = "Exit System";
    private static final String HOME_FOOTER_LOGGED_IN_AS = "Logged in as: ";

    // General Messages & Titles
    private static final String MSG_SUCCESS_TITLE = "Success";
    private static final String MSG_ERROR_TITLE = "Error";
    private static final String MSG_WARNING_TITLE = "Warning";
    private static final String MSG_CONFIRMATION_TITLE = "Confirm Action";
    private static final String MSG_NETWORK_ERROR_TITLE = "Connection Error";
    private static final String MSG_NETWORK_ERROR_CONTENT = "Could not connect to the server. Please check your network connection and try again later.";
    private static final String MSG_OPERATION_CANCELLED = "Operation cancelled by user.";
    private static final String MSG_ENTER_FILENAME_PROMPT = "Enter filename:";
    private static final String MSG_FILE_NOT_FOUND_LOCAL = "The selected local file was not found: ";


    // Upload
    private static final String UPLOAD_DIALOG_TITLE = "Select File to Upload";
    private static final String UPLOAD_PROMPT_FILENAME_STORAGE = "Enter filename for storage on server:";
    private static final String UPLOAD_SUCCESS_MSG = "File uploaded successfully.";
    private static final String UPLOAD_FAILURE_MSG = "File upload failed. You may not have permission or an error occurred.";
    private static final String UPLOAD_ERROR_READING_FILE = "An error occurred while reading the local file.";

    // Download
    private static final String DOWNLOAD_DIALOG_TITLE = "Save Downloaded File";
    private static final String DOWNLOAD_PROMPT_FILENAME = "Enter the name of the file to download:";
    private static final String DOWNLOAD_SUCCESS_MSG_PREFIX = "File downloaded successfully to: ";
    private static final String DOWNLOAD_FILE_NOT_FOUND_MSG = "The requested file was not found on the server.";
    private static final String DOWNLOAD_FAILURE_MSG = "File download failed. Please try again.";
    private static final String DOWNLOAD_ERROR_WRITING_FILE = "An error occurred while saving the downloaded file.";

    // Delete
    private static final String DELETE_PROMPT_FILENAME = "Enter the name of the file to delete:";
    private static final String DELETE_CONFIRM_MSG = "Are you sure you want to delete this file? This action cannot be undone.";
    private static final String DELETE_SUCCESS_MSG = "File deleted successfully.";
    private static final String DELETE_FAILURE_MSG = "Failed to delete the file. It may not exist or you may not have permission.";

    // Edit/Update
    private static final String EDIT_DIALOG_TITLE = "Select File for Update";
    private static final String EDIT_PROMPT_FILENAME_SERVER = "Enter the name of the server file to update:";
    private static final String EDIT_SUCCESS_MSG = "File updated successfully.";
    private static final String EDIT_FAILURE_MSG = "File update failed. You may not have permission or an error occurred.";

    // Register User
    private static final String REGISTER_PROMPT_USERNAME = "Enter username for the new user:";
    private static final String REGISTER_PROMPT_DEPARTMENT = "Enter department (e.g., Development, Quality Assurance):";
    private static final String REGISTER_SUCCESS_MSG = "New user account created successfully.";
    private static final String REGISTER_NO_PERMISSION_MSG = "You do not have permission to create new users.";
    private static final String REGISTER_FAILURE_MSG = "User registration could not be completed.";

    // Exit
    private static final String EXIT_CONFIRM_MSG = "Are you sure you want to exit the system?";

    public static void main(String[] args) {

        System.out.println("Starting File System Client...");

        try {
            // Set Look and Feel
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("Nimbus L&F not found, using default. " + e.getMessage());
        }

        try {
            System.out.println("Connecting to RMI registry on localhost:1099...");
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            coordinator = (ICoordinator) registry.lookup("CoordinatorFileSystem");
            System.out.println("Successfully connected to server coordinator.");

            SwingUtilities.invokeLater(() -> {
                System.out.println("Launching login window...");
                showLoginWindow();
            });
        }
        catch (Exception e) {
            System.err.println("Client Main Error - Connection to server failed: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    MSG_NETWORK_ERROR_CONTENT,
                    MSG_NETWORK_ERROR_TITLE,
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private static Border createPaddedBorder() {
        Border line = BorderFactory.createLineBorder(COLOR_TEXT_FIELD_BORDER);
        Border padding = new EmptyBorder(PADDING_TEXT_FIELD_INTERNAL);
        return new CompoundBorder(line, padding);
    }

    private static JButton createStyledButton(String text, Color backgroundColor, Color textColor) {
        JButton button = new JButton(text);
        button.setFont(FONT_BUTTON);
        button.setBackground(backgroundColor);
        button.setForeground(textColor);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        Color hoverColor = backgroundColor.brighter();
        Color pressedColor = backgroundColor.darker();

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(hoverColor);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(backgroundColor);
            }
            @Override
            public void mousePressed(MouseEvent e) {
                button.setBackground(pressedColor);
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (button.getMousePosition() != null) { // Check if mouse is still over the button
                    button.setBackground(hoverColor);
                } else {
                    button.setBackground(backgroundColor);
                }
            }
        });
        return button;
    }


    private static void showLoginWindow() {
        currentFrame = new JFrame(LOGIN_WINDOW_TITLE);
        currentFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        currentFrame.setSize(400, 330);
        currentFrame.setLocationRelativeTo(null);
        currentFrame.setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(new EmptyBorder(PADDING_PANEL));
        mainPanel.setBackground(COLOR_BACKGROUND_LIGHT);

        // Header Panel
        JLabel titleLabel = new JLabel(LOGIN_HEADER_TEXT, JLabel.CENTER);
        titleLabel.setFont(FONT_TITLE_MEDIUM);
        titleLabel.setForeground(COLOR_DARK_TEXT);
        titleLabel.setBorder(new EmptyBorder(0, 0, 15, 0)); // Margin below header
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // Form Panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(COLOR_BACKGROUND_LIGHT);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = PADDING_FORM_ELEMENT;
        gbc.anchor = GridBagConstraints.LINE_START;

        JLabel userLabel = new JLabel(LOGIN_USERNAME_LABEL);
        userLabel.setFont(FONT_LABEL);
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE;
        formPanel.add(userLabel, gbc);

        JTextField userField = new JTextField(20);
        userField.setFont(FONT_INPUT);
        userField.setBorder(createPaddedBorder());
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL;
        formPanel.add(userField, gbc);

        JLabel passLabel = new JLabel(LOGIN_PASSWORD_LABEL);
        passLabel.setFont(FONT_LABEL);
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE;
        formPanel.add(passLabel, gbc);

        JPasswordField passField = new JPasswordField(20);
        passField.setFont(FONT_INPUT);
        passField.setBorder(createPaddedBorder());
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL;
        formPanel.add(passField, gbc);

        mainPanel.add(formPanel, BorderLayout.CENTER);

        // Login Button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 10));
        buttonPanel.setBackground(COLOR_BACKGROUND_LIGHT);
        JButton loginBtn = createStyledButton(LOGIN_BUTTON_TEXT, COLOR_PRIMARY, COLOR_BUTTON_TEXT);
        loginBtn.setPreferredSize(new Dimension(150, 40)); // Set preferred size for login button

        loginBtn.addActionListener(e -> {
            username = userField.getText().trim();
            String password = new String(passField.getPassword());

            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(currentFrame,
                        LOGIN_ERROR_EMPTY_FIELDS_MSG,
                        LOGIN_ERROR_EMPTY_FIELDS_TITLE,
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            System.out.println("Login attempt for user: " + username);
            try {
                token = coordinator.signIn(username, password);
                if (token != null) {
                    System.out.println("Login successful for user: " + username);
                    currentFrame.dispose();
                    showHomeWindow();
                } else {
                    System.out.println("Login failed - invalid credentials for user: " + username);
                    JOptionPane.showMessageDialog(currentFrame,
                            LOGIN_ERROR_INVALID_CREDENTIALS,
                            LOGIN_ERROR_TITLE,
                            JOptionPane.ERROR_MESSAGE);
                    passField.setText("");
                    passField.requestFocusInWindow();
                }
            } catch (Exception ex) {
                System.err.println("Login Action Error: " + ex.getMessage());
                ex.printStackTrace();
                JOptionPane.showMessageDialog(currentFrame,
                        MSG_NETWORK_ERROR_CONTENT,
                        MSG_NETWORK_ERROR_TITLE,
                        JOptionPane.ERROR_MESSAGE);
            }
        });
        buttonPanel.add(loginBtn);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        currentFrame.add(mainPanel);
        currentFrame.setVisible(true);
        SwingUtilities.invokeLater(userField::requestFocusInWindow);
    }

    private static void showHomeWindow() {
        System.out.println("Displaying home window for user: " + username);

        currentFrame = new JFrame(HOME_WINDOW_TITLE_PREFIX + username);
        currentFrame.setSize(600, 550);
        currentFrame.setLocationRelativeTo(null);
        currentFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        currentFrame.setResizable(true); // Allow resizing for home window

        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(new EmptyBorder(PADDING_PANEL));
        mainPanel.setBackground(COLOR_BACKGROUND_LIGHT);
        // Title Panel
        JLabel titleLabel = new JLabel(HOME_HEADER_TEXT, JLabel.CENTER);
        titleLabel.setFont(FONT_TITLE_LARGE);
        titleLabel.setForeground(COLOR_DARK_TEXT);
        titleLabel.setBorder(new EmptyBorder(0, 0, 20, 0));
        mainPanel.add(titleLabel, BorderLayout.NORTH);
        // Buttons Panel
        JPanel buttonPanel = new JPanel(new GridLayout(0, 1, 10, 12)); // Flexible rows, 1 column
        buttonPanel.setBorder(new EmptyBorder(10, 80, 10, 80)); // More horizontal padding
        buttonPanel.setBackground(COLOR_BACKGROUND_LIGHT);
        // Button Definitions
        JButton btnUpload = createStyledButton(HOME_BUTTON_UPLOAD, COLOR_PRIMARY, COLOR_BUTTON_TEXT);
        JButton btnDownload = createStyledButton(HOME_BUTTON_DOWNLOAD, COLOR_SUCCESS, COLOR_BUTTON_TEXT);
        JButton btnDelete = createStyledButton(HOME_BUTTON_DELETE, COLOR_DANGER, COLOR_BUTTON_TEXT);
        JButton btnEdit = createStyledButton(HOME_BUTTON_EDIT, COLOR_INFO, COLOR_BUTTON_TEXT);
        JButton btnRegister = createStyledButton(HOME_BUTTON_REGISTER_USER, COLOR_WARNING, Color.BLACK); // Yellow needs dark text
        JButton btnExit = createStyledButton(HOME_BUTTON_EXIT, COLOR_SECONDARY, COLOR_BUTTON_TEXT);

        buttonPanel.add(btnUpload);
        buttonPanel.add(btnDownload);
        buttonPanel.add(btnDelete);
        buttonPanel.add(btnEdit);
        buttonPanel.add(btnRegister);
        buttonPanel.add(btnExit);

        // Button Actions
        btnUpload.addActionListener(e -> uploadFile());
        btnDownload.addActionListener(e -> downloadFile());
        btnDelete.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(currentFrame,
                    DELETE_CONFIRM_MSG, MSG_CONFIRMATION_TITLE,
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) deleteFile();
        });
        btnEdit.addActionListener(e -> editFile());
        btnRegister.addActionListener(e -> registerUser());
        btnExit.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(currentFrame,
                    EXIT_CONFIRM_MSG, MSG_CONFIRMATION_TITLE,
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                System.out.println("User " + username + " exited the application.");
                currentFrame.dispose();
                System.exit(0); // Ensure application fully exits
            }
        });

        mainPanel.add(buttonPanel, BorderLayout.CENTER);

        // Footer
        JLabel footerLabel = new JLabel(HOME_FOOTER_LOGGED_IN_AS + username, JLabel.CENTER);
        footerLabel.setFont(FONT_FOOTER);
        footerLabel.setForeground(COLOR_SECONDARY);
        footerLabel.setBorder(new EmptyBorder(20, 0, 0, 0));
        mainPanel.add(footerLabel, BorderLayout.SOUTH);

        currentFrame.add(mainPanel);
        currentFrame.setVisible(true);
    }

    private static void registerUser() {
        System.out.println("User registration process initiated by: " + username);
        String newUsername = JOptionPane.showInputDialog(currentFrame, REGISTER_PROMPT_USERNAME, MSG_CONFIRMATION_TITLE, JOptionPane.PLAIN_MESSAGE);
        if (newUsername == null || newUsername.trim().isEmpty()) {
            System.out.println(MSG_OPERATION_CANCELLED + " (No username entered)");
            return;
        }
        newUsername = newUsername.trim();

        String department = JOptionPane.showInputDialog(currentFrame, REGISTER_PROMPT_DEPARTMENT, MSG_CONFIRMATION_TITLE, JOptionPane.PLAIN_MESSAGE);
        if (department == null || department.trim().isEmpty()) {
            System.out.println(MSG_OPERATION_CANCELLED + " (No department entered)");
            return;
        }
        department = department.trim();

        System.out.println("Attempting to register new user: " + newUsername + " in department: " + department);
        User user = new User(newUsername, "employee", department); // Assuming "employee" role by default
        try {
            boolean success = coordinator.createUser(user, token);
            if (success) {
                System.out.println("User registration successful: " + newUsername);
                JOptionPane.showMessageDialog(currentFrame, REGISTER_SUCCESS_MSG, MSG_SUCCESS_TITLE, JOptionPane.INFORMATION_MESSAGE);
            } else {
                System.out.println("User registration failed (permission or existing user): " + newUsername);
                JOptionPane.showMessageDialog(currentFrame, REGISTER_NO_PERMISSION_MSG, MSG_ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            System.err.println("Registration Action Error: " + ex.getMessage());
            ex.printStackTrace();
            JOptionPane.showMessageDialog(currentFrame, REGISTER_FAILURE_MSG, MSG_ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void uploadFile() {
        System.out.println("File upload process initiated by: " + username);
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(UPLOAD_DIALOG_TITLE);
        int result = chooser.showOpenDialog(currentFrame);

        if (result == JFileChooser.APPROVE_OPTION) {
            File fileToUpload = chooser.getSelectedFile();
            System.out.println("Selected file for upload: " + fileToUpload.getAbsolutePath());

            String storageFilename = JOptionPane.showInputDialog(currentFrame, UPLOAD_PROMPT_FILENAME_STORAGE, MSG_CONFIRMATION_TITLE, JOptionPane.PLAIN_MESSAGE);
            if (storageFilename == null || storageFilename.trim().isEmpty()) {
                System.out.println(MSG_OPERATION_CANCELLED + " (No storage filename provided)");
                return;
            }
            storageFilename = storageFilename.trim();

            try (FileInputStream fis = new FileInputStream(fileToUpload)) {
                byte[] fileData = new byte[(int) fileToUpload.length()];
                int bytesRead = fis.read(fileData);
                if (bytesRead != fileToUpload.length()) {
                    System.err.println("Could not read the entire file: " + fileToUpload.getName());
                    JOptionPane.showMessageDialog(currentFrame, UPLOAD_ERROR_READING_FILE, MSG_ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
                    return;
                }

                System.out.println("Uploading file '" + storageFilename + "' to server...");
                boolean success = coordinator.saveFile(token, storageFilename, fileData);

                if (success) {
                    System.out.println("File upload successful: " + storageFilename);
                    JOptionPane.showMessageDialog(currentFrame, UPLOAD_SUCCESS_MSG, MSG_SUCCESS_TITLE, JOptionPane.INFORMATION_MESSAGE);
                } else {
                    System.out.println("File upload failed on server: " + storageFilename);
                    JOptionPane.showMessageDialog(currentFrame, UPLOAD_FAILURE_MSG, MSG_ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
                }
            } catch (FileNotFoundException fnfEx) {
                System.err.println("Upload Action Error - File not found: " + fileToUpload.getAbsolutePath() + " - " + fnfEx.getMessage());
                JOptionPane.showMessageDialog(currentFrame, MSG_FILE_NOT_FOUND_LOCAL + fileToUpload.getName(), MSG_ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
            }
            catch (IOException ioEx) {
                System.err.println("Upload Action Error - IO: " + ioEx.getMessage());
                ioEx.printStackTrace();
                JOptionPane.showMessageDialog(currentFrame, UPLOAD_ERROR_READING_FILE, MSG_ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
            }
            catch (Exception ex) {
                System.err.println("Upload Action Error - General: " + ex.getMessage());
                ex.printStackTrace();
                JOptionPane.showMessageDialog(currentFrame, UPLOAD_FAILURE_MSG, MSG_ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
            }
        } else {
            System.out.println(MSG_OPERATION_CANCELLED + " (File chooser closed or cancelled)");
        }
    }

    private static void deleteFile() {
        System.out.println("File deletion process initiated by: " + username);
        String filenameToDelete = JOptionPane.showInputDialog(currentFrame, DELETE_PROMPT_FILENAME, MSG_CONFIRMATION_TITLE, JOptionPane.PLAIN_MESSAGE);
        if (filenameToDelete == null || filenameToDelete.trim().isEmpty()) {
            System.out.println(MSG_OPERATION_CANCELLED + " (No filename entered for deletion)");
            return;
        }
        filenameToDelete = filenameToDelete.trim();

        System.out.println("Attempting to delete file: " + filenameToDelete);
        try {
            boolean success = coordinator.removeFile(token, filenameToDelete);
            if (success) {
                System.out.println("File deletion successful: " + filenameToDelete);
                JOptionPane.showMessageDialog(currentFrame, DELETE_SUCCESS_MSG, MSG_SUCCESS_TITLE, JOptionPane.INFORMATION_MESSAGE);
            } else {
                System.out.println("File deletion failed on server: " + filenameToDelete);
                JOptionPane.showMessageDialog(currentFrame, DELETE_FAILURE_MSG, MSG_ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            System.err.println("Deletion Action Error: " + ex.getMessage());
            ex.printStackTrace();
            JOptionPane.showMessageDialog(currentFrame, DELETE_FAILURE_MSG, MSG_ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void editFile() {
        System.out.println("File edit process initiated by: " + username);
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(EDIT_DIALOG_TITLE);
        int result = chooser.showOpenDialog(currentFrame);

        if (result == JFileChooser.APPROVE_OPTION) {
            File fileToEditWith = chooser.getSelectedFile();
            System.out.println("Selected local file for new content: " + fileToEditWith.getAbsolutePath());

            String serverFilename = JOptionPane.showInputDialog(currentFrame, EDIT_PROMPT_FILENAME_SERVER, MSG_CONFIRMATION_TITLE, JOptionPane.PLAIN_MESSAGE);
            if (serverFilename == null || serverFilename.trim().isEmpty()) {
                System.out.println(MSG_OPERATION_CANCELLED + " (No server filename provided for update)");
                return;
            }
            serverFilename = serverFilename.trim();

            try (FileInputStream fis = new FileInputStream(fileToEditWith)) {
                byte[] fileData = new byte[(int) fileToEditWith.length()];
                int bytesRead = fis.read(fileData);
                if (bytesRead != fileToEditWith.length()) {
                    System.err.println("Could not read the entire file: " + fileToEditWith.getName());
                    JOptionPane.showMessageDialog(currentFrame, UPLOAD_ERROR_READING_FILE, MSG_ERROR_TITLE, JOptionPane.ERROR_MESSAGE); // Re-use upload error message
                    return;
                }

                System.out.println("Updating file '" + serverFilename + "' on server with content from '" + fileToEditWith.getName() + "'");
                boolean success = coordinator.updateFile(token, serverFilename, fileData);

                if (success) {
                    System.out.println("File update successful: " + serverFilename);
                    JOptionPane.showMessageDialog(currentFrame, EDIT_SUCCESS_MSG, MSG_SUCCESS_TITLE, JOptionPane.INFORMATION_MESSAGE);
                } else {
                    System.out.println("File update failed on server: " + serverFilename);
                    JOptionPane.showMessageDialog(currentFrame, EDIT_FAILURE_MSG, MSG_ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
                }
            } catch (FileNotFoundException fnfEx) {
                System.err.println("Edit Action Error - File not found: " + fileToEditWith.getAbsolutePath() + " - " + fnfEx.getMessage());
                JOptionPane.showMessageDialog(currentFrame, MSG_FILE_NOT_FOUND_LOCAL + fileToEditWith.getName(), MSG_ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
            }
            catch (IOException ioEx) {
                System.err.println("Edit Action Error - IO: " + ioEx.getMessage());
                ioEx.printStackTrace();
                JOptionPane.showMessageDialog(currentFrame, UPLOAD_ERROR_READING_FILE, MSG_ERROR_TITLE, JOptionPane.ERROR_MESSAGE); // Re-use upload error
            }
            catch (Exception ex) {
                System.err.println("Edit Action Error - General: " + ex.getMessage());
                ex.printStackTrace();
                JOptionPane.showMessageDialog(currentFrame, EDIT_FAILURE_MSG, MSG_ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
            }
        } else {
            System.out.println(MSG_OPERATION_CANCELLED + " (File chooser closed or cancelled for edit)");
        }
    }

    private static void downloadFile() {
        System.out.println("File download process initiated by: " + username);
        String filenameToDownload = JOptionPane.showInputDialog(currentFrame, DOWNLOAD_PROMPT_FILENAME, MSG_CONFIRMATION_TITLE, JOptionPane.PLAIN_MESSAGE);
        if (filenameToDownload == null || filenameToDownload.trim().isEmpty()) {
            System.out.println(MSG_OPERATION_CANCELLED + " (No filename entered for download)");
            return;
        }
        filenameToDownload = filenameToDownload.trim();

        System.out.println("Attempting to download file: " + filenameToDownload);
        try {
            byte[] fileData = coordinator.getFile(token, filenameToDownload);
            if (fileData != null && fileData.length > 0) {
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle(DOWNLOAD_DIALOG_TITLE);
                chooser.setSelectedFile(new File(filenameToDownload)); // Suggest original filename
                int saveResult = chooser.showSaveDialog(currentFrame);

                if (saveResult == JFileChooser.APPROVE_OPTION) {
                    File outputFile = chooser.getSelectedFile();
                    if (outputFile.getParentFile() != null) {
                        outputFile.getParentFile().mkdirs(); // Ensure parent directory exists
                    }
                    try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                        fos.write(fileData);
                        String successMessage = DOWNLOAD_SUCCESS_MSG_PREFIX + outputFile.getAbsolutePath();
                        System.out.println(successMessage);
                        JOptionPane.showMessageDialog(currentFrame, successMessage, MSG_SUCCESS_TITLE, JOptionPane.INFORMATION_MESSAGE);
                    } catch (IOException ioEx) {
                        System.err.println("Download Action Error - Writing file: " + ioEx.getMessage());
                        ioEx.printStackTrace();
                        JOptionPane.showMessageDialog(currentFrame, DOWNLOAD_ERROR_WRITING_FILE, MSG_ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    System.out.println(MSG_OPERATION_CANCELLED + " (Save dialog closed or cancelled)");
                }
            } else {
                System.out.println("File not found on server or is empty: " + filenameToDownload);
                JOptionPane.showMessageDialog(currentFrame, DOWNLOAD_FILE_NOT_FOUND_MSG, MSG_WARNING_TITLE, JOptionPane.WARNING_MESSAGE);
            }
        } catch (Exception ex) {
            System.err.println("Download Action Error: " + ex.getMessage());
            ex.printStackTrace();
            JOptionPane.showMessageDialog(currentFrame, DOWNLOAD_FAILURE_MSG, MSG_ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
        }
    }
}