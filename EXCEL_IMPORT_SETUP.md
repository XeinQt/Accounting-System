# Excel Import Setup Guide

## Apache POI Library Required

The Excel import functionality requires Apache POI library. Follow these steps to add it:

### Step 1: Download Apache POI

Download the following JAR files from: https://poi.apache.org/download.html

Required JARs:
- `poi-X.X.X.jar` (Core library)
- `poi-ooxml-X.X.X.jar` (For .xlsx files)
- `poi-scratchpad-X.X.X.jar` (For .xls files - optional but recommended)
- `xmlbeans-X.X.X.jar` (Dependency)
- `commons-compress-X.X.X.jar` (Dependency)
- `commons-collections4-X.X.X.jar` (Dependency)

**Or download the complete binary distribution:**
- Go to: https://poi.apache.org/download.html
- Download: `poi-bin-X.X.X-YYYYMMDD.zip`
- Extract and find the JAR files in the `lib` folder

### Step 2: Add to NetBeans Project

1. **Create a `lib` folder** in your project root (if it doesn't exist)

2. **Copy all JAR files** to the `lib` folder

3. **Add to Project Libraries:**
   - Right-click on your project (`AccountingSystem`)
   - Select **Properties**
   - Go to **Libraries** category
   - Click **Add JAR/Folder...**
   - Select all the POI JAR files from the `lib` folder
   - Click **OK**

### Step 3: Verify Installation

After adding the libraries, clean and rebuild your project:
- Right-click project → **Clean**
- Right-click project → **Build**

## Excel File Format

When importing students from Excel, the file should follow this format:

### Expected Column Order:
1. **Student ID** (Column A) - Required
2. **Full Name** (Column B) - Required
3. **Major** (Column C) - Optional
4. **Year** (Column D) - Optional
5. **Department** (Column E) - Optional
6. **College** (Column F) - Optional

### Example Excel Format:

| Student ID | Full Name | Major | Year | Department | College |
|------------|-----------|-------|------|------------|---------|
| 2024-1234 | Juan Dela Cruz | BSIT-BC | 1 | Computer Science | College of Engineering |
| 2024-1235 | Maria Santos | Business Admin | 2 | Business | College of Business |

### Notes:
- The first row can be a header row (it will be automatically skipped)
- Student ID and Full Name are required fields
- Other fields are optional
- If a student with the same Student ID already exists, it will be skipped
- The import will use the first available School Year as default

## Using Excel Import

1. **Click "Import Student" button** on the Students page
2. **Select an Excel file** (.xlsx or .xls format)
3. **Wait for import to complete**
4. **Check the success message** showing how many students were imported
5. **Refresh the table** to see imported students

## Troubleshooting

### Error: "Apache POI library not found"
- Make sure you've added all required POI JAR files to project libraries
- Clean and rebuild the project
- Restart NetBeans if needed

### Error: "Cannot read Excel file"
- Check that the file is not open in another program
- Verify the file format (.xlsx or .xls)
- Make sure the file is not corrupted

### Imported 0 students
- Check that Student ID and Full Name columns are not empty
- Verify the Excel file format matches the expected format
- Check if students with those IDs already exist (they will be skipped)

### Import partially successful
- Some rows may have errors (check console for details)
- Invalid rows are skipped automatically
- The import continues with remaining rows

## Alternative: Manual Entry

If you prefer not to use Excel import, you can always add students manually using the "Add" button on the Students page.

