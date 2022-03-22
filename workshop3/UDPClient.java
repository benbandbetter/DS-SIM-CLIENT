import java.net.*;
import java.nio.charset.*;
import java.util.*;
import java.io.*;

public class UDPClient {
    public static void main(String[] args) throws IOException {
        DatagramSocket ds = new DatagramSocket();
        ds.setSoTimeout(1000);
        ds.connect(InetAddress.getByName("192.168.56.2"), 5000);
        Scanner scanner = new Scanner(System.in);
        String s;
        byte[] data;
        byte[] buffer;
        for (;;) {
            System.out.print("Tx <<< ");
            s = scanner.nextLine();
            data = s.getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length);
            ds.send(packet);

            buffer = new byte[1024];
            packet = new DatagramPacket(buffer, buffer.length);
            ds.receive(packet);
            String resp = new String(packet.getData(), packet.getOffset(), packet.getLength());
            System.out.println("RX>>> " + resp);
            if (resp.equalsIgnoreCase("bye\n")) {
                scanner.close();
                ds.disconnect();
                System.out.println("disconnected.");
                break;
            }
        }
    }
}
