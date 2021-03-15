/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package MultiThreadDownload;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 *
 * @author Admin
 */
public class Client {

    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
        //load server config
        ClientConfig ccf = new ClientConfig();
        //Set up - Client can set where to save file, server and port at class ClientConfig
        int port = ccf.getPort();
        String server = ccf.getServer();
        File clientFolder = new File(ccf.getClientFolder());
        Scanner scan = new Scanner(System.in);

        //Try to connect to server and Wait for user interaction
        try {
            Socket clientSock = new Socket(server, port);
            if (clientSock.isConnected()) {
                System.out.println("Connected to server.");
            }
            //Data i/o for communication with server
            DataInputStream dis = new DataInputStream(clientSock.getInputStream());
            DataOutputStream dos = new DataOutputStream(clientSock.getOutputStream());
            OUTER:
            while (true) {
                System.out.print("Type Files to get file list.\n"
                        + "     Download to download file.\n"
                        + "     Exit to disconnect.\n"
                        + "Input = ");
                //Send command to server
                String toSend = scan.nextLine();
                String recive;
                dos.writeUTF(toSend);
                switch (toSend) {
                    case "Files":
                        System.out.println("- File list -");
                        int totalFiles = dis.read();
                        for (int i = 1; i <= totalFiles; i++) {
                            System.out.println(i + " " + dis.readUTF());
                        }
                        //Recive message
                        recive = dis.readUTF();
                        System.out.println(recive + "\n");
                        break;
                    case "Download":
                        //Recive input from user and sent to filename to server
                        System.out.print("File name = ");
                        String fileName = scan.nextLine();
                        dos.writeUTF(fileName);
                        //Get file size from server
                        int fileSize = dis.readInt();
                        if (fileSize > 0) {
                            System.out.println("Downloading...");
                            int partSize = fileSize / 10;
                            FileOutputStream fos = new FileOutputStream(clientFolder + "/" + fileName, true);
                            ExecutorService executor = Executors.newFixedThreadPool(10);
                            for (int i = 0; i < 10; i++) {
                                int startIndex = i * partSize;
                                int endIndex = ((i + 1) * partSize) - 1;
                                if (i == 9) {
                                    endIndex = fileSize;
                                }
                                System.out.println("Part " + i + ", StartIndex = " + startIndex + ", EndIndex = " + endIndex);
                                Future<byte[]> dPart = executor.submit(new Download(fileName, startIndex, endIndex));
                                fos.write(dPart.get());
                            }
                            executor.shutdown();
                            fos.close();
                            //Recive message
                            recive = dis.readUTF();
                            System.out.println(recive + "\n");
                        } else {
                            //Recive message
                            recive = dis.readUTF();
                            System.out.println(recive + " \n");
                        }
                        break;
                    case "Exit":
                        //Recive message
                        recive = dis.readUTF();
                        System.out.println(recive + " ");
                        //Close connection and end loop
                        clientSock.close();
                        break OUTER;
                    default:
                        //Recive message
                        recive = dis.readUTF();
                        System.out.println(recive + "\n");
                        break;
                }
            }
        } catch (IOException ex) {
            System.out.println("Can't connect to the Server.");
            //System.out.println(ex);
            System.exit(0);
        }
    }
}

class ClientConfig {

    private final int port = 9000;
    private final String server = "127.0.0.1";
    private final String clientFolder = "//Change Dir before use";

    public int getPort() {
        return port;
    }

    public String getServer() {
        return server;

    }

    public String getClientFolder() {
        return clientFolder;
    }
}
