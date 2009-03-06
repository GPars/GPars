import org.gparallelizer.actors.pooledActors.AbstractPooledActor

class GameMaster extends AbstractPooledActor {
       int secretNum

       void afterStart()
       {
               secretNum = new Random().nextInt(10)
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
                       myNum = new Random().nextInt(10)

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

def master = new GameMaster().start()
new Player( name: 'Player', server: master ).start()

