package custom_builtins

zero_arg() := custom.zeroArgBuiltin()

one_arg() := custom.oneArgBuiltin(input.args[0])

two_arg() := custom.twoArgBuiltin(
    input.args[0],
    input.args[1],
)

three_arg() := custom.threeArgBuiltin(
    input.args[0],
    input.args[1],
    input.args[2],
)

four_arg() := custom.fourArgBuiltin(
    input.args[0],
    input.args[1],
    input.args[2],
    input.args[3],
)

valid_json if {
    json.is_valid("{}")
}
