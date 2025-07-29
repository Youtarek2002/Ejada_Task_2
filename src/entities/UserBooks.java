package entities;

public class UserBooks {
    private int id;
    private int userId;
    private int bookId;

    public UserBooks(int id, int userId, int bookId) {
        this.id = id;
        this.userId = userId;
        this.bookId = bookId;
    }
}
