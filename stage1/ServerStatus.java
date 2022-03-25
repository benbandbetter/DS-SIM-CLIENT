public class ServerStatus {
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
        String[] array= msg.split("\\s+");
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
