package utils;

import nlp.wordnet.WordNetUnpacker;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.jar.JarFile;

/**
 * Allow to use opinion words within a jar.
 */
public class SentimentUnpacker {

    private static final Logger logger = Logger.getLogger(SentimentUnpacker.class);
    static final String ID = "23454325119"; // minimize the chance of interfering  with an existing directory

    /**
     * Extract opinion words from the jar file to tmp dir and return the path.
     *
     * @param jarDir Opinion file name.
     * @return Path to extract file.
     */
    public static File getUnpackedWordNetDir(String jarDir)// throws IOException
    {
        return getUnpackedSentimentDir(SentimentUnpacker.class, jarDir);
    }

    private static File getUnpackedSentimentDir(Class<SentimentUnpacker> clazz, String jarDir) {
        try {
            String codeSource = clazz.getProtectionDomain().getCodeSource().getLocation().getPath();
            logger.debug("WordNetUnpacker.getUnpackedWordNetDir: using code source " + codeSource);
            if (!codeSource.endsWith(".jar")) {
                //			logger.debug("not running from jar, no unpacking necessary");
                try {
                    return new File(SentimentUnpacker.class.getClassLoader().getResource(jarDir).toURI());
                } catch (URISyntaxException e) {
                    throw new IOException(e);
                }
            }
            try (JarFile jarFile = new JarFile(codeSource)) {
                String tempDirString = System.getProperty("java.io.tmpdir");
                if (tempDirString == null) {
                    throw new IOException("java.io.tmpdir not set");
                }
                File tempDir = new File(tempDirString);
                if (!tempDir.exists()) {
                    throw new IOException("temporary directory does not exist");
                }
                if (!tempDir.isDirectory()) {
                    throw new IOException("temporary directory is a file, not a directory ");
                }
                File wordNetDir = new File(tempDirString + '/' + "sentiment" + ID);
                wordNetDir.mkdir();
                logger.debug("unpacking jarfile " + jarFile.getName());
                WordNetUnpacker.copyResourcesToDirectory(jarFile, jarDir, wordNetDir.getAbsolutePath());
                return wordNetDir;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
