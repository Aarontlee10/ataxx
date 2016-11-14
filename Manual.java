package ataxx;


/** A Player that receives its moves from its Game's getMoveCmnd method.
 *  @author Aaron Lee
 */
class Manual extends Player {

    /** A Player that will play MYCOLOR on GAME, taking its moves from
     *  GAME. */
    Manual(Game game, PieceColor myColor) {
        super(game, myColor);
    }

    @Override
    Move myMove() {
        Command moveCommand = game().getMoveCmnd(myColor().toString() + ": ");
        if (moveCommand.commandType().equals(Command.Type.PIECEMOVE)) {
            String[] arguments = moveCommand.operands();
            char c0 = arguments[0].charAt(0);
            char r0 = arguments[1].charAt(0);
            char c1 = arguments[2].charAt(0);
            char r1 = arguments[3].charAt(0);
            return Move.move(c0, r0, c1, r1);
        } else if (moveCommand.commandType().equals(Command.Type.QUIT)) {
            return new Move();
        }
        return null;
    }
}

