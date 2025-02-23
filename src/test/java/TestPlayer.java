import java.util.Scanner;

public class TestPlayer {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String tileCount = scanner.nextLine();
        String tiles = scanner.nextLine();
        String sizes = scanner.nextLine();
        for (int i = 0; i < 11; i++) {
            String line = scanner.nextLine();
            System.err.println(line);
        }

        System.out.println("J 0 1 0 0");
        System.out.println("H 1 2 1 0");
        System.out.println("N 1 0 2 0");
        System.out.println("X 0 0 3 0");
        System.out.println("I 0 1 5 0");
        System.out.println("U 0 2 0 1");
        System.out.println("R 1 2 0 1");
        System.out.println("T 0 0 3 1");
        System.out.println("W 0 0 1 2");
        System.out.println("Z 1 0 2 2");
        System.out.println("V 0 3 0 3");
        System.out.println("P 0 0 3 3");
        System.out.println("Y 0 0 0 4");
        System.out.println("F 1 0 2 4");
        System.out.println("O 1 0 4 4");
    }
}
