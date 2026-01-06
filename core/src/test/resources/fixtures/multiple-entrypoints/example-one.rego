package example.one

import data.example.one.myCompositeRule

default myRule := false
default myOtherRule := false

myRule if {
    input.someProp == "thisValue"
}

myOtherRule if {
    input.anotherProp == "thatValue"
}
