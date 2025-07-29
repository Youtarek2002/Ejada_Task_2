package entities;

public class Book {
    private String id;
    private String title;
    private String author;
    private String genre;
    private int availableCopies;

    public Book(String id, String title, String author, String genre, int availableCopies) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.genre = genre;
        this.availableCopies = availableCopies;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }
    public void setTitle(String title) { this.title = title; }
    public void setAuthor(String author) { this.author = author; }
    public void setAvailableCopies(int availableCopies) { this.availableCopies = availableCopies; }

    public int getAvailableCopies() { return availableCopies; }

    public void decreaseCopies() { availableCopies--; }
    public void increaseCopies() { availableCopies++; }

    @Override
    public String toString() {
        return id + ": " + title + " by " + author + " [" + genre + "] - Available: " + availableCopies;
    }
}