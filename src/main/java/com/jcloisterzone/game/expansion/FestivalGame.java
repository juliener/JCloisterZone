package com.jcloisterzone.game.expansion;

import java.util.List;

import org.w3c.dom.Element;

import com.jcloisterzone.PlayerRestriction;
import com.jcloisterzone.action.PlayerAction;
import com.jcloisterzone.action.UndeployAction;
import com.jcloisterzone.board.Tile;
import com.jcloisterzone.board.TileTrigger;
import com.jcloisterzone.collection.Sites;
import com.jcloisterzone.figure.Meeple;
import com.jcloisterzone.game.GameExtension;

public class FestivalGame extends GameExtension {

    @Override
    public void initTile(Tile tile, Element xml) {
        if (xml.getElementsByTagName("festival").getLength() > 0) {
            tile.setTrigger(TileTrigger.FESTIVAL);
        }
    }

    @Override
    public void prepareActions(List<PlayerAction> actions, Sites commonSites) {
        if (getTile().getTrigger() != TileTrigger.FESTIVAL) return;

        UndeployAction action = new UndeployAction("festival", PlayerRestriction.only(getGame().getActivePlayer()));

        for(Meeple m : getGame().getActivePlayer().getFollowers()) {
            if (m.isDeployed()) {
                action.getOrCreate(m.getPosition()).add(m.getLocation());
            }
        }
        for(Meeple m : getGame().getActivePlayer().getSpecialMeeples()) {
            if (m.isDeployed()) {
                //TODO verify barn
                action.getOrCreate(m.getPosition()).add(m.getLocation());
            }
        }
        if (! action.getSites().isEmpty()) {
            actions.add(action);
        }
    }

}
