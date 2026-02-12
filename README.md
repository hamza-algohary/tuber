# Tuber

Tuber is a CLI frontend for YouTube, PeerTube, SoundCloud and other sites, powered by the fantastic [NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor) java library.

Moreover, tuber is suitable for usage as a backend since all its output is in JSON format.

### Installation
1. Clone this repo
```bash
git clone https://github.com/hamza-algohary/tuber
```
2. Build
```bash
make
```
3. Give execution permission
```bash
chmod +x tuber.jar
```
4. Run the program
```bash
java -jar tuber.jar help
```
### Usage
On running `tuber help`:
```bash
    Search Commands:
        search-providers                                -> List<String> 
        search <search provider> <query> [--filters <colon separated list>] [--sort <criteria>] -> SearchResult
        filters  <search provider>                      -> List<String>
        sort-options <search provider>                  -> List<String>
    Url Handlers:
        stream   <url>                                  -> StreamInfo
        playlist <url>                                  -> PlaylistInfo
        channel  <url>                                  -> ChannelInfo
    Page Tokens Handler
        more     <page token>                            -> Items
    Catalogs/Recommendations
        catalogs                                        -> List<String>
        catalog  <catalog provider>                     -> List<PlaylistInfo>
    Lists
        lists                              -> List<String> # available lists names
        list-add    <list name> <url>      # Adds url of stream, playlist or channel to list. Adding a playlist also adds its entire content.
        list-remove <list name> <url>      # Removes an item from list, removing a playlist removes all its content
        list-search <list name> <query>    # Search inside a list
        list-export <list name> <path>     # Export an entire list to a file (a text file of NULL delimited stream of Summary JSONs)
        list-import <list name> <path>     # Import an entire list from a file
        list-delete <list name>            # Deletes an entire list
        list-plugins <list name>          # Get all plugins used in a list
        list-channels <list name>          # Get all explicitly added channels to a list
    Others
        prepare-podcastindex <path to podcastindex sqlite db>  -> Progressive null-delimited stream of Progress objects each in Json format.
```
Example
```bash
java -jar tuber.jar search youtube "linux" --filters video:audio --sort date
```

All output is in JSON. To know structure of resulted JSONs, please refer to [app/src/main/kotlin/plugins/Data.kt](app/src/main/kotlin/plugins/Data.kt).

Also run `make test` to see all JSON outputs in `app/test_results`

In case of an error the program exit code is 1 and error message is printed. **Maybe we should assign every error a sepecific error code?**