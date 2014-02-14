# clortex (pre-alpha)

Clortex is an implementation in Clojure of Jeff Hawkins' Hierarchical Temporal Memory & Cortical Learning Algorithm. See the [website](http://fergalbyrne.github.io) for full details.

## Example Usage (not yet implemented)

(require '[clortex.core])

(make-cortex test-cortex)

(add-encoder test-cortex (file-encoder "data.csv"))

(run-cortex test-cortex :all-records)

## Developer Information

* [Documentation](http://fergalbyrne.github.io)
* [API and Source Docs](http://fergalbyrne.github.io/uberdoc.html)
* [GitHub project](https://github.com/fergalbyrne/clortex)

## License

Copyright &copy; 2014 Fergal Byrne, Brenter IT

Distributed under the GNU Public Licence, Version 3 [http://www.gnu.org/licenses/gpl.html](http://www.gnu.org/licenses/gpl.html), same as NuPIC. For commercial use, please contact [Grok Solutions](http://groksolutions.com).
