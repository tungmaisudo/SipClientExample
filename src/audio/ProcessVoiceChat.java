package audio;

import net.sf.fmj.media.rtp.RTPSessionMgr;
import net.sf.fmj.utility.URLUtils;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import javax.media.*;
import javax.media.control.FormatControl;
import javax.media.control.TrackControl;
import javax.media.format.AudioFormat;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;
import javax.media.rtp.ReceiveStream;
import javax.media.rtp.ReceiveStreamListener;
import javax.media.rtp.SendStream;
import javax.media.rtp.SessionAddress;
import javax.media.rtp.event.NewReceiveStreamEvent;
import javax.media.rtp.event.ReceiveStreamEvent;

/**
 *
 * @author KhangDang
 */
public class ProcessVoiceChat implements ReceiveStreamListener {

    Processor processor;
    DataSource outDataSource;

    RTPSessionMgr voiceSession;
    SendStream sendStream;

    ReceiveStream receiveStream;
    Player player;

    public ProcessVoiceChat(String revIP, int revPort, int localPort) {
        init(revIP, revPort, localPort);
        startMedia();
        send();
        stopMedia();
    }

    public void init(String revIP, int revPort, int localPort) {
        try {
            voiceSession = new RTPSessionMgr();
            voiceSession.addReceiveStreamListener(this);

            SessionAddress localSessionAddress = new SessionAddress(
                    InetAddress.getLocalHost(), localPort);

            InetAddress revInetAddress = InetAddress.getByName(revIP);
            SessionAddress revSessionAddress = new SessionAddress(
                    revInetAddress, revPort);

            voiceSession.initSession(new SessionAddress(), null, 0.25, 0.5);
            voiceSession.startSession(localSessionAddress, localSessionAddress, revSessionAddress, null);

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void startMedia() {

        final String urlStr = URLUtils.createUrlStr(new File("samplemedia/gulp2.wav"));//"file://samplemedia/gulp2.wav";
//        File file = new File("C:\\Users\\Tung\\Desktop\\file_example_WAV_1MG-1575813828.wav");

        // g729,g711a
        Format format =  null;
//        format = new AudioFormat(BonusAudioFormatEncodings.ALAW_RTP);
        format = new AudioFormat(AudioFormat.ULAW_RTP, 8000, 8, 1);
//        format = new AudioFormat(AudioFormat.ULAW_RTP, 8000.0, 8, 1, AudioFormat.LITTLE_ENDIAN, AudioFormat.SIGNED);
//        format = new AudioFormat(BonusAudioFormatEncodings.ALAW_RTP, 8000, 8, 1);
//        format = new AudioFormat(BonusAudioFormatEncodings.SPEEX_RTP, 8000, 8, 1, -1, AudioFormat.SIGNED);
//        format = new AudioFormat(BonusAudioFormatEncodings.ILBC_RTP, 8000.0, 16, 1, AudioFormat.LITTLE_ENDIAN, AudioFormat.SIGNED);

        // Create a processor for this capturedevice & exit if we
        // cannot create it
        try {
            processor = Manager.createProcessor(new MediaLocator(urlStr));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        } catch (NoProcessorException e) {
            e.printStackTrace();
            return;
        }

        // configure the processor
        processor.configure();


        while (processor.getState() != Processor.Configured) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        processor.setContentDescriptor(new ContentDescriptor(ContentDescriptor.RAW_RTP));

        TrackControl track[] = processor.getTrackControls();


        boolean encodingOk = false;

        // Go through the tracks and try to program one of them to
        // output gsm data.



        for (int i = 0; i < track.length; i++) {
            if (!encodingOk && track[i] instanceof FormatControl) {
                if (((FormatControl) track[i]).setFormat(format) == null) {

                    track[i].setEnabled(false);
                } else {
                    encodingOk = true;
                }
            } else {
                // we could not set this track to gsm, so disable it
                track[i].setEnabled(false);
            }
        }

        // At this point, we have determined where we can send out
        // gsm data or not.
        // realize the processor
        if (encodingOk) {
            if (!new net.sf.fmj.ejmf.toolkit.util.StateWaiter(processor).blockingRealize()) {
                System.err.println("Failed to realize");
                return;
            }


            try {
                outDataSource = processor.getDataOutput();
            } catch (NotRealizedError e) {
                e.printStackTrace();
                return;
            }

        }
    }

    public void send() {
        try {
            System.out.println("Send stream");
            sendStream = voiceSession.createSendStream(outDataSource, 0);
            sendStream.start();
            processor.start();

        } catch (Exception e) {
            System.out.println("error : " + e.getMessage());
        }
    }

    public void stopMedia() {
        try {

            player.stop();
            player.deallocate();
            player.close();

            sendStream.stop();

            processor.stop();
            processor.deallocate();
            processor.close();

            voiceSession.closeSession("");
            voiceSession.dispose();

        } catch (Exception e) {
            System.out.println("StopMedia : " + e.getMessage());
        }
    }

    @Override
    public void update(ReceiveStreamEvent rse) {
        try {
            if (rse instanceof NewReceiveStreamEvent) {
                receiveStream = rse.getReceiveStream();
                DataSource myDs = receiveStream.getDataSource();

                player = Manager.createRealizedPlayer(myDs);
                player.start();

            }
        } catch (Exception e) {
            System.out.println("Update : " + e.getMessage());
        }
    }

}
