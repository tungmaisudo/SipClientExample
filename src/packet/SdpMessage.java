package packet;

import gov.nist.javax.sdp.fields.SDPKeywords;
import utils.Utils;

import javax.sdp.*;
import java.util.Date;
import java.util.Vector;

public class SdpMessage {
    private static String ipv4Local = Utils.getIPLocal();

    public static byte[] createSDPMessage(int rtpLocalPort) throws SdpException {
        SdpFactory sdpFactory = SdpFactory.getInstance();
        SessionDescription sessionDescription = sdpFactory.createSessionDescription();

        Version version = sdpFactory.createVersion(0);
        long ss = sdpFactory.getNtpTime(new Date());
        Origin origin = sdpFactory.createOrigin("-", ss, ss, SDPKeywords.IN, SDPKeywords.IPV4, ipv4Local);
        SessionName sessionName = sdpFactory.createSessionName("-");
        Connection connection = sdpFactory.createConnection(ipv4Local);
        // create t
        TimeDescription timeDescription = sdpFactory.createTimeDescription();
        Vector timeVector = new Vector();
        timeVector.add(timeDescription);
        // create m
        int[] payloadAudio = new int[5];
        payloadAudio[0] = 8;
        payloadAudio[1] = 0;
        payloadAudio[2] = 4;
        payloadAudio[3] = 18;
        payloadAudio[4] = 101;
        MediaDescription mediaDescription = sdpFactory.createMediaDescription("audio", rtpLocalPort, 1, "RTP/AVP", payloadAudio);
        mediaDescription.setAttribute("rtcp", rtpLocalPort + 1 + " IN IP4 " + ipv4Local);
        mediaDescription.setAttribute("rtpmap", "8 PCMA/8000");
        mediaDescription.setAttribute("rtpmap", "0 PCMU/8000");
        mediaDescription.setAttribute("rtpmap", "4 G723/8000");
        mediaDescription.setAttribute("rtpmap", "18 G729/8000");
//        mediaDescription.set("sendrecv");
        mediaDescription.setAttribute("rtpmap", "101 telephone-event/8000");
        mediaDescription.setAttribute("fmtp", "101 0-15");

        Vector vmedia = new Vector();
        vmedia.add(mediaDescription);


        sessionDescription.setVersion(version);
        sessionDescription.setOrigin(origin);
        sessionDescription.setSessionName(sessionName);
        sessionDescription.setConnection(connection);
        sessionDescription.setTimeDescriptions(timeVector);
        sessionDescription.setMediaDescriptions(vmedia);

        return sessionDescription.toString().getBytes();
    }

    public static int getVoicePortSDPMessage(byte[] content) throws SdpException {
        SdpFactory sdpFactory = SdpFactory.getInstance();
        SessionDescription recSdp = sdpFactory.createSessionDescription(new String(content));
        // lấy ra m-line
        Vector recMediaDescriptionVector = recSdp.getMediaDescriptions(false);
        MediaDescription myAudioDescription = (MediaDescription) recMediaDescriptionVector.elementAt(0);
        // lấy ra port trong m-line
        int voicePort = myAudioDescription.getMedia().getMediaPort();

        return voicePort;
    }
}
