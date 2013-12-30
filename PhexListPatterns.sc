// abstract factory for creating list patterns that
// take Phex style hex strings as inputs.
HexListPattern {
	*new { |string=" " spec repeats=inf listPattern|
		// use default spec if none provided
		spec = (spec ?? { [0, 15] }).asSpec;

		^listPattern.new(string.phexToPatternList(spec), repeats)
	}
}

// a straight forward "phex" - just a Pseq behind the scenes
Phex {
	*new { |string=" " spec repeats=inf|
		^HexListPattern(string, spec, repeats, Pseq);
	}
}

// a Phuf is the Phex equivalent of a Pshuf
Phuf {
	*new { |string=" " spec repeats=inf|
		^HexListPattern(string, spec, repeats, Pshuf);
	}
}
