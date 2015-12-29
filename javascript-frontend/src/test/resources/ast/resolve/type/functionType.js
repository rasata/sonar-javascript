function f1(p1){}

var f2 = function(p2, p3){};

f1(2);

f2("str", true);
f2(1);
f2(true, true, true);

f3 = function({name:n, age:a}, msg){}

f3(obj, "str");


function f4(p4) {} // p4 -> [UNKNOWN]

function f5(p5) { p5 = true; } // p5 -> [UNKNOWN, NUMBER]
f5(unknown);
f5(5);

function f6(p6) {} // p6 -> [UNKNOWN, NUMBER]
f6(5);
f6(unknown);

function f7(p7) { } // p7 -> [NUMBER, STRING]
f7(5);
f7("str");

function f8(p8) { p8 = true; } // p7 -> [NUMBER, BOOLEAN, UNKNOWN]
f8(5);

