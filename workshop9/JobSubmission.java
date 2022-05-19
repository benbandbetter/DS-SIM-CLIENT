
//        Receive a job submitted by the ds-server
//        JOBN 32 0 47066 1 700 600
//        After format with JobSubmission class.
//        type:JOBN,submitTime:32,jobID:0,estRuntime:47066,cores:1,memory:700,disk:600

public class JobSubmission {
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
        String[] array= msg.split("\\s+");
        this.type = array[0];
        this.submitTime = Integer.parseInt(array[1]);
        this.jobID = Integer.parseInt(array[2]);
        this.estRuntime = Integer.parseInt(array[3]);
        this.cores = Integer.parseInt(array[4]);
        this.memory = Integer.parseInt(array[5]);
        this.disk = Integer.parseInt(array[6]);

        System.out.println("JOB "+ toString());
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
