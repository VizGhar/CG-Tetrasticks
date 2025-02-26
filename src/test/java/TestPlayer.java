import java.util.Scanner;

public class TestPlayer {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String tileCount = scanner.nextLine();
        String tiles = scanner.nextLine();
        String sizes = scanner.nextLine();
        for (int i = 0; i < 18; i++) {
            String line = scanner.nextLine();
            System.err.println(line);
        }

    }
}
