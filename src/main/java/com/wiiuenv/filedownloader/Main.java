package com.wiiuenv.filedownloader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.UnrecognizedOptionException;

import de.mas.wiiu.jnus.DecryptionService;
import de.mas.wiiu.jnus.NUSTitle;
import de.mas.wiiu.jnus.NUSTitleLoaderRemote;
import de.mas.wiiu.jnus.Settings;
import de.mas.wiiu.jnus.entities.Ticket;
import de.mas.wiiu.jnus.utils.Utils;

public class Main {
    private final static String OPTION_HELP = "help";
    private final static String OPTION_TITLEID = "titleID";
    private final static String OPTION_TITLEKEY = "titlekey";
    private final static String OPTION_FILE = "file";
    private final static String OPTION_VERSION = "version";
    private static final String OPTION_OUT = "out";
    private static final String OPTION_COMMON_KEY = "commonkey";
    private static final String OPTION_OVERWRITE = "overwrite";

    public static void main(String[] args) throws IOException, Exception {
        System.out.println("JNUSLib File Remote Decryptor 0.2");
        System.out.println();
        Options options = getOptions();

        if (args.length == 0) {
            showHelp(options);
            return;
        }

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (MissingArgumentException e) {
            System.out.println(e.getMessage());
            return;
        } catch (UnrecognizedOptionException e) {
            System.out.println(e.getMessage());
            showHelp(options);
            return;
        }

        Long titleID = null;
        String output = null;
        Short version = Settings.LATEST_TMD_VERSION;
        boolean overwrite = false;
        byte[] titlekey = null;
        readKey();

        if (cmd.hasOption(OPTION_HELP)) {
            showHelp(options);
            return;
        }

        if (cmd.hasOption(OPTION_TITLEKEY)) {
            String titlekey_string = cmd.getOptionValue(OPTION_TITLEKEY);
            titlekey = Utils.StringToByteArray(titlekey_string);
            if (titlekey.length != 0x10) {
                titlekey = null;
            } else {
                System.out.println("Titlekey was set to: " + Utils.ByteArrayToString(titlekey));
            }
        }

        if (cmd.hasOption(OPTION_TITLEID)) {
            titleID = Utils.StringToLong(cmd.getOptionValue(OPTION_TITLEID));
        }
        if (titleID == null) {
            System.out.println("Failed to get the titleID");
            showHelp(options);
        }
        if (cmd.hasOption(OPTION_OUT)) {
            output = cmd.getOptionValue(OPTION_OUT);
        }

        if (cmd.hasOption(OPTION_VERSION)) {            
            version = Short.parseShort(cmd.getOptionValue(OPTION_VERSION));
            System.out.println("Using version " + version);
        }

        if (cmd.hasOption(OPTION_COMMON_KEY)) {
            String commonKey = cmd.getOptionValue(OPTION_COMMON_KEY);
            byte[] key = Utils.StringToByteArray(commonKey);
            if (key.length == 0x10) {
                Settings.commonKey = key;
                System.out.println("Commonkey was set to: " + Utils.ByteArrayToString(key));
            }
        }

        if (cmd.hasOption(OPTION_OVERWRITE)) {
            overwrite = true;
        }

        String regex = ".*";

        if (cmd.hasOption(OPTION_FILE)) {
            regex = cmd.getOptionValue(OPTION_FILE);
            System.out.println("Decrypting files matching \"" + regex + "\" for titleID "
                    + String.format("%016X version: %s", titleID, version == null ? "latest" : version.toString()));
        }else {
            showHelp(options);
        }
        decryptFiles(titleID, version, output, regex, overwrite, titlekey);
    }

    private static NUSTitle getTitle(Long titleID, Short version, byte[] titlekey) throws IOException, Exception {
        NUSTitle title = null;

        if (titlekey != null) {
            title = NUSTitleLoaderRemote.loadNUSTitle(titleID, version, Ticket.createTicket(titlekey, titleID));
        } else {
            title = NUSTitleLoaderRemote.loadNUSTitle(titleID, version);
        }

        return title;
    }

    private static void decryptFiles(Long titleID, Short version, String output, String regex, boolean overwrite, byte[] titleKey)
            throws IOException, Exception {
        NUSTitle title = getTitle(titleID, version, titleKey);

        if (title == null) {
            System.err.println("Failed to open title.");
            return;
        }

        String newOutput = output;

        if (newOutput == null) {
            newOutput = String.format("%016X", title.getTMD().getTitleID());
        } else {
            newOutput += File.separator + String.format("%016X", title.getTMD().getTitleID());
        }

        File outputFolder = new File(newOutput);

        System.out.println("To the folder: " + outputFolder.getAbsolutePath());
        title.setSkipExistingFiles(!overwrite);
        DecryptionService decryption = DecryptionService.getInstance(title);

        decryption.decryptFSTEntriesTo(regex, outputFolder.getAbsolutePath());

        System.out.println("Decryption done");
    }

    private static void readKey() throws IOException {
        File file = new File("common.key");
        if (file.isFile()) {
            byte[] key = Files.readAllBytes(file.toPath());
            Settings.commonKey = key;
            System.out.println("Commonkey was set to: " + Utils.ByteArrayToString(key));
        }
    }

    private static Options getOptions() {
        Options options = new Options();
        options.addOption(
                Option.builder(OPTION_TITLEID).argName("titleID").hasArg().desc("TitleID of the title to be downloaded. example: 000500101000400A").build());
        options.addOption(Option.builder(OPTION_OUT).argName("output path").hasArg().desc("The path where the result will be saved").build());
        options.addOption(Option.builder(OPTION_OVERWRITE).desc("Optional. Overwrites existing files").build());
        options.addOption(
                Option.builder(OPTION_VERSION).hasArg().desc("Optional. Version of the title. If no version is provided the latest will be used.").build());
        options.addOption(Option.builder(OPTION_COMMON_KEY).argName("WiiU common key").hasArg()
                .desc("Optional. HexString. Will be used if no \"common.key\" in the folder of this .jar is found").build());
        options.addOption(Option.builder(OPTION_TITLEKEY).argName("Ticket title key").hasArg().desc("Optional. HexString. ").build());
        options.addOption(Option.builder(OPTION_FILE).argName("regular expression").hasArg()
                .desc("Decrypts the files that matches the given regular expression.").build());

        options.addOption(OPTION_HELP, false, "shows this text");

        return options;
    }

    private static void showHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.setWidth(100);
        formatter.printHelp(" ", options);
    }
}
