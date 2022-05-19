import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class DsClient {
    // default setup
    String serverIP = "localhost"; // localhost 10.0.0.137
    int serverPort = 50000;
    String algorithmMode = "BF";
    final File dsSystemXML = new File("ds-system.xml");
    List<ServerType> serversList = new ArrayList<ServerType>();
    Statement state = Statement.INIT;
    final long packetTimeout = 5000;
    BufferedWriter writer = null;
    BufferedReader reader = null;
    int serverIndex_LRR = 0;
    String maxServerType_LRR = null;

    /*
     * Main
     */
    public static void main(String[] args) throws Exception {
        DsClient client = new DsClient();
        client.processArgument(args);
        client.TcpSocketCon();
    }

    /*
     * process argument get serverIP, serverPort, algorithmMode.
     * command example:
     * java DsClient localhost 50000 lrr
     */
    public void processArgument(String[] argument) {
        // if (argument.length != 3) {
        // System.out.println("Running default setup");
        // System.out.println("Running with server:" + serverIP + " port:" + serverPort
        // + " Mode:" + algorithmMode);

        // } else {
        // serverIP = argument[0];
        // serverPort = Integer.parseInt(argument[1]);
        // algorithmMode = argument[2].toUpperCase();
        // System.out.println("Running with server:" + serverIP + " port:" + serverPort
        // + " Mode:" + algorithmMode);
        // }
        if (argument.length != 1) {
            System.out.println("Running default setup");
            System.out.println("Running with server:" + serverIP + " port:" + serverPort + " Mode:" + algorithmMode);

        } else {
            // serverIP = argument[0];
            // serverPort = Integer.parseInt(argument[1]);
            algorithmMode = argument[0].toUpperCase();
            System.out.println("Running with server:" + serverIP + " port:" + serverPort + " Mode:" + algorithmMode);
        }
    }

    /*
     * Handle Tcp socket connection and error throw out
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
     * ReadLine is a blocking function, I add timeout feature and modify it to
     * non-blocking
     */
    public String cReadLine(long timeOut, BufferedReader reader) throws Exception {
        // long l1 = System.currentTimeMillis();
        // String s = null;
        //
        // while (reader.ready() == false) {
        // //Thread.sleep(1);
        // long l2 = System.currentTimeMillis();
        // if (l2 - l1 > timeOut) {
        // System.out.println("System timeout " + timeOut + " milliseconds, no packet
        // received");
        // return null;
        // }
        // }
        return reader.readLine();
    }

    public void cSendLine(String sendString) throws Exception {
        sendString += "\n";
        writer.write(sendString);
        writer.flush();
    }

    /*
     * ds-sim generate ds-system.xml after auth
     * read the xml file, store server info into arraylist
     */
    public void processDsSystemXML() {
        /*
         * DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
         * try {
         * DocumentBuilder builder = factory.newDocumentBuilder();
         * Document doc = builder.parse(dsSystemXML);
         * NodeList nl = doc.getElementsByTagName("server");
         * for (int i = 0; i < nl.getLength(); i++) {
         * String[] attribute = new String[7];
         * Node node = nl.item(i);
         * NamedNodeMap nnm = node.getAttributes();
         * for (int j = 0; j < nnm.getLength(); j++) {
         * switch (nnm.item(j).getNodeName()) {
         * case "type":
         * attribute[0] = nnm.item(j).getNodeValue();
         * break;
         * case "limit":
         * attribute[1] = nnm.item(j).getNodeValue();
         * break;
         * case "bootupTime":
         * attribute[2] = nnm.item(j).getNodeValue();
         * break;
         * case "hourlyRate":
         * attribute[3] = nnm.item(j).getNodeValue();
         * break;
         * case "cores":
         * attribute[4] = nnm.item(j).getNodeValue();
         * break;
         * case "memory":
         * attribute[5] = nnm.item(j).getNodeValue();
         * break;
         * case "disk":
         * attribute[6] = nnm.item(j).getNodeValue();
         * break;
         * }
         * }
         * var serverType = new ServerType(attribute);
         * serversList.add(serverType);
         * }
         * Collections.sort(serversList,
         * Comparator.comparing(ServerType::getCores).reversed());
         * //serversList.forEach(System.out::println);
         * } catch (Exception e) {
         * e.printStackTrace();
         * }
         */

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(dsSystemXML);

            NodeList nl = doc.getElementsByTagName("server");
            for (int i = 0; i < nl.getLength(); i++) {
                String[] attribute = new String[7];
                Element server = (Element) nl.item(i);
                attribute[0] = server.getAttribute("type");
                attribute[1] = server.getAttribute("limit");
                attribute[2] = server.getAttribute("bootupTime");
                attribute[3] = server.getAttribute("hourlyRate");
                attribute[4] = server.getAttribute("cores");
                attribute[5] = server.getAttribute("memory");
                attribute[6] = server.getAttribute("disk");
                var serverType = new ServerType(attribute);
                serversList.add(serverType);
            }
            Collections.sort(serversList, Comparator.comparing(ServerType::getCores).reversed());
            // serversList.forEach(System.out::println);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ################################################################################################
    public void algorithmLRR(JobSubmission jobSubmission, List<ServerStatus> lServer) throws Exception {
        if (maxServerType_LRR == null) {
            // processDsSystemXML();
            maxServerType_LRR = serversList.get(0).type;
        }
        // Collections.sort(lServer,
        // Comparator.comparing(ServerStatus::getCores).reversed());
        // System.out.println("====Sort LRR====");
        // lServer.forEach(System.out::println);
        List<ServerStatus> result = null;
        result = lServer.stream().filter((ServerStatus a) -> a.type.equals(maxServerType_LRR))
                .collect(Collectors.toList());
        System.out.println("====Sort LRR====");
        result.forEach(System.out::println);

        if (serverIndex_LRR >= lServer.size()) {
            serverIndex_LRR = 0;
        }
        // SCHD 0 t2.aws 0
        String sendString = Commands.SCHD.getDescription() + " " + jobSubmission.jobID + " " +
                maxServerType_LRR + " " + serverIndex_LRR;
        // result.get(serverIndex_LRR).type + " "+result.get(serverIndex_LRR).serverID;
        // System.out.println(sendString);
        cSendLine(sendString);

        if (++serverIndex_LRR >= result.size()) {
            serverIndex_LRR = 0;
        }
    }

    public void algorithmFC(JobSubmission jobSubmission, List<ServerStatus> lServer) throws Exception {
        System.out.println("====Sort FC====");
        System.out.println(lServer.get(0));
        cSendLine(Commands.SCHD.getDescription() + " " + jobSubmission.jobID + " " +
                lServer.get(0).type + " " + lServer.get(0).serverID);
    }

    public void algorithmATL(JobSubmission jobSubmission, List<ServerStatus> lServer) throws Exception {
        Collections.sort(lServer, Comparator.comparing(ServerStatus::getCores).reversed());
        System.out.println("====Sort ATL====");
        System.out.println(lServer.get(0));
        cSendLine(Commands.SCHD.getDescription() + " " + jobSubmission.jobID + " " +
                lServer.get(0).type + " 0");
    }

    public void algorithmBF(JobSubmission jobSubmission, List<ServerStatus> lServer) throws Exception {
        ServerStatus bestServer = lServer.get(0);

        System.out.println("====Sort before-BF====");
        lServer.forEach(System.out::println);

        lServer.removeIf((server) -> {
            return (server.cores < jobSubmission.cores ||
                    server.disk < jobSubmission.disk || server.memory < jobSubmission.memory);
        });
        System.out.println("====Sort BF-resources====");
        lServer.forEach(System.out::println);
        // ###############################
        if (lServer.size() == 0) {
            // throw new RuntimeException("[Job ID " + jobSubmission.jobID + "] Scheduling
            // failed!");
            System.out.println("None");
        } else {
            Collections.sort(lServer, Comparator.comparing(ServerStatus::getCores));
            bestServer = lServer.get(0);
            System.out.println("====Sort BF-cores====");
            lServer.forEach(System.out::println);
        }

        cSendLine(Commands.SCHD.getDescription() + " " + jobSubmission.jobID + " " +
                bestServer.type + " " + bestServer.serverID);
        System.out.println(Commands.SCHD.getDescription() + " " + jobSubmission.jobID + " " +
                bestServer.type + " " + bestServer.serverID);
    }

    public void algorithmWF(JobSubmission jobSubmission, List<ServerStatus> lServer) throws Exception {
        ServerStatus bestServer = lServer.get(0);

        System.out.println("====Sort before-WF====");
        lServer.forEach(System.out::println);

        lServer.removeIf((server) -> {
            return (server.cores < jobSubmission.cores ||
                    server.disk < jobSubmission.disk || server.memory < jobSubmission.memory);
        });
        System.out.println("====Sort WF-resources====");
        lServer.forEach(System.out::println);
        // ###############################
        if (lServer.size() == 0) {
            // throw new RuntimeException("[Job ID " + jobSubmission.jobID + "] Scheduling
            // failed!");
            System.out.println("None");
            bestServer.type = serversList.get(0).type;
            bestServer.serverID = 0;
        } else {
            Collections.sort(lServer, Comparator.comparing(ServerStatus::getCores));
            int maxCores = lServer.get(lServer.size() - 1).getCores();
            for (int i = 0; i < lServer.size(); ++i) {
                ServerStatus temp = lServer.get(i);
                if (temp.getCores() == maxCores) {
                    bestServer = temp;
                    break;
                }
            }
            System.out.println("====Sort WF-cores====");
            lServer.forEach(System.out::println);
        }

        cSendLine(Commands.SCHD.getDescription() + " " + jobSubmission.jobID + " " +
                bestServer.type + " " + bestServer.serverID);
        System.out.println(Commands.SCHD.getDescription() + " " + jobSubmission.jobID + " " +
                bestServer.type + " " + bestServer.serverID);
    }

    public void algorithmFF(JobSubmission jobSubmission, List<ServerStatus> lServer) throws Exception {
        ServerStatus bestServer = lServer.get(0);

        System.out.println("====Sort before-FF====");
        lServer.forEach(System.out::println);

        lServer.removeIf((server) -> {
            return (server.cores < jobSubmission.cores ||
                    server.disk < jobSubmission.disk || server.memory < jobSubmission.memory);
        });
        // ###############################
        if (lServer.size() == 0) {
            // throw new RuntimeException("[Job ID " + jobSubmission.jobID + "] Scheduling
            // failed!");
            System.out.println("None");
        } else {
            bestServer = lServer.get(0);
        }
        cSendLine(Commands.SCHD.getDescription() + " " + jobSubmission.jobID + " " +
                bestServer.type + " " + bestServer.serverID);
        System.out.println(Commands.SCHD.getDescription() + " " + jobSubmission.jobID + " " +
                bestServer.type + " " + bestServer.serverID);
    }

    /*
     * jobSchedule algorithmMode
     * LRR or ATL
     */
    public void jobSchedule(JobSubmission jobSubmission, List<ServerStatus> lServer) throws Exception {
        switch (algorithmMode) {
            case "BF":
                algorithmBF(jobSubmission, lServer);
                break;
            case "WF":
                algorithmWF(jobSubmission, lServer);
                break;
            case "FF":
                algorithmFF(jobSubmission, lServer);
                break;
            case "FC":
                algorithmFC(jobSubmission, lServer);
                break;
            case "LRR":
                algorithmLRR(jobSubmission, lServer);
                break;
            case "ATL":
            default:
                algorithmATL(jobSubmission, lServer);
                break;
        }
    }
    // ################################################################################################

    /*
     * process JOBN command
     */
    public void processJob(DsClient dsClient, String resp) throws Exception {
        var jobSubmission = new JobSubmission(resp);
        cSendLine(jobSubmission.Gets()); // GETS Capable 1 700 600
        resp = reader.readLine(); // DATA 7 124
        String[] command = resp.split("\\s+");
        int msgCount = Integer.parseInt(command[1]);
        cSendLine(Commands.OK.getDescription()); // OK

        // t1.micro 0 inactive -1 2 4000 16000 0 0
        // t1.micro 1 inactive -1 2 4000 16000 0 0
        // t1.small 0 inactive -1 2 8000 32000 0 0
        // t1.small 1 inactive -1 2 8000 32000 0 0
        // t1.medium 0 inactive -1 4 16000 64000 0 0
        // t1.medium 1 inactive -1 4 16000 64000 0 0
        // t2.aws 0 inactive -1 16 64000 512000 0 0
        List<ServerStatus> lServer = new ArrayList<ServerStatus>();
        for (int i = 0; i < msgCount; ++i) {
            resp = reader.readLine();
            var serverStatus = new ServerStatus(resp);
            lServer.add(serverStatus);
        }
        // System.out.println("====receive server status====");
        // lServer.forEach(System.out::println);

        cSendLine(Commands.OK.getDescription()); // OK

        resp = reader.readLine(); // .
        jobSchedule(jobSubmission, lServer); // SCHD 0 t2.aws 0
        resp = reader.readLine(); // OK
    }

    /*
     * Main loop
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
                        cSendLine(Commands.AUTH.getDescription() + " " + System.getProperty("user.name"));
                        state = Statement.AUTHENTICATED;
                    } else {
                        state = Statement.QUIT;
                        readline_Flag = false;
                    }
                    break;
                case AUTHENTICATED:
                    if (resp.equalsIgnoreCase(Commands.OK.getDescription())) {
                        processDsSystemXML();
                        state = Statement.READY;
                    } else {
                        state = Statement.QUIT;
                    }
                    readline_Flag = false;
                    break;

                case READY:
                    cSendLine(Commands.REDY.getDescription());
                    state = Statement.READYNEXT;
                    break;

                case READYNEXT:
                    String[] command = resp.split("\\s+");
                    switch (command[0]) {
                        case "JOBN":
                        case "JOBP":
                            processJob(this, resp);
                        case "JCPL":
                            state = Statement.READY;
                            readline_Flag = false;
                            break;

                        case "RESF":
                        case "RESR":
                        case "NONE":
                            state = Statement.QUIT;
                            readline_Flag = false;
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
            if (readline_Flag) {
                resp = cReadLine(packetTimeout, reader);
                // System.out.println("RX>>> " + resp);
            } else {
                readline_Flag = true;
            }
        }
    }
}
