defmodule Generator do
  #@alphabet Enum.concat([?0..?9, ?A..?Z, ?a..?z])
  @alphabetUtf8 Enum.concat([
    #32..(2**7-1),
    0..(2**7-1),
  ])

  def getAlphabetUtf8 do
    @alphabetUtf8
  end

  #https://stackoverflow.com/questions/10229156/how-many-characters-can-utf-8-encode
  def randutf8(randUtf) do
    Enum.map(randUtf, fn val -> Enum.at(@alphabetUtf8, val) end)
    |> List.to_string()
  end
end

defmodule PropcheckTest do
  @moduledoc """
  Basic Tests for PropCheck, delegating mostly to doc tests.
  """
  use ExUnit.Case, async: true
  use PropCheck, default_opts: &PropCheck.TestHelpers.config/0

  import ExUnit.CaptureIO
  require Logger

  @moduletag capture_log: true

  def randUtf8ListGen do
    list(integer(0, Enum.count(Generator.getAlphabetUtf8())-1))
  end

  @weatherParameters ["precip_1h:mm",     "t_2m:C",
                      "wind_speed_2m:ms", "wind_dir_2m:d",
                      "global_rad:W",     "effective_cloud_cover:p",
                      "fresh_snow_1h:cm", "relative_humidity_2m:p",
                      "sfc_pressure:Pa",  "air_density_2m:kgm3",
                       "dew_point_2m:C"]

  def dataGen do
    let [
      valueParams <- Enum.map(@weatherParameters, fn _ -> float() end)
    ] do
      allData = @weatherParameters
      |> Stream.with_index
      |> Enum.map(fn {parameter, index} ->
        dataTemplate = """
        {
          "parameter": "PARAMETER_PARAM_HERE",
          "coordinates": [
             {
             "lat": 43.59152,
             "lon": 3.934591,
             "dates": [
               {
               "date": "2024-07-01T14:00:00Z",
               "value": VALUE_PARAM_HERE
               },
               {
               "date": "2024-07-01T14:30:00Z",
               "value": VALUE_PARAM_HERE
               }
             ]
             }
          ]
        }
        """
        dataTemplate = String.replace(dataTemplate, ~r/PARAMETER_PARAM_HERE/, parameter)
        dataTemplate = String.replace(dataTemplate, ~r/VALUE_PARAM_HERE/, Float.to_string(Enum.at(valueParams, index)))
        dataTemplate
      end)
      allDataWithCommas = Enum.join(allData, ",")
      result = Enum.join(["[", allDataWithCommas, "]"], " ")
      Poison.decode!(result)
    end
  end

  @doc """
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
  {
  "date": "2024-07-01T14:00:00Z",
  "value": 30.5
  },
  """
  test "let/2 generates larger lists of bindings" do
    :rand.seed(:exsplus, :os.timestamp())
    let_gen =
      let [
        randUtf1 <- randUtf8ListGen(),
        randUtf2 <- randUtf8ListGen(),
        randUtf3 <- randUtf8ListGen(),
        randUtf4 <- randUtf8ListGen(),
        randData <- dataGen(),
      ] do
      version = Generator.randutf8(randUtf1)
      user = Generator.randutf8(randUtf2)
      dateGenerated  = Generator.randutf8(randUtf3)
      status = Generator.randutf8(randUtf4)
      data = randData
      keywordList = [{"version", version}, {"user", user}, {"dateGenerated", dateGenerated}, {"status", status}, {"data", data}]
      keywordList2 = Enum.shuffle keywordList
      #IO.inspect(keywordList2)
      ordered = Jason.OrderedObject.new(keywordList2)
      encodedJson = Jason.encode!(ordered)
      IO.puts(Jason.Formatter.pretty_print(encodedJson))
      encodedJson
    end
    #assert capture_io(fn ->
      quickcheck(
        #forall x <- let_gen, [{:detect_exceptions, true}] do
        forall x <- let_gen do
          {result, decoded} = Poison.decode(x)
          WeatherAcq.JsonValidator.validate_json(decoded, :meteomatics_worker)
          #raise "test an exception"
          equals(:ok, result)
        end
      )
    #end) =~ "Passed"
  end
end
