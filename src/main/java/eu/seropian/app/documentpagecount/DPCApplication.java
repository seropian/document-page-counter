package eu.seropian.app.documentpagecount;

import static java.lang.System.exit;

import lombok.extern.slf4j.Slf4j;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.poi.ooxml.POIXMLDocument;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class DPCApplication {

  private final AtomicLong totalPages = new AtomicLong(0);
  private final AtomicLong totalDocuments = new AtomicLong(0);
  private final ExecutorService executor = Executors.newFixedThreadPool(1);

  public static void main(String[] args) throws InterruptedException {
    checkArgs(args);
    new DPCApplication().start(args);
  }

  private void start(String[] args) throws InterruptedException {
    final Path rootPath = getRootPath(args[0]);
    final Instant start = Instant.now();
    process(rootPath);
    executor.shutdown();
    executor.awaitTermination(1, TimeUnit.DAYS);
    // CODE HERE
    final Instant finish = Instant.now();
    log.info("Total number of documents: {}", totalDocuments.get());
    log.info("Total number of pages: {}", totalPages.get());
    log.info("Total processing time: {}", getElapsedTimeAsString(start, finish));
  }

  private String getElapsedTimeAsString(Instant start, Instant finish) {
    final Duration duration = Duration.between(start, finish);
    long hours = duration.toHours();
    long minutes = duration.minusHours(hours).toMinutes();
    long seconds = duration.minusHours(hours).minusMinutes(minutes).getSeconds();
    return hours + "h " + minutes + "m " + seconds + "s";
  }

  private void process(Path rootPath) {
    final FileVisitor<Path> fileVisitor = new SimpleFileVisitor<Path>() {

      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        if (Files.isReadable(file) && canProcess(file)) {
          executor.submit(() -> processFile(file));
          totalDocuments.incrementAndGet();
        }
        return FileVisitResult.CONTINUE;
      }

      private boolean canProcess(Path file) {
        final String fileName = file.toString();
        return fileName.endsWith(".pdf") || fileName.endsWith(".docx");
      }

      @Override
      public FileVisitResult visitFileFailed(Path file, IOException exc) {
        log.warn(">> {} >> Error: {}", file.toString(), exc.toString());
        return FileVisitResult.CONTINUE;
      }
    };
    try {
      Files.walkFileTree(rootPath, fileVisitor);
    } catch (IOException e) {
      log.warn("Error: {}", e.toString());
    }
  }

  private void processFile(Path path) {
    final long documentPages;
    final String fileName = path.toFile().getName();
    try {
      if (fileName.endsWith(".pdf")) {
        documentPages = processPdfFile(path);
      } else if (fileName.endsWith(".docx")) {
        documentPages = processDocxFile(path);
      } else {
        return;
      }
      totalPages.getAndAdd(documentPages);
      log.info(">> {} >> Pages: {}", path.toString(), documentPages);
    } catch (Exception e) {
      log.warn(">> {} >> Error: {}", path.toString(), e.toString());
    }
  }

  private Path getRootPath(String arg) {
    Path path = Paths.get(arg);
    log.info("Path = {}", path);
    return path;
  }

  private static void checkArgs(String[] args) {
    if (args.length == 0) {
      log.info("Usage: java -jar DocumentPageCounter-0.2-full.jar <target directory>");
      exit(1);
    }
  }

  private long processPdfFile(Path path) throws IOException {
    try (PDDocument doc = PDDocument.load(path.toFile())) {
      return doc.getNumberOfPages();
    }
  }

  private long processDocxFile(Path path) throws IOException {
    try (XWPFDocument docx = new XWPFDocument(POIXMLDocument.openPackage(path.toString()))) {
      return docx.getProperties().getExtendedProperties().getUnderlyingProperties().getPages();
    }
  }
}
