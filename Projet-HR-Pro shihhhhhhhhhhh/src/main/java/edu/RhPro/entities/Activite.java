package edu.RhPro.entities;

import java.time.LocalDateTime;
public class Activite {

        private long id;
        private String titre;
        private String description;
        private long evenementId;

        // 🔹 Constructeur vide (obligatoire)
        public Activite() {
        }

        // 🔹 Constructeur sans id (INSERT)
        public Activite(String titre, String description, long evenementId) {
            this.titre = titre;
            this.description = description;
            this.evenementId = evenementId;
        }

        // 🔹 Constructeur avec id (SELECT)
        public Activite(long id, String titre, String description, long evenementId) {
            this.id = id;
            this.titre = titre;
            this.description = description;
            this.evenementId = evenementId;
        }

        // 🔹 Getters & Setters

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getTitre() {
            return titre;
        }

        public void setTitre(String titre) {
            this.titre = titre;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public long getEvenementId() {
            return evenementId;
        }

        public void setEvenementId(long evenementId) {
            this.evenementId = evenementId;
        }

        @Override
        public String toString() {
            return "Activite{" +
                    "id=" + id +
                    ", titre='" + titre + '\'' +
                    '}';
        }
    }

