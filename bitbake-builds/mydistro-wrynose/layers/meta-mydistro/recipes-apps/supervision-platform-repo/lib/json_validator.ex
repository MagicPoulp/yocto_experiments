
defmodule WeatherAcq.JsonValidator do

  @doc """
  pattern match all elements and and check the types
  """
  def validate_json(json, :meteomatics_worker) do
    %{
      "version" => _,
      "data" => data
    } = json
    check_is_non_empty_list(data)
    Enum.each(data, fn dataElement ->
        %{"parameter" => _, "coordinates" => coordinates} = dataElement
        check_is_non_empty_list(coordinates)
        Enum.each(coordinates, fn coordinatesElement ->
          %{"dates" => dates, "lat" => _, "lon" => _} = coordinatesElement
          check_is_non_empty_list(dates)
          Enum.each(dates, fn datesElement ->
            %{"date" => date, "value" => value} = datesElement
            check_is_string(date)
            check_is_number(value)
          end)
        end)
    end)
  end

  def check_is_non_empty_list(list) do
    case list do
      listMatch when is_list(listMatch) and length(listMatch) > 0 ->
        :ok
      _ -> raise "JSON Parsing Error - invalid non empty list"
    end
  end

  def check_is_number(value) do
    case value do
      valueMatch when is_number(valueMatch) ->
        :ok
      _ -> raise "JSON Parsing Error - invalid number"
    end
  end

  def check_is_string(value) do
    case value do
      # is_binary for a string
      # https://stackoverflow.com/questions/54161232/use-is-bitstring-or-is-binary-in-elixir-guard-for-string
      valueMatch when is_binary(valueMatch) ->
        :ok
      _ -> raise "JSON Parsing Error - invalid string"
    end
  end

  def validate_strings_for_sql_injection(decoded) do
    %{
      "version" => _,
      "data" => data
    } = decoded
    Enum.each(data, fn dataElement ->
        %{"parameter" => _, "coordinates" => coordinates} = dataElement
        Enum.each(coordinates, fn coordinatesElement ->
          %{"dates" => dates, "lat" => _, "lon" => _} = coordinatesElement
          Enum.each(dates, fn datesElement ->
            %{"date" => date, "value" => _} = datesElement
            validate_timestamp(date)
          end)
        end)
    end)
  end

  # 2024-07-01T14:00:00Z
  def validate_timestamp(date) do
    match = String.match?(date, ~r/^\d\d\d\d-\d\d-\d\dT\d\d:\d\d:\d\dZ$/)
    case match do
      true ->
        :ok
      _ -> raise "JSON Parsing Error - invalid timestamp"
    end

  end
end
