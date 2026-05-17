defmodule WeatherAcq.FailoverManager do
  use GenServer
  require Logger

  def start_link(opts), do: GenServer.start_link(__MODULE__, opts, name: __MODULE__)

  def init(opts) do
    Process.flag(:trap_exit, true) # Intercept worker crashes

    state = %{
      primary:        Keyword.fetch!(opts, :primary),
      secondary:      Keyword.fetch!(opts, :secondary),
      using_fallback: false,
      worker_pid:     nil # Track PID instead of Ref
    }
    {:ok, state, {:continue, :start_worker}}
  end

  def handle_continue(:start_worker, state) do
    {:noreply, start_worker(state.primary, state)}
  end

  # Handle primary crash via EXIT signal
  def handle_info({:EXIT, pid, reason}, %{worker_pid: pid, using_fallback: false} = state) do
    Logger.error("FailoverManager: primary crashed (#{inspect(reason)}), switching to secondary")
    {:noreply, start_worker(state.secondary, %{state | using_fallback: true})}
  end

  # Handle secondary crash via EXIT signal
  def handle_info({:EXIT, pid, reason}, %{worker_pid: pid, using_fallback: true} = state) do
    Logger.error("FailoverManager: secondary crashed (#{inspect(reason)}), restarting secondary")
    {:noreply, start_worker(state.secondary, state)}
  end

  # Ignore exit signals from other random processes (like standard Logger/GenServer internal noise)
  def handle_info({:EXIT, _pid, _reason}, state) do
    Logger.error("FailoverManager: impossible EXIT case")
    {:noreply, state}
  end

  def start_worker(opts, state) do
    module = Keyword.fetch!(opts, :module)
    # This creates the link
    {:ok, pid} = module.start_link(opts)
    %{state | worker_pid: pid}
  end
end

