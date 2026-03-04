import java.net.Socket;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class TestConnection {
    public static void main(String[] args) {
        try {
            System.out.println("Testing connection to localhost:5000...");
            Socket socket = new Socket("localhost", 5000);
            System.out.println(" Connected!");

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println("Test message");
            System.out.println(" Sent test message");

            String response = in.readLine();
            System.out.println(" Response: " + response);
            socket.close();
        } catch (Exception e) {
            System.out.println(" Failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
