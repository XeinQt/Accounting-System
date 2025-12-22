package utils;

import dao.PromissoryNoteDAO;
import models.AcademicYearSemesterBalance;
import models.PromissoryNoteView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

/**
 * Utility class for generating Word-compatible documents.
 * Creates RTF (Rich Text Format) files that can be opened in Microsoft Word.
 */
public class WordDocumentGenerator {
    
    /**
     * Escape RTF special characters in text
     * RTF requires escaping: {, }, \
     */
    private static String escapeRtf(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\")
                   .replace("{", "\\{")
                   .replace("}", "\\}");
    }
    
    /**
     * Convert image file to RTF hex format for embedding
     */
    private static String imageToRtfHex(File imageFile) {
        return imageToRtfHex(imageFile, false);
    }
    
    
    /**
     * Convert image file to RTF hex format for embedding
     * @param imageFile The image file to convert
     * @param isHeaderOrFooter If true, uses consistent width for header/footer matching
     */
    private static String imageToRtfHex(File imageFile, boolean isHeaderOrFooter) {
        try {
            BufferedImage image = ImageIO.read(imageFile);
            if (image == null) {
                return null;
            }
            
            // Read image bytes
            byte[] imageBytes;
            try (FileInputStream fis = new FileInputStream(imageFile)) {
                imageBytes = new byte[(int) imageFile.length()];
                fis.read(imageBytes);
            }
            
            // Convert to hex string
            StringBuilder hex = new StringBuilder();
            for (byte b : imageBytes) {
                hex.append(String.format("%02x", b));
            }
            
            // Determine image format
            String format = "jpeg";
            String fileName = imageFile.getName().toLowerCase();
            if (fileName.endsWith(".png")) {
                format = "png";
            } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
                format = "jpeg";
            }
            
            // Get image dimensions
            int width = image.getWidth();
            int height = image.getHeight();
            
            // For header/footer, use consistent width so they match
            // A4 width ~8.27 inches = 11900 twips, use maximum width for both
            int maxWidthTwips = 11500; // Increased width - almost full page width for both header and footer
            double aspectRatio = (double) height / width;
            int widthTwips = maxWidthTwips;
            int heightTwips = (int) (maxWidthTwips * aspectRatio);
            
            // Increased height limit for header/footer (max 3.5 inches = 5040 twips)
            // Allow more vertical space for better visibility
            int maxHeightTwips = 5040; // Increased from 3600 to 5040 twips
            if (heightTwips > maxHeightTwips) {
                heightTwips = maxHeightTwips;
                widthTwips = (int) (heightTwips / aspectRatio);
            }
            
            // Ensure header and footer have the same width and height
            if (isHeaderOrFooter) {
                widthTwips = maxWidthTwips; // Force same width
                heightTwips = (int) (maxWidthTwips * aspectRatio);
                // Re-limit height if needed, but use increased limit
                if (heightTwips > maxHeightTwips) {
                    heightTwips = maxHeightTwips;
                }
            }
            
            // RTF picture format
            StringBuilder rtfPict = new StringBuilder();
            rtfPict.append("{\\pict\\");
            if (format.equals("png")) {
                rtfPict.append("pngblip");
            } else {
                rtfPict.append("jpegblip");
            }
            rtfPict.append("\\picw").append(width).append("\\pich").append(height);
            rtfPict.append("\\picwgoal").append(widthTwips).append("\\pichgoal").append(heightTwips);
            rtfPict.append(" ");
            rtfPict.append(hex.toString());
            rtfPict.append("}");
            
            return rtfPict.toString();
        } catch (Exception e) {
            System.err.println("Error converting image to RTF: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Find image file in common locations, including the images package
     */
    private static File findImageFile(String fileName) {
        String userDir = System.getProperty("user.dir");
        
        // Try to load from classpath first (for packaged applications)
        try {
            java.net.URL resourceUrl = WordDocumentGenerator.class.getClassLoader().getResource("images/" + fileName);
            if (resourceUrl != null) {
                File imageFile = new File(resourceUrl.toURI());
                if (imageFile.exists() && imageFile.isFile()) {
                    System.out.println("Found image file from classpath: " + imageFile.getAbsolutePath());
                    return imageFile;
                }
            }
        } catch (Exception e) {
            // Continue to file system search
        }
        
        // Common locations to look for image, including images package
        String[] possiblePaths = {
            userDir + "/src/images/" + fileName,           // images package (source)
            userDir + "/build/classes/images/" + fileName, // images package (compiled)
            userDir + "/images/" + fileName,               // images folder at root
            userDir + "/src/accountingsystem/images/" + fileName,  // accountingsystem/images package
            userDir + "/assets/" + fileName,
            userDir + "/src/assets/" + fileName,
            userDir + "/" + fileName,
            "src/images/" + fileName,                      // relative path to images package
            "images/" + fileName,                         // relative path to images folder
            "assets/" + fileName,
            "src/assets/" + fileName,
            fileName
        };
        
        for (String path : possiblePaths) {
            File imageFile = new File(path);
            if (imageFile.exists() && imageFile.isFile()) {
                System.out.println("Found image file: " + imageFile.getAbsolutePath());
                return imageFile;
            }
        }
        
        System.out.println("Image file not found: " + fileName);
        return null;
    }
    
    /**
     * Find header image file
     */
    private static File findHeaderImage() {
        // Try university_letterhead.png first (the new image)
        File header = findImageFile("university_letterhead.png");
        if (header == null) {
            header = findImageFile("header.png");
        }
        if (header == null) {
            header = findImageFile("header.jpg");
        }
        return header;
    }
    
    /**
     * Find footer image file
     */
    private static File findFooterImage() {
        // Try university_footer.png first (the new image)
        File footer = findImageFile("university_footer.png");
        if (footer == null) {
            footer = findImageFile("footer.png");
        }
        if (footer == null) {
            footer = findImageFile("footer.jpg");
        }
        return footer;
    }
    
    /**
     * Generate a promissory note document (RTF format, compatible with Word)
     * Returns the File object if successful, null otherwise
     * Format: A4 size with university header and footer
     */
    public static File generatePromissoryNoteFile(PromissoryNoteView student, LocalDate agreedPaymentDate, Stage stage) {
        try {
            // Create a temporary file in the system temp directory
            String tempDir = System.getProperty("java.io.tmpdir");
            String fileName = "PromissoryNote_" + student.getStudentNumber() + "_" + 
                            System.currentTimeMillis() + ".rtf";
            File file = new File(tempDir, fileName);
            
            // If file already exists, delete it
            if (file.exists()) {
                file.delete();
            }
            
            // Generate RTF content with A4 size and formatting
            StringBuilder rtf = new StringBuilder();
            
            // RTF Header with color table and fonts - Arial as default font
            rtf.append("{\\rtf1\\ansi\\ansicpg1252\\deff0\\deflang1033\n");
            rtf.append("{\\fonttbl{\\f0\\fnil\\fcharset0 Arial;}{\\f1\\fnil\\fcharset0 Arial;}{\\f2\\fnil\\fcharset0 Arial;}}\n");
            rtf.append("{\\colortbl ;\\red0\\green0\\blue255;\\red0\\green0\\blue0;\\red128\\green128\\blue128;}\n"); // Blue, Black, and Gray
            rtf.append("\\deff0\\f0\\fs20\n"); // Set Arial as default font
            
            // A4 page size: 8.27 x 11.69 inches = 595 x 842 points
            // Margins for content - header/footer are in document flow
            rtf.append("\\paperw11900\\paperh16840\\margl720\\margr720\\margt0\\margb0\n");
            rtf.append("\\viewkind1\\viewscale100\n");
            
            // Header Section - Place at ABSOLUTE TOP of document
            File headerImage = findHeaderImage();
            if (headerImage != null) {
                String headerRtf = imageToRtfBackground(headerImage, true);
                if (headerRtf != null) {
                    rtf.append("\\pard\\qc");
                    rtf.append(headerRtf);
                    rtf.append("\\par\n");
                    rtf.append("\\sl-50\\slmult0\\par\n"); // Minimal spacing after header
                }
            } else {
                // Fallback if header image not found - match design exactly
                rtf.append("\\pard\\qc\\f0\\fs16 Republic of the Philippines\\par\n");
                rtf.append("\\pard\\qc\\brdrb\\brdrs\\brdrw10\\brdrcf1\\brsp10\\par\n"); // Blue line
                rtf.append("\\pard\\qc\\f0\\fs36\\b\\cf2 DAVAO ORIENTAL\\par\n");
                rtf.append("\\pard\\qc\\f0\\fs32\\b\\cf2 STATE UNIVERSITY\\par\n");
                rtf.append("\\pard\\qc\\f0\\fs14\\i A university of excellence, innovation, and inclusion\\par\n");
                rtf.append("\\pard\\qc\\brdrb\\brdrs\\brdrw10\\brdrcf1\\brsp10\\par\n"); // Blue line
                rtf.append("\\sl-50\\slmult0\\par\n");
            }
            
            // Main content area - Compact spacing to fit on one page
            rtf.append("\\pard\\ql\\par\n"); // Left alignment for content (letter format)
            rtf.append("\\sl60\\slmult0\\par\n"); // Minimal spacing after header
            
            // Date - Left-aligned (format: DECEMBER, 2025) - Arial font
            String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM, yyyy")).toUpperCase();
            rtf.append("\\pard\\ql\\f0\\fs18 ").append(escapeRtf(currentDate)).append("\\par\n");
            rtf.append("\\sl-60\\slmult0\\par\n"); // Reduced spacing
            
            // Recipient Information - centered, match image format (bold, uppercase, Arial)
            String studentName = student.getStudentName() != null ? student.getStudentName().toUpperCase() : "";
            String address = student.getCollege() != null ? student.getCollege() : "Davao Oriental State University-Banaybanay Campus";
            rtf.append("\\pard\\ql\\f0\\fs18\\b ").append(escapeRtf(studentName)).append("\\par\n");
            // Format address with italicized "University-Banaybanay" part
            if (address.contains("University-Banaybanay")) {
                String[] parts = address.split("University-Banaybanay");
                rtf.append("\\pard\\ql\\f0\\fs18 ").append(escapeRtf(parts[0])).append("\\i University-Banaybanay\\i0");
                if (parts.length > 1) {
                    rtf.append(escapeRtf(parts[1]));
                }
                rtf.append("\\par\n");
            } else {
                rtf.append("\\pard\\ql\\f0\\fs18 ").append(escapeRtf(address)).append("\\par\n");
            }
            rtf.append("\\sl-60\\slmult0\\par\n"); // Reduced spacing
            
            // Salutation - Arial font, centered
            String lastName = "";
            if (studentName != null && !studentName.isEmpty()) {
                String[] nameParts = studentName.split(" ");
                if (nameParts.length > 0) {
                    lastName = nameParts[nameParts.length - 1];
                }
            }
            rtf.append("\\pard\\ql\\f0\\fs18 Dear Ms./Mr. ").append(escapeRtf(lastName)).append(",\\par\n");
            rtf.append("\\sl-60\\slmult0\\par\n"); // Reduced spacing
            rtf.append("\\pard\\ql\\f0\\fs18 Greetings of peace.\\par\n");
            rtf.append("\\sl-60\\slmult0\\par\n"); // Reduced spacing
            
            // Body Paragraph - justified text, Arial font, bold key phrases, centered
            rtf.append("\\pard\\ql\\qj\\f0\\fs18 This is to inform you that you have an existing \\b unpaid balance stated in your promissory letter\\b0  during your stay at the university. Below is the table showing the academic year/s, semester/s, and the total amount of unpaid balances for tuition and miscellaneous fees.\\par\n");
            rtf.append("\\sl-60\\slmult0\\par\n"); // Reduced spacing
            
            // Get unpaid balances by academic year and semester
            PromissoryNoteDAO promissoryNoteDAO = new PromissoryNoteDAO();
            List<AcademicYearSemesterBalance> balances = promissoryNoteDAO.getUnpaidBalancesByAcademicYearAndSemester(student.getStudentId());
            
            // If no breakdown available, use total balance
            if (balances.isEmpty()) {
                AcademicYearSemesterBalance totalBalance = new AcademicYearSemesterBalance(
                    LocalDate.now().getYear() + "", 
                    "1st", 
                    student.getRemainingBalance()
                );
                balances.add(totalBalance);
            }
            
            // Table with Academic Year (AY), Semester, and Amount (PhP) - centered table, Arial font
            // Table header row - bold headers, centered in cells (letter format)
            rtf.append("\\pard\\qc\\trowd\\trgaph54\\trleft-54\\trbrdrt\\brdrw10\\brdrs\\trbrdrl\\brdrw10\\brdrs\\trbrdrr\\brdrw10\\brdrs\\trbrdrb\\brdrw10\\brdrs\n");
            rtf.append("\\cellx3000\\cellx6000\\cellx9000\n");
            rtf.append("\\intbl\\pard\\qc\\f0\\fs18\\b Academic Year (AY)\\cell\n");
            rtf.append("\\intbl\\pard\\qc\\f0\\fs18\\b Semester\\cell\n");
            rtf.append("\\intbl\\pard\\qc\\f0\\fs18\\b Amount (PhP)\\cell\n");
            rtf.append("\\row\n");
            
            // Data rows - Arial font, regular weight, left-aligned for text, right-aligned for amounts (letter format)
            double totalAmount = 0.0;
            for (AcademicYearSemesterBalance balance : balances) {
                rtf.append("\\trowd\\trgaph54\\trleft-54\\trbrdrt\\brdrw10\\brdrs\\trbrdrl\\brdrw10\\brdrs\\trbrdrr\\brdrw10\\brdrs\\trbrdrb\\brdrw10\\brdrs\n");
                rtf.append("\\cellx3000\\cellx6000\\cellx9000\n");
                rtf.append("\\intbl\\pard\\ql\\f0\\fs18 ").append(escapeRtf(balance.getAcademicYear() != null ? balance.getAcademicYear() : "")).append("\\cell\n");
                rtf.append("\\intbl\\pard\\ql\\f0\\fs18 ").append(escapeRtf(balance.getSemester() != null ? balance.getSemester() : "")).append("\\cell\n");
                String amountStr = String.format("%,.2f", balance.getAmount());
                rtf.append("\\intbl\\pard\\qr\\f0\\fs18 ").append(escapeRtf(amountStr)).append("\\cell\n");
                rtf.append("\\row\n");
                totalAmount += balance.getAmount();
            }
            
            // Total row - bold TOTAL (left-aligned in first cell) and amount (right-aligned in last cell)
            rtf.append("\\trowd\\trgaph54\\trleft-54\\trbrdrt\\brdrw10\\brdrs\\trbrdrl\\brdrw10\\brdrs\\trbrdrr\\brdrw10\\brdrs\\trbrdrb\\brdrw10\\brdrs\n");
            rtf.append("\\cellx3000\\cellx6000\\cellx9000\n");
            rtf.append("\\intbl\\pard\\ql\\f0\\fs18\\b TOTAL\\cell\n");
            rtf.append("\\intbl\\pard\\qc\\f0\\fs18\\b \\cell\n");
            String totalAmountStr = String.format("%,.2f", totalAmount);
            rtf.append("\\intbl\\pard\\qr\\f0\\fs18\\b ").append(escapeRtf(totalAmountStr)).append("\\cell\n");
            rtf.append("\\row\n");
            rtf.append("\\pard\\ql\\par\n"); // Back to left alignment
            rtf.append("\\sl-60\\slmult0\\par\n"); // Reduced spacing
            
            // Settlement paragraph - justified text, Arial font, bold key dates, left-aligned
            String promissoryLetterDate = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"));
            String settlementDate = agreedPaymentDate.format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"));
            rtf.append("\\pard\\ql\\qj\\f0\\fs18 In this regard, we are demanding the settlement of your outstanding balance mentioned in your \\b Promissory Letter\\b0  dated \\b ").append(escapeRtf(promissoryLetterDate)).append("\\b0 . The settlement should be done on or before \\b ").append(escapeRtf(settlementDate)).append("\\b0 .\\par\n");
            rtf.append("\\sl-60\\slmult0\\par\n"); // Reduced spacing
            
            // Closing paragraph - justified text, Arial font, bold "Accounting Office", left-aligned
            rtf.append("\\pard\\ql\\qj\\f0\\fs18 This letter serves as your notice. We are looking forward to your immediate action on this matter. For clarifications, please visit the \\b Accounting Office\\b0 .\\par\n");
            rtf.append("\\sl-60\\slmult0\\par\n"); // Reduced spacing
            
            // Signatures - Arial font, bold names, left-aligned
            rtf.append("\\pard\\ql\\f0\\fs18 Respectfully yours,\\par\n");
            rtf.append("\\sl-60\\slmult0\\par\n"); // Reduced spacing
            rtf.append("\\pard\\ql\\f0\\fs18\\b MARICHU P. BERNAL\\par\n");
            rtf.append("\\pard\\ql\\f0\\fs18 Student Accounts In-Charge\\par\n");
            rtf.append("\\sl-60\\slmult0\\par\n"); // Reduced spacing
            rtf.append("\\pard\\ql\\f0\\fs18 Noted by:\\par\n");
            rtf.append("\\sl-60\\slmult0\\par\n"); // Reduced spacing
            rtf.append("\\pard\\ql\\f0\\fs18\\b MARIA MICHELE O. CHATTO, MPA\\par\n");
            rtf.append("\\pard\\ql\\f0\\fs18 Administrative Officer V\\par\n");
            
            // Minimal spacing before footer to fit on one page
            rtf.append("\\sl120\\slmult0\\par\n"); // Reduced spacing to push footer down
            rtf.append("\\par\\par\n"); // Minimal additional spacing
            
            // Footer Section - Place at ABSOLUTE BOTTOM of document
            File footerImage = findFooterImage();
            if (footerImage != null) {
                String footerRtf = imageToRtfBackground(footerImage, true);
                if (footerRtf != null) {
                    rtf.append("\\pard\\qc");
                    rtf.append(footerRtf);
                    rtf.append("\\par\n");
                }
            } else {
                // Fallback if footer image not found - use text footer
                rtf.append("\\pard\\qc\\brdrb\\brdrs\\brdrw15\\brdrcf1\\brsp20 \\par\n");
                rtf.append("\\trowd\\trgaph108\\trleft-108\n");
                rtf.append("\\cellx4000\\cellx8000\\cellx12000\n");
                rtf.append("\\intbl\\pard\\ql\\f0\\fs12 Davao Oriental State University\\par\n");
                rtf.append("\\f0\\fs12 Guang-guang, Dahican, City of Mati,\\par\n");
                rtf.append("\\f0\\fs12 Davao Oriental, 8200\\par\n");
                rtf.append("\\f0\\fs12 Republic of the Philippines\\cell\n");
                rtf.append("\\intbl\\pard\\ql\\f0\\fs12 website: www.dorsu.edu.ph\\par\n");
                rtf.append("\\f0\\fs12 phone: +63 (087)3883 195\\par\n");
                rtf.append("\\f0\\fs12 e-mail: op@dorsu.edu.ph\\par\n");
                rtf.append("\\f0\\fs12 Facebook: @dorsuofficial\\cell\n");
                rtf.append("\\intbl\\pard\\qr\\f0\\fs10\\b SOCOTEC\\par\n");
                rtf.append("\\f0\\fs10\\b ISO 9001\\par\n");
                rtf.append("\\par\n");
                rtf.append("\\f0\\fs9\\b PAB ACCREDITED\\par\n");
                rtf.append("\\f0\\fs9\\b CERTIFICATION BODY\\par\n");
                rtf.append("\\f0\\fs8\\b MS001\\cell\n");
                rtf.append("\\row\n");
            }
            rtf.append("\\pard\\par\n");
            
            rtf.append("}");
            
            // Write to file with proper encoding
            try (PrintWriter writer = new PrintWriter(new FileOutputStream(file), true, java.nio.charset.StandardCharsets.ISO_8859_1)) {
                writer.print(rtf.toString());
                writer.flush();
            }
            
            return file;
        } catch (Exception e) {
            System.err.println("Error generating document: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Convert image to RTF positioned as background - WPS Writer compatible
     * Uses simple approach with negative spacing to create background effect
     * @param imageFile The image file
     * @param isHeaderOrFooter If true, uses consistent width
     * @return RTF string with image (to be placed before content)
     */
    private static String imageToRtfBackground(File imageFile, boolean isHeaderOrFooter) {
        try {
            BufferedImage image = ImageIO.read(imageFile);
            if (image == null) {
                return null;
            }
            
            // Read image bytes
            byte[] imageBytes;
            try (FileInputStream fis = new FileInputStream(imageFile)) {
                imageBytes = new byte[(int) imageFile.length()];
                fis.read(imageBytes);
            }
            
            // Convert to hex string
            StringBuilder hex = new StringBuilder();
            for (byte b : imageBytes) {
                hex.append(String.format("%02x", b));
            }
            
            // Determine image format
            String format = "jpeg";
            String fileName = imageFile.getName().toLowerCase();
            if (fileName.endsWith(".png")) {
                format = "png";
            } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
                format = "jpeg";
            }
            
            // Get image dimensions
            int width = image.getWidth();
            int height = image.getHeight();
            
            // For header/footer, use consistent width
            int maxWidthTwips = 11500;
            double aspectRatio = (double) height / width;
            int widthTwips = maxWidthTwips;
            int heightTwips = (int) (maxWidthTwips * aspectRatio);
            int maxHeightTwips = 5040;
            if (heightTwips > maxHeightTwips) {
                heightTwips = maxHeightTwips;
                widthTwips = (int) (heightTwips / aspectRatio);
            }
            
            if (isHeaderOrFooter) {
                widthTwips = maxWidthTwips;
                heightTwips = (int) (maxWidthTwips * aspectRatio);
                if (heightTwips > maxHeightTwips) {
                    heightTwips = maxHeightTwips;
                }
            }
            
            // Simple RTF image format - WPS Writer compatible
            StringBuilder rtfImage = new StringBuilder();
            rtfImage.append("\\pard\\qc");
            rtfImage.append("{\\pict\\");
            if (format.equals("png")) {
                rtfImage.append("pngblip");
            } else {
                rtfImage.append("jpegblip");
            }
            rtfImage.append("\\picw").append(width).append("\\pich").append(height);
            rtfImage.append("\\picwgoal").append(widthTwips).append("\\pichgoal").append(heightTwips);
            rtfImage.append(" ");
            rtfImage.append(hex.toString());
            rtfImage.append("}");
            rtfImage.append("\\par");
            
            return rtfImage.toString();
        } catch (Exception e) {
            System.err.println("Error creating background image: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Generate a letter document (RTF format, compatible with Word)
     * Header and footer are positioned behind text as background elements
     * Content is centered on the page
     * @param recipientName Name of the recipient
     * @param recipientAddress Address of the recipient
     * @param letterContent The main body content of the letter
     * @param stage JavaFX stage for file operations
     * @return File object if successful, null otherwise
     */
    public static File generateLetterFile(String recipientName, String recipientAddress, String letterContent, Stage stage) {
        try {
            // Create a temporary file in the system temp directory
            String tempDir = System.getProperty("java.io.tmpdir");
            String fileName = "Letter_" + System.currentTimeMillis() + ".rtf";
            File file = new File(tempDir, fileName);
            
            // If file already exists, delete it
            if (file.exists()) {
                file.delete();
            }
            
            // Generate RTF content with A4 size and formatting
            StringBuilder rtf = new StringBuilder();
            
            // RTF Header with color table and fonts - Arial as default font
            rtf.append("{\\rtf1\\ansi\\ansicpg1252\\deff0\\deflang1033\n");
            rtf.append("{\\fonttbl{\\f0\\fnil\\fcharset0 Arial;}{\\f1\\fnil\\fcharset0 Arial;}{\\f2\\fnil\\fcharset0 Arial;}}\n");
            rtf.append("{\\colortbl ;\\red0\\green0\\blue255;\\red0\\green0\\blue0;\\red128\\green128\\blue128;}\n");
            rtf.append("\\deff0\\f0\\fs20\n");
            
            // A4 page size with margins for content - header/footer are in document flow
            rtf.append("\\paperw11900\\paperh16840\\margl720\\margr720\\margt0\\margb0\n");
            rtf.append("\\viewkind1\\viewscale100\n");
            
            // Header Section - Place at top of document (not using RTF header command to avoid UI elements)
            File headerImage = findHeaderImage();
            if (headerImage != null) {
                String headerRtf = imageToRtfBackground(headerImage, true);
                if (headerRtf != null) {
                    rtf.append("\\pard\\qc");
                    rtf.append(headerRtf);
                    rtf.append("\\par\n");
                    rtf.append("\\sl-200\\slmult0\\par\n"); // Negative spacing to bring content up
                }
            } else {
                // Fallback if header image not found
                rtf.append("\\pard\\qc\\f0\\fs16 Republic of the Philippines\\par\n");
                rtf.append("\\pard\\qc\\brdrb\\brdrs\\brdrw10\\brdrcf1\\brsp10\\par\n"); // Blue line
                rtf.append("\\pard\\qc\\f0\\fs36\\b\\cf2 DAVAO ORIENTAL\\par\n");
                rtf.append("\\pard\\qc\\f0\\fs32\\b\\cf2 STATE UNIVERSITY\\par\n");
                rtf.append("\\pard\\qc\\f0\\fs14\\i A university of excellence, innovation, and inclusion\\par\n");
                rtf.append("\\pard\\qc\\brdrb\\brdrs\\brdrw10\\brdrcf1\\brsp10\\par\n"); // Blue line
                rtf.append("\\sl-200\\slmult0\\par\n");
            }
            
            // Main content area - CENTERED in the middle
            rtf.append("\\pard\\qc\\par\n");
            rtf.append("\\sl120\\slmult0\\par\n");
            
            // Date - Centered with three dashes
            String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM, yyyy")).toUpperCase();
            rtf.append("\\pard\\qc\\f0\\fs18---").append(escapeRtf(currentDate)).append("\\par\n");
            rtf.append("\\sl-120\\slmult0\\par\n");
            
            // Recipient Information - centered
            if (recipientName != null && !recipientName.isEmpty()) {
                rtf.append("\\pard\\qc\\f0\\fs18\\b ").append(escapeRtf(recipientName.toUpperCase())).append("\\par\n");
            }
            if (recipientAddress != null && !recipientAddress.isEmpty()) {
                rtf.append("\\pard\\qc\\f0\\fs18 ").append(escapeRtf(recipientAddress)).append("\\par\n");
            }
            rtf.append("\\sl-120\\slmult0\\par\n");
            
            // Salutation - centered
            String lastName = "";
            if (recipientName != null && !recipientName.isEmpty()) {
                String[] nameParts = recipientName.split(" ");
                if (nameParts.length > 0) {
                    lastName = nameParts[nameParts.length - 1];
                }
            }
            rtf.append("\\pard\\qc\\f0\\fs18 Dear Ms./Mr. ").append(escapeRtf(lastName)).append(",\\par\n");
            rtf.append("\\sl-120\\slmult0\\par\n");
            rtf.append("\\pard\\qc\\f0\\fs18 Greetings of peace.\\par\n");
            rtf.append("\\sl-120\\slmult0\\par\n");
            
            // Letter Content - justified text, centered block
            if (letterContent != null && !letterContent.isEmpty()) {
                // Split content into paragraphs if it contains newlines
                String[] paragraphs = letterContent.split("\\n\\n|\\r\\n\\r\\n");
                for (String paragraph : paragraphs) {
                    if (paragraph.trim().isEmpty()) continue;
                    rtf.append("\\pard\\qc\\qj\\f0\\fs18 ").append(escapeRtf(paragraph.trim())).append("\\par\n");
                    rtf.append("\\sl-120\\slmult0\\par\n");
                }
            }
            
            // Closing - centered
            rtf.append("\\pard\\qc\\f0\\fs18 Respectfully yours,\\par\n");
            rtf.append("\\sl-120\\slmult0\\par\n");
            rtf.append("\\pard\\qc\\f0\\fs18\\b MARICHU P. BERNAL\\par\n");
            rtf.append("\\pard\\qc\\f0\\fs18 Student Accounts In-Charge\\par\n");
            rtf.append("\\sl-120\\slmult0\\par\n");
            rtf.append("\\pard\\qc\\f0\\fs18 Noted by:\\par\n");
            rtf.append("\\sl-120\\slmult0\\par\n");
            rtf.append("\\pard\\qc\\f0\\fs18\\b MARIA MICHELE O. CHATTO, MPA\\par\n");
            rtf.append("\\pard\\qc\\f0\\fs18 Administrative Officer V\\par\n");
            rtf.append("\\sl-120\\slmult0\\par\n");
            
            // Add spacing before footer to center content
            rtf.append("\\sl120\\slmult0\\par\n");
            rtf.append("\\par\n");
            
            // Footer Section - Place at bottom of document (not using RTF footer command to avoid UI elements)
            File footerImage = findFooterImage();
            if (footerImage != null) {
                String footerRtf = imageToRtfBackground(footerImage, true);
                if (footerRtf != null) {
                    rtf.append("\\pard\\qc");
                    rtf.append(footerRtf);
                    rtf.append("\\par\n");
                }
            } else {
                // Fallback if footer image not found
                rtf.append("\\pard\\qc\\brdrb\\brdrs\\brdrw15\\brdrcf1\\brsp20 \\par\n");
                rtf.append("\\trowd\\trgaph108\\trleft-108\n");
                rtf.append("\\cellx4000\\cellx8000\\cellx12000\n");
                rtf.append("\\intbl\\pard\\ql\\f0\\fs12 Davao Oriental State University\\par\n");
                rtf.append("\\f0\\fs12 Guang-guang, Dahican, City of Mati,\\par\n");
                rtf.append("\\f0\\fs12 Davao Oriental, 8200\\par\n");
                rtf.append("\\f0\\fs12 Republic of the Philippines\\cell\n");
                rtf.append("\\intbl\\pard\\ql\\f0\\fs12 website: www.dorsu.edu.ph\\par\n");
                rtf.append("\\f0\\fs12 phone: +63 (087)3883 195\\par\n");
                rtf.append("\\f0\\fs12 e-mail: op@dorsu.edu.ph\\par\n");
                rtf.append("\\f0\\fs12 Facebook: @dorsuofficial\\cell\n");
                rtf.append("\\intbl\\pard\\qr\\f0\\fs10\\b SOCOTEC\\par\n");
                rtf.append("\\f0\\fs10\\b ISO 9001\\par\n");
                rtf.append("\\par\n");
                rtf.append("\\f0\\fs9\\b PAB ACCREDITED\\par\n");
                rtf.append("\\f0\\fs9\\b CERTIFICATION BODY\\par\n");
                rtf.append("\\f0\\fs8\\b MS001\\cell\n");
                rtf.append("\\row\n");
            }
            rtf.append("\\pard\\par\n");
            
            rtf.append("}");
            
            // Write to file with proper encoding
            try (PrintWriter writer = new PrintWriter(new FileOutputStream(file), true, java.nio.charset.StandardCharsets.ISO_8859_1)) {
                writer.print(rtf.toString());
                writer.flush();
            }
            
            return file;
        } catch (Exception e) {
            System.err.println("Error generating letter: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Generate a promissory note document (RTF format, compatible with Word)
     * Returns true if successful, false otherwise (for backward compatibility)
     */
    public static boolean generatePromissoryNote(PromissoryNoteView student, LocalDate agreedPaymentDate, Stage stage) {
        File file = generatePromissoryNoteFile(student, agreedPaymentDate, stage);
        return file != null;
    }
}

