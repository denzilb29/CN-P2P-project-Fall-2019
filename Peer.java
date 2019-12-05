import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.*;

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
