  # weather is the DB table
#  schema "weather.forecast" do
#    field :temperature, :float
#    field :wind_speed, :float
#    field :wind_direction, :float
#    field :global_irradiance, :float
#    field :cloud_cover, :float
#    field :snow_depth, :float
#    field :humidity, :float
#    field :air_pressure, :float
#    field :air_density, :float
#    field :dust_concentration, :float
#    field :dew_point, :float
#    field :precipitation, :float
#  end

defmodule WeatherAcq.DbManager do
  @moduledoc """
  Database Manager
  """

  def store(allForecastPoints) do
    {:ok, pid} = Postgrex.start_link(hostname: "localhost", username: "ems", password: "a", database: "ems")
    delete_old_data_in_same_range(pid)
    storeForecastPoints(allForecastPoints, pid)
    # this erases the local server data
    # TODO
    #delete_very_old_data(pid)
  end

  ##TODO it does not check the range yet
  def delete_old_data_in_same_range(pid) do
    query = "DELETE FROM weather.forecast;"
    Postgrex.query!(pid, query, [])
  end

  # we do a bulk insert
  @doc """
  the input contains a list of elements like this
  {"2024-07-03 14:00:00",
   %{
     "air_density" => 1.163,
     "air_pressure" => 100909.0,
     "cloud_cover" => 42.4,
     "dew_point" => 10.5,
     "forecast_time" => "2024-07-03 14:00:00",
     "global_irradiance" => 677.8,
     "humidity" => 34.4,
     "precipitation" => 0.0,
     "snow_depth" => 0.0,
     "temperature" => 27.6,
     "wind_direction" => 321.1,
     "wind_speed" => 3.5
   }}

  A bulk insert looks like this:
  INSERT INTO films (code, title, did, date_prod, kind) VALUES
  ('B6717', 'Tampopo', 110, '1985-02-10', 'Comedy'),
  ('HG120', 'The Dinner Game', 140, DEFAULT, 'Comedy');
  """
  def storeForecastPoints(allForecastPoints, _) when length(allForecastPoints) == 0 do
    :ok
  end

  def storeForecastPoints(allForecastPoints, pid) do
    valuesList = Enum.reduce(allForecastPoints, [], fn {_, point}, acc1 ->
      point2 = Map.put(point, "forecast_time", "'" <> point["forecast_time"] <> "'")
      ["(" <> Enum.join(Map.values(point2), ",") <> ")" | acc1]
    end)
    valuesList = Enum.reverse(valuesList)
    valuesList = Enum.join(valuesList, ",")
    firstPoint = Enum.at(allForecastPoints, 0)
    firstPointMap = elem(firstPoint, 1)
    keysList = Map.keys(firstPointMap)
    keysList = Enum.join(keysList, ",")
    query = "INSERT INTO weather.forecast (" <> keysList <> ") VALUES " <> valuesList <> ";"
    #IO.inspect(query)
    Postgrex.query!(pid, query, [])
  end

  def delete_very_old_data(pid) do
    Postgrex.query!(pid, "DELETE from weather.forecast WHERE forecast_time < CURRENT_DATE - interval '1 month';", [])
  end
end
