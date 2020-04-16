# cogmind-scores

A set of Clojure functions to analyze Cogmind score data in Protobuf
format.

For more on Cogmind, see https://www.gridsagegames.com/cogmind/

This is a fan project and not sponsored by or affiliated with Grid Sage
Games / Josh Ge. Any opinions represented here are my own.

I've done this as a fun way to learn Clojure; don't expect too much.

## Usage

What you'll need:

1. protoc
2. [Leinengen](https://leiningen.org/#install)
3. Score data (you'll have to ask Kyzrati for this)

Set the path to protoc in project.clj if it's not `/usr/bin/protoc`.

Then run main:

```
lein run -- -d score-data-directory leaders 100
```

Or run the REPL and experiment.

```
lein repl
```

## Copyright

Copyright © 2020 Joshua Hoke

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
