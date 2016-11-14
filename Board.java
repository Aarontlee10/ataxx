package ataxx;

/* Author: Aaron Lee */

import java.util.Arrays;
import java.util.Stack;
import java.util.Observable;
import java.util.ArrayList;
import java.util.HashMap;

import static ataxx.PieceColor.*;
import static ataxx.GameException.error;

/** An Ataxx board.   The squares are labeled by column (a char value between
 *  'a' - 2 and 'g' + 2) and row (a char value between '1' - 2 and '7'
 *  + 2) or by linearized index, an integer described below.  Values of
 *  the column outside 'a' and 'g' and of the row outside '1' to '7' denote
 *  two layers of border squares, which are always blocked.
 *  This artificial border (which is never actually printed) is a common
 *  trick that allows one to avoid testing for edge conditions.
 *  For example, to look at all the possible moves from a square, sq,
 *  on the normal board (i.e., not in the border region), one can simply
 *  look at all squares within two rows and columns of sq without worrying
 *  about going off the board. Since squares in the border region are
 *  blocked, the normal logic that prevents moving to a blocked square
 *  will apply.
 *
 *  For some purposes, it is useful to refer to squares using a single
 *  integer, which we call its "linearized index".  This is simply the
 *  number of the square in row-major order (counting from 0).
 *
 *  Moves on this board are denoted by Moves.
 *  @author Aaron Lee
 */
class Board extends Observable {

    /** Number of squares on a side of the board. */
    static final int SIDE = 7;
    /** Length of a side + an artificial 2-deep border region. */
    static final int EXTENDED_SIDE = SIDE + 4;

    /** Number of non-extending moves before game ends. */
    static final int JUMP_LIMIT = 25;

    /** A new, cleared board at the start of the game. */
    Board() {
        _whoseMove = RED;
        _board = new PieceColor[EXTENDED_SIDE * EXTENDED_SIDE];
        _numConsJumpsTotal = 0;
        _bluePieces = 2;
        _redPieces = 2;
        _jumps = new Stack<Integer>();
        _undoStack = new Stack<Move>();
        _changed = new HashMap<Move, ArrayList<Integer>>();
        clear();
    }

    /** A copy of B. */
    Board(Board b) {
        _numConsJumpsTotal = b.numJumps();
        _board = b._board.clone();
        _whoseMove = b.whoseMove();
        _jumps =  b.jumps();
        _bluePieces = b.bluePieces();
        _undoStack = b.getUndo();
        _changed = b.getChanged();
        _redPieces = b.redPieces();
    }

    /** Return the linearized index of square COL ROW. */
    static int index(char col, char row) {
        return (row - '1' + 2) * EXTENDED_SIDE + (col - 'a' + 2);
    }

    /** Return the linearized index of the square that is DC columns and DR
     *  rows away from the square with index SQ. */
    static int neighbor(int sq, int dc, int dr) {
        return sq + dc + dr * EXTENDED_SIDE;
    }

    /** Clear me to my starting state, with pieces in their initial
     *  positions and no blocks. */
    void clear() {
        _numConsJumpsTotal = 0;
        _redPieces = 2;
        _bluePieces = 2;
        _whoseMove = RED;
        _undoStack.clear();
        _changed.clear();
        for (int i = 0; i < _board.length; i++) {
            _board[i] = EMPTY;
            if (i % 11 == 0 || i % 11 == 1
                    || i % 11 == aa || i % 11 == ab) {
                _board[i] = BLOCKED;
            } else if (i <= ac || i >= ad) {
                _board[i] = BLOCKED;
            }
        }
        _board[ae] = BLUE;
        _board[af] = RED;
        _board[ag] = BLUE;
        _board[ah] = RED;
        setChanged();
        notifyObservers();
    }



    /** Return true iff the game is over: i.e., if neither side has
     *  any moves, if one side has no pieces, or if there have been
     *  MAX_JUMPS consecutive jumps without intervening extends. */
    boolean gameOver() {
        return ((!canMove(RED) && !canMove(BLUE)) || redPieces() == 0
                || bluePieces() == 0 || _numConsJumpsTotal >= JUMP_LIMIT);
    }

    /** Return number of red pieces on the board. */
    int redPieces() {
        return numPieces(RED);
    }

    /** Return number of blue pieces on the board. */
    int bluePieces() {
        return numPieces(BLUE);
    }

    /** Return number of COLOR pieces on the board. */
    private int numPieces(PieceColor color) {
        String colorString = color.toString();
        if (colorString.equals("Red")) {
            return _redPieces;
        } else {
            return _bluePieces;
        }
    }

    /** Increment numPieces(COLOR) by K. */
    private void incrPieces(PieceColor color, int k) {
        String colorString = color.toString();
        if (colorString.equals("Red")) {
            _redPieces += k;
        } else if (colorString.equals("Blue")) {
            _bluePieces += k;
        }
    }

    /** The current contents of square CR, where 'a'-2 <= C <= 'g'+2, and
     *  '1'-2 <= R <= '7'+2.  Squares outside the range a1-g7 are all
     *  BLOCKED.  Returns the same value as get(index(C, R)). */
    PieceColor get(char c, char r) {
        return _board[index(c, r)];
    }

    /** Return the current contents of square with linearized index SQ. */
    PieceColor get(int sq) {
        return _board[sq];
    }

    /** Return true iff MOVE is legal on the current board. */
    boolean legalMove(Move move) {
        int moveFromIndex = index(move.col0(), move.row0());
        int moveToIndex = index(move.col1(), move.row1());
        int distance = Math.abs(moveFromIndex - moveToIndex);
        int columnDistance =
                Math.abs((moveToIndex % 11) - (moveFromIndex % 11));
        return (_board[moveFromIndex].equals(_whoseMove)
                && _board[moveToIndex].equals(EMPTY)
                && distance <= ae
                && distance > 0
                && columnDistance <= 2
                && columnDistance >= 0);
    }

    /** Return true iff player WHO can move, ignoring whether it is
     *  that player's move and whether the game is over.*/
    boolean canMove(PieceColor who) {
        for (int i = ae; i <= ag; i++) {
            if (_board[i].equals(who)) {
                for (int j = 0; j < 3; j++) {
                    int x = i + ab;
                    int y = i - aj;
                    int z = i - 1;
                    if (_board[x + j].equals(EMPTY)
                            || _board[y + j].equals(EMPTY)
                            || _board[z + j].equals(EMPTY)) {
                        return true;
                    }
                }
                for (int j = 0; j < 5; j++) {
                    int alpha = i - ae;
                    int beta = i - ai;
                    int gamma = i - 2;
                    int delta = i + aa;
                    int epsilon = i + ak;
                    if (_board[alpha + j].equals(EMPTY)
                            || _board[beta + j].equals(EMPTY)
                            || _board[gamma + j].equals(EMPTY)
                            || _board[delta + j].equals(EMPTY)
                            || _board[epsilon + j].equals(EMPTY)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Return the color of the player who has the next move.  The
     *  value is arbitrary if gameOver(). */
    PieceColor whoseMove() {
        return _whoseMove;
    }

    /** Return total number of moves and passes since the last
     *  clear or the creation of the board. */
    int numMoves() {
        return _undoStack.size();
    }

    /** Return number of non-pass moves made in the current game since the
     *  last extend move added a piece to the board (or since the
     *  start of the game). Used to detect end-of-game. */
    int numJumps() {
        return _numConsJumpsTotal;
    }

    /** @return get changed. */
    HashMap<Move, ArrayList<Integer>> getChanged() {
        return  _changed;
    }
    /** @return get undo. */
    Stack<Move> getUndo() {
        return _undoStack;
    }


    /** Perform the move C0R0-C1R1, or pass if C0 is '-'.  For moves
     *  other than pass, assumes that legalMove(C0, R0, C1, R1). */
    void makeMove(char c0, char r0, char c1, char r1) {
        if (c0 == '-') {
            makeMove(new Move());
        } else {
            if (!_board[index(c1, r1)].equals(EMPTY)
                    || !_board[index(c0, r0)].equals(_whoseMove)) {
                throw error("Illegal move.");
            }
            makeMove(Move.move(c0, r0, c1, r1));
        }
    }

    /** Make the MOVE on this Board, assuming it is legal. */
    void makeMove(Move move) {
        if (move == null) {
            throw error("That move is illegal.");
        }
        if (move.isPass()) {
            pass();
            return;
        }
        if (!legalMove(move)) {
            throw error("That move is illegal");
        }
        ArrayList<Integer> indexChanged = new ArrayList<Integer>();
        int pls = index(move.col0(), move.row0());
        int help = index(move.col1(), move.row1());
        _board[help] = _whoseMove;
        if (move.isJump()) {
            _numConsJumpsTotal += 1;
            _board[pls] = EMPTY;
        } else if (move.isExtend()) {
            _jumps.push(_numConsJumpsTotal);
            _numConsJumpsTotal = 0;
            incrPieces(_whoseMove, 1);
        }
        for (int di = -1; di <= 1; di++) {
            for (int dj = -1; dj <= 1; dj++) {
                if (_board[neighbor(help, di, dj)]
                    == _whoseMove.opposite()) {
                    _board[neighbor(help, di, dj)] = _whoseMove;
                    incrPieces(_whoseMove, 1);
                    incrPieces(_whoseMove.opposite(), -1);
                }
            }
        }
        _undoStack.push(move);
        _changed.put(move, indexChanged);
        _whoseMove = _whoseMove.opposite();
        setChanged();
        notifyObservers();
    }

    /** Update to indicate that the current player passes, assuming it
     *  is legal to do so.  The only effect is to change whoseMove(). */
    void pass() {
        if (canMove(_whoseMove)) {
            throw error("You shall not pass! (but you can make a legal move)");
        }
        PieceColor opponent = _whoseMove.opposite();
        Move pass = new Move();
        _undoStack.push(pass);
        _changed.put(pass, null);
        _whoseMove = opponent;
        setChanged();
        notifyObservers();
    }

    /** @return Number of consecutive jumps. */
    Stack<Integer> jumps() {
        return _jumps;
    }

    /** Undo the last move. */
    void undo() {
        if (_undoStack.empty()) {
            throw error("No moves to undo");
        }
        Move move = _undoStack.pop();
        ArrayList<Integer> indexes = _changed.get(move);
        _board[index(move.col0(), move.row0())] =
                _board[index(move.col1(), move.row1())];
        _board[index(move.col1(), move.row1())] = EMPTY;
        if (move.isJump()) {
            _numConsJumpsTotal -= 1;
        } else if (move.isExtend()) {
            _numConsJumpsTotal = _jumps.pop();
        }
        for (Integer index : indexes) {
            _board[index] = _board[index].opposite();
        }
        _whoseMove = _whoseMove.opposite();
        setChanged();
        notifyObservers();
    }

    /** Return true iff it is legal to place a block at C R. */
    boolean legalBlock(char c, char r) {
        int index = index(c, r);
        char newC, newR;
        int reflectedC, reflectedR, reflectedB;
        if (c < 'd') {
            newC = (char) ('d' + ('d' - c));
            reflectedC = index(newC, r);
        } else {
            newC = (char) ('d' - (c - 'd'));
            reflectedC = index(newC, r);
        }
        if (r < '4') {
            newR = (char) ('4' + ('4' - r));
            reflectedR = index(c, newR);
        } else {
            newR = (char) ('4' - (r - '4'));
            reflectedR = index(c, newR);
        }
        reflectedB = index(newC, newR);
        return (_board[index].equals(EMPTY) && _board[reflectedC].equals(EMPTY)
                && _board[reflectedR].equals(EMPTY))
                && _board[reflectedB].equals(EMPTY);
    }

    /** Return true iff it is legal to place a block at CR. */
    boolean legalBlock(String cr) {
        return legalBlock(cr.charAt(0), cr.charAt(1));
    }

    /** Set a block on the square C R and its reflections across the middle
     *  row and/or column, if that square is unoccupied and not
     *  in one of the corners. Has no effect if any of the squares isy
     *  already occupied by a block.  It is an error to place a block on a
     *  piece. */

    void setBlock(char c, char r) {
        if (!legalBlock(c, r)) {
            throw error("illegal block placement");
        }
        int index = index(c, r);
        char newC, newR;
        int reflectedC, reflectedR, reflectedB;
        if (c < 'd') {
            newC = (char) ('d' + ('d' - c));
            reflectedC = index(newC, r);
        } else {
            newC = (char) ('d' - (c - 'd'));
            reflectedC = index(newC, r);
        }
        if (r < '4') {
            newR = (char) ('4' + ('4' - r));
            reflectedR = index(c, newR);
        } else {
            newR = (char) ('4' - (r - '4'));
            reflectedR = index(c, newR);
        }
        reflectedB = index(newC, newR);
        _board[index] = _board[reflectedC] =
                _board[reflectedR] = _board[reflectedB] = BLOCKED;
        setChanged();
        notifyObservers();
    }

    /** Place a block at CR. */
    void setBlock(String cr) {
        setBlock(cr.charAt(0), cr.charAt(1));
    }

    /** Return a list of all moves made since the last clear (or start of
     *  game). */
    ArrayList<Move> allMoves() {
        return new ArrayList<Move>(_undoStack);
    }

    @Override
    public String toString() {
        return toString();
    }

    /* .equals used only for testing purposes. */
    @Override
    public boolean equals(Object obj) {
        Board other = (Board) obj;
        return Arrays.equals(_board, other._board);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(_board);
    }



    /** For reasons of efficiency in copying the board,
     *  we use a 1D array to represent it, using the usual access
     *  algorithm: row r, column c => index(r, c).
     *
     *  Next, instead of using a 7x7 board, we use an 11x11 board in
     *  which the outer two rows and columns are blocks, and
     *  row 2, column 2 actually represents row 0, column 0
     *  of the real board.  As a result of this trick, there is no
     *  need to special-case being near the edge: we don't move
     *  off the edge because it looks blocked.
     *
     *  Using characters as indices, it follows that if 'a' <= c <= 'g'
     *  and '1' <= r <= '7', then row c, column r of the board corresponds
     *  to board[(c -'a' + 2) + 11 (r - '1' + 2) ], or by a little
     *  re-grouping of terms, board[c + 11 * r + SQUARE_CORRECTION]. */
    private PieceColor[] _board;

    /** Numbers of consecutive jumps before clear. */
    private Stack<Integer> _jumps;
    /** Player that is on move. */
    private int _numConsJumpsTotal;
    /** Who's move it is. */
    private PieceColor _whoseMove;
    /** all moves that can be undone. */
    private Stack<Move> _undoStack;
    /** all the red pieces.*/
    private int _redPieces;
    /** all the blue pieces.*/
    private int _bluePieces;
    /** all indexes changed with moves. */
    private HashMap<Move, ArrayList<Integer>> _changed;
    /** Special Index in _board.*/
    private final int aa = 9;
    /** Special Index in _board.*/
    private final int ab = 10;
    /** Special Index in _board.*/
    private final int ac = 19;
    /** Special Index in _board.*/
    private final int ad = 101;
    /** Special Index in _board.*/
    private final int ae = 24;
    /** Special Index in _board.*/
    private final int af = 30;
    /** Special Index in _board.*/
    private final int ag = 96;
    /** Special Index in _board.*/
    private final int ah = 90;
    /** Special Index in _board.*/
    private final int ai = 13;
    /** Special Index in _board.*/
    private final int aj = 12;
    /** Special Index in _board.*/
    private final int ak = 20;

    /** @return Special Index in _board.*/
    int getAa() {
        return aa;
    }
    /** @return Special Index in _board.*/
    int getAb() {
        return ab;
    }
    /** @return Special Index in _board.*/
    int getAc() {
        return ac;
    }
    /** @return Special Index in _board.*/
    int getAd() {
        return ad;
    }
    /** @return Special Index in _board.*/
    int getAe() {
        return ae;
    }
    /** @return Special Index in _board.*/
    int getAf() {
        return af;
    }
    /** @return Special Index in _board.*/
    int getAg() {
        return ag;
    }
    /** @return Special Index in _board.*/
    int getAh() {
        return ah;
    }
    /** @return Special Index in _board.*/
    int getAi() {
        return ai;
    }
    /** @return Special Index in _board.*/
    int getAj() {
        return aj;
    }
    /** @return Special Index in _board.*/
    int getAk() {
        return ak;
    }
}
