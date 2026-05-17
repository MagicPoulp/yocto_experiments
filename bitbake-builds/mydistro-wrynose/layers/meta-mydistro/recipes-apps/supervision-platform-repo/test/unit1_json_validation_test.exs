defmodule WeatherAcqTest do
  use ExUnit.Case
  #doctest WeatherAcq

  test "validate_json from a file example_json1" do
    pathJson1 = Path.join([File.cwd!, "/test", "/data", "/example_json1.json"])
    { :ok, fileData} = File.read(pathJson1)
    #IO.inspect(fileData)
    decoded = Poison.decode!(fileData)
    WeatherAcq.JsonValidator.validate_json(decoded, :meteomatics_worker)
    #assert true == true
  end

  test "validate_json from a file example_json2" do
    pathJson1 = Path.join([File.cwd!, "/test", "/data", "/example_json2.json"])
    { :ok, fileData} = File.read(pathJson1)
    #IO.inspect(fileData)
    decoded = Poison.decode!(fileData)
    WeatherAcq.JsonValidator.validate_json(decoded, :meteomatics_worker)
  end

  test "validate_json from a bad file example_bad_json1" do
    pathJson1 = Path.join([File.cwd!, "/test", "/data", "/example_bad_json1.json"])
    { :ok, fileData} = File.read(pathJson1)
    #IO.inspect(fileData)
    decoded = Poison.decode!(fileData)
    try do
      WeatherAcq.JsonValidator.validate_json(decoded, :meteomatics_worker)
    rescue
      _ in MatchError -> :ok
      other -> raise other
    end
  end
end
