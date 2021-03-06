package com.jcloisterzone.game.capability;

import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.google.common.collect.Sets;
import com.jcloisterzone.Expansion;
import com.jcloisterzone.Player;
import com.jcloisterzone.XmlUtils;
import com.jcloisterzone.board.Position;
import com.jcloisterzone.board.Tile;
import com.jcloisterzone.board.TileTrigger;
import com.jcloisterzone.event.GameEventAdapter;
import com.jcloisterzone.game.Game;
import com.jcloisterzone.game.GameExtension;

public class DragonCapability extends GameExtension {

    public static final int DRAGON_MOVES = 6;

    public Position dragonPosition;
    public int dragonMovesLeft;
    public Player dragonPlayer;
    public Set<Position> dragonVisitedTiles;


    @Override
    public void setGame(Game game) {
        super.setGame(game);

        game.addGameListener(new GameEventAdapter() {
            @Override
            public void tilePlaced(Tile tile) {
                if (tile.getTrigger() == TileTrigger.VOLCANO) {
                    setDragonPosition(tile.getPosition());
                    getTilePack().activateGroup("dragon");
                    getGame().fireGameEvent().dragonMoved(tile.getPosition());
                }
            }
        });
    }

    @Override
    public String getTileGroup(Tile tile) {
        return (tile.getTrigger() == TileTrigger.DRAGON) ? "dragon" : null;
    }

    @Override
    public void initTile(Tile tile, Element xml) {
        if (xml.getElementsByTagName("volcano").getLength() > 0) {
            tile.setTrigger(TileTrigger.VOLCANO);
        }
        if (xml.getElementsByTagName("dragon").getLength() > 0) {
            tile.setTrigger(TileTrigger.DRAGON);
        }
    }

    public Position getDragonPosition() {
        return dragonPosition;
    }

    public void setDragonPosition(Position dragonPosition) {
        this.dragonPosition = dragonPosition;
    }

    public Player getDragonPlayer() {
        return dragonPlayer;
    }
    public void setDragonPlayer(Player dragonPlayer) {
        this.dragonPlayer = dragonPlayer;
    }

    public int getDragonMovesLeft() {
        return dragonMovesLeft;
    }

    public Set<Position> getDragonVisitedTiles() {
        return dragonVisitedTiles;
    }

    public void triggerDragonMove() {
        dragonMovesLeft = DRAGON_MOVES;
        dragonPlayer = game.getTurnPlayer();
        dragonVisitedTiles = Sets.newHashSet();
        dragonVisitedTiles.add(dragonPosition);
    }

    public void endDragonMove() {
        dragonMovesLeft = 0;
        dragonVisitedTiles = null;
        dragonPlayer = null;
    }

    public void moveDragon(Position p) {
        dragonVisitedTiles.add(p);
        dragonPosition = p;
        dragonPlayer = game.getNextPlayer(dragonPlayer);
        dragonMovesLeft--;
    }

    public Set<Position> getAvailDragonMoves() {
        Set<Position> result = Sets.newHashSet();
        FairyCapability fairyCap = game.getFairyCapability();
        for (Position offset: Position.ADJACENT.values()) {
            Position position = dragonPosition.add(offset);
            Tile tile = getBoard().get(position);
            if (tile == null || tile.isForbidden()) continue;
            if (dragonVisitedTiles != null && dragonVisitedTiles.contains(position)) { continue; }
            if (fairyCap != null && position.equals(fairyCap.getFairyPosition())) { continue; }
            result.add(position);
        }
        return result;
    }

    @Override
    public DragonCapability copy() {
        DragonCapability copy = new DragonCapability();
        copy.game = game;
        copy.dragonPosition = dragonPosition;
        copy.dragonMovesLeft = dragonMovesLeft;
        copy.dragonPlayer = dragonPlayer;
        if (dragonVisitedTiles != null) copy.dragonVisitedTiles = Sets.newHashSet(dragonVisitedTiles);
        return copy;
    }

    @Override
    public void saveToSnapshot(Document doc, Element node, Expansion nodeFor) {
        if (dragonPosition != null) {
            Element dragon = doc.createElement("dragon");
            XmlUtils.injectPosition(dragon, dragonPosition);
            if (dragonMovesLeft > 0) {
                dragon.setAttribute("moves", "" + dragonMovesLeft);
                dragon.setAttribute("movingPlayer", "" + dragonPlayer.getIndex());
                if (dragonVisitedTiles != null) {
                    for(Position visited : dragonVisitedTiles) {
                        Element ve = doc.createElement("visited");
                        XmlUtils.injectPosition(ve, visited);
                        dragon.appendChild(ve);
                    }
                }
            }
            node.appendChild(dragon);
        }
    }

    @Override
    public void loadFromSnapshot(Document doc, Element node) {
        NodeList nl = node.getElementsByTagName("dragon");
        if (nl.getLength() > 0) {
            Element dragon = (Element) nl.item(0);
            dragonPosition = XmlUtils.extractPosition(dragon);
            game.fireGameEvent().dragonMoved(dragonPosition);
            if (dragon.hasAttribute("moves")) {
                dragonMovesLeft  = Integer.parseInt(dragon.getAttribute("moves"));
                dragonPlayer = game.getPlayer(Integer.parseInt(dragon.getAttribute("movingPlayer")));
                dragonVisitedTiles = Sets.newHashSet();
                NodeList vl = dragon.getElementsByTagName("visited");
                for (int i = 0; i < vl.getLength(); i++) {
                    dragonVisitedTiles.add(XmlUtils.extractPosition((Element) vl.item(i)));
                }
            }
        }
    }


}
