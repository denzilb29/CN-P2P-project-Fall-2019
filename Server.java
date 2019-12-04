import java.net.*;
import java.io.*;
import java.util.*;
import java.lang.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.BufferedReader; 
import java.io.InputStreamReader;

class ServerThread extends SocketThread {
    
    protected int clientId = -1;
    
    private int generatePID() {
        int count=0;
        while(count < 100){
            if (Server.peerConfig.containsKey(count) && (!Server.peerList.containsKey(count))) {
                Server.peerList.put(count, (Server.peerConfig.get(count)).get(0));
                return count;
            }
            ++count;
        }
        return -1;
    }

    
    public void run() {
        for(;;) {
            try {
                System.out.println("[Server] Server is listening command:");
                Object payload;
                for(;;) {
                    try {
                        payload = this.iStream.readObject();
                        assert(payload instanceof String);
                        break;
                    } catch (Exception ignored) {

                    }
                }
                String message = (String) payload;
                System.out.println("[Server] Received message (" + message + ") from " + clientId);
                int p = -1;

                


                //switch (message) {

                    if (message.compareTo("LIST")==0) {
                        // Send chunk list
                        ArrayList<Integer> q = new ArrayList<Integer>(this.workingChunk.size());
                        int i=0;
                        while(i < this.workingChunk.size()){
                            if(this.workingChunk.containsKey(i)){
                                q.add(i);
                            }
                            ++i;
                        }
                        send(q);
                        //break;
                    }
                    else if (message.compareTo("NAME")==0) {
                        // Send file name
                        // Additional let us send the filename
                        send((Object) this.fileName);
                        //break;
                    }
                    else if(message.compareTo("REQUEST")==0) {
                        // Read first int as chunk number
                        p = this.iStream.readInt();
                        // Send that chunk
                        send(p);
                        send(this.workingChunk.get(p));
                       // break;
                    }
                    else if(message.compareTo("DATA")==0) {
                        // Read first int as chunk number
                        p = this.iStream.readInt();
                        // Save received data
                        byte[] chunk = (byte[]) this.iStream.readObject();
                        //break;
                    }
                    else if(message.compareTo("REGISTER")==0) {
                        // Read first int as peer port
                        // int port = this.iStream.readInt();
                        // Return a peer id for client
                        int peer = this.generatePID();
                        int port = Server.peerList.get(peer);
                        send(peer);
                        send(port);
                        //break;
                    }
                    else if(message.compareTo("PEER")==0) {
                        // Send Peer HasMap
                        System.out.print("[Server] Peer list:");
                        int clientPeerId = iStream.readInt();
                        for (int _peer : Server.peerList.keySet()) {
                            System.out.print(_peer + " ");
                        }
                        System.out.println();
                        System.out.println("[Server] Send U/D neighbor to clients");
                        send(Server.peerList);
                        send((Object) (Server.peerConfig.get(clientPeerId)).get(1));
                        send((Object) (Server.peerConfig.get(clientPeerId)).get(2));
                       // break;
                    }
                    else if(message.compareTo("CLOSE")==0) {
                        // Close stream and exit thread
                        oStream.close();
                        iStream.close();
                        Server.peerList.remove(clientId);
                        return;
                    }
                //}


            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
                System.out.println("[" + this.getName() + "]: Session ended.");
                Server.peerList.remove(clientId);
                return;
            }
        }
    }
}

// This is the file owner server
public class Server extends Peer{

    private String peerName = "Server";
    String wd = System.getProperty("user.dir");
    private String filePath = wd+"//Test.pptx";
    public static int port = 37000;
    private ServerSocket serverSocket;
    public static HashMap<Integer, Integer> peerList = new HashMap<Integer, Integer>();
    public static HashMap<Integer, ArrayList<Integer>> peerConfig = new HashMap<Integer, ArrayList<Integer>>();
    //String wd = System.getProperty("user.dir");

    static{
        if (peerList != null) {
            String reqFile = "Test-Peer";
        }
        else{
            peerList = new HashMap<Integer, Integer>();
        }
    }

    public Server() {
        
        this(System.getProperty("user.dir")+"//Test.pptx", 37000);
    }

    public Server(String filePath) {
        this(filePath, 37000);
    }

    public Server(String filePath, int port) {
        /*if (null != filePath && new File(filePath).exists()) {
            this.filePath = filePath;
        }*/

        if (null != filePath) {
            if(new File(filePath).exists()){
                this.filePath = filePath;
            }
        }
        else{
            HashMap<Integer,Integer> m = new HashMap<>();
        }

        this.port = port;

        try {
            serverSocket = new ServerSocket(this.port);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        // Init file chunk list
        this.chunkFile();
    }

    protected void chunkFile() {
        try {
            File sepDir = new File("ServerDir");
            /*if(!sepDir.exists()) {
                sepDir.mkdir();
            }*/

            int p=0;
            if(sepDir.exists()) {
                p++;
            }
            else{
                sepDir.mkdir();
            }
            // Read everything to memory
            FileInputStream fs = new FileInputStream(this.filePath);
            byte[] buffer = new byte[this.BUFFER_SIZE];
            int ch;
            int index = 0;
            for(;(ch = fs.read(buffer)) != -1;) {
                byte[] bChunk = Arrays.copyOfRange(buffer, 0, ch);
                chunkList.put(index, bChunk);
                System.out.println("Chunk #" + index + " = " + ch + "bytes");
                FileOutputStream fso = new FileOutputStream("ServerDir/" + index, false);
                fso.write(bChunk);
                fso.flush();
                fso.close();
                buffer = new byte[this.BUFFER_SIZE];
                index++;
            }
            System.out.println("[Server] Total " + index + " chunks");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void Start() {

        try {
            for(;;) {
                System.out.println(this.peerName + " is waiting clients...");
                Socket p = serverSocket.accept();
                ServerThread st = new ServerThread();
                st.setFileChunk(chunkList);
                st.setFileName(this.filePath);
                st.setSocket(p);
                st.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static void ReadConfig() {
        try {
            Scanner f = new Scanner(new FileInputStream("config.txt"));
            // server line
            int serverId = f.nextInt();
            port = f.nextInt();
            for(;f.hasNext();) {
                int peerId = f.nextInt();
                ArrayList<Integer> peerInfo = new ArrayList<Integer>();
                peerInfo.add(f.nextInt());
                peerInfo.add(f.nextInt());
                peerInfo.add(f.nextInt());
                peerConfig.put(peerId, peerInfo);
            }
            f.close();
        } catch (FileNotFoundException e) {
            port = 37000;
            System.out.println("Configuration failed. Will use random port for all peers");
        }
    }


        private static void appendUsingBufferedWriter(String filePath, String text,int noOfLines) {
        File file = new File(filePath);
        FileWriter fr = null;
        BufferedWriter br = null;
        try {
            // to append to file, you need to initialize FileWriter using below constructor
            fr = new FileWriter(file, true);
            br = new BufferedWriter(fr);

            if (file.length() == 0) {
                //System.out.println("File is empty ...");
                br.write(text);

            } else {
                //System.out.println("File is not empty ...");
                br.newLine();
                // you can use write or append method
                br.write(text);
            }
            

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                br.close();
                fr.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws IOException{

        String wd = System.getProperty("user.dir");
        String filePath = wd+"//config.txt";
        //String appendText = "9000 1 2";

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        

       /* System.out.println("Enter the server/client id number\n");
        String eid = reader.readLine();
        System.out.println("ID entered is "+eid+"\n");

        System.out.println("Enter the port number\n");
        String eport = reader.readLine();
        System.out.println("Port number entered is "+eport+"\n");
        
        String appendText = eid+" "+eport;
        System.out.println(appendText);*/
        
        //appendUsingBufferedWriter(filePath,appendText,1);

        /*try {
            Thread.sleep(60000);                 //1000 milliseconds is one second.
        } catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
        }*/

        ReadConfig();

        String inputFile = "sample.txt";

        if(args.length <= 0) {
            ArrayList<Integer> ls = new ArrayList<>();
        }
        else{
            inputFile = args[0];
        }

        new Server(inputFile, port).Start();
    }
}
