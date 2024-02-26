package work;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import javax.imageio.ImageIO;

// Clasa ReadImage este un fir de execuție care se ocupă de citirea și preprocesarea unei imagini.
public class ReadImage extends TimeProcessing {
    // Bufferul pentru partajarea imaginii cu alte thread-uri.
    private final BlockingQueue<BufferedImage> buffer;
    // Calea către fișierul de intrare care conține imaginea.
    private final String inputPath;
    // Variabilă pentru a marca dacă procesul de citire s-a încheiat.
    private volatile boolean isComplete;
    // Variabilă pentru monitorizarea timpului necesar citirii imaginii.
    private long timeRead;
    
    // Bloc de inițializare non-static pentru a inițializa variabilele timeRead și isComplete.
    {
        timeRead = 0;
        isComplete = false;
    }

    // Constructorul clasei ReadImage.
    public ReadImage(BlockingQueue<BufferedImage> buffer, String inputPath) {
        this.buffer = buffer;
        this.inputPath = inputPath;
    }

    // Metoda run care este executată atunci când thread-ul este pornit.
    @Override
    public void run() {
        try {
            // Începe monitorizarea timpului de citire.
            timeRead = System.currentTimeMillis();
            // Citirea imaginii originale de la calea specificată.
            BufferedImage originalImage = ImageIO.read(new File(inputPath));
            int width = originalImage.getWidth();
            int height = originalImage.getHeight();
            // Calcularea înălțimii fiecărui segment.
            int segmentHeight = height / 4;

            // Crearea unei imagini noi pentru a reasambla segmentele citite.
            BufferedImage reconstructedImage = new BufferedImage(width, height, originalImage.getType());
            Graphics g = reconstructedImage.getGraphics();

            // Procesul de citire și adăugare a fiecărui segment în imaginea reconstruită.
            for (int i = 0; i < 4; i++) {
            	int y = i * segmentHeight;
                int h = (i == 3) ? (height - y) : segmentHeight;

                System.out.println("Începe citirea segmentului " + (i + 1));
                
                for (int row = y; row < y + h; row++) {
                  System.out.print("Producer a citit rândul " + row + ": ");
                  for (int col = 0; col < width; col++) {
                      System.out.printf("Pixel [%d,%d] ", col, row);
                  }
                  
                  System.out.println();
                 
            }
                BufferedImage segment = originalImage.getSubimage(0, y, width, h);
                g.drawImage(segment, 0, y, null); // Adaugă segmentul la imaginea reconstruită
                
                System.out.println("Segmentul " + (i + 1) + " a fost citit.");
              Thread.sleep(1000);
            }
            // Finalizarea procesului de citire și adăugare a imaginii în buffer.
            g.dispose();
            buffer.put(reconstructedImage);

            // Marcarea finalizării procesului de citire și notificarea altor thread-uri.
            synchronized (this) {
                isComplete = true;
                notifyAll();
            }
            // Calcularea timpului total necesar pentru citire.
            timeRead = System.currentTimeMillis() - timeRead;
        } catch (IOException e) {
            // Gestionarea erorilor de citire a imaginii.
            System.err.println("Eroare la citirea imaginii: " + e.getMessage());
        } catch (InterruptedException e) {
            // Gestionarea întreruperilor thread-ului.
            Thread.currentThread().interrupt();
            System.err.println("Thread-ul a fost întrerupt: " + e.getMessage());
        }
    }

    // Metoda pentru a verifica dacă procesul de citire s-a încheiat. 
    //Această metodă este utilă pentru a sincroniza firul de execuție al procesului de citire cu alte firuri de execuție.

public synchronized boolean isComplete() {
    return isComplete;
}

/**
 * Calculează și afișează timpul total necesar pentru citirea și preprocesarea imaginii.
 * Această metodă este utilă pentru a evalua performanța procesului de citire.
 *
 * returneaza Timpul total necesar pentru citirea imaginii, în secunde.
 */
@Override
public long timpCitire() {
    System.out.println("Citirea imaginii a durat " + timeRead / 1000.0f + " secunde");
    return timeRead;
}

@Override
public long timpProcesare() {
	// TODO Auto-generated method stub
	
	return 0;
}
}