package work;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;

import javax.imageio.ImageIO;

//Clasa ImageProcess extinde Thread, permițând executarea procesării imaginii într-un fir de execuție separat.
public class ImageProcess extends TimeProcessing {
 private BlockingQueue<BufferedImage> buffer; // Bufferul pentru stocarea și extragerea imaginilor.
 private PipedOutputStream output; // Stream pentru trimiterea imaginii procesate către un alt proces.
 private long timeProcess; // Variabilă pentru monitorizarea timpului de procesare.

 // Constructorul clasei ImageProcess.
 public ImageProcess(BlockingQueue<BufferedImage> buffer, PipedOutputStream output) {
     this.buffer = buffer;
     this.output = output;
 }

 /**
  * Metoda 'run' reprezintă funcția principală a firului de execuție pentru clasa ImageProcess.
  * În această metodă, imaginea este preluată din buffer, procesată și apoi împărțită în segmente.
  * Fiecare segment este trimis individual prin PipedOutputStream pentru a fi ulterior scris în fișier.
  */
 @Override
 public void run() {
     try {
         // Începe monitorizarea timpului de procesare.
         

         // Extrage imaginea din buffer.
         BufferedImage image = buffer.take(); 
            BufferedImage processedImage = processImage(image);//proceseaza imaginea

            // Împarte imaginea procesată în 4 segmente și le trimite
            int segmentHeight = processedImage.getHeight() / 4;

            for (int i = 0; i < 4; i++) {
                // Calculează coordonatele y și înălțimea pentru fiecare segment
                int y = i * segmentHeight;
                int h = (i == 3) ? (processedImage.getHeight() - y) : segmentHeight; // Ajustare pentru ultimul segment
                BufferedImage segment = processedImage.getSubimage(0, y, processedImage.getWidth(), h);
                
             // Crează un flux de ieșire pentru a scrie segmentul în formatul BMP.
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(segment, "bmp", baos);
                byte[] segmentBytes = baos.toByteArray();

                // Trimite dimensiunea segmentului înainte de datele efective ale segmentului.
                output.write(ByteBuffer.allocate(4).putInt(segmentBytes.length).array());
                // Trimite datele segmentului prin pipe.
                output.write(segmentBytes);
    
                // Afișează un mesaj în consolă pentru a indica transmiterea fiecărui segment.
                System.out.println("Segmentul " + (i + 1) + " a fost transmis.");
                Thread.sleep(1000);
            }

            // Închide PipedOutputStream după transmiterea tuturor segmentelor pentru a semnala finalul transmisiei.
            output.close(); 
//            timeProcess = System.currentTimeMillis() - timeProcess;
        } catch (IOException e) {
            System.err.println("Eroare la procesarea sau scrierea imaginii: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Thread-ul a fost întrerupt: " + e.getMessage());
            e.printStackTrace();
        }
    }

 /**
  * Procesează imaginea primită pentru a îmbunătăți calitatea acesteia.
  * Aplică egalizarea histogramei pe imagine pentru a îmbunătăți contrastul și luminozitatea.
  * 
  * param original e imaginea originală care trebuie procesată.
  * returneaza o imagine procesată cu histograma egalizată.
  */

private BufferedImage processImage(BufferedImage original) {
	timeProcess = System.currentTimeMillis();
    int width = original.getWidth();
    int height = original.getHeight();

    BufferedImage equalizedImage = new BufferedImage(width, height, original.getType());
    ArrayList<int[]> histLUT = equalizeHistogram(original);

    for (int i = 0; i < width; i++) {
        for (int j = 0; j < height; j++) {
            int alpha = new Color(original.getRGB(i, j)).getAlpha();
            int red = new Color(original.getRGB(i, j)).getRed();
            int green = new Color(original.getRGB(i, j)).getGreen();
            int blue = new Color(original.getRGB(i, j)).getBlue();

            // Setam noile valori ale pixelilor folosind histograma
            red = histLUT.get(0)[red];
            green = histLUT.get(1)[green];
            blue = histLUT.get(2)[blue];

            int newPixel = convertColorToRGB(alpha, red, green, blue);
            equalizedImage.setRGB(i, j, newPixel);
        }
    }
    timeProcess = System.currentTimeMillis() - timeProcess;
    return equalizedImage;
}

/**
 * Calculează histogramele pentru fiecare canal de culoare al imaginii și creează o tabelă de căutare (LUT)
 * pentru a egaliza histograma fiecărui canal. Aceasta îmbunătățește contrastul imaginii.
 * 
 * param original e imaginea originală pentru care se calculează histogramele.
 * returneaza Un ArrayList conținând histogramele egalizate pentru fiecare canal de culoare.
 */

private ArrayList<int[]> equalizeHistogram(BufferedImage original) {
    

     // calculam valoare histogramei pentru fiecare canal de culoare
     ArrayList<int[]> imageHist = saveHistogram(original);

     // vectorul  in care se vor salva valorile
     ArrayList<int[]> imageLUT = new ArrayList<int[]>();

     // Declaram vectorii
     int[] rhistogram = new int[256];
     int[] ghistogram = new int[256];
     int[] bhistogram = new int[256];

     for(int i=0; i<rhistogram.length; i++) rhistogram[i] = 0;
     for(int i=0; i<ghistogram.length; i++) ghistogram[i] = 0;
     for(int i=0; i<bhistogram.length; i++) bhistogram[i] = 0;

     long sumr = 0;
     long sumg = 0;
     long sumb = 0;

     //Calculam factorul de scalare
     float scaleFactor = (float) (255.0 / (original.getWidth() * original.getHeight()));

     for(int i=0; i<rhistogram.length; i++) {
         sumr += imageHist.get(0)[i];
         int valr = (int) (sumr * scaleFactor);
         if(valr > 255) {
             rhistogram[i] = 255;
         }
         else rhistogram[i] = valr;

         sumg += imageHist.get(1)[i];
         int valg = (int) (sumg * scaleFactor);
         if(valg > 255) {
             ghistogram[i] = 255;
         }
         else ghistogram[i] = valg;

         sumb += imageHist.get(2)[i];
         int valb = (int) (sumb * scaleFactor);
         if(valb > 255) {
             bhistogram[i] = 255;
         }
         else bhistogram[i] = valb;
     }

     imageLUT.add(rhistogram);
     imageLUT.add(ghistogram);
     imageLUT.add(bhistogram);

     return imageLUT;
}

/**
 * Salvează histogramele pentru fiecare canal de culoare al imaginii originale.
 * Această metodă este folosită pentru a calcula distribuția pixelilor pe fiecare canal de culoare.
 * 
 * param original Imaginea originală pentru care se calculează histogramele.
 * returneaza un ArrayList conținând histogramele pentru fiecare canal de culoare.
 */

public static ArrayList<int[]> saveHistogram(BufferedImage original) {

    int[] rhistogram = new int[256];
    int[] ghistogram = new int[256];
    int[] bhistogram = new int[256];

    for(int i=0; i<rhistogram.length; i++) rhistogram[i] = 0;
    for(int i=0; i<ghistogram.length; i++) ghistogram[i] = 0;
    for(int i=0; i<bhistogram.length; i++) bhistogram[i] = 0;

    for(int i=0; i<original.getWidth(); i++) {
        for(int j=0; j<original.getHeight(); j++) {

            int red = new Color(original.getRGB (i, j)).getRed();
            int green = new Color(original.getRGB (i, j)).getGreen();
            int blue = new Color(original.getRGB (i, j)).getBlue();

            
            rhistogram[red]++; ghistogram[green]++; bhistogram[blue]++;

        }
    }

    ArrayList<int[]> hist = new ArrayList<int[]>();
    hist.add(rhistogram);
    hist.add(ghistogram);
    hist.add(bhistogram);

    return hist;

}

/**
 * Converteste valorile individuale de culoare într-o valoare de pixel RGB.
 * Această metodă combină valorile separate ale canalelor
de culoare într-un singur pixel colorat.

* param alpha =>Valoarea alpha a pixelului (transparență).
* param red =>Valoarea canalului roșu a pixelului.
* param green =>Valoarea canalului verde a pixelului.
* param blue => Valoarea canalului albastru a pixelului.
* returneaza valoarea pixelului combinat, formată din componentele alpha, red, green și blue.
*/

private static int convertColorToRGB(int alpha, int red, int green, int blue) {
	  int newPixel = 0;
      newPixel += alpha; newPixel = newPixel << 8;
      newPixel += red; newPixel = newPixel << 8;
      newPixel += green; newPixel = newPixel << 8;
      newPixel += blue;

      return newPixel;
}
//calculeaza timpul de procesare a imaginii
//@Override
//public long timpProcesare() {
//    System.out.println("Procesarea imaginii a durat " + timeProcess / 1000.0f + " secunde");
//    return 0;
//}

@Override
public long timpProcesare() {
	// TODO Auto-generated method stub
	System.out.println("Procesarea imaginii a durat " + timeProcess / 1000.0f + " secunde");
	return 0;
}

@Override
public long timpCitire() {
	// TODO Auto-generated method stub
	return 0;
}


}