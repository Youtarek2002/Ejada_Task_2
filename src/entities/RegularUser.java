package entities;

import entities.User;
import enums.Role;
import interfaces.Borrowable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

public class RegularUser extends User implements Borrowable {
    Map<String, Book> books;

    public RegularUser(String id, String name, Map<String, Book> books) {
        super(id, name, Role.REGULAR);
        this.books = books;
    }

    public void borrowBook(String bookId) {
        Book book = books.get(bookId);
        if (book == null) {
            System.out.println("Book not found.");
        } else if (book.getAvailableCopies() <= 0) {
            System.out.println("No available copies.");
        } else if (borrowedBooks.contains(bookId)) {
            System.out.println("You have already borrowed this book.");
        } else {
            borrowedBooks.add(bookId);
            book.decreaseCopies();
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:library.db")) {
                PreparedStatement stmt = conn.prepareStatement("UPDATE books SET availableCopies = ? WHERE id = ?");
                stmt.setInt(1, book.getAvailableCopies());
                stmt.setString(2, book.getId());
                stmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }            System.out.println("Book borrowed successfully.");
        }
    }

    public void returnBook(String bookId) {
        if (!borrowedBooks.contains(bookId)) {
            System.out.println("You haven't borrowed this book.");
        } else {
            borrowedBooks.remove(bookId);
            Book book = books.get(bookId);
            book.increaseCopies();
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:library.db")) {
                PreparedStatement stmt = conn.prepareStatement("UPDATE books SET availableCopies = ? WHERE id = ?");
                stmt.setInt(1, book.getAvailableCopies());
                stmt.setString(2, book.getId());
                stmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }            System.out.println("Book returned successfully.");
        }
    }
}