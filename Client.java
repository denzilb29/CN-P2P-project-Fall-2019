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


public class Client extends Peer implements Runnable {

    MultithreadedC mc = new MultithreadedC();

    public static int sp = 40000;
    private int dPo = -1;
    private int dPe = -1;
    private int uPo = -1;
    private int uPe = -1;
    public ServerSocket css;
    static int pid = -1;
    public static HashMap<Integer, Integer> pl = new HashMap<Integer, Integer>();
    public static String mfn = "merge.dat";
    public static int p = -2;

    public Client() {
        this(9000);
    }
    public Client(int sp)
    {
        this.sp = sp;
    }

    public static void printMsg(ObjectOutputStream stream, String msg) throws IOException {
        stream.writeObject(msg);
        stream.flush();
        stream.reset();
    }

    public static void printMsg(ObjectOutputStream stream, int value) throws IOException {
        stream.writeInt(value);
        stream.flush();
    }

    public static void storeD(ObjectOutputStream stream, byte[] payload) throws IOException {
        stream.writeObject(payload);
        stream.flush();
        stream.reset();
    }

    public boolean checkPiece() {

        for(int i: Index_of_chunks) {
            if(List_of_chunks.containsKey(i)){
                continue;
            }
            else{
                return false;
            }
        }
        
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
            while(i < Index_of_chunks.size()){
                storeF(i, List_of_chunks.get(Index_of_chunks.get(i)));
                fos.write(List_of_chunks.get(Index_of_chunks.get(i)));
                ++i;
            }
            fos.flush();
            fos.close();
            System.out.println("[" + /*this.pName +*/ "] has finished the operation");
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }

        return true;
    }

    public void Begin() {
        try {
            
            Socket s = new Socket("localhost", sp);
            ObjectOutputStream os = new ObjectOutputStream(s.getOutputStream());
            printMsg(os, "REGISTER");
            ObjectInputStream is = new ObjectInputStream(s.getInputStream());
            pid = is.readInt();
            p = is.readInt();
            Pname = "PEER" + pid;
            System.out.println(pid);
            
            File pd = new File(Pname + "Dir");
            if(pd.exists()) {
                System.out.println("The Directory already exists.");
            }
            else{
                pd.mkdir();
            }
           
            printMsg(os, "LIST");
            Index_of_chunks = (ArrayList<Integer>) is.readObject();
            
            int begin = (int)(1.0 * Index_of_chunks.size() / peer_total * (pid % peer_total));
            int end = (int)(1.0 * Index_of_chunks.size() / peer_total * ((pid  % peer_total) + 1));
            int i = begin;
            while(i < end){
                printMsg(os, "REQUEST");
                printMsg(os, Index_of_chunks.get(i));
                int temp = is.readInt();
                byte[] piece = (byte[]) is.readObject();
                List_of_chunks.put(temp, piece);
                System.out.println("Peer has received Chunk #" + Index_of_chunks.get(i) + " from the server");
                storeF(temp, piece);
                ++i;
            }
            makeSumF(0);
            Random r = new Random();
            
            printMsg(os, "NAME");
            String fp = (String) is.readObject();
            String get_base = new File(fp).getName();
            String get_ext = get_base.substring(get_base.lastIndexOf('.') + 1);
            String fr = get_base.substring(0, get_base.lastIndexOf('.'));
            mfn = fr + "_by_peer_" + pid + "." + get_ext;
            System.out.println("The final file is " + mfn);
            
            printMsg(os, "PEER");
            printMsg(os, pid);
            pl = (HashMap<Integer, Integer>) is.readObject();

            System.out.println("[" + Pname + "] waiting for neighbors from server:");
            dPe = (int) is.readObject();
            uPe = (int) is.readObject();
            dPo = pl.containsKey(dPe) ? pl.get(dPe) : 0;
            uPo = pl.containsKey(uPe) ? pl.get(uPe) : 0;
            Thread.sleep(1000);
            while (this.dPo <= 0 || this.uPo <= 0){
                printMsg(os, "PEER");
                printMsg(os, pid);
                pl = (HashMap<Integer, Integer>) is.readObject();

                System.out.println("[" + Pname + "] waiting for neighbors from server:");
                dPe = (int) is.readObject();
                uPe = (int) is.readObject();
                dPo = pl.containsKey(dPe) ? pl.get(dPe) : 0;
                uPo = pl.containsKey(uPe) ? pl.get(uPe) : 0;
                Thread.sleep(1000);
            }
            System.out.println("[" + Pname + "] Uploading to " + uPe + ":" + uPo);
            System.out.println("[" + Pname + "] Downloading from " + dPe + ":" + dPo);

            (new Thread() {
                @Override
                public void run() {
                    try {
                        System.out.println("==================");
                        Thread.sleep(10000);
                        System.out.println("Establishing upload connection");
                        Socket us = new Socket("localhost", uPo);
                        ObjectOutputStream ous = new ObjectOutputStream(us.getOutputStream());
                        System.out.println("Establishing download connection");
                        Socket dsk = new Socket("localhost", dPo);
                        ObjectOutputStream ods = new ObjectOutputStream(dsk.getOutputStream());
                        ObjectInputStream ids = new ObjectInputStream(dsk.getInputStream());
                        System.out.println("Connection Success!!!");
                        for (;!checkPiece();) {
                            System.out.println("Received list from peers");
                            printMsg(ods, "LIST");
                            ArrayList<Integer> list = (ArrayList<Integer>) ids.readObject();
                            int i=0;
                            while(i < list.size()){
                                int q = list.get(i);
                                if(!Client.List_of_chunks.containsKey(q)) {
                                    System.out.print(q + "=> NEW\t");
                                }
                                else {
                                    System.out.print(q + "=>" + q + "\t");
                                }
                                ++i;
                            }
                            System.out.println();
                            for (i = 0; i < Client.Index_of_chunks.size(); i++) {
                                int temp = Client.Index_of_chunks.get(i);
                                if (!Client.List_of_chunks.containsKey(temp)) {
                                    System.out.println("[" + Client.Pname + "] Asking peer" + dPe + " for Chunk no." + temp);
                                    printMsg(ods, "ASK");
                                    printMsg(ods, temp);
                                    if (ids.readInt() != 1) { 
                                        System.out.println("[" + Client.Pname + "] peer" + dPe + " doesn't have Chunk no." + temp);
                                    } 
                                    else {
                                        printMsg(ods, "REQUEST");
                                        printMsg(ods, temp);
                                        int temp2 = ids.readInt();
                                        byte[] piece = (byte[]) ids.readObject();
                                        Client.List_of_chunks.put(temp2, piece);
                                        System.out.println("Received Chunk no" + Index_of_chunks.get(i) + " from peer " + dPe);
                                    }
                                }
                                else{
                                    continue;
                                }
                            }
                            System.out.println("[" + Client.Pname + "] Completed operation");
                            System.out.println("Start sending chunk list");
                            for (Integer ii : Client.Index_of_chunks) {
                                int temp = ii;
                                if (Client.List_of_chunks.containsKey(temp)) {
                                    System.out.print(temp + " ");
                                    printMsg(ous, "DATA");
                                    printMsg(ous, temp);
                                    storeD(ous, Client.List_of_chunks.get(temp));
                                }
                                else{
                                    continue;
                                }

                            }
                            System.out.println();
                            Thread.sleep(1200);
                        }
                    } catch (IOException | ClassNotFoundException | InterruptedException exceptions) {
                        exceptions.printStackTrace();
                    }

                }
            }).start();

            for(; p < 0;){
                Thread.sleep(550);
            }

            css = new ServerSocket(this.p);

            for (;;) {
                MultithreadedC ld = new MultithreadedC();
                Socket sckt = null;
                try {
                    System.out.println("Peer is listening at port no. " + css.getLocalPort());
                    sckt = css.accept();
                    ld.setS(sckt);
                    ld.setPid(Client.pid);
                    ld.ChunkSetter(List_of_chunks);
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
            FileOutputStream fos = new FileOutputStream(Pname + "Dir/" + x, false);
            fos.write(chunk);
            fos.flush();
            fos.close();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    private void makeSumF(int c) {
            try {
                FileOutputStream fos = new FileOutputStream(Pname + "Dir/summary.txt", false);
                StringBuilder builder = new StringBuilder();
                int i=0;
                while(i < Client.Index_of_chunks.size()){
                    int temp = Client.Index_of_chunks.get(i);
                    ++i;
                    if (!Client.List_of_chunks.containsKey(temp)) {
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
        Begin();
    }

    private static int parseCFile() {
        try {
            Scanner scanner = new Scanner(new FileInputStream("config.txt"));
            // server line
            int sid = scanner.nextInt();
            p = scanner.nextInt();
            scanner.close();
            return p;

        } catch (FileNotFoundException fileNotFoundException) {
            System.out.println("Config file Incomplete");

        }
        return p = 40000;
    }

    public static void main(String[] args) throws IOException {
        String str = System.getProperty("user.dir");
        String fp = str+"//config.txt";
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

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

        File file = new File("config.txt");
        Scanner scanner = new Scanner(file);
        int lineNum = 0;
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            lineNum++;
            if(line.contains(fport)) {
                System.out.println("Port already in use!!");
                System.exit(0);
            }
        }

        appendUsingBufferedWriter(fp,appendText,1);

        System.out.println("Do you want to begin? 1 for yes, 0 for no\n");
        String bp = reader.readLine();
        int temp = Integer.parseInt(bp);

        if(temp == 1){
            int sp = parseCFile();
            if(args.length > 0) {
                 sp = Integer.parseInt(args[0]);
            }
            new Client(sp).Begin();
        }
        else {
            System.exit(0);
        }

    }
}
