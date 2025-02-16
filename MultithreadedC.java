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

class MultithreadedC extends MultithreadedP {
    //test
    protected int pid;
    //hello
    public void setPid(int id) {
        this.pid = id;
    }

    public void run() {
        for (;;) {
            try {
                System.out.println("[" + /*this.pName +*/ "] *******listening******");
                Object packet = this.ois.readObject();
                assert (packet instanceof String);
                String msg = (String) packet;

                System.out.println("[" + /*this.pName +*/ "] received (" + msg + ")");
                int readIndex = -1;

                if(msg.compareTo("LIST") == 0){
                    ArrayList<Integer> list = new ArrayList<Integer>(this.wc.size());
                    for (Integer v : this.wc.keySet()) {
                        list.add(v);
                    }
                    sendMsg(list);
                }
                else if(msg.compareTo("REQUEST") == 0){
                    readIndex = this.ois.readInt();
                    sendMsg(readIndex);
                    sendMsg(this.wc.get(readIndex));
                }
                else if(msg.compareTo("ASK") == 0){
                    readIndex = this.ois.readInt();
                    if (!this.wc.containsKey(readIndex)) {
                        sendMsg(0);
                    }
                    else {
                        sendMsg(1);
                    }
                }
                else if(msg.compareTo("DATA") == 0){
                    readIndex = this.ois.readInt();
                    byte[] piece = (byte[]) this.ois.readObject();
                    int t=0;
                    if (this.wc.containsKey(readIndex)) {
                        ++t;
                    }
                    else{
                        wc.put(readIndex, piece);
                        System.out.println("Received the Chunk no. " + readIndex);
                        storePiece(readIndex, piece);
                    }
                }
                else if(msg.compareTo("CLOSE") == 0){
                    oos.close();
                    ois.close();
                    return;
                }
            }
            catch (ClassNotFoundException | IOException exceptions) {
                exceptions.printStackTrace();
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