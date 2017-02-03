# gong
This program implements the IHF gong.

## Usage
```
java -jar gong-1.1-SNAPSHOT.jar -h
```

usage: `gong`
* `-b`, `--burstthreshold <arg>`   Threshold for single bursts.
* `-f`, `--bufferFraction <arg>`   Fraction of the audio input buffer to be read each cycle. Default: 5
* `-g`, `--gong <arg>`             Wave file to be played.
* `-h`, `--help`                   Show this help.
* `-l`, `--lower <arg>`            Lower threshold to activate gong.
* `-ls`,` --list-sounddevices`     Show list of all sound devices.
* `-n`, `--numberGlm <arg>`        Number of values for calculating the moving average. Default: 10
* `-s`, `--sounddevice <arg>`      Use the given sound device.
* `-u`, `--upper <arg>`            Upper threshold to activate gong.
* `-v`, `--version`                Show version information.