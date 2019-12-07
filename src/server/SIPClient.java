package server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;


public class SIPClient {
    private static ArrayList<PacketInfo> sipCandidateClients;
    public static DatagramSocket serverSocket;  //The main socket to listen on
    //For sending and receiving via UDP
    private DatagramPacket receivePacket;

    private boolean isOk = false;


    public SIPClient() throws SocketException, UnknownHostException {
        sipCandidateClients = new ArrayList<PacketInfo>();

        SIPClient.serverSocket = new DatagramSocket(Configuration.sipPort(), Configuration.sipInterface());
    }

    public void sendInvite(PacketInfo packetInfo) {
        try {
            SIPMessages.Register(packetInfo);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendAck(PacketInfo packetInfo) {
        try {
            SIPMessages.Ack(packetInfo);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() throws IOException, InterruptedException {

        while (true) {
            System.out.println("Concurrent clients:" + VoIPWorker.numClients());

            receivePacket = SIPUtil.getPacket();
            String received = new String(receivePacket.getData(), 0, receivePacket.getLength());

            System.out.println("Received:\n" + received);
            PacketInfo packetInfo = SIPUtil.parseUDP(received);
            packetInfo.senderPort = receivePacket.getPort();

            switch (SIPMessages.RequestType(packetInfo.statusLine[0])) {
//        case "INVITE":
//          this.processInvite(packetInfo);
//          break;
                case "OK":
                    if (!isOk) {
                        isOk = true;
                        System.out.println("200 OK received!");

                        packetInfo.senderAddress = "sip.iptel.org";
                        this.sendAck(packetInfo);

//                        PacketInfo client = new PacketInfo();
//                        client.senderAddress = "117.4.244.215";
//                        client.senderRtpPort = "47757";
//
//                        VoIPWorker voipWorker = new VoIPWorker(client);
//                        voipWorker.start();
                    }
                    break;
//        case "CANCEL":
//          System.out.println("CANCEL received!");
//          break;
//        case "BYE":
//          System.out.println("BYE received!");
//          //removeClient(this);
//          SIPMessages.OkForBye(packetInfo);
//          break;
//        case "ACK":
//          this.processAck(packetInfo);
//          break;
            }
        }
    }


    private void processInvite(PacketInfo packetInfo) throws UnknownHostException, IOException, InterruptedException {
        this.addCandidateClient(packetInfo);

        if (packetInfo.receiverUser.equals(Configuration.sipUser())) {
            System.out.println("Trying");
            SIPMessages.Trying(packetInfo);
            Thread.sleep(100);//wait a little for ringing in softphone
            //send ringing
            System.out.println("Ringing");
            SIPMessages.Ringing(packetInfo);
            Thread.sleep(100);//wait a little for ringing in softphone
            //send OK
            System.out.println("OK");
            SIPMessages.Ok(packetInfo);
        } else {
            SIPMessages.NotFound(packetInfo);
        }
    }

    private void processAck(PacketInfo packetInfo) throws UnknownHostException, IOException {
        System.out.println("ACK received! " + packetInfo.receiverUser);
        if ((packetInfo.receiverUser.equals(Configuration.sipUser()))) {
            PacketInfo client = this.getCandidateClient(packetInfo.senderUsername);
            if (client != null) {
                this.removeCandidateClient(client);
                VoIPWorker voipWorker = new VoIPWorker(client);
                voipWorker.start();
            }
        }
        System.out.println("Clients connected now:" + VoIPWorker.numClients());

    }


    private void addCandidateClient(PacketInfo packetInfo) {
        if (this.getCandidateClient(packetInfo.senderAddress) == null) {
            sipCandidateClients.add(packetInfo);
        }
    }

    private void removeCandidateClient(PacketInfo candidateClient) {
        sipCandidateClients.remove(candidateClient);
    }

    private PacketInfo getCandidateClient(String address) {

        for (PacketInfo obj : sipCandidateClients) {
            if (obj.senderUsername.equals(address)) {
                return obj;
            }
        }
        return null;
    }
}
