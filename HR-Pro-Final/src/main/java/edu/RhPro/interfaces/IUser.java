package edu.RhPro.interfaces;

import edu.RhPro.entities.User;
import java.sql.SQLException;
import java.util.List;

public interface IUser {

    void addUser(User user) throws SQLException;
    int addUserAndReturnId(User user) throws SQLException;   // ✅

    void removeUser(User user) throws SQLException;
    void updateUser(User user) throws SQLException;

    List<User> getData() throws SQLException;

    User findByEmail(String email) throws SQLException;      // ✅
    User authenticate(String email, String password) throws SQLException; // ✅
}
