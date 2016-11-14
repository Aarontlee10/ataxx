package ataxx;

import static ataxx.PieceColor.*;

/** A Player that computes its own moves.
 *  @author Aaron Lee
 */
class AI extends Player {

    /** Maximum minimax search depth before going to static evaluation. */
    private static final int MAX_DEPTH = 4;
    /** A position magnitude indicating a win (for red if positive, blue
     *  if negative). */
    private static final int WINNING_VALUE = Integer.MAX_VALUE - 1;
    /** A magnitude greater than a normal value. */
    private static final int INFTY = Integer.MAX_VALUE;

    /** A new AI for GAME that will play MYCOLOR. */
    AI(Game game, PieceColor myColor) {
        super(game, myColor);
    }

    @Override
    Move myMove() {
        _lastFoundMove = null;
        if (!board().canMove(myColor())) {
            System.out.println(myColor() + " passes.");
            return Move.pass();
        }
        Move move = findMove();
        System.out.println(myColor()
                + " moves " + move.toString() + ".");
        return move;
    }

    /** Return a move for me from the current position, assuming there
     *  is a move. */
    private Move findMove() {
        Board b = new Board(board());
        if (myColor() == RED) {
            findMove(b, MAX_DEPTH, true, 1, -INFTY, INFTY);
        } else {
            findMove(b, MAX_DEPTH, true, -1, -INFTY, INFTY);
        }
        return _lastFoundMove;
    }

    /** Used to communicate best moves found by findMove, when asked for. */
    private Move _lastFoundMove;

    /** Find a move from position BOARD and return its value, recording
     *  the move found in _lastFoundMove iff SAVEMOVE. The move
     *  should have maximal value or have value >= BETA if SENSE==1,
     *  and minimal value or value <= ALPHA if SENSE==-1. Searches up to
     *  DEPTH levels before using a static estimate. */
    private int findMove(Board board, int depth, boolean saveMove, int sense,
                         int alpha, int beta) {
        int endVal = 0;
        if (board().gameOver()) {
            return sense * WINNING_VALUE;
        } else if (depth == 0) {
            return staticScore(board);
        }
        for (int i = 'a'; i <= 'g'; i++) {
            for (int j = '1'; j <= '7'; j++) {
                int linearized = Board.index((char) i, (char) j);
                if (board.get(linearized).equals(board.whoseMove())) {
                    for (int di = -2; di <= 2; di++) {
                        for (int dj = -2; dj <= 2; dj++) {
                            int neighborIndex =
                                    Board.neighbor(linearized, di, dj);
                            if (board().get(neighborIndex)
                                    .equals(EMPTY)) {
                                Move move =  Move.move((char) i, (char) j,
                                        (char) (i + di), (char) (j + dj));
                                if (board().legalMove(move)) {
                                    board.makeMove(move);
                                    int heuristicValue =
                                            findMove(board, depth - 1, false,
                                                    sense * -1, alpha, beta);
                                    board.undo();
                                    if (sense == 1) {
                                        if (heuristicValue > alpha) {
                                            alpha = heuristicValue;
                                            endVal = alpha;
                                            if (saveMove) {
                                                _lastFoundMove = move;
                                            }
                                        }
                                    } else {
                                        if (heuristicValue < beta) {
                                            beta = heuristicValue;
                                            endVal = beta;
                                            if (saveMove) {
                                                _lastFoundMove = move;
                                            }
                                        }
                                    }
                                    if (beta <= alpha) {
                                        return endVal;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (_lastFoundMove == null) {
            _lastFoundMove = Move.pass();
        }

        return endVal;
    }

    /** Return a heuristic value for BOARD. */
    private int staticScore(Board board) {
        return board.redPieces() - board().bluePieces();
    }
}
