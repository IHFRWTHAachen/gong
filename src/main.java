/**
 * Created by wilke on 13.12.2016.
 */

import java.io.*;
import javax.sound.sampled.*;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayOutputStream;

public final class main {

    public static void main(String[] args) {

        AudioFormat af48000 = new AudioFormat(24000, 16, 1, true, true);

        TargetDataLine line = null;
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, af48000); // format is an AudioFormat object
        if (!AudioSystem.isLineSupported(info)) {
            // Handle the error ...

        }
// Obtain and open the line.
        try {
            line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(af48000);
        } catch (LineUnavailableException ex) {
            // Handle the error ...
        }

// Assume that the TargetDataLine, line, has already
// been obtained and opened.
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int numBytesRead;
        byte[] data = new byte[line.getBufferSize() / 5];

// Begin audio capture.
        line.start();
        System.out.println("Test");

        final byte numberglm = 10;
        int[] glm = new int[numberglm];
        int counter = 0;

        boolean spracheaktiv = false;

        while (true) {

            // Read the next chunk of data from the TargetDataLine.

            numBytesRead = line.read(data, 0, data.length);
            // Save this chunk of data.
            int summe = 0;
            int intdata = 0;
            for (int i = 0; i < numBytesRead; i += 2) {

                intdata = (data[i] * 265) + data[(i+1)];

                summe += Math.abs(intdata);
            }
            summe /= (numBytesRead/2);

            glm[counter] = summe;
            counter = (counter + 1) % numberglm;

            int glMittelwert = 0;
            for (int j = 0; j < numberglm ; j++) {
                glMittelwert += glm[j];
            }
            glMittelwert /= numberglm;


//            System.out.println(numBytesRead);
            System.out.println(glMittelwert);

            if (spracheaktiv) {
                if (glMittelwert < 600) {
                    spracheaktiv = false;
                    System.out.println("Mikro ist aus");
                }
            } else {

                if (glMittelwert > 1000) {
                    spracheaktiv = true;
                    System.out.println("Mikro ist an");

                    try
                    {
                        Clip clip = AudioSystem.getClip();
                        clip.open(AudioSystem.getAudioInputStream(new File("audio/gong1.wav")));
                        clip.start();
                    }
                    catch (Exception exc)
                    {
                        exc.printStackTrace(System.out);
                    }
                }
            }

            //out.write(data, 0, numBytesRead);
        }
    }
}