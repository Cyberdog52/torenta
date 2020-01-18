# torenta

## Setup

#### Java
Install Java OpenJDK 13 from [here](https://jdk.java.net/13/). 
Make sure it is also in the system's path, because gradlew will use the java version that is on the path.
To see which java version is used, type the following in the terminal:

```
Windows: 
for %i in (java.exe) do @echo.   %~$PATH:i

Linux/Unix/Mac OS X: 
which java
```


#### IntelliJ
Install new version of IntelliJ Idea (minimum 2018.3) from [here](https://www.jetbrains.com/idea/download/).
The enterprise version is recommended.

#### TMDB-Key
First, you need to get an API key from [TMDB](https://developers.themoviedb.org/3/getting-started/introduction). Create an `application.properties` from the template and add the key. Do not check in the `application.properties` file.

#### NodeJs
Install newest version of NodeJs from [here](https://nodejs.org/en/download/).
Restart IntelliJ and execute the following code:
```
cd frontend
npm install
```

## Run

#### Run Backend:



Execute TorentaApplication through RunConfigurations

Will start at http://localhost:8080/

#### Run Frontend:

```
cd frontend
npm start
```

Will start at http://localhost:4200/

## Development links

[Jira Backlog](https://andreskonrad.atlassian.net/jira/software/projects/TOR/boards/1/backlog)

[Swagger UI](http://localhost:8080/swagger-ui.html)

[Swagger Docs](http://localhost:8080/v2/api-docs)

[TMDB](https://developers.themoviedb.org/3/getting-started/introduction)

Designs:
[TV-Overview](https://hubmovies-a26fc.firebaseapp.com/movie/496243)


 

## Further reading:

https://pub.tik.ee.ethz.ch/students/2006-So/MA-2006-26.pdf

http://www.kristenwidman.com/blog/71/how-to-write-a-bittorrent-client-part-2/

https://wiki.theory.org/index.php/BitTorrentSpecification

https://github.com/atomashpolskiy/bittorrent

https://github.com/pmoor/bitthief

https://github.com/clamarque/HubMovies

