# selenium-manager

A simple utility to download the required browser driver using the selenium-manager.
Created this utility to download and archive the browser driver executables behind a firewall for automation engineers
to use. These archives will eventually be made available via tool like JFrog artifactory.

### Browsers supported:
- Chrome
- Edge

### How to run:
```
mvn compile exec:java -DbrowserVersion=115
```
