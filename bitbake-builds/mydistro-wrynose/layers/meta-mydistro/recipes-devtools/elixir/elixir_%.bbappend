
# To remove this error while keeping production debug capabilities
# ERROR: elixir-1.19.5-r0 do_package_qa: QA Issue: File /usr/lib/elixir/lib/eex/ebin/Elixir
# .EEx.SmartEngine.beam in package elixir-eex contains reference to TMPDIR [buildpaths]
INSANE_SKIP:append:pn-elixir = " buildpaths"
INSANE_SKIP:append:pn-elixir-eex = " buildpaths"
