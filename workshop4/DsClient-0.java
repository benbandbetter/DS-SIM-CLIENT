import java.net.*;
import java.nio.charset.*;
import java.util.*;
import java.io.*;

public class DsClient {
    final String serverIP = "10.0.0.137";    // localhost
    final int serverPort = 5000;

    public static void main(String[] args) throws IOException {
        DsClient client = new DsClient();
        client.TcpSocketCon();
    }

    void TcpSocketCon() throws IOException {
        Socket sock = new Socket(serverIP, serverPort);
        System.out.println("TCP connection established.");
        try (InputStream input = sock.getInputStream()) {
            try (OutputStream output = sock.getOutputStream()) {
                handle(input, output);
            }
        } catch (Exception e) {
            try {
                sock.close();
            } catch (IOException ioe) {
            }
            System.out.println("Connection disconnected.");
        }
    }

    private void handle(InputStream input, OutputStream output) throws IOException {
        var writer = new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8));
        var reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
        Scanner scanner = new Scanner(System.in);
        if (reader.ready() == true)
            System.out.println("[server] " + reader.readLine());
        for (;;) {
            System.out.print("Tx <<< ");
            String s = scanner.nextLine();
            writer.write(s);
            writer.newLine();
            writer.flush();

            String resp = reader.readLine();
            System.out.println("RX>>> " + resp);
            if (resp.equalsIgnoreCase("bye")) {
                scanner.close();
                break;
            }
        }
    }
}