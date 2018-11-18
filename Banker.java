/**
 * Ciara McMullin
 * Operating Systems Fall 2018
 * Lab 3: Banker's
 * The goal of this lab is to do resource allocation using both an optimistic resource manager and the banker’s
 * algorithm of Dijkstra.
 */

import java.io.*;
import java.util.*;

public class Banker {

    static int numOfTasks; // number of tasks
    static int numOfResources; // number of resources
    static int[] resourceList1; // list of resources for optimistic algorithm
    static int[] resourceList2; // list of resources for bankers
    static Task[] tasksList1; // list of tasks for optimistic algorithm
    static Task[] tasksList2; // list of tasks for bankers


    public static void main(String[] args) throws FileNotFoundException{

        try {
            // read the file in
            File file = new File(args[0]);
            Scanner input = new Scanner(file);

            numOfTasks = Integer.parseInt(input.next());
            numOfResources = Integer.parseInt(input.next());

            // set the arbitrary limits for N and T to be the max value of integer (2^31 -1)
            // if either or both exceed we abort the program
            if (numOfTasks > Integer.MAX_VALUE) {
                System.out.println("ERROR. Number of Tasks exceeds limits");
                System.exit(0);
            }

            if (numOfResources > Integer.MAX_VALUE) {
                System.out.println("ERROR. Number of Resources exceeds limits");
                System.exit(0);
            }

            // two tasks lists and resource lists so the data is not affected for each algorithm
            tasksList1 = new Task[numOfTasks];
            tasksList2 = new Task[numOfTasks];
            resourceList1 = new int[numOfResources];
            resourceList2 = new int[numOfResources];

            int id = 1; // task id
            // populate the tasks list with new tasks
            for (int i = 0; i < numOfTasks; i++) {
                Task t1 = new Task(id, numOfResources);
                Task t2 = new Task(id, numOfResources);
                tasksList1[i] = t1;
                tasksList2[i] = t2;
                id++;
            }

            // populate the resource list with default values of 0
            for (int j = 0; j < numOfResources; j++) {
                resourceList1[j] = 0;
                resourceList2[j] = 0;
            }

            int inc = 0;
            // while there are units available continue populate the resources
            while (input.hasNextInt()) {
                int units = Integer.parseInt(input.next());
                resourceList1[inc] = units;
                resourceList2[inc] = units;
                inc += 1;
            }

            // for loop used to make sure we populate entire array
            // EX: 2 2 4 --> we have two resources both of 4 units so we want to set both to 4 even
            // though there is only one 4
            for (int j = 0; j < numOfResources; j++) {
                if (resourceList1[j] == 0) {
                    if (j != 0) {
                        resourceList1[j] = resourceList1[j - 1];
                        resourceList2[j] = resourceList1[j - 1];
                    }
                }
            }

            // set task id back to 1
            id = 1;
            // read the activities
            while (input.hasNext()) {
                String instruction = input.next();
                int taskNum = Integer.parseInt(input.next());
                int resource = Integer.parseInt(input.next());
                int claim = Integer.parseInt(input.next());
                // create new activities with the input data
                Activity act1 = new Activity(instruction, taskNum, resource, claim, id);
                Activity act2 = new Activity(instruction, taskNum, resource, claim, id);
                // if instruction is initiate we can set initial claims
                if (instruction.equals("initiate")) {
                    tasksList1[taskNum - 1].claim.set(act1.resourceNum - 1, act1.claim);
                    tasksList2[taskNum - 1].claim.set(act2.resourceNum - 1, act2.claim);
                }
                // add the activities to the task in task list
                tasksList1[taskNum - 1].activities.add(act1);
                tasksList2[taskNum - 1].activities.add(act2);
                id++;
            }

            // call the optimistic resource manager and banker's algorithm of Dijkstra to do resource allocation!!
            ArrayList<Task> fifoFinishedTasks = optimisticResourceManager(tasksList1, resourceList2);
            ArrayList<Task> bankersFinishedTasks = bankersAlgorithm(tasksList2, resourceList2);

            // Print the ouput of the new finished, sorted lists!!!!
            System.out.println();
            System.out.println("\t\tFIFO'S\t\t\t\t\t\tBANKER'S");

            int totalTimeFIFO = 0;
            int totalWaitFIFO = 0;
            int totalTimeBankers = 0;
            int totalWaitBankers = 0;

            for (int j = 0; j < numOfTasks; j++) {
                Task task1 = fifoFinishedTasks.get(j);
                Task task2 = bankersFinishedTasks.get(j);
                if (task1.aborted) {
                    System.out.print("Task " + task1.taskID + "\t\taborted\t\t");
                } else {
                    totalTimeFIFO += task1.totalTime;
                    totalWaitFIFO += task1.waitTime;
                    float waitingPercent = 100 * ((float) task1.waitTime / (float) task1.totalTime);
                    System.out.format("Task " + task1.taskID + "\t\t%d\t%d\t%.0f%s", +task1.totalTime, task1.waitTime, waitingPercent, "%");
                }

                if (task2.aborted) {
                    System.out.println("\t\tTask " + task2.taskID + "\t\taborted");
                } else {
                    totalTimeBankers += task2.totalTime;
                    totalWaitBankers += task2.waitTime;
                    float waitingPercent = 100 * ((float) task2.waitTime / (float) task2.totalTime);
                    System.out.format("\t\tTask " + task2.taskID + "\t\t%d\t%d\t%.0f%s", +task2.totalTime, task2.waitTime, waitingPercent, "%");
                    System.out.println();
                }

            }

            float totalWaitingPercentFIFO = 100 * ((float) totalWaitFIFO / (float) totalTimeFIFO);
            System.out.format("Total \t\t%d\t%d\t%.0f%s", totalTimeFIFO, totalWaitFIFO, totalWaitingPercentFIFO, "%");

            float totalWaitingPercentBankers = 100 * ((float) totalWaitBankers / (float) totalTimeBankers);
            System.out.format("\t\tTotal \t\t%d\t%d\t%.0f%s", totalTimeBankers, totalWaitBankers, totalWaitingPercentBankers, "%");
            System.out.println();
        } catch(Exception e){
            System.out.println("ERROR PROGRAM CANNOT BE RUN");
            System.exit(0);
        }
    }

    /**
     * This function is the optimistic resource manager which does resource allocation that satisfies a request if possible and if not
     * makes the task wait. When a release occurs it tries to satisfy pending requests in a FIFO manner.
     * First, the algorithm initializes the running lists with all tasks and available list with all the resources. Next, it continues
     * to loop until there are no more tasks left in running, blocked, or freedTasks. In the while loop, first we check if we can unblock
     * any of the blocked/waiting tasks. If a request can be granted, we remove the task from the blocked list and add to the freedTasks.
     * After checking the blocked, the algorithm looks through the next task in the running list and it's first activity (activity.get(0))
     * It then checks for the type of instruction and manipulates the data based on the instruction. It then adds all freed tasks to running,
     * increments the wait time of tasks blocked, and adds the freed resources to the available resource list (resources that are free at cycle
     * n become available at cycle n+1). We then check for deadlock and continue to fix it until no deadlock remains.
     * The algorithms returns the new finished, sorted tasks.
     * @param tasksList
     * @param resourceList
     */

    private static ArrayList<Task> optimisticResourceManager(Task[] tasksList, int[] resourceList){

        int cycles = 0; // time is measured in cycles
        int i = 0;

        ArrayList<Task> running = new ArrayList<>(); // list of running tasks
        ArrayList<Task> blocked = new ArrayList<>(); // list of blocked tasks
        ArrayList<Task> finished = new ArrayList<>(); // list of finished tasks
        ArrayList<Task> freedTasks = new ArrayList<>(); // list of tasks that have become unblocked and can run next cycle
        ArrayList<Task> released = new ArrayList<>(); // list of tasks that terminate after release

        ArrayList<Integer> available = new ArrayList<>(); // list of available resources
        ArrayList<Integer> freed = new ArrayList<>(); // freed list holds the resources freed at cycle n and makes them available at cycle n + 1

        // initialize running to hold all tasks
        for(Task t: tasksList){
            running.add(t);
        }
        // initialize available to hold all resources and freed to be all set to 0
        for(int r: resourceList){
            available.add(r);
            freed.add(0);
        }

        // while loop that continues until all tasks are put in finished
        while(running.size() + blocked.size() + freedTasks.size() != 0) {

          //  System.out.println("During cycles " + cycles + "-" + (cycles+1));

            for(Task t: released){
                running.remove(t); // we remove the tasks from running that have been released and terminated
            }

            // first we check blocked tasks
            for(int j = 0; j < blocked.size(); j++) {

                Task task = blocked.get(0); // get first tasks
                Activity activity = task.activities.get(0); // get activity of task
                int request = available.get(activity.resourceNum - 1);

                if (request < activity.claim) {
                //    System.out.println("cannot satisfy request. Task " + task.taskID + "remains blocked");
                    blocked.remove(0); // remove task from blocked
                    blocked.add(task); // add task to back of blocked list

                } else {
                    // if we satisfy request, we remove task from blocked list and grant it's request. We add task to freedTasks for next cycle
                    //  System.out.println("granting request for " + task.taskID);
                    task.resourceList.set(activity.resourceNum - 1, task.resourceList.get(activity.resourceNum - 1) + activity.claim);
                    available.set(activity.resourceNum - 1, available.get(activity.resourceNum - 1) - activity.claim);
                    task.activities.remove(0);
                    blocked.remove(0);
                    freedTasks.add(task);
                    j--;
                }
            }

            // while loop that continues to run while running is greater than increment i
            while (i < running.size()) {

                    // get the next task in running list and it's first activity and instruction of that activity
                    Task task = running.get(i);
                    Activity activity = task.activities.get(0);
                    String instruction = activity.instruction;

                    // check if task is computing
                    if (task.computing > 0) {
                        // System.out.println("Task " + task.taskID + " delayed " + task.computing);
                        task.computing -= 1;
                        i++;

                    } else {

                        if (instruction.equals("initiate")) {
                          //  System.out.println("Task " + task.taskID + " is intitiated");
                            task.status = "initiated";
                            task.activities.remove(0); // we are done with this activity
                        } else if (instruction.equals("request")) {
                           // System.out.println("Task " + task.taskID + " is requesting");
                            int request = available.get(activity.resourceNum - 1);

                            if (request < activity.claim) {
                              //  System.out.println("cannot satisfy request. Task blocked");
                                blocked.add(task);
                                running.remove(task); // remove and add to finished list
                                i--;

                            } else {
                              //  System.out.println("granting request");
                                task.resourceList.set(activity.resourceNum - 1, task.resourceList.get(activity.resourceNum - 1) + activity.claim);
                                available.set(activity.resourceNum - 1, available.get(activity.resourceNum - 1) - activity.claim);
                                task.activities.remove(activity);
                            }
                        } else if (instruction.equals("release")) {
                            // System.out.println("Task " + task.taskID + " releases " + (freed.get(activity.resourceNum - 1)));
                            freed.set(activity.resourceNum - 1, freed.get(activity.resourceNum - 1) + activity.claim);
                            task.resourceList.set(activity.resourceNum - 1, task.resourceList.get(activity.resourceNum - 1) - activity.claim);
                            task.activities.remove(0);
                            // since terminate does not require a cycle, we can check here if the task is now finished after releasing its units
                            if(task.activities.get(0).instruction.equals("terminate")){
                               // System.out.println("Task " + task.taskID + " terminates at " + (cycles + 1));
                                task.totalTime = cycles + 1;

                                for (int j = 0; j < task.resourceList.size(); j++) {
                                    freed.set(j, freed.get(j) + task.resourceList.get(j));
                                    task.resourceList.set(j, 0);
                                }

                                task.activities.remove(0);
                                released.add(task);
                                finished.add(task);

                            }

                        } else if (instruction.equals("compute")) {
                            //  System.out.println("Task " + task.taskID + " delayed " + task.computing);
                            task.computing = activity.resourceNum - 1;
                            task.activities.remove(activity);
                        } else if (instruction.equals("terminate")) {
                          //  System.out.println("Task " + task.taskID + " terminates at " + cycles);
                            task.totalTime = cycles;

                            for (int j = 0; j < task.resourceList.size(); j++) {
                                freed.set(j, freed.get(j) + task.resourceList.get(j));
                                task.resourceList.set(j, 0);
                            }

                            task.activities.remove(0);
                            finished.add(task);
                            running.remove(0);
                            i--;

                        }

                        i++;
                    }

            }
            // increment time and reset i to 0
            cycles += 1;
            i = 0;

            // update all freed tasks, freed resources, and blocked tasks
            if(!freedTasks.isEmpty()) {
                for (int k = 0; k < freedTasks.size(); k++) {
                    running.add(freedTasks.get(k));
                    freedTasks.remove(k);
                    k--;
                }
            }
            for(Task t: blocked){
                t.waitTime += 1;
            }

            for(int j = 0; j < freed.size(); j++){
                available.set(j, available.get(j) + freed.get(j));
                freed.set(j, 0);
            }

            // check for deadlock
            boolean deadlockDetection = (finished.size() != numOfTasks) && (running.size() == 0);

            // if deadlock is found call deadlock function that fixes it
            if(deadlockDetection){
                deadlock(running, available ,blocked, finished);
            }

        }

        // sort the finished list by its task ID for printing
        Collections.sort(finished, new Task.CompID());

        // return finished, sorted tasks
        return finished;
    }

    /**
     * This function is the Bankers algorithm that does resource allocation using a deadlock avoidance algorithm
     * First, the algorithm initializes the running lists with all tasks and available list with all the resources. Next, it continues
     * to loop until there are no more tasks left in running, blocked, or freedTasks. In the while loop, first we add all freed tasks to running,
     * increment the wait time of tasks blocked, and adds the freed resources to the available resource list (resources that are free at cycle
     * n become available at cycle n+1) Next, we check the blocked tasks and see if any of the blocked tasks requests can be granted aka they are safe
     * If a request can be granted, we remove the task from the blocked list and add to the freedTasks.
     * After checking the blocked, the algorithm looks through the next task in the running list and it's first activity (activity.get(0))
     * It then checks for the type of instruction and manipulates the data based on the instruction.
     * The algorithm returns the new finished, sorted tasks.
     * @param tasksList
     * @param resourceList
     * @return
     */
    private static ArrayList<Task> bankersAlgorithm(Task[] tasksList, int[] resourceList){

        int cycles = 0; // time in cycles
        int i = 0; // i for incrementing

        ArrayList<Task> running = new ArrayList<>(); // running tasks
        ArrayList<Task> blocked = new ArrayList<>(); // blocked/waiting tasks
        ArrayList<Task> finished = new ArrayList<>(); // finished tasks
        ArrayList<Task> released = new ArrayList<>(); // released tasks that become available at the next cycle (n + 1)
        ArrayList<Task> freedTasks = new ArrayList<>(); // freed / unblocked tasks that become available at the next cycle (n + 1)

        ArrayList<Integer> available = new ArrayList<>(); // available resources
        ArrayList<Integer> freed = new ArrayList<>(); // freed list holds the resources freed at cycle n and makes them available at cycle n + 1

        // initialize running and available lists
        for(Task t: tasksList){
            running.add(t);
        }

        for(int r: resourceList){
            available.add(r);
            freed.add(0);
        }

        // check if any of the tasks initial claims exceed the number of units present; if so, abort immediately
        for(int j = 0; j < running.size(); j++){
            for(int k = 0; k < running.get(j).claim.size(); k++){
                if(running.get(j).claim.get(k) > available.get(k)){
                    System.out.println("Banker aborts task " + running.get(j).taskID + " before run begins:");
                    System.out.println("\tclaim for resource " + (k + 1) + " (" + running.get(j).claim.get(k) + ") exceeds the number of units present" + " (" + available.get(k) + ")");
                    running.get(j).aborted = true;
                    finished.add(running.get(j));
                    running.remove(running.get(j));
                    j--;

                }
            }
        }

        // while loop that continues as long as tasks remain in running, blocked, and/or freed lists
        while(running.size() + blocked.size() + freedTasks.size() != 0) {

            // add all freed resources from cycle n to the lists so they are available at cycle n + 1
            if(!freedTasks.isEmpty()) {
                for (int k = 0; k < freedTasks.size(); k++) {
                    running.add(freedTasks.get(k));
                    freedTasks.remove(k);
                    k--;
                }
            }

            for(int j = 0; j < freed.size(); j++){
                available.set(j, available.get(j) + freed.get(j));
                freed.set(j, 0);
            }

            for(Task t: released){
                running.remove(t);
            }

            // increment the wait time for blocked tasks remaining
            for(Task t: blocked){
                t.waitTime += 1;
            }

            //     System.out.println("During cycles " + cycles + "-" + (cycles+1));

            // first we check if we can grant the requests of the blocked tasks; we do this by checking if the request is now safe with the current resources available
            for (int k = 0; k < blocked.size(); k++){
                // get first task in blocked and its first activity
                Task task = blocked.get(k);
                Activity activity = task.activities.get(0);
                running.add(0, task); // add to the front of the running list so we can call safety algorithm

                boolean safe = isSafe(running, available); // boolean value that tells us if it is a safe request from the blocked task

                running.remove(0);

                    if (!safe) {
                        // leave task in blocked list
                        //  System.out.println("Task " + task.taskID + " cannot grant request (not safe). Task " + task.taskID + " is blocked");

                    } else {
                        // unblock task, update the resource lists, and add task to freed tasks list
                        //  System.out.println("Task " + task.taskID + " completes its request (i.e the request is granted)");
                        task.resourceList.set(activity.resourceNum - 1, task.resourceList.get(activity.resourceNum - 1) + activity.claim);
                        available.set(activity.resourceNum - 1, available.get(activity.resourceNum - 1) - activity.claim);
                        task.activities.remove(0);
                        freedTasks.add(task);
                        blocked.remove(0);
                        k--; // we want to now look at the new first task in the blocked list
                    }

            }

            while (i < running.size()){
                // get first task in running list and its first activity and that activity instruction
                Task task = running.get(i);
                Activity activity = task.activities.get(0); // we want the next activity
                String instruction = activity.instruction;


                if(task.computing > 0){
                   // System.out.println("Task " + task.taskID + " delayed " + task.computing);
                    task.computing -= 1;
                    i++;
                }
                else {

                    if (instruction.equals("initiate")) {
                        // if the activity of a task exceeds total in system we abort and this task terminates
                        if (activity.claim > resourceList[activity.resourceNum - 1]) {
                           System.out.println("During cycle " + cycles + "-" + (cycles + 1) + " of Banker's algorithms");
                           System.out.println("\tTask " + task.taskID + " request exceeds its claim; aborted;");

                            task.status = "aborted";
                            running.remove(task); // remove and add to finished list
                            finished.add(task);
                            task.aborted = true;
                            i--;
                        } else {
                          //  System.out.println("Task " + task.taskID + " is intitiated");
                            task.status = "initiated";
                            task.activities.remove(activity); // we are done with this activity
                        }
                    } else if (instruction.equals("request")) {

                        // make sure available is updated
                        for(int j = 0; j < freed.size(); j++){
                            available.set(j, available.get(j) + freed.get(j));
                            freed.set(j, 0);
                        }
                        // the task's request that we need in order to check if it is a safe request
                        int request = task.resourceList.get(activity.resourceNum - 1) + activity.claim;
                        // check if task should be aborted (request exceeds the tasks claim)
                        if (request > task.claim.get(activity.resourceNum - 1)) {
                            System.out.println("During cycle " + cycles + "-" + (cycles + 1) + " of Banker's algorithms");
                            System.out.println("\tTask " + task.taskID + " request exceeds its claim; aborted;");
                            task.status = "aborted";
                            task.aborted = true;
                            running.remove(task); // remove and add to finished list
                            finished.add(task);
                            i--;

                            for (int j = 0; j < resourceList.length; j++) {
                                freed.set(j, freed.get(j) + task.resourceList.get(j));
                                task.resourceList.set(j, 0);
                            }

                        } else {
                            // call the helper safety algorithm to check if request can be granted!!
                            boolean safe = isSafe(running, available);
                            if (!safe) {
                                // not safe so we add to blocked list so task can wait to grant it's request when it is safe
                                //  System.out.println("Task " + task.taskID + " cannot grant request (not safe). Task " + task.taskID + " is blocked");
                                running.remove(task);
                                blocked.add(task);
                                i--;

                            } else {
                                // if safe, grant the request and remove the activity and update resources
                                //  System.out.println("Task " + task.taskID + " completes its request (i.e the request is granted)");
                                task.resourceList.set(activity.resourceNum - 1, task.resourceList.get(activity.resourceNum - 1) + activity.claim);
                                available.set(activity.resourceNum - 1, available.get(activity.resourceNum - 1) - activity.claim);
                                task.activities.remove(activity);
                            }
                        }
                    }

                    else if(instruction.equals("release")){
                        // release the resources from the current task; check if task can then terminate (terminate operation does NOT require a cycle)
                        //  System.out.println("Task " + task.taskID + " releases " + freed.get(activity.resourceNum - 1) + activity.claim);
                        freed.set(activity.resourceNum - 1, freed.get(activity.resourceNum - 1) + activity.claim);
                        task.resourceList.set(activity.resourceNum - 1, task.resourceList.get(activity.resourceNum - 1) - activity.claim);
                        task.activities.remove(activity);

                        // add finished task to finished and update task time and free it's resources
                        if(task.activities.get(0).instruction.equals("terminate")){
                            // System.out.println("Task " + task.taskID + " terminates at " + (cycles + 1));
                            task.totalTime = cycles + 1;

                            for (int j = 0; j < task.resourceList.size(); j++) {
                                freed.set(j, freed.get(j) + task.resourceList.get(j));
                                task.resourceList.set(j, 0);
                            }

                            task.activities.remove(0);
                            released.add(task);
                            finished.add(task);
                        }
                    }

                    else if(instruction.equals("compute")){
                        // uppdate the tasks computation
                        // System.out.println("Task " + task.taskID + " delayed " + task.computing);
                        task.computing = activity.resourceNum - 1;
                        task.activities.remove(activity);
                    }
                    else if(instruction.equals("terminate")){
                        // add finished task to finished and update task time and free it's resources
                        // System.out.println("Task " + task.taskID + " terminates at " + cycles);
                        task.totalTime = cycles;

                        for(int j = 0; j < task.resourceList.size(); j++){
                            freed.set(j, freed.get(j) + task.resourceList.get(j));
                            task.resourceList.set(j, 0);
                        }

                        task.activities.remove(activity);
                        finished.add(task);
                        running.remove(task);
                        i--;
                    }
                    i++;
                }

            }

            cycles += 1; // increment cycles
            i = 0; // set i back to 0

        }
        // sort the finished tasks
        Collections.sort(finished, new Task.CompID());

        // return finished, sorted tasks for output
        return finished;
    }

    /**
     * This is a helper function for the optimistic resource manager. It is used for deadlock detection in the algorithm. In the optimistic
     * resource manager if deadlock equals true, this function is called and finds the task with the lowest priority/ task ID and aborts the task.
     * It continues to abort the lowest task priority and release the aborted tasks resources until deadlock does not remain.
     * @param running
     * @param available
     * @param blocked
     * @param finished
     */
    private static void deadlock(ArrayList<Task> running, ArrayList<Integer> available, ArrayList<Task> blocked, ArrayList<Task> finished){

        boolean deadlockDetection = true;

        // get the task with the lowest ID number in blocked
        while(deadlockDetection){

            int lowestPriority = 0;

            for(int i = 0; i < blocked.size(); i++){
                if(blocked.get(i).taskID < blocked.get(lowestPriority).taskID){
                    lowestPriority = i;
                }
            }

            // free its resources
            for(int j = 0; j < blocked.get(lowestPriority).resourceList.size(); j++){
                available.set(j, available.get(j) + blocked.get(lowestPriority).resourceList.get(j));
                blocked.get(lowestPriority).resourceList.set(j, 0);
            }

            // abort task and print out message
            blocked.get(lowestPriority).aborted = true;
            System.out.println("Optimistic Resource Manager aborts Task " + blocked.get(lowestPriority).taskID + " because deadlock");
            finished.add(blocked.get(lowestPriority));
            blocked.remove(lowestPriority);

            // Get the new blocked list to be added to beginning of running so we can make sure no deadlock remains
            // and if it does we can correct it
            // A stack is used to add the tasks from blocked in order to running
            Stack<Task> stack = new Stack<>();

            for (int k = 0; k < blocked.size(); k++) {
                int ind = blocked.get(k).activities.get(0).resourceNum;
                if (ind > 0) {
                    ind -= 1;
                }
                if (available.get(ind) >= blocked.get(k).activities.get(0).claim) {
                    stack.push(blocked.remove(k));
                }
            }

            while (!stack.isEmpty()) {
                running.add(0, stack.pop());
            }

            // check if deadlock remains
            deadlockDetection = (finished.size() != numOfTasks) && (running.size() == 0);

        }
    }

    /**
     * This is a safety algorithm used for the Banker's algorithm. It is used to keep the system always in a safe state.
     * It checks the allocation of predetermined maximum possible amounts and then makes a safety state check to test for possible conditions that would cause
     * an unsafe state before deciding whether resource allocation can be granted.
     * @param available
     * @return
     */
    private static boolean isSafe(ArrayList<Task> running, ArrayList<Integer> available){

        // copies of running list and available list so the lists data is not affected/altered during this algorithm
        ArrayList<Task> runningCopy = new ArrayList<>();
        ArrayList<Integer> availableCopy = new ArrayList<>();

        // initialize copies of running and available
        for(Task t: running){
            runningCopy.add(new Task(t));
        }

        for(Integer a: available){
            availableCopy.add(a);
        }

        // check if request can we granted (aka safe state!)
        for(int i = 0; i < runningCopy.size(); i++) {
            for (int j = 0; j < runningCopy.get(i).resourceList.size(); j++) {
                int max = runningCopy.get(i).claim.get(j) - running.get(i).resourceList.get(j);
                // is max is greater than available (it will produce a negative number)
                if (availableCopy.get(j) - max < 0) {
                    return false; // return not safe
                }
            }
        }
        // if no false, true; safe and request can be granted
        return true;
    }

}
