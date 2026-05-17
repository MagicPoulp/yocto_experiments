
defmodule WeatherAcq.MeteomaticsCppWorker do

  def start_link() do
    pid = spawn_link(__MODULE__, :init, [])
    {:ok, pid}
  end

  def init() do
    IO.puts("init")
    :exec.start
     {:ok, pid, _} = :exec.run("/LONGPATH/main",
      #[{:stdout, fn(_fdName,_osPid, data) -> IO.puts("-->#{data}\n") end}])
      [{:stdout, WeatherAcq.MeteomaticsCppWorker.callbackGetData}])
    :erlang.monitor(:process, pid)
    receive do
       {:DOWN, _aliasMonReqId, _process, _serverPid, _exitReason} ->
        raise("detection of a crashed process MeteomaticsCppWorker")
        _ -> :ok
    end
  end

  def callbackGetData do
    fn(_fdName,_osPid, data) ->
      IO.puts("-->#{data}|\n")
      if String.contains? data, "wrap_msg:dataset1" do
        IO.puts(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> dataset1 received\n")
      end
    end
  end

end
