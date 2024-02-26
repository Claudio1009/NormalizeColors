package work;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PipedInputStream;
import java.nio.ByteBuffer;

// Clasa WriteImage este responsabilă pentru recepționarea și scrierea imaginii procesate.
public class WriteImage extends Thread implements InterfaceFunction {
    // Streamul de intrare pentru a primi datele imaginii procesate.
    private PipedInputStream input;
    // Variabilă pentru monitorizarea timpului de scriere al imaginii.
    private long timeWrite;
    // Calea către fișierul unde va fi salvată imaginea procesată.
    private String outputPath;

    // Constructorul clasei WriteImage.
    public WriteImage(PipedInputStream input, String outputPath) {
        this.input = input;
        this.outputPath = outputPath;
    }
    static int mesaj;
    static {
    	System.out.println("Incepem scrierea in fisier.");
    }

    // Metoda run care este executată atunci când thread-ul este pornit.
    @Override
    public void run() {
        try {
        	timeWrite = System.currentTimeMillis();
            // Inițializează o imagine combinată pentru a reasambla segmentele primite.
            BufferedImage combinedImage = null;

            // Procesul de citire a fiecărui segment din Pipe și adăugarea acestuia la imaginea combinată.
            for (int i = 0; i < 4; i++) {
                byte[] segmentData = readSegmentFromPipe();
                BufferedImage segment = ImageIO.read(new ByteArrayInputStream(segmentData));

                // Inițializează imaginea combinată cu dimensiunile adecvate.
                if (i == 0) {
                    combinedImage = new BufferedImage(segment.getWidth(), segment.getHeight() * 4, BufferedImage.TYPE_INT_RGB);
                }

                // Verifică dacă segmentul și imaginea combinată sunt valide și adaugă segmentul la imaginea combinată.
                if (segment != null && combinedImage != null) {
                    combinedImage.getGraphics().drawImage(segment, 0, i * segment.getHeight(), null);
                    System.out.println("Segmentul " + (i + 1) + " a fost recepționat și adăugat.");
                    Thread.sleep(1000);
                }
            }

            // Verifică dacă imaginea combinată a fost creată cu succes și o scrie în fișier.
            if (combinedImage != null) {
                File outputFile = new File(outputPath);
                ImageIO.write(combinedImage, "bmp", outputFile);
                System.out.println("Imaginea a fost scrisă în: " + outputPath);
            }

            // Calculează timpul total necesar pentru scrierea imaginii.
            timeWrite = System.currentTimeMillis() - timeWrite;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
            // Închide streamul de intrare.
            try {
                input.close();
            } catch (IOException e) {
              e.printStackTrace();
            }
}
}

/**
 * Citeste un segment de imagine din Pipe. Această metodă se ocupă de citirea
 * dimensiunii segmentului și apoi a datelor efective ale segmentului.
 * 
 * returneaza un array de bytes care reprezintă datele segmentului de imagine.
 * throws IOException Dacă apare o eroare în timpul citirii din pipe.
 */
private byte[] readSegmentFromPipe() throws IOException {
    // Citeste mai întâi dimensiunea segmentului.
    byte[] sizeInfo = new byte[4];
    if (input.read(sizeInfo) != 4) throw new IOException("Nu s-au putut citi informațiile despre dimensiunea segmentului.");
    int segmentSize = ByteBuffer.wrap(sizeInfo).getInt();

    // Citeste datele segmentului.
    byte[] segmentData = new byte[segmentSize];
    int bytesRead = 0;
    while (bytesRead < segmentSize) {
        int result = input.read(segmentData, bytesRead, segmentSize - bytesRead);
        if (result == -1) throw new IOException("Sfârșitul stream-ului a fost atins prematur.");
        bytesRead += result;
    }
    return segmentData;
}

/**
 * Calculează și afișează timpul necesar pentru scrierea întregii imagini.
 * Această metodă este utilă pentru a evalua performanța procesului de scriere.
 *
 * returneaza Timpul de scriere al imaginii, în secunde.
 */
@Override
public long timpScriere() {
    System.out.println("Scrierea imaginii a durat " + timeWrite / 1000.0f + " secunde");
    return timeWrite;
}
}