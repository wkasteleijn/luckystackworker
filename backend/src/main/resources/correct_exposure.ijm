level = ${level};
isStack = ${isStack};
factor1 = 4000;
factor2 = 1000;
run("Window/Level...");
total1 = 48984 + (level * factor1);
total2 = 0 - (level * factor2);
setMinAndMax(total2, total1);
if (isStack) {
	run("Next Slice [>]");
	run("Window/Level...");
	setMinAndMax(total2, total1);
	run("Next Slice [>]");
	run("Window/Level...");
	setMinAndMax(total2, total1);
}
run("Apply LUT");
run("Close");