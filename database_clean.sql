-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Dec 09, 2025 at 05:45 PM
-- Server version: 10.4.32-MariaDB
-- PHP Version: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `accounting_system`
--

-- --------------------------------------------------------

--
-- Table structure for table `admin`
--

CREATE TABLE `admin` (
  `admin_id` int(11) NOT NULL,
  `username` varchar(50) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `fullname` varchar(100) NOT NULL,
  `email` varchar(100) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `admin`
--

INSERT INTO `admin` (`admin_id`, `username`, `password_hash`, `fullname`, `email`) VALUES
(352, 'admin', 'G85+WONd8nkHogJBqR3GGQ==:ZDU0Yjg1ZGI2NmZlZTM3Nzk1YTM1ODczNzYzYWZmZjAyZTRlZWVhZWZjYzI3MjBkMWE2MzkwNzYyMDdkN2E5Zg==', 'System Administrator', 'admin');

-- --------------------------------------------------------

--
-- Table structure for table `belong`
--

CREATE TABLE `belong` (
  `belong_id` int(11) NOT NULL,
  `student_id` int(11) NOT NULL,
  `school_year_id` int(11) NOT NULL,
  `semester_id` int(11) NOT NULL,
  `status` varchar(20) DEFAULT 'active'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `belong`
--

INSERT INTO `belong` (`belong_id`, `student_id`, `school_year_id`, `semester_id`, `status`) VALUES
(74, 32, 141, 34, 'active'),
(75, 33, 141, 34, 'active'),
(76, 34, 141, 37, 'active'),
(77, 34, 141, 36, 'deactivated'),
(78, 32, 141, 37, 'active'),
(79, 35, 141, 34, 'active'),
(80, 35, 141, 35, 'active'),
(81, 35, 141, 32, 'active');

-- --------------------------------------------------------

--
-- Table structure for table `duedate`
--

CREATE TABLE `duedate` (
  `duedate_id` int(11) NOT NULL,
  `due_date` date NOT NULL,
  `message` text DEFAULT NULL,
  `promissory_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `duedate`
--

INSERT INTO `duedate` (`duedate_id`, `due_date`, `message`, `promissory_id`) VALUES
(16, '2026-02-04', NULL, NULL),
(17, '2026-02-04', NULL, NULL),
(18, '2026-02-04', NULL, NULL),
(19, '2026-02-06', NULL, NULL),
(20, '2026-02-07', NULL, NULL),
(21, '2026-02-07', NULL, NULL),
(22, '2026-02-07', NULL, NULL),
(23, '2026-02-07', NULL, NULL),
(24, '2026-02-07', NULL, NULL),
(25, '2026-02-07', NULL, NULL),
(26, '2026-02-07', NULL, NULL),
(27, '2026-02-07', NULL, NULL),
(28, '2026-02-07', NULL, NULL),
(29, '2026-02-07', NULL, NULL),
(30, '2026-02-07', NULL, NULL),
(31, '2026-02-07', NULL, NULL),
(32, '2025-12-07', NULL, NULL),
(33, '2026-02-09', NULL, NULL),
(34, '2026-02-10', NULL, NULL);

-- --------------------------------------------------------

--
-- Triggers `duedate`
--

DELIMITER $$

CREATE TRIGGER `trg_check_overdue_status` BEFORE UPDATE ON `duedate` FOR EACH ROW BEGIN
    UPDATE student_payables sp
    SET sp.status = 'OVERDUE'
    WHERE sp.duedate_id = NEW.duedate_id
      AND NEW.due_date < CURDATE()
      AND sp.status != 'PAID'
      AND sp.amount_paid < (
          SELECT COALESCE(first_sem_amount, 0) + COALESCE(second_sem_amount, 0) + COALESCE(summer_sem_amount, 0)
          FROM semester s
          INNER JOIN belong b ON s.semester_id = b.semester_id
          WHERE b.belong_id = sp.belong_id
      );
END
$$

DELIMITER ;

-- --------------------------------------------------------

--
-- Table structure for table `promissory_note`
--

CREATE TABLE `promissory_note` (
  `promissory_id` int(11) NOT NULL,
  `created_date` date NOT NULL,
  `due_date_extended` date DEFAULT NULL,
  `remaining_balance_snapshot` decimal(10,2) NOT NULL,
  `note_text` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `promissory_note`
--

INSERT INTO `promissory_note` (`promissory_id`, `created_date`, `due_date_extended`, `remaining_balance_snapshot`, `note_text`) VALUES
(9, '2025-12-07', '2026-02-07', 400.00, 'Promissory note generated for kg kg kg'),
(10, '2025-12-07', '2025-12-07', 400.00, 'Promissory note generated for ewr wre wer'),
(11, '2025-12-07', '2025-12-07', 400.00, 'Promissory note generated for ewr wre wer'),
(12, '2025-12-07', '2025-12-07', 400.00, 'Promissory note generated for ewr wre wer'),
(13, '2025-12-07', '2025-12-07', 400.00, 'Promissory note generated for ewr wre wer'),
(14, '2025-12-07', '2025-12-07', 400.00, 'Promissory note generated for ewr wre wer'),
(15, '2025-12-07', '2025-12-07', 400.00, 'Promissory note generated for ewr wre wer'),
(16, '2025-12-07', '2025-12-07', 400.00, 'Promissory note generated for ewr wre wer'),
(17, '2025-12-07', '2025-12-07', 400.00, 'Promissory note generated for ewr wre wer'),
(18, '2025-12-07', '2025-12-07', 400.00, 'Promissory note generated for ewr wre wer'),
(19, '2025-12-08', '2025-12-07', 400.00, 'Promissory note generated for ewr wre wer'),
(20, '2025-12-08', '2025-12-07', 400.00, 'Promissory note generated for ewr wre wer'),
(21, '2025-12-08', '2025-12-07', 400.00, 'Promissory note generated for ewr wre wer'),
(22, '2025-12-08', '2025-12-07', 400.00, 'Promissory note generated for ewr wre wer'),
(23, '2025-12-08', '2025-12-07', 400.00, 'Promissory note generated for ewr wre wer'),
(24, '2025-12-08', '2025-12-07', 400.00, 'Promissory note generated for ewr wre wer'),
(25, '2025-12-08', '2025-12-07', 400.00, 'Promissory note generated for ewr wre wer'),
(26, '2025-12-08', '2025-12-07', 400.00, 'Promissory note generated for ewr wre wer'),
(27, '2025-12-08', '2025-12-07', 400.00, 'Promissory note generated for ewr wre wer'),
(28, '2025-12-08', '2025-12-07', 400.00, 'Promissory note generated for ewr wre wer'),
(29, '2025-12-08', '2025-12-07', 400.00, 'Promissory note generated for ewr wre wer'),
(30, '2025-12-08', '2025-12-07', 400.00, 'Promissory note generated for ewr wre wer'),
(31, '2025-12-08', '2025-12-07', 400.00, 'Promissory note generated for ewr wre wer'),
(32, '2025-12-08', '2025-12-07', 400.00, 'Promissory note generated for ewr wre wer'),
(33, '2025-12-08', '2025-12-07', 400.00, 'Promissory note generated for ewr wre wer'),
(34, '2025-12-08', '2025-12-07', 400.00, 'Promissory note generated for ewr wre wer'),
(35, '2025-12-08', '2025-12-07', 400.00, 'Promissory note generated for ewr wre wer'),
(36, '2025-12-08', '2025-12-07', 400.00, 'Promissory note generated for ewr wre wer'),
(37, '2025-12-08', '2025-12-07', 400.00, 'Promissory note generated for ewr wre wer'),
(38, '2025-12-08', '2025-12-07', 400.00, 'Promissory note generated for ewr wre wer'),
(39, '2025-12-08', '2025-12-07', 400.00, 'Promissory note generated for ewr wre wer'),
(40, '2025-12-08', '2025-12-07', 400.00, 'Promissory note generated for ewr wre wer'),
(41, '2025-12-08', '2025-12-07', 400.00, 'Promissory note generated for ewr wre wer'),
(42, '2025-12-08', '2025-12-07', 400.00, 'Promissory note generated for ewr wre wer'),
(43, '2025-12-08', '2025-12-07', 400.00, 'Promissory note generated for ewr wre wer'),
(44, '2025-12-08', '2025-12-07', 400.00, 'Promissory note generated for ewr wre wer'),
(45, '2025-12-08', '2025-12-07', 400.00, 'Promissory note generated for ewr wre wer'),
(46, '2025-12-08', '2025-12-07', 400.00, 'Promissory note generated for ewr wre wer'),
(47, '2025-12-08', '2025-12-07', 400.00, 'Promissory note generated for ewr wre wer'),
(48, '2025-12-08', '2025-12-07', 400.00, 'Promissory note generated for ewr wre wer'),
(49, '2025-12-09', '2025-12-07', 400.00, 'Promissory note generated for ewr wre wer');

-- --------------------------------------------------------

--
-- Table structure for table `school_year`
--

CREATE TABLE `school_year` (
  `school_year_id` int(11) NOT NULL,
  `year_range` varchar(20) NOT NULL,
  `is_active` tinyint(1) DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `school_year`
--

INSERT INTO `school_year` (`school_year_id`, `year_range`, `is_active`) VALUES
(124, '2025-2026', 0),
(128, '2024-2025', 1),
(129, '2023-2024', 1),
(141, '2027-2028', 1);

-- --------------------------------------------------------

--
-- Triggers `school_year`
--

DELIMITER $$

CREATE TRIGGER `trg_prevent_deactivate_school_year` BEFORE UPDATE ON `school_year` FOR EACH ROW BEGIN
    IF NEW.is_active = 0 AND OLD.is_active = 1 THEN
        IF EXISTS (
            SELECT 1 FROM student s 
            WHERE s.school_year_id = NEW.school_year_id 
            AND COALESCE(s.status, 'active') = 'active'
        ) OR EXISTS (
            SELECT 1 FROM belong b 
            WHERE b.school_year_id = NEW.school_year_id 
            AND COALESCE(b.status, 'active') = 'active'
        ) THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Cannot deactivate school year with active students';
        END IF;
    END IF;
END
$$

DELIMITER ;

-- --------------------------------------------------------

--
-- Table structure for table `semester`
--

CREATE TABLE `semester` (
  `semester_id` int(11) NOT NULL,
  `first_sem_amount` decimal(10,2) DEFAULT 0.00,
  `second_sem_amount` decimal(10,2) DEFAULT 0.00,
  `summer_sem_amount` decimal(10,2) DEFAULT 0.00
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `semester`
--

INSERT INTO `semester` (`semester_id`, `first_sem_amount`, `second_sem_amount`, `summer_sem_amount`) VALUES
(31, 1200.00, 0.00, 0.00),
(32, 9200.00, 0.00, 0.00),
(33, 0.00, 0.00, 0.00),
(34, 1.00, 0.00, 0.00),
(35, 9000.00, 0.00, 0.00),
(36, 0.00, 1.00, 0.00),
(37, 0.00, 0.00, 1.00);

-- --------------------------------------------------------

--
-- Table structure for table `student`
--

CREATE TABLE `student` (
  `student_id` int(11) NOT NULL,
  `student_number` varchar(50) NOT NULL,
  `fullname` varchar(100) NOT NULL,
  `major` varchar(100) DEFAULT NULL,
  `year` varchar(20) DEFAULT NULL,
  `school_year_id` int(11) DEFAULT NULL,
  `first_name` varchar(100) DEFAULT NULL,
  `middle_name` varchar(100) DEFAULT NULL,
  `last_name` varchar(100) DEFAULT NULL,
  `status` varchar(20) DEFAULT 'active'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `student`
--

INSERT INTO `student` (`student_id`, `student_number`, `fullname`, `major`, `year`, `school_year_id`, `first_name`, `middle_name`, `last_name`, `status`) VALUES
(32, '2024', 'Ricoss S Alentijo', 'BSBA - Bachelor of Science in Business Administration', '2nd Year', 141, 'Ricoss', 'S', 'Alentijo', 'active'),
(33, '1233', 'Ricos S Alentijo', 'BSBA - Bachelor of Science in Business Administration', '1st Year', 141, 'Ricos', 'S', 'Alentijo', 'active'),
(34, '123', 'Rico S Alentijo', 'BSBA - Bachelor of Science in Business Administration', '1st Year', 141, 'Rico', 'S', 'Alentijo', 'active'),
(35, '2023-1334', 'John Wayne Aklase', 'BSBA - Bachelor of Science in Business Administration', '2nd Year', 141, 'John', 'Wayne', 'Aklase', 'active');

-- --------------------------------------------------------

--
-- Triggers `student`
--

DELIMITER $$

CREATE TRIGGER `trg_insert_student_fullname` BEFORE INSERT ON `student` FOR EACH ROW BEGIN
    IF (NEW.first_name IS NOT NULL OR NEW.middle_name IS NOT NULL OR NEW.last_name IS NOT NULL) THEN
        SET NEW.fullname = TRIM(CONCAT_WS(' ', 
            COALESCE(NEW.first_name, ''),
            COALESCE(NEW.middle_name, ''),
            COALESCE(NEW.last_name, '')
        ));
    END IF;
END
$$

DELIMITER ;

DELIMITER $$

CREATE TRIGGER `trg_update_student_fullname` BEFORE UPDATE ON `student` FOR EACH ROW BEGIN
    IF (NEW.first_name IS NOT NULL OR NEW.middle_name IS NOT NULL OR NEW.last_name IS NOT NULL) THEN
        SET NEW.fullname = TRIM(CONCAT_WS(' ', 
            COALESCE(NEW.first_name, ''),
            COALESCE(NEW.middle_name, ''),
            COALESCE(NEW.last_name, '')
        ));
    END IF;
END
$$

DELIMITER ;

-- --------------------------------------------------------

--
-- Table structure for table `student_payables`
--

CREATE TABLE `student_payables` (
  `payable_id` int(11) NOT NULL,
  `belong_id` int(11) NOT NULL,
  `downpayment_amount` decimal(10,2) NOT NULL DEFAULT 0.00,
  `amount_paid` decimal(10,2) NOT NULL DEFAULT 0.00,
  `remaining_balance` decimal(10,2) NOT NULL DEFAULT 0.00,
  `status` enum('UNPAID','PARTIAL','PAID','OVERDUE') DEFAULT 'UNPAID',
  `duedate_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `student_payables`
--

INSERT INTO `student_payables` (`payable_id`, `belong_id`, `downpayment_amount`, `amount_paid`, `remaining_balance`, `status`, `duedate_id`) VALUES
(46, 80, 9000.00, 0.00, 9000.00, 'UNPAID', 33),
(47, 81, 9200.00, 900.00, 8300.00, 'PARTIAL', 34);

-- --------------------------------------------------------

--
-- Triggers `student_payables`
--

DELIMITER $$

CREATE TRIGGER `trg_insert_remaining_balance` BEFORE INSERT ON `student_payables` FOR EACH ROW BEGIN
    DECLARE total_payable DECIMAL(10,2) DEFAULT 0.00;
    SELECT COALESCE(
        (SELECT COALESCE(first_sem_amount, 0) + COALESCE(second_sem_amount, 0) + COALESCE(summer_sem_amount, 0)
         FROM semester s
         INNER JOIN belong b ON s.semester_id = b.semester_id
         WHERE b.belong_id = NEW.belong_id), 0
    ) INTO total_payable;
    IF NEW.remaining_balance IS NULL OR NEW.remaining_balance = 0 THEN
        SET NEW.remaining_balance = GREATEST(total_payable - COALESCE(NEW.amount_paid, 0), 0.00);
    END IF;
    IF NEW.status IS NULL OR NEW.status = '' THEN
        IF NEW.amount_paid >= total_payable OR ABS(NEW.amount_paid - total_payable) < 0.01 THEN
            SET NEW.status = 'PAID';
        ELSEIF NEW.amount_paid > 0 THEN
            SET NEW.status = 'PARTIAL';
        ELSE
            SET NEW.status = 'UNPAID';
        END IF;
    END IF;
END
$$

DELIMITER ;

DELIMITER $$

CREATE TRIGGER `trg_update_remaining_balance` BEFORE UPDATE ON `student_payables` FOR EACH ROW BEGIN
    DECLARE total_payable DECIMAL(10,2) DEFAULT 0.00;
    SELECT COALESCE(
        (SELECT COALESCE(first_sem_amount, 0) + COALESCE(second_sem_amount, 0) + COALESCE(summer_sem_amount, 0)
         FROM semester s
         INNER JOIN belong b ON s.semester_id = b.semester_id
         WHERE b.belong_id = NEW.belong_id), 0
    ) INTO total_payable;
    SET NEW.remaining_balance = GREATEST(total_payable - NEW.amount_paid, 0.00);
    IF NEW.amount_paid >= total_payable OR ABS(NEW.amount_paid - total_payable) < 0.01 THEN
        SET NEW.status = 'PAID';
    ELSEIF NEW.amount_paid > 0 THEN
        IF EXISTS (
            SELECT 1 FROM duedate d 
            WHERE d.duedate_id = NEW.duedate_id 
            AND d.due_date < CURDATE()
        ) THEN
            SET NEW.status = 'OVERDUE';
        ELSE
            SET NEW.status = 'PARTIAL';
        END IF;
    ELSE
        IF EXISTS (
            SELECT 1 FROM duedate d 
            WHERE d.duedate_id = NEW.duedate_id 
            AND d.due_date < CURDATE()
        ) THEN
            SET NEW.status = 'OVERDUE';
        ELSE
            SET NEW.status = 'UNPAID';
        END IF;
    END IF;
END
$$

DELIMITER ;

--
-- Indexes for dumped tables
--

--
-- Indexes for table `admin`
--

ALTER TABLE `admin`
  ADD PRIMARY KEY (`admin_id`),
  ADD UNIQUE KEY `username` (`username`);

--
-- Indexes for table `belong`
--

ALTER TABLE `belong`
  ADD PRIMARY KEY (`belong_id`),
  ADD UNIQUE KEY `unique_belong` (`student_id`,`school_year_id`,`semester_id`),
  ADD KEY `school_year_id` (`school_year_id`),
  ADD KEY `semester_id` (`semester_id`);

--
-- Indexes for table `duedate`
--

ALTER TABLE `duedate`
  ADD PRIMARY KEY (`duedate_id`),
  ADD KEY `promissory_id` (`promissory_id`);

--
-- Indexes for table `promissory_note`
--

ALTER TABLE `promissory_note`
  ADD PRIMARY KEY (`promissory_id`);

--
-- Indexes for table `school_year`
--

ALTER TABLE `school_year`
  ADD PRIMARY KEY (`school_year_id`),
  ADD UNIQUE KEY `year_range` (`year_range`);

--
-- Indexes for table `semester`
--

ALTER TABLE `semester`
  ADD PRIMARY KEY (`semester_id`);

--
-- Indexes for table `student`
--

ALTER TABLE `student`
  ADD PRIMARY KEY (`student_id`),
  ADD UNIQUE KEY `student_number` (`student_number`),
  ADD KEY `school_year_id` (`school_year_id`);

--
-- Indexes for table `student_payables`
--

ALTER TABLE `student_payables`
  ADD PRIMARY KEY (`payable_id`),
  ADD KEY `belong_id` (`belong_id`),
  ADD KEY `duedate_id` (`duedate_id`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `admin`
--

ALTER TABLE `admin`
  MODIFY `admin_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=383;

--
-- AUTO_INCREMENT for table `belong`
--

ALTER TABLE `belong`
  MODIFY `belong_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=82;

--
-- AUTO_INCREMENT for table `duedate`
--

ALTER TABLE `duedate`
  MODIFY `duedate_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=35;

--
-- AUTO_INCREMENT for table `promissory_note`
--

ALTER TABLE `promissory_note`
  MODIFY `promissory_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=50;

--
-- AUTO_INCREMENT for table `school_year`
--

ALTER TABLE `school_year`
  MODIFY `school_year_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=1119;

--
-- AUTO_INCREMENT for table `semester`
--

ALTER TABLE `semester`
  MODIFY `semester_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=38;

--
-- AUTO_INCREMENT for table `student`
--

ALTER TABLE `student`
  MODIFY `student_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=36;

--
-- AUTO_INCREMENT for table `student_payables`
--

ALTER TABLE `student_payables`
  MODIFY `payable_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=48;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `belong`
--

ALTER TABLE `belong`
  ADD CONSTRAINT `belong_ibfk_1` FOREIGN KEY (`student_id`) REFERENCES `student` (`student_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `belong_ibfk_2` FOREIGN KEY (`school_year_id`) REFERENCES `school_year` (`school_year_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `belong_ibfk_3` FOREIGN KEY (`semester_id`) REFERENCES `semester` (`semester_id`) ON DELETE CASCADE;

--
-- Constraints for table `duedate`
--

ALTER TABLE `duedate`
  ADD CONSTRAINT `duedate_ibfk_1` FOREIGN KEY (`promissory_id`) REFERENCES `promissory_note` (`promissory_id`) ON DELETE SET NULL;

--
-- Constraints for table `student`
--

ALTER TABLE `student`
  ADD CONSTRAINT `student_ibfk_1` FOREIGN KEY (`school_year_id`) REFERENCES `school_year` (`school_year_id`) ON DELETE SET NULL;

--
-- Constraints for table `student_payables`
--

ALTER TABLE `student_payables`
  ADD CONSTRAINT `student_payables_ibfk_1` FOREIGN KEY (`belong_id`) REFERENCES `belong` (`belong_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `student_payables_ibfk_2` FOREIGN KEY (`duedate_id`) REFERENCES `duedate` (`duedate_id`) ON DELETE SET NULL;

COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;

