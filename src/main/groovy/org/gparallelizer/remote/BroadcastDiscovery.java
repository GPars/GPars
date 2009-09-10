package org.gparallelizer.remote;

import java.io.*;
import java.net.*;
import java.util.UUID;

public class BroadcastDiscovery {
    private static InetAddress GROUP;
    private static final int PORT = 4239;
    private static final int MAGIC = 0x23982391;

    static {
        try {
            GROUP = InetAddress.getByName("230.0.0.239");
        } catch (UnknownHostException e) {
            GROUP = null;
        }
    }

    public BroadcastDiscovery (final UUID uid, InetSocketAddress address) {
        try {
            final MulticastSocket socket = new MulticastSocket(PORT);
            InetAddress group = GROUP;
            socket.joinGroup(group);

            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            final DataOutputStream stream = new DataOutputStream(out);

            stream.writeLong(MAGIC);
            stream.writeLong(uid.getMostSignificantBits());
            stream.writeLong(uid.getLeastSignificantBits());
            stream.writeInt(address.getPort());
            byte[] addrBytes = address.getAddress().getAddress();
            stream.writeInt(addrBytes.length);
            stream.write(addrBytes);
            stream.close();

            final byte[] bytes = out.toByteArray();

            new Thread () {
                @Override
                public void run() {
                    while(true) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                        }

                        try {
                            DatagramPacket packet = new DatagramPacket(bytes, bytes.length, GROUP, PORT);
                            socket.send(packet);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }.start ();

            new Thread () {
                @Override
                public void run() {
                    byte buf [] = new byte [3*8+3*4];
                    byte addrBuf4 [] = new byte [4];
                    byte addrBuf6 [] = new byte [6];
                    while(true) {
                        DatagramPacket packet = new DatagramPacket(buf, buf.length);
                        try {
                            socket.receive(packet);
                            DataInputStream in = new DataInputStream(new ByteArrayInputStream(buf));
                            if (in.readLong() == MAGIC) {
                                UUID uuid = new UUID(in.readLong(), in.readLong());
                                int port = in.readInt();
                                int addrLen = in.readInt();
                                if (addrLen == 4) {
                                    in.read(addrBuf4);
                                    onDiscovery (uuid, new InetSocketAddress(InetAddress.getByAddress(addrBuf4), port));
                                }
                                else {
                                    in.read(addrBuf6);
                                    onDiscovery (uuid, new InetSocketAddress(InetAddress.getByAddress(addrBuf6), port));
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }.start ();
        }
        catch (Throwable t) {
            t.printStackTrace();
        }
    }

    protected void onDiscovery(UUID uuid, SocketAddress address) {
    }
}
