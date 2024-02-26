package test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import work.ImageProcess;
import work.ReadImage;
import work.WriteImage;

public class Main {
    @SuppressWarnings("resource")
    public static void main(String[] args) {
        // Initializarea scanner-ului pentru citirea inputului de la utilizator
Scanner scanner = new Scanner(System.in);


    // Solicită utilizatorului să introducă calea către fișierul de intrare (imaginea sursă)
    System.out.println("Introduceți calea către fișierul de intrare (imaginea sursă):");
    String inputPath = scanner.nextLine();

    // Solicită utilizatorului să introducă calea către fișierul de ieșire (imaginea procesată)
    System.out.println("Introduceți calea către fișierul de ieșire (imaginea procesată):");
    String outputPath = scanner.nextLine();

    // Creează un buffer comun între Producer (ReadImage) și Consumer (ImageProcess)
    // Acest buffer va fi folosit pentru a stoca și a transfera imaginea procesată între aceste două componente
    BlockingQueue<BufferedImage> buffer = new LinkedBlockingQueue<>();

    // Creează un PipedOutputStream și un PipedInputStream
    // Acestea sunt folosite pentru a realiza comunicarea între Consumer (ImageProcess) și Writer (WriteImage)
    PipedOutputStream outputPipe = new PipedOutputStream();
    PipedInputStream inputPipe = new PipedInputStream();

    try {
        // Conectează PipedOutputStream cu PipedInputStream
        // Aceasta permite ca datele scrise în outputPipe să fie citite din inputPipe
        outputPipe.connect(inputPipe);
    } catch (IOException e) {
        // Tratează cazul în care conexiunea între pipe-uri eșuează
        System.err.println("Eroare la conectarea Pipes-urilor: " + e.getMessage());
        // Închide scanner-ul și încheie programul în caz de eroare
        scanner.close();
        return;
    }

    // Creează instanțele pentru Producer (ReadImage), Consumer (ImageProcess) și Writer (WriteImage)
    ReadImage producer = new ReadImage(buffer, inputPath); // Producer care citește imaginea
    ImageProcess consumer = new ImageProcess(buffer, outputPipe); // Consumer care procesează imaginea
    WriteImage writer = new WriteImage(inputPipe, outputPath); // Writer care scrie imaginea procesată în fișier

    // Pornește thread-urile pentru Producer, Consumer și Writer
    producer.start();
    consumer.start();
    writer.start();

    // Așteaptă finalizarea execuției tuturor thread-urilor
    try {
        producer.join();
        consumer.join();
        writer.join();
    } catch (InterruptedException e) {
        // Gestionează cazul în care thread-ul curent este întrerupt
        Thread.currentThread().interrupt();
        System.err.println("Procesul a fost întrerupt: " + e.getMessage());
    }

    // Afișează timpul necesar pentru fiecare etapă a procesului
    producer.timpCitire();
    consumer.timpProcesare();
    writer.timpScriere();

    // Informează utilizatorul că prelucrarea imaginii a fost finalizată

        System.out.println("Prelucrarea imaginii a fost finalizată.");
        scanner.close();
    }
}
