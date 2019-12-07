package jain;

import audio.AudioTest;
import audio.SimpleVoiceTransmiter;
import gov.nist.javax.sip.SipStackExt;
import gov.nist.javax.sip.clientauthutils.AuthenticationHelper;
import jlibrtp.Participant;
import server.SoundSender;
import test.*;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Properties;

import javax.sip.ClientTransaction;
import javax.sip.Dialog;
import javax.sip.DialogState;
import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.InvalidArgumentException;
import javax.sip.ListeningPoint;
import javax.sip.PeerUnavailableException;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipFactory;
import javax.sip.SipListener;
import javax.sip.SipProvider;
import javax.sip.SipStack;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.address.URI;
import javax.sip.header.*;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;

/**
 * This class is a UAC template. Shootist is the guy that shoots and shootme is
 * the guy that gets shot.
 *
 * @author M. Ranganathan
 * @author Kathleen McCallum
 */

public class ShootistAuth implements SipListener {

    private static SipProvider sipProvider;
    private static AddressFactory addressFactory;
    private static MessageFactory messageFactory;
    private static HeaderFactory headerFactory;
    private static SipStack sipStack;
    private ContactHeader contactHeader;
    private ListeningPoint udpListeningPoint;
    private ClientTransaction inviteTid;
    private Dialog dialog;

    private static boolean isRegister = false;
    long invco = 30247l;

    String fromName = "milete02";
    String fromSipAddress = "sip.linphone.org";
    String fromDisplayName = "milete02";

    String toSipAddress = "sip.linephone.org";
    String toUser = "milete01";
    String toDisplayName = "milete01";


    String peerHostPort = "192.168.33.5:5070";
    String transport = "udp";

    String uacHost = "192.168.33.5";
    String uacPort = "5071";


    // voiceClient truyền media từ UAC -> UAS
    VoiceTool voiceClient;
    // voiceServer truyền media từ UAS -> UAC
    VoiceTool voiceServer;
    // sdpOffer : tạo SDP message từ UAC và lưu thông tin
    // trong SDP message nhận được từ UAS.
    private SdpTool sdpOffer;
    // sdpOffer : tạo SDP message từ UAS và lưu thông tin
    // trong SDP message nhận được từ UAC.
    private SdpTool sdpAnswer;


    public void processRequest(RequestEvent requestReceivedEvent) {
        Request request = requestReceivedEvent.getRequest();
        ServerTransaction serverTransactionId = requestReceivedEvent
                .getServerTransaction();

        System.out.println("\n\nRequest " + request.getMethod()
                + " received at " + sipStack.getStackName()
                + " with server transaction id " + serverTransactionId);

        // We are the UAC so the only request we get is the BYE.
        if (request.getMethod().equals(Request.BYE))
            processBye(request, serverTransactionId);
    }

    public void processBye(Request request,
                           ServerTransaction serverTransactionId) {
        try {
            System.out.println("shootist:  got a bye .");
            if (serverTransactionId == null) {
                System.out.println("shootist:  null TID.");
                return;
            }
            Dialog dialog = serverTransactionId.getDialog();
            System.out.println("Dialog State = " + dialog.getState());
            Response response = messageFactory.createResponse(200, request);
            serverTransactionId.sendResponse(response);
            System.out.println("shootist:  Sending OK.");
            System.out.println("Dialog State = " + dialog.getState());

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void processResponse(ResponseEvent responseReceivedEvent) {
        System.out.println("Got a response");
        Response response = (Response) responseReceivedEvent.getResponse();
        ClientTransaction tid = responseReceivedEvent.getClientTransaction();
        CSeqHeader cseq = (CSeqHeader) response.getHeader(CSeqHeader.NAME);

        System.out.println("Response received : Status Code = "
                + response.getStatusCode() + " " + cseq);
        if (tid == null) {
            System.out.println("Stray response -- dropping ");
            return;
        }

        try {

            if (response.getStatusCode() == Response.OK) {

                if (cseq.getMethod().equals(Request.REGISTER)) {
                    CallIdHeader callIdHeader = (CallIdHeader) response.getHeader(CallIdHeader.NAME);
                    Request request = createInvite(callIdHeader.getCallId());
                    // Create the client transaction.
                    inviteTid = sipProvider.getNewClientTransaction(request);
                    // send the request out.
                    inviteTid.sendRequest();
                    System.out.println("INVITE sent:\n" + request);
                    dialog = inviteTid.getDialog();
                }

                if (cseq.getMethod().equals(Request.INVITE)) {
                    // lấy SDP message trong message body của 200 OK
                    String content = new String((byte[]) response.getContent());
                    // lấy ra các thông tin trong SDP message này và lưu trong biến receiverInfo
                    int port = Integer.parseInt((content.split("audio ")[1]).split(" ")[0]);

                    Dialog dialog = inviteTid.getDialog();
                    Request ackRequest = dialog.createAck(cseq.getSeqNumber());
                    System.out.println("Sending ACK");
                    dialog.sendAck(ackRequest);
                    Thread.sleep(2000);
                    sendRtp(port);


                }
                if (cseq.getMethod().equals(Request.CANCEL)) {
                    if (dialog.getState() == DialogState.CONFIRMED) {
                        // oops cancel went in too late. Need to hang up the
                        // dialog.
                        System.out
                                .println("Sending BYE -- cancel went in too late !!");
                        Request byeRequest = dialog.createRequest(Request.BYE);
                        ClientTransaction ct = sipProvider
                                .getNewClientTransaction(byeRequest);
                        dialog.sendRequest(ct);
                    }
                }
            } else if (response.getStatusCode() == Response.PROXY_AUTHENTICATION_REQUIRED
                    || response.getStatusCode() == Response.UNAUTHORIZED) {
                AuthenticationHelper authenticationHelper =
                        ((SipStackExt) sipStack).getAuthenticationHelper(new AccountManagerImpl(), headerFactory);

                inviteTid = authenticationHelper.handleChallenge(response, tid, sipProvider, 5);

                inviteTid.sendRequest();

                invco++;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public void sendRtp(int port) throws Exception {
        SimpleVoiceTransmiter audioTest = new SimpleVoiceTransmiter();
        audioTest.main(port);
        // thực hiện voice chat phía UAC :

//        SoundSenderDemo2 soundSenderDemo = new SoundSenderDemo2(false, 16010);
//        Participant p = new Participant("sip.linphone.org",
//                port,port + 1);
//        soundSenderDemo.filename = "D:\\New folder\\file_example_WAV_1MG.wav";
//        soundSenderDemo.rtpSession.addParticipant(p);
//        soundSenderDemo.star();
//        SoundSender aDemo = new SoundSender(false, "D:\\New folder\\file_example_WAV_1MG.wav");
//        Participant p = new Participant("sip.linphone.org",
//                port, port + 1);
//        System.out.println("Send to " + port);
//        aDemo.rtpSession.addParticipant(p);
//        aDemo.run();

    }

    public void processTimeout(javax.sip.TimeoutEvent timeoutEvent) {

        System.out.println("Transaction Time out");
    }

    public void sendCancel() {
        try {
            System.out.println("Sending cancel");
            Request cancelRequest = inviteTid.createCancel();
            ClientTransaction cancelTid = sipProvider
                    .getNewClientTransaction(cancelRequest);
            cancelTid.sendRequest();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    public Request createInvite(String callId) throws ParseException,
            InvalidArgumentException {

        // create >From Header
        SipURI fromAddress = addressFactory.createSipURI(fromName,
                fromSipAddress);

        Address fromNameAddress = addressFactory.createAddress(fromAddress);
        fromNameAddress.setDisplayName(fromDisplayName);
        FromHeader fromHeader = headerFactory.createFromHeader(fromNameAddress,
                "12345");

        // create To Header
        SipURI toAddress = addressFactory.createSipURI(toUser, toSipAddress);
        Address toNameAddress = addressFactory.createAddress(toAddress);
        toNameAddress.setDisplayName(toDisplayName);
        ToHeader toHeader = headerFactory.createToHeader(toNameAddress, null);

        // create Request URI
        SipURI requestURI = addressFactory.createSipURI(toUser, fromSipAddress);

        // Create ViaHeaders
        ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
        ViaHeader viaHeader = headerFactory.createViaHeader(uacHost,
                sipProvider.getListeningPoint(transport).getPort(), transport,
                null);
        // add via headers
        viaHeaders.add(viaHeader);

        // Create ContentTypeHeader
        ContentTypeHeader contentTypeHeader = headerFactory
                .createContentTypeHeader("application", "sdp");

        // Create a new CallId header
        CallIdHeader callIdHeader;
        callIdHeader = sipProvider.getNewCallId();
        if (callId.trim().length() > 0)
            callIdHeader.setCallId(callId);

        // Create a new Cseq header
        CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(invco,
                Request.INVITE);

        // Create a new MaxForwardsHeader
        MaxForwardsHeader maxForwards = headerFactory
                .createMaxForwardsHeader(70);

        // Create the request.
        Request request = messageFactory.createRequest(requestURI,
                Request.INVITE, callIdHeader, cSeqHeader, fromHeader, toHeader,
                viaHeaders, maxForwards);
        // Create contact headers

//        SipURI contactUrl = addressFactory.createSipURI(fromName, host);
//        contactUrl.setPort(udpListeningPoint.getPort());

        // Create the contact name address.
        SipURI contactURI = addressFactory.createSipURI(fromName, uacHost);
        contactURI.setPort(sipProvider.getListeningPoint(transport).getPort());

        Address contactAddress = addressFactory.createAddress(contactURI);

        // Add the contact address.
        contactAddress.setDisplayName(fromName);

        contactHeader = headerFactory.createContactHeader(contactAddress);
        request.addHeader(contactHeader);

        String sdpData = "v=0\r\n" +
                "o=- 3784718361 3784718361 IN IP4 192.168.33.5\r\n" +
                "s=SJPhone\r\n" +
                "c=IN IP4 192.168.33.5\r\n" +
                "t=0 0\r\n" +
                "m=audio 16010 RTP/AVP 8 0 4 18 101\r\n" +
                "a=rtcp:16011 IN IP4 192.168.33.5\r\n" +
                "a=rtpmap:8 PCMA/8000\r\n" +
                "a=rtpmap:0 PCMU/8000\r\n" +
                "a=rtpmap:4 G723/8000\r\n" +
                "a=rtpmap:18 G729/8000\r\n" +
                "a=sendrecv\r\n" +
                "a=rtpmap:101 telephone-event/8000\r\n" +
                "a=fmtp:101 0-15\r\n";
        // senderInfo_UAC : chứa các thông tin để thực hiện voice chat của UAC
        SdpInfo senderInfo_UAC = new SdpInfo();
        senderInfo_UAC.setIpSender(uacHost);
        senderInfo_UAC.setVoicePort(16010);
        senderInfo_UAC.setVoiceFormat(8);
        // tạo SDP message và lưu các thông tin vào biến senderInfo của sdpOffer
        sdpOffer.createSdp(senderInfo_UAC);
        byte[] contents = sdpData.getBytes();

        request.setContent(contents, contentTypeHeader);


        return request;
    }


    public Request createRegister() throws ParseException,
            InvalidArgumentException {
        isRegister = true;
        // create >From Header
        SipURI fromAddress = addressFactory.createSipURI(fromName,
                fromSipAddress);

        Address fromNameAddress = addressFactory.createAddress(fromAddress);
        FromHeader fromHeader = headerFactory.createFromHeader(fromNameAddress,
                "12345");

        // create To Header
        SipURI toAddress = addressFactory.createSipURI(fromName, toSipAddress);
        Address toNameAddress = addressFactory.createAddress(toAddress);
        ToHeader toHeader = headerFactory.createToHeader(toNameAddress, null);

        // create Request URI
        URI requestURI = addressFactory.createURI("sip:sip.linphone.org");

        // Create ViaHeaders
        ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
        ViaHeader viaHeader = headerFactory.createViaHeader(uacHost,
                sipProvider.getListeningPoint(transport).getPort(), transport,
                null);
        // add via headers
        viaHeaders.add(viaHeader);

        // Create a new CallId header
        CallIdHeader callIdHeader;
        callIdHeader = sipProvider.getNewCallId();

        // Create a new Cseq header
        CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(invco,
                Request.REGISTER);

        // Create a new MaxForwardsHeader
        MaxForwardsHeader maxForwards = headerFactory
                .createMaxForwardsHeader(70);

        // Create the request.
        Request request = messageFactory.createRequest(requestURI,
                Request.REGISTER, callIdHeader, cSeqHeader, fromHeader, toHeader,
                viaHeaders, maxForwards);
        // Create contact headers

        ExpiresHeader expiresHeader = headerFactory.createExpiresHeader(3600);
        request.setExpires(expiresHeader);


        // Create the contact name address.
        SipURI contactURI = addressFactory.createSipURI(fromName, uacHost);
        contactURI.setPort(sipProvider.getListeningPoint(transport).getPort());

        Address contactAddress = addressFactory.createAddress(contactURI);

        contactHeader = headerFactory.createContactHeader(contactAddress);
        request.addHeader(contactHeader);

        String methods = Request.INVITE + ", " + Request.ACK + ", "
                + Request.OPTIONS + ", " + Request.CANCEL + ", " + Request.BYE
                + ", " + Request.INFO + ", " + Request.REFER + ", "
                + Request.MESSAGE + ", " + Request.NOTIFY + ", "
                + Request.SUBSCRIBE;
        AllowHeader allowHeader = headerFactory.createAllowHeader(methods);
        request.addHeader(allowHeader);

        return request;
    }


    public void init() {

        // khởi tạo sdpOffer và sdpAnswer
        sdpOffer = new SdpTool();
        sdpAnswer = new SdpTool();

        // khởi tạo voiceClient và voiceServer
        voiceClient = new VoiceTool();
        voiceServer = new VoiceTool();

        SipFactory sipFactory = null;
        sipStack = null;
        sipFactory = SipFactory.getInstance();
        sipFactory.setPathName("gov.nist");

        Properties properties = new Properties();
        properties.setProperty("javax.sip.STACK_NAME", "myStack");

        try {
            // Create SipStack object
            sipStack = sipFactory.createSipStack(properties);
            System.out.println("createSipStack " + sipStack);
        } catch (PeerUnavailableException e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
        }
        try {
            headerFactory = sipFactory.createHeaderFactory();
            addressFactory = sipFactory.createAddressFactory();
            messageFactory = sipFactory.createMessageFactory();
            udpListeningPoint = sipStack.createListeningPoint("192.168.33.5",
                    5060, "udp");
            sipProvider = sipStack.createSipProvider(udpListeningPoint);
            ShootistAuth listener = this;
            sipProvider.addSipListener(listener);

        } catch (PeerUnavailableException e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
        } catch (Exception e) {
            System.out.println("Creating Listener Points");
            System.out.println(e.getMessage());
            e.printStackTrace();
        }

        try {
            System.out.println("ShootistAuth Process ");
            Request request = this.createRegister();
            // Create the client transaction.
            inviteTid = sipProvider.getNewClientTransaction(request);
            // send the request out.
            inviteTid.sendRequest();
            System.out
                    .println("INVITE with no Authorization sent:\n" + request);
            dialog = inviteTid.getDialog();

        } catch (Exception e) {
            System.out.println("Creating call CreateInvite()");
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }


    public static void main(String args[]) {
        new ShootistAuth().init();
    }

    public void processIOException(IOExceptionEvent exceptionEvent) {
        System.out.println("IOException happened for "
                + exceptionEvent.getHost() + " port = "
                + exceptionEvent.getPort());

    }

    public void processTransactionTerminated(
            TransactionTerminatedEvent transactionTerminatedEvent) {
        System.out.println("Transaction terminated event recieved");
    }

    public void processDialogTerminated(
            DialogTerminatedEvent dialogTerminatedEvent) {
        System.out.println("dialogTerminatedEvent");

    }
}