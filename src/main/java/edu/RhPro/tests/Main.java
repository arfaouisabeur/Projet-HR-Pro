package edu.RhPro.tests;

import edu.RhPro.tools.MyConnection;

public class Main {
    public static void main(String[] args) {
        int m1= MyConnection.getInstance().hashCode();
        System.out.println(m1);
    }
}
