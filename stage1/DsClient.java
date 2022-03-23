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

    // the state in the main loop
    enum Statement {
        INIT,
        AUTHENTICATION,
        AUTHENTICATED,
        READY,
        READYNEXT,
        JOBN,
        JOBP,
        JCPL,
        RESF,
        RESR,
        NONE,
        BREAK,
        QUIT,
        ACKQUIT,
    }

    ;

    Statement state = Statement.INIT;
    String serverIP = "10.0.0.137";    // localhost 10.0.0.137
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
     *  process argument get serverIP and serverPort
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
            serversList.forEach(System.out::println);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void jobSchedule(JobSubmission jobSubmission) throws Exception {
        if (algorithmMode.equalsIgnoreCase("LRR")) {
            cSendLine(Commands.SCHD.getDescription() + " " + jobSubmission.jobID + " " +
                    serversList.get(0).type + " "+serverIndex_LRR);
            if(++serverIndex_LRR >= serversList.get(0).limit){
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
        cSendLine(jobSubmission.Gets());
        resp=reader.readLine();
        String[] command= resp.split(" ");
        int msgCount = Integer.parseInt(command[1]);
        cSendLine(Commands.OK.getDescription());

        for(int i=0 ; i<msgCount; ++i) {
            resp = reader.readLine();
            var serverStatus = new ServerStatus(resp);
            //System.out.println(serverStatus);
        }
        cSendLine(Commands.OK.getDescription());

        resp=reader.readLine();
        jobSchedule(jobSubmission);
        resp=reader.readLine();
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
                        cSendLine(Commands.AUTH.getDescription() + System.getProperty("user.name"));
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
                    String[] command= resp.split(" ");
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
                System.out.println("RX>>> " + resp);
            } else {
                readline_Flag = true;
            }
        }
    }
}

class ServerType {
    String type;
    int limit;
    int bootUpTime;
    double hourlyRate;
    int cores;
    int memory;
    int disk;

    public ServerType() {
    }

    public ServerType(String type, int limit, int bootUpTime, double hourlyRate, int cores, int memory, int disk) {
        this.type = type;
        this.limit = limit;
        this.bootUpTime = bootUpTime;
        this.hourlyRate = hourlyRate;
        this.cores = cores;
        this.memory = memory;
        this.disk = disk;
    }

    public ServerType(String[] array) {
        this.type = array[0];
        this.limit = Integer.parseInt(array[1]);
        this.bootUpTime = Integer.parseInt(array[2]);
        this.hourlyRate = Double.parseDouble(array[3]);
        this.cores = Integer.parseInt(array[4]);
        this.memory = Integer.parseInt(array[5]);
        this.disk = Integer.parseInt(array[6]);
    }

    public int getCores() {
        return cores;
    }

    public String toString() {
        return "type:" + type + ",limit:" + limit + ",bootUpTime:" + bootUpTime + ",hourlyRate:" + hourlyRate +
                ",cores:" + cores + ",memory:" + memory + ",disk:" + disk;
    }
}

class ServerStatus {
    String type;
    int serverID;
    String state;
    int curStartTime;
    int cores;
    int memory;
    int disk;
    int wJobs;
    int rJobs;

    public ServerStatus() {
    }

    public ServerStatus(String type, int serverID, String state, int curStartTime, int cores, int memory, int disk,
                        int wJobs, int rJobs) {
        this.type = type;
        this.serverID = serverID;
        this.state = state;
        this.curStartTime = curStartTime;
        this.cores = cores;
        this.memory = memory;
        this.disk = disk;
        this.wJobs = wJobs;
        this.rJobs = rJobs;
    }

    public ServerStatus(String[] array) {
        this.type = array[0];
        this.serverID = Integer.parseInt(array[1]);
        this.state = array[2];
        this.curStartTime = Integer.parseInt(array[3]);
        this.cores = Integer.parseInt(array[4]);
        this.memory = Integer.parseInt(array[5]);
        this.disk = Integer.parseInt(array[6]);
        this.wJobs = Integer.parseInt(array[7]);
        this.rJobs = Integer.parseInt(array[8]);
    }
    public ServerStatus(String msg) {
        String[] array= msg.split(" ");
        this.type = array[0];
        this.serverID = Integer.parseInt(array[1]);
        this.state = array[2];
        this.curStartTime = Integer.parseInt(array[3]);
        this.cores = Integer.parseInt(array[4]);
        this.memory = Integer.parseInt(array[5]);
        this.disk = Integer.parseInt(array[6]);
        this.wJobs = Integer.parseInt(array[7]);
        this.rJobs = Integer.parseInt(array[8]);
    }

    public String toString() {
        return "type:" + type + ",serverID:" + serverID + ",state:" + state + ",curStartTime:" + curStartTime +
                ",cores:" + cores + ",memory:" + memory + ",disk:" + disk + ",wJobs:" + wJobs + ",rJobs:" + rJobs;
    }
}

class JobSubmission {
    String type;
    int submitTime;
    int jobID;
    int estRuntime;
    int cores;
    int memory;
    int disk;

    public JobSubmission() {
    }

    public JobSubmission(String type, int submitTime, int jobID, int estRuntime, int cores, int memory, int disk) {
        this.type = type;
        this.submitTime = submitTime;
        this.jobID = jobID;
        this.estRuntime = estRuntime;
        this.cores = cores;
        this.memory = memory;
        this.disk = disk;
    }

    public JobSubmission(String[] array) {
        this.type = array[0];
        this.submitTime = Integer.parseInt(array[1]);
        this.jobID = Integer.parseInt(array[2]);
        this.estRuntime = Integer.parseInt(array[3]);
        this.cores = Integer.parseInt(array[4]);
        this.memory = Integer.parseInt(array[5]);
        this.disk = Integer.parseInt(array[6]);
    }
    public JobSubmission(String msg) {
        String[] array= msg.split(" ");
        this.type = array[0];
        this.submitTime = Integer.parseInt(array[1]);
        this.jobID = Integer.parseInt(array[2]);
        this.estRuntime = Integer.parseInt(array[3]);
        this.cores = Integer.parseInt(array[4]);
        this.memory = Integer.parseInt(array[5]);
        this.disk = Integer.parseInt(array[6]);
    }

    //Server information request
    public String Gets(){
        return "GETS Capable "+ cores + " " + memory + " " + disk;
    }

    public String toString() {
        return "type:" + type + ",submitTime:" + submitTime + ",jobID:" + jobID + ",estRuntime:" + estRuntime +
                ",cores:" + cores + ",memory:" + memory + ",disk:" + disk;
    }
}