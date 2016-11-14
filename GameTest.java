package ataxx;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Created by Aarontlee10 on 11/9/16.
 * A Game junit test.
 */
public class GameTest {
    Board board = new Board();
    CommandSources commands = new CommandSources();
    Reporter reporter = new TextReporter();
    Game game = new Game(board, commands, reporter);
    String[] array = {""};

    @Test
    public void testDoCommands() {
        String[] moveArray = {"a", "7", "a", "6"};
        game.doMove(moveArray);
        assertTrue(board.get('a', '6').equals(PieceColor.RED));
        String[] moveArray2 = {"a", "1", "a", "2"};
        game.doMove(moveArray2);
        assertTrue(board.get('a', '2').equals(PieceColor.BLUE));
    }
    @Test
    public void testBoardMechanics() {
        board.makeMove('a', '7', 'b', '7');
        board.makeMove('g', '7', 'e', '6');
        board.makeMove('b', '7', 'a', '5');
        board.makeMove('e', '6', 'e', '5');
        board.undo();
        assertTrue(board.get(72).equals(PieceColor.EMPTY));
        board.undo();
        board.undo();
        board.undo();
        assertFalse(board.get(91).equals(PieceColor.BLUE));
    }
    @Test
    public void gameProcessTest() {
        game.doSeed(null);
        game.doDump(null);
    }


}
