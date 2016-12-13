package de.rwth_aachen.ihf.gong;


import java.io.*;
import javax.sound.sampled.*;
import java.lang.*;

public final class Main {

    public static void main(String[] args) {


        AudioFormat af48000 = new AudioFormat(44100, 16, 1, true, true);

        TargetDataLine line = null;
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, af48000); // format is an audioformat object
        if (!AudioSystem.isLineSupported(info)) {
            // handle the error ...

        }
// obtain and open the line.
        try {
            line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(af48000);
        } catch (LineUnavailableException ex) {
            // handle the error ...
        }

// assume that the TargetDataLine, line, has already
// been obtained and opened.
        int numbytesread;
        byte[] data = new byte[line.getBufferSize() / 5];

// begin audio capture.
        line.start();
        System.out.println("test");

        final byte numberglm = 10;
        int[] glm = new int[numberglm];
        int counter = 0;

        boolean spracheaktiv = false;

        while (true) {

            // read the next chunk of data from the targetdataline.

            numbytesread = line.read(data, 0, data.length);
            // save this chunk of data.
            int summe = 0;
            int intdata = 0;
            for (int i = 0; i < numbytesread; i += 2) {

                intdata = (data[i] * 265) + data[(i+1)];

                summe += Math.abs(intdata);
            }
            summe /= (numbytesread/2);

            glm[counter] = summe;
            counter = (counter + 1) % numberglm;

            int glmittelwert = 0;
            for (int j = 0; j < numberglm ; j++) {
                glmittelwert += glm[j];
            }
            glmittelwert /= numberglm;


//            system.out.println(numbytesread);
            System.out.println(glmittelwert);

            if (spracheaktiv) {
                if (glmittelwert < 600) {
                    spracheaktiv = false;
                    System.out.println("mikro ist aus");
                }
            } else {

                if (glmittelwert > 1000) {
                    spracheaktiv = true;
                    System.out.println("mikro ist an");
                    try {
                        Class cls = Class.forName("de.rwth_aachen.ihf.gong.Main");
                        ClassLoader classLoader = cls.getClassLoader();

                        try {
                            Clip clip = AudioSystem.getClip();
                            File Gongfile = new File(classLoader.getResource("gong1.wav").getFile());
                            clip.open(AudioSystem.getAudioInputStream(Gongfile));
                            clip.start();
                        } catch (Exception exc) {
                            exc.printStackTrace(System.out);
                        }
                    } catch (ClassNotFoundException e) {
                        System.out.println(e.toString());
                    }
                }
            }
        }
    }
}