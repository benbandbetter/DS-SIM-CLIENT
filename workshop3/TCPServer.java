import java.net.*;
import java.nio.charset.*;
import java.io.*;

public class TCPServer {
    public static void main(String[] args) throws IOException {
        ServerSocket ss = new ServerSocket(5000);
        System.out.println("server is running...");
        for (;;) {
            Socket sock = ss.accept();
            System.out.println("new connection from " + sock.getRemoteSocketAddress());
            Thread t = new Handler(sock);
            t.start();
        }
    }
}

class Handler extends Thread {
    Socket sock;

    public Handler(Socket sock) {
        this.sock = sock;
    }

    @Override
    public void run() {
        try (InputStream input = this.sock.getInputStream()) {
            try (OutputStream output = this.sock.getOutputStream()) {
                handle(input, output);
                System.out.println("Sever close socket itself.");
            }
        } catch (Exception e) {
            try {
                this.sock.close();
            } catch (IOException ioe) {
            }
            System.out.println("client disconnected.");
        }

    }

    private void handle(InputStream input, OutputStream output) throws IOException {
        var writer = new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8));
        var reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
        // writer.write("hello\n");
        // writer.flush();
        for (;;) {
            String s = reader.readLine();
            System.out.println("S-r" + sock.getRemoteSocketAddress() + "<<< " + s);
            if (s.equalsIgnoreCase("bye") == true) {
                writer.write("BYE\n");
                writer.flush();
                break;
            } else if (s.equalsIgnoreCase("HELO") == true) {
                writer.write("G'DAY\n");
                writer.flush();
            } else {
                writer.write("ok: " + s + "\n");
                writer.flush();
            }

        }
    }
}