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

    public boolean checkPiece() {
        for(int i: chunkIndex) {
            if(chunkList.containsKey(i)){
                continue;
            }
            else{
                return false;
            }
        }
        //This means we already had all chunks, combine and write it out
        try {
            File f = new File(mfn);
            int h = 0;
            if(!f.exists()){
                ++h;
            }
            else{
                f.delete();
            }
            FileOutputStream fos = new FileOutputStream(f);
            int i=0;
            while(i < chunkIndex.size()){
                storeF(i, chunkList.get(chunkIndex.get(i)));
                fos.write(chunkList.get(chunkIndex.get(i)));
                ++i;
            }
            fos.flush();
            fos.close();
            System.out.println("[" + peerName + "] Finished downloading and writing");
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }

        return true;
    }

    public void Start() {
        try {
            //Step 1: Bootstrap. Register and get peer id
            Socket s = new Socket("localhost", sp);
            ObjectOutputStream os = new ObjectOutputStream(s.getOutputStream());
            printMsg(os, "REGISTER");
            ObjectInputStream is = new ObjectInputStream(s.getInputStream());
            pid = is.readInt();
            p = is.readInt();
            peerName = "PEER" + pid;
            System.out.println(pid);
            // Create a peerDir to save file chunk from server
            File pd = new File(peerName + "Dir");
            if(pd.exists()) {
                System.out.println("Dir exists.");
            }
            else{
                pd.mkdir();
            }
            //Step 2: Get chunk list
            printMsg(os, "LIST");
            chunkIndex = (ArrayList<Integer>) is.readObject();
            //Step 3: Get initial chunks from server;
            int begin = (int)(1.0 * chunkIndex.size() / TOTAL_PEERS * (pid % TOTAL_PEERS));
            int end = (int)(1.0 * chunkIndex.size() / TOTAL_PEERS * ((pid  % TOTAL_PEERS) + 1));
            int i = begin;
            while(i < end){
                printMsg(os, "REQUEST");
                printMsg(os, chunkIndex.get(i));
                int temp = is.readInt();
                byte[] piece = (byte[]) is.readObject();
                chunkList.put(temp, piece);
                System.out.println("Received Chunk #" + chunkIndex.get(i) + " from server");
                storeF(temp, piece);
                ++i;
            }
            makeSumF(0);
            Random r = new Random();
            // Step 3-1: Get filename
            printMsg(os, "NAME");
            String fp = (String) is.readObject();
            String get_base = new File(fp).getName();
            String get_ext = get_base.substring(get_base.lastIndexOf('.') + 1);
            String fr = get_base.substring(0, get_base.lastIndexOf('.'));
            mfn = fr + "-peer-" + pid + "." + get_ext;
            System.out.println("Output file is " + mfn);
            //Step 4: Get a upload neighbor and download neighbor
            ////////////////////////////////////////////////////
            printMsg(os, "PEER");
            printMsg(os, pid);
            pl = (HashMap<Integer, Integer>) is.readObject();

            System.out.println("[" + peerName + "] Ask bootstrap server for neighbors:");
            dPe = (int) is.readObject();
            uPe = (int) is.readObject();
            dPo = pl.containsKey(dPe) ? pl.get(dPe) : 0;
            uPo = pl.containsKey(uPe) ? pl.get(uPe) : 0;
            Thread.sleep(1000);
            while (this.dPo <= 0 || this.uPo <= 0){
                printMsg(os, "PEER");
                printMsg(os, pid);
                pl = (HashMap<Integer, Integer>) is.readObject();

                System.out.println("[" + peerName + "] Ask bootstrap server for neighbors:");
                dPe = (int) is.readObject();
                uPe = (int) is.readObject();
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
                        Socket us = new Socket("localhost", uPo);
                        ObjectOutputStream ous = new ObjectOutputStream(us.getOutputStream());
                        System.out.println("Making download connection...");
                        Socket dsk = new Socket("localhost", dPo);
                        ObjectOutputStream ods = new ObjectOutputStream(dsk.getOutputStream());
                        ObjectInputStream ids = new ObjectInputStream(dsk.getInputStream());
                        System.out.println("Connection Made!");
                        for (;!checkPiece();) {
                            System.out.println("Got chunk list from neighbor");
                            printMsg(ods, "LIST");
                            ArrayList<Integer> list = (ArrayList<Integer>) ids.readObject();
                            int i=0;
                            while(i < list.size()){
                                int q = list.get(i);
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
                                int temp = Client.chunkIndex.get(i);
                                if (!Client.chunkList.containsKey(temp)) {
                                    System.out.println("[" + Client.peerName + "] Ask PEER" + dPe + " Chunk #" + temp);
                                    printMsg(ods, "ASK");
                                    printMsg(ods, temp);
                                    if (ids.readInt() != 1) { //Means peer has that chunk
                                        System.out.println("[" + Client.peerName + "] PEER" + dPe + " doesn't have Chunk #" + temp);
                                    } 
                                    else {
                                        printMsg(ods, "REQUEST");
                                        printMsg(ods, temp);
                                        int temp2 = ids.readInt();
                                        byte[] piece = (byte[]) ids.readObject();
                                        Client.chunkList.put(temp2, piece);
                                        System.out.println("Received Chunk #" + chunkIndex.get(i) + " from Peer " + dPe);
                                    }
                                }
                                else{
                                    continue;
                                }
                            }
                            System.out.println("[" + Client.peerName + "] Finished pulling...");
                            System.out.println("Start pushing chunk list...");
                            for (Integer ii : Client.chunkIndex) {
                                int temp = ii;
                                if (Client.chunkList.containsKey(temp)) {
                                    System.out.print(temp + " ");
                                    printMsg(ous, "DATA");
                                    printMsg(ous, temp);
                                    storeD(ous, Client.chunkList.get(temp));
                                }
                                else{
                                    continue;
                                }

                            }
                            System.out.println();
                            System.out.println("[" + Client.peerName + "] Finished pushing, sleep 1sec.");
                            Thread.sleep(1000);
                        }
                    } catch (IOException | ClassNotFoundException | InterruptedException exceptions) {
                        exceptions.printStackTrace();
                    }

                }
            }).start();

            for(; p < 0;){
                Thread.sleep(500);
            }

            css = new ServerSocket(this.p);

            for (;;) {
                ClientSocket ld = new ClientSocket();
                Socket sckt = null;
                try {
                    System.out.println("Peer is listening at Port " + css.getLocalPort());
                    sckt = css.accept();
                    ld.setSocket(sckt);
                    ld.setPid(Client.pid);
                    ld.setFileChunk(chunkList);
                    ld.start();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }

        } catch (IOException | ClassNotFoundException | InterruptedException exceptions) {
            exceptions.printStackTrace();
        }


    }

    private void storeF(int x, byte[] chunk) {
        try {
            FileOutputStream fos = new FileOutputStream(peerName + "Dir/" + x, false);
            fos.write(chunk);
            fos.flush();
            fos.close();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    private void makeSumF(int c) {
            try {
                FileOutputStream fos = new FileOutputStream(peerName + "Dir/summary.txt", false);
                StringBuilder builder = new StringBuilder();
                int i=0;
                while(i < Client.chunkIndex.size()){
                    int temp = Client.chunkIndex.get(i);
                    ++i;
                    if (!Client.chunkList.containsKey(temp)) {
                        continue;
                    }
                    else{
                        builder.append(temp);
                        builder.append(" ");
                    }
                }
                fos.write(builder.toString().getBytes());
                fos.flush();
                fos.close();
            } catch (FileNotFoundException fileNotFoundException) {
                fileNotFoundException.printStackTrace();
            } catch (IOException ioException) {
                ioException.printStackTrace();
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
