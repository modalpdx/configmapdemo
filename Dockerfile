# Use the official Maven image for a build stage
FROM maven:3.6.3-openjdk-17 as build

# Copy the project files to the container
COPY ./ /usr/src/app

# Set the working directory
WORKDIR /usr/src/app

# Build the application
RUN mvn clean package

# Use OpenJDK for the application stage
FROM openjdk:17-jdk-slim

# Copy the built jar file from the build stage
COPY --from=build /usr/src/app/target/*.jar /usr/app/app.jar

# Set the working directory for the application
WORKDIR /usr/app

# Command to run the application
CMD ["java", "-jar", "app.jar"]