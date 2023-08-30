# Missionary website

Work in progress.
- [X] General structure
- [ ] Gorgeous graphic design [#1](https://github.com/leonoel/missionary-website/issues/1)
- [ ] Great home page [#2](https://github.com/leonoel/missionary-website/issues/2)
- [ ] Awesome documentation [#3](https://github.com/leonoel/missionary-website/issues/3)

Temporary URL : https://gorgeous-sorbet-a5a2bf.netlify.app

## Authoring
* Edit markdown files in directory `assets`. Paths and metadata are defined in `pages.edn`.
* Regenerate website with `clojure -M:release`. Alternatively, evaluate `(release/-main)` at the REPL.
* Serve directory `release`. Example : `python3 -m http.server 9000 --directory release`.