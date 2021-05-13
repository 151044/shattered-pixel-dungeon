package com.shatteredpixel.shatteredpixeldungeon.items.wands;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Freezing;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frost;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Chilling;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.ConeAOE;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class WandOfSnowstorm extends DamageWand {
    private static final ItemSprite.Glowing color = new ItemSprite.Glowing( 0x88EEFF );

    {
        image = ItemSpriteSheet.WAND_FROST;

        collisionProperties = Ballistica.STOP_SOLID | Ballistica.IGNORE_SOFT_SOLID;
    }

    //1x/2x/3x damage
    public int min(int lvl) {
        return (1 + lvl) * chargesPerCast();
    }

    //1x/2x/3x damage
    public int max(int lvl) {
        return (6 + 2 * lvl) * chargesPerCast();
    }

    ConeAOE cone;

    @Override
    protected void onZap(Ballistica bolt) {
        //Copied verbatim from fireblast again
        //Should change the comments later

        ArrayList<Char> affectedChars = new ArrayList<>();
        ArrayList<Integer> adjacentCells = new ArrayList<>();
        for (int cell : cone.cells) {

            //ignore caster cell
            if (cell == bolt.sourcePos) {
                continue;
            }

            //knock doors open
            if (Dungeon.level.map[cell] == Terrain.DOOR) {
                Level.set(cell, Terrain.OPEN_DOOR);
                GameScene.updateMap(cell);
            }

            //only ignite cells directly near caster if they are flammable
            if (Dungeon.level.adjacent(bolt.sourcePos, cell) && !Dungeon.level.water[cell]) {
                adjacentCells.add(cell);
            } else {
                GameScene.add(Blob.seed(cell, 1 + chargesPerCast(), Freezing.class));
            }

            Char ch = Actor.findChar(cell);
            if (ch != null) {
                affectedChars.add(ch);
            }
        }

        //ignite cells that share a side with an adjacent cell, are flammable, and are further from the source pos
        //This prevents short-range casts not igniting barricades or bookshelves
        for (int cell : adjacentCells) {
            for (int i : PathFinder.NEIGHBOURS4) {
                if (Dungeon.level.trueDistance(cell + i, bolt.sourcePos) > Dungeon.level.trueDistance(cell, bolt.sourcePos)
                        && Dungeon.level.water[cell + i]
                        && Freezing.volumeAt(cell + i, Freezing.class) == 0) {
                    GameScene.add(Blob.seed(cell + i, 1 + chargesPerCast(), Freezing.class));
                }
            }
        }

        for (Char ch : affectedChars) {
            processSoulMark(ch, chargesPerCast());
            ch.damage(damageRoll(), this);
            if (ch.isAlive()) {
                Buff.affect(ch, Chill.class, 2+buffedLvl());
                switch (chargesPerCast()) {
                    case 1:
                        break; //no effects
                    case 2:
                        Buff.affect(ch, Chill.class, 4f);
                        break;
                    case 3:
                        Buff.affect(ch, Frost.class, 4f);
                        break;
                }
            }
        }
    }

    @Override
    public void onHit(MagesStaff staff, Char attacker, Char defender, int damage) {
        //acts like chilling enchantment
        new Chilling().proc(staff, attacker, defender, damage);
    }

    @Override
    protected void fx(Ballistica bolt, Callback callback) {
        //Copied verbatim from wand of fireblast
        //need to perform flame spread logic here so we can determine what cells to put flames in.

        // 5/7/9 distance
        int maxDist = 3 + 2 * chargesPerCast();
        int dist = Math.min(bolt.dist, maxDist);

        cone = new ConeAOE(bolt,
                maxDist,
                30 + 20 * chargesPerCast(),
                collisionProperties | Ballistica.STOP_TARGET);

        //cast to cells at the tip, rather than all cells, better performance.
        for (Ballistica ray : cone.rays) {
            ((MagicMissile) curUser.sprite.parent.recycle(MagicMissile.class)).reset(
                    MagicMissile.FROST_CONE,
                    curUser.sprite,
                    ray.path.get(ray.dist),
                    null
            );
        }

        //final zap at half distance, for timing of the actual wand effect
        MagicMissile.boltFromChar(curUser.sprite.parent,
                MagicMissile.FROST_CONE,
                curUser.sprite,
                bolt.path.get(dist / 2),
                callback);
        Sample.INSTANCE.play(Assets.Sounds.ZAP);
        //Is there a freezing sound of some sort?
        Sample.INSTANCE.play(Assets.Sounds.BURNING);
    }

    @Override
    protected int chargesPerCast() {
        //consumes 30% of current charges, rounded up, with a minimum of one.
        return Math.max(1, (int) Math.ceil(curCharges * 0.3f));
    }

    @Override
    public String statsDesc() {
        if (levelKnown)
            return Messages.get(this, "stats_desc", chargesPerCast(), min(), max());
        else
            return Messages.get(this, "stats_desc", chargesPerCast(), min(0), max(0));
    }

    @Override
    public void staffFx(MagesStaff.StaffParticle particle) {
        particle.color(0x88CCFF);
        particle.am = 0.6f;
        particle.setLifespan(2f);
        float angle = Random.Float(PointF.PI2);
        particle.speed.polar( angle, 2f);
        particle.acc.set( 0f, 1f);
        particle.setSize( 0f, 1.5f);
        particle.radiateXY(Random.Float(1f));
    }
    @Override
    public ItemSprite.Glowing glowing() {
        return color;
    }
}
