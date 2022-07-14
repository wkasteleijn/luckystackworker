setMinAndMax(${newMin}, ${newMax});
if (${isStack}) {
	run("Next Slice [>]");
	setMinAndMax(${newMin}, ${newMax});
	run("Next Slice [>]");
	setMinAndMax(${newMin}, ${newMax});
}
run("Apply LUT");
