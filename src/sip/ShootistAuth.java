package sip;

import audio.ProcessVoiceChat;
import audio.SimpleVoiceTransmiter;
import audio.Test;
import auth.AccountManagerImpl;
import dto.PacketInfo;
import gov.nist.javax.sip.SipStackExt;
import gov.nist.javax.sip.clientauthutils.AuthenticationHelper;
import packet.SdpMessage;
import packet.SipMessage;
import utils.Utils;

import java.util.Properties;

import javax.sip.ClientTransaction;
import javax.sip.Dialog;
import javax.sip.DialogState;
import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
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
import javax.sip.address.AddressFactory;
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
    private ListeningPoint udpListeningPoint;
    private ClientTransaction inviteTid;
    private Dialog dialog;

    private PacketInfo sender;
    private String receiverName;

    private SipMessage sipMessage;


    public ShootistAuth(PacketInfo packetInfo, String receiverName) {
        this.sender = packetInfo;
        this.receiverName = receiverName;
    }


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

    public void processBye(Request request, ServerTransaction serverTransactionId) {
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
                    Request request = this.sipMessage.createInvite(this.sender, this.receiverName, callIdHeader.getCallId());
                    // Create the client transaction.
                    inviteTid = sipProvider.getNewClientTransaction(request);
                    // send the request out.
                    inviteTid.sendRequest();
                    System.out.println("INVITE sent:\n" + request);
                    dialog = inviteTid.getDialog();
                }

                if (cseq.getMethod().equals(Request.INVITE)) {
                    int port = SdpMessage.getVoicePortSDPMessage((byte[]) response.getContent());
                    Dialog dialog = inviteTid.getDialog();
                    Request ackRequest = dialog.createAck(cseq.getSeqNumber());
                    System.out.println("Sending ACK");
                    dialog.sendAck(ackRequest);
                    sendRtpAudio(port);
                }
                if (cseq.getMethod().equals(Request.CANCEL)) {
                    if (dialog.getState() == DialogState.CONFIRMED) {
                        // oops cancel went in too late. Need to hang up the
                        // dialog.
                        System.out.println("Sending BYE -- cancel went in too late !!");
                        Request byeRequest = dialog.createRequest(Request.BYE);
                        ClientTransaction ct = sipProvider.getNewClientTransaction(byeRequest);
                        dialog.sendRequest(ct);
                    }
                }
            } else if (response.getStatusCode() == Response.PROXY_AUTHENTICATION_REQUIRED
                    || response.getStatusCode() == Response.UNAUTHORIZED) {
                AuthenticationHelper authenticationHelper =
                        ((SipStackExt) sipStack).getAuthenticationHelper(
                                new AccountManagerImpl(this.sender.getUsername(), this.sender.getSipHost(), this.sender.getPassword()), headerFactory);

                inviteTid = authenticationHelper.handleChallenge(response, tid, sipProvider, 5);

                inviteTid.sendRequest();

                SipMessage.invco++;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public void sendRtpAudio(int port) throws Exception {
//        SimpleVoiceTransmiter audioTest = new SimpleVoiceTransmiter("D:\\New folder\\file_example_WAV_1MG.wav", this.sender.getSipHost(), port);
//        audioTest.run();
//        ProcessVoiceChat processVoiceChat = new ProcessVoiceChat("sip.linphone.org", port, 16010);
        Test test = new Test();
        test.run(port);
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

    public void init() {
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
            udpListeningPoint = sipStack.createListeningPoint(Utils.getIPLocal(),
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
            this.sipMessage = new SipMessage(sipProvider, addressFactory, messageFactory, headerFactory, sipStack);

            System.out.println("ShootistAuth Process ");
            Request request = this.sipMessage.createRegister(this.sender);
            // Create the client transaction.
            inviteTid = sipProvider.getNewClientTransaction(request);
            // send the request out.
            inviteTid.sendRequest();
            System.out.println("REGISTER with no Authorization sent:\n" + request);
            dialog = inviteTid.getDialog();

        } catch (Exception e) {
            System.out.println("Creating call CreateInvite()");
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }


    public static void main(String args[]) {
//        PacketInfo sender = new PacketInfo("17999901", "04731747", "101.99.18.210", 5060);
//        String callId = "84394419265";
        PacketInfo sender = new PacketInfo("milete02", "milete02", "sip.linphone.org", 5060);
        String callId = "milete01";
        new ShootistAuth(sender, callId).init();
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