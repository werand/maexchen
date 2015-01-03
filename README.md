# maexchen

An alternative Clojure maexchen client and bot to play Mia, see 
https://github.com/janernsting/maexchen for details.

## Usage

The mia server must be running.  

To start the simple bot (in a repl):

(start-bot (maexchen.bot.SimpleBot. "clojure") "localhost" 9000)

To stop the bot 
(stop-bot)

The bot can interactively be tweaked, that means changes to bot functions
will take effect immediately.

## License

Copyright © 2015 by Andreas Werner

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
