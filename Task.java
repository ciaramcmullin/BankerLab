/**
 * Ciara McMullin
 * Operating Systems Fall 2018
 * Lab 3: Banker's
 * Task class used for task objects. It holds the status of the task, time, blocked/ waiting time, computing time,
 * if aborted, resource list, claims, and activities
 */

import java.util.*;

public class Task {

    String status;
    int totalTime;
    int waitTime;
    int taskID;
    int computing;
    boolean aborted;
    ArrayList<Integer> resourceList;
    ArrayList<Integer> claim;
    ArrayList<Activity> activities;

    /**
     * initialize task object to default values and taskID and number of resources for resource list
     * @param taskID
     * @param numOfResources
     */
    public Task(int taskID, int numOfResources){
        this.status = "unstarted";
        this.totalTime = 0;
        this.waitTime = 0;
        this.taskID = taskID;
        this.claim = new ArrayList<>(numOfResources);
        this.resourceList = new ArrayList<>(numOfResources);
        for(int i = 0; i < numOfResources; i++){
            claim.add(0);
            resourceList.add(0);
        }
        this.aborted = false;
        this.activities = new ArrayList<>();
    }

    /**
     * This is used to easily make copies of tasks
     * @param t
     */
    public Task(Task t){
        this.status = t.status;
        this.totalTime = t.totalTime;
        this.waitTime = t.waitTime;
        this.taskID = t.taskID;
        this.claim = t.claim;
        this.aborted = t.aborted;
        this.activities = t.activities;
        this.resourceList = t.resourceList;

    }

    /**
     * This method is used to sort the tasks based on the ID numbers for printing the output
     */
    public static class CompID implements Comparator<Task> {
        @Override
        public int compare(Task t1, Task t2) {
            return t1.taskID - t2.taskID;
        }
    }




}
