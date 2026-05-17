# Before the Yocto build this should be run locally.
# Networking to get dependencies is not allowed for security in a Yocto build.
# mix deps.get --only prod
# mix deps.compile
# tar -czvf supervision-platform-deps.tar.gz deps/
defmodule EMS.MixProject do
  use Mix.Project

  def project do
    [
      app: :ems,
      version: "1.0.0",
      elixir: "~> 1.19",
      start_permanent: Mix.env() == :prod,
      aliases: [test: "test --no-start"],
      deps: deps(),
      releases: [
        supervision_platform: [
          # Tells mix to use the system's Yocto Erlang installation
          include_erts: false,
          include_executables_for: [:unix],
          applications: [runtime_tools: :permanent]
        ]
      ]
    ]
  end

  # Run "mix help compile.app" to learn about applications.
  def application do
    [
      # erlexecis needed to do exec.run and start C++ processes
      extra_applications: [:logger, :erlexec],
      mod: {EMS.Application, []}
    ]
  end

  # Run "mix help deps" to learn about dependencies.
  defp deps do
    [
      {:propcheck, "~> 1.4", only: [:test]},
      {:jason, "~> 1.4.4", only: [:test]},

      {:httpoison, "~> 2.2.1"},
      {:poison, "~> 6.0"},
      {:postgrex, ">= 0.18.0"},
      {:erlexec, "~> 2.0.5"}
    ]
  end
end
