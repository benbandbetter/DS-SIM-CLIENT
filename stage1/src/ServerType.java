
// Format of ds-system.xml
//output:
//        type:t2.aws,limit:1,bootUpTime:60,hourlyRate:0.8,cores:16,memory:64000,disk:512000
//        type:t1.medium,limit:2,bootUpTime:40,hourlyRate:0.4,cores:4,memory:16000,disk:64000
//        type:t1.micro,limit:2,bootUpTime:30,hourlyRate:0.2,cores:2,memory:4000,disk:16000
//        type:t1.small,limit:2,bootUpTime:40,hourlyRate:0.4,cores:2,memory:8000,disk:32000

public class ServerType {
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
