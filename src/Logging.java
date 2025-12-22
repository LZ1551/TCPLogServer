import java.io.FileWriter;
import java.io.IOException;

//denna klass är för att spara loggar
public class Logging {

    private String logFilePath;

    //konstruktor där man sätter in fil namn
    private static final Object fileLock = new Object();

    public Logging(String logFilePath) {
        this.logFilePath = logFilePath;
    }

    //lägger till meddelande till text filen
    public void addLog(String message) {
        if (message != null && !message.isEmpty()) {
            writeToFile(message);
        }
    }

    //sparar meddelandet i filen
    private void writeToFile(String text) {
        synchronized (fileLock) {
            try (FileWriter fw = new FileWriter(logFilePath, true)) {
                fw.write(text + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
