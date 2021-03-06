package nz.ac.aut.ense701.gameModel;

import java.awt.Image;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import javax.imageio.ImageIO;

/**
 * This is the class that knows the Kiwi Island game rules and state and
 * enforces those rules.
 *
 * @author AS
 * @version 1.0 - created Maintenance History August 2011 Extended for stage 2.
 * AS
 */
public class Game {

    //Constants shared with UI to provide player data
    public static final int STAMINA_INDEX = 0;
    public static final int MAXSTAMINA_INDEX = 1;
    public static final int MAXWEIGHT_INDEX = 2;
    public static final int WEIGHT_INDEX = 3;
    public static final int MAXSIZE_INDEX = 4;
    public static final int SIZE_INDEX = 5;

    /**
     * A new instance of Kiwi island that reads data from "IslandData.txt".
     */
    public Game() {
        eventListeners = new HashSet<GameEventListener>();

        createNewGame();
    }

    /**
     * Starts a new game. At this stage data is being read from a text file
     */
    public void createNewGame() {
        totalPredators = 0;
        totalKiwis = 0;
        predatorsTrapped = 0;
        kiwiCount = 0;
        initialiseIslandFromFile(selectMapFile());
        drawIsland();
        state = GameState.PLAYING;
        winMessage = "";
        loseMessage = "";
        playerMessage = "";
        notifyGameEventListeners();
    }

    /**
     * *********************************************************************************************************************
     * Accessor methods for game data
     * **********************************************************************************************************************
     */
    /**
     * Get number of rows on island
     *
     * @return number of rows.
     */
    public int getNumRows() {
        return island.getNumRows();
    }

    /**
     * Get number of columns on island
     *
     * @return number of columns.
     */
    public int getNumColumns() {
        return island.getNumColumns();
    }

    /**
     * Gets the current state of the game.
     *
     * @return the current state of the game
     */
    public GameState getState() {
        return state;
    }

    /**
     * Provide a description of occupant
     *
     * @param whichOccupant
     * @return description if whichOccuoant is an instance of occupant, empty
     * string otherwise
     */
    public String getOccupantDescription(Object whichOccupant) {
        String description = "";
        if (whichOccupant != null && whichOccupant instanceof Occupant) {
            Occupant occupant = (Occupant) whichOccupant;
            description = occupant.getDescription();
        }
        return description;
    }

    /**
     * Gets the player object.
     *
     * @return the player object
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Checks if possible to move the player in the specified direction.
     *
     * @param direction the direction to move
     * @return true if the move was successful, false if it was an invalid move
     */
    public boolean isPlayerMovePossible(MoveDirection direction) {
        boolean isMovePossible = false;
        // what position is the player moving to?
        Position newPosition = player.getPosition().getNewPosition(direction);
        // is that a valid position?
        if ((newPosition != null) && newPosition.isOnIsland()) {
            // what is the terrain at that new position?
            Terrain newTerrain = island.getTerrain(newPosition);
            // can the playuer do it?
            isMovePossible = player.hasStaminaToMove(newTerrain)
                    && player.isAlive();
        }
        return isMovePossible;
    }

    /**
     * Get terrain for position
     *
     * @param row
     * @param column
     * @return Terrain at position row, column
     */
    public Terrain getTerrain(int row, int column) {
        return island.getTerrain(new Position(island, row, column));
    }

    /**
     * Is this position visible?
     *
     * @param row
     * @param column
     * @return true if position row, column is visible
     */
    public boolean isVisible(int row, int column) {
        return island.isVisible(new Position(island, row, column));

    }

    /**
     * Is this position explored?
     *
     * @param row
     * @param column
     * @return true if position row, column is explored.
     */
    public boolean isExplored(int row, int column) {
        return island.isExplored(new Position(island, row, column));
    }

    /**
     * Get occupants for player's position
     *
     * @return occupants at player's position
     */
    public Occupant[] getOccupantsPlayerPosition() {
        return island.getOccupants(player.getPosition());
    }

    /**
     * Get string for occupants of this position
     *
     * @param row
     * @param column
     * @return occupant string for this position row, column
     */
    public String getOccupantStringRepresentation(int row, int column) {
        return island.getOccupantStringRepresentation(new Position(island, row, column));
    }

    /**
     * Get image for occupant on this position
     * @param row
     * @param column
     * @return 
     */
    public Occupant[] getGridOccupants(int row, int column) {
        return(island.getGridOccupants(new Position(island, row,column)));
    }
    
    /**
     * Get values from player for GUI display
     *
     * @return player values related to stamina and backpack.
     */
    public int[] getPlayerValues() {
        int[] playerValues = new int[6];
        playerValues[STAMINA_INDEX] = (int) player.getStaminaLevel();
        playerValues[MAXSTAMINA_INDEX] = (int) player.getMaximumStaminaLevel();
        playerValues[MAXWEIGHT_INDEX] = (int) player.getMaximumBackpackWeight();
        playerValues[WEIGHT_INDEX] = (int) player.getCurrentBackpackWeight();
        playerValues[MAXSIZE_INDEX] = (int) player.getMaximumBackpackSize();
        playerValues[SIZE_INDEX] = (int) player.getCurrentBackpackSize();

        return playerValues;

    }

    /**
     * How many kiwis have been counted?
     *
     * @return count
     */
    public int getKiwiCount() {
        return kiwiCount;
    }

    /**
     * How many predators are left?
     *
     * @return number remaining
     */
    public int getPredatorsRemaining() {
        return totalPredators - predatorsTrapped;
    }

    /**
     * Get contents of player backpack
     *
     * @return objects in backpack
     */
    public Object[] getPlayerInventory() {
        return player.getInventory().toArray();
    }

    /**
     * Get player name
     *
     * @return player name
     */
    public String getPlayerName() {
        return player.getName();
    }
    
    /**
     * Get the last map played
     * 
     * @return map file path
     */
    public String getPrevMap(){
        return (this.prevMap);
    }

    /**
     * Is player in this position?
     *
     * @param row
     * @param column
     * @return true if player is at row, column
     */
    public boolean hasPlayer(int row, int column) {
        return island.hasPlayer(new Position(island, row, column));
    }

    /**
     * Only exists for use of unit tests
     *
     * @return island
     */
    public Island getIsland() {
        return island;
    }

    /**
     * Draws the island grid to standard output.
     */
    public void drawIsland() {
        island.draw();
    }

    /**
     * Is this object collectable
     *
     * @param itemToCollect
     * @return true if is an item that can be collected.
     */
    public boolean canCollect(Object itemToCollect) {
        boolean result = (itemToCollect != null) && (itemToCollect instanceof Item);
        if (result) {
            Item item = (Item) itemToCollect;
            result = item.isOkToCarry();
        }
        return result;
    }

    /**
     * Is this object a countable kiwi
     *
     * @param itemToCount
     * @return true if is an item is a kiwi.
     */
    public boolean canCount(Object itemToCount) {
        boolean result = (itemToCount != null) && (itemToCount instanceof Kiwi);
        if (result) {
            Kiwi kiwi = (Kiwi) itemToCount;
            result = !kiwi.counted();
        }
        return result;
    }

    /**
     * Is this object usable
     *
     * @param itemToUse
     * @return true if is an item that can be collected.
     */
    public boolean canUse(Object itemToUse) {
        boolean result = (itemToUse != null) && (itemToUse instanceof Item);
        if (result) {
            //Food can always be used (though may be wasted)
            // so no need to change result

            if (itemToUse instanceof Tool) {
                Tool tool = (Tool) itemToUse;
                //Traps can only be used if there is a predator to catch
                if (tool.isTrap()) {
                    result = island.hasPredator(player.getPosition());
                } //Screwdriver can only be used if player has a broken trap
                else if (tool.isScrewdriver() && player.hasTrap()) {
                    result = player.getTrap().isBroken();
                } else {
                    result = false;
                }
            }
        }
        return result;
    }

    /**
     * Details of why player won
     *
     * @return winMessage
     */
    public String getWinMessage() {
        return winMessage;
    }

    /**
     * Details of why player lost
     *
     * @return loseMessage
     */
    public String getLoseMessage() {
        return loseMessage;
    }

    /**
     * Details of information for player
     *
     * @return playerMessage
     */
    public String getPlayerMessage() {
        String message = playerMessage;
        playerMessage = ""; // Already told player.
        return message;
    }

    /**
     * Is there a message for player?
     *
     * @return true if player message available
     */
    public boolean messageForPlayer() {
        return !("".equals(playerMessage));
    }

    /**
     * *************************************************************************************************************
     * Mutator Methods
     * **************************************************************************************************************
     */
    /**
     * Picks up an item at the current position of the player Ignores any
     * objects that are not items as they cannot be picked up
     *
     * @param item the item to pick up
     * @return true if item was picked up, false if not
     */
    public boolean collectItem(Object item) {
        boolean success = (item instanceof Item) && (player.collect((Item) item));
        if (success) {
            // player has picked up an item: remove from grid square
            island.removeOccupant(player.getPosition(), (Item) item);

            // everybody has to know about the change
            notifyGameEventListeners();
        }
        return success;
    }

    /**
     * Drops what from the player's backpack.
     *
     * @param what to drop
     * @return true if what was dropped, false if not
     */
    public boolean dropItem(Object what) {
        boolean success = player.drop((Item) what);
        if (success) {
            // player has dropped an what: try to add to grid square
            Item item = (Item) what;
            success = island.addOccupant(player.getPosition(), item);
            if (success) {
                // drop successful: everybody has to know that
                notifyGameEventListeners();
            } else {
                // grid square is full: player has to take what back
                player.collect(item);
            }
        }
        return success;
    }

    /**
     * Uses an item in the player's inventory. This can be food or tool items.
     *
     * @param item to use
     * @return true if the item has been used, false if not
     */
    public boolean useItem(Object item) {
        boolean success = false;
        if (item instanceof Food && player.hasItem((Food) item)) //Player east food to increase stamina
        {
            Food food = (Food) item;
            // player gets energy boost from food
            player.increaseStamina(food.getEnergy());
            // player has consumed the food: remove from inventory
            player.drop(food);
            // use successful: everybody has to know that
            notifyGameEventListeners();
        } else if (item instanceof Tool) {
            Tool tool = (Tool) item;
            if (tool.isTrap() && !tool.isBroken()) {
                success = trapPredator();
            } else if (tool.isScrewdriver())// Use screwdriver (to fix trap)
            {
                if (player.hasTrap()) {
                    Tool trap = player.getTrap();
                    trap.fix();
                }
            }
        }
        updateGameState();
        return success;
    }

    /**
     * Count any kiwis in this position
     */
    public void countKiwi() {
        //check if there are any kiwis here
        for (Occupant occupant : island.getOccupants(player.getPosition())) {
            if (occupant instanceof Kiwi) {
                Kiwi kiwi = (Kiwi) occupant;
                if (!kiwi.counted()) {
                    kiwi.count();
                    kiwiCount++;
                    island.removeOccupant(player.getPosition(), occupant);
                }
            }
        }
        updateGameState();
    }

    /**
     * Generates a random discount code as a reward for winning the game.
     *
     * @return a String of 8 random alphanumeric characters.
     */
    public static String generateCoupon() {
        int length = 8;
        char[] characterSet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
        Random random = new SecureRandom();
        char[] result = new char[length];
        for (int i = 0; i < result.length; i++) {
            // picks a random index out of character set > random character
            int randomCharIndex = random.nextInt(characterSet.length);
            result[i] = characterSet[randomCharIndex];
        }
        return new String(result);
    }

    /**
     * Attempts to move the player in the specified direction.
     *
     * @param direction the direction to move
     * @return true if the move was successful, false if it was an invalid move
     */
    public boolean playerMove(MoveDirection direction) {
        // what terrain is the player moving on currently
        boolean successfulMove = false;
        if (isPlayerMovePossible(direction)) {
            Position newPosition = player.getPosition().getNewPosition(direction);
            Terrain terrain = island.getTerrain(newPosition);

            // move the player to new position
            player.moveToPosition(newPosition, terrain);
            island.updatePlayerPosition(player);
            successfulMove = true;

            // Is there a hazard?
            checkForHazard();

            updateGameState();
        }
        return successfulMove;
    }

    /**
     * Adds a game event listener.
     *
     * @param listener the listener to add
     */
    public void addGameEventListener(GameEventListener listener) {
        eventListeners.add(listener);
    }

    /**
     * Removes a game event listener.
     *
     * @param listener the listener to remove
     */
    public void removeGameEventListener(GameEventListener listener) {
        eventListeners.remove(listener);
    }

    /**
     * *******************************************************************************************************************************
     * Private methods
     * *******************************************************************************************************************************
     */
    /**
     * Used after player actions to update game state. Applies the Win/Lose
     * rules.
     */
    private void updateGameState() {
        String message = "";
        if (!player.isAlive()) {
            state = GameState.LOST;
            message = "Sorry, you have lost the game. " + this.getLoseMessage();
            this.setLoseMessage(message);
        } else if (!playerCanMove()) {
            state = GameState.LOST;
            message = "Sorry, you have lost the game. You do not have sufficient stamina to move.";
            this.setLoseMessage(message);
        } else if (predatorsTrapped == totalPredators) {
            state = GameState.WON;
            message = "You win! You have done an excellent job and trapped all the predators."
                    + "\n Your $2 tuck shop discount code: " + generateCoupon();
            this.setWinMessage(message);
        } else if (kiwiCount == totalKiwis) {
            if (predatorsTrapped >= totalPredators * MIN_REQUIRED_CATCH) {
                state = GameState.WON;
                message = "You win! You have counted all the kiwi and trapped at least 80% of the predators."
                        + "\n Your $2 tuck shop discount code: " + generateCoupon();
                this.setWinMessage(message);
            }
        }
        // notify listeners about changes
        notifyGameEventListeners();
    }

    /**
     * Sets details about players win
     *
     * @param message
     */
    private void setWinMessage(String message) {
        winMessage = message;
    }

    /**
     * Sets details of why player lost
     *
     * @param message
     */
    private void setLoseMessage(String message) {
        loseMessage = message;
    }

    /**
     * Set a message for the player
     *
     * @param message
     */
    private void setPlayerMessage(String message) {
        playerMessage = message;

    }

    /**
     * Check if player able to move
     *
     * @return true if player can move
     */
    private boolean playerCanMove() {
        return (isPlayerMovePossible(MoveDirection.NORTH) || isPlayerMovePossible(MoveDirection.SOUTH)
                || isPlayerMovePossible(MoveDirection.EAST) || isPlayerMovePossible(MoveDirection.WEST));

    }

    /**
     * Trap a predator in this position
     *
     * @return true if predator trapped
     */
    private boolean trapPredator() {
        Position current = player.getPosition();
        boolean hadPredator = island.hasPredator(current);
        if (hadPredator) //can trap it
        {
            Occupant occupant = island.getPredator(current);
            //Predator has been trapped so remove
            island.removeOccupant(current, occupant);
            predatorsTrapped++;
        }

        return hadPredator;
    }

    /**
     * Checks if the player has met a hazard and applies hazard impact. Fatal
     * hazards kill player and end game.
     */
    private void checkForHazard() {
        //check if there are hazards
        for (Occupant occupant : island.getOccupants(player.getPosition())) {
            if (occupant instanceof Hazard) {
                handleHazard((Hazard) occupant);
            }
        }
    }

    /**
     * Apply impact of hazard
     *
     * @param hazard to handle
     */
    private void handleHazard(Hazard hazard) {
        if (hazard.isFatal()) {
            player.kill();
            this.setLoseMessage(hazard.getDescription() + " has killed you.");
        } else if (hazard.isBreakTrap()) {
            Tool trap = player.getTrap();
            if (trap != null) {
                trap.setBroken();
                this.setPlayerMessage("Sorry your predator trap is broken. You will need to find tools to fix it before you can use it again.");
            }
        } else // hazard reduces player's stamina
        {
            double impact = hazard.getImpact();
            // Impact is a reduction in players energy by this % of Max Stamina
            double reduction = player.getMaximumStaminaLevel() * impact;
            player.reduceStamina(reduction);
            // if stamina drops to zero: player is dead
            if (player.getStaminaLevel() <= 0.0) {
                player.kill();
                this.setLoseMessage(" You have run out of stamina");
            } else // Let player know what happened
            {
                this.setPlayerMessage(hazard.getDescription() + " has reduced your stamina.");
            }
        }
    }

    /**
     * Notifies all game event listeners about a change.
     */
    private void notifyGameEventListeners() {
        for (GameEventListener listener : eventListeners) {
            listener.gameStateChanged();
        }
    }

    /**
     * Loads terrain and occupant data from a file. At this stage this method
     * assumes that the data file is correct and just throws an exception or
     * ignores it if it is not.
     *
     * @param fileName file name of the data file
     */
    private void initialiseIslandFromFile(String fileName) {
        try {
            Scanner input = new Scanner(new File(fileName));
            // make sure decimal numbers are read in the form "123.23"
            input.useLocale(Locale.US);
            input.useDelimiter("\\s*,\\s*");

            // create the island
            int numRows = input.nextInt();
            int numColumns = input.nextInt();
            island = new Island(numRows, numColumns);

            // read and setup the terrain
            setUpTerrain(input);

            // read and setup the player
            setUpPlayer(input);

            // read and setup the occupants
            setUpOccupants(input);

            input.close();
        } catch (FileNotFoundException e) {
            System.err.println("Unable to find data file '" + fileName + "'");
        } catch (IOException e) {
            System.err.println("Problem encountered processing file.");
        }
    }

    /**
     * Reads terrain data and creates the terrain.
     *
     * @param input data from the level file
     */
    private void setUpTerrain(Scanner input) {
        for (int row = 0; row < island.getNumRows(); row++) {
            String terrainRow = input.next();
            for (int col = 0; col < terrainRow.length(); col++) {
                Position pos = new Position(island, row, col);
                String terrainString = terrainRow.substring(col, col + 1);
                Terrain terrain = Terrain.getTerrainFromStringRepresentation(terrainString);
                island.setTerrain(pos, terrain);
            }
        }
    }

    /**
     * Reads player data and creates the player.
     *
     * @param input data from the level file
     */
    private void setUpPlayer(Scanner input) {
        String playerName = input.next();
        int playerPosRow = input.nextInt();
        int playerPosCol = input.nextInt();
        double playerMaxStamina = input.nextDouble();
        double playerMaxBackpackWeight = input.nextDouble();
        double playerMaxBackpackSize = input.nextDouble();

        Position pos = new Position(island, playerPosRow, playerPosCol);
        player = new Player(pos, playerName,
                playerMaxStamina,
                playerMaxBackpackWeight, playerMaxBackpackSize);
        island.updatePlayerPosition(player);
    }

    /**
     * Creates occupants listed in the file and adds them to the island.
     *
     * @param input data from the level file
     */
    private void setUpOccupants(Scanner input) {
        int numItems = input.nextInt();
        for (int i = 0; i < numItems; i++) {
            String occType = input.next();
            String occName = input.next();
            String occDesc = input.next();
            int occRow = input.nextInt();
            int occCol = input.nextInt();
            Position occPos = new Position(island, occRow, occCol);
            Occupant occupant = null;

            if (occType.equals("T")) {
                double weight = input.nextDouble();
                double size = input.nextDouble();
                try {
                    occupant = new Tool(occPos, occName, occDesc, weight, size, getOccupantImageFile("T",occName));
                } catch (IOException ex) {
                    System.err.println("Error reading File");
                    occupant = new Tool(occPos, occName, occDesc, weight, size, null);               }
            } else if (occType.equals("E")) {
                double weight = input.nextDouble();
                double size = input.nextDouble();
                double energy = input.nextDouble();
                try {
                    occupant = new Food(occPos, occName, occDesc, weight, size, energy, getOccupantImageFile("E",occName));
                } catch (IOException ex) {
                    occupant = new Food(occPos, occName, occDesc, weight, size, energy, null);
                }
            } else if (occType.equals("H")) {
                double impact = input.nextDouble();
                try {
                    occupant = new Hazard(occPos, occName, occDesc, impact, getOccupantImageFile("H",occName));
                } catch (IOException ex) {
                    occupant = new Hazard(occPos, occName, occDesc, impact, null);
                }
            } else if (occType.equals("K")) {
                try {
                    occupant = new Kiwi(occPos, occName, occDesc, getOccupantImageFile("K",occName));
                } catch (IOException ex) {
                    System.err.println("Error reading File");
                    occupant = new Kiwi(occPos, occName, occDesc, null);
                }
                totalKiwis++;
            } else if (occType.equals("P")) {
                try {
                    occupant = new Predator(occPos, occName, occDesc, getOccupantImageFile("P",occName));
                } catch (IOException ex) {
                    System.err.println("Error reading File");
                    occupant = new Predator(occPos, occName, occDesc, null);
                }
                totalPredators++;
            } else if (occType.equals("F")) {
                try {
                    occupant = new Fauna(occPos, occName, occDesc, getOccupantImageFile("F",occName));
                } catch (IOException ex) {
                    System.err.println("Error reading File");  
                    occupant = new Fauna(occPos, occName, occDesc, null);
                }
            }
            if (occupant != null) {
                island.addOccupant(occPos, occupant);
            }
        }
    }
    
    /**
     * Randomly picks a map to be played on from the maps folder
     * 
     * @return path location of chosen map
     */
    private String selectMapFile() {
        File folder = new File("maps");
        File[] mapFiles = folder.listFiles();
        if (mapFiles.length == 2){
            if (mapFiles[1].getPath().equals(prevMap)){
                prevMap= mapFiles[0].getPath();
                return(mapFiles[0].getPath());
            } else{
                prevMap = mapFiles[1].getPath();
                return(mapFiles[1].getPath());
            }
        } else{
            int randIndex = ThreadLocalRandom.current().nextInt(0, mapFiles.length);
            while (mapFiles[randIndex].getPath().equals(prevMap)){
                randIndex = ThreadLocalRandom.current().nextInt(0, mapFiles.length);
            }
            prevMap = mapFiles[randIndex].getPath();
            return(mapFiles[randIndex].getPath());
        }
    }
    //written by glen deleted by brendon
    /**
     * Sets picture for button for corresponding Occupant
     *
    public String getDescIcon(Occupant occ){
            String iconURL ="/nz/ac/aut/ense701/gui/images/doc.jpg";
            if(occ instanceof Kiwi){
                if(occ.getStringRepresentation()=="K"){
                    iconURL = "/nz/ac/aut/ense701/gui/images/0.jpg";    //kiwi
                    return iconURL;
                }
            }else if(occ instanceof Fauna){
                Fauna fauna = (Fauna)occ;
                if(fauna.getName().equals("Rat") ){
                    iconURL = "/nz/ac/aut/ense701/gui/images/6.jpg";     //rat
                    return iconURL;
                }
                if(fauna.getName().equals("Kiore") ){
                    iconURL = "/nz/ac/aut/ense701/gui/images/6.jpg";     //rat
                    return iconURL;
                }
                if(fauna.getName().equals("Cat") ){
                    iconURL = "/nz/ac/aut/ense701/gui/images/4.jpg";     //cat
                    return iconURL;
                }
                if(fauna.getName().equals("Possum") ){
                    iconURL = "/nz/ac/aut/ense701/gui/images/5.jpg";     //rat
                    return iconURL;
                }
                if(fauna.getName().equals("Stoat") ){
                    iconURL = "/nz/ac/aut/ense701/gui/images/9.jpg";     //rat
                    return iconURL;
                }
                if(fauna.getName().equals("Fernbird") ){
                    iconURL = "/nz/ac/aut/ense701/gui/images/1.jpg";     //rat
                    return iconURL;
                }
                if(fauna.getName().equals("Heron") ){
                    iconURL = "/nz/ac/aut/ense701/gui/images/2.jpg";     //rat
                    return iconURL;
                }
                if(fauna.getName().equals("Tui") ){
                    iconURL = "/nz/ac/aut/ense701/gui/images/10.jpg";     //rat
                    return iconURL;
                }
                if(fauna.getName().equals("Robin") ){
                    iconURL = "/nz/ac/aut/ense701/gui/images/11.jpg";     //rat
                    return iconURL;
                }
                if(fauna.getName().equals("Crab") ){
                    iconURL = "/nz/ac/aut/ense701/gui/images/12.jpg";     //rat
                    return iconURL;
                }
                if(fauna.getName().equals("Oystercatcher") ){
                    iconURL = "/nz/ac/aut/ense701/gui/images/13.jpg";     //rat
                    return iconURL;
                }
                if(fauna.getName().equals("Dolphin") ){
                    iconURL = "/nz/ac/aut/ense701/gui/images/3.jpg";     //rat
                    return iconURL;
                }
            }else if(occ instanceof Food){
                    iconURL = "/nz/ac/aut/ense701/gui/images/8.jpg";     //food
                    return iconURL;

            }else if(occ instanceof Predator){
                    iconURL = "/nz/ac/aut/ense701/gui/images/4.jpg";       //predator
                    return iconURL;

            }else if(occ instanceof Tool){
                    iconURL = "/nz/ac/aut/ense701/gui/images/7.jpg";        //tool
                    return iconURL;
            }
            return iconURL;
        }
    /**
     * Returns the description of an Occupant
     */
        public String getOccupantDescription(Occupant occ){
            return (occ.getDescription());
        }
        
        private Image getOccupantImageFile(String occType, String occName) throws IOException {
            File file = new File("images/doc.jpg");
            try{
                if (occType.equals("K")) {
                    file = new File("images/Kiwi.jpg");
                    return (ImageIO.read(file));
                } 
                else if (occType.equals("E")){
                    file = new File("images/Food.jpg");
                    return (ImageIO.read(file));
                }
                else if (occType.equals("T")){
                    if (occName.equals("Trap")){
                        file = new File("images/Trap.jpg");
                        return (ImageIO.read(file));
                    } else{
                        file = new File("images/Screwdriver.jpg");
                        return (ImageIO.read(file));
                    }
                }
                else if (occType.equals("F")) { 
                    if (occName.equals("Crab")){
                        file = new File("images/Crab.jpg");
                        return (ImageIO.read(file));
                    } else if (occName.equals("Dolphin")){
                        file = new File("images/Dolphin.jpg");
                        return (ImageIO.read(file));
                    }else if (occName.equals("Tui")){
                        file = new File("images/Tui.jpg");
                        return (ImageIO.read(file));
                    }else if (occName.equals("Oystercatcher")){
                        file = new File("images/Oystercatcher.jpg");
                        return (ImageIO.read(file));
                    }else if (occName.equals("Fenbird")){
                        file = new File("images/Fenbird.jpg");
                        return (ImageIO.read(file));
                    }else if (occName.equals("Heron")){
                        file = new File("images/Heron.jpg");
                        return (ImageIO.read(file));
                    }else{
                        file = new File("images/Robin.jpg");
                        return (ImageIO.read(file));
                    }
                } else if (occType.equals("P")){
                    if (occName.equals("Rat")){
                        file = new File("images/Rat.jpg");
                        return (ImageIO.read(file));
                    }else if (occName.equals("Cat")){
                        file = new File("images/Cat.jpg");
                        return (ImageIO.read(file));
                    }else if (occName.equals("Stoat")){
                        file = new File("images/Stoat.jpg");
                        return (ImageIO.read(file));
                    } else{
                        file = new File("images/Possum.jpg");
                        return(ImageIO.read(file));
                    }
                }
            } catch(Exception e){
                System.err.println("Error reading image");
            }
        return (ImageIO.read(file));
    }
        
    

        private Island island;
        private Player player;
        private GameState state;
        private int kiwiCount;
        private int totalPredators;
        private int totalKiwis;
        private int predatorsTrapped;
        private Set<GameEventListener> eventListeners;

        private final double MIN_REQUIRED_CATCH = 0.8;

        private String winMessage = "";
        private String loseMessage = "";
        private String playerMessage = "";
        private String prevMap = "";
        //import the descriptions and images for description pannel
        private DescImporter DI = new DescImporter();
        private ArrayList<String> descriptionIconList = DI.importImages();



    
}