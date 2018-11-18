/**
 * Ciara McMullin
 * Operating Systems Fall 2018
 * Lab 3: Banker's
 * This class is used for the Activity object. It holds the instruction, task number, resource number, and claim.
 * It is initialized with the input data from the file and added to it's corresponding task.
 *
 */
public class Activity {
    String instruction;
    int taskNum;
    int resourceNum;
    int claim;

    public Activity (String instruction, int taskNum, int resourceNum, int claim, int id){
        this.instruction = instruction;
        this.taskNum = taskNum;
        this.resourceNum = resourceNum;
        this.claim = claim;
    }



}
