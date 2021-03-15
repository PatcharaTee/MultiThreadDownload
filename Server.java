/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package MultiThreadDownload;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

/*
import java.util.logging.Level;
import java.util.logging.Logger;
 */
/**
 *
 * @author Admin
 */
public class Server {

    public static void main(String[] args) throws IOException {
        //load server config
        ServerConfig scf = new ServerConfig();
        //Set up - Admin can set where client to read file and port at class ServerConfig
        int port = scf.getPort();
        ServerSocket serverSock = new ServerSocket(port);
        System.out.println("Server started.");

        //Wait for connection
        while (true) {
            try {
                Socket clientSock = serverSock.accept();
                System.out.println("Client accept. " + clientSock);
                //Data i/o for communication with client
                DataInputStream dis = new DataInputStream(clientSock.getInputStream());
                DataOutputStream dos = new DataOutputStream(clientSock.getOutputStream());
                //New client thread
                ClientHandler handler = new ClientHandler(clientSock, dis, dos);
                handler.start();
            } catch (IOException ex) {
                //System.out.println(ex);
            }
        }
    }
}

class ServerConfig {

    private final int port = 9000;
    private final String serverFolder = "//change before use";

    public String getServerFolder() {
        return serverFolder;
    }

    public int getPort() {
        return port;
    }
}

class ClientHandler extends Thread {

    //load server config
    ServerConfig scf = new ServerConfig();
    //var
    File serverFolder = new File(scf.getServerFolder());
    Socket clientSock;
    DataInputStream dis;
    DataOutputStream dos;

    public ClientHandler(Socket clientSock, DataInputStream dis, DataOutputStream dos) {
        this.clientSock = clientSock;
        this.dis = dis;
        this.dos = dos;
    }

    @Override
    public void run() {
        System.out.println("Thread start for Client " + clientSock);
        File[] files = serverFolder.listFiles();
        while (true) {
            try {
                String recive = dis.readUTF();
                String toSend;
                switch (recive) {
                    case "Files":
                        //Return message
                        int totalFiles = files.length;
                        dos.write(totalFiles);
                        //Sent all file name to client to print
                        for (File file : files) {
                            String filename = file.getName();
                            dos.writeUTF(filename);
                        }
                        //Return message
                        toSend = "Finish tranfer.";
                        dos.writeUTF(toSend);
                        break;
                    case "Download":
                        //Recive file name from client
                        String fileName = dis.readUTF();
                        //Find lenght of file
                        int fileSize = 0;
                        for (File file : files) {
                            if (fileName.equals(file.getName())) {
                                fileSize = (int) file.length();
                                break;
                            }
                        }
                        //Return message (file size, path to file)
                        dos.writeInt(fileSize);
                        if (fileSize > 0) {
                            //Server echo
                            System.out.println("Client " + clientSock + " download request file " + fileName + ", Size = " + fileSize + " bytes, Start sending file.");
                            //Return message
                            toSend = "Finished tranfer file.";
                            dos.writeUTF(toSend);
                        } else {
                            //Server echo
                            System.out.println("Client " + clientSock + " download request file " + fileName + ", File not found.");
                            //Return message
                            toSend = "File not found.";
                            dos.writeUTF(toSend);
                        }
                        break;
                    case "Exit":
                        //Server echo
                        System.out.println("Client disconnect. " + clientSock);
                        //Return message
                        toSend = "Connection is closed, Goodbye.";
                        dos.writeUTF(toSend);
                        break;
                    default:
                        //Return message
                        toSend = "Wrong Input.";
                        dos.writeUTF(toSend);
                }
            } catch (IOException ex) {
                //Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}

class Download implements Callable<byte[]> {

    //load server config
    ServerConfig scf = new ServerConfig();
    //var
    int startIndex, endIndex;
    String fileName, serverFolder = scf.getServerFolder();

    public Download(String fileName, int startIndex, int endIndex) {
        this.fileName = fileName;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
    }

    @Override
    public byte[] call() throws FileNotFoundException, IOException {
        int len = (endIndex - startIndex) + 1;
        byte[] dPart = new byte[len];
        RandomAccessFile raf = new RandomAccessFile(serverFolder + "/" + fileName, "r");
        raf.seek(startIndex);
        raf.read(dPart, 0, len);
        raf.close();
        return dPart;
    }
}
