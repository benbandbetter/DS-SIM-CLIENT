public enum Commands {
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
