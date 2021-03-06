package com.jcloisterzone.game;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ini4j.Ini;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.jcloisterzone.Expansion;
import com.jcloisterzone.Player;
import com.jcloisterzone.PointCategory;
import com.jcloisterzone.UserInterface;
import com.jcloisterzone.board.Board;
import com.jcloisterzone.board.Location;
import com.jcloisterzone.board.Position;
import com.jcloisterzone.board.Tile;
import com.jcloisterzone.board.TilePack;
import com.jcloisterzone.board.TileTrigger;
import com.jcloisterzone.collection.Sites;
import com.jcloisterzone.event.EventMulticaster;
import com.jcloisterzone.event.GameEventListener;
import com.jcloisterzone.feature.City;
import com.jcloisterzone.feature.visitor.score.CompletableScoreContext;
import com.jcloisterzone.feature.visitor.score.ScoreContext;
import com.jcloisterzone.figure.Follower;
import com.jcloisterzone.figure.Meeple;
import com.jcloisterzone.game.capability.AbbeyCapability;
import com.jcloisterzone.game.capability.BarnCapability;
import com.jcloisterzone.game.capability.BazaarCapability;
import com.jcloisterzone.game.capability.BridgeCapability;
import com.jcloisterzone.game.capability.BuilderCapability;
import com.jcloisterzone.game.capability.CastleCapability;
import com.jcloisterzone.game.capability.ClothWineGrainCapability;
import com.jcloisterzone.game.capability.DragonCapability;
import com.jcloisterzone.game.capability.FairyCapability;
import com.jcloisterzone.game.capability.TowerCapability;
import com.jcloisterzone.game.capability.WagonCapability;
import com.jcloisterzone.game.expansion.CatharsGame;
import com.jcloisterzone.game.expansion.CornCirclesGame;
import com.jcloisterzone.game.expansion.FlierGame;
import com.jcloisterzone.game.expansion.KingAndScoutGame;
import com.jcloisterzone.game.expansion.TunnelGame;
import com.jcloisterzone.game.phase.GameOverPhase;
import com.jcloisterzone.game.phase.Phase;


/**
 * Other information than board needs in game. Contains players with their
 * points, followers ... and game rules of current game.
 */
public class Game extends GameSettings {

    protected final transient Logger logger = LoggerFactory.getLogger(getClass());
    private Ini config;

    /** pack of remaining tiles */
    private TilePack tilePack;
    /** pack of remaining tiles */
    private Tile currentTile;
    /** game board, contains placed tiles */
    private Board board;

    /** list of players in game */
    private Player[] plist;
    /** rules of current game */

    /** player in turn */
    private Player turnPlayer;

    private final Map<Class<? extends Phase>, Phase> phases = Maps.newHashMap();
    private Phase phase;

    private GameEventListener eventListener;
    private UserInterface userInterface;

    private Map<Object, GameExtension> extensions = Maps.newHashMap();
    private final GameDelegation extensionsDelegate = new ExtensionsDelegate(this);

    private int idSequenceCurrVal = 0;


    public Ini getConfig() {
        return config;
    }

    public void setConfig(Ini config) {
        this.config = config;
    }

    public Tile getCurrentTile() {
        return currentTile;
    }

    public void setCurrentTile(Tile currentTile) {
        this.currentTile = currentTile;
    }

    public Phase getPhase() {
        return phase;
    }

    public void setPhase(Phase phase) {
        phase.setEntered(false);
        this.phase = phase;
    }

    public Map<Class<? extends Phase>, Phase> getPhases() {
        return phases;
    }

    public GameEventListener fireGameEvent() {
        return eventListener;
    }
    public void addGameListener(GameEventListener listener) {
        eventListener = (GameEventListener) EventMulticaster.addListener(eventListener, listener);
    }
    public void removeGameListener(GameEventListener listener) {
        eventListener = (GameEventListener) EventMulticaster.removeListener(eventListener, listener);
    }
    public UserInterface getUserInterface() {
        return userInterface;
    }
    public void addUserInterface(UserInterface ui) {
        userInterface = (UserInterface) EventMulticaster.addListener(userInterface, ui);
    }

    public Iterable<Meeple> getDeployedMeeples() {
        Iterable<Meeple> iter = null;
        for(Player player : plist) {
            if (iter == null) {
                iter = Iterables.concat(player.getFollowers(), player.getSpecialMeeples());
            } else {
                iter = Iterables.concat(iter, player.getFollowers(), player.getSpecialMeeples());
            }
        }
        return Iterables.filter(iter, new Predicate<Meeple>() {
            @Override
            public boolean apply(Meeple m) {
                return m.getPosition() != null;
            }
        });
    }

    public Player getTurnPlayer() {
        return turnPlayer;
    }

    public void setTurnPlayer(Player turnPlayer) {
        this.turnPlayer = turnPlayer;
        fireGameEvent().playerActivated(turnPlayer, turnPlayer);
    }

    /**
     * Returns player who is allowed to make next action.
     * @return
     */
    public Player getActivePlayer() {
        Phase phase = getPhase();
        return phase == null ? null : phase.getActivePlayer();
    }


    /**
     * Ends turn of current active player and make active the next.
     */
    public Player getNextPlayer() {
        return getNextPlayer(turnPlayer);
    }

    public Player getNextPlayer(Player p) {
        int playerIndex = p.getIndex();
        int nextPlayerIndex = playerIndex == (plist.length - 1) ? 0 : playerIndex + 1;
        return getPlayer(nextPlayerIndex);
    }


    /**
     * Return player with the given index.
     * @param i player index
     * @return demand player
     */
    public Player getPlayer(int i) {
        return plist[i];
    }

    /**
     * Returns whole player list
     * @return player list
     */
    public Player[] getAllPlayers() {
        return plist;
    }

    public TilePack getTilePack() {
        return tilePack;
    }

    public void setTilePack(TilePack tilePack) {
        this.tilePack = tilePack;
    }

    public Board getBoard() {
        return board;
    }


    public Meeple getMeeple(final Position p, final Location loc, Class<? extends Meeple> meepleType, Player owner) {
        for (Meeple m : getDeployedMeeples()) {
            if (m.getPosition().equals(p) && m.getLocation().equals(loc)) {
                if (m.getClass().equals(meepleType) && m.getPlayer().equals(owner)) {
                    return m;
                }
            }
        }
        return null;
    }

    public void setPlayers(List<Player> players, int turnPlayer) {
        Player[] plist = players.toArray(new Player[players.size()]);
        this.plist = plist;
        this.turnPlayer = getPlayer(turnPlayer);
    }

    private void createGameExtensions(Object key, Class<? extends GameExtension> clazz) {
        if (clazz == null) return;
        /* Expansions can share implementations - e.g Crop Circles 1 & 2
         * in such case only one instance must be created
         */
        for (GameExtension ge : extensions.values()) {
            if (ge.getClass().equals(clazz)) return;
        }
        try {
            GameExtension eg = clazz.newInstance();
            eg.setGame(this);
            extensions.put(key, eg);
        } catch (Exception e) {
            logger.error(e.getMessage(), e); //should never happen
        }
    }

    public void start() {
        for (Expansion expansion: getExpansions()) {
            createGameExtensions(expansion, expansion.getImplemetedBy());
        }
        for (Capability capability: getCapabilities()) {
            createGameExtensions(capability, capability.getImplementedBy());
        }
        board = new Board(this);
    }

    //TODO refactor and clear ?

    public Collection<GameExtension> getExtensions() {
        return extensions.values();
    }

    public Map<Object, GameExtension> getExtensionMap() {
        return extensions;
    }

    public GameExtension getExpandedGameFor(Expansion expansion) {
        return extensions.get(expansion);
    }

    public GameExtension getCapabilityFor(Capability cap) {
        return extensions.get(cap);
    }

    public GameDelegation extensionsDelegate() {
        return extensionsDelegate;
    }

    public Sites prepareCommonSites() {
        Sites sites = new Sites();
        //on Volcano tile common figure cannot be placed when it comes into play
        if (getCurrentTile().getTrigger() != TileTrigger.VOLCANO) {
             //make shared sites
            Set<Location> tileSites = prepareCommonForTile(getCurrentTile(), false);
            if (! tileSites.isEmpty()) {
                sites.put(getCurrentTile().getPosition(), tileSites);
                return sites;
            }
        }
        return sites;
    }

    public Set<Location> prepareCommonForTile(Tile tile, boolean excludeFinished) {
        Set<Location> locations = tile.getUnoccupiedScoreables(excludeFinished);
        Tile nextTile = getCurrentTile(); //can be tile != nextTile
        //v pripade custom pravidla zakazeme pokladat na princeznu obyc figurku
        if (nextTile != null) { //nextTile muze byt null v pripade volani z AI, kde se muze predpripravovat figure rating pred polozenim
            City princessLoc = nextTile.getPrincessCityPiece();
            if (princessLoc != null && hasRule(CustomRule.PRINCESS_MUST_REMOVE_KNIGHT)) {
                locations.remove(princessLoc.getLocation());
            }
        }
        return locations;
    }

    //scoring helpers

    public void scoreFeature(int points, ScoreContext ctx, Player p) {
        p.addPoints(points, ctx.getMasterFeature().getPointCategory());
        Follower follower = ctx.getSampleFollower(p);
        boolean isFinalScoring = getPhase() instanceof GameOverPhase;
        FairyCapability fairyCap = getFairyCapability();
        if (fairyCap != null && follower.getPosition().equals(fairyCap.getFairyPosition())) {
            p.addPoints(FairyCapability.FAIRY_POINTS_FINISHED_OBJECT, PointCategory.FAIRY);
            fireGameEvent().scored(follower.getFeature(), points+FairyCapability.FAIRY_POINTS_FINISHED_OBJECT,
                    points+" + "+FairyCapability.FAIRY_POINTS_FINISHED_OBJECT, follower,
                    isFinalScoring);
        } else {
            fireGameEvent().scored(follower.getFeature(), points, points+"", follower, isFinalScoring);
        }
    }

    public void scoreCompletableFeature(CompletableScoreContext ctx) {
        Set<Player> players = ctx.getMajorOwners();
        if (players.isEmpty()) return;
        int points = ctx.getPoints();
        for (Player p : players) {
            scoreFeature(points, ctx, p);
        }
    }

    public int idSequnceNextVal() {
        return ++idSequenceCurrVal;
    }

    //shortcut methods

    public KingAndScoutGame getKingAndScoutGame() {
        return (KingAndScoutGame) extensions.get(Expansion.KING_AND_SCOUT);
    }
    public CatharsGame getCatharsGame() {
        return (CatharsGame) extensions.get(Expansion.CATHARS);
    }
    public TunnelGame getTunnelGame() {
        return (TunnelGame) extensions.get(Expansion.TUNNEL);
    }
    public CornCirclesGame getCornCirclesGame() {
        GameExtension eg = extensions.get(Expansion.CORN_CIRCLES);
        if (eg == null) {
            eg = extensions.get(Expansion.CORN_CIRCLES_II);
        }
        return (CornCirclesGame) eg;
    }
    public FlierGame getFlierGame() {
        return (FlierGame) extensions.get(Expansion.FLIER);
    }

    public BuilderCapability getBuilderCapability() {
        return (BuilderCapability) extensions.get(Capability.BUILDER);
    }
    public ClothWineGrainCapability getClothWineGrainCapability() {
        return (ClothWineGrainCapability) extensions.get(Capability.CLOTH_WINE_GRAIN);
    }
    public FairyCapability getFairyCapability() {
        return (FairyCapability) extensions.get(Capability.FAIRY);
    }
    public DragonCapability getDragonCapability() {
        return (DragonCapability) extensions.get(Capability.DRAGON);
    }
    public AbbeyCapability getAbbeyCapability() {
        return (AbbeyCapability) extensions.get(Capability.ABBEY);
    }
    public TowerCapability getTowerCapability() {
        return (TowerCapability) extensions.get(Capability.TOWER);
    }
    public WagonCapability getWagonCapability() {
        return (WagonCapability) extensions.get(Capability.WAGON);
    }
    public BarnCapability getBarnCapability() {
        return (BarnCapability) extensions.get(Capability.BARN);
    }
    public BridgeCapability getBridgeCapability() {
        return (BridgeCapability) extensions.get(Capability.BRIDGE);
    }
    public CastleCapability getCastleCapability() {
        return (CastleCapability) extensions.get(Capability.CASTLE);
    }
    public BazaarCapability getBazaarCapability() {
        return (BazaarCapability) extensions.get(Capability.BAZAAR);
    }

}
