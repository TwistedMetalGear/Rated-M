package net.silentbyte.ratedm;

public class GameConstants
{
    public static final int VERSION = 1; // Client version.
    public static final int MIN_NUM_PLAYERS = 3; // Minimum # of players.
    public static final int MAX_NUM_PLAYERS = 8; // Maximum # of players.
    public static final int MAX_PLAYER_NAME_LENGTH = 16; // Maximum player name length.
    public static final int MAX_MATCH_NAME_LENGTH = 32; // Maximum match name length.
    public static final int CARD_HAND_SIZE = 10; // # cards dealt to each player.
    public static final int DRAW_BLANK_CHANCE = 10; // Chances of drawing a blank card.
    public static final int POINT_VALUE = 10; // # of points awarded for winning round.
    public static final int POINTS_TO_WIN = 100; // # of points required to win match.
    public static final int CARD_ROTATION = 20; // Rotation angle of the outer white cards.
    public static final boolean PAID = false; // Whether or not this is the paid version of the game.

    // Result codes
    public static final int RESULT_EXPIRED = 1;
    public static final int RESULT_REMOVED = 2;
    public static final int RESULT_LOAD_FAILED = 3;
}
