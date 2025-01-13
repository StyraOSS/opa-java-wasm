package string_builtins

invoke_sprintf = x {
   x = { "printed": sprintf("hello %s your number is %d!", ["user", 321]) }
}

integer_fastpath = x {
   x = { "printed": sprintf("%d", [123]) }
}

string_example = x {
   x = { "printed": sprintf("%s", ["my string"]) }
}
