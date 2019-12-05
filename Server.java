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


// This is the file owner server
public class Server extends Peer{

    MultithreadedS ms = new MultithreadedS();

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
        this.divide();
    }

    protected void divide() {
        try {
            File sd = new File("ServerDir");
            int p=0;
            if(sd.exists()) {
                p++;
            }
            else{
                sd.mkdir();
            }
            FileInputStream fis = new FileInputStream(this.des);
            byte[] temp = new byte[this.BUFFER_SIZE];
            int ptr;
            int idx = 0;
            for(;(ptr = fis.read(temp)) != -1;) {
                byte[] bc = Arrays.copyOfRange(temp, 0, ptr);
                chunkList.put(idx, bc);
                System.out.println("Chunk #" + idx + " = " + ptr + "bytes");
                FileOutputStream fos = new FileOutputStream("ServerDir/" + idx, false);
                fos.write(bc);
                fos.flush();
                fos.close();
                temp = new byte[this.BUFFER_SIZE];
                idx++;
            }
            System.out.println("[Server] Total " + idx + " chunks");
        }
        catch (IOException ioexception) {
            ioexception.printStackTrace();
        }
    }

    @Override
    public void Start() {

        try {
            for(;;) {
                System.out.println(this.val + " is waiting clients...");
                Socket socket = ss.accept();
                ServerThread serverThread = new ServerThread();
                serverThread.setFileChunk(chunkList);
                serverThread.setFileName(this.des);
                serverThread.setSocket(socket);
                serverThread.start();
            }
        } catch (IOException ioexception) {
            ioexception.printStackTrace();
        }

    }

    private static void ReadConfig() {
        try {
            Scanner scanner = new Scanner(new FileInputStream("config.txt"));
            // server line
            int sid = scanner.nextInt();
            pNumber = scanner.nextInt();
            for(;scanner.hasNext();) {
                int pid = scanner.nextInt();
                ArrayList<Integer> list = new ArrayList<Integer>();
                list.add(scanner.nextInt());
                list.add(scanner.nextInt());
                list.add(scanner.nextInt());
                peerConfig.put(pid, list);
            }
            scanner.close();
        }
        catch (FileNotFoundException fileNotFoundException) {
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
            

        }
        catch (IOException ioException) {
            ioException.printStackTrace();
        }
        finally {
            try {
                br.close();
                fr.close();
            }
            catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws IOException{

        String wd = System.getProperty("user.dir");
        String fp = wd+"//config.txt";
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
        int var2=0;
        if(args.length <= 0) {
            ++var2;
        }
        else{
            inputFile = args[0];
        }

        new Server(inputFile, pNumber).Start();
    }
}
