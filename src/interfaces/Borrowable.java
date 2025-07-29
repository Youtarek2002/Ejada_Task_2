package interfaces;

public interface Borrowable {
    void borrowBook(String bookId);
    void returnBook(String bookId);
}
