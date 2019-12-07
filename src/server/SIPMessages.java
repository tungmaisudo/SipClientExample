package server;

import java.io.IOException;
import java.net.UnknownHostException;


public class SIPMessages {
    static Integer sequence = 2;

    public static String RequestType(String line) {
        String type;
        if (line.matches("INVITE sip:.*@.*")) {
            type = "INVITE";
        } else if (line.matches("SIP/2.0 200 OK")) {
            type = "OK";
        } else if (line.matches("^BYE sip:.* SIP/2.0$")) {
            type = "BYE";
        } else if (line.matches("^ACK sip:.* SIP/2.0$")) {
            type = "ACK";
        } else if (line.contains("CANCEL")) {
            type = "CANCEL";
        } else {
            type = "OTHER";
        }

        return type;
    }

    public static String getSDP(PacketInfo packetInfo) throws UnknownHostException {
        System.out.println("====>" + packetInfo.senderRtpPort);
        System.out.println("====>" + Configuration.sipInterfaceStr());

        String sdp_message = "v=0\r\n" +
                "o=- 3784718361 3784718361 IN IP4 192.168.33.5\r\n" +
                "s=SJPhone\r\n" +
                "c=IN IP4 192.168.33.5\r\n" +
                "t=0 0\r\n" +
                "m=audio 16100 RTP/AVP 8 0 4 18 101\r\n" +
                "a=rtcp:16101 IN IP4 192.168.33.5\r\n" +
                "a=rtpmap:8 PCMA/8000\r\n" +
                "a=rtpmap:0 PCMU/8000\r\n" +
                "a=rtpmap:4 G723/8000\r\n" +
                "a=rtpmap:18 G729/8000\r\n" +
                "a=sendrecv\\rn" +
                "a=rtpmap:101 telephone-event/8000\r\n" +
                "a=fmtp:101 0-15\r\n";
        return sdp_message;
    }

    public static void Trying(PacketInfo packetInfo) throws UnknownHostException, IOException {
        String message = "SIP/2.0 100 Trying\r\n"
                + "Via: SIP/2.0/UDP " + packetInfo.senderAddress + ";"
                + "rport=" + Configuration.sipPort() + ";"
                + "received=" + packetInfo.senderAddress + ";"
                + "branch=" + packetInfo.branch + "\r\n"
                + "Content-Length: 0\r\n"
                + "Contact: <sip:" + Configuration.sipAddress() + ">\r\n"
                + "Call-ID: " + packetInfo.callId + "\r\n"
                + "CSeq: 1 INVITE\r\n"
                + "From: \"" + packetInfo.senderUsername + "\"<sip:" + packetInfo.senderAddress + ">;tag=" + packetInfo.tag + "\r\n"
                + "To: \"" + Configuration.sipUser() + "\"<sip:" + Configuration.sipFullAddress() + ">;"
                + "tag=" + Configuration.tag() + "\r\n\r\n";

        SIPUtil.SendPacket(message, packetInfo.senderAddress, packetInfo.senderPort);

    }

    public static void Invite(PacketInfo packetInfo) throws IOException {
        String sdp_message = SIPMessages.getSDP(packetInfo);

        String message = "INVITE sip:milete01@sip.linphone.org:5060 SIP/2.0\r\n" +
                "Via: SIP/2.0/UDP 192.168.33.5:5070;rport;branch=z9hG4bKPj9459aa487d404f4fbb0849ab394fe2e7\r\n" +
                "Max-Forwards: 70\r\n" +
                "From: sip:milete02@sip.linphone.org;tag=7bf0cadfb2014869a215c0a0b5982746\r\n" +
                "To: sip:milete01@sip.linphone.org\r\n" +
                "Contact: <sip:milete02@192.168.33.5:5070>\r\n" +
                "Call-ID: 57ca3766857241ddbd9456ddd7fff60f\r\n" +
                "CSeq: 23742 INVITE\r\n" +
                "Allow: INFO, PRACK, SUBSCRIBE, NOTIFY, REFER, INVITE, ACK, BYE, CANCEL, UPDATE\r\n" +
                "Content-Type: application/sdp\r\n" +
                "Content-Length:   325\r\n"
                + "\r\n"
                + sdp_message;

        SIPUtil.SendPacket(message, packetInfo.senderAddress, packetInfo.senderPort);
    }

    public static void Ringing(PacketInfo packetInfo) throws UnknownHostException, IOException {

        String message = "SIP/2.0 180 Ringing\r\n"
                + "Via: SIP/2.0/UDP " + packetInfo.senderAddress + ";"
                + "rport=" + Configuration.sipPort() + ";"
                + "received=" + packetInfo.senderAddress + ";"
                + "branch=" + packetInfo.branch + "\r\n"
                + "Content-Length: 0\r\n"
                + "Contact: <sip:" + Configuration.sipAddress() + ">\r\n"
                + "Call-ID: " + packetInfo.callId + "\r\n"
                + "CSeq: 1 INVITE\r\n"
                + "From: \"" + packetInfo.senderUsername + "\"<sip:" + packetInfo.senderAddress + ">;tag=" + packetInfo.tag + "\r\n"
                + "To: \"" + Configuration.sipUser() + "\"<sip:" + Configuration.sipFullAddress() + ">;"
                + "tag=" + Configuration.tag() + "\r\n\r\n";

        SIPUtil.SendPacket(message, packetInfo.senderAddress, packetInfo.senderPort);
    }

    public static void Ok(PacketInfo packetInfo) throws UnknownHostException, IOException {

        String sdp_message = SIPMessages.getSDP(packetInfo);

        String message = "SIP/2.0 200 OK\r\n"
                + "Via: SIP/2.0/UDP " + packetInfo.senderAddress + ";"
                + "rport=" + Configuration.sipPort() + ";received=" + packetInfo.senderAddress + ";"
                + "branch=" + packetInfo.branch + "\r\n"
                + "Content-Length: " + sdp_message.length() + "\r\n"
                + "Contact: <sip:" + Configuration.sipAddress() + ">\r\n"
                + "Call-ID: " + packetInfo.callId + "\r\n"
                + "Content-Type: application/sdp\r\n"
                + "CSeq: 1 INVITE\r\n"
                + "From: \"" + packetInfo.senderUsername + "\"<sip:" + packetInfo.senderAddress + ">;tag=" + packetInfo.tag + "\r\n"
                + "To: \"" + Configuration.sipUser() + "\"<sip:" + Configuration.sipFullAddress() + ">;"
                + "tag=" + Configuration.tag() + "\r\n\r\n"
                + sdp_message;

        SIPUtil.SendPacket(message, packetInfo.senderAddress, packetInfo.senderPort);
    }

    public static void OkForBye(PacketInfo packetInfo) throws UnknownHostException, IOException {

        String message = "SIP/2.0 200 OK\r\n"
                + "Via: SIP/2.0/UDP " + packetInfo.senderAddress + ";\r\n"
                + "branch=" + packetInfo.branch + "\r\n"
                + "Call-ID: " + packetInfo.callId + "\r\n"
                + "CSeq: " + packetInfo.cSeq + " BYE\r\n"
                + "From: \"" + packetInfo.senderUsername + "\"<sip:" + packetInfo.senderAddress + ">;tag=" + packetInfo.tag + "\r\n"
                + "To: \"" + Configuration.sipUser() + "\"<sip:" + Configuration.sipFullAddress() + ">;"
                + "tag=" + Configuration.tag() + "\r\n\r\n";

        SIPUtil.SendPacket(message, packetInfo.senderAddress, packetInfo.senderPort);
    }

    public static void NotFound(PacketInfo packetInfo) throws UnknownHostException, IOException {
        String message = "SIP/2.0 404 Not Found\r\n"
                + "Via: SIP/2.0/UDP " + packetInfo.senderAddress + ";"
                + "rport=" + Configuration.sipPort() + ";received=" + packetInfo.senderAddress + ";"
                + "branch=" + packetInfo.branch + "\r\n"
                + "Content-Length: 0\r\n"
                + "Contact: <sip:" + Configuration.sipAddress() + ">\r\n"
                + "Call-ID: " + packetInfo.callId + "\r\n"
                + "CSeq: 1 INVITE\r\n"
                + "From: \"" + packetInfo.senderUsername + "\"<sip:" + packetInfo.senderAddress + ">;tag=" + packetInfo.tag + "\r\n"
                + "To: \"" + Configuration.sipUser() + "\"<sip:" + Configuration.sipFullAddress() + ">;"
                + "tag=" + Configuration.tag() + "\r\n\r\n";

        SIPUtil.SendPacket(message, packetInfo.senderAddress, packetInfo.senderPort);
    }

    public static void Bye(PacketInfo packetInfo) throws UnknownHostException, IOException {

        String message = "BYE sip:" + packetInfo.senderAddress + " SIP/2.0\r\n"
                + "Via: SIP/2.0/UDP " + packetInfo.sipAddress + ";"
                + "rport;branch=" + packetInfo.branch + "\r\n"
                + "Content-Length: 0\r\n" + //nothing to send additionally
                "Call-ID: " + packetInfo.callId + "\r\n"
                + "CSeq: " + sequence + " BYE\r\n" + //It is the first message sent
                "From: \"" + Configuration.sipUser() + "\"<sip:" + Configuration.sipAddress() + ">;"
                + "tag=" + Configuration.tag() + "\r\n"
                + "Max-Forwards: 70\r\n"
                + "To: <sip:" + packetInfo.senderAddress + ">;tag=" + packetInfo.tag + "\r\n"
                + "User-Agent: SJphone/1.60.299a/L (SJ Labs)\r\n\r\n";

        SIPUtil.SendPacket(message, packetInfo.senderAddress, packetInfo.senderPort);
        System.out.println("Sending Bye to " + packetInfo.senderAddress + ":" + packetInfo.senderPort);
        //removeClient(this);
    }

    public static void Ack(PacketInfo packetInfo) throws IOException {
        String message = "ACK sip:84394419265@101.99.18.210:5060 SIP/2.0\r\n" +
                "Via: SIP/2.0/UDP 192.168.8.215:5070;rport;branch=z9hG4bKPjd65a61f36e8644348686d0ee279e4a67\r\n" +
                "Max-Forwards: 70\r\n" +
                "From: sip:17999901@101.99.18.210;tag=" + packetInfo.tagFrom + "\r\n" +
                "To: sip:84394419265@101.99.18.210;tag=" + packetInfo.tagTo + "\r\n" +
                "Call-ID: " + packetInfo.callId + "\r\n" +
                "CSeq: " + packetInfo.cSeq + " ACK\r\n" +
                "Content-Length:  0\r\n";

        SIPUtil.SendPacket(message, packetInfo.senderAddress, packetInfo.senderPort);
    }

    public static void Register(PacketInfo packetInfo) throws IOException {
        String message = "REGISTER sip:sip.linphone.org:5060 SIP/2.0\r\n" +
                "Via: SIP/2.0/UDP 192.168.33.5:5070;rport;branch=z9hG4bKPj020d358d1c8a40b0878ac16a65216f44\r\n" +
                "Max-Forwards: 70\r\n" +
                "From: <sip:milete02@sip.linphone.org>;tag=eb666f13b2d044b58bc5a300745e4a5e\r\n" +
                "To: <sip:milete02@sip.linphone.org>\r\n" +
                "Call-ID: 72f1e730685844fab9c0ff8deef176e0\r\n" +
                "CSeq: 8863 REGISTER\r\n" +
                "Allow: INFO, PRACK, SUBSCRIBE, NOTIFY, REFER, INVITE, ACK, BYE, CANCEL, UPDATE\r\n" +
                "Contact: <sip:milete02@192.168.33.5:5070>\r\n" +
                "Expires: 3600\r\n" +
//                "Authorization:  Digest realm=\"sip.linphone.org\", nonce=\"qMmV4QAAAABdRkQPAACZDrwOecQAAAAA\", algorithm=MD5, opaque=\"+GNywA==\", username=\"milete02\",  uri=\"sip:sip.linphone.org\", response=\"435745ec9497f2c549b8dc98d47407a4\", cnonce\r\n" +
                "Content-Length:  0\r\n";

        SIPUtil.SendPacket(message, packetInfo.senderAddress, packetInfo.senderPort);
    }

}
