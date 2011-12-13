package com.bomber.gamestates;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Rectangle;
import com.bomber.GameScreen;
import com.bomber.common.Assets;
import com.bomber.gameobjects.Player;
import com.bomber.gametypes.GameTypeCampaign;
import com.bomber.input.InputPlayingState;

public class GameStatePlaying extends GameState {

	public GameStatePlaying(GameScreen _gameScreen) {
		super(_gameScreen);

		mInput = new InputPlayingState(this);
	}

	public void update()
	{
		mInput.update();
		mGameWorld.update();
		
		if(mGameWorld.mGameType.isOver())
		{
			if(mGameWorld.mGameType instanceof GameTypeCampaign)
			{
				mGameScreen.setGameState(new GameStateLevelCompleted(mGameScreen));
			}
			
			//TODO : outros gametypes

		}
		else if(mGameWorld.mGameType.isLost())
		{
			mGameScreen.setGameState(new GameStateGameOver(mGameScreen));
		}

	}


	public void present(float _interpolation)
	{
		mWorldRenderer.render();
		mBatcher.setProjectionMatrix(mUICamera.combined);

	
		BitmapFont font = Assets.mFont;
		Player player = mGameWorld.getLocalPlayer();
	
		mBatcher.begin();	
		//desenha imagem do controller
		mBatcher.draw(Assets.mControllerBar,0,0);

		//desenha nivel ao canto
		font.draw(mBatcher,"LEVEL 1", 10 , 470);
		
		//desenha tempo e score		
		if(mGameWorld.mClock != null)
		font.draw(mBatcher, mGameWorld.mClock.toString(), 235, 125);
		//TODO: mPointsAsString n�o � necess�rio??
		font.draw(mBatcher, "SCORE: " + player.getPointsAsString(), 350, 125);
		
		//desenha quantidades de bonus ao fundo
		Integer value;
		
		value = (int) player.mLives;
		font.draw(mBatcher, value.toString(), 285, 47);
		
		value = (int) player.mBombExplosionSize;
		font.draw(mBatcher, value.toString(), 355, 47);
		
		value = (int) player.mMaxSimultaneousBombs;
		font.draw(mBatcher, value.toString(), 430, 47);
	
		value = (int) player.mSpeedFactor;
		font.draw(mBatcher, value.toString(), 500, 47);
		
		//desenha bonus ao canto
		float x = 755;
		float y = 437;		
		boolean drawBonusHand = player.mIsAbleToPushBombs;
		boolean drawBonusShield = player.mIsShieldActive;
		boolean drawBonusStar = player.mPointsMultiplier != 1;
		
		if(drawBonusHand)
		{
			mBatcher.draw(Assets.mBonus.get("bonus_hand").getKeyFrame(0, false), x, y);
			x -= 57;
		}
		if(drawBonusShield)
		{
			mBatcher.draw(Assets.mBonus.get("bonus_shield").getKeyFrame(0, false), x, y);
			x -= 57;
		}
		if(drawBonusStar)
		{
			mBatcher.draw(Assets.mBonus.get("bonus_star").getKeyFrame(0, false), x, y - 5);
			x -= 57;	
		}
		
//		Rectangle[] zones = mInput.getZones();
//		for (int i = 0; i < zones.length; i++)
//			mBatcher.draw(Assets.mAtlas.findRegion("tiles_", 123), zones[i].x, zones[i].y, zones[i].width, zones[i].height);

		Integer fps = Gdx.graphics.getFramesPerSecond();
		Assets.mFont.draw(mBatcher, fps.toString(), 150, 470);
		Assets.mFont.draw(mBatcher, GameScreen.ticksPerSecond.toString(), 200, 470);

		
		mBatcher.end();

		
		
	}


	
}