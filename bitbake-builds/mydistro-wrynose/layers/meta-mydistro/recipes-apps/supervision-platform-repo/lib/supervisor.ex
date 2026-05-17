defmodule WeatherAcq.Supervisor do

  use Supervisor

  def start_link(opts) do
    Supervisor.start_link(__MODULE__, opts, name: __MODULE__)
  end

  def init(_opts) do
    children = [
      #%{
      #  id: WeatherAcq.MeteomaticsWorker,
      #  start: {WeatherAcq.MeteomaticsWorker, :start_link, []}
      #},
      %{
        id: WeatherAcq.MeteomaticsCppWorker,
        start: {WeatherAcq.MeteomaticsCppWorker, :start_link, []}
      },
    ]

    Supervisor.init(children, strategy: :one_for_one, max_restarts: 1, max_seconds: 200)
  end
end
