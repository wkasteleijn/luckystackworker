open("${inputFile}");

radius = ${radius};
amount = ${amount};
level = ${level};
iterations =  ${iterations};
theta = ${denoise};
level = ${level};
isStack = ${isStack};
outputFile = "${outputFile}";

// correct exposure
if (level>0) {
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
}

// apply unsharp mask
for (i=0;i<iterations;i++) {
	run("Unsharp Mask...", "radius="+radius+" mask="+amount);
}

// denoise
if (theta>0) {
	run("32-bit");
	run("ROF Denoise...", "theta="+theta);
	setOption("ScaleConversions", true);
	run("16-bit");
}

saveAs("PNG", "${outputFile}");
