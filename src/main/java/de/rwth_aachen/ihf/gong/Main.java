package de.rwth_aachen.ihf.gong;

import org.apache.commons.cli.*;

import javax.sound.sampled.*;
import java.io.File;

public final class Main {
	private static String gongFileName = "";
	private static int lowerBound = 0;
	private static int upperBound = 0;

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
		CommandLine line;
		try {
			line = parser.parse(opts, args);
		} catch (ParseException e) {
			System.out.println("ERROR: Failed to parse command-line (" + e.getMessage() + ").");
			return false;
		}

		if (line.hasOption('h')) {
			HelpFormatter fmt = new HelpFormatter();
			fmt.printHelp("gong", opts);
			return false;
		}

		if (line.hasOption('v')) {
			printVersion();
			return false;
		}

		if (line.hasOption('g')) {
			gongFileName = line.getOptionValue('g');
		} else {
			System.out.println("ERROR: No Gong file.");
		}

		if (line.hasOption('l')) {
			lowerBound = Integer.parseInt(line.getOptionValue('l'));
		} else {
			System.out.println("ERROR: No lower bound given.");
			return false;
		}

		if (line.hasOption('u')) {
			upperBound = Integer.parseInt(line.getOptionValue('u'));
		} else {
			System.out.println("ERROR: No upper bound given.");
			return false;
		}

		return true;
	}

	public static void main(String[] args) {
		if (!parseArguments(args)) {
			System.exit(1);
		}

		AudioFormat af48000 = new AudioFormat(44100, 16, 1, true, true);

		TargetDataLine line = null;
		DataLine.Info info = new DataLine.Info(TargetDataLine.class, af48000);
		if (!AudioSystem.isLineSupported(info)) {
			System.out.println("ERROR: Line is not supported.");
			System.exit(1);
		}

		// obtain and open the line
		try {
			line = (TargetDataLine) AudioSystem.getLine(info);
			line.open(af48000);
		} catch (LineUnavailableException e) {
			System.out.println("ERROR: Line is not available (" + e.getMessage() + ").");
			System.exit(1);
		}

		// assume that the TargetDataLine, line, has already been obtained and opened
		int numbytesread;
		byte[] data = new byte[line.getBufferSize() / 5];

		// begin audio capture
		line.start();
		System.out.println("INFO: Audio capture started.");

		final byte numberglm = 10;
		int[] glm = new int[numberglm];
		int counter = 0;

		boolean spracheAktiv = false;
		while (true) {
			// read the next chunk of data from the TargetDataLine
			numbytesread = line.read(data, 0, data.length);

			// save this chunk of data.
			int summe = 0;
			int intdata;
			for (int i = 0; i < numbytesread; i += 2) {
				intdata = (data[i] * 265) + data[(i + 1)];
				summe += Math.abs(intdata);
			}
			summe /= (numbytesread / 2);

			glm[counter] = summe;
			counter = (counter + 1) % numberglm;

			int glMittelwert = 0;
			for (int j = 0; j < numberglm; j++) {
				glMittelwert += glm[j];
			}
			glMittelwert /= numberglm;
			System.out.println("DEBUG: glMittelwert = " + glMittelwert);

			if (spracheAktiv) {
				if (glMittelwert < lowerBound) {
					spracheAktiv = false;
					System.out.println("INFO: Microphone is inactive.");
				}
			} else {
				if (glMittelwert > upperBound) {
					spracheAktiv = true;
					System.out.println("INFO: Microphone is active.");

					try (Clip clip = AudioSystem.getClip()) {
						clip.open(AudioSystem.getAudioInputStream(new File(gongFileName)));
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
					} catch (Exception e) {
						System.out.println("ERROR: Unable to play sound (" + e.getMessage() + ").");
					}
				}
			}
		}
	}
}