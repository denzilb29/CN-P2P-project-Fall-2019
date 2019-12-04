import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.BufferedReader; 
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import java.net.*;
import java.io.*;
import java.util.*;
import java.lang.*;

class ClientSocket extends SocketThread {
//test
    protected int peerId;
//hello
    public void setPeerId(int id) {
        this.peerId = id;
    }

    @Override
    public void run() {
        for (;;) {
            try {
                System.out.println("[" + this.peerName + "] Peer is listening command:");
                Object payload = this.iStream.readObject();
                assert (payload instanceof String);
                String message = (String) payload;
                System.out.println("[" + this.peerName + "] Received message (" + message + ")");
                int p = -1;
                if(message.compareTo("LIST") == 0){
                    ArrayList<Integer> q = new ArrayList<Integer>(this.workingChunk.size());
                    for (Integer key : this.workingChunk.keySet()) {
                        q.add(key);
                    }
                    send(q);
                }
                else if(message.compareTo("REQUEST") == 0){
                    p = this.iStream.readInt();
                    send(p);
                    send(this.workingChunk.get(p));
                }
                else if(message.compareTo("ASK") == 0){
                    p = this.iStream.readInt();
                    if (!this.workingChunk.containsKey(p)) {
                        send(0);
                    }
                    else {
                        send(1);
                    }
                }
                else if(message.compareTo("DATA") == 0){
                    p = this.iStream.readInt();
                    byte[] chunk = (byte[]) this.iStream.readObject();
                    int t=0;
                    if (this.workingChunk.containsKey(p)) {
                        ++t;
                    }
                    else{
                        workingChunk.put(p, chunk);
                        System.out.println("Received Chunk #" + p);
                        saveChunkFile(p, chunk);
                    }
                }
                else if(message.compareTo("CLOSE") == 0){
                    oStream.close();
                    iStream.close();
                    return;
                }
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
                System.out.println("[" + this.getName() + "]: Session ended.");
                return;
            }
        }
    }

    private void saveChunkFile(int x, byte[] chunk) {
        try {
            FileOutputStream fso = new FileOutputStream("PEER" + this.peerId + "Dir/" + x, false);
            fso.write(chunk);
            fso.flush();
            fso.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

public class Client extends Peer implements Runnable {

    public static int serverPort = 37000;

    public static String mergeFileName = "merge.dat";

    public static int port = -2;
    public ServerSocket clientServerSocket;
    static int peerId = -1;
    public static HashMap<Integer, Integer> peerList = new HashMap<Integer, Integer>();

    private int downloadPort = -1;
    private int downloadPeer = -1;
    private int uploadPort = -1;
    private int uploadPeer = -1;

    public Client(int serverPort)
    {
        this.serverPort = serverPort;
    }

    public Client() {
        this(9000);
    }


    public static void writeMessage(ObjectOutputStream stream, String msg) throws IOException {
        stream.writeObject(msg);
        stream.flush();
        stream.reset();
    }

    public static void writeData(ObjectOutputStream stream, byte[] payload) throws IOException {
        stream.writeObject(payload);
        stream.flush();
        stream.reset();
    }

    public static void writeMessage(ObjectOutputStream stream, int value) throws IOException {
        stream.writeInt(value);
        stream.flush();
    }

    public boolean checkingChunk() {
        for(int key: chunkIndex) {
            if(chunkList.containsKey(key)){
                continue;
            }
            else{
                return false;
            }
        }
        //This means we already had all chunks, combine and write it out
        try {
            File fout = new File(mergeFileName);
            int h = 0;
            if(!fout.exists()){
                ++h;
            }
            else{
                fout.delete();
            }
            FileOutputStream fs = new FileOutputStream(fout);
            int i=0;
            while(i < chunkIndex.size()){
                saveChunkFile(i, chunkList.get(chunkIndex.get(i)));
                fs.write(chunkList.get(chunkIndex.get(i)));
                ++i;
            }
            fs.flush();
            fs.close();
            System.out.println("[" + peerName + "] Finished downloading and writing");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }

    public void Start() {
        try {
            //Step 1: Bootstrap. Register and get peer id
            Socket s = new Socket("localhost", serverPort);
            ObjectOutputStream oStream = new ObjectOutputStream(s.getOutputStream());
            writeMessage(oStream, "REGISTER");
            ObjectInputStream iStream = new ObjectInputStream(s.getInputStream());
            peerId = iStream.readInt();
            port = iStream.readInt();
            peerName = "PEER" + peerId;
            System.out.println(peerId);
            // Create a peerDir to save file chunk from server
            File peerDir = new File(peerName + "Dir");
            if(peerDir.exists()) {
                System.out.println("Dir exists.");
            }
            else{
                peerDir.mkdir();
            }
            //Step 2: Get chunk list
            writeMessage(oStream, "LIST");
            chunkIndex = (ArrayList<Integer>) iStream.readObject();
            //Step 3: Get initial chunks from server;
            int startIndex = (int)(1.0 * chunkIndex.size() / TOTAL_PEERS * (peerId % TOTAL_PEERS));
            int endIndex = (int)(1.0 * chunkIndex.size() / TOTAL_PEERS * ((peerId  % TOTAL_PEERS) + 1));
            int i = startIndex;
            while(i < endIndex){
                writeMessage(oStream, "REQUEST");
                writeMessage(oStream, chunkIndex.get(i));
                int x = iStream.readInt();
                byte[] chunk = (byte[]) iStream.readObject();
                chunkList.put(x, chunk);
                System.out.println("Received Chunk #" + chunkIndex.get(i) + " from server");
                saveChunkFile(x, chunk);
                ++i;
            }
            CreateSummaryFile(0);
            Random rand = new Random();
            // Step 3-1: Get filename
            writeMessage(oStream, "NAME");
            String filePath = (String) iStream.readObject();
            String basename = new File(filePath).getName();
            String extension = basename.substring(basename.lastIndexOf('.') + 1);
            String fileRoot = basename.substring(0, basename.lastIndexOf('.'));
            mergeFileName = fileRoot + "-peer-" + peerId + "." + extension;
            System.out.println("Output file is " + mergeFileName);
            //Step 4: Get a upload neighbor and download neighbor
            ////////////////////////////////////////////////////
            writeMessage(oStream, "PEER");
            writeMessage(oStream, peerId);
            peerList = (HashMap<Integer, Integer>) iStream.readObject();

            System.out.println("[" + peerName + "] Ask bootstrap server for neighbors:");
            downloadPeer = (int) iStream.readObject();
            uploadPeer = (int) iStream.readObject();
            downloadPort = peerList.containsKey(downloadPeer) ? peerList.get(downloadPeer) : 0;
            uploadPort = peerList.containsKey(uploadPeer) ? peerList.get(uploadPeer) : 0;
            Thread.sleep(1000);
            while (this.downloadPort <= 0 || this.uploadPort <= 0){
                writeMessage(oStream, "PEER");
                writeMessage(oStream, peerId);
                peerList = (HashMap<Integer, Integer>) iStream.readObject();

                System.out.println("[" + peerName + "] Ask bootstrap server for neighbors:");
                downloadPeer = (int) iStream.readObject();
                uploadPeer = (int) iStream.readObject();
                downloadPort = peerList.containsKey(downloadPeer) ? peerList.get(downloadPeer) : 0;
                uploadPort = peerList.containsKey(uploadPeer) ? peerList.get(uploadPeer) : 0;
                Thread.sleep(1000);
            }
            System.out.println("[" + peerName + "] Uploading to " + uploadPeer + ":" + uploadPort);
            System.out.println("[" + peerName + "] Downloading from " + downloadPeer + ":" + downloadPort);

            (new Thread() {
                @Override
                public void run() {
                    try {
                        System.out.println("==================");
                        Thread.sleep(10000);
                        System.out.println("Making upload connection...");
                        Socket upSock = new Socket("localhost", uploadPort);
                        ObjectOutputStream oUpStream = new ObjectOutputStream(upSock.getOutputStream());
                        System.out.println("Making download connection...");
                        Socket downSock = new Socket("localhost", downloadPort);
                        ObjectOutputStream oDownStream = new ObjectOutputStream(downSock.getOutputStream());
                        ObjectInputStream iDownStream = new ObjectInputStream(downSock.getInputStream());
                        System.out.println("Connection Made!");
                        for (;!checkingChunk();) {
                            System.out.println("Got chunk list from neighbor");
                            writeMessage(oDownStream, "LIST");
                            ArrayList<Integer> c = (ArrayList<Integer>) iDownStream.readObject();
                            int i=0;
                            while(i < c.size()){
                                int q = c.get(i);
                                if(!Client.chunkList.containsKey(q)) {
                                    System.out.print(q + "=> NEW\t");
                                }
                                else {
                                    System.out.print(q + "=>" + q + "\t");
                                }
                                ++i;
                            }
                            System.out.println();
                            for (i = 0; i < Client.chunkIndex.size(); i++) {
                                int q = Client.chunkIndex.get(i);
                                if (!Client.chunkList.containsKey(q)) {
                                    System.out.println("[" + Client.peerName + "] Ask PEER" + downloadPeer + " Chunk #" + q);
                                    writeMessage(oDownStream, "ASK");
                                    writeMessage(oDownStream, q);
                                    if (iDownStream.readInt() != 1) { //Means peer has that chunk
                                        System.out.println("[" + Client.peerName + "] PEER" + downloadPeer + " doesn't have Chunk #" + q);
                                    } 
                                    else {
                                        writeMessage(oDownStream, "REQUEST");
                                        writeMessage(oDownStream, q);
                                        int x = iDownStream.readInt();
                                        byte[] chunk = (byte[]) iDownStream.readObject();
                                        Client.chunkList.put(x, chunk);
                                        System.out.println("Received Chunk #" + chunkIndex.get(i) + " from Peer " + downloadPeer);
                                    }
                                }
                                else{
                                    continue;
                                }
                            }
                            System.out.println("[" + Client.peerName + "] Finished pulling...");
                            System.out.println("Start pushing chunk list...");
                            for (Integer aChunkIndex : Client.chunkIndex) {
                                int q = aChunkIndex;
                                if (Client.chunkList.containsKey(q)) {
                                    System.out.print(q + " ");
                                    writeMessage(oUpStream, "DATA");
                                    writeMessage(oUpStream, q);
                                    writeData(oUpStream, Client.chunkList.get(q));
                                }
                                else{
                                    continue;
                                }

                            }
                            System.out.println();
                            System.out.println("[" + Client.peerName + "] Finished pushing, sleep 1sec.");
                            Thread.sleep(1000);
                        }
                    } catch (IOException | ClassNotFoundException | InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }).start();

            for(;port < 0;){
                Thread.sleep(500);
            }

            clientServerSocket = new ServerSocket(this.port);

            for (;;) {
                ClientSocket localDaemon = new ClientSocket();
                Socket socket = null;
                try {
                    System.out.println("Peer is listening at Port " + clientServerSocket.getLocalPort());
                    socket = clientServerSocket.accept();
                    localDaemon.setSocket(socket);
                    localDaemon.setPeerId(Client.peerId);
                    localDaemon.setFileChunk(chunkList);
                    localDaemon.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        } catch (IOException | ClassNotFoundException | InterruptedException e) {
            e.printStackTrace();
        }


    }

    private void saveChunkFile(int x, byte[] chunk) {
        try {
            FileOutputStream fso = new FileOutputStream(peerName + "Dir/" + x, false);
            fso.write(chunk);
            fso.flush();
            fso.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void CreateSummaryFile(int c) {
            try {
                FileOutputStream fso = new FileOutputStream(peerName + "Dir/summary.txt", false);
                StringBuilder sb = new StringBuilder();
                int i=0;
                while(i < Client.chunkIndex.size()){
                    int q = Client.chunkIndex.get(i);
                    ++i;
                    if (!Client.chunkList.containsKey(q)) {
                        continue;
                    }
                    else{
                        sb.append(q);
                        sb.append(" ");
                    }
                }
                fso.write(sb.toString().getBytes());
                fso.flush();
                fso.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
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

            if (file.length() != 0) {
                br.newLine();
                br.write(text);
            } 
            else {
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

    @Override
    public void run() {
        Start();
    }

    private static int ReadConfig() {
        try {
            Scanner f = new Scanner(new FileInputStream("config.txt"));
            // server line
            int serverId = f.nextInt();
            port = f.nextInt();
            f.close();
            return port;

        } catch (FileNotFoundException e) {
            System.out.println("Configuration failed. Will use random port for all peers");

        }
        return port = 37000;
    }

    public static void main(String[] args) throws IOException {
        String wd = System.getProperty("user.dir");
        String filePath = wd+"//config.txt";
        //String appendText = "9000 1 2";
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
/*
        System.out.println("Enter the server/client id number\n");
        String fid = reader.readLine();
        System.out.println("ID entered is "+fid+"\n");

        System.out.println("Enter the port number\n");
        String fport = reader.readLine();
        System.out.println("Port number entered is "+fport+"\n");
        
        System.out.println("Enter the upload neighbour\n");
        String fupload = reader.readLine();
        System.out.println("Upload neighbour entered is "+fupload+"\n");

        System.out.println("Enter the download neighbour\n");
        String fdownload = reader.readLine();
        System.out.println("Download neighbour entered is "+fdownload+"\n");
    
        String appendText = fid+" "+fport+" "+fupload+" "+fdownload;
        System.out.println(appendText);
        
        appendUsingBufferedWriter(filePath,appendText,1);*/

        /*try {
            Thread.sleep(40000);                 //1000 milliseconds is one second.
        } catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
        }*/

        System.out.println("Do you want to begin? 1 for yes, 0 for no\n");
        String beginProcess = reader.readLine();
        int v = Integer.parseInt(beginProcess);

        if(v == 1){
            int _serverPort = ReadConfig();
            if(args.length > 0) {
                 _serverPort = Integer.parseInt(args[0]);
            }
            new Client(_serverPort).Start();
        }
        else {
            System.exit(0);
        }

    }
}
