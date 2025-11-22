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
commands:
    search-providers                                -> List<String>
    search   <search provider> <query> [filters]    -> SearchResult
    more     <pageToken>                            -> Items
    stream   <url>                                  -> StreamInfo
    playlist <url>                                  -> PlaylistInfo
    channel  <url>                                  -> ChannelInfo
    help
```
All output is in JSON. To know structure of resulted JSONs, please refer to [app/src/main/kotlin/backend/Backend.kt](app/src/main/kotlin/backend/Backend.kt).

Also run `make test` to see all JSON outputs in `app/test_results`

In case of an error the program exit code is 1 and error message is printed. **Maybe we should assign every error a sepecific error code?**

**TODO: Add support for search filters**
