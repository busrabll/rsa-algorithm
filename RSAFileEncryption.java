import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

public class RSAFileEncryption {
    private static HashSet<Integer> prime = new HashSet<>();
    private static Integer public_key = null;
    private static Integer private_key = null;
    private static Integer n = null;
    private static Random random = new Random();

    private static final int KEY_SIZE_1024 = 1024;
    private static final int KEY_SIZE_2048 = 2048;
    private static final int KEY_SIZE_4096 = 4096;

    public static void main(String[] args) {
        primeFiller();

        JFrame frame = new JFrame("RSA Dosya Şifreleme");
        JButton selectFileButton = new JButton("Dosya seçin lütfen");

        selectFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
                fileChooser.setDialogTitle("Şifrelenecek dosyayı seçin");
                fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

                int returnValue = fileChooser.showOpenDialog(frame);
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    String inputFilePath = selectedFile.getAbsolutePath();

                    try {
                        if (selectedFile.length() == 0) {
                            JOptionPane.showMessageDialog(frame, "İşlem başarısız: Dosya boş!", "Hata",
                                    JOptionPane.ERROR_MESSAGE);
                        } else if (inputFilePath.contains("_encrypted")) {

                            loadKeys("keys.txt");

                            String decryptedFilePath = inputFilePath.replace("_encrypted", "_decrypted");
                            List<Integer> encryptedMessage = readEncryptedFile(inputFilePath);
                            String decodedMessage = decoder(encryptedMessage);
                            writeDecryptedFile(decodedMessage, decryptedFilePath);

                            JOptionPane.showMessageDialog(frame, "Şifre çözme başarılı!");
                        } else {

                            String encryptedFilePath = inputFilePath.replace(".txt", "_encrypted.txt");

                            // Anahtarları ayarlayın
                            if (!setKeysBasedOnFileSize(selectedFile.length())) {
                                JOptionPane.showMessageDialog(frame,
                                        "Dosya boyutu çok büyük, uygun anahtar boyutu belirlenemedi!", "Uyarı",
                                        JOptionPane.WARNING_MESSAGE);
                                return;
                            }

                            saveKeys("keys.txt");

                            String message = new String(Files.readAllBytes(Paths.get(inputFilePath)));

                            List<Integer> coded = encoder(message);
                            writeEncryptedFile(coded, encryptedFilePath);

                            JOptionPane.showMessageDialog(frame, "Şifreleme başarılı!");
                        }
                    } catch (IOException ex) {
                        System.err.println("Dosya işleme hatası: " + ex.getMessage());
                    } catch (Exception ex) {
                        System.err.println("Bir hata oluştu: " + ex.getMessage());
                    }
                } else {
                    System.out.println("Dosya seçimi iptal edildi.");
                }
            }
        });

        frame.add(selectFileButton);
        frame.setSize(300, 100);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        frame.setLocationRelativeTo(null);
    }

    public static void primeFiller() {
        boolean[] sieve = new boolean[250];
        for (int i = 0; i < 250; i++) {
            sieve[i] = true;
        }
        sieve[0] = false;
        sieve[1] = false;

        for (int i = 2; i < 250; i++) {
            for (int j = i * 2; j < 250; j += i) {
                sieve[j] = false;
            }
        }

        for (int i = 0; sieve.length > i; i++) {
            if (sieve[i]) {
                prime.add(i);
            }
        }
    }

    public static int pickRandomPrime() {
        int k = random.nextInt(prime.size());
        List<Integer> primeList = new ArrayList<>(prime);
        int ret = primeList.get(k);
        prime.remove(ret);
        return ret;
    }

    public static boolean setKeysBasedOnFileSize(long fileSize) {
        int keySize;
        if (fileSize <= 117) {
            keySize = KEY_SIZE_1024;
        } else if (fileSize <= 245) {
            keySize = KEY_SIZE_2048;
        } else if (fileSize <= 501) {
            keySize = KEY_SIZE_4096;
        } else {
            return false;
        }
        setKeys(keySize);
        return true;
    }

    public static void setKeys(int keySize) {
        int prime1 = pickRandomPrime();
        int prime2 = pickRandomPrime();

        n = prime1 * prime2;
        int fi = (prime1 - 1) * (prime2 - 1);
        int e = 2;

        while (true) {
            if (gcd(e, fi) == 1) {
                break;
            }
            e += 1;
        }
        public_key = e;
        int d = 2;
        while (true) {
            if ((d * e) % fi == 1) {
                break;
            }
            d += 1;
        }
        private_key = d;
    }

    public static int encrypt(int message) {
        int e = public_key;
        int encrypted_text = 1;
        while (e > 0) {
            encrypted_text *= message;
            encrypted_text %= n;
            e -= 1;
        }
        return encrypted_text;
    }

    public static int decrypt(int encrypted_text) {
        int d = private_key;
        int decrypted = 1;
        while (d > 0) {
            decrypted *= encrypted_text;
            decrypted %= n;
            d -= 1;
        }
        return decrypted;
    }

    public static int gcd(int a, int b) {
        if (b == 0) {
            return a;
        }
        return gcd(b, a % b);
    }

    public static List<Integer> encoder(String message) {
        List<Integer> coded = new ArrayList<>();
        for (int i = 0; i < message.length(); i++) {
            int encryptedChar = encrypt(message.charAt(i));
            coded.add(encryptedChar);
        }
        return coded;
    }

    public static String decoder(List<Integer> codedMessage) {
        StringBuilder decodedMessage = new StringBuilder();
        for (Integer encryptedChar : codedMessage) {
            char decryptedChar = (char) decrypt(encryptedChar);
            decodedMessage.append(decryptedChar);
        }
        return decodedMessage.toString();
    }

    public static List<Integer> readEncryptedFile(String filePath) throws IOException {
        List<Integer> encryptedMessage = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                encryptedMessage.add(Integer.parseInt(line));
            }
        }
        return encryptedMessage;
    }

    public static void writeEncryptedFile(List<Integer> encryptedMessage, String filePath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (Integer encryptedChar : encryptedMessage) {
                writer.write(encryptedChar + "\n");
            }
        }
    }

    public static void writeDecryptedFile(String message, String filePath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write(message);
        }
    }

    public static void saveKeys(String filePath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write(public_key + "\n");
            writer.write(private_key + "\n");
            writer.write(n + "\n");
        }
    }

    public static void loadKeys(String filePath) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            public_key = Integer.parseInt(reader.readLine());
            private_key = Integer.parseInt(reader.readLine());
            n = Integer.parseInt(reader.readLine());
        }
    }
}
