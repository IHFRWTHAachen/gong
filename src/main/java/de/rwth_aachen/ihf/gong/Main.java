package de.rwth_aachen.ihf.gong;


import java.io.*;
import javax.sound.sampled.*;
import java.lang.*;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;


public final class Main {

    private static String GongFileName = "";
    private static int LowerBound = 0;
    private static int UpperBound = 0;

    private static void printVersion() {
        System.out.println("Gong Version 1.0.4");
        System.out.println("by Ralf Wilke, Korbinian Schraml, powered by IHF RWTH Aachen");
        System.out.println("New Versions at https://github.com/ihfrwthaachen/gong");
        System.out.println();
    }


    private static boolean parseArguments(String[] args) {

        Options opts = new Options();
        opts.addOption("g", "gong", true, "Wave file to be played.");
        opts.addOption("h", "help", false, "Show this help.");
        opts.addOption("v", "version", false, "Show version information.");
        opts.addOption("l", "lower", true, "Lower threshold to activate gong.");
        opts.addOption("u", "upper", true, "Upper threshold to activate gong.");



        CommandLineParser parser = new DefaultParser();
        CommandLine line = null;
        try {
            line = parser.parse(opts, args);
        } catch (ParseException ex) {
//            log.log(Level.SEVERE, "Failed to parse command line.", ex);
            return false;
        }

        if (line.hasOption('h')) {
            HelpFormatter fmt = new HelpFormatter();
            fmt.printHelp("gong", opts);
            System.exit(0);
            return false;
        }

        if (line.hasOption('v')) {
            printVersion();
            return false;
        }

        if (line.hasOption('g')) {
            GongFileName = line.getOptionValue('g');
        } else {
            System.out.println("No Gong file");
        }

        if (line.hasOption('l')) {
            LowerBound = Integer.parseInt(line.getOptionValue('l'));
        } else {
            System.out.println("No lower bound given");
            return false;
        }

        if (line.hasOption('u')) {
            UpperBound = Integer.parseInt(line.getOptionValue('u'));
        } else {
            System.out.println("No upper bound given");
            return false;
        }
       return true;
    }

    public static void main(String[] args) {
        if (!parseArguments(args)) { System.exit(1); }


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


            System.out.println(glmittelwert);

            if (spracheaktiv) {
                if (glmittelwert < LowerBound) {
                    spracheaktiv = false;
                    System.out.println("Mikro ist aus");
                }
            } else {

                if (glmittelwert > UpperBound) {
                    spracheaktiv = true;
                    System.out.println("Mikro ist an");
                    try (Clip clip = AudioSystem.getClip()) {
                        clip.open(AudioSystem.getAudioInputStream(new File(GongFileName)));
                        clip.addLineListener((e) -> {
                            if (e.getType() == LineEvent.Type.STOP) {
                                try {
                                    clip.close();
                                    e.getLine().close();
                                } catch (Exception ex) {
                                    ex.printStackTrace(System.out);
                                }

                            }

                        });
                        clip.start();
                        clip.loop(0);
                    } catch (Exception exc) {
                        exc.printStackTrace(System.out);
                    }
                }
            }
        }
    }
}