import java.util.ArrayList;
import java.util.Arrays;

public class AI extends Board{



    public final byte PLAYER_CODE;

    public int difficulty;


    private int[] anticipatoryMoveWeight;
    private int[] checkOrder;


    private final boolean dynamicDifficulty;

    private final boolean memoize;
    private final Memoize memoizer;

    

    public AI(){

        this(6, 7);

    }
    public AI(int height, int width){

        this(height, width, 6);

    }
    public AI(int height, int width, int difficulty){

        super(height, width);
        this.difficulty = difficulty;
        this.PLAYER_CODE = (byte) 2;
        this.anticipatoryMoveWeight = new int[super.WIDTH];
        this.dynamicDifficulty = true;
        this.memoize = difficulty >= 6 && height == 6 && width == 7;
        this.memoizer = memoize? new Memoize() : null;

    }


  

    public int getBestMove(){

        
        System.out.print("\u001b[32m");

        System.out.println("RECURSIVE DEPTH: " + this.difficulty);


        int zeros = zeroSum();
        double allSeeing = printAllSeeing(zeros);
        String boardAsString = Arrays.toString(super.board);
        int preMinimax = this.preMinimaxMoveFinding(boardAsString, zeros);
        if(preMinimax != -1) return preMinimax;

        ArrayList<Integer> bestMoves = new ArrayList<>();
        int[] lossList = this.minimaxMoveFinding(bestMoves, zeros, allSeeing);

        return this.postMinimaxMoveFinding(bestMoves, lossList, boardAsString);


    }

    public int minimax(boolean aiTurn, int countDown, int alpha, int beta, int zeros){

        if (super.hasWon() || countDown == 0) return this.evaluateBoard(!aiTurn, zeros);

        String boardAsString = Arrays.toString(super.board);
        if(aiTurn && this.memoize && this.memoizer.dictionary.containsKey(boardAsString))
        {
            System.out.print("×");
            return this.memoizer.getMinMax(boardAsString, aiTurn);
        }

        int minMax = aiTurn? Integer.MIN_VALUE + 1 : Integer.MAX_VALUE - 1;
        for(int i = 0; i < super.WIDTH; i++)
        {
            int col = checkOrder[i];
            if(super.colIsOpen(col))
            {
                super.placeCoin(col, (byte) (aiTurn? 2 : 1));
                int evaluation = minimax(
                        !aiTurn,
                        countDown - 1,
                        alpha, beta,
                        zeros - 1
                );
                undoLastMove(col);

                // Update minMax depending on whether it's the minimizing or maximizing turn.
                minMax = aiTurn?
                        Math.max(minMax, evaluation):
                        Math.min(minMax, evaluation);

                // Alpha-Beta pruning.
                // If we know there's already a better option, there is no reason to search this.
                // This lets us avoid redundancy.
                if(aiTurn) alpha = Math.max(alpha, evaluation);
                else beta = Math.min(beta, evaluation);
                if(beta <= alpha) break;
            }
        }
        return minMax;

    }



    public int preMinimaxMoveFinding(String boardAsString, int zeros){

        int winningColumn = this.canWin(this.PLAYER_CODE);
        if(winningColumn != -1)
        {
            return winningColumn;
        }
        int losingColumn = this.canWin((byte) 1);
        if(losingColumn != -1)
        {
            return losingColumn;
        }

        // Check if the boards been memoized.
        int remembered = rememberBestMove(boardAsString);
        if(remembered != -1)
        {
            return remembered;
        }


        this.checkOrder = this.getDistributionOrder();

        return this.getImmediateBestMove(zeros, 3);

    }

    public int[] minimaxMoveFinding(ArrayList<Integer> bestMoves, int zeros, double allSeeing){

        long startTime = System.currentTimeMillis();


        int[] lossList = this.getLossList(bestMoves, zeros, allSeeing);

        if(this.dynamicDifficulty) this.doDynamicDifficulty(zeros, startTime);

        return lossList;

    }


    public int postMinimaxMoveFinding(ArrayList<Integer> bestMoves, int[] lossList, String boardAsString){

        if(bestMoves.size() == 0)
            for(int i : checkOrder)
                if(this.colIsOpen(i))
                    return i;

        if(this.memoize && this.difficulty >= 7)
            memoizer.cacheBoard(boardAsString, Arrays.toString(lossList));

        return bestMoves.get((int)(Math.random() * bestMoves.size()));

    }


    public int getImmediateBestMove(int zeros, int depth){

        for(int i = 0; i < super.WIDTH; i++) {

            int col = checkOrder[i];

            if (super.colIsOpen(col)) {

                super.placeCoin(col, (byte) 2);
                int loss = minimax(
                        false,
                        depth,
                        Integer.MIN_VALUE,
                        Integer.MAX_VALUE,
                        zeros
                );
                this.undoLastMove(col);
                if(loss >= 100)
                {
                    System.out.println("Defeat is imminent.");
                    return col;
                }

            }
        }

        return -1;

    }

    public int rememberBestMove(String boardAsString){

        return this.memoize? this.memoizer.getBestMove(boardAsString) : -1;

    }

    public double printAllSeeing(int zeros){

        double allSeeing = (double) Math.min(this.difficulty, zeros) / zeros;
        if(allSeeing >= 1) System.out.println("The AI has reached zenith.");
        allSeeing = Math.round(allSeeing * 10000) / 100.0;
        System.out.println("AI intelligence: " + allSeeing + " %");
        return allSeeing;

    }

    public int canWin(byte playerCode){

        for(int col = 0; col < super.WIDTH; col++)
            if(this.testWin(col, playerCode))
                return col;
        return -1;

    }


    public void doDynamicDifficulty(int zeros, long startTime){

        long elapsedTime = System.currentTimeMillis() - startTime;
        if(elapsedTime <= 400 && this.difficulty >= 4 && super.board.length - zeros >= 8)
        {
            this.difficulty += 2;
            System.out.println("The AI is closing in.");
        }
        else if (elapsedTime <= 1000)   this.difficulty++;
        else if (elapsedTime >= 7500 && this.difficulty > 6) this.difficulty = 6;
        else if (elapsedTime >= 4000 && this.difficulty > 5) this.difficulty--;


        this.difficulty = Math.min(this.difficulty, zeros);

    }


    public int[] getLossList(ArrayList<Integer> bestMoves, int zeros, double allSeeing){

        return this.getLossList(
                bestMoves,
                zeros,
                allSeeing,
                this.difficulty + this.filledColumns()
        );

    }
    public int[] getLossList(ArrayList<Integer> bestMoves, int zeros, double allSeeing, int depth){

        boolean willWin = false;
        int averageLoss = 0;
        int lossCount = 0;
        int playerTraps = 0;

        System.out.print("The AI is thinking ");
        int max = Short.MIN_VALUE;
        int[] lossList = new int[super.WIDTH];
        for(int i = 0; i < super.WIDTH; i++) {

            int col = checkOrder[i];

            if (super.colIsOpen(col)) {

                super.placeCoin(col, (byte) 2);
                int loss = minimax(
                        false,
                        depth,
                        Integer.MIN_VALUE,
                        Integer.MAX_VALUE,
                        zeros
                );
                this.undoLastMove(col);
                lossList[col] = loss;

                if(loss >= 100) willWin = true;
                else if(loss <= -100) playerTraps++;
                else
                {
                    averageLoss += loss;
                    lossCount++;
                }

                System.out.print("•");

                if (loss > max)
                {
                    max = loss;
                    bestMoves.clear();
                    bestMoves.add(col);
                }
                else if (loss == max) bestMoves.add(col);
            }
        }

        this.getWinningTotals(zeros);

        System.out.println();
        if(lossCount != 0) averageLoss /= lossCount;
        if (willWin) System.out.println("The AI has formulated a plan.");
        else if(playerTraps >= 3) System.out.println(allSeeing == 1? "The AI accepts its defeat." : "The AI is being very cautious.");
        else if(averageLoss <= -20) System.out.println("The AI trying to plan.");

        return lossList;

    }



    public int evaluateBoard(boolean aiTurn, int zeros) {

        int score = this.getNonTerminalPoints(!aiTurn, zeros);


        return aiTurn? score : -score;

    }

    public int getNonTerminalPoints(boolean aiTurn, int zeros){

        int totalPoints = 0;
        for(int row = 0; row < this.HEIGHT; row++)
            for (int col = 0; col < this.WIDTH; col++)
            {

                byte player = this.board[getIndex(row, col)];

                StringBuilder identity = new StringBuilder();
                if (col >= 3) {
                    for (int offset = 0; offset < 4; offset++)
                    {
                        byte position = this.board[getIndex(row, col - offset)];
                        String value;
                        switch (position)
                        {
                            case 0: value = " "; break;
                            case 1: value = "1"; break;
                            case 2: value = "2"; break;
                            default: throw new IllegalStateException("Unexpected value.");
                        }
                        identity.append(value);
                    }
                    totalPoints += scoreIdentityStr(identity.toString(), aiTurn, zeros);
                }

                identity = new StringBuilder();
                if (row >= 3) {
                    for (int offset = 0; offset < 4; offset++)
                    {
                        byte position = this.board[getIndex(row - offset, col)];
                        String value;
                        switch (position)
                        {
                            case 0: value = " "; break;
                            case 1: value = "1"; break;
                            case 2: value = "2"; break;
                            default: throw new IllegalStateException("Unexpected value.");
                        }
                        identity.append(value);
                    }
                    totalPoints += scoreIdentityStr(identity.toString(), aiTurn, zeros);
                }

                identity = new StringBuilder();
                if (row >= 3 && col >= 3) {
                    for (int offset = 0; offset < 4; offset++) {
                        byte position = this.board[getIndex(row - offset, col - offset)];
                        String value;
                        switch (position)
                        {
                            case 0: value = " "; break;
                            case 1: value = "1"; break;
                            case 2: value = "2"; break;
                            default: throw new IllegalStateException("Unexpected value.");
                        }
                        identity.append(value);
                    }
                    totalPoints += scoreIdentityStr(identity.toString(), aiTurn, zeros);
                }

                identity = new StringBuilder();
                if (row >= 3 && col <= this.WIDTH - 4) {
                    for (int offset = 0; offset < 4; offset++)
                    {
                        byte position = this.board[getIndex(row - offset, col + offset)];
                        String value;
                        switch (position)
                        {
                            case 0: value = " "; break;
                            case 1: value = "1"; break;
                            case 2: value = "2"; break;
                            default: throw new IllegalStateException("Unexpected value.");
                        }
                        identity.append(value);
                    }
                    totalPoints += scoreIdentityStr(identity.toString(), aiTurn, zeros);
                }

            }
        return totalPoints;

    }

    public int scoreIdentityStr(String identity, boolean aiTurn, int zeros){

        int playerPoints;
        int aiPoints;

        switch(identity){
            default: playerPoints = 0; break;

            // Two cases.
            case " 11 ": playerPoints = 5; break;
            case "1  1": playerPoints = 3; break;
            case "11  ": playerPoints = 2; break;
            case "11 2": playerPoints = 1; break;
            case "  11": playerPoints = 2; break;
            case "2 11": playerPoints = 1; break;

            // Three cases.
            case "111 ": playerPoints = 7; break;
            case " 111": playerPoints = 7; break;
            case "1 11": playerPoints = 7; break;
            case "11 1": playerPoints = 7; break;

            case "1111": playerPoints = 100 * zeros; break;
        };
        switch(identity){
            default: aiPoints = 0; break;

            // Two cases.
            case " 22 ": aiPoints = 5; break;
            case "2  2": aiPoints = 3; break;
            case "22  ": aiPoints = 2; break;
            case "22 1": aiPoints = 1; break;
            case "  22": aiPoints = 2; break;
            case "1 22": aiPoints = 1; break;

            // Three cases.
            case "222 ": aiPoints = 7; break;
            case " 222": aiPoints = 7; break;
            case "2 22": aiPoints = 7; break;
            case "22 2": aiPoints = 7; break;

            case "2222": aiPoints = 100 * zeros; break;
        };

        if(aiTurn) return playerPoints - aiPoints;
        else return aiPoints - playerPoints;

    }



    private void getWinningTotals(int zeros){

        final byte COIN = 2;
        this.anticipatoryMoveWeight = new int[super.WIDTH];

        for(int col = 0; col < super.WIDTH; col++)
            if(super.colIsOpen(col))
            {

                super.placeCoin(col, COIN);

                anticipatoryMoveWeight[col] += this.evaluateBoard(true, zeros);

                this.undoLastMove(col);
            }


    }

    private void undoLastMove(int col){

        for(int i = super.HEIGHT - 1; i >= 0; i--)
        {
            int index = super.getIndex(i, col);
            if(super.board[index] != 0)
            {
                super.board[index] = 0;
                return;
            }
        }

    }


    private int filledColumns(){

        int total = 0;
        for(int i = 0; i < super.WIDTH; i++)
            if(!super.colIsOpen(i))
                total++;
        return total;

    }


    public int zeroSum(){

        int total = 0;
        for(byte b : super.board)
            if(b == 0)
                total++;
        return total;

    }

    private int[] getDistributionOrder(){


        int[] identities = new int[super.WIDTH];
        for(int i = 0; i < identities.length; i++)
            identities[i] = i;

        for (int n = 0; n < this.anticipatoryMoveWeight.length; n++)
            for (int j = 0; j < this.anticipatoryMoveWeight.length - n - 1; j++)
                if (this.anticipatoryMoveWeight[j] < this.anticipatoryMoveWeight[j + 1]) {
                    int swapString = this.anticipatoryMoveWeight[j];
                    this.anticipatoryMoveWeight[j] = this.anticipatoryMoveWeight[j + 1];
                    this.anticipatoryMoveWeight[j + 1] = swapString;
                    int swapInt = identities[j];
                    identities[j] = identities[j + 1];
                    identities[j + 1] = swapInt;
                }
        return identities;

    }

    private boolean testWin(int col, byte playerCode){

        boolean victory = false;
        if (super.colIsOpen(col)) {
            super.placeCoin(col, playerCode);
            victory = super.hasWon();
            this.undoLastMove(col);
        }
        return victory;

    }


}
