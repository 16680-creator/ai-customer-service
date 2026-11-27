// tcp_socket_defaults.java -- print JDK Socket defaults, then create a batch of TIME_WAIT entries.
// Run: java tcp_socket_defaults.java [connections]
// ASCII-only on purpose: on a Chinese Windows console the JDK 17 source launcher decodes the
// file with the platform charset, so non-ASCII comments/strings show up as mojibake.
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class tcp_socket_defaults {
    public static void main(String[] args) throws Exception {
        int n = args.length > 0 ? Integer.parseInt(args[0]) : 100;

        try (Socket s = new Socket()) {
            System.out.println("DEFAULTS tcpNoDelay=" + s.getTcpNoDelay()
                    + " keepAlive=" + s.getKeepAlive()
                    + " soTimeout=" + s.getSoTimeout()
                    + " soLinger=" + s.getSoLinger()
                    + " recvBuf=" + s.getReceiveBufferSize()
                    + " sendBuf=" + s.getSendBufferSize());
        }

        try (ServerSocket ss = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"))) {
            System.out.println("SERVER port=" + ss.getLocalPort()
                    + " recvBuf=" + ss.getReceiveBufferSize()
                    + " reuseAddress=" + ss.getReuseAddress());

            // server side: accept, read until EOF (client sent FIN), then close -> passive closer
            Thread acceptor = new Thread(() -> {
                List<Socket> held = new ArrayList<>();
                try {
                    for (int i = 0; i < n; i++) {
                        Socket h = ss.accept();
                        held.add(h);
                        InputStream in = h.getInputStream();
                        while (in.read() != -1) {
                            // clients send no payload; only waiting for EOF
                        }
                        h.close();
                    }
                } catch (Exception ignored) {
                }
            });
            acceptor.start();

            List<Integer> localPorts = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                Socket c = new Socket();
                c.connect(new InetSocketAddress("127.0.0.1", ss.getLocalPort()));
                localPorts.add(c.getLocalPort());
                c.close(); // client closes first -> client is the active closer -> TIME_WAIT
            }
            StringBuilder sb = new StringBuilder();
            for (int p : localPorts) {
                if (sb.length() > 0) {
                    sb.append(',');
                }
                sb.append(p);
            }
            System.out.println("PORTS=" + sb);
            acceptor.join();
            System.out.println("CLOSED count=" + localPorts.size());
        }
    }
}
