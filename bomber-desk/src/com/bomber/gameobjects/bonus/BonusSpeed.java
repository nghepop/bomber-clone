package com.bomber.gameobjects.bonus;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.bomber.common.assets.Assets;
import com.bomber.gameobjects.Player;

public class BonusSpeed extends Bonus {

	public BonusSpeed() {

		Animation anim = Assets.mBonusAnimations.get("bonus_speed");
		setCurrentAnimation(anim, (short) NUMBER_OF_ANIMATION_FRAMES, true, true);
	}

	@Override
	public void applyEffect(Player _player)
	{
		_player.mSpeedFactor++;
		_player.mSpeed += 0.25;
	}
}
