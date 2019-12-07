package audio;

import javax.media.*;
import javax.media.control.TrackControl;
import javax.media.format.AudioFormat;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;
import java.io.File;
import java.io.IOException;

public class AudioTest {

    public void run(int port) throws Exception {
        File file = new File("D:\\New folder\\file_example_WAV_1MG.wav");
        // bắt media stream từ soundcard
        MediaLocator locator = new MediaLocator(file.toURL());

        DataSource dataSource = Manager.createDataSource(locator);

        ContentDescriptor outputFile = new ContentDescriptor(ContentDescriptor.RAW_RTP);

        // kiểu định dạng audio
        AudioFormat[] aFormat = new AudioFormat[1];
        aFormat[0] = new AudioFormat(AudioFormat.G729_RTP);

        // tạo đối tượng Processor
        ProcessorModel processorModel = new ProcessorModel(dataSource, aFormat, outputFile);
//        ProcessorModel processorModel = new ProcessorModel(dataSource);
        Processor processor = Manager.createRealizedProcessor(processorModel);

//        TrackControl[] trackControl = processor.getTrackControls();
//        trackControl[0].setFormat()

        // tạo DataSource đầu ra
        DataSource outDataSource = processor.getDataOutput();

        MediaLocator outLocator = new MediaLocator("rtp://91.121.209.194:" + port + "/audio/1");
        System.out.println("outLocator");
        DataSink dataSink = Manager.createDataSink(outDataSource, outLocator);

        dataSink.open();

        dataSink.start();
        System.out.println("dataSink.start()");
        processor.start();
        System.out.println("processor.start();");

        Thread.sleep(30000);

        dataSink.stop();
        processor.stop();
        processor.close();
    }

}
