package edu.RhPro.tools;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyConnection {
    private String url="jdbc:mysql://localhost:3306/pidev";
    private String login="root";
    private String pwd="";
    public static MyConnection instance;
    private Connection cnx;

    private MyConnection(){
        try {
            cnx=DriverManager.getConnection(url,login,pwd);
            System.out.println("connection etablie yaa rojlaaaaaaaa ya lbabour ya mon amour");
        } catch (SQLException e) {
            System.out.println(e.getMessage());        }
    }
    public Connection getCnx() {
        return cnx;
    }
    public static MyConnection getInstance(){
        if (instance == null){
            instance = new MyConnection();
        }
        return instance;
    }
}
