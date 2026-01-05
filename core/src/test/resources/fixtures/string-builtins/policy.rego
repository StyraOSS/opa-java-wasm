package string_builtins

invoke_sprintf := {
  "printed": sprintf("hello %s your number is %d!", ["user", 321])
}

integer_fastpath := {
  "printed": sprintf("%d", [123])
}

string_example := {
  "printed": sprintf("%s", ["my string"])
}
