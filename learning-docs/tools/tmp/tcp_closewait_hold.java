// tcp_closewait_hold.java -- demonstrate CLOSE_WAIT: the client has closed (FIN sent),
// but the server never calls close(), so the connection sticks in CLOSE_WAIT.
// Run: java tcp_closewait_hold.java [connections] [holdMillis]
// While it holds, inspect with: Get-NetTCPConnection -State CloseWait -LocalPort <LISTEN_PORT>
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class tcp_closewait_hold {
    public static void main(String[] args) throws Exception {
        int n = args.length > 0 ? Integer.parseInt(args[0]) : 5;
        int holdMs = args.length > 1 ? Integer.parseInt(args[1]) : 4000;

        try (ServerSocket ss = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"))) {
            System.out.println("LISTEN_PORT=" + ss.getLocalPort());

            // clients: connect then close immediately (active closer side)
            for (int i = 0; i < n; i++) {
                Socket c = new Socket();
                c.connect(new InetSocketAddress("127.0.0.1", ss.getLocalPort()));
                c.close();
            }

            // server: accept but deliberately never close during the hold window
            List<Socket> held = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                held.add(ss.accept());
            }
            System.out.println("HOLDING sockets=" + held.size() + " for_ms=" + holdMs
                    + " (all of them are in CLOSE_WAIT now)");
            Thread.sleep(holdMs);

            for (Socket h : held) {
                h.close();
            }
            System.out.println("RELEASED all sockets, CLOSE_WAIT should disappear");
            Thread.sleep(500);
        }
    }
}
