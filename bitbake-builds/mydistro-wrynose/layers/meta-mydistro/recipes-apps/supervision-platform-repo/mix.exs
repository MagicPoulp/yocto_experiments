defmodule EMS.MixProject do
  use Mix.Project

  def project do
    [
      app: :ems,
      version: "1.0.0",
      elixir: "~> 1.19",
      start_permanent: Mix.env() == :prod,
      aliases: [test: "test --no-start"],
      deps: deps()
    ]
  end

  # Run "mix help compile.app" to learn about applications.
  def application do
    [
      extra_applications: [:logger],
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
