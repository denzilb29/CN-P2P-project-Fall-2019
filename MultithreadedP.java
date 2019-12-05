import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.*;

abstract class MultithreadedP extends Thread {

    protected Socket s;
    protected ObjectOutputStream oos;
    protected ObjectInputStream ois;
    protected String fName = "Dir/Test.pptx";
    protected HashMap<Integer, byte[]> wc;
    public String pName = this.getName();
    

    public abstract void run();

    public void sendMsg(int msg) throws IOException {
        oos.writeInt(msg);
        oos.flush();
        oos.reset();
    }

    public void ChunkSetter(HashMap<Integer, byte[]> fileChunk) {
        this.wc = fileChunk;
    }

    public void sendMsg(Object msg) throws IOException {
        oos.writeObject(msg);
        oos.flush();
        oos.reset();
    }

    public void setS(Socket s) {
        this.s = s;
        System.out.println("[" + /*this.pName +*/ "] is connected from " + s.getPort());
        try {
            oos = new ObjectOutputStream(this.s.getOutputStream());
            ois = new ObjectInputStream(this.s.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setfName(String fName) {
        this.fName = fName;
    }


}