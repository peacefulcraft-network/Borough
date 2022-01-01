-- phpMyAdmin SQL Dump
-- version 5.0.4
-- https://www.phpmyadmin.net/
--
-- Host: 172.16.1.10:3306
-- Generation Time: Jan 01, 2022 at 09:15 PM
-- Server version: 10.6.5-MariaDB-1:10.6.5+maria~focal
-- PHP Version: 8.0.7

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `s43_borough`
--

-- --------------------------------------------------------

--
-- Table structure for table `claim`
--

CREATE TABLE `claim` (
  `claim_id` bigint(20) UNSIGNED NOT NULL,
  `claim_name` varchar(50) NOT NULL,
  `creator_uuid` char(36) NOT NULL,
  `allow_block_damage` tinyint(1) NOT NULL DEFAULT 1 COMMENT 'Natural block damage events (explosion, fire, etc)',
  `allow_fluid_movement` tinyint(1) NOT NULL DEFAULT 1,
  `allow_pvp` tinyint(1) NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------

--
-- Table structure for table `claim_chunk`
--

CREATE TABLE `claim_chunk` (
  `claim_id` bigint(20) UNSIGNED NOT NULL,
  `world` varchar(50) NOT NULL,
  `x` int(11) NOT NULL,
  `z` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------

--
-- Table structure for table `claim_permission`
--

CREATE TABLE `claim_permission` (
  `user_uuid` char(36) NOT NULL,
  `claim_id` bigint(20) UNSIGNED NOT NULL,
  `level` enum('OWNER','MODERATOR','BUILDER') NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------

--
-- Table structure for table `player`
--

CREATE TABLE `player` (
  `UUID` char(36) NOT NULL,
  `username` varchar(16) NOT NULL,
  `preference` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '{}' CHECK (json_valid(`preference`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Indexes for dumped tables
--

--
-- Indexes for table `claim`
--
ALTER TABLE `claim`
  ADD PRIMARY KEY (`claim_id`);

--
-- Indexes for table `claim_chunk`
--
ALTER TABLE `claim_chunk`
  ADD UNIQUE KEY `world` (`world`,`x`,`z`),
  ADD KEY `claim_id` (`claim_id`);

--
-- Indexes for table `claim_permission`
--
ALTER TABLE `claim_permission`
  ADD UNIQUE KEY `user_uuid` (`user_uuid`,`claim_id`,`level`),
  ADD KEY `chunk_id` (`claim_id`);

--
-- Indexes for table `player`
--
ALTER TABLE `player`
  ADD PRIMARY KEY (`UUID`),
  ADD KEY `username` (`username`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `claim`
--
ALTER TABLE `claim`
  MODIFY `claim_id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `claim_chunk`
--
ALTER TABLE `claim_chunk`
  ADD CONSTRAINT `claim_id` FOREIGN KEY (`claim_id`) REFERENCES `claim` (`claim_id`) ON DELETE CASCADE;

--
-- Constraints for table `claim_permission`
--
ALTER TABLE `claim_permission`
  ADD CONSTRAINT `chunk_id` FOREIGN KEY (`claim_id`) REFERENCES `claim` (`claim_id`) ON DELETE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
