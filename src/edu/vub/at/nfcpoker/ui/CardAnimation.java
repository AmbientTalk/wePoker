package edu.vub.at.nfcpoker.ui;

import edu.vub.at.nfcpoker.R;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.os.Build;
import android.widget.ImageButton;

public class CardAnimation {
	static boolean isHoneyComb = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB);
	private static ICardAnimation cardInstance = null;

	static public void setCardImage(ImageButton ib, int drawable) {
		getCardAnimation().setCardImage(ib, drawable);
	}

	private static ICardAnimation getCardAnimation() {
		if (cardInstance == null) {
			cardInstance = isHoneyComb ? new CardAnimationHC() : new CardAnimationGB();
		}
		return cardInstance;
	}
	
	
	private interface ICardAnimation {
		public void setCardImage(ImageButton ib, int drawable);
	}
	
	public static class CardAnimationGB implements ICardAnimation {

		@Override
		public void setCardImage(ImageButton ib, int drawable) {
			ib.setImageResource(drawable);
			
		}

	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static class CardAnimationHC implements ICardAnimation {

		@Override
		public void setCardImage(final ImageButton ib, int drawable) {
			ObjectAnimator animX = ObjectAnimator.ofFloat(ib, "scaleX", 1.f, 0.f);
			ObjectAnimator animY = ObjectAnimator.ofFloat(ib, "scaleY", 1.f, 0.f);
			animX.setDuration(500); animY.setDuration(500);
			final AnimatorSet scalers = new AnimatorSet();
			scalers.play(animX).with(animY);
			scalers.addListener(new AnimatorListenerAdapter() {

				@Override
				public void onAnimationEnd(Animator animation) {
					ib.setScaleX(1.f);
					ib.setScaleY(1.f);
					ib.setImageResource(R.drawable.backside);
				}

			});
			scalers.start();			
		}
	}
}
