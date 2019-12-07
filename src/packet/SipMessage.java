package packet;

import dto.PacketInfo;
import utils.Utils;

import javax.sip.SipProvider;
import javax.sip.SipStack;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.address.URI;
import javax.sip.header.*;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import java.util.ArrayList;

public class SipMessage {

    public static final String TRANSPORT = "udp";
    private static final int EXPIRES_TIME = 3600;
    public static final int MAX_FORWARDS = 70;
    public static long invco = 30247l; //TODO: save to db
    public static int RTP_PORT_LOCAL = 16010;

    private static String ipv4Local = Utils.getIPLocal();

    private SipProvider sipProvider;
    private AddressFactory addressFactory;
    private MessageFactory messageFactory;
    private HeaderFactory headerFactory;
    private SipStack sipStack;

    public SipMessage(SipProvider sipProvider, AddressFactory addressFactory, MessageFactory messageFactory, HeaderFactory headerFactory, SipStack sipStack) {
        this.sipProvider = sipProvider;
        this.addressFactory = addressFactory;
        this.messageFactory = messageFactory;
        this.headerFactory = headerFactory;
        this.sipStack = sipStack;
    }

    public Request createRegister(PacketInfo sender) throws Exception {
        // create >From Header
        SipURI fromAddress = this.addressFactory.createSipURI(sender.getUsername(), sender.getSipHost());

        Address fromNameAddress = this.addressFactory.createAddress(fromAddress);
        FromHeader fromHeader = this.headerFactory.createFromHeader(fromNameAddress, Utils.generateString());

        // create To Header
        SipURI toAddress = this.addressFactory.createSipURI(sender.getUsername(), sender.getSipHost());
        Address toNameAddress = this.addressFactory.createAddress(toAddress);
        ToHeader toHeader = this.headerFactory.createToHeader(toNameAddress, null);

        // create Request URI
        URI requestURI = this.addressFactory.createURI("sip:" + sender.getSipHost());

        // Create ViaHeaders
        ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
        ViaHeader viaHeader = this.headerFactory.createViaHeader(ipv4Local,
                this.sipProvider.getListeningPoint(TRANSPORT).getPort(), TRANSPORT,
                null);
        // add via headers
        viaHeaders.add(viaHeader);

        // Create a new CallId header
        CallIdHeader callIdHeader;
        callIdHeader = this.sipProvider.getNewCallId();

        // Create a new Cseq header
        CSeqHeader cSeqHeader = this.headerFactory.createCSeqHeader(invco, Request.REGISTER);

        // Create a new MaxForwardsHeader
        MaxForwardsHeader maxForwards = this.headerFactory.createMaxForwardsHeader(MAX_FORWARDS);

        // Create the request.
        Request request = this.messageFactory.createRequest(requestURI,
                Request.REGISTER, callIdHeader, cSeqHeader, fromHeader, toHeader,
                viaHeaders, maxForwards);

        // Create contact headers
        ExpiresHeader expiresHeader = this.headerFactory.createExpiresHeader(EXPIRES_TIME);
        request.setExpires(expiresHeader);


        // Create the contact name address.
        SipURI contactURI = this.addressFactory.createSipURI(sender.getUsername(), ipv4Local);
        contactURI.setPort(this.sipProvider.getListeningPoint(TRANSPORT).getPort());

        Address contactAddress = this.addressFactory.createAddress(contactURI);

        ContactHeader contactHeader = this.headerFactory.createContactHeader(contactAddress);
        request.addHeader(contactHeader);

        String methods = Request.INVITE + ", " + Request.ACK + ", "
                + Request.OPTIONS + ", " + Request.CANCEL + ", " + Request.BYE
                + ", " + Request.INFO + ", " + Request.REFER + ", "
                + Request.MESSAGE + ", " + Request.NOTIFY + ", "
                + Request.SUBSCRIBE;
        AllowHeader allowHeader = this.headerFactory.createAllowHeader(methods);
        request.addHeader(allowHeader);

        return request;
    }

    public Request createInvite(PacketInfo sender, String receiverName, String callId) throws Exception {
        // create >From Header
        SipURI fromAddress = addressFactory.createSipURI(sender.getUsername(), sender.getSipHost());

        Address fromNameAddress = addressFactory.createAddress(fromAddress);
        FromHeader fromHeader = headerFactory.createFromHeader(fromNameAddress, "1234");

        // create To Header
        SipURI toAddress = addressFactory.createSipURI(receiverName, sender.getSipHost());
        Address toNameAddress = addressFactory.createAddress(toAddress);
        ToHeader toHeader = headerFactory.createToHeader(toNameAddress, null);

        // create Request URI
        SipURI requestURI = addressFactory.createSipURI(receiverName, sender.getSipHost());

        // Create ViaHeaders
        ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
        ViaHeader viaHeader = headerFactory.createViaHeader(ipv4Local,
                sipProvider.getListeningPoint(TRANSPORT).getPort(), TRANSPORT,
                null);
        // add via headers
        viaHeaders.add(viaHeader);

        // Create ContentTypeHeader
        ContentTypeHeader contentTypeHeader = headerFactory.createContentTypeHeader("application", "sdp");

        // Create a new CallId header
        CallIdHeader callIdHeader;
        callIdHeader = sipProvider.getNewCallId();
        if (callId.trim().length() > 0)
            callIdHeader.setCallId(callId);

        // Create a new Cseq header
        CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(invco, Request.INVITE);

        // Create a new MaxForwardsHeader
        MaxForwardsHeader maxForwards = headerFactory.createMaxForwardsHeader(MAX_FORWARDS);

        // Create the request.
        Request request = messageFactory.createRequest(requestURI,
                Request.INVITE, callIdHeader, cSeqHeader, fromHeader, toHeader,
                viaHeaders, maxForwards);

        // Create the contact name address.
        SipURI contactURI = addressFactory.createSipURI(sender.getUsername(), ipv4Local);
        contactURI.setPort(sipProvider.getListeningPoint(TRANSPORT).getPort());

        Address contactAddress = addressFactory.createAddress(contactURI);

        ContactHeader contactHeader = headerFactory.createContactHeader(contactAddress);
        request.addHeader(contactHeader);

//        String sdpData = "v=0\r\n" +
//                "o=- 3784718361 3784718361 IN IP4 192.168.33.5\r\n" +
//                "s=SJPhone\r\n" +
//                "c=IN IP4 192.168.33.5\r\n" +
//                "t=0 0\r\n" +
//                "m=audio 16010 RTP/AVP 8 0 4 18 101\r\n" +
//                "a=rtcp:16011 IN IP4 192.168.33.5\r\n" +
//                "a=rtpmap:8 PCMA/8000\r\n" +
//                "a=rtpmap:0 PCMU/8000\r\n" +
//                "a=rtpmap:4 G723/8000\r\n" +
//                "a=rtpmap:18 G729/8000\r\n" +
//                "a=sendrecv\r\n" +
//                "a=rtpmap:101 telephone-event/8000\r\n" +
//                "a=fmtp:101 0-15\r\n";
        // senderInfo_UAC : chứa các thông tin để thực hiện voice chat của UAC
        byte[] contents = SdpMessage.createSDPMessage(RTP_PORT_LOCAL);

        request.setContent(contents, contentTypeHeader);

        return request;
    }


}
