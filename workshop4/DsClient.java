import java.net.*;
import java.sql.Statement;
import java.util.*;
import java.io.*;

public class DsClient {
    enum Commands {
        HELO("HELO"),    //Initial message from client
        AUTH("AUTH"),    //Authentication information
        QUIT("QUIT"),    //Simulation termination

        REDY("REDY"),    //Client signals server for a job
        DATA("DATA"),    //The indicator for the actual information to be sent

        JOBN("JOBN"),    //Job submission information
        JOBP("JOBP"),    //Job resubmission information after pre-emption
        JCPL("JCPL"),    //Job completion
        RESF("RESF"),    //Server failure notice
        RESR("RESR"),    //Server recovery notice
        NONE("NONE"),    //No more jobs to schedule

        GETS("GETS"),    //Server information request
        SCHD("SCHD"),    //Scheduling decision
        CNTJ("CNTJ"),    //The number of jobs on a specified server with a particular state
        EJWT("EJWT"),    //The sum of estimated waiting time on a given server
        LSTJ("LSTJ"),    //Job list of a server, i.e., all pending jobs (waiting and running jobs)
        PSHJ("PSHJ"),    //Force to get the next job to schedule skipping the current job
        MIGJ("MIGJ"),    //Migrate a job from a source server to a destination server
        KILJ("KILJ"),    //Kill a job
        TERM("TERM"),    //Server termination

        ERR("ERR"),    //Error message
        OK("OK");    //Response to a valid command

        private String description;

        Commands(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    ;

    enum Statement {
        INIT,
        AUTHENTICATION,
        READY,
        BREAK,
        QUIT,
        ACKQUIT,
    }

    ;
    Statement state = Statement.INIT;
    final String serverIP = "localhost";    // localhost 10.0.0.137
    final int serverPort = 50000;
    final long packetTimeout = 5000;


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
        }
        System.out.println("Connection disconnected.");
    }

    public String cReadLine(long timeOut, BufferedReader reader) throws IOException {
        boolean overTimeFlag = false;//超时标志位
        long l1 = System.currentTimeMillis();
        String s = null;

        while (reader.ready() == false) {
            long l2 = System.currentTimeMillis();
            if (l2 - l1 > timeOut) {
                System.out.println("System timeout " + timeOut + " milliseconds, no packet received");
                return null;
            }
        }
        return reader.readLine();
    }

    private void handle(InputStream input, OutputStream output) throws IOException {
        var writer = new BufferedWriter(new OutputStreamWriter(output));
        var reader = new BufferedReader(new InputStreamReader(input));
        String resp = null;
        String sendString;
        for (; ; ) {

            switch (state) {
                case INIT:
                    sendString = Commands.HELO.getDescription() + "\n";
                    writer.write(sendString);
                    writer.flush();
                    state = Statement.AUTHENTICATION;
                    break;
                case AUTHENTICATION:
                    if (resp.equalsIgnoreCase(Commands.OK.getDescription())) {
                        sendString = Commands.AUTH.getDescription() + System.getProperty("user.name") + "\n";
                        writer.write(sendString);
                        writer.flush();
                        state = Statement.READY;
                    } else {
                        state = Statement.QUIT;
                        continue;
                    }
                    break;
                case READY:
                    if (resp.equalsIgnoreCase(Commands.OK.getDescription())) {
                        sendString = Commands.REDY.getDescription() + "\n";
                        writer.write(sendString);
                        writer.flush();
                        state = Statement.QUIT;
                    } else {
                        state = Statement.QUIT;
                        continue;
                    }
                    break;




                case QUIT:
                    sendString = Commands.QUIT.getDescription() + "\n";
                    writer.write(sendString);
                    writer.flush();
                    state = Statement.ACKQUIT;
                    break;
                case ACKQUIT:
                    if (resp.equalsIgnoreCase(Commands.QUIT.getDescription())) {
                        System.out.println("Loop end normal");
                        return;
                    } else {
                        state = Statement.BREAK;
                        continue;
                    }
                case BREAK:
                    System.out.println("Loop break");
                    return;
            }

            resp = cReadLine(packetTimeout, reader);
            System.out.println("RX>>> " + resp);

        }
    }
}