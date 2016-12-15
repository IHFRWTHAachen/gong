package de.rwth_aachen.ihf.gong;

import org.apache.commons.cli.*;

import javax.sound.sampled.*;
import java.io.File;

public final class Main {
	private static String gongFileName = "";
	private static int lowerBound = 0;
	private static int upperBound = 0;
	private static Mixer.Info soundDevice = null;
	private static int knackschwelle = 0;

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
		opts.addOption("ls", "list-sounddevices", false, "Show list of all sound devices.");
		opts.addOption("s", "sounddevice", true, "Use the given sound device.");
		opts.addOption("k", "knackschwelle", true, "Threshold for single bursts.");

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

		if (line.hasOption("ls")) {
			Mixer.Info[] soundDevices = AudioSystem.getMixerInfo();
			for (Mixer.Info device : soundDevices) {
				System.out.println(device.getName());
			}
			return false;
		}

		if (line.hasOption('g')) {
			gongFileName = line.getOptionValue('g');
		} else {
			System.out.println("ERROR: No sound file given.");
			return false;
		}

		if (line.hasOption('l')) {
			lowerBound = Integer.parseInt(line.getOptionValue('l'));
		} else {
			System.out.println("ERROR: No lower bound given.");
			return false;
		}

		if (line.hasOption('k')) {
			knackschwelle = Integer.parseInt(line.getOptionValue('k'));
		} else {
			System.out.println("ERROR: No knackschwelle given.");
			return false;
		}

		if (line.hasOption('u')) {
			upperBound = Integer.parseInt(line.getOptionValue('u'));
		} else {
			System.out.println("ERROR: No upper bound given.");
			return false;
		}

		if (line.hasOption('s')) {
			String soundDeviceName = line.getOptionValue('s');

			Mixer.Info[] soundDevices = AudioSystem.getMixerInfo();
			for (Mixer.Info device : soundDevices) {
				if (device.getName().equalsIgnoreCase(soundDeviceName)) {
					soundDevice = device;
					break;
				}
			}

			if (soundDevice == null) {
				System.out.println("ERROR: Sound device not found.");
			}
		} else {
			System.out.println("ERROR: No sound device given.");
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

		// assume that the TargetDataLine (line) has already been obtained and opened
		int numBytesRead;
		byte[] data = new byte[line.getBufferSize() / 5];

		// begin audio capture
		line.start();
		System.out.println("INFO: Audio capture started.");

		final byte numberGlm = 10;
		int[] glm = new int[numberGlm];
		int counter = 0;

		boolean voiceActive = false;
		while (true) {
			// read the next chunk of data from the TargetDataLine
			numBytesRead = line.read(data, 0, data.length);

			// save this chunk of data
			int sum = 0;
			int intData;
			for (int i = 0; i < numBytesRead; i += 2) {
				intData = (data[i] * 265) + data[(i + 1)];
				sum += (Math.pow(Math.abs(intData), 2)) / 100;
			}
			sum /= (numBytesRead / 2);

			if (sum > knackschwelle) {
				System.out.println("DEBUG: Knacken unterdrueckt, Wert: " + sum);
				continue;
			}

			glm[counter] = sum;
			counter = (counter + 1) % numberGlm;

			int glAverage = 0;
			for (int j = 0; j < numberGlm; j++) {
				glAverage += glm[j];
			}
			glAverage /= numberGlm;
			System.out.println("DEBUG: glAverage = " + glAverage);

			if (voiceActive) {
				if (glAverage < lowerBound) {
					voiceActive = false;
					System.out.println("INFO: Microphone is inactive.");
				}
			} else {
				if (glAverage > upperBound) {
					voiceActive = true;
					System.out.println("INFO: Microphone is active.");

					try (Clip clip = AudioSystem.getClip(soundDevice)) {
						clip.open(AudioSystem.getAudioInputStream(new File(gongFileName)));
						clip.addLineListener((e) -> {
							if (e.getType() == LineEvent.Type.STOP) {
								try {
									clip.close();
									e.getLine().close();
								} catch (Exception e2) {
									System.out.println("ERROR: Unable to stop playback (" + e2.getMessage() + ").");
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
