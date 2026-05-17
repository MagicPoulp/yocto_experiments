
## Author

Thierry Vilmart
2024
I have worked for 3 years with Erlang and Elixir at Ericsson (Erlang creator) and at Campanja (google adwords).

## Content of the repository

The was done for a recruitment in a company running Elixir.

A C++ program can be supervised and if it terminates we caputure the event.
And we can use the STDIN/STDOUt to pass messages between ELixir and the C++ program.
See a simple web client API on a meteo API.

See random test generation with propcheck (proper/quickcheck in Erlang)
Random JSONS are generated.

## to run random generated tests

```
mix deps.get
mix test
mix test test/rand_proper1_test.exs
```

## To build in Yocto

Before the Yocto build this should be run locally.
Networking to get dependencies is not allowed for security in a Yocto build.
```
mix deps.get --only prod
tar -czvf supervision-platform-deps.tar.gz deps/
```
