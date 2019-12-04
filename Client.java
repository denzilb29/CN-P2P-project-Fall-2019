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
    protected int pid;
//hello
    public void setPid(int id) {
        this.pid = id;
    }

    @Override
    public void run() {
        for (;;) {
            try {
                System.out.println("[" + this.peerName + "] Peer is listening command:");
                Object packet = this.iStream.readObject();
                assert (packet instanceof String);
                String msg = (String) packet;
                System.out.println("[" + this.peerName + "] Received message (" + msg + ")");
                int readIndex = -1;
                if(msg.compareTo("LIST") == 0){
                    ArrayList<Integer> list = new ArrayList<Integer>(this.workingChunk.size());
                    for (Integer v : this.workingChunk.keySet()) {
                        list.add(v);
                    }
                    send(list);
                }
                else if(msg.compareTo("REQUEST") == 0){
                    readIndex = this.iStream.readInt();
                    send(readIndex);
                    send(this.workingChunk.get(readIndex));
                }
                else if(msg.compareTo("ASK") == 0){
                    readIndex = this.iStream.readInt();
                    if (!this.workingChunk.containsKey(readIndex)) {
                        send(0);
                    }
                    else {
                        send(1);
                    }
                }
                else if(msg.compareTo("DATA") == 0){
                    readIndex = this.iStream.readInt();
                    byte[] piece = (byte[]) this.iStream.readObject();
                    int t=0;
                    if (this.workingChunk.containsKey(readIndex)) {
                        ++t;
                    }
                    else{
                        workingChunk.put(readIndex, piece);
                        System.out.println("Received Chunk #" + readIndex);
                        storePiece(readIndex, piece);
                    }
                }
                else if(msg.compareTo("CLOSE") == 0){
                    oStream.close();
                    iStream.close();
                    return;
                }
            }
            catch (ClassNotFoundException | IOException exceptions) {
                exceptions.printStackTrace();
                System.out.println("[" + this.getName() + "]: Session ended.");
                return;
            }
        }
    }

    private void storePiece(int x, byte[] chunk) {
        try {
            FileOutputStream fos = new FileOutputStream("PEER" + this.pid + "Dir/" + x, false);
            fos.write(chunk);
            fos.flush();
            fos.close();
        }
        catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }
}

public class Client extends Peer implements Runnable {

    public static int sp = 37000;

    public static String mfn = "merge.dat";

    public static int p = -2;
    public ServerSocket css;
    static int pid = -1;
    public static HashMap<Integer, Integer> pl = new HashMap<Integer, Integer>();

    private int dPo = -1;
    private int dPe = -1;
    private int uPo = -1;
    private int uPe = -1;

    public Client(int sp)
    {
        this.sp = sp;
    }

    public Client() {
        this(9000);
    }


    public static void printMsg(ObjectOutputStream stream, String msg) throws IOException {
        stream.writeObject(msg);
        stream.flush();
        stream.reset();
    }

    public static void storeD(ObjectOutputStream stream, byte[] payload) throws IOException {
        stream.writeObject(payload);
        stream.flush();
        stream.reset();
    }

    public static void printMsg(ObjectOutputStream stream, int value) throws IOException {
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
            File fout = new File(mfn);
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
            Socket s = new Socket("localhost", sp);
            ObjectOutputStream oStream = new ObjectOutputStream(s.getOutputStream());
            printMsg(oStream, "REGISTER");
            ObjectInputStream iStream = new ObjectInputStream(s.getInputStream());
            pid = iStream.readInt();
            p = iStream.readInt();
            peerName = "PEER" + pid;
            System.out.println(pid);
            // Create a peerDir to save file chunk from server
            File peerDir = new File(peerName + "Dir");
            if(peerDir.exists()) {
                System.out.println("Dir exists.");
            }
            else{
                peerDir.mkdir();
            }
            //Step 2: Get chunk list
            printMsg(oStream, "LIST");
            chunkIndex = (ArrayList<Integer>) iStream.readObject();
            //Step 3: Get initial chunks from server;
            int startIndex = (int)(1.0 * chunkIndex.size() / TOTAL_PEERS * (pid % TOTAL_PEERS));
            int endIndex = (int)(1.0 * chunkIndex.size() / TOTAL_PEERS * ((pid  % TOTAL_PEERS) + 1));
            int i = startIndex;
            while(i < endIndex){
                printMsg(oStream, "REQUEST");
                printMsg(oStream, chunkIndex.get(i));
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
            printMsg(oStream, "NAME");
            String filePath = (String) iStream.readObject();
            String basename = new File(filePath).getName();
            String extension = basename.substring(basename.lastIndexOf('.') + 1);
            String fileRoot = basename.substring(0, basename.lastIndexOf('.'));
            mfn = fileRoot + "-peer-" + pid + "." + extension;
            System.out.println("Output file is " + mfn);
            //Step 4: Get a upload neighbor and download neighbor
            ////////////////////////////////////////////////////
            printMsg(oStream, "PEER");
            printMsg(oStream, pid);
            pl = (HashMap<Integer, Integer>) iStream.readObject();

            System.out.println("[" + peerName + "] Ask bootstrap server for neighbors:");
            dPe = (int) iStream.readObject();
            uPe = (int) iStream.readObject();
            dPo = pl.containsKey(dPe) ? pl.get(dPe) : 0;
            uPo = pl.containsKey(uPe) ? pl.get(uPe) : 0;
            Thread.sleep(1000);
            while (this.dPo <= 0 || this.uPo <= 0){
                printMsg(oStream, "PEER");
                printMsg(oStream, pid);
                pl = (HashMap<Integer, Integer>) iStream.readObject();

                System.out.println("[" + peerName + "] Ask bootstrap server for neighbors:");
                dPe = (int) iStream.readObject();
                uPe = (int) iStream.readObject();
                dPo = pl.containsKey(dPe) ? pl.get(dPe) : 0;
                uPo = pl.containsKey(uPe) ? pl.get(uPe) : 0;
                Thread.sleep(1000);
            }
            System.out.println("[" + peerName + "] Uploading to " + uPe + ":" + uPo);
            System.out.println("[" + peerName + "] Downloading from " + dPe + ":" + dPo);

            (new Thread() {
                @Override
                public void run() {
                    try {
                        System.out.println("==================");
                        Thread.sleep(10000);
                        System.out.println("Making upload connection...");
                        Socket upSock = new Socket("localhost", uPo);
                        ObjectOutputStream oUpStream = new ObjectOutputStream(upSock.getOutputStream());
                        System.out.println("Making download connection...");
                        Socket downSock = new Socket("localhost", dPo);
                        ObjectOutputStream oDownStream = new ObjectOutputStream(downSock.getOutputStream());
                        ObjectInputStream iDownStream = new ObjectInputStream(downSock.getInputStream());
                        System.out.println("Connection Made!");
                        for (;!checkingChunk();) {
                            System.out.println("Got chunk list from neighbor");
                            printMsg(oDownStream, "LIST");
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
                                    System.out.println("[" + Client.peerName + "] Ask PEER" + dPe + " Chunk #" + q);
                                    printMsg(oDownStream, "ASK");
                                    printMsg(oDownStream, q);
                                    if (iDownStream.readInt() != 1) { //Means peer has that chunk
                                        System.out.println("[" + Client.peerName + "] PEER" + dPe + " doesn't have Chunk #" + q);
                                    } 
                                    else {
                                        printMsg(oDownStream, "REQUEST");
                                        printMsg(oDownStream, q);
                                        int x = iDownStream.readInt();
                                        byte[] chunk = (byte[]) iDownStream.readObject();
                                        Client.chunkList.put(x, chunk);
                                        System.out.println("Received Chunk #" + chunkIndex.get(i) + " from Peer " + dPe);
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
                                    printMsg(oUpStream, "DATA");
                                    printMsg(oUpStream, q);
                                    storeD(oUpStream, Client.chunkList.get(q));
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

            for(; p < 0;){
                Thread.sleep(500);
            }

            css = new ServerSocket(this.p);

            for (;;) {
                ClientSocket localDaemon = new ClientSocket();
                Socket socket = null;
                try {
                    System.out.println("Peer is listening at Port " + css.getLocalPort());
                    socket = css.accept();
                    localDaemon.setSocket(socket);
                    localDaemon.setPid(Client.pid);
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
            p = f.nextInt();
            f.close();
            return p;

        } catch (FileNotFoundException e) {
            System.out.println("Configuration failed. Will use random port for all peers");

        }
        return p = 37000;
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
