# CS300-Dominos

Open folder CS300-Dominos in integrated terminal in VSCode.

In terminal, run command: 

    Windows: .\gradlew.bat run

    Mac: ./gradlew run

This will run the game. 

If this does not work, then install JDK 21 and set to PATH in environment variables.

Restart computer.

# TO-DO

# Section 1
- Create Domino structure in src\main\java\models\CDominoes.java -- DONE

- Create distribution of 10 random dominoes to each player hand (associate resource image with each domino) in src\main\java\models\CRandom.java -- DONE

- src\main\java\models\Hand.java will contain currentHand information as an ArrayList(what dominoes and how many, as well as adding and removing from hand) -- Just need to include add/subtract from hand

- Dominoes in each players hand, as defined in src\main\java\models\Hand.java will be displayed in players hand area in src\main\java\views\CTable.java -- Reference is passed, need to apply to ui

- Pieces not distributed to player hands will be sent to src\main\java\models\AvailablePieces.java (these will be displayed in the popup panel when clicking Remaining Pieces on the table) Needs to allow player to select pieces from the menu when they don't have a piece that will work -- Reference passed, need to apply to ui

- First player to move will be determined 50-50 (src\main\java\models\CRandom.java)

# Section 2

- Player structure needs to be defined (src\main\java\controllers\Player.java) AiPlayer will extend Player

- AIPlayer rules need to be defined (src\main\java\controllers\AIPlayer.java)

- Need to create rules for how dominoes will be placed on the table (src\main\java\models\TableLayout.java)

- Dominoes in player hand need to be able to be dragged and dropped onto table, as well as rotated with a key event (src\main\java\models\TableLayout.java)

- Win state needs to be defined


# Section 3

- Store final results and display to the console in ASCII (src\main\java\util\ConsoleLogger.java)