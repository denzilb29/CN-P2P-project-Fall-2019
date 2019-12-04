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

class MultithreadedS extends SocketThread {
    
    protected int cid = -1;
    
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
                Object obj;
                for(;;) {
                    try {
                        obj = this.iStream.readObject();
                        assert(obj instanceof String);
                        break;
                    } catch (Exception ignored) {

                    }
                }
                String m = (String) obj;
                System.out.println("[Server] Received message (" + m + ") from " + cid);
                int var1 = -1;
                if (m.compareTo("LIST")==0) {
                    ArrayList<Integer> list1 = new ArrayList<Integer>(this.workingChunk.size());
                    int i=0;
                    while(i < this.workingChunk.size()){
                        if(this.workingChunk.containsKey(i)){
                            list1.add(i);
                        }
                        ++i;
                    }
                    send(list1);
                }
                else if (m.compareTo("NAME")==0) {
                    send((Object) this.fileName);
                }
                else if(m.compareTo("REQUEST")==0) {
                    var1 = this.iStream.readInt();
                    send(var1);
                    send(this.workingChunk.get(var1));
                }
                else if(m.compareTo("DATA")==0) {
                    var1 = this.iStream.readInt();
                    byte[] chunk = (byte[]) this.iStream.readObject();
                }
                else if(m.compareTo("REGISTER")==0) {
                    int x = this.generatePID();
                    int y = Server.peerList.get(x);
                    send(x);
                    send(y);
                }
                else if(m.compareTo("PEER")==0) {
                    System.out.print("[Server] Peer list:");
                    int cpid = iStream.readInt();
                    for (int i : Server.peerList.keySet()) {
                        System.out.print(i + " ");
                    }
                    System.out.println();
                    System.out.println("[Server] Send U/D neighbor to clients");
                    send(Server.peerList);
                    send((Object) (Server.peerConfig.get(cpid)).get(1));
                    send((Object) (Server.peerConfig.get(cpid)).get(2));
                }
                else if(m.compareTo("CLOSE")==0) {
                    oStream.close();
                    iStream.close();
                    Server.peerList.remove(cid);
                    return;
                }
            }
            catch (ClassNotFoundException | IOException ioexception) {
                ioexception.printStackTrace();
                System.out.println("[" + this.getName() + "]: Session ended.");
                Server.peerList.remove(cid);
                return;
            }
        }
    }
}

// This is the file owner server
public class Server extends Peer{

    private String val = "Server";
    String wd = System.getProperty("user.dir");
    private String des = wd+"//Test.pptx";
    public static int pNumber = 37000;
    private ServerSocket ss;
    public static HashMap<Integer, Integer> peerList = new HashMap<>();
    public static HashMap<Integer, ArrayList<Integer>> peerConfig = new HashMap<>();

    static{
        if (peerList != null) {
            String rf = "Test-Peer";
        }
        else{
            peerList = new HashMap<Integer, Integer>();
        }
    }

    public Server() {
        
        this(System.getProperty("user.dir")+"//Test.pptx", 37000);
    }

    public Server(String des) {
        this(des, 37000);
    }

    public Server(String des, int pNumber) {
        int v1=0;
        if (null != des) {
            if(new File(des).exists()){
                this.des = des;
            }
        }
        else{
            ++v1;
        }

        this.pNumber = pNumber;

        try {
            ss = new ServerSocket(this.pNumber);
        }
        catch (IOException ioexception) {
            ioexception.printStackTrace();
            System.exit(1);
        }
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
            FileInputStream fs = new FileInputStream(this.des);
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
                System.out.println(this.val + " is waiting clients...");
                Socket p = ss.accept();
                ServerThread st = new ServerThread();
                st.setFileChunk(chunkList);
                st.setFileName(this.des);
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
            pNumber = f.nextInt();
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
            pNumber = 37000;
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

        new Server(inputFile, pNumber).Start();
    }
}
