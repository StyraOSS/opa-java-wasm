package opa.wasm.test

default allowed := false

allowed if {
    user := input.user
    data.role[user] == "admin"
}
