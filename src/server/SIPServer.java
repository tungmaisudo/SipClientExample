package server;

//fix imports

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.ParseException;

public class SIPServer {

    public static void main(String[] args) throws UnknownHostException, SocketException, IOException, InterruptedException {

        try {
            // parse the command line arguments
            Configuration.sipUser("tungtest2");
            Configuration.sipInterface("192.168.33.5");
            Configuration.sipPort("5070");
        } catch (Exception exp) {
            // oops, something went wrong
            System.err.println("Parsing failed.  Reason: " + exp.getMessage());
        }

        System.out.println(Configuration.sipFullAddress());

        SIPClient sipClient = new SIPClient();

        PacketInfo packetInfo = new PacketInfo();
        packetInfo.senderAddress = "sip.linphone.org";
        packetInfo.senderPort = 5060;

//        VoIPWorker voipWorker = new VoIPWorker(packetInfo);
//        voipWorker.start();

        sipClient.sendInvite(packetInfo);

        sipClient.start();




//    System.out.println("Sip Server " + Configuration.sipUser()
//    		+ " listening to :" + Configuration.sipInterface() + "- on port :" + Configuration.sipPort());}

    }

}
