# cogmind-scores

A set of Clojure functions to analyze Cogmind score data in Protobuf
format.

For more on Cogmind, see https://www.gridsagegames.com/cogmind/

I've done this as a fun way to learn Clojure; don't expect too much.

## Usage

What you'll need:

1. [Leinengen](https://leiningen.org/#install)
2. The protobuf definition from https://github.com/Kyzrati/cogmind-scoresheet
copied into resources/protobuf/scoresheet.proto
3. Score data (you'll have to ask Kyzrati for this)

Compile the protobuf definition with:

```
protoc -I resources/proto \
	resources/proto/scoresheet.proto \
	resources/proto/archived_scoresheet.proto \
	 --java_out .
```

Then run it:

```
lein run -- -d score-data-directory leaders 100
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
