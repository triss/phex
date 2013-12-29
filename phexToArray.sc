+String {
	// convert string in phex style to an array
	// of values mapped according to spec and
	// with spaces replaced with \rests
	phexToArray { |spec|
		var a = [];
		
		spec = (spec ?? { [0, 16] }).asSpec;

		this.do { |c| 
			a = a ++ if(c == $ ) { 
				\rest 
			} {
				spec.map(c.digit.clip(0, 15) / 15);
			}
		};
		
		^a;
	}
}
