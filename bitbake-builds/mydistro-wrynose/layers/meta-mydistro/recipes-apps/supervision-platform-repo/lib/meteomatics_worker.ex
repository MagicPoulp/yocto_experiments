
defmodule WeatherAcq.MeteomaticsWorker do
  alias WeatherAcq.JsonValidator, as: JsonValidator

  @fake_json_raw """
  {
    "version": "3.0",
    "user": "Schneider_beginner",
    "dateGenerated": "2024-07-01T14:47:50Z",
    "status": "OK",
    "data": [
      {
        "parameter": "precip_1h:mm",
        "coordinates": [
          {
            "lat": 43.59152,
            "lon": 3.934591,
            "dates": [
              { "date": "2024-07-01T14:00:00Z", "value": 30.5 },
              { "date": "2024-07-01T20:00:00Z", "value": 22.9 },
              { "date": "2024-07-02T02:00:00Z", "value": 19.5 },
              { "date": "2024-07-02T08:00:00Z", "value": 21.9 },
              { "date": "2024-07-02T14:00:00Z", "value": 27.5 },
              { "date": "2024-07-02T20:00:00Z", "value": 21.7 },
              { "date": "2024-07-03T02:00:00Z", "value": 18.3 },
              { "date": "2024-07-03T08:00:00Z", "value": 22.5 },
              { "date": "2024-07-03T14:00:00Z", "value": 27.6 }
            ]
          }
        ]
      },
      {
        "parameter": "t_2m:C",
        "coordinates": [
          {
            "lat": 43.59152,
            "lon": 3.934591,
            "dates": [
              { "date": "2024-07-01T14:00:00Z", "value": 30.5 },
              { "date": "2024-07-01T20:00:00Z", "value": 22.9 },
              { "date": "2024-07-02T02:00:00Z", "value": 19.5 },
              { "date": "2024-07-02T08:00:00Z", "value": 21.9 },
              { "date": "2024-07-02T14:00:00Z", "value": 27.5 },
              { "date": "2024-07-02T20:00:00Z", "value": 21.7 },
              { "date": "2024-07-03T02:00:00Z", "value": 18.3 },
              { "date": "2024-07-03T08:00:00Z", "value": 22.5 },
              { "date": "2024-07-03T14:00:00Z", "value": 27.6 }
            ]
          }
        ]
      }
    ]
  }
  """

  def start_link() do
    pid = spawn_link(__MODULE__, :init, [])
    {:ok, pid}
  end

  def init() do
    IO.puts "Start MeteomaticsWorker with pid #{inspect self()}"
    currentIndex = 0
    worker_loop(currentIndex)
  end

  def worker_loop(currentIndex) do
    IO.puts("\nloop: item index: #{currentIndex}")
    next_index = get_json_fake(currentIndex)
    :timer.sleep(3000)
    #Pass the next index down to the next loop
    worker_loop(next_index)
  end

  def build_videos_url(_opts \\ %{}) do
    "https://localhost:3000/api/items"
  end

  def get_json_fake(currentIndex, opts \\ %{}) do
    rawJson = @fake_json_raw
    decoded = Poison.decode!(rawJson)
    JsonValidator.validate_json(decoded, :meteomatics_worker)
    JsonValidator.validate_strings_for_sql_injection(decoded)
    forecastPoints = create_forecast_points(decoded)
    total_elements = length(forecastPoints)
    if total_elements > 0 do
      single_point = Enum.at(forecastPoints, currentIndex)
      IO.inspect(single_point, label: "Point #{currentIndex}")

      # Return the incremented index (wrapped around by total elements)
      rem(currentIndex + 1, total_elements)
    else
      IO.puts("No forecast data found.")
      0
    end
  end

  def get_json(opts \\ %{}) do
    with url <- build_videos_url(opts),
         # temporary ssl disabling
         response <- HTTPoison.get!(url, [], [ssl: [verify: :verify_none]]),
      decoded <- Poison.decode!(response.body) do
      JsonValidator.validate_json(decoded, :meteomatics_worker)
      JsonValidator.validate_strings_for_sql_injection(decoded)
      forecastPoints = create_forecast_points(decoded)
      IO.puts(forecastPoints)
      WeatherAcq.DbManager.store(forecastPoints)
    end
  end

  def create_forecast_points(decoded) do
    usefulParametersMap = %{
      "precip_1h:mm1": "precipitation",
      "t_2m:C": "temperature",
      "wind_speed_2m:ms": "wind_speed",
      "wind_dir_2m:d": "wind_direction",
      "global_rad:W": "global_irradiance",
      "effective_cloud_cover:p": "cloud_cover",
      "fresh_snow_1h:cm": "snow_depth",
      "relative_humidity_2m:p": "humidity",
      "sfc_pressure:Pa": "air_pressure",
      "air_density_2m:kgm3": "air_density",
      "dew_point_2m:C": "dew_point"
    }
    # left column
    usefulParametersList = Map.keys(usefulParametersMap)
    usefulParametersList = Enum.map(usefulParametersList, fn v -> Atom.to_string(v) end)

    # right column
    usefulParametersListPretty = Map.values(usefulParametersMap)

    # the key will have a dateSqlFriendly
    # and the value will have a map with pretty parameters and decoded values
    allForecastPoints = %{}
    %{
      "version" => _,
      "data" => data
    } = decoded
    # first loop, we jsut stor dates
    allForecastPoints = Enum.reduce(data, allForecastPoints, fn dataElement, acc1 ->
      %{"parameter" => parameter, "coordinates" => coordinates} = dataElement
      Enum.reduce(coordinates, acc1, fn coordinatesElement, acc2 ->
        %{"dates" => dates, "lat" => _, "lon" => _} = coordinatesElement
        Enum.reduce(dates, acc2, fn datesElement, acc3 ->
          %{"date" => date, "value" => _} = datesElement
          dateSqlFriendly = makeDate(date)
          case Enum.member?(usefulParametersList, parameter) do
            true -> Map.put_new(acc3, dateSqlFriendly, %{})
            _ -> acc3
          end
        end)
      end)
    end)

    # second loop, we save values
    allForecastPoints = Enum.reduce(data, allForecastPoints, fn dataElement, acc1 ->
      %{"parameter" => parameter, "coordinates" => coordinates} = dataElement
      Enum.reduce(coordinates, acc1, fn coordinatesElement, acc2 ->
        %{"dates" => dates, "lat" => _, "lon" => _} = coordinatesElement
        Enum.reduce(dates, acc2, fn datesElement, acc3 ->
          %{"date" => date, "value" => value} = datesElement
          dateSqlFriendly = makeDate(date)
          parameterPretty = usefulParametersMap[String.to_atom(parameter)]
          valueFloat = value * 1.0
          case Enum.member?(usefulParametersList, parameter) do
            true -> (fn ->
              forecastPoint = acc3[dateSqlFriendly]
              forecastPoint = Map.put(forecastPoint, parameterPretty, valueFloat)

              Map.put(acc3, dateSqlFriendly, forecastPoint)
            end).()
            _ -> acc3
          end
        end)
      end)
    end)
    allForecastPoints = Enum.map(allForecastPoints, fn {k, v} ->
      # we save the timestamp key
      v2 = Map.put(v, "forecast_time", k)
      v2 = Enum.reduce(usefulParametersListPretty, v2, fn prettyParameter, acc1 ->
        # we fill in zeros for missing values
        case acc1[prettyParameter] do
          nil -> Map.put(acc1, prettyParameter, 0.0)
          _ -> acc1
        end
      end)
      {k, v2}
    end)
    #IO.inspect(allForecastPoints)
    allForecastPoints
  end

  def makeDate(date1) do
    WeatherAcq.JsonValidator.validate_timestamp(date1)
    date2 = String.replace(date1, ~r/[T]/, " ")
    date3 = String.replace(date2, ~r/[Z]/, "")
    date3
  end
end
