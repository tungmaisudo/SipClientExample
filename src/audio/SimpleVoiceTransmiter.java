package audio;

import net.sf.fmj.media.BonusAudioFormatEncodings;
import net.sf.fmj.media.codec.audio.alaw.Packetizer;
import net.sf.fmj.utility.URLUtils;

import java.io.File;
import java.io.IOException;
import java.util.Vector;

import javax.media.CaptureDeviceInfo;
import javax.media.CaptureDeviceManager;
import javax.media.DataSink;
import javax.media.Format;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.NoProcessorException;
import javax.media.NotRealizedError;
import javax.media.Processor;
import javax.media.control.FormatControl;
import javax.media.control.TrackControl;
import javax.media.format.AudioFormat;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;

public class SimpleVoiceTransmiter {

    private String file;
    private String sipHost;
    private int sipPort;

    public SimpleVoiceTransmiter(String file, String sipHost, int sipPort) {
        this.file = file;
        this.sipHost = sipHost;
        this.sipPort = sipPort;
    }

    public static void main(String[] args) {
        SimpleVoiceTransmiter audioTest = new SimpleVoiceTransmiter("C:\\project\\SipClientExample\\samplemedia\\gulp2.wav", "sip.linphone.org", 7990);
        audioTest.run();
    }

    /**
     *
     */
    public void run() {
//        final String urlStr = URLUtils.createUrlStr(new File("samplemedia/gulp.wav"));//"file://samplemedia/gulp2.wav";
        File file = new File("C:\\project\\SipClientExample\\samplemedia\\gulp2.wav");

        // g729,g711a
        Format format =  null;
        format = new AudioFormat(AudioFormat.G729_RTP, 8000, 8, 1);

        // Create a processor for this capturedevice & exit if we
        // cannot create it
        Processor processor = null;
        try {
            MediaLocator mediaLocator = new MediaLocator(file.toURL());
            processor = Manager.createProcessor(mediaLocator);
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
                // TODO Auto-generated catch block
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

			while (processor.getState() != Processor.Realized)
			{
				try
				{
					Thread.sleep(100);
				} catch (InterruptedException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
            // get the output datasource of the processor and exit
            // if we fail
            DataSource ds = null;

            try {
                ds = processor.getDataOutput();
            } catch (NotRealizedError e) {
                e.printStackTrace();
                return;
            }

            // hand this datasource to manager for creating an RTP
            // datasink our RTP datasink will multicast the audio
            try {
                String url = "rtp://" + this.sipHost + ":" + this.sipPort + "/audio/16";

                MediaLocator m = new MediaLocator(url);

                DataSink d = Manager.createDataSink(ds, m);
                d.open();
                d.start();

                System.out.println("Starting processor");
                processor.start();
                Thread.sleep(3000);
                System.out.println("end");
                d.close();
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }

    }

}
