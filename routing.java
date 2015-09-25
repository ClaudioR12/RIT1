/**
 * Redes Integradas de Telecomunica√ß√µes I
 * MIEEC 2015/2016
 *
 * routing.java
 *
 * Class with routing data and routing logic, and data processing
 *
 * @author  Luis Bernardo
 */
package router;
import java.util.*;
import java.net.*;
import java.io.*;
import javax.swing.*;
import java.awt.event.*;

public class routing {

    public final int MAX_PATH_LEN = 8;
    // Variables
    private char local_name;        // Local router name
    private neighbourList neig;     // List of neighbors
    private int period;             // Period for sending ROUTE packets
    private int min_interval;       // Minimum interval between ROUTE packets
    private int local_TTL;          // TTL for packets ROUTE received
    private router win;             // Main window
    private DatagramSocket ds;      // UDP unicast socket
    private JTable tabela;          // Graphical routing table in win
    private int route_seq;          // ROUTE packet sequence number
    private int data_delay;         // Delay before sending DATA packets
    private int data_seq;           // DATA packet sequence number
    private MulticastDaemon mdaemon;// Multicast communication object
    
    private javax.swing.Timer timer; //INICIALIZA«√O TIMER
    
    
    public final Integer lock = new Integer(0);


    /** Creates a new instance of routing */
    public routing(char local_name, neighbourList neig,
            int period, int min_interval, int data_delay,
            String multi_addr, int multi_port,
            router win, DatagramSocket ds, JTable tabela) {
        this.local_name = local_name;
        this.neig = neig;
        this.period = period;
        this.local_TTL = period + 10;
        this.min_interval = min_interval * 1000;
        this.data_delay = data_delay;
        this.data_seq = 1;
        this.win = win;
        this.ds = ds;
        this.tabela = tabela;
        // Initialize everything
        this.mdaemon = new MulticastDaemon(ds, multi_addr, multi_port, win, this);
        this.route_seq = 1;
        Log2("new routing(local='" + local_name + "', period=" + period
                + ", min_interval=" + min_interval + ")");
    }

    /** Starts routing thread */
    public boolean start() {
        // Start mdaemon thread
        if (!mdaemon.valid()) {
            return false;
        }
        if (!mdaemon.isAlive()) {
            mdaemon.start();
        }
        //Log2("start\n");
        update_routing_table();
        //Log2("start 1\n");
        start_announce_timer();
        //Log2("start ended\n");
        return true;
    }

    /** Signal network changes */
    public void network_changed(boolean network_changed) {
        if (win.is_SendIfChanges() && network_changed) {
            Log("Network changed: not implemented yet\n");
        }
    }

    /** Stops routing thread */
    public void stop() {
        // Stop Data queue timers
        // ...

        // Stop multicast daemon
        mdaemon.stopRunning();
        mdaemon = null;
        // ...
        stop_announce_timer();
        // PASSO 2
        // Clean routing table
        // ???

        update_routing_window();

        local_name = ' ';
        neig = null;
        win = null;
        ds = null;
        tabela = null;
    }

    /** Sends a ROUTE packet with neighbour information */
    public DatagramPacket make_ROUTE_packet(char name, int seq, int ttl,
            Entry[] vec) {
        if (vec == null) {
            Log("ERROR: null vec in send_ROUTE_packet\n");
            return null;
        }
        /*      Log2("make_ROUTE_packet("+name+seq+","+ttl+","+"[");
        for (int i=0;i<vec.length;i++)
        Log2(""+(i>0?",":"")+vec[i].toString());
        Log2("])\n");*/

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(os);
        try {
            dos.writeByte(router.PKT_ROUTE);
            dos.writeChar(name);
            dos.writeInt(seq);
            dos.writeInt(ttl);
            dos.writeInt(vec.length);
            for (int i = 0; i < vec.length; i++) {
                vec[i].writeEntry(dos);
            }
            byte[] buffer = os.toByteArray();
            DatagramPacket dp = new DatagramPacket(buffer, buffer.length);

            return dp;
        } catch (IOException e) {
            Log("Error making ROUTE: " + e + "\n");
            return null;
        }
    }

    /** Sends a ROUTE packet with neighbour information */
    public boolean send_local_ROUTE(boolean use_multicast) {
        Log("send_local_ROUTE(" + (use_multicast ? "multicast" : "unicast") + ")\n");
        Entry[] vec = neig.local_vec(true);
        if (vec == null) { // No local information ??
            win.Log("Internal error in routing.send_local_ROUTE\n");
            return false;
        }

        DatagramPacket dp = make_ROUTE_packet(local_name, route_seq++, local_TTL,
                vec);
        try {
            if (use_multicast) {
                //Log2("send_local_ROUTE(multicast)\n");
                mdaemon.send_packet(dp);
            } else {  // Use unicast
                Log("send_local_ROUTE(unicast) not implemented yet\n");
                // ...
            }
            win.ROUTE_snt++;
            win.ROUTE_loc++;
            return true;
        } catch (IOException e) {
            Log("Error sending ROUTE: " + e + "\n");
            return false;
        }
        

    }

    /** Unmarshalls unicast ROUTE packet e process it */
    public boolean process_ROUTE(char sender, DatagramPacket dp,
            String ip, DataInputStream dis, boolean multicast) {
        //Log("Packet ROUTE not supported yet\n");

        if (sender == local_name) {
            Log2("Packet loopback in process_ROUTE - ignored\n");
            return true;
        }
        try {
            Log("PKT_ROUTE(" + (multicast ? "mcast" : "ucast") + ",");
            String aux;
            aux = "(" + sender + ",";
            int seq = dis.readInt();
            aux += "seq=" + seq + ",";
            int TTL = dis.readInt();
            aux += "TTL=" + TTL + ",";
            int n = dis.readInt();
            aux += "List(" + n + ": ";
            if ((n <= 0) || (n > MAX_PATH_LEN)) {
                Log("\nInvalid list length '" + n + "'\n");
                return false;
            }
            Entry[] data = new Entry[n];
            for (int i = 0; i < n; i++) {
                try {
                    data[i] = new Entry(dis);
                } catch (IOException e) {
                    Log("\nERROR - Invalid vector Entry: " + e.getMessage() + "\n");
                    return false;
                }
                aux += (i == 0 ? "" : " ; ") + data[i].toString();
            }
            Log(aux + ")\n");

            // ... place here the code to handle the ROUTE packet ...

            Log("ROUTE packet("+(multicast?"multicast":"unicast")+") not processed yet ...");
            return true;
        } catch (IOException e) {
            Log("\nERROR - ROUTE packet too short\n");
            return false;
        }
    }

    /** Unmarshalls multicast ROUTE packet e process it */
    public boolean process_multicast_ROUTE(char sender, DatagramPacket dp,
            String ip, DataInputStream dis) {
        //Log("Packet ROUTE not supported yet\n");
        if (!win.jCheckBoxMulticast.isSelected()) {
            return true;
        }
        if (sender == local_name) {
            // Packet loopback - ignore
            return true;
        }
        synchronized (lock) {
            Log2("multicast ");
            return process_ROUTE(sender, dp, ip, dis, true);
        }
    }

    /** Recalculate routing table */
    private synchronized void update_routing_table() {
        Log("update_routing_table not implemented yet\n");
        // Run dijkstra
        // ...
        win.Dijkstra_cnt++;

        // Echo routing table 
        update_routing_window();
    }

    /** Print routing table into the main window */
    public void update_routing_window() {
        // Example code for a HashMap<String, RouteEntry> tab; routing table
        Iterator<RouteEntry> iter= null;

        // Initialize
        //if (tab!=null)
        //    iter= tab.values().iterator();

        // update window
        for (int i= 0; i<tabela.getRowCount(); i++) {
            if (/*(tab != null) && iter.hasNext()*/false) {
                RouteEntry next= iter.next();
//                Log2("("+tab[i].dest+" : "+tab[i].prox+" : "+tab[i].dist+" : "+tab[i].path+")");
                tabela.setValueAt(""+next.dest,i,0);
                tabela.setValueAt(""+next.prox,i,1);
                tabela.setValueAt(""+next.dist,i,2);
            } else {
                tabela.setValueAt("",i,0);
                tabela.setValueAt("",i,1);
                tabela.setValueAt("",i,2);
            }
        }

        // Example code for a RouteEntry[] tab; routing table
/*
        for (int i= 0; i<tabela.getRowCount(); i++) {
            if ((tab != null) && (i < tab.length)) {
                Log2("("+tab[i].dest+" : "+tab[i].prox+" : "+tab[i].dist+")");
                tabela.setValueAt(""+tab[i].dest,i,0);
                tabela.setValueAt(""+tab[i].prox,i,1);
                tabela.setValueAt(""+tab[i].dist,i,2);
            } else {
                tabela.setValueAt("",i,0);
                tabela.setValueAt("",i,1);
                tabela.setValueAt("",i,2);
            }
        } */
    }
   
    
    
    /** Launches timer responsible for sending periodic distance packets to
        neighbours */
    private void start_announce_timer() {
       /** Arranca o relÛgio que envia periodicamente o pacote ROUTE */
        int delay=10;
        timer = new javax.swing.Timer(delay, new ActionListener(){
            public void actionPerformed(ActionEvent evt){
                
                send_local_ROUTE(true);
                //update_routing_table();
            }
        });
    }

    private void stop_announce_timer() {
        timer.stop();
    }



    /***************************************************************************
     *              DATA HANDLING
     */

    /** returns next hop to reach destination. Returns ' ' if not found. */
    public char next_Hop(char dest) {
        Log("Function next_Hop not implemented yet\n");
        return ' ';     //
    }

    /** sends a DATA packet; uses routing table and neighbor information */
    public void send_data_packet(char dest, DatagramPacket dp) {
        if (win.is_local_name(dest)) {
            // Send to local node
            try {
                dp.setAddress(InetAddress.getLocalHost());
                dp.setPort(ds.getLocalPort());
                ds.send(dp);
                win.DATA_snt++;
            } catch (UnknownHostException e) {
                Log("Error sending packet to himself: " + e + "\n");
            } catch (IOException e) {
                Log("Error sending packet to himself: " + e + "\n");
            }

        } else { // Send to neighbour router
            char prox = next_Hop(dest);
            if (prox == ' ') {
                Log("No route to destination: packet discarded\n");
                return;
            } else {
                // Lookup neighbour
                neighbour pt = neig.locate_neig(prox);
                if (pt == null) {
                    Log("Invalid neighbour (" + prox
                            + ") in routing table: packet discarder\n");
                    return;
                }
                try {
                    pt.send_packet(ds, dp);
                    win.DATA_snt++;
                } catch (IOException e) {
                    Log("Error sending DATA packet: " + e + "\n");
                }
            }
        }
    }

    /** prepares a data packet; adds local_name to path */
    public DatagramPacket make_data_packet(char sender, int seq, char dest,
            String msg, String path) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(os);
        try {
            dos.writeByte(router.PKT_DATA);
            dos.writeChar(sender);
            dos.writeInt(seq);
            dos.writeChar(dest);
            dos.writeShort(msg.length());
            dos.writeBytes(msg);
            dos.writeByte(path.length() + 1);
            dos.writeBytes(path + win.local_name());
        } catch (IOException e) {
            Log("Error encoding data packet: " + e + "\n");
            return null;
        }
        byte[] buffer = os.toByteArray();
        return new DatagramPacket(buffer, buffer.length);
    }

    /** prepares a data packet; adds local_name to path */
    public void send_data_packet(char sender, int seq, char dest,
            String msg, String path) {
        if (!Character.isUpperCase(sender)) {
            Log("Invalid sender '" + sender + "'\n");
            return;
        }
        if (!Character.isUpperCase(dest)) {
            Log("Invalid destination '" + dest + "'\n");
            return;
        }
        if (win.is_SlowData()) {
            win.Log("Slow data not implemented yet in fuction send_data_packet\n");
            // ...
        } else {
            DatagramPacket dp = make_data_packet(sender, seq, dest, msg, path);
            if (dp != null) {
                send_data_packet(dest, dp);
            }
        }
    }

    /** prepares a data packet; adds local_name to path */
    public void send_new_data_packet(char sender, char dest,
            String msg, String path) {
        send_data_packet(sender, this.data_seq++, dest, msg, path);
    }

    /** unmarshals DATA packet e process it */
    public boolean process_DATA(char sender, DatagramPacket dp,
            String ip, DataInputStream dis) {
        try {
            Log("PKT_DATA");
            if (!Character.isUpperCase(sender)) {
                Log("Invalid sender '" + sender + "'\n");
                return false;
            }
            // Read seq
            int seq = dis.readInt();
            // Read Dest
            char dest = dis.readChar();
            // Read message
            int len_msg = dis.readShort();
            if (len_msg > 255) {
                Log(": message too long (" + len_msg + ">255)\n");
                return false;
            }
            byte[] sbuf1 = new byte[len_msg];
            int n = dis.read(sbuf1, 0, len_msg);
            if (n != len_msg) {
                Log(": Invalid message length\n");
                return false;
            }
            String msg = new String(sbuf1, 0, n);
            // Read path
            int len_path = dis.readByte();
            if (len_path > router.MAX_PATH_LEN) {
                Log(": path length too long (" + len_msg + ">" + router.MAX_PATH_LEN
                        + ")\n");
                return false;
            }
            byte[] sbuf2 = new byte[len_path];
            n = dis.read(sbuf2, 0, len_path);
            if (n != len_path) {
                Log(": Invalid path length\n");
                return false;
            }
            String path = new String(sbuf2, 0, n);
            Log(" (" + sender + " ("+seq+ ")-" + dest + "):'" + msg + "':Path='" + path + win.local_name() + "'\n");
            // Test routing table
            if (win.is_local_name(dest)) {
                // Arrived at destination
                Log("DATA packet reached destination\n");
                return true;
            } else {
                char prox = next_Hop(dest);
                if (prox == ' ') {
                    Log("No route to destination: packet discarded\n");
                    return false;
                } else {
                    // Send packet to next hop
                    send_data_packet(sender, seq, dest, msg, path);
                    return true;
                }
            }
        } catch (IOException e) {
            Log(" Error decoding data packet: " + e + "\n");
        }
        return false;
    }

    private void Log(String s) {
        win.Log(s);
    }

    private void Log2(String s) {
        //win.Log(s);  // For detailed debug purposes
    }
}
