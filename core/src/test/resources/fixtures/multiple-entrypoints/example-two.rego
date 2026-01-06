package example.two

import data.example.two.coolRule

default theirRule := false
default ourRule := false

theirRule if {
    input.anyProp == "aValue"
}

ourRule if {
    input.ourProp == "inTheMiddleOfTheStreet"
}
