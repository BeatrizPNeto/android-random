package org.devtcg.games.solitaire.game;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

import org.devtcg.games.solitaire.R;
import org.devtcg.games.solitaire.game.rules.Freecell;
import org.devtcg.games.solitaire.game.rules.Klondike;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class GameManager extends Activity
{
	public static final String TAG = "GameManager";
	
	public static final String PREFS_LAST_GAME = "lastGame";
	
	protected static final String STATE_FILE = "gamestate";
	
	private static final String DEFAULT_GAME = Klondike.TAG;

	protected static final int MENU_NEW_GAME = Menu.FIRST;
	protected static final int MENU_RESTART_GAME = Menu.FIRST + 1;
	protected static final int MENU_CHANGE_RULES = Menu.FIRST + 2;

	private static HashMap<String, Class> mGames = null;

	protected Game mCurrent;

	private FrameLayout mRoot;

	@Override
	public void onCreate(Bundle icicle)
	{
		super.onCreate(icicle);
		setContentView(R.layout.main);

		mRoot = (FrameLayout)findViewById(R.id.root);
		
		registerGames();

		Game game;

		if ((game = tryLoadGame()) == null)
		{
			if ((game = tryNewGame()) == null)
			{
				Toast.makeText(this, "PANIC: Unable to find suitable game class", 
				  Toast.LENGTH_LONG).show();
				finish();
				return;
			}
		}

		switchCurrentGame(game);
	}
	
	private void registerGames()
	{
		if (mGames != null)
			return;

		mGames = new HashMap<String, Class>(2);
		registerGame(Klondike.TAG, Klondike.class);
		registerGame(Freecell.TAG, Freecell.class);
	}

	protected void registerGame(String name, Class game)
	{
		mGames.put(name, game);
	}

	protected Class lookupGame(String name)
	{
		return mGames.get(name);
	}

	public Game tryLoadGame()
	{
		FileInputStream inf = null;
		GameInputStream in = null;
		
		try {
			inf = openFileInput(STATE_FILE);
			in = new GameInputStream(inf);
			
			String ruleName = in.readUTF();
			long seed = in.readLong();

			Class gameClass;

			if ((gameClass = lookupGame(ruleName)) == null)
			{
				Log.d(TAG, "Game '" + ruleName + "' not found, weird.");
				return null;
			}

			Game game = (Game)gameClass.newInstance();
			game.init(this);
			game.setSeed(seed);

			if (game.loadGame(in) == false)
				return null;

			return game;
		} catch (Exception e) {
			Log.d(TAG, "Unable to load saved game from " + STATE_FILE, e);
			return null;
		} finally {
			if (in != null)
				try { in.close(); } catch (IOException e) {}
			else if (inf != null)
				try { inf.close(); } catch (IOException e) {}
		}
	}

	public Game tryNewGame()
	{
		SharedPreferences prefs = getSharedPreferences(TAG, MODE_PRIVATE);
		String ruleName = prefs.getString(PREFS_LAST_GAME, DEFAULT_GAME);

		Class gameClass;

		if ((gameClass = lookupGame(ruleName)) == null)
		{
			Log.d(TAG, "Game '" + ruleName + "' not found, weird.");
			
			if ((gameClass = lookupGame(DEFAULT_GAME)) == null)
				return null;
		}

		try {
			Game game = (Game)gameClass.newInstance();
			game.init(this);
			game.newGame();
			return game;
		} catch (Exception e) {
			Log.d(TAG, "Unable to instantiate game '" + ruleName + "'", e);
			return null;
		}
	}

	private void switchCurrentGame(Game game)
	{
		setTitle(game.getName()); 

		mRoot.removeAllViews();

		mCurrent = game;

		View root = mCurrent.getGameView();
		mRoot.addView(root, new LayoutParams(LayoutParams.FILL_PARENT,
		  LayoutParams.FILL_PARENT));
	}

	@Override
	protected void onPause()
	{
		FileOutputStream outf = null;
		GameOutputStream out = null;

		try {
			outf = openFileOutput(STATE_FILE, MODE_PRIVATE);
			out = new GameOutputStream(outf);

			out.writeUTF(mCurrent.getName());
			out.writeLong(mCurrent.getSeed());
			mCurrent.saveGame(out);
			out.close();
		} catch (IOException e) {
			Log.d(TAG, "Unable to save game state", e);
		} finally {
			if (out != null)
				try { out.close(); } catch (IOException e) {}
			else if (outf != null)
				try { outf.close(); } catch (IOException e) {}
		}

		super.onPause();
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
    	super.onCreateOptionsMenu(menu);

    	menu.add(0, MENU_NEW_GAME, Menu.NONE, "New Game");
    	menu.add(0, MENU_RESTART_GAME, Menu.NONE, "Restart Game");
    	menu.add(0, MENU_CHANGE_RULES, Menu.NONE, "Choose Game");
    	
    	return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
    	switch (item.getItemId())
    	{
    	case MENU_NEW_GAME:
    		deleteFile(STATE_FILE);
    		mCurrent.newGame();
    		return true;
    	case MENU_RESTART_GAME:
    		deleteFile(STATE_FILE);
    		mCurrent.newGame(mCurrent.getSeed());
    		return true;
    	case MENU_CHANGE_RULES:
    		ChangeRulesDialog.show(this, "Choose game rules", mGames, mChangeRules);
    		return true;
    	}

    	return super.onOptionsItemSelected(item);
    }
    
    private final ChangeRulesDialog.OnChangeListener mChangeRules = new ChangeRulesDialog.OnChangeListener() {
    	public void onChange(DialogInterface dialog, String ruleName)
    	{
    		dialog.dismiss();
    		deleteFile(STATE_FILE);

    		Class<Game> ruleClass = lookupGame(ruleName);
    		Game rules;

			try {
				rules = ruleClass.newInstance();
	    		rules.init(GameManager.this);
	    		rules.newGame();

	    		switchCurrentGame(rules);
			} catch (Exception e) {
				Log.d(TAG, "Unable to load game '" + ruleName + "'", e);
			}    		
    	}
    };

    public void onWin(Game game)
    {
    	assert mCurrent == game;

		Toast.makeText(this, "You WIN!", Toast.LENGTH_LONG).show();
		game.newGame();
    }

    private static class ChangeRulesDialog extends Dialog
    {
    	private String[] mChoices;  
    	private String mTitle;
    	private OnChangeListener mChangeListener;

    	public ChangeRulesDialog(Context ctx)
    	{
    		super(ctx);
    	}

    	public void setChoices(HashMap<String, Class> games)
    	{
    		Set<String> names = games.keySet();
    		mChoices = new String[names.size()];
    		names.toArray(mChoices);
    		Arrays.sort(mChoices);
    	}

    	public void setTitle(String title)
    	{
    		super.setTitle(title);
    		mTitle = title;
    	}

    	public void setOnChangeListener(OnChangeListener listener)
    	{
    		mChangeListener = listener;
    	}

    	public static ChangeRulesDialog show(Context ctx, String title,
    	  HashMap<String, Class> games, OnChangeListener listener)
    	{
    		ChangeRulesDialog dialog = new ChangeRulesDialog(ctx);

    		dialog.setTitle(title);
    		dialog.setChoices(games);
    		dialog.setOnChangeListener(listener);
    		dialog.setCancelable(true);
    		dialog.show();

    		return dialog;
    	}

    	public void onStart()
    	{
    		if (TextUtils.isEmpty(mTitle) == true)
    			requestWindowFeature(Window.FEATURE_NO_TITLE);

    		setContentView(R.layout.change_rules_dialog);

    		ListView list = (ListView)findViewById(R.id.rules);
    		list.setAdapter(new ArrayAdapter<String>(getContext(),
    		  android.R.layout.simple_list_item_1, mChoices));
    		list.setOnItemClickListener(mItemClicked);
    	}
    	
    	private final OnItemClickListener mItemClicked = new OnItemClickListener()
    	{
			public void onItemClick(AdapterView adapter, View v, int pos, long id)
			{
				if (mChangeListener != null)
				{
					mChangeListener.onChange(GameManager.ChangeRulesDialog.this,
					  mChoices[pos]);
				}
			}
    	};

    	public static interface OnChangeListener
    	{
    		void onChange(DialogInterface dialog, String ruleName);
    	}
    }
}
