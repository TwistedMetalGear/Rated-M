package net.silentbyte.ratedm.adapters;

import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import net.silentbyte.ratedm.EllipsisView;
import net.silentbyte.ratedm.GameConstants;
import net.silentbyte.ratedm.Match;
import net.silentbyte.ratedm.Player;
import net.silentbyte.ratedm.R;
import net.silentbyte.ratedm.Round;
import net.silentbyte.ratedm.activities.GameActivity;
import net.silentbyte.ratedm.activities.SettingsActivity;

public class PlayerAdapter extends ArrayAdapter<String>
{
    private LayoutInflater mInflater;
    
    // JS: Is there a better way to get the activity rather than passing it to the constructor?
    public PlayerAdapter(Context context, List<String> playerIds)
    {
        super(context, 0, playerIds);
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        GameActivity activity = (GameActivity)getContext();
        Match match = activity.getMatch();
        Round round = match.getRound(activity.getRoundToShow());

        // If we weren't given a view, inflate one.
        if (convertView == null)
        {
            float density = getContext().getResources().getDisplayMetrics().density;

            if (density == 3.5)
                convertView = mInflater.inflate(R.layout.list_item_player_3_5, parent, false);
            else
                convertView = mInflater.inflate(R.layout.list_item_player, parent, false);
        }

        // Correct the height of the rows in the player list.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        int numRows = GameConstants.MAX_NUM_PLAYERS;

        try
        {
            numRows = Integer.parseInt(prefs.getString(SettingsActivity.KEY_PLAYER_LIST_NUM_ROWS, ""));

            if (numRows < 1)
                numRows = 1;

            if (numRows > GameConstants.MAX_NUM_PLAYERS)
                numRows = GameConstants.MAX_NUM_PLAYERS;
        }
        catch (NumberFormatException e) {}

        int height = parent.getMeasuredHeight() / numRows;
        int correction = parent.getMeasuredHeight() % numRows;

        if (position < correction)
            height += 1;

        convertView.getLayoutParams().height = height;

        View playerNameCell = convertView.findViewById(R.id.player_name_cell);
        View playerScoreCell = convertView.findViewById(R.id.player_score_cell);
        View playerWinPercentageCell = convertView.findViewById(R.id.player_win_percentage_cell);
        View playerBetCell = convertView.findViewById(R.id.player_bet_cell);
        TextView playerName = (TextView)convertView.findViewById(R.id.player_name);
        EllipsisView playerEllipsis = (EllipsisView)convertView.findViewById(R.id.player_ellipsis);
        TextView playerJudgeIndicator = (TextView)convertView.findViewById(R.id.player_judge_indicator);
        ImageView playerWinnerIndicator = (ImageView)convertView.findViewById(R.id.player_winner_indicator);
        ImageView playerReadyIndicator = (ImageView)convertView.findViewById(R.id.player_ready_indicator);
        TextView playerAutoPickIndicator = (TextView)convertView.findViewById(R.id.player_auto_pick_indicator);
        TextView playerSkipIndicator = (TextView)convertView.findViewById(R.id.player_skip_indicator);
        TextView playerScore = (TextView)convertView.findViewById(R.id.player_score);
        TextView playerScoreChange = (TextView)convertView.findViewById(R.id.player_score_change);
        TextView playerWinPercentage = (TextView)convertView.findViewById(R.id.player_win_percentage);
        TextView playerBet = (TextView)convertView.findViewById(R.id.player_bet);

        playerJudgeIndicator.setVisibility(View.GONE);
        playerWinnerIndicator.setVisibility(View.GONE);
        playerReadyIndicator.setVisibility(View.GONE);
        playerAutoPickIndicator.setVisibility(View.GONE);
        playerSkipIndicator.setVisibility(View.GONE);

        if (position % 2 == 0)
        {
            playerNameCell.setBackgroundResource(R.drawable.table_cell_dark_border_right);
            playerScoreCell.setBackgroundResource(R.drawable.table_cell_dark_border_right);
            playerWinPercentageCell.setBackgroundResource(R.drawable.table_cell_dark_border_right);
            playerBetCell.setBackgroundResource(R.drawable.table_cell_dark);
        }
        else
        {
            playerNameCell.setBackgroundResource(R.drawable.table_cell_light_border_right);
            playerScoreCell.setBackgroundResource(R.drawable.table_cell_light_border_right);
            playerWinPercentageCell.setBackgroundResource(R.drawable.table_cell_light_border_right);
            playerBetCell.setBackgroundResource(R.drawable.table_cell_light);
        }

        // Configure the view for this player.
        String playerId = getItem(position);
        Player player = round.getPlayers().get(playerId);

        // Booleans to help out with our conditions below.
        boolean isJudge = playerId.equals(round.getJudgeId());
        boolean isRoundWinner = playerId.equals(round.getWinnerId());
        boolean hasSubmittedCards = activity.isCardSubmitted(playerId);
        boolean showingResults = activity.isOnResultsScreen() || activity.isMatchComplete() || activity.isMatchCanceled();
        boolean playerReplacingBot = false;

        // If you prefer to show a bot tag:
        /*
        if (match.getInviteeIds().contains(playerId))
            playerName.setText("[Bot] " + player.getName());
        else
            playerName.setText(player.getName());
        */

        if (playerId.startsWith("bot_") && player.getName().matches("^Bot [0-9]* (.*)$"))
            playerReplacingBot = true;

        if ((match.getInviteeIds().contains(playerId) || match.getPendingLeaveIds().contains(playerId) || !match.getPlayerIds().contains(playerId)) &&
            (showingResults || !playerReplacingBot))
        {
            playerName.setTextColor(ContextCompat.getColor(getContext(), R.color.disabled_text));
            playerEllipsis.setTextColor(ContextCompat.getColor(getContext(), R.color.disabled_text));
        }
        else
        {
            playerName.setTextColor(ContextCompat.getColor(getContext(), R.color.table_row_text));
            playerEllipsis.setTextColor(ContextCompat.getColor(getContext(), R.color.table_row_text));
        }

        if ((match.getPendingLeaveIds().contains(playerId) || (!match.getInviteeIds().contains(playerId) && !match.getPlayerIds().contains(playerId))) &&
            (showingResults || !playerReplacingBot))
        {
            playerName.setPaintFlags(playerName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        }

        playerName.setText(player.getName());
        playerScore.setText(Integer.toString(player.getScore()));
        playerWinPercentage.setText(Integer.toString(player.getWinPercentage()));

        if (showingResults)
        {
            // A player may join the match and replace the bot name, hence we need to regenerate it.
            if (playerReplacingBot)
                playerName.setText("Bot " + playerId.substring(playerId.indexOf("_") + 1));

            if (isJudge)
                playerJudgeIndicator.setVisibility(View.VISIBLE);
            else if  (!player.autoPicked() && !player.skipped())
                playerReadyIndicator.setVisibility(View.VISIBLE);

            if (isRoundWinner)
                playerWinnerIndicator.setVisibility(View.VISIBLE);

            if (player.autoPicked())
                playerAutoPickIndicator.setVisibility(View.VISIBLE);
            else if (player.skipped())
                playerSkipIndicator.setVisibility(View.VISIBLE);

            if (player.getScoreChange() < 0)
            {
                playerScoreChange.setTextColor(ContextCompat.getColor(getContext(), R.color.red));
                playerScoreChange.setText("-" + Math.abs(player.getScoreChange()));
            }
            else if (player.getScoreChange() > 0)
            {
                playerScoreChange.setTextColor(ContextCompat.getColor(getContext(), R.color.green));
                playerScoreChange.setText("+" + player.getScoreChange());
            }

            playerBet.setText(Integer.toString(player.getBet()));
        }
        else
        {
            if ((match.getState() == Match.MATCH_STATE_JUDGING || match.getState() == Match.MATCH_STATE_WRITING_CARD) && isJudge)
                playerEllipsis.startEllipsisCycle();
            else
                playerEllipsis.stopEllipsisCycle();

            if (isJudge)
                playerJudgeIndicator.setVisibility(View.VISIBLE);

            // Not necessary to check player.skipped, but can't hurt.
            if (hasSubmittedCards && !player.autoPicked() && !player.skipped())
                playerReadyIndicator.setVisibility(View.VISIBLE);

            if (player.autoPicked())
                playerAutoPickIndicator.setVisibility(View.VISIBLE);
            else if (player.skipped())
                playerSkipIndicator.setVisibility(View.VISIBLE);

            playerScoreChange.setText("");
            playerBet.setText(Integer.toString(player.getBet()));
        }

        return convertView;
    }
}
