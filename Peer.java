import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.*;


abstract class SocketThread extends Thread {

    protected Socket socket;
    protected ObjectOutputStream oStream;
    protected ObjectInputStream iStream;

    public String peerName = this.getName();

    protected HashMap<Integer, byte[]> workingChunk;

    protected String fileName = "Dir/Test.pptx";

    public void setFileChunk(HashMap<Integer, byte[]> fileChunk)
    {
        this.workingChunk = fileChunk;
    }

    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
        System.out.println("[" + peerName + "] get connected from " + socket.getPort());
        try {
            oStream = new ObjectOutputStream(this.socket.getOutputStream());
            iStream = new ObjectInputStream(this.socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void send(Object message) throws IOException {
        oStream.writeObject(message);
        oStream.flush();
        oStream.reset();
    }

    public void send(int message) throws IOException {
        oStream.writeInt(message);
        oStream.flush();
        oStream.reset();
    }

    public abstract void run();

}

abstract class Peer {

    public final int BUFFER_SIZE = 102400; //100kB

    public final static int TOTAL_PEERS = 5;

    public static String peerName = "";

    public abstract void Start();

    public static HashMap<Integer, byte[]> chunkList = new HashMap<Integer, byte[]>();

    public static ArrayList<Integer> chunkIndex = new ArrayList<Integer>();

    Peer() {

    }

}
