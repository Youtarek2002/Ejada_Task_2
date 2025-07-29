# Use lightweight Java image
FROM openjdk:17-jdk-slim

# Set working directory
WORKDIR /app

# Copy your JAR file
COPY LibraryApp-1.0.jar app.jar

# Environment variables for DB
ENV DB_HOST=db
ENV DB_PORT=3306
ENV DB_USER=appuser
ENV DB_PASSWORD=apppassword
ENV DB_NAME=library_db

# Run the application
CMD ["java", "-jar", "app.jar"]
