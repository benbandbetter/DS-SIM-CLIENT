import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.net.*;
import java.io.*;
import java.util.*;


public class DsClient {
    // default setup
    Statement state = Statement.INIT;
    String serverIP = "localhost";    // localhost 10.0.0.137
    int serverPort = 50000;
    final long packetTimeout = 5000;
    final File dsSystemXML = new File("ds-system.xml");
    List<ServerType> serversList = new ArrayList<ServerType>();
    BufferedWriter writer = null;
    BufferedReader reader = null;
    int serverIndex_LRR = 0;
    String algorithmMode = "LRR";

    /*
     *  Main
     */
    public static void main(String[] args) throws Exception {
        DsClient client = new DsClient();
        client.processArgument(args);
        client.TcpSocketCon();
    }

    /*
     *  process argument get serverIP, serverPort, algorithmMode.
     *  command example:
     *      java DsClient localhost 50000 lrr
     */
    public void processArgument(String[] argument) {
        if (argument.length != 3) {
            System.out.println("Running default setup");
        } else {
            serverIP = argument[0];
            serverPort = Integer.parseInt(argument[1]);
            algorithmMode = argument[2];
            System.out.println("Running with server:" + serverIP + " port:" + serverPort+ " Mode:" + algorithmMode);
        }
    }

    /*
     *  Handle Tcp socket connection and error throw out
     */
    public void TcpSocketCon() throws IOException {
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
        System.out.println("Socket terminate, connection disconnected!");
    }

    /*
     *  ReadLine is a blocking function, I add timeout feature and modify it to non-blocking
     */
    public String cReadLine(long timeOut, BufferedReader reader) throws Exception {
        long l1 = System.currentTimeMillis();
        String s = null;

        while (reader.ready() == false) {
            Thread.sleep(10);
            long l2 = System.currentTimeMillis();
            if (l2 - l1 > timeOut) {
                System.out.println("System timeout " + timeOut + " milliseconds, no packet received");
                return null;
            }
        }
        return reader.readLine();
    }

    public void cSendLine(String sendString) throws Exception {
        sendString  += "\n";
        writer.write(sendString);
        writer.flush();
    }

    /*
     *  ds-sim generate ds-system.xml after auth
     *  read the xml file, store server info into arraylist
     */
    public void processDsSystemXML() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(dsSystemXML);
            NodeList nl = doc.getElementsByTagName("server");
            for (int i = 0; i < nl.getLength(); i++) {
                String[] attribute = new String[7];
                Node node = nl.item(i);
                NamedNodeMap nnm = node.getAttributes();
                for (int j = 0; j < nnm.getLength(); j++) {
                    switch (nnm.item(j).getNodeName()) {
                        case "type":
                            attribute[0] = nnm.item(j).getNodeValue();
                            break;
                        case "limit":
                            attribute[1] = nnm.item(j).getNodeValue();
                            break;
                        case "bootupTime":
                            attribute[2] = nnm.item(j).getNodeValue();
                            break;
                        case "hourlyRate":
                            attribute[3] = nnm.item(j).getNodeValue();
                            break;
                        case "cores":
                            attribute[4] = nnm.item(j).getNodeValue();
                            break;
                        case "memory":
                            attribute[5] = nnm.item(j).getNodeValue();
                            break;
                        case "disk":
                            attribute[6] = nnm.item(j).getNodeValue();
                            break;
                    }
                }
                var serverType = new ServerType(attribute);
                serversList.add(serverType);
            }
            Collections.sort(serversList, Comparator.comparing(ServerType::getCores).reversed());
            //serversList.forEach(System.out::println);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /*
     *  jobSchedule algorithmMode
     *  LRR or ATL
     */
    public void jobSchedule(JobSubmission jobSubmission, List<ServerStatus> lServer ) throws Exception {
        if (algorithmMode.equalsIgnoreCase("LRR")) {
            if(serverIndex_LRR >= lServer.size()){
                serverIndex_LRR=0;
            }
            //SCHD 0 t2.aws 0
            String sendString = Commands.SCHD.getDescription() + " " + jobSubmission.jobID + " " +
                    lServer.get(serverIndex_LRR).type + " "+lServer.get(serverIndex_LRR).serverID;
            //System.out.println(sendString);
            cSendLine(sendString);

            if(++serverIndex_LRR >= lServer.size()){
                serverIndex_LRR=0;
            }
        } else {
            cSendLine(Commands.SCHD.getDescription() + " " + jobSubmission.jobID + " " +
                    serversList.get(0).type + " 0");
        }

    }
    /*
     * process JOBN command
     */
    public void processJob(DsClient dsClient, String resp) throws Exception {
        var jobSubmission = new JobSubmission(resp);
        cSendLine(jobSubmission.Gets());            // GETS Capable 1 700 600
        resp=reader.readLine();                     // DATA 7 124
        String[] command= resp.split("\\s+");
        int msgCount = Integer.parseInt(command[1]);
        cSendLine(Commands.OK.getDescription());    // OK

                                                    //t1.micro 0 inactive -1 2 4000 16000 0 0
                                                    //t1.micro 1 inactive -1 2 4000 16000 0 0
                                                    //t1.small 0 inactive -1 2 8000 32000 0 0
                                                    //t1.small 1 inactive -1 2 8000 32000 0 0
                                                    //t1.medium 0 inactive -1 4 16000 64000 0 0
                                                    //t1.medium 1 inactive -1 4 16000 64000 0 0
                                                    //t2.aws 0 inactive -1 16 64000 512000 0 0
        List<ServerStatus> lServer = new ArrayList<ServerStatus>();
        for(int i=0 ; i<msgCount; ++i) {
            resp = reader.readLine();
            var serverStatus = new ServerStatus(resp);
            // check how many largestServer are available, from the info of GET
            // add all the largestServer to Arraylist
            if(serverStatus.type.equals(serversList.get(0).type)){
                lServer.add(serverStatus);
            }
            //System.out.println(serverStatus);
        }
        cSendLine(Commands.OK.getDescription());    // OK

        resp=reader.readLine();                     // .
        jobSchedule(jobSubmission, lServer);        // SCHD 0 t2.aws 0
        resp=reader.readLine();                     // OK
    }

    /*
     *  Main loop
     */
    private void handle(InputStream input, OutputStream output) throws Exception {
        writer = new BufferedWriter(new OutputStreamWriter(output));
        reader = new BufferedReader(new InputStreamReader(input));
        String resp = null;
        boolean readline_Flag = true;
        while (true) {
            switch (state) {
                case INIT:
                    cSendLine(Commands.HELO.getDescription());
                    state = Statement.AUTHENTICATION;
                    break;
                case AUTHENTICATION:
                    if (resp.equalsIgnoreCase(Commands.OK.getDescription())) {
                        cSendLine(Commands.AUTH.getDescription() +" "+ System.getProperty("user.name"));
                        state = Statement.AUTHENTICATED;
                    } else {
                        state = Statement.QUIT;
                        readline_Flag=false;
                    }
                    break;
                case AUTHENTICATED:
                    if (resp.equalsIgnoreCase(Commands.OK.getDescription())) {
                        processDsSystemXML();
                        state = Statement.READY;
                    } else {
                        state = Statement.QUIT;
                    }
                    readline_Flag=false;
                    break;

                case READY:
                    cSendLine(Commands.REDY.getDescription());
                    state = Statement.READYNEXT;
                    break;

                case READYNEXT:
                    String[] command= resp.split("\\s+");
                    switch (command[0]){
                        case "JOBN":
                        case "JOBP":
                            processJob(this, resp);
                        case "JCPL":
                            state = Statement.READY;
                            readline_Flag=false;
                            break;

                        case "RESF":
                        case "RESR":
                        case "NONE":
                            state = Statement.QUIT;
                            readline_Flag=false;
                            break;
                    }
                    break;

                case QUIT:
                    cSendLine(Commands.QUIT.getDescription());
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
            if(readline_Flag) {
                resp = cReadLine(packetTimeout, reader);
                //System.out.println("RX>>> " + resp);
            } else {
                readline_Flag = true;
            }
        }
    }
}



