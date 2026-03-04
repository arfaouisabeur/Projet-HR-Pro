-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Hôte : 127.0.0.1
-- Généré le : lun. 02 mars 2026 à 20:13
-- Version du serveur : 10.4.32-MariaDB
-- Version de PHP : 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Base de données : `pidev`
--

-- --------------------------------------------------------

--
-- Structure de la table `activite`
--

CREATE TABLE `activite` (
  `id` bigint(20) NOT NULL,
  `titre` varchar(200) NOT NULL,
  `description` text DEFAULT NULL,
  `evenement_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `activite`
--

INSERT INTO `activite` (`id`, `titre`, `description`, `evenement_id`) VALUES
(3, 'formation', 'tttttttt', 2),
(4, 'test act', 'c un test du act', 2);

-- --------------------------------------------------------

--
-- Structure de la table `candidat`
--

CREATE TABLE `candidat` (
  `user_id` bigint(20) NOT NULL,
  `cv` text DEFAULT NULL,
  `niveau_etude` varchar(120) DEFAULT NULL,
  `experience` int(11) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `candidature`
--

CREATE TABLE `candidature` (
  `id` bigint(20) NOT NULL,
  `date_candidature` date NOT NULL,
  `statut` varchar(20) NOT NULL,
  `cv` text DEFAULT NULL,
  `candidat_id` bigint(20) NOT NULL,
  `offre_emploi_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `conge_tt`
--

CREATE TABLE `conge_tt` (
  `id` bigint(20) NOT NULL,
  `type_conge` varchar(10) NOT NULL,
  `date_debut` date NOT NULL,
  `date_fin` date NOT NULL,
  `statut` varchar(20) NOT NULL,
  `description` text DEFAULT NULL,
  `employe_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `demande_service`
--

CREATE TABLE `demande_service` (
  `id` bigint(20) NOT NULL,
  `titre` varchar(200) NOT NULL,
  `description` text DEFAULT NULL,
  `date_demande` date NOT NULL,
  `statut` varchar(20) NOT NULL,
  `employe_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `employe`
--

CREATE TABLE `employe` (
  `user_id` bigint(20) NOT NULL,
  `matricule` varchar(60) NOT NULL,
  `position` varchar(120) NOT NULL,
  `date_embauche` date NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `employe`
--

INSERT INTO `employe` (`user_id`, `matricule`, `position`, `date_embauche`) VALUES
(2, 'EMP001', 'Caissier', '2026-02-01'),
(4, '456987', '555', '2026-02-05');

-- --------------------------------------------------------

--
-- Structure de la table `evenement`
--

CREATE TABLE `evenement` (
  `id` bigint(20) NOT NULL,
  `titre` varchar(200) NOT NULL,
  `date_debut` datetime NOT NULL,
  `date_fin` datetime NOT NULL,
  `lieu` varchar(200) NOT NULL,
  `description` text DEFAULT NULL,
  `rh_id` bigint(20) NOT NULL,
  `image_url` varchar(500) DEFAULT NULL,
  `latitude` double DEFAULT NULL,
  `longitude` double DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `evenement`
--

INSERT INTO `evenement` (`id`, `titre`, `date_debut`, `date_fin`, `lieu`, `description`, `rh_id`, `image_url`, `latitude`, `longitude`) VALUES
(2, 'Lomou', '2026-12-20 09:00:00', '2026-12-24 18:00:00', '36.838843318416046, 10.298390682958727', 'PAYE', 3, 'https://i.ibb.co/bMb7KRFv/1b1062ee4d27.png', 0, 0),
(4, 'formation', '2026-01-27 00:00:00', '2026-01-28 00:00:00', 'lac', 'testttttttttttttttttttttttttttttttttttt', 3, 'https://img.freepik.com/vecteurs-libre/fond-camp-ete-tente-feu-camp_23-2147802700.jpg?t=st=1771148340~exp=1771151940~hmac=6a0312e5986b3400c9a3e6b21b4eb183e25bc924b38f35c2a25b658c01694caf&w=2000', NULL, NULL),
(5, 'formation', '2026-01-27 00:00:00', '2026-01-28 00:00:00', 'lac', 'testttttttttttttttttttttttttttttttttttt', 1, 'https://img.freepik.com/vecteurs-libre/fond-camp-ete-tente-feu-camp_23-2147802700.jpg?t=st=1771148340~exp=1771151940~hmac=6a0312e5986b3400c9a3e6b21b4eb183e25bc924b38f35c2a25b658c01694caf&w=2000', NULL, NULL),
(6, 'Soiree Ramadhan', '2026-01-27 00:00:00', '2026-01-28 00:00:00', 'Esprit, نهج نيوتن, 2088 Ariana, Tunisia', 'test after adding new image and location', 3, 'https://i.ibb.co/Zphdg3pt/ca612a735f56.jpg', 36.89977705, 10.189972832663749),
(7, 'formation sprintboot', '2026-01-28 00:00:00', '2026-01-29 00:00:00', 'lac 3', 'testtttt', 3, 'https://img.freepik.com/vecteurs-libre/fond-camp-ete-tente-feu-camp_23-2147802700.jpg?t=st=1771148340~exp=1771151940~hmac=6a0312e5986b3400c9a3e6b21b4eb183e25bc924b38f35c2a25b658c01694caf&w=2000', NULL, NULL);

-- --------------------------------------------------------

--
-- Structure de la table `event_participation`
--

CREATE TABLE `event_participation` (
  `id` bigint(20) NOT NULL,
  `date_inscription` date NOT NULL,
  `statut` varchar(60) NOT NULL,
  `evenement_id` bigint(20) NOT NULL,
  `employe_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `event_participation`
--

INSERT INTO `event_participation` (`id`, `date_inscription`, `statut`, `evenement_id`, `employe_id`) VALUES
(24, '2026-02-15', 'ACCEPTEE', 2, 4),
(25, '2026-02-15', 'REFUSEE', 4, 4),
(26, '2026-02-15', 'EN_ATTENTE', 5, 4);

-- --------------------------------------------------------

--
-- Structure de la table `offre_emploi`
--

CREATE TABLE `offre_emploi` (
  `id` bigint(20) NOT NULL,
  `titre` varchar(200) NOT NULL,
  `description` text NOT NULL,
  `localisation` varchar(200) NOT NULL,
  `type_contrat` varchar(80) NOT NULL,
  `date_publication` date NOT NULL,
  `date_expiration` date NOT NULL,
  `statut` varchar(20) NOT NULL,
  `rh_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `prime`
--

CREATE TABLE `prime` (
  `id` bigint(20) NOT NULL,
  `montant` decimal(12,2) NOT NULL,
  `date_attribution` date NOT NULL,
  `description` text DEFAULT NULL,
  `rh_id` bigint(20) NOT NULL,
  `employe_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `projet`
--

CREATE TABLE `projet` (
  `id` bigint(20) NOT NULL,
  `titre` varchar(200) NOT NULL,
  `statut` varchar(10) NOT NULL,
  `description` text DEFAULT NULL,
  `rh_id` bigint(20) NOT NULL,
  `responsable_employe_id` bigint(20) NOT NULL,
  `date_debut` date NOT NULL,
  `date_fin` date NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `projet`
--

INSERT INTO `projet` (`id`, `titre`, `statut`, `description`, `rh_id`, `responsable_employe_id`, `date_debut`, `date_fin`) VALUES
(2, 'RH website', 'DOING', 'RH managment in web ', 3, 4, '2026-02-12', '2026-03-13'),
(4, 'testing', 'DOING', 'test', 3, 4, '2026-02-13', '2026-02-20'),
(5, 'testing2', 'DOING', 'test', 3, 4, '2026-02-13', '2026-02-20'),
(6, 'TEST_Application_1771270721546', 'DOING', 'TEST_Développement application mobile', 1, 2, '2026-02-16', '2026-04-16'),
(7, 'TEST_Application_1771270721628', 'DOING', 'TEST_Développement application mobile', 1, 2, '2026-02-16', '2026-04-16'),
(8, 'TEST_Application_1771270721640', 'DOING', 'TEST_Développement application mobile', 1, 2, '2026-02-16', '2026-04-16'),
(9, 'TEST_Update_1771270721653', 'DONE', 'TEST_Description mise à jour', 1, 2, '2026-02-16', '2026-04-16'),
(10, 'TEST_Application_1771270721663', 'DOING', 'TEST_Développement application mobile', 1, 2, '2026-02-16', '2026-04-16'),
(12, 'TEST_Projet_Dates_Invalides', 'DOING', 'Description test', 1, 2, '2026-02-21', '2026-02-16'),
(13, 'TEST_Projet_1_1771270721698', 'DOING', 'Description 1', 1, 2, '2026-02-16', '2026-03-16'),
(15, 'TEST_Application_1771270818903', 'DOING', 'TEST_Développement application mobile', 1, 2, '2026-02-16', '2026-04-16'),
(16, 'TEST_Application_1771270818975', 'DOING', 'TEST_Développement application mobile', 1, 2, '2026-02-16', '2026-04-16'),
(17, 'TEST_Application_1771270818986', 'DOING', 'TEST_Développement application mobile', 1, 2, '2026-02-16', '2026-04-16'),
(18, 'TEST_Update_1771270819007', 'DONE', 'TEST_Description mise à jour', 1, 2, '2026-02-16', '2026-04-16'),
(19, 'TEST_Application_1771270819016', 'DOING', 'TEST_Développement application mobile', 1, 2, '2026-02-16', '2026-04-16'),
(21, 'TEST_Projet_Dates_Invalides_1771270819036', 'DOING', 'Description test', 1, 2, '2026-02-21', '2026-02-16'),
(23, 'TEST_Application_1771271216810', 'DOING', 'TEST_Développement application mobile', 1, 2, '2026-02-16', '2026-04-16'),
(24, 'TEST_Application_1771271216883', 'DOING', 'TEST_Développement application mobile', 1, 2, '2026-02-16', '2026-04-16'),
(25, 'TEST_Application_1771271216892', 'DOING', 'TEST_Développement application mobile', 1, 2, '2026-02-16', '2026-04-16'),
(26, 'TEST_Update_1771271216913', 'DONE', 'TEST_Description mise à jour', 1, 2, '2026-02-16', '2026-04-16'),
(27, 'TEST_Application_1771271216923', 'DOING', 'TEST_Développement application mobile', 1, 2, '2026-02-16', '2026-04-16'),
(29, 'TEST_Projet_Dates_Invalides_1771271216941', 'DOING', 'Description test', 1, 2, '2026-02-21', '2026-02-16'),
(31, 'TEST_Application_1771271303119', 'DOING', 'TEST_Développement application mobile', 1, 2, '2026-02-16', '2026-04-16'),
(32, 'TEST_Application_1771271303187', 'DOING', 'TEST_Développement application mobile', 1, 2, '2026-02-16', '2026-04-16'),
(33, 'TEST_Application_1771271303199', 'DOING', 'TEST_Développement application mobile', 1, 2, '2026-02-16', '2026-04-16'),
(34, 'TEST_Update_1771271303221', 'DONE', 'TEST_Description mise à jour', 1, 2, '2026-02-16', '2026-04-16'),
(35, 'TEST_Application_1771271303230', 'DOING', 'TEST_Développement application mobile', 1, 2, '2026-02-16', '2026-04-16'),
(37, 'TEST_Projet_Dates_Invalides_1771271303251', 'DOING', 'Description test', 1, 2, '2026-02-21', '2026-02-16'),
(38, 'TEST_Projet_1_1771271303261', 'DOING', 'Description 1', 1, 2, '2026-02-16', '2026-03-16'),
(39, 'TEST_Projet_2_1771271303261', 'DOING', 'Description 2', 1, 4, '2026-02-21', '2026-04-16'),
(40, 'TEST_Application_1771271395482', 'DOING', 'TEST_Développement application mobile', 1, 2, '2026-02-16', '2026-04-16'),
(41, 'TEST_Application_1771271395560', 'DOING', 'TEST_Développement application mobile', 1, 2, '2026-02-16', '2026-04-16'),
(42, 'TEST_Application_1771271395570', 'DOING', 'TEST_Développement application mobile', 1, 2, '2026-02-16', '2026-04-16'),
(43, 'TEST_Update_1771271395589', 'DONE', 'TEST_Description mise à jour', 1, 2, '2026-02-16', '2026-04-16'),
(44, 'TEST_Application_1771271395598', 'DOING', 'TEST_Développement application mobile', 1, 2, '2026-02-16', '2026-04-16'),
(46, 'TEST_Projet_Dates_Invalides_1771271395622', 'DOING', 'Description test', 1, 2, '2026-02-21', '2026-02-16'),
(47, 'TEST_Projet_1_1771271395633', 'DOING', 'Description 1', 1, 2, '2026-02-16', '2026-03-16'),
(48, 'TEST_Projet_2_1771271395633', 'DOING', 'Description 2', 1, 4, '2026-02-21', '2026-04-16'),
(49, 'TEST_Application_1771842349883', 'DOING', 'TEST_Développement application mobile', 1, 2, '2026-02-23', '2026-04-23'),
(50, 'TEST_Application_1771842349970', 'DOING', 'TEST_Développement application mobile', 1, 2, '2026-02-23', '2026-04-23'),
(51, 'TEST_Application_1771842349984', 'DOING', 'TEST_Développement application mobile', 1, 2, '2026-02-23', '2026-04-23'),
(52, 'TEST_Update_1771842350007', 'DONE', 'TEST_Description mise à jour', 1, 2, '2026-02-23', '2026-04-23'),
(53, 'TEST_Application_1771842350017', 'DOING', 'TEST_Développement application mobile', 1, 2, '2026-02-23', '2026-04-23'),
(55, 'TEST_Projet_Dates_Invalides_1771842350046', 'DOING', 'Description test', 1, 2, '2026-02-28', '2026-02-23'),
(56, 'TEST_Projet_1_1771842350056', 'DOING', 'Description 1', 1, 2, '2026-02-23', '2026-03-23'),
(57, 'TEST_Projet_2_1771842350056', 'DOING', 'Description 2', 1, 4, '2026-02-28', '2026-04-23'),
(58, 'ggggggggggggggggg', 'DOING', 'gggggggggggggggg', 3, 4, '2026-03-03', '2026-04-03'),
(59, 'rrrrrrrrrrrrrrrrrrrrrrrrrrrrrr', 'DOING', 'rstnsrtndtnfgnb', 3, 4, '2026-03-05', '2026-03-31');

-- --------------------------------------------------------

--
-- Structure de la table `rating`
--

CREATE TABLE `rating` (
  `id` bigint(20) NOT NULL,
  `evenement_id` bigint(20) NOT NULL,
  `employe_id` bigint(20) NOT NULL,
  `commentaire` text NOT NULL,
  `etoiles` int(11) NOT NULL CHECK (`etoiles` between 1 and 5),
  `date_creation` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `rating`
--

INSERT INTO `rating` (`id`, `evenement_id`, `employe_id`, `commentaire`, `etoiles`, `date_creation`) VALUES
(1, 2, 4, 'very good , i love it', 3, '2026-03-01 16:33:08'),
(2, 7, 4, 'désastre', 1, '2026-03-01 16:34:01');

-- --------------------------------------------------------

--
-- Structure de la table `reponse`
--

CREATE TABLE `reponse` (
  `id` bigint(20) NOT NULL,
  `decision` varchar(60) NOT NULL,
  `commentaire` text DEFAULT NULL,
  `rh_id` bigint(20) NOT NULL,
  `employe_id` bigint(20) NOT NULL,
  `conge_tt_id` bigint(20) DEFAULT NULL,
  `demande_service_id` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `rh`
--

CREATE TABLE `rh` (
  `user_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `rh`
--

INSERT INTO `rh` (`user_id`) VALUES
(1),
(3);

-- --------------------------------------------------------

--
-- Structure de la table `salaire`
--

CREATE TABLE `salaire` (
  `id` bigint(20) NOT NULL,
  `mois` int(11) NOT NULL,
  `annee` int(11) NOT NULL,
  `montant` decimal(12,2) NOT NULL,
  `date_paiement` date DEFAULT NULL,
  `statut` varchar(20) NOT NULL,
  `rh_id` bigint(20) NOT NULL,
  `employe_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `tache`
--

CREATE TABLE `tache` (
  `id` bigint(20) NOT NULL,
  `titre` varchar(200) NOT NULL,
  `statut` varchar(10) NOT NULL,
  `description` text DEFAULT NULL,
  `projet_id` bigint(20) NOT NULL,
  `employe_id` bigint(20) NOT NULL,
  `prime_id` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `tache`
--

INSERT INTO `tache` (`id`, `titre`, `statut`, `description`, `projet_id`, `employe_id`, `prime_id`) VALUES
(1, 'backend auth', 'DONE', 'creation api backend', 2, 4, NULL),
(2, 'test tes t', 'DONE', 'hhhhhhhhhhhhhhhhhhhhhhh', 2, 4, NULL),
(3, 'new test', 'DOING', 'new ', 2, 4, NULL);

-- --------------------------------------------------------

--
-- Structure de la table `users`
--

CREATE TABLE `users` (
  `id` bigint(20) NOT NULL,
  `nom` varchar(120) NOT NULL,
  `prenom` varchar(120) NOT NULL,
  `email` varchar(255) NOT NULL,
  `mot_de_passe` varchar(255) NOT NULL,
  `telephone` varchar(40) DEFAULT NULL,
  `adresse` varchar(255) DEFAULT NULL,
  `role` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `users`
--

INSERT INTO `users` (`id`, `nom`, `prenom`, `email`, `mot_de_passe`, `telephone`, `adresse`, `role`) VALUES
(1, 'RH', 'Admin', 'rh@rhpro.com', '123', '00000000', 'HQ', 'RH'),
(2, 'Employe', 'Test', 'emp@rhpro.com', '123', '11111111', 'Store', 'condidat'),
(3, 'leila', 'adouani', 'leila@gmail', 'leila', '123456', 'tunis', 'RH'),
(4, 'adoueni', 'leila', 'semer.belghith@esprit.tn', 'leila', '95507847', 'tunis', 'EMPLOYE');

--
-- Index pour les tables déchargées
--

--
-- Index pour la table `activite`
--
ALTER TABLE `activite`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_activite_evenement` (`evenement_id`);

--
-- Index pour la table `candidat`
--
ALTER TABLE `candidat`
  ADD PRIMARY KEY (`user_id`);

--
-- Index pour la table `candidature`
--
ALTER TABLE `candidature`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uq_candidature_unique` (`candidat_id`,`offre_emploi_id`),
  ADD KEY `fk_candidature_offre` (`offre_emploi_id`);

--
-- Index pour la table `conge_tt`
--
ALTER TABLE `conge_tt`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_conge_employe` (`employe_id`);

--
-- Index pour la table `demande_service`
--
ALTER TABLE `demande_service`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_demande_service_employe` (`employe_id`);

--
-- Index pour la table `employe`
--
ALTER TABLE `employe`
  ADD PRIMARY KEY (`user_id`),
  ADD UNIQUE KEY `matricule` (`matricule`);

--
-- Index pour la table `evenement`
--
ALTER TABLE `evenement`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_evenement_rh` (`rh_id`);

--
-- Index pour la table `event_participation`
--
ALTER TABLE `event_participation`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_participation_evenement` (`evenement_id`),
  ADD KEY `fk_participation_employe` (`employe_id`);

--
-- Index pour la table `offre_emploi`
--
ALTER TABLE `offre_emploi`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_offre_rh` (`rh_id`);

--
-- Index pour la table `prime`
--
ALTER TABLE `prime`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_prime_rh` (`rh_id`),
  ADD KEY `fk_prime_employe` (`employe_id`);

--
-- Index pour la table `projet`
--
ALTER TABLE `projet`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_projet_rh` (`rh_id`),
  ADD KEY `fk_projet_responsable` (`responsable_employe_id`);

--
-- Index pour la table `rating`
--
ALTER TABLE `rating`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `unique_rating` (`evenement_id`,`employe_id`),
  ADD KEY `employe_id` (`employe_id`);

--
-- Index pour la table `reponse`
--
ALTER TABLE `reponse`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_reponse_rh` (`rh_id`),
  ADD KEY `fk_reponse_employe` (`employe_id`),
  ADD KEY `fk_reponse_conge` (`conge_tt_id`),
  ADD KEY `fk_reponse_demande` (`demande_service_id`);

--
-- Index pour la table `rh`
--
ALTER TABLE `rh`
  ADD PRIMARY KEY (`user_id`);

--
-- Index pour la table `salaire`
--
ALTER TABLE `salaire`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_salaire_rh` (`rh_id`),
  ADD KEY `fk_salaire_employe` (`employe_id`);

--
-- Index pour la table `tache`
--
ALTER TABLE `tache`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_tache_projet` (`projet_id`),
  ADD KEY `fk_tache_employe` (`employe_id`),
  ADD KEY `fk_tache_prime` (`prime_id`);

--
-- Index pour la table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `email` (`email`);

--
-- AUTO_INCREMENT pour les tables déchargées
--

--
-- AUTO_INCREMENT pour la table `activite`
--
ALTER TABLE `activite`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT pour la table `candidature`
--
ALTER TABLE `candidature`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `conge_tt`
--
ALTER TABLE `conge_tt`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `demande_service`
--
ALTER TABLE `demande_service`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `evenement`
--
ALTER TABLE `evenement`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=64;

--
-- AUTO_INCREMENT pour la table `event_participation`
--
ALTER TABLE `event_participation`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=27;

--
-- AUTO_INCREMENT pour la table `offre_emploi`
--
ALTER TABLE `offre_emploi`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `prime`
--
ALTER TABLE `prime`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `projet`
--
ALTER TABLE `projet`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=60;

--
-- AUTO_INCREMENT pour la table `rating`
--
ALTER TABLE `rating`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT pour la table `reponse`
--
ALTER TABLE `reponse`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `salaire`
--
ALTER TABLE `salaire`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `tache`
--
ALTER TABLE `tache`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT pour la table `users`
--
ALTER TABLE `users`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- Contraintes pour les tables déchargées
--

--
-- Contraintes pour la table `activite`
--
ALTER TABLE `activite`
  ADD CONSTRAINT `fk_activite_evenement` FOREIGN KEY (`evenement_id`) REFERENCES `evenement` (`id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `candidat`
--
ALTER TABLE `candidat`
  ADD CONSTRAINT `fk_candidat_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `candidature`
--
ALTER TABLE `candidature`
  ADD CONSTRAINT `fk_candidature_candidat` FOREIGN KEY (`candidat_id`) REFERENCES `candidat` (`user_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_candidature_offre` FOREIGN KEY (`offre_emploi_id`) REFERENCES `offre_emploi` (`id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `conge_tt`
--
ALTER TABLE `conge_tt`
  ADD CONSTRAINT `fk_conge_employe` FOREIGN KEY (`employe_id`) REFERENCES `employe` (`user_id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `demande_service`
--
ALTER TABLE `demande_service`
  ADD CONSTRAINT `fk_demande_service_employe` FOREIGN KEY (`employe_id`) REFERENCES `employe` (`user_id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `employe`
--
ALTER TABLE `employe`
  ADD CONSTRAINT `fk_employe_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `evenement`
--
ALTER TABLE `evenement`
  ADD CONSTRAINT `fk_evenement_rh` FOREIGN KEY (`rh_id`) REFERENCES `rh` (`user_id`);

--
-- Contraintes pour la table `event_participation`
--
ALTER TABLE `event_participation`
  ADD CONSTRAINT `fk_participation_employe` FOREIGN KEY (`employe_id`) REFERENCES `employe` (`user_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_participation_evenement` FOREIGN KEY (`evenement_id`) REFERENCES `evenement` (`id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `offre_emploi`
--
ALTER TABLE `offre_emploi`
  ADD CONSTRAINT `fk_offre_rh` FOREIGN KEY (`rh_id`) REFERENCES `rh` (`user_id`);

--
-- Contraintes pour la table `prime`
--
ALTER TABLE `prime`
  ADD CONSTRAINT `fk_prime_employe` FOREIGN KEY (`employe_id`) REFERENCES `employe` (`user_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_prime_rh` FOREIGN KEY (`rh_id`) REFERENCES `rh` (`user_id`);

--
-- Contraintes pour la table `projet`
--
ALTER TABLE `projet`
  ADD CONSTRAINT `fk_projet_responsable` FOREIGN KEY (`responsable_employe_id`) REFERENCES `employe` (`user_id`),
  ADD CONSTRAINT `fk_projet_rh` FOREIGN KEY (`rh_id`) REFERENCES `rh` (`user_id`);

--
-- Contraintes pour la table `rating`
--
ALTER TABLE `rating`
  ADD CONSTRAINT `rating_ibfk_1` FOREIGN KEY (`evenement_id`) REFERENCES `evenement` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `rating_ibfk_2` FOREIGN KEY (`employe_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `reponse`
--
ALTER TABLE `reponse`
  ADD CONSTRAINT `fk_reponse_conge` FOREIGN KEY (`conge_tt_id`) REFERENCES `conge_tt` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_reponse_demande` FOREIGN KEY (`demande_service_id`) REFERENCES `demande_service` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_reponse_employe` FOREIGN KEY (`employe_id`) REFERENCES `employe` (`user_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_reponse_rh` FOREIGN KEY (`rh_id`) REFERENCES `rh` (`user_id`);

--
-- Contraintes pour la table `rh`
--
ALTER TABLE `rh`
  ADD CONSTRAINT `fk_rh_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `salaire`
--
ALTER TABLE `salaire`
  ADD CONSTRAINT `fk_salaire_employe` FOREIGN KEY (`employe_id`) REFERENCES `employe` (`user_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_salaire_rh` FOREIGN KEY (`rh_id`) REFERENCES `rh` (`user_id`);

--
-- Contraintes pour la table `tache`
--
ALTER TABLE `tache`
  ADD CONSTRAINT `fk_tache_employe` FOREIGN KEY (`employe_id`) REFERENCES `employe` (`user_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_tache_prime` FOREIGN KEY (`prime_id`) REFERENCES `prime` (`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `fk_tache_projet` FOREIGN KEY (`projet_id`) REFERENCES `projet` (`id`) ON DELETE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
