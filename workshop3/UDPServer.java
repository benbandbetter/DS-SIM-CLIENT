import java.net.*;
import java.nio.charset.*;
import java.util.*;
import java.io.*;

public class UDPServer {
    public static void main(String[] args) throws IOException {
        DatagramSocket ds = new DatagramSocket(5000);
        byte[] buffer = new byte[1024];
        byte[] data;
        for (;;) {

            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            ds.receive(packet);
            String s = new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
            System.out.println("S-r" + "<<< " + s);

            if (s.equalsIgnoreCase("bye") == true) {
                data = "BYE\n".getBytes(StandardCharsets.UTF_8);
                packet.setData(data);
                ds.send(packet);
                System.out.println("client disconnected.");
                ds.disconnect();
                break;
            } else if (s.equalsIgnoreCase("HELO") == true) {
                data = "G'DAY\n".getBytes(StandardCharsets.UTF_8);
                packet.setData(data);
                ds.send(packet);
            } else {
                s = "ok: " + s;
                data = s.getBytes(StandardCharsets.UTF_8);
                packet.setData(data);
                ds.send(packet);
            }
        }
    }
}