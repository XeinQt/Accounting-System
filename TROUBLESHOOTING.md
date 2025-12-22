# Troubleshooting Guide

## Common Errors and Solutions

### 1. Error: "ClassNotFoundException: org.sqlite.JDBC"

**Problem:** SQLite JDBC driver is not in the classpath.

**Solution:**
1. Download SQLite JDBC driver from: https://github.com/xerial/sqlite-jdbc/releases
2. Download the latest `sqlite-jdbc-X.X.X.jar` file
3. In NetBeans:
   - Right-click project → Properties → Libraries
   - Click "Add JAR/Folder"
   - Select the downloaded JAR file
   - Click OK
4. Clean and rebuild the project (Right-click project → Clean and Build)

---

### 2. Error: "Cannot find login.fxml" or "Cannot find dashboard.fxml"

**Problem:** FXML files are not being found in the classpath.

**Solution:**
1. Make sure FXML files are in `src/views/` folder:
   - `src/views/login.fxml`
   - `src/views/dashboard.fxml`
2. Clean and rebuild the project
3. Check that the files are included in the build (not excluded)

---

### 3. Error: "NoClassDefFoundError" or "ClassNotFoundException"

**Problem:** Missing dependencies or classpath issues.

**Solution:**
1. Clean the project: Right-click project → Clean
2. Rebuild: Right-click project → Build
3. Check that all required libraries are added:
   - JavaFX SDK (should already be configured)
   - SQLite JDBC driver (needs to be added manually)
4. Verify project properties → Libraries section

---

### 4. Application Starts But Shows Blank Window

**Problem:** FXML loading failed silently or controller not found.

**Solution:**
1. Check the Output window in NetBeans for error messages
2. Verify FXML files have controller declarations:
   - `login.fxml` should have: `fx:controller="controllers.LoginController"`
   - `dashboard.fxml` should have: `fx:controller="controllers.DashboardController"`
3. Make sure all controller classes compile without errors

---

### 5. Database Errors

**Problem:** Database connection or initialization fails.

**Solution:**
1. **If SQLite driver is missing:** See Error #1 above
2. **If database file is locked:**
   - Close any other instances of the application
   - Delete `accounting_system.db` file in project root
   - Run again (database will be recreated)
3. **If permission errors:**
   - Make sure you have write permissions in the project directory
   - Check that the database file isn't read-only

---

### 6. Image Loading Errors

**Problem:** Images referenced in FXML don't exist.

**Solution:**
1. The application will still work without images
2. Image loading is optional - commented out in login.fxml
3. To add images:
   - Place images in `src/images/` folder
   - Uncomment the ImageView in login.fxml if needed

---

### 7. "JavaFX runtime components are missing"

**Problem:** JavaFX SDK not properly configured.

**Solution:**
1. Verify JavaFX SDK path in `nbproject/project.properties`
2. Check that `run.jvmargs` includes:
   ```
   --module-path "C:\JavaFX\javafx-sdk-24.0.2\lib" --add-modules javafx.controls,javafx.fxml
   ```
3. Update the path if your JavaFX SDK is in a different location

---

### 8. Application Doesn't Start at All

**Problem:** Various startup issues.

**Solution Checklist:**
- [ ] SQLite JDBC driver added to libraries
- [ ] Project cleaned and rebuilt
- [ ] All Java files compile without errors
- [ ] FXML files are in `src/views/` folder
- [ ] JavaFX SDK is properly configured
- [ ] Check Output window for specific error messages
- [ ] Try running from command line to see full error stack trace

---

### 9. Login Button Doesn't Work

**Problem:** Controller method not being called.

**Solution:**
1. Verify `login.fxml` has: `fx:controller="controllers.LoginController"`
2. Check that `loginButton` has: `onAction="#handleLogin"`
3. Make sure `LoginController.java` has the `handleLogin()` method
4. Check for compilation errors in LoginController

---

### 10. Build Errors After Clean

**Problem:** Build fails after cleaning.

**Solution:**
1. Close NetBeans
2. Delete the `build` folder manually
3. Delete the `dist` folder if it exists
4. Reopen NetBeans
5. Open the project
6. Right-click project → Clean and Build

---

## How to Check for Errors

1. **NetBeans Output Window:**
   - View → Output (or press Ctrl+4)
   - Look for red error messages

2. **Problems Window:**
   - Window → Problems (or press Ctrl+1)
   - Shows compilation errors

3. **Console Output:**
   - When running, check the console for stack traces
   - Look for "Exception" or "Error" messages

---

## Quick Fix Steps

If nothing works, try this sequence:

1. **Add SQLite JDBC driver** (if not done)
2. **Clean project:** Right-click → Clean
3. **Delete build folder** manually (if it exists)
4. **Rebuild project:** Right-click → Build
5. **Check for errors** in Problems window
6. **Fix any compilation errors**
7. **Run again:** Right-click AccountingSystem.java → Run File

---

## Still Having Issues?

1. Check the exact error message in the Output window
2. Take a screenshot of the error
3. Verify your project structure matches:
   ```
   src/
   ├── accountingsystem/
   │   └── AccountingSystem.java
   ├── controllers/
   │   ├── LoginController.java
   │   └── DashboardController.java
   ├── dao/
   ├── models/
   ├── utils/
   │   └── DatabaseUtil.java
   └── views/
       ├── login.fxml
       └── dashboard.fxml
   ```

