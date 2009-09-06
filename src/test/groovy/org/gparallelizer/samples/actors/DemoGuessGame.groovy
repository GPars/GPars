package org.gparallelizer.samples.actors

import org.gparallelizer.actors.pooledActors.AbstractPooledActor

/**
 * A guess game. A player actor guesses a number and the master replies with either'too large', 'too small' or 'guessed'.
 * The player continues guessing until he guesses the correct number.
 * @author Jordi Campos i Miralles, Departament de Matemàtica Aplicada i Anàlisi, MAiA Facultat de Matemàtiques, Universitat de Barcelona
 */
class GameMaster extends AbstractPooledActor {
       int secretNum

       void afterStart()
       {
               secretNum = new Random().nextInt(20)
       }

       void act()
       {
               loop
               {
                       react { int num ->
                               if      ( num > secretNum )
                                       reply 'too large'
                               else if ( num < secretNum )
                                       reply 'too small'
                               else
                               {
                                       reply 'you win'
                                       stop()
                               }
                       }
               }
       }
}

class Player extends AbstractPooledActor {
       String              name
       AbstractPooledActor server
       int                 myNum

       void act()
       {
               loop
               {
                       myNum = new Random().nextInt(20)

                       server << myNum

                       react {
                               switch( it )
                               {
                                       case 'too large': println "$name: $myNum was too large"; break
                                       case 'too small': println "$name: $myNum was too small"; break
                                       case 'you win':   println "$name: I won $myNum"; stop(); break
                               }
                       }
               }
       }
}

final def master = new GameMaster().start()
final def player = new Player(name: 'Player', server: master).start()


[master, player]*.join()