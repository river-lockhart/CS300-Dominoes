//AUTHORED BY RIVER LOCKHART, KHUSBU BHUSHAL, GABRIEL BOAFO\\

#PROJECT COMPLETED!

# CS300-Dominos


# In case of Java issues                                                              

Download JDK 21. -> Add path to JAVA_HOME in System Variables (may need to create JAVA_HOME). -> Ensure selected IDE is using correct Java version. -> Restart computer.

# Run program

Open folder CS300-Dominos in terminal.

In terminal, run command: 

    Windows: .\gradlew.bat run

    Mac: ./gradlew run

This will run the game. 

# In case of weird bugs. (Gradle is finnicky)

Close IDE. -> Go to CS300-Dominoes directory. -> Delete .gradle folder. This will delete cache manually.

Open folder CS300-Dominos in terminal.

In terminal, run command: 

    Windows: .\gradlew.bat clean build    

    Mac: ./gradlew clean build      or      chmod +x gradlew
                                            ./gradlew clean build



# TO-DO

# Section 1
- Create Domino structure in src\main\java\models\CDominoes.java -- DONE

- Create distribution of 10 random dominoes to each player hand (associate resource image with each domino) in src\main\java\models\CRandom.java -- DONE

- src\main\java\models\Hand.java will contain currentHand information as an ArrayList(what dominoes and how many, as well as adding and removing from hand) -- DONE

- Dominoes in each players hand, as defined in src\main\java\models\Hand.java will be displayed in players hand area in src\main\java\views\CTable.java -- DONE

- Pieces not distributed to player hands will be sent to src\main\java\models\AvailablePieces.java (these will be displayed in the popup panel when clicking Remaining Pieces on the table) Needs to allow player to select pieces from the menu when they don't have a piece that will work -- DONE

- First player to move will be determined 50-50 (src\main\java\models\CRandom.java) - DONE

# Section 2

- Player structure needs to be defined (src\main\java\controllers\Player.java) - DONE

- AIPlayer rules need to be defined (src\main\java\controllers\AIPlayer.java) - DONE

- Need to create rules for how dominoes will be placed on the table (src\main\java\models\TableLayout.java) - DONE

- Dominoes in player hand need to be able to be dragged and dropped onto table, as well as rotated with a key event (src\main\java\models\TableLayout.java) - DONE

- Win state needs to be defined - DONE


# Section 3

- Store final results and display to the console in ASCII (src\main\java\util\ConsoleLogger.java) - DONE


