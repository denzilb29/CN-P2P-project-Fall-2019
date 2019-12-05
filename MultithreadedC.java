/*import java.io.BufferedWriter;
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

class MultithreadedC extends SocketThread {
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
}*/