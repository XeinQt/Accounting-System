-- =====================================================
-- Sample Data for Accounting System
-- 50 records for all pages
-- =====================================================

USE accounting_system;

-- =====================================================
-- 1. SCHOOL YEARS (5 school years)
-- =====================================================
INSERT INTO school_year (year_range, is_active) VALUES
('2020-2021', 0),
('2021-2022', 0),
('2022-2023', 0),
('2023-2024', 1),
('2024-2025', 1),
('2025-2026', 1),
('2026-2027', 1),
('2027-2028', 1);

-- =====================================================
-- 2. SEMESTERS (Multiple semester records with different amounts)
-- =====================================================
INSERT INTO semester (first_sem_amount, second_sem_amount, summer_sem_amount) VALUES
-- 1st Semester amounts
(0.00, 0.00, 0.00),
(8500.00, 0.00, 0.00),
(9000.00, 0.00, 0.00),
(9200.00, 0.00, 0.00),
(9500.00, 0.00, 0.00),
(10000.00, 0.00, 0.00),
(10500.00, 0.00, 0.00),
(11000.00, 0.00, 0.00),
(11500.00, 0.00, 0.00),
(12000.00, 0.00, 0.00),
-- 2nd Semester amounts
(0.00, 0.00, 0.00),
(0.00, 8500.00, 0.00),
(0.00, 9000.00, 0.00),
(0.00, 9200.00, 0.00),
(0.00, 9500.00, 0.00),
(0.00, 10000.00, 0.00),
(0.00, 10500.00, 0.00),
(0.00, 11000.00, 0.00),
(0.00, 11500.00, 0.00),
(0.00, 12000.00, 0.00),
-- Summer Semester amounts
(0.00, 0.00, 5000.00),
(0.00, 0.00, 5500.00),
(0.00, 0.00, 6000.00),
(0.00, 0.00, 6500.00),
(0.00, 0.00, 7000.00);

-- =====================================================
-- 3. STUDENTS (50 students)
-- =====================================================
-- Get the active school year ID first
SET @active_school_year_id = (SELECT school_year_id FROM school_year WHERE is_active = 1 ORDER BY school_year_id DESC LIMIT 1);

INSERT INTO student (student_number, first_name, middle_name, last_name, fullname, major, year, school_year_id, status) VALUES
('2020-0001', 'Juan', 'Dela', 'Cruz', 'Juan Dela Cruz', 'BSBA - Bachelor of Science in Business Administration', '1st Year', @active_school_year_id, 'active'),
('2020-0002', 'Maria', 'Santos', 'Garcia', 'Maria Santos Garcia', 'BSIT - Bachelor of Science in Information Technology', '1st Year', @active_school_year_id, 'active'),
('2020-0003', 'Jose', 'Reyes', 'Lopez', 'Jose Reyes Lopez', 'BSA - Bachelor of Science in Agriculture', '1st Year', @active_school_year_id, 'active'),
('2020-0004', 'Ana', 'Torres', 'Martinez', 'Ana Torres Martinez', 'BTLED - Bachelor of Technology and Livelihood Education', '1st Year', @active_school_year_id, 'active'),
('2020-0005', 'Carlos', 'Fernandez', 'Rodriguez', 'Carlos Fernandez Rodriguez', 'BSBA - Bachelor of Science in Business Administration', '1st Year', @active_school_year_id, 'active'),
('2021-0001', 'Rosa', 'Gonzalez', 'Perez', 'Rosa Gonzalez Perez', 'BSIT - Bachelor of Science in Information Technology', '2nd Year', @active_school_year_id, 'active'),
('2021-0002', 'Miguel', 'Sanchez', 'Ramirez', 'Miguel Sanchez Ramirez', 'BSA - Bachelor of Science in Agriculture', '2nd Year', @active_school_year_id, 'active'),
('2021-0003', 'Carmen', 'Morales', 'Flores', 'Carmen Morales Flores', 'BTLED - Bachelor of Technology and Livelihood Education', '2nd Year', @active_school_year_id, 'active'),
('2021-0004', 'Pedro', 'Rivera', 'Gomez', 'Pedro Rivera Gomez', 'BSBA - Bachelor of Science in Business Administration', '2nd Year', @active_school_year_id, 'active'),
('2021-0005', 'Elena', 'Castro', 'Diaz', 'Elena Castro Diaz', 'BSIT - Bachelor of Science in Information Technology', '2nd Year', @active_school_year_id, 'active'),
('2022-0001', 'Roberto', 'Mendoza', 'Herrera', 'Roberto Mendoza Herrera', 'BSA - Bachelor of Science in Agriculture', '3rd Year', @active_school_year_id, 'active'),
('2022-0002', 'Sofia', 'Vargas', 'Jimenez', 'Sofia Vargas Jimenez', 'BTLED - Bachelor of Technology and Livelihood Education', '3rd Year', @active_school_year_id, 'active'),
('2022-0003', 'Fernando', 'Ortega', 'Moreno', 'Fernando Ortega Moreno', 'BSBA - Bachelor of Science in Business Administration', '3rd Year', @active_school_year_id, 'active'),
('2022-0004', 'Isabel', 'Ruiz', 'Alvarez', 'Isabel Ruiz Alvarez', 'BSIT - Bachelor of Science in Information Technology', '3rd Year', @active_school_year_id, 'active'),
('2022-0005', 'Ricardo', 'Medina', 'Vega', 'Ricardo Medina Vega', 'BSA - Bachelor of Science in Agriculture', '3rd Year', @active_school_year_id, 'active'),
('2023-0001', 'Patricia', 'Guerrero', 'Ramos', 'Patricia Guerrero Ramos', 'BTLED - Bachelor of Technology and Livelihood Education', '4th Year', @active_school_year_id, 'active'),
('2023-0002', 'Antonio', 'Paredes', 'Silva', 'Antonio Paredes Silva', 'BSBA - Bachelor of Science in Business Administration', '4th Year', @active_school_year_id, 'active'),
('2023-0003', 'Gabriela', 'Navarro', 'Cortes', 'Gabriela Navarro Cortes', 'BSIT - Bachelor of Science in Information Technology', '4th Year', @active_school_year_id, 'active'),
('2023-0004', 'Manuel', 'Campos', 'Mendez', 'Manuel Campos Mendez', 'BSA - Bachelor of Science in Agriculture', '4th Year', @active_school_year_id, 'active'),
('2023-0005', 'Laura', 'Vega', 'Rojas', 'Laura Vega Rojas', 'BTLED - Bachelor of Technology and Livelihood Education', '4th Year', @active_school_year_id, 'active'),
('2024-0001', 'Daniel', 'Herrera', 'Fuentes', 'Daniel Herrera Fuentes', 'BSBA - Bachelor of Science in Business Administration', '1st Year', @active_school_year_id, 'active'),
('2024-0002', 'Monica', 'Soto', 'Aguilar', 'Monica Soto Aguilar', 'BSIT - Bachelor of Science in Information Technology', '1st Year', @active_school_year_id, 'active'),
('2024-0003', 'Francisco', 'Molina', 'Castillo', 'Francisco Molina Castillo', 'BSA - Bachelor of Science in Agriculture', '1st Year', @active_school_year_id, 'active'),
('2024-0004', 'Andrea', 'Delgado', 'Ortiz', 'Andrea Delgado Ortiz', 'BTLED - Bachelor of Technology and Livelihood Education', '1st Year', @active_school_year_id, 'active'),
('2024-0005', 'Alejandro', 'Rios', 'Valdez', 'Alejandro Rios Valdez', 'BSBA - Bachelor of Science in Business Administration', '1st Year', @active_school_year_id, 'active'),
('2025-0001', 'Valentina', 'Cordero', 'Pacheco', 'Valentina Cordero Pacheco', 'BSIT - Bachelor of Science in Information Technology', '2nd Year', @active_school_year_id, 'active'),
('2025-0002', 'Sergio', 'Barrera', 'Salazar', 'Sergio Barrera Salazar', 'BSA - Bachelor of Science in Agriculture', '2nd Year', @active_school_year_id, 'active'),
('2025-0003', 'Natalia', 'Acosta', 'Trujillo', 'Natalia Acosta Trujillo', 'BTLED - Bachelor of Technology and Livelihood Education', '2nd Year', @active_school_year_id, 'active'),
('2025-0004', 'Diego', 'Montoya', 'Villarreal', 'Diego Montoya Villarreal', 'BSBA - Bachelor of Science in Business Administration', '2nd Year', @active_school_year_id, 'active'),
('2025-0005', 'Camila', 'Escobar', 'Zamora', 'Camila Escobar Zamora', 'BSIT - Bachelor of Science in Information Technology', '2nd Year', @active_school_year_id, 'active'),
('2026-0001', 'Andres', 'Palacios', 'Beltran', 'Andres Palacios Beltran', 'BSA - Bachelor of Science in Agriculture', '3rd Year', @active_school_year_id, 'active'),
('2026-0002', 'Mariana', 'Carrillo', 'Gallegos', 'Mariana Carrillo Gallegos', 'BTLED - Bachelor of Technology and Livelihood Education', '3rd Year', @active_school_year_id, 'active'),
('2026-0003', 'Sebastian', 'Villanueva', 'Espinoza', 'Sebastian Villanueva Espinoza', 'BSBA - Bachelor of Science in Business Administration', '3rd Year', @active_school_year_id, 'active'),
('2026-0004', 'Isabella', 'Maldonado', 'Cervantes', 'Isabella Maldonado Cervantes', 'BSIT - Bachelor of Science in Information Technology', '3rd Year', @active_school_year_id, 'active'),
('2026-0005', 'Mateo', 'Aguirre', 'Rangel', 'Mateo Aguirre Rangel', 'BSA - Bachelor of Science in Agriculture', '3rd Year', @active_school_year_id, 'active'),
('2027-0001', 'Lucia', 'Bermudez', 'Salinas', 'Lucia Bermudez Salinas', 'BTLED - Bachelor of Technology and Livelihood Education', '4th Year', @active_school_year_id, 'active'),
('2027-0002', 'Emilio', 'Cardenas', 'Vasquez', 'Emilio Cardenas Vasquez', 'BSBA - Bachelor of Science in Business Administration', '4th Year', @active_school_year_id, 'active'),
('2027-0003', 'Renata', 'Franco', 'Mejia', 'Renata Franco Mejia', 'BSIT - Bachelor of Science in Information Technology', '4th Year', @active_school_year_id, 'active'),
('2027-0004', 'Javier', 'Ibarra', 'Nunez', 'Javier Ibarra Nunez', 'BSA - Bachelor of Science in Agriculture', '4th Year', @active_school_year_id, 'active'),
('2027-0005', 'Adriana', 'Lozano', 'Quintero', 'Adriana Lozano Quintero', 'BTLED - Bachelor of Technology and Livelihood Education', '4th Year', @active_school_year_id, 'active'),
('2028-0001', 'Rodrigo', 'Miranda', 'Rivas', 'Rodrigo Miranda Rivas', 'BSBA - Bachelor of Science in Business Administration', '1st Year', @active_school_year_id, 'active'),
('2028-0002', 'Ximena', 'Nava', 'Serrano', 'Ximena Nava Serrano', 'BSIT - Bachelor of Science in Information Technology', '1st Year', @active_school_year_id, 'active'),
('2028-0003', 'Bruno', 'Ochoa', 'Tapia', 'Bruno Ochoa Tapia', 'BSA - Bachelor of Science in Agriculture', '1st Year', @active_school_year_id, 'active'),
('2028-0004', 'Diana', 'Ponce', 'Uribe', 'Diana Ponce Uribe', 'BTLED - Bachelor of Technology and Livelihood Education', '1st Year', @active_school_year_id, 'active'),
('2028-0005', 'Eduardo', 'Quiroz', 'Velasco', 'Eduardo Quiroz Velasco', 'BSBA - Bachelor of Science in Business Administration', '1st Year', @active_school_year_id, 'active'),
('2029-0001', 'Fernanda', 'Rendon', 'Yanez', 'Fernanda Rendon Yanez', 'BSIT - Bachelor of Science in Information Technology', '2nd Year', @active_school_year_id, 'active'),
('2029-0002', 'Gustavo', 'Solis', 'Zarate', 'Gustavo Solis Zarate', 'BSA - Bachelor of Science in Agriculture', '2nd Year', @active_school_year_id, 'active'),
('2029-0003', 'Helena', 'Tovar', 'Acevedo', 'Helena Tovar Acevedo', 'BTLED - Bachelor of Technology and Livelihood Education', '2nd Year', @active_school_year_id, 'active'),
('2029-0004', 'Ivan', 'Urbina', 'Baez', 'Ivan Urbina Baez', 'BSBA - Bachelor of Science in Business Administration', '2nd Year', @active_school_year_id, 'active'),
('2029-0005', 'Julia', 'Valencia', 'Cisneros', 'Julia Valencia Cisneros', 'BSIT - Bachelor of Science in Information Technology', '2nd Year', @active_school_year_id, 'active');

-- =====================================================
-- 4. BELONG (Enrollment records - 50 records linking students to school years and semesters)
-- =====================================================
-- Get the most recent active school year ID
SET @school_year_id = (SELECT school_year_id FROM school_year WHERE is_active = 1 ORDER BY school_year_id DESC LIMIT 1);

-- Get semester IDs (1st Sem, 2nd Sem, Summer Sem)
SET @first_sem_id = (SELECT semester_id FROM semester WHERE first_sem_amount > 0 AND (second_sem_amount = 0 OR second_sem_amount IS NULL) AND (summer_sem_amount = 0 OR summer_sem_amount IS NULL) LIMIT 1);
SET @second_sem_id = (SELECT semester_id FROM semester WHERE second_sem_amount > 0 AND (first_sem_amount = 0 OR first_sem_amount IS NULL) AND (summer_sem_amount = 0 OR summer_sem_amount IS NULL) LIMIT 1);
SET @summer_sem_id = (SELECT semester_id FROM semester WHERE summer_sem_amount > 0 AND (first_sem_amount = 0 OR first_sem_amount IS NULL) AND (second_sem_amount = 0 OR second_sem_amount IS NULL) LIMIT 1);

-- If no semesters exist, create default ones
INSERT INTO semester (first_sem_amount, second_sem_amount, summer_sem_amount) 
SELECT 9000.00, 0.00, 0.00
WHERE NOT EXISTS (SELECT 1 FROM semester WHERE first_sem_amount > 0 LIMIT 1);

INSERT INTO semester (first_sem_amount, second_sem_amount, summer_sem_amount) 
SELECT 0.00, 9000.00, 0.00
WHERE NOT EXISTS (SELECT 1 FROM semester WHERE second_sem_amount > 0 LIMIT 1);

INSERT INTO semester (first_sem_amount, second_sem_amount, summer_sem_amount) 
SELECT 0.00, 0.00, 5000.00
WHERE NOT EXISTS (SELECT 1 FROM semester WHERE summer_sem_amount > 0 LIMIT 1);

-- Update semester IDs
SET @first_sem_id = (SELECT semester_id FROM semester WHERE first_sem_amount > 0 AND (second_sem_amount = 0 OR second_sem_amount IS NULL) AND (summer_sem_amount = 0 OR summer_sem_amount IS NULL) LIMIT 1);
SET @second_sem_id = (SELECT semester_id FROM semester WHERE second_sem_amount > 0 AND (first_sem_amount = 0 OR first_sem_amount IS NULL) AND (summer_sem_amount = 0 OR summer_sem_amount IS NULL) LIMIT 1);
SET @summer_sem_id = (SELECT semester_id FROM semester WHERE summer_sem_amount > 0 AND (first_sem_amount = 0 OR first_sem_amount IS NULL) AND (second_sem_amount = 0 OR second_sem_amount IS NULL) LIMIT 1);

-- Create belong records for all 50 students
-- Get the last 50 student IDs that were just inserted
-- First 25 students in 1st Semester
INSERT INTO belong (student_id, school_year_id, semester_id, status)
SELECT s.student_id, @school_year_id, @first_sem_id, 'active'
FROM student s
WHERE s.student_number LIKE '2020-%' OR s.student_number LIKE '2021-%' OR s.student_number LIKE '2022-%' OR s.student_number LIKE '2023-%' OR s.student_number LIKE '2024-%'
ORDER BY s.student_id
LIMIT 25;

-- Next 15 students in 2nd Semester  
INSERT INTO belong (student_id, school_year_id, semester_id, status)
SELECT s.student_id, @school_year_id, @second_sem_id, 'active'
FROM student s
WHERE s.student_number LIKE '2025-%' OR s.student_number LIKE '2026-%'
ORDER BY s.student_id
LIMIT 15;

-- Last 10 students in Summer Semester
INSERT INTO belong (student_id, school_year_id, semester_id, status)
SELECT s.student_id, @school_year_id, @summer_sem_id, 'active'
FROM student s
WHERE s.student_number LIKE '2027-%' OR s.student_number LIKE '2028-%' OR s.student_number LIKE '2029-%'
ORDER BY s.student_id
LIMIT 10;

-- =====================================================
-- 5. DUEDATES (50 due dates - 2 months from various dates)
-- =====================================================
INSERT INTO duedate (due_date, message) VALUES
(DATE_ADD(CURDATE(), INTERVAL 2 MONTH), NULL),
(DATE_ADD(CURDATE(), INTERVAL 2 MONTH), NULL),
(DATE_ADD(CURDATE(), INTERVAL 2 MONTH), NULL),
(DATE_ADD(CURDATE(), INTERVAL 2 MONTH), NULL),
(DATE_ADD(CURDATE(), INTERVAL 2 MONTH), NULL),
(DATE_ADD(CURDATE(), INTERVAL 3 MONTH), NULL),
(DATE_ADD(CURDATE(), INTERVAL 3 MONTH), NULL),
(DATE_ADD(CURDATE(), INTERVAL 3 MONTH), NULL),
(DATE_ADD(CURDATE(), INTERVAL 3 MONTH), NULL),
(DATE_ADD(CURDATE(), INTERVAL 3 MONTH), NULL),
(DATE_ADD(CURDATE(), INTERVAL 1 MONTH), NULL),
(DATE_ADD(CURDATE(), INTERVAL 1 MONTH), NULL),
(DATE_ADD(CURDATE(), INTERVAL 1 MONTH), NULL),
(DATE_ADD(CURDATE(), INTERVAL 1 MONTH), NULL),
(DATE_ADD(CURDATE(), INTERVAL 1 MONTH), NULL),
(DATE_ADD(CURDATE(), INTERVAL 4 MONTH), NULL),
(DATE_ADD(CURDATE(), INTERVAL 4 MONTH), NULL),
(DATE_ADD(CURDATE(), INTERVAL 4 MONTH), NULL),
(DATE_ADD(CURDATE(), INTERVAL 4 MONTH), NULL),
(DATE_ADD(CURDATE(), INTERVAL 4 MONTH), NULL),
(DATE_ADD(CURDATE(), INTERVAL 2 MONTH), NULL),
(DATE_ADD(CURDATE(), INTERVAL 2 MONTH), NULL),
(DATE_ADD(CURDATE(), INTERVAL 2 MONTH), NULL),
(DATE_ADD(CURDATE(), INTERVAL 2 MONTH), NULL),
(DATE_ADD(CURDATE(), INTERVAL 2 MONTH), NULL),
(DATE_ADD(CURDATE(), INTERVAL 3 MONTH), NULL),
(DATE_ADD(CURDATE(), INTERVAL 3 MONTH), NULL),
(DATE_ADD(CURDATE(), INTERVAL 3 MONTH), NULL),
(DATE_ADD(CURDATE(), INTERVAL 3 MONTH), NULL),
(DATE_ADD(CURDATE(), INTERVAL 3 MONTH), NULL),
(DATE_ADD(CURDATE(), INTERVAL 1 MONTH), NULL),
(DATE_ADD(CURDATE(), INTERVAL 1 MONTH), NULL),
(DATE_ADD(CURDATE(), INTERVAL 1 MONTH), NULL),
(DATE_ADD(CURDATE(), INTERVAL 1 MONTH), NULL),
(DATE_ADD(CURDATE(), INTERVAL 1 MONTH), NULL),
(DATE_ADD(CURDATE(), INTERVAL 4 MONTH), NULL),
(DATE_ADD(CURDATE(), INTERVAL 4 MONTH), NULL),
(DATE_ADD(CURDATE(), INTERVAL 4 MONTH), NULL),
(DATE_ADD(CURDATE(), INTERVAL 4 MONTH), NULL),
(DATE_ADD(CURDATE(), INTERVAL 4 MONTH), NULL),
(DATE_ADD(CURDATE(), INTERVAL 2 MONTH), NULL),
(DATE_ADD(CURDATE(), INTERVAL 2 MONTH), NULL),
(DATE_ADD(CURDATE(), INTERVAL 2 MONTH), NULL),
(DATE_ADD(CURDATE(), INTERVAL 2 MONTH), NULL),
(DATE_ADD(CURDATE(), INTERVAL 2 MONTH), NULL),
(DATE_ADD(CURDATE(), INTERVAL 3 MONTH), NULL),
(DATE_ADD(CURDATE(), INTERVAL 3 MONTH), NULL),
(DATE_ADD(CURDATE(), INTERVAL 3 MONTH), NULL),
(DATE_ADD(CURDATE(), INTERVAL 3 MONTH), NULL),
(DATE_ADD(CURDATE(), INTERVAL 3 MONTH), NULL);

-- =====================================================
-- 6. STUDENT_PAYABLES (50 payable records with varying amounts and statuses)
-- =====================================================
-- This will create payables for all belong records
INSERT INTO student_payables (belong_id, downpayment_amount, amount_paid, remaining_balance, status, duedate_id)
SELECT 
    b.belong_id,
    CASE 
        WHEN sem.first_sem_amount > 0 THEN sem.first_sem_amount
        WHEN sem.second_sem_amount > 0 THEN sem.second_sem_amount
        WHEN sem.summer_sem_amount > 0 THEN sem.summer_sem_amount
        ELSE 0
    END AS downpayment_amount,
    -- Varying payment amounts: some paid, some partial, some unpaid
    CASE 
        WHEN MOD(b.belong_id, 5) = 0 THEN 
            CASE 
                WHEN sem.first_sem_amount > 0 THEN sem.first_sem_amount
                WHEN sem.second_sem_amount > 0 THEN sem.second_sem_amount
                WHEN sem.summer_sem_amount > 0 THEN sem.summer_sem_amount
                ELSE 0
            END  -- Fully paid (20%)
        WHEN MOD(b.belong_id, 5) = 1 THEN 
            CASE 
                WHEN sem.first_sem_amount > 0 THEN sem.first_sem_amount * 0.5
                WHEN sem.second_sem_amount > 0 THEN sem.second_sem_amount * 0.5
                WHEN sem.summer_sem_amount > 0 THEN sem.summer_sem_amount * 0.5
                ELSE 0
            END  -- Half paid (20%)
        WHEN MOD(b.belong_id, 5) = 2 THEN 
            CASE 
                WHEN sem.first_sem_amount > 0 THEN sem.first_sem_amount * 0.3
                WHEN sem.second_sem_amount > 0 THEN sem.second_sem_amount * 0.3
                WHEN sem.summer_sem_amount > 0 THEN sem.summer_sem_amount * 0.3
                ELSE 0
            END  -- 30% paid (20%)
        WHEN MOD(b.belong_id, 5) = 3 THEN 
            CASE 
                WHEN sem.first_sem_amount > 0 THEN sem.first_sem_amount * 0.7
                WHEN sem.second_sem_amount > 0 THEN sem.second_sem_amount * 0.7
                WHEN sem.summer_sem_amount > 0 THEN sem.summer_sem_amount * 0.7
                ELSE 0
            END  -- 70% paid (20%)
        ELSE 0  -- Unpaid (20%)
    END AS amount_paid,
    -- Calculate remaining balance
    CASE 
        WHEN sem.first_sem_amount > 0 THEN 
            GREATEST(sem.first_sem_amount - 
                CASE 
                    WHEN MOD(b.belong_id, 5) = 0 THEN sem.first_sem_amount
                    WHEN MOD(b.belong_id, 5) = 1 THEN sem.first_sem_amount * 0.5
                    WHEN MOD(b.belong_id, 5) = 2 THEN sem.first_sem_amount * 0.3
                    WHEN MOD(b.belong_id, 5) = 3 THEN sem.first_sem_amount * 0.7
                    ELSE 0
                END, 0)
        WHEN sem.second_sem_amount > 0 THEN 
            GREATEST(sem.second_sem_amount - 
                CASE 
                    WHEN MOD(b.belong_id, 5) = 0 THEN sem.second_sem_amount
                    WHEN MOD(b.belong_id, 5) = 1 THEN sem.second_sem_amount * 0.5
                    WHEN MOD(b.belong_id, 5) = 2 THEN sem.second_sem_amount * 0.3
                    WHEN MOD(b.belong_id, 5) = 3 THEN sem.second_sem_amount * 0.7
                    ELSE 0
                END, 0)
        WHEN sem.summer_sem_amount > 0 THEN 
            GREATEST(sem.summer_sem_amount - 
                CASE 
                    WHEN MOD(b.belong_id, 5) = 0 THEN sem.summer_sem_amount
                    WHEN MOD(b.belong_id, 5) = 1 THEN sem.summer_sem_amount * 0.5
                    WHEN MOD(b.belong_id, 5) = 2 THEN sem.summer_sem_amount * 0.3
                    WHEN MOD(b.belong_id, 5) = 3 THEN sem.summer_sem_amount * 0.7
                    ELSE 0
                END, 0)
        ELSE 0
    END AS remaining_balance,
    -- Set status based on payment
    CASE 
        WHEN MOD(b.belong_id, 5) = 0 THEN 'PAID'
        WHEN MOD(b.belong_id, 5) IN (1, 2, 3) THEN 'PARTIAL'
        ELSE 'UNPAID'
    END AS status,
    -- Link to duedate (assign duedates in order, cycling through available ones)
    (SELECT duedate_id FROM (
        SELECT duedate_id, @row := @row + 1 AS rn
        FROM duedate, (SELECT @row := -1) AS r
        ORDER BY duedate_id
        LIMIT 50
    ) AS d
    WHERE d.rn = MOD(b.belong_id - 1, 50)
    LIMIT 1) AS duedate_id
FROM belong b
INNER JOIN semester sem ON b.semester_id = sem.semester_id
CROSS JOIN (SELECT @row := -1) AS r2
WHERE b.school_year_id = @school_year_id
  AND NOT EXISTS (SELECT 1 FROM student_payables sp WHERE sp.belong_id = b.belong_id)
ORDER BY b.belong_id
LIMIT 50;

-- =====================================================
-- Summary
-- =====================================================
-- This script creates:
-- - 8 School Years
-- - 25 Semester records (various amounts)
-- - 50 Students (with different names, majors, and years)
-- - 50 Belong records (enrollment records)
-- - 50 Due dates
-- - 50 Student payables (with varying payment statuses: PAID, PARTIAL, UNPAID)

