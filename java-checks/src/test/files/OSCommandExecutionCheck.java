import java.io.*;

public class JavaRunCommand {

    public static void main(String args[]) {

        String s = null;
        String samp = "| grep";

        try {
            // 1. no branch, constant str : no issue
            Process p2 = Runtime.getRuntime().exec("ps -ef");               // Compliant

            // 2. no branch, dynamic str : issue
            Process p = Runtime.getRuntime().exec("ps -ef" + samp);

            // 2. no branch, dynamic str : issue
            Process p1 = Runtime.getRuntime().exec("ps -ef" + samp);

            // 3. branch, dynamic str : no issue
            if(1) {
                Process p3 = Runtime.getRuntime().exec("ps -ef" + samp);    // Compliant
            }

            // 4. branch, constant str : no issue
            if(1) {
                Process p4 = Runtime.getRuntime().exec("ps -ef");           // Compliant
            }


            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(p.getInputStream()));


            BufferedReader stdError = new BufferedReader(new
                    InputStreamReader(p.getErrorStream()));

            // read the output from the command
            System.out.println("Here is the standard output of the command:\n");
            while ((s = stdInput.readLine()) != null) {
                System.out.println(s);
            }

            // read any errors from the attempted command
            System.out.println("Here is the standard error of the command (if any):\n");
            while ((s = stdError.readLine()) != null) {
                System.out.println(s);
            }

            System.exit(0);
        }
        catch (IOException e) {
            System.out.println("exception happened - here's what I know: ");
            e.printStackTrace();
            System.exit(-1);
        }
    }
}