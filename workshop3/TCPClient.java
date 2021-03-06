import java.net.*;
import java.nio.charset.*;
import java.util.*;
import java.io.*;

public class TCPClient {
    public static void main(String[] args) throws IOException {
        Socket sock = new Socket("192.168.56.2", 5000);
        try (InputStream input = sock.getInputStream()) {
            try (OutputStream output = sock.getOutputStream()) {
                handle(input, output);
            }
        }
        sock.close();
        System.out.println("disconnected.");
    }

    private static void handle(InputStream input, OutputStream output) throws IOException {
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