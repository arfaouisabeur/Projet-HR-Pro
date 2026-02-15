package edu.RhPro.utils;

import edu.RhPro.entities.User;

public class Session {
    private static User currentUser;
    private static String selectedRole; // chosen at welcome screen

    public static User getCurrentUser() { return currentUser; }
    public static void setCurrentUser(User u) { currentUser = u; }

    public static String getSelectedRole() { return selectedRole; }
    public static void setSelectedRole(String role) { selectedRole = role; }

    public static void clear() {
        currentUser = null;
        selectedRole = null;
    }
}
