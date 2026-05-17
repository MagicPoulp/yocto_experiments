
defmodule WeatherAcq.MeteomaticsWorker do
  alias WeatherAcq.JsonValidator, as: JsonValidator

  def start_link() do
    pid = spawn_link(__MODULE__, :init, [])
    {:ok, pid}
  end

  def init() do
    IO.puts "Start MeteomaticsWorker with pid #{inspect self()}"
    worker_loop()
  end

  def worker_loop() do
    IO.puts("loop")
    get_json()
    :timer.sleep(5000)
    worker_loop()
  end


  def build_videos_url(_opts \\ %{}) do
    "https://localhost:3000/api/items"
  end

  def get_json(opts \\ %{}) do
    with url <- build_videos_url(opts),
         # temporary ssl disabling
         response <- HTTPoison.get!(url, [], [ssl: [verify: :verify_none]]),
      decoded <- Poison.decode!(response.body) do
      JsonValidator.validate_json(decoded, :meteomatics_worker)
      JsonValidator.validate_strings_for_sql_injection(decoded)
      forecastPoints = create_forecast_points(decoded)
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
