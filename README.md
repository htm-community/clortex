# clortex (pre-alpha)

Clortex is an implementation in Clojure of Jeff Hawkins' Hierarchical Temporal Memory & Cortical Learning Algorithm. See the [website](http://fergalbyrne.github.io) for full details.

Clortex is a reimagining and reimplementation of Numenta Platfrom for Intelligent Computing (NuPIC), which
is also an Open Source project released by Grok Solutions (formerly Numenta), the company founded by Jeff to
make his theories a practical and commercial reality. NuPIC is a mature, excellent and useful software platform, with a vibrant community, so please join us at [Numenta.org](http://numenta.org).

**Warning: pre-alpha software**. This project is only beginning, and everything you see here will eventually be thrown away as we develop better ways to do things. The design and the APIs are subject to drastic change without notice.

Clortex is Open Source software, released under the GPL Version 3 (see the end of this README). You are free to use, copy, modify, and redistribute this software according to the terms of that license. For commercial use of the algorithms used in Clortex, please contact [Grok Solutions](http://groksolutions.com), where they'll be happy to discuss commercial licensing.

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
