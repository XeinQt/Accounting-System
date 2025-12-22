# How to Run the DORPAY Accounting System

## Prerequisites

1. **Java 8 or higher** (JDK 25 is configured in your project)
2. **JavaFX SDK** (already configured in your project)
3. **SQLite JDBC Driver** (needs to be added)

## Step 1: Add SQLite JDBC Driver

### Option A: Using NetBeans (Recommended)

1. **Download SQLite JDBC Driver:**
   - Go to: https://github.com/xerial/sqlite-jdbc/releases
   - Download the latest `sqlite-jdbc-X.X.X.jar` file (e.g., `sqlite-jdbc-3.44.1.0.jar`)

2. **Add to Project:**
   - In NetBeans, right-click on your project (`AccountingSystem`)
   - Select **Properties**
   - Go to **Libraries** category
   - Click **Add JAR/Folder...** button
   - Browse and select the downloaded `sqlite-jdbc-X.X.X.jar` file
   - Click **OK**

### Option B: Manual Setup

1. Create a `lib` folder in your project root (if it doesn't exist)
2. Copy the `sqlite-jdbc-X.X.X.jar` file to the `lib` folder
3. In NetBeans, right-click on the project → Properties → Libraries → Add JAR/Folder → Select the JAR from `lib` folder

## Step 2: Run the Application

### Method 1: Run from NetBeans (Easiest)

1. **Open the main class:**
   - Navigate to `src/accountingsystem/AccountingSystem.java`
   - Right-click on the file
   - Select **Run File** (or press `Shift + F6`)

   **OR**

2. **Run the project:**
   - Right-click on the project name (`AccountingSystem`) in the Projects window
   - Select **Run** (or press `F6`)

### Method 2: Run from Command Line

1. **Open Command Prompt or PowerShell** in your project directory

2. **Compile the project:**
   ```bash
   javac --module-path "C:\JavaFX\javafx-sdk-24.0.2\lib" --add-modules javafx.controls,javafx.fxml -d build/classes -cp "lib/sqlite-jdbc-X.X.X.jar" src/accountingsystem/*.java src/controllers/*.java src/dao/*.java src/models/*.java src/utils/*.java
   ```

3. **Run the application:**
   ```bash
   java --module-path "C:\JavaFX\javafx-sdk-24.0.2\lib" --add-modules javafx.controls,javafx.fxml -cp "build/classes;lib/sqlite-jdbc-X.X.X.jar" accountingsystem.AccountingSystem
   ```

   **Note:** Replace `X.X.X` with the actual version number of your SQLite JDBC driver.

## Step 3: Login

When the application starts, you'll see the login screen. Use these default credentials:

- **Username:** `admin`
- **Password:** `admin123`

After successful login, you'll be taken to the dashboard.

## Troubleshooting

### Error: "ClassNotFoundException: org.sqlite.JDBC"
- **Solution:** Make sure you've added the SQLite JDBC driver JAR to your project libraries (Step 1)

### Error: "Could not find or load main class"
- **Solution:** Clean and rebuild the project:
  - Right-click project → **Clean**
  - Then right-click project → **Build**
  - Then run again

### Error: "Cannot find resource views/login.fxml"
- **Solution:** Make sure the FXML files are in the `src/views/` folder and the project structure is correct

### Database Error
- The database file (`accounting_system.db`) will be created automatically in your project root directory on first run
- If you get database errors, delete the `accounting_system.db` file and run again (it will recreate)

### JavaFX Module Error
- Make sure JavaFX SDK is properly configured in your project
- Check that the JavaFX path in `nbproject/project.properties` matches your JavaFX installation

## Project Structure

Make sure your project structure looks like this:

```
AccountingSystem/
├── src/
│   ├── accountingsystem/
│   │   └── AccountingSystem.java  ← Main class to run
│   ├── controllers/
│   ├── dao/
│   ├── models/
│   ├── utils/
│   │   └── DatabaseUtil.java
│   └── views/
│       ├── login.fxml
│       └── dashboard.fxml
├── lib/
│   └── sqlite-jdbc-X.X.X.jar  ← Add this file
└── accounting_system.db  ← Created automatically on first run
```

## Quick Start Checklist

- [ ] SQLite JDBC driver downloaded and added to project libraries
- [ ] Project is clean and built successfully
- [ ] Main class `AccountingSystem.java` is open or selected
- [ ] Press `F6` or right-click → Run
- [ ] Login with `admin` / `admin123`

## Need Help?

If you encounter any issues:
1. Check the NetBeans Output window for error messages
2. Verify all files are in the correct locations
3. Make sure JavaFX SDK path is correct in project properties
4. Ensure SQLite JDBC driver is in the classpath

