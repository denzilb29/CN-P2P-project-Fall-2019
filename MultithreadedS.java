/*import java.net.*;
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
}*/
