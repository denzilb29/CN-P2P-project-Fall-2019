import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.*;

abstract class Peer {

    public final int buff_size = 102400; //100kB
    public final static int peer_total = 5;
    public static String Pname = "";
    public abstract void Begin();
    public static HashMap<Integer, byte[]> List_of_chunks = new HashMap<Integer, byte[]>();
    public static ArrayList<Integer> Index_of_chunks = new ArrayList<Integer>();
    Peer() {
    }
}
