package ua.stamanker;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.SCPClient;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Application {

    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();
        if(args.length<3) {
            throw new RuntimeException("Program args: host login pwd path2Build path2RemoteDir");
        }
        String hostname = args[0];
        String login = args[1];
        String password = args[2];
        String localFile = args[3];
        String remoteDir = args[4];
        System.out.println("Connect: " + hostname);
        Connection conn = new Connection(hostname);
        conn.connect();
        if (!conn.authenticateWithPassword(login, password)) {
            throw new IOException("Authentication failed.");
        }
        transfer(conn, localFile, remoteDir);
        executeCommand(conn, "ls -lt");
        executeCommand(conn, "./0");
        executeCommand(conn, "./1");
        conn.close();
        System.out.println(String.format("%s Processing took %d ms",
                new SimpleDateFormat("HH:mm:ss").format(new Date()),
                (System.currentTimeMillis() - start))
        );
    }

    private static void transfer(Connection conn, String localFile, String remoteDir) throws IOException {
        System.out.println("transferring file: " + localFile);
        SCPClient scpClient = conn.createSCPClient();
        scpClient.put(localFile, remoteDir);
    }

    private static void executeCommand(Connection conn, String command) throws IOException {
        Session sess = conn.openSession();
        try {
            sess.execCommand(command);
            try (InputStream stdout = new StreamGobbler(sess.getStdout())) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(stdout))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        System.out.println("\t"+line);
                    }
                }
            }
            /* Show exit status, if available (otherwise "null") */
            System.out.println("ExitCode: " + sess.getExitStatus());
        } finally {
            sess.close();
        }
    }
}
