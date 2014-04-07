#
# remove an element from an array:
Array::remove = (e) -> @[t..t] = [] if (t = @indexOf(e)) > -1
# determine if an array contains the element
Array::contains = (e) -> @indexOf(e) > -1

alert 'hello world!'
