import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class App extends JFrame {
    private Connection conn;
    private int currentUserId;

    private CardLayout card = new CardLayout();
    private JPanel mainPanel = new JPanel(card);

    public App() {
        setTitle("Social Media Platform");
        setSize(600, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        try {
            conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/social_media?useSSL=false&serverTimezone=UTC",
                "root", "");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "DB Error: " + e.getMessage());
            System.exit(1);
        }

        mainPanel.add(buildLoginPanel(), "LOGIN");
        mainPanel.add(buildFeedPanel(), "FEED");

        add(mainPanel);
        card.show(mainPanel, "LOGIN");
    }

    private JPanel buildLoginPanel() {
        JPanel panel = new JPanel(new GridLayout(3,2,10,10));
        JTextField userField = new JTextField();
        JPasswordField passField = new JPasswordField();
        JButton loginBtn = new JButton("Login");

        panel.add(new JLabel("Username:"));
        panel.add(userField);
        panel.add(new JLabel("Password:"));
        panel.add(passField);
        panel.add(new JLabel(""));
        panel.add(loginBtn);

        loginBtn.addActionListener(e -> {
            try {
                PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM users WHERE username=? AND password=?");
                ps.setString(1, userField.getText().trim());
                ps.setString(2, new String(passField.getPassword()).trim());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    currentUserId = rs.getInt("user_id");
                    card.show(mainPanel, "FEED");
                } else {
                    JOptionPane.showMessageDialog(this, "Invalid credentials.");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });
        return panel;
    }

    private JPanel buildFeedPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel feedPanel = new JPanel();
        feedPanel.setLayout(new BoxLayout(feedPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(feedPanel);

        JButton postBtn = new JButton("New Post");
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(postBtn, BorderLayout.SOUTH);

        // Load posts from DB
        try {
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(
                "SELECT p.post_id, u.username, p.content, p.created_at " +
                "FROM posts p JOIN users u ON p.user_id=u.user_id ORDER BY p.created_at DESC");
            while (rs.next()) {
                int postId = rs.getInt("post_id");
                String username = rs.getString("username");
                String content = rs.getString("content");
                String time = rs.getString("created_at");

                JPanel postCard = new JPanel(new BorderLayout());
                JLabel postLabel = new JLabel("<html><b>" + username + "</b>: " + content + "<br><i>" + time + "</i></html>");
                JButton likeBtn = new JButton("Like");
                JButton commentBtn = new JButton("Comment");
                JLabel statusLabel = new JLabel("Likes: 0 | Comments: 0");

                JPanel actionPanel = new JPanel();
                actionPanel.add(likeBtn);
                actionPanel.add(commentBtn);
                actionPanel.add(statusLabel);

                postCard.add(postLabel, BorderLayout.NORTH);
                postCard.add(actionPanel, BorderLayout.SOUTH);

                // Like button action
                likeBtn.addActionListener(e -> {
                    try {
                        PreparedStatement ps = conn.prepareStatement("INSERT INTO likes(user_id, post_id) VALUES (?,?)");
                        ps.setInt(1, currentUserId);
                        ps.setInt(2, postId);
                        ps.executeUpdate();
                        statusLabel.setText("Liked!");
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
                    }
                });

                // Comment button action
                commentBtn.addActionListener(e -> {
                    String comment = JOptionPane.showInputDialog(this, "Enter comment:");
                    if (comment != null && !comment.isEmpty()) {
                        try {
                            PreparedStatement ps = conn.prepareStatement("INSERT INTO comments(user_id, post_id, content) VALUES (?,?,?)");
                            ps.setInt(1, currentUserId);
                            ps.setInt(2, postId);
                            ps.setString(3, comment);
                            ps.executeUpdate();
                            statusLabel.setText("Comment added!");
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
                        }
                    }
                });

                feedPanel.add(postCard);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading feed: " + e.getMessage());
        }

        // Add new post
        postBtn.addActionListener(e -> {
            String content = JOptionPane.showInputDialog(this, "Enter post content:");
            if (content != null && !content.isEmpty()) {
                try {
                    PreparedStatement ps = conn.prepareStatement("INSERT INTO posts(user_id, content) VALUES (?,?)");
                    ps.setInt(1, currentUserId);
                    ps.setString(2, content);
                    ps.executeUpdate();
                    JOptionPane.showMessageDialog(this, "Post added!");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
                }
            }
        });

        return panel;
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new App().setVisible(true));
    }
}
