package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.Enchanting;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.PurpleParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfMagicMapping;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBag;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;
import java.util.Iterator;

public class Debugger extends Item{
    private static final String AC_UP = "UPGRADE";
    private static final String AC_CHARGE = "RECHARGE";
    private static final String AC_NEXT = "NEXT";
    private static final String AC_MAP = "MAP";
    {
        image = ItemSpriteSheet.BEACON;

        stackable = false;

        bones = false;

        unique = true;

        defaultAction = AC_UP;
    }
    @Override
    public ArrayList<String> actions(Hero hero ) {
        ArrayList<String> actions = super.actions( hero );
        actions.add(AC_UP);
        actions.add(AC_CHARGE);
        actions.add(AC_NEXT);
        actions.add(AC_MAP);
        return actions;
    }

    @Override
    public void execute( Hero hero, String action ) {

        super.execute( hero, action );

        if (action.equals(AC_CHARGE)) {
            curUser = hero;
            Dungeon.hero.belongings.charge(1000);
        }else if(action.equals(AC_UP)) {
            curUser = hero;
            GameScene.selectItem(itemSelector, WndBag.Mode.UPGRADEABLE, Messages.get(this, "prompt"));
        }else if(action.equals(AC_NEXT)){
            InterlevelScene.mode = InterlevelScene.Mode.DESCEND;
            Game.switchScene( InterlevelScene.class );
        }else if(action.equals(AC_MAP)){
            new ScrollOfMagicMapping().doRead();
        }
    }

    @Override
    public boolean isUpgradable() {
        return false;
    }

    @Override
    public boolean isIdentified() {
        return true;
    }

    @Override
    public int value() {
        return 30 * quantity;
    }
    private final WndBag.Listener itemSelector = item -> {
        if (item != null) {
            if(item.isUpgradable()){
                item.upgrade(100);
                curUser.sprite.emitter().start( Speck.factory( Speck.UP ), 0.2f, 3 );
            }
        }
    };
}
