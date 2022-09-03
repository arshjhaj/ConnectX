import java.util.Scanner;

public class Board{


    private static final String ANSI_RESET = "\u001B[0m";
    private static final String[] PLAYER_COLORS = new String[]{
            "\u001B[31m", "\u001b[33m", "\u001b[32m", "\u001B[35m", "\u001b[36m",
    };


    public byte[] board;
    public final int HEIGHT;
    public final int WIDTH;

    private final Scanner SC = new Scanner(System.in);


    public Board() {

        this(6, 7);

    }
    public Board(int height, int width) {

        this.HEIGHT = Math.max(height, 4);
        this.WIDTH = Math.max(width, 4);
        this.board  = new byte[this.HEIGHT * this.WIDTH];

    }


    public void promptUserTurn(){

        String choice = "";
        int col = -1;
        while(!this.colIsOpen(col))
        {
            try
            {
                System.out.print("Choose a valid colum: ");
                choice = SC.nextLine();
                col = Integer.parseInt(choice) - 1;
            }
            catch(Exception e)
            {
                choice = "";
            }
        }
        this.placeCoin(col, (byte) 1);


    }

    public void placeCoin(int col, byte player){

        for(int row = 0; row < this.HEIGHT; row++)
            if(this.board[getIndex(row, col)] == 0)
            {
                this.board[getIndex(row, col)] = player;
                return;
            }

    }

    public boolean hasWon(){

        for(int row = 0; row < this.HEIGHT; row++)
            for (int col = 0; col < this.WIDTH; col++)
            {

                byte player = this.board[getIndex(row, col)];

                boolean victory = true;
                if (player != 0 && col >= 3) {
                    for (int offset = 1; offset < 4; offset++)
                        victory = victory && player == this.board[getIndex(row, col - offset)];
                    if (victory) return true;
                }

                // Test for vertical wins.
                victory = true;
                if (player != 0 && row >= 3) {
                    for (int offset = 1; offset < 4; offset++)
                        victory = victory && player == this.board[getIndex(row - offset, col)];
                    if (victory) return true;
                }

                // Test for right diagonal wins.
                victory = true;
                if (player != 0 && row >= 3 && col >= 3) {
                    for (int offset = 1; offset < 4; offset++)
                        victory = victory && player == this.board[getIndex(row - offset, col - offset)];
                    if (victory) return true;
                }

                // Test for left diagonal wins.
                victory = true;
                if (player != 0 && row >= 3 && col <= this.WIDTH - 4) {
                    for (int offset = 1; offset < 4; offset++)
                        victory = victory && player == this.board[getIndex(row - offset, col + offset)];
                    if (victory) return true;
                }

            }
        return false;

    }


    // Print methods
    public void print(){

        printBar();
        for(int i = this.HEIGHT - 1; i >= 0; i--) {
            for (int j = 0; j < this.WIDTH; j++) {
                System.out.print(("\t" + formatCoinColor(this.board[getIndex(i, j)])));
            }
            System.out.println();
        }
        printBar();
        printColNumbers();

    }
    private void printBar(){

        StringBuilder bar = new StringBuilder("   ");
        int times = Math.max(0, 4 * this.WIDTH - 1);
        for(int i = 0; i < times; i++)
            bar.append("_");
        System.out.println(bar);

    }
    private void printColNumbers(){

        StringBuilder numbers = new StringBuilder();
        for(int i = 1; i <= this.WIDTH; i++) numbers.append("\t").append(i);
        System.out.println(numbers);

    }



    private String formatCoinColor(byte coin){

        if(coin == 0) return "•";
        return PLAYER_COLORS[coin - 1] + "O" + ANSI_RESET;
    }

    public int getIndex(int row, int col){

        return row * this.WIDTH + col;

    }

    public void setValue(int row, int col, byte setTo){

        this.board[getIndex(row, col)] = setTo;

    }

    public boolean colIsOpen(int col){

        if(col > this.WIDTH - 1 || col < 0) return false;
        return this.board[this.getIndex(this.HEIGHT - 1, col)] == 0;

    }




}
