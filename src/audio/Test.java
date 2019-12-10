package audio;

import jlibrtp.Participant;
import server.SoundSender;
import utils.Utils;

public class Test {
    public void run(int port) {
        SoundSender aDemo = new SoundSender(false, "C:\\project\\SipClientExample\\samplemedia\\file_example_WAV_1MG_alaw.wav");
        Participant p = new Participant("101.99.18.210", port, port + 1);
        aDemo.rtpSession.addParticipant(p);
        aDemo.start();
    }
}
