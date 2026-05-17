defmodule WeatherAcq.MeteomaticsCppWorker do
  use GenServer
  require Logger

  def start_link(opts \\ []) do
    GenServer.start_link(__MODULE__, opts)
  end

  @impl true
  def init(opts) do
    Logger.info("MeteomaticsCppWorker: starting")
    path = Keyword.fetch!(opts, :path)
    :exec.start()

    # Capture the TRUE GenServer PID right here during initialization
    worker_pid = self()

    # Wrap the callback so it passes the correct target PID along with the data
    {:ok, os_pid, _} = :exec.run(path, [
      {:stdout, fn fd, os_pid, data -> callback_get_data(fd, os_pid, data, worker_pid) end}
    ])

    ref = :erlang.monitor(:process, os_pid)
    {:ok, %{os_pid: os_pid, ref: ref}}
  end

  # C++ process died
  @impl true
  def handle_info({:DOWN, _ref, :process, _pid, reason}, state) do
    Logger.error("CppWorker: C++ process crashed: #{inspect(reason)}")
    {:stop, {:cpp_process_crashed, reason}, state}
  end

  # stdout data from C++ process
  @impl true
  def handle_info({:stdout, _pid, data}, state) do
    Logger.info("CppWorker: received data: #{data}")
    if String.contains?(data, "wrap_msg:dataset1") do
      Logger.info("CppWorker: dataset1 received")
    end
    {:noreply, state}
  end

  # Updated callback logic
  defp callback_get_data(_fd, _os_pid, data, target_pid) do
    # Explicitly send to our pinned GenServer PID, not self()
    send(target_pid, {:stdout, _os_pid, data})

    # Return an empty tuple/list back to erlexec.
    # This prevents the "unknown msg" warning inside exec.erl.
    {}
  end
end
