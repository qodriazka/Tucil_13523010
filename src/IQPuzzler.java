import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class IQPuzzler {

    static int N, M, P;
    static int iterationCount = 0;
    static char[][] papan;
    static char[][] jawaban;
    static List<String> pieces = new ArrayList<>();
    static long startTime, endTime;
    static boolean foundSolution = false;
    static Map<Character, String> bentukPiece;
    static Map<Character, Integer> banyakHuruf = new HashMap<>();
    static List<char[][]> hasilPermutasi = new ArrayList<>();


    @SuppressWarnings("resource")
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        try {
            System.out.print("Masukkan nama file test case: ");
            String fileName = scanner.nextLine();
            if (!validasiInput(fileName)) {
                System.out.println("Format input tidak sesuai.");
                return;
            }
            int totalPieceCount = pieces.stream().mapToInt(piece -> piece.replace("\n", "").replace(".", "").length()).sum();
            System.out.println("Jumlah huruf yang akan digunakan: " + totalPieceCount);
            if (totalPieceCount != N * M) {
                System.out.println("Tidak ada solusi: Jumlah piece tidak sama dengan luas papan.");
                return;
            }
            System.out.println("\nBentuk setiap piece:");
            for (Map.Entry<Character, String> entry : bentukPiece.entrySet()) {
                System.out.println("Piece '" + entry.getKey() + "':");
                System.out.println(entry.getValue());
            }
            System.out.println("\nBanyak huruf setiap piece:");
            for (Map.Entry<Character, Integer> entry : banyakHuruf.entrySet()) {
                System.out.println("Piece '" + entry.getKey() + "' memiliki " + entry.getValue() + " huruf.");
            }
            startTime = System.currentTimeMillis();
            permutasi(papan, bentukPiece, banyakHuruf, 0);
            endTime = System.currentTimeMillis();
            System.out.println("Waktu pencarian: " + (endTime - startTime) + " ms");
            System.out.println("Banyak kasus yang ditinjau: " + iterationCount);
            System.out.println("Apakah mau menyimpan solusi? (Y/N): ");
            String save = scanner.nextLine();
    
            if (save.equalsIgnoreCase("Y")) {
                System.out.println("Masukkan nama file output: ");
                String outputFileName = scanner.nextLine();
                saveSolution(outputFileName, jawaban);
            }
    
            scanner.close();

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    public static char[][] copyBoard(char[][] papan) {
        char[][] newBoard = new char[papan.length][papan[0].length];
        for (int i = 0; i < papan.length; i++) {
            newBoard[i] = papan[i].clone();
        }
        return newBoard;
    }

    public static void restoreBoard(char[][] papan, char[][] original) {
        for (int i = 0; i < papan.length; i++) {
            System.arraycopy(original[i], 0, papan[i], 0, original[i].length);
        }
    }

    @SuppressWarnings("resource")
    public static boolean validasiInput(String fileName) throws IOException {
        bentukPiece = new HashMap<>();
        Path filePath = Paths.get("../test", fileName);
        BufferedReader br = new BufferedReader(new FileReader(filePath.toFile()));
        String[] dims = br.readLine().split(" ");
        if (dims.length != 3) {
            System.out.println("Jumlah angka di baris pertama tidak sesuai.");
            return false;
        }
        N = Integer.parseInt(dims[0]);
        M = Integer.parseInt(dims[1]);
        P = Integer.parseInt(dims[2]);
        if (N <= 0 || M <= 0 || P <= 0) {
            System.out.println("Nilai N, M, dan P tidak boleh kurang dari sama dengan 0.");
            return false;
        }
        papan = new char[N][M];
        for (char[] row : papan) Arrays.fill(row, '.');
        
        String caseType = br.readLine();
        if (!(caseType.equals("DEFAULT") || caseType.equals("CUSTOM") || caseType.equals("PYRAMID"))) {
            System.out.println("Jenis kasus tidak sesuai.");
            return false;
        }
        Set<Character> uniquePieces = new HashSet<>();
        String line;
        while ((line = br.readLine()) != null) {
            if (line.isEmpty()) continue;
            line = line.replaceAll(" ", ".");
            int l=0;
            while(line.charAt(l) == '.' || line.charAt(l) == ' '){
                l++;
            }
            char pieceChar = line.charAt(l);
            if (pieceChar != '.' && pieceChar != ' ') {
                uniquePieces.add(pieceChar);
                bentukPiece.putIfAbsent(pieceChar, "");
            }
            bentukPiece.put(pieceChar, bentukPiece.get(pieceChar) + (bentukPiece.get(pieceChar).isEmpty() ? "" : "\n") + line);
        }
        for (Map.Entry<Character, String> entry : bentukPiece.entrySet()) {
            char key = entry.getKey();
            String shape = entry.getValue();
            pieces.add(shape);
            banyakHuruf.put(key, banyakHuruf.getOrDefault(key, 0) + shape.replace("\n", "").replace(".", "").length());
        }
        if (uniquePieces.size() != P){
            System.out.println("Jumlah piece tidak sesuai.");
            return false;
        }
        br.close();
        return true;
    }
    
    public static void permutasi(char[][] papan, Map<Character, String> bentukPiece, Map<Character, Integer> banyakHuruf, int indeks) {
        if (banyakHuruf.isEmpty() || indeks >= banyakHuruf.size()) return;
    
        List<Character> hurufList = new ArrayList<>(banyakHuruf.keySet());
        char huruf = hurufList.get(indeks);
        int jumlah = banyakHuruf.get(huruf);
        Map<Character, Set<String>> bentukPieceTransformasi = new HashMap<>();
        Set<String> allForms = new HashSet<>();
        
        String[] shapeArray = bentukPiece.get(huruf).split("\n");
        allForms.addAll(generateTransformations(shapeArray));
        bentukPieceTransformasi.put(huruf, allForms);

        cariPermutasi(papan, huruf, jumlah, 0, 0, bentukPieceTransformasi, indeks, bentukPiece, banyakHuruf);
    }
    
    private static void cariPermutasi(char[][] papan, char huruf, int sisa, int row, int col, Map<Character, Set<String>> bentukPieceTransformasi, int indeks, Map<Character, String> bentukPiece, Map<Character, Integer> banyakHuruf) {
        iterationCount++;
        if (sisa == 0) {
            if (cocok(papan, bentukPieceTransformasi)) {
                if (isBoardFull(papan)){
                    System.out.println("Solusi ditemukan");
                    cetakPapan(papan);
                    jawaban = copyBoard(papan);
                    foundSolution=true;
                    return;
                }else if (foundSolution==false && indeks < banyakHuruf.size() - 1) {
                    System.out.println("Lanjut ke indeks: " + (indeks + 1));
                    permutasi(papan, bentukPiece, banyakHuruf, indeks + 1);
                }
            }
            return;
        }
    
        if (row >= papan.length) return;
    
        for (int i = row; i < papan.length; i++) {
            for (int j = (i == row ? col : 0); j < papan[i].length; j++) {
                if (papan[i][j] == '.') {
                    papan[i][j] = huruf;
                    cariPermutasi(papan, huruf, sisa - 1, i, j + 1, bentukPieceTransformasi, indeks, bentukPiece, banyakHuruf);
                    papan[i][j] = '.';
                }
            }
        }
    }

    public static boolean isBoardFull(char[][] papan) {
        for (char[] row : papan) {
            for (char c : row) {
                if (c == '.') return false;
            }
        }
        return true;
    }

    public static boolean cocok(char[][] p, Map<Character, Set<String>> bentukPiece) {
        int rows = p.length, cols = p[0].length;

        for (Map.Entry<Character, Set<String>> entry : bentukPiece.entrySet()) {
            Set<String> pieceShapes = entry.getValue();
            for (String shape : pieceShapes) {
                char[][] shapeMatrix = parseShape(shape);
                int shapeRows = shapeMatrix.length, shapeCols = shapeMatrix[0].length;
                for (int i = 0; i <= rows - shapeRows; i++) {
                    for (int j = 0; j <= cols - shapeCols; j++) {
                        if (matchAt(p, shapeMatrix, i, j)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private static char[][] parseShape(String shape) {
        String[] lines = shape.split("\n");
        int rows = lines.length, cols = lines[0].length();
        char[][] matrix = new char[rows][cols];

        for (int i = 0; i < rows; i++) {
            matrix[i] = lines[i].toCharArray();
        }
        return matrix;
    }

    private static boolean matchAt(char[][] p, char[][] shape, int x, int y) {
        int shapeRows = shape.length, shapeCols = shape[0].length;

        for (int i = 0; i < shapeRows; i++) {
            for (int j = 0; j < shapeCols; j++) {
                if (shape[i][j] != '.' && shape[i][j] != p[x + i][y + j]) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void cetakPapan(char[][] papan) {
        for (char[] row : papan) {
            String outputRow = getColoredText(new String(row));
            System.out.println(outputRow);
        }
    }

    public static List<String> generateTransformations(String[] bentuk) {
        List<String> transformations = new ArrayList<>();
    
        char[][] original = convertToMatrix(bentuk);
        
        char[][] rot90 = rotate(original);
        char[][] rot180 = rotate(rot90);
        char[][] rot270 = rotate(rot180);
        
        char[][] mirror = mirror(original);
        char[][] mirrorRot90 = rotate(mirror);
        char[][] mirrorRot180 = rotate(mirrorRot90);
        char[][] mirrorRot270 = rotate(mirrorRot180);
        
        transformations.add(convertToString(original));
        transformations.add(convertToString(rot90));
        transformations.add(convertToString(rot180));
        transformations.add(convertToString(rot270));
        transformations.add(convertToString(mirror));
        transformations.add(convertToString(mirrorRot90));
        transformations.add(convertToString(mirrorRot180));
        transformations.add(convertToString(mirrorRot270));
        
        return transformations;
    }
    
    private static char[][] convertToMatrix(String[] shape){
        int rows = shape.length;
        int cols = 0;
        for (String row : shape) {
            cols = Math.max(cols, row.length());
        }
        char[][] matrix = new char[rows][cols];
        for (int i = 0; i < rows; i++) {
            String row = shape[i];
            for (int j = 0; j < cols; j++) {
                matrix[i][j] = (j < row.length()) ? row.charAt(j) : '.';
            }
        }
        return matrix;
    }
    
    private static char[][] rotate(char[][] matrix) {
        int rows = matrix.length;
        int cols = matrix[0].length;
        char[][] rotated = new char[cols][rows];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                rotated[j][rows - 1 - i] = matrix[i][j];
            }
        }
        return rotated;
    }

    private static char[][] mirror(char[][] matrix) {
        int rows = matrix.length;
        int cols = matrix[0].length;
        char[][] mirrored = new char[rows][cols];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                mirrored[i][cols - 1 - j] = matrix[i][j];
            }
        }
        return mirrored;
    }

    private static String convertToString(char[][] matrix) {
        StringBuilder sb = new StringBuilder();
        for (char[] row : matrix) {
            sb.append(new String(row)).append("\n");
        }
        return sb.toString().trim();
    }

    public static String getColoredText(String text) {
        StringBuilder coloredText = new StringBuilder();
        for (char c : text.toCharArray()) {
            if(c == '.'){
                coloredText.append(c);
                continue;
            }
            int index = Character.toUpperCase(c) - 'A';
            coloredText.append(COLORS[index]).append(c).append(RESET);
        }
        return coloredText.toString();
    }

    public static void saveSolution(String fileName, char[][] papan) {
        Path filePath = Paths.get("../test", fileName);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath.toFile()))) {
            for (char[] row : papan) {
                writer.write(row);
                writer.newLine();
            }
            System.out.println("Solusi berhasil disimpan ke dalam file: " + filePath);
        } catch (IOException e) {
            System.err.println("Terjadi kesalahan saat menyimpan solusi: " + e.getMessage());
        }
    }
    public static final String RESET = "\u001B[0m";
    public static final String[] COLORS = {
        "\u001B[31m", // A - Merah
        "\u001B[34m", // B - Biru
        "\u001B[32m", // C - Hijau
        "\u001B[33m", // D - Kuning
        "\u001B[35m", // E - Ungu
        "\u001B[36m", // F - Cyan
        "\u001B[91m", // G - Merah terang
        "\u001B[94m", // H - Biru terang
        "\u001B[92m", // I - Hijau terang
        "\u001B[93m", // J - Kuning terang
        "\u001B[95m", // K - Ungu terang
        "\u001B[96m", // L - Cyan terang
        "\u001B[37m", // M - Putih
        "\u001B[90m", // N - Abu-abu
        "\u001B[97m", // O - Putih terang
        "\u001B[30m", // P - Hitam (jika latar belakang terang)
        "\u001B[41m", // Q - Background merah
        "\u001B[42m", // R - Background hijau
        "\u001B[43m", // S - Background kuning
        "\u001B[44m", // T - Background biru
        "\u001B[45m", // U - Background ungu
        "\u001B[46m", // V - Background cyan
        "\u001B[100m", // W - Background abu-abu gelap
        "\u001B[101m", // X - Background merah terang
        "\u001B[102m", // Y - Background hijau terang
        "\u001B[103m"  // Z - Background kuning terang
    };
}