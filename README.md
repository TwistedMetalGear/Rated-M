# Rated M

Rated M is an online multiplayer game based on Cards Against Humanity. The game is simple. Play with a group of friends and take turns submitting and judging the most hilarious card combinations.

Play store link: https://play.google.com/store/apps/details?id=net.silentbyte.ratedm

This was my very first Android app. I started writing it in 2013 after getting aquainted with a book that many of you are familiar with: Android Programming: The Big Nerd Ranch Guide. I decided to go all out and dive right into a complex app, because why not?

Being my first app, the structure of the code leaves a lot to be desired. Note the lack of an architecture. There is no MVP or MVVM to be seen here. I guess you could call it MVC, with the activities being the controllers, leading to lengthy and overly complex activities.

There's also no real packaging structure here. Ideally, things would be packaged by feature, but here I just put all activities under an "activities" package, all fragments under a "fragments" package, etc...

The use of 3rd party libraries was kept to a minimum. For example, instead of using something like Retrofit to handle HTTP requests, I used the much lower level HttpURLConnection and manually parsed the response JSON. Part of this is because I wanted to get a good understanding of how things work at a lower level.

So, this codebase is definitely not winning any awards. However, I am damn proud of what I was able to accomplish. The app is very stable and supports devices all the way back to API 14. Despite being locked to portrait mode, I made it a priority to support config changes and gracefully resume upon the process being destroyed in a low memory situation.

Note that I removed some private ids and URLs from this codebase. So it won't run, or at least it won't connect to the server in this state. My intent is not to provide everything you need to get connected and running, but rather just to share the code of my first app and hopefully motivate other developers with similar ideas.

# Screenshots:

![](https://i.imgur.com/8ZvQK0L.jpg)

![](https://i.imgur.com/6fv5A1z.jpg)

![](https://i.imgur.com/R1eHTip.jpg)

![](https://i.imgur.com/FOSePtJ.jpg)

![](https://i.imgur.com/aTWdPfv.jpg)

![](https://i.imgur.com/xDzWoFB.jpg)

![](https://i.imgur.com/wfUXEam.jpg)

# Features:
* Online multiplayer with up to 8 players.
  <br/>Log in with email or Facebook.
  <br/>Play against friends and random players from around the world.
  <br/>Play against bots while you wait for human players to join.

* 2,058 cards from the official game that started it all. *
  <br/>Includes the base deck and all expansions.
  <br/>Includes pick two cards.

* Custom text cards
  <br/>Respond to black cards with your own custom text.

* Custom picture cards
  <br/>Bored of text? Search an internet full of images and respond to black cards with picture cards.

* Persistent matches
  <br/>Play at your own leisure. No need to leave the app running.
  <br/>Notification system alerts you when it's time to submit or judge a card.

* Betting system
  <br/>Think your card is the best? Bet your points on it!

* Toss unwanted cards
  <br/>Don't like a card? Toss it for a new one in exchange for points.

* In-game chat
  <br/>Trash talk your friends.

* Auto Pick / Skip
  <br/>Configurable timeouts allow a random auto pick to be made for inactive players.
  <br/>Alternatively, inactive players can be skipped.

* Reaction emoji
  <br/>The judge can react to submitted cards with a wide range of emoji.

* Full match history
  <br/>View previous round results at any time.

* Optional text to speech
  <br/>Allows your device to read the cards out loud.

* Encrypted data
  <br/>All cards, player data, and chat is securely encrypted.

* Completely free now and forever
  <br/>No limits, no ads, no in-app purchases.
