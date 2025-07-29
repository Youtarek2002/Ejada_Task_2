import entities.Admin;
import entities.RegularUser;
import entities.User;
import entities.Book;
import enums.Role;

import java.sql.*;
import java.util.*;

public class LibraryApp {
    static Map<String, Book> books = new HashMap<>();
    static Map<String, User> users = new HashMap<>();
    static Set<String> genres = new HashSet<>();
    static Scanner scanner = new Scanner(System.in);
    static Connection connection;

    public static void main(String[] args) {
        connectDatabase();
        createTables();
        loadBooksFromDB();
        loadUsersFromDB();
        loadBorrowedBooksFromDB();

        if (users.isEmpty()) {
            Admin defaultAdmin = new Admin("admin", "admin");
            users.put("admin", defaultAdmin);
            saveUserToDB(defaultAdmin);
        }

        while (true) {
            System.out.println("\nLogin (Enter User ID):");
            String userId = scanner.nextLine();
            User user = users.get(userId);
            if (user == null) {
                System.out.println("Invalid user.");
                continue;
            }
            if (user.getRole() == Role.ADMIN) handleAdmin((Admin) user);
            else handleRegularUser((RegularUser) user);
        }
    }

    static void connectDatabase() {
        try {
            String dbHost = System.getenv("DB_HOST");
            String dbPort = System.getenv("DB_PORT");
            String dbName = System.getenv("DB_NAME");
            String dbUser = System.getenv("DB_USER");
            String dbPassword = System.getenv("DB_PASSWORD");

            String connectionUrl = String.format("jdbc:mysql://%s:%s/%s", dbHost, dbPort, dbName);

            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(connectionUrl, dbUser, dbPassword);
            System.out.println("Connected to database successfully.");
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
            System.exit(1);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC Driver not found", e);
        }
    }

    static void createTables() {
        try {
            Statement stmt = connection.createStatement();
            // Create books table if not exists
            stmt.execute("CREATE TABLE IF NOT EXISTS books (" +
                    "id VARCHAR(50) PRIMARY KEY, " +
                    "title VARCHAR(100) NOT NULL, " +
                    "author VARCHAR(100) NOT NULL, " +
                    "genre VARCHAR(50) NOT NULL, " +
                    "availableCopies INT NOT NULL)");

            // Create users table if not exists
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "id VARCHAR(50) PRIMARY KEY, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "role ENUM('ADMIN', 'REGULAR') NOT NULL)");

            // Create userbook table for tracking borrowed books
            stmt.execute("CREATE TABLE IF NOT EXISTS userbook (" +
                    "user_id VARCHAR(50) NOT NULL, " +
                    "book_id VARCHAR(50) NOT NULL, " +
                    "PRIMARY KEY (user_id, book_id), " +
                    "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE)");
        } catch (SQLException e) {
            System.err.println("Error creating tables: " + e.getMessage());
            System.exit(1);
        }
    }

    static void handleAdmin(Admin admin) {
        while (true) {
            System.out.println("\n--- Admin Menu ---");
            System.out.println("1. Add Book\n2. Edit Book\n3. Delete Book\n4. Register User\n5. View All Borrowed Books\n6. Logout");
            String choice = scanner.nextLine();
            switch (choice) {
                case "1" -> addBook();
                case "2" -> editBook();
                case "3" -> deleteBook();
                case "4" -> registerUser();
                case "5" -> viewAllBorrowedBooks();
                case "6" -> { return; }
                default -> System.out.println("Invalid option.");
            }
        }
    }

    static void handleRegularUser(RegularUser user) {
        while (true) {
            System.out.println("\n--- User Menu ---");
            System.out.println("1. View Catalog\n2. Borrow Book\n3. Return Book\n4. View My Borrowed Books\n5. Logout");
            String choice = scanner.nextLine();
            switch (choice) {
                case "1" -> books.values().forEach(System.out::println);
                case "2" -> {
                    System.out.println("Enter Book ID:");
                    String bookId = scanner.nextLine();
                    if (borrowBook(user.getId(), bookId)) {
                        System.out.println("Book borrowed successfully.");
                    } else {
                        System.out.println("Failed to borrow book. It may not be available.");
                    }
                }
                case "3" -> {
                    System.out.println("Enter Book ID:");
                    String bookId = scanner.nextLine();
                    if (returnBook(user.getId(), bookId)) {
                        System.out.println("Book returned successfully.");
                    } else {
                        System.out.println("Failed to return book. You may not have borrowed it.");
                    }
                }
                case "4" -> viewUserBorrowedBooks(user.getId());
                case "5" -> { return; }
                default -> System.out.println("Invalid option.");
            }
        }
    }

    static boolean borrowBook(String userId, String bookId) {
        try {
            // Check if book exists and has available copies
            Book book = books.get(bookId);
            if (book == null || book.getAvailableCopies() <= 0) {
                return false;
            }

            // Start transaction
            connection.setAutoCommit(false);

            // Insert into userbook table
            PreparedStatement insertStmt = connection.prepareStatement(
                    "INSERT INTO userbook (user_id, book_id) VALUES (?, ?)");
            insertStmt.setString(1, userId);
            insertStmt.setString(2, bookId);
            insertStmt.executeUpdate();

            // Update available copies
            PreparedStatement updateStmt = connection.prepareStatement(
                    "UPDATE books SET availableCopies = availableCopies - 1 WHERE id = ?");
            updateStmt.setString(1, bookId);
            updateStmt.executeUpdate();

            // Commit transaction
            connection.commit();
            connection.setAutoCommit(true);

            // Update in-memory data
            book.setAvailableCopies(book.getAvailableCopies() - 1);
            ((RegularUser) users.get(userId)).borrowBook(bookId);

            return true;
        } catch (SQLException e) {
            try {
                connection.rollback();
                connection.setAutoCommit(true);
            } catch (SQLException ex) {
                System.err.println("Error during rollback: " + ex.getMessage());
            }
            System.err.println("Error borrowing book: " + e.getMessage());
            return false;
        }
    }

    static boolean returnBook(String userId, String bookId) {
        try {
            // Check if the user has borrowed this book
            PreparedStatement checkStmt = connection.prepareStatement(
                    "SELECT 1 FROM userbook WHERE user_id = ? AND book_id = ?");
            checkStmt.setString(1, userId);
            checkStmt.setString(2, bookId);
            ResultSet rs = checkStmt.executeQuery();
            if (!rs.next()) {
                return false;
            }

            // Start transaction
            connection.setAutoCommit(false);

            // Delete from userbook table
            PreparedStatement deleteStmt = connection.prepareStatement(
                    "DELETE FROM userbook WHERE user_id = ? AND book_id = ?");
            deleteStmt.setString(1, userId);
            deleteStmt.setString(2, bookId);
            deleteStmt.executeUpdate();

            // Update available copies
            PreparedStatement updateStmt = connection.prepareStatement(
                    "UPDATE books SET availableCopies = availableCopies + 1 WHERE id = ?");
            updateStmt.setString(1, bookId);
            updateStmt.executeUpdate();

            // Commit transaction
            connection.commit();
            connection.setAutoCommit(true);

            // Update in-memory data
            Book book = books.get(bookId);
            if (book != null) {
                book.setAvailableCopies(book.getAvailableCopies() + 1);
            }
            ((RegularUser) users.get(userId)).returnBook(bookId);

            return true;
        } catch (SQLException e) {
            try {
                connection.rollback();
                connection.setAutoCommit(true);
            } catch (SQLException ex) {
                System.err.println("Error during rollback: " + ex.getMessage());
            }
            System.err.println("Error returning book: " + e.getMessage());
            return false;
        }
    }

    static void viewUserBorrowedBooks(String userId) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "SELECT b.* FROM books b JOIN userbook ub ON b.id = ub.book_id WHERE ub.user_id = ?");
            stmt.setString(1, userId);
            ResultSet rs = stmt.executeQuery();

            System.out.println("\n--- Your Borrowed Books ---");
            boolean hasBooks = false;
            while (rs.next()) {
                hasBooks = true;
                System.out.println(new Book(
                        rs.getString("id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getString("genre"),
                        rs.getInt("availableCopies")
                ));
            }
            if (!hasBooks) {
                System.out.println("You haven't borrowed any books yet.");
            }
        } catch (SQLException e) {
            System.err.println("Error fetching borrowed books: " + e.getMessage());
        }
    }

    static void viewAllBorrowedBooks() {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "SELECT u.id as user_id, u.name as user_name, b.id as book_id, b.title as book_title " +
                            "FROM userbook ub " +
                            "JOIN users u ON ub.user_id = u.id " +
                            "JOIN books b ON ub.book_id = b.id " +
                            "ORDER BY u.name");
            ResultSet rs = stmt.executeQuery();

            System.out.println("\n--- All Borrowed Books ---");
            boolean hasBorrowings = false;
            while (rs.next()) {
                hasBorrowings = true;
                System.out.printf("User: %s (%s) - Book: %s (%s)%n",
                        rs.getString("user_name"),
                        rs.getString("user_id"),
                        rs.getString("book_title"),
                        rs.getString("book_id"));
            }
            if (!hasBorrowings) {
                System.out.println("No books are currently borrowed.");
            }
        } catch (SQLException e) {
            System.err.println("Error fetching all borrowed books: " + e.getMessage());
        }
    }

    static void loadBorrowedBooksFromDB() {
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM userbook");
            while (rs.next()) {
                String userId = rs.getString("user_id");
                String bookId = rs.getString("book_id");

                User user = users.get(userId);
                if (user != null && user.getRole() == Role.REGULAR) {
                    ((RegularUser) user).borrowBook(bookId);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading borrowed books: " + e.getMessage());
        }
    }

    static void addBook() {
        System.out.println("Enter Book ID:");
        String id = scanner.nextLine();
        System.out.println("Enter Title:");
        String title = scanner.nextLine();
        System.out.println("Enter Author:");
        String author = scanner.nextLine();
        System.out.println("Enter Genre:");
        String genre = scanner.nextLine();
        System.out.println("Enter Available Copies:");
        int copies = Integer.parseInt(scanner.nextLine());

        Book book = new Book(id, title, author, genre, copies);
        books.put(id, book);
        genres.add(genre);
        saveBookToDB(book);
        System.out.println("Book added successfully.");
    }

    static void editBook() {
        System.out.println("Enter Book ID to edit:");
        String id = scanner.nextLine();
        Book book = books.get(id);
        if (book == null) {
            System.out.println("Book not found.");
            return;
        }

        System.out.println("Enter new Title (current: " + book.getTitle() + "):");
        String title = scanner.nextLine();
        System.out.println("Enter new Author (current: " + book.getAuthor() + "):");
        String author = scanner.nextLine();
        System.out.println("Enter new Genre (current: " + book.getGenre() + "):");
        String genre = scanner.nextLine();
        System.out.println("Enter new Available Copies (current: " + book.getAvailableCopies() + "):");
        int copies = Integer.parseInt(scanner.nextLine());

        book.setTitle(title);
        book.setAuthor(author);
        book.setGenre(genre);
        book.setAvailableCopies(copies);
        genres.add(genre);
        updateBookInDB(book);
        System.out.println("Book updated successfully.");
    }

    static void deleteBook() {
        System.out.println("Enter Book ID to delete:");
        String id = scanner.nextLine();
        if (books.remove(id) != null) {
            deleteBookFromDB(id);
            System.out.println("Book deleted successfully.");
        } else {
            System.out.println("Book not found.");
        }
    }

    static void registerUser() {
        System.out.println("Enter User ID:");
        String id = scanner.nextLine();
        System.out.println("Enter Name:");
        String name = scanner.nextLine();
        System.out.println("Enter Role (ADMIN/REGULAR):");
        Role role = Role.valueOf(scanner.nextLine().toUpperCase());

        User user;
        if (role == Role.ADMIN) {
            user = new Admin(id, name);
        } else {
            user = new RegularUser(id, name, (Map<String, Book>) new ArrayList<>());
        }

        users.put(id, user);
        saveUserToDB(user);
        System.out.println("User registered successfully.");
    }

    static void saveBookToDB(Book book) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO books VALUES (?, ?, ?, ?, ?)");
            stmt.setString(1, book.getId());
            stmt.setString(2, book.getTitle());
            stmt.setString(3, book.getAuthor());
            stmt.setString(4, book.getGenre());
            stmt.setInt(5, book.getAvailableCopies());
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error saving book: " + e.getMessage());
        }
    }

    static void updateBookInDB(Book book) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "UPDATE books SET title=?, author=?, genre=?, availableCopies=? WHERE id=?");
            stmt.setString(1, book.getTitle());
            stmt.setString(2, book.getAuthor());
            stmt.setString(3, book.getGenre());
            stmt.setInt(4, book.getAvailableCopies());
            stmt.setString(5, book.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating book: " + e.getMessage());
        }
    }

    static void deleteBookFromDB(String bookId) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "DELETE FROM books WHERE id=?");
            stmt.setString(1, bookId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error deleting book: " + e.getMessage());
        }
    }

    static void saveUserToDB(User user) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO users VALUES (?, ?, ?)");
            stmt.setString(1, user.getId());
            stmt.setString(2, user.getName());
            stmt.setString(3, user.getRole().name());
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error saving user: " + e.getMessage());
        }
    }

    static void loadBooksFromDB() {
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM books");
            while (rs.next()) {
                Book book = new Book(
                        rs.getString("id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getString("genre"),
                        rs.getInt("availableCopies")
                );
                books.put(book.getId(), book);
                genres.add(book.getGenre());
            }
        } catch (SQLException e) {
            System.err.println("Error loading books: " + e.getMessage());
        }
    }

    static void loadUsersFromDB() {
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM users");
            while (rs.next()) {
                String id = rs.getString("id");
                String name = rs.getString("name");
                Role role = Role.valueOf(rs.getString("role"));

                if (role == Role.ADMIN) {
                    users.put(id, new Admin(id, name));
                } else {
                    users.put(id, new RegularUser(id, name, (Map<String, Book>) new ArrayList<>()));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading users: " + e.getMessage());
        }
    }
}