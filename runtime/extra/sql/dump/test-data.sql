-- MariaDB dump 10.17  Distrib 10.5.4-MariaDB, for debian-linux-gnu (x86_64)
--
-- Host: localhost    Database: cldsdb4
-- ------------------------------------------------------
-- Server version	10.5.4-MariaDB-1:10.5.4+maria~focal

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Current Database: `cldsdb4`
--

USE `cldsdb4`;

--
-- Dumping data for table `loop_element_models`
--

LOCK TABLES `loop_element_models` WRITE;
/*!40000 ALTER TABLE `loop_element_models` DISABLE KEYS */;
/*!40000 ALTER TABLE `loop_element_models` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `loop_templates`
--

LOCK TABLES `loop_templates` WRITE;
/*!40000 ALTER TABLE `loop_templates` DISABLE KEYS */;
/*!40000 ALTER TABLE `loop_templates` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `loopelementmodels_to_policymodels`
--

LOCK TABLES `loopelementmodels_to_policymodels` WRITE;
/*!40000 ALTER TABLE `loopelementmodels_to_policymodels` DISABLE KEYS */;
/*!40000 ALTER TABLE `loopelementmodels_to_policymodels` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `looptemplates_to_loopelementmodels`
--

LOCK TABLES `looptemplates_to_loopelementmodels` WRITE;
/*!40000 ALTER TABLE `looptemplates_to_loopelementmodels` DISABLE KEYS */;
/*!40000 ALTER TABLE `looptemplates_to_loopelementmodels` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `policy_models`
--

LOCK TABLES `policy_models` WRITE;
/*!40000 ALTER TABLE `policy_models` DISABLE KEYS */;
/*!40000 ALTER TABLE `policy_models` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `services`
--

LOCK TABLES `services` WRITE;
/*!40000 ALTER TABLE `services` DISABLE KEYS */;
/*!40000 ALTER TABLE `services` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2022-04-22 10:19:58
